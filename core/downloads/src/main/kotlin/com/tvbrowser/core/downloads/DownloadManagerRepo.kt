package com.tvbrowser.core.downloads

import android.app.DownloadManager
import android.content.Context
import android.net.Uri
import android.os.Environment
import android.webkit.CookieManager
import android.webkit.URLUtil
import androidx.core.content.ContextCompat
import com.tvbrowser.core.db.entity.DownloadRecord
import com.tvbrowser.core.db.repo.DownloadRepository
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.currentCoroutineContext
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.flowOn
import kotlinx.coroutines.isActive
import kotlinx.coroutines.withContext
import kotlinx.coroutines.sync.Mutex
import java.io.File
import java.io.IOException
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class DownloadManagerRepo @Inject constructor(
    @ApplicationContext private val context: Context,
    private val repo: DownloadRepository,
) {

    private val dm by lazy { ContextCompat.getSystemService(context, DownloadManager::class.java)!! }
    private val enqueueMutex = Mutex()

    /** The app-owned folder used for new downloads; it may be unavailable when storage is unmounted. */
    fun defaultDirectory(): File? = context.getExternalFilesDir(Environment.DIRECTORY_DOWNLOADS)

    suspend fun enqueue(
        url: String,
        userAgent: String?,
        contentDisposition: String?,
        mime: String?,
        targetDir: File? = null,
    ): Long {
        val scheme = Uri.parse(url).scheme
        require(scheme == "http" || scheme == "https") { "Only HTTP and HTTPS downloads are supported" }
        // WebView invokes download callbacks on the UI thread. Read cookies before moving
        // DownloadManager and database work to IO, matching the existing WebView access.
        val cookie = CookieManager.getInstance().getCookie(url)
        return withContext(Dispatchers.IO) {
            enqueueMutex.lock()
            try {
                val dir = targetDir ?: defaultDirectory() ?: throw IOException("Download storage is unavailable")
                if (!dir.isDirectory && !dir.mkdirs()) throw IOException("Download folder is unavailable")
                val filename = safeDownloadName(URLUtil.guessFileName(url, contentDisposition, mime))
                val occupiedPaths = repo.all().first().mapTo(HashSet()) { it.localPath }
                val dest = uniqueDownloadFile(dir, filename, occupiedPaths)

                val req = DownloadManager.Request(Uri.parse(url))
                    .setTitle(dest.name)
                    .setMimeType(mime)
                    .setNotificationVisibility(DownloadManager.Request.VISIBILITY_VISIBLE_NOTIFY_COMPLETED)
                    .setDestinationUri(Uri.fromFile(dest))
                    .setAllowedOverMetered(true)
                    .setAllowedOverRoaming(true)
                if (!userAgent.isNullOrBlank()) req.addRequestHeader("User-Agent", userAgent)
                if (!cookie.isNullOrBlank()) req.addRequestHeader("Cookie", cookie)

                val id = dm.enqueue(req)
                try {
                    repo.upsert(
                        DownloadRecord(
                            dmId = id, url = url, filename = dest.name, localPath = dest.absolutePath,
                            mime = mime, status = DownloadRecord.STATUS_RUNNING,
                        )
                    )
                } catch (error: Exception) {
                    runCatching { dm.remove(id) }
                    throw error
                }
                id
            } finally {
                enqueueMutex.unlock()
            }
        }
    }

    suspend fun cancel(dmId: Long) {
        remove(dmId)
        repo.delete(dmId)
    }

    suspend fun remove(dmId: Long) = withContext(Dispatchers.IO) { dm.remove(dmId) }

    fun openFile(record: DownloadRecord): Boolean {
        val file = File(record.localPath)
        if (!file.isFile) return false
        return runCatching {
            val uri = androidx.core.content.FileProvider.getUriForFile(
                context, "${context.packageName}.fileprovider", file,
            )
            val intent = android.content.Intent(android.content.Intent.ACTION_VIEW).apply {
                setDataAndType(uri, record.mime ?: "*/*")
                addFlags(android.content.Intent.FLAG_GRANT_READ_URI_PERMISSION)
                addFlags(android.content.Intent.FLAG_ACTIVITY_NEW_TASK)
            }
            context.startActivity(intent)
        }.isSuccess
    }

    fun observeProgress(records: Collection<DownloadRecord>): Flow<Map<Long, DownloadProgress>> = flow {
        if (records.isEmpty()) {
            emit(emptyMap())
            return@flow
        }
        val ids = records.map { it.dmId }.toLongArray()
        while (currentCoroutineContext().isActive) {
            val query = DownloadManager.Query().setFilterById(*ids)
            val progress = HashMap<Long, DownloadProgress>()
            val cursor = dm.query(query)
            if (cursor == null) {
                delay(1_000)
                continue
            }
            cursor.use { c ->
                val idIdx = c.getColumnIndexOrThrow(DownloadManager.COLUMN_ID)
                val sIdx = c.getColumnIndexOrThrow(DownloadManager.COLUMN_STATUS)
                val rIdx = c.getColumnIndexOrThrow(DownloadManager.COLUMN_REASON)
                val bIdx = c.getColumnIndexOrThrow(DownloadManager.COLUMN_BYTES_DOWNLOADED_SO_FAR)
                val tIdx = c.getColumnIndexOrThrow(DownloadManager.COLUMN_TOTAL_SIZE_BYTES)
                while (c.moveToNext()) {
                    val id = c.getLong(idIdx)
                    progress[id] = DownloadProgress(
                        dmId = id,
                        bytesSoFar = c.getLong(bIdx),
                        totalBytes = c.getLong(tIdx),
                        statusCode = c.getInt(sIdx),
                        reason = c.getInt(rIdx),
                    )
                }
            }
            emit(progress)
            for (record in records) {
                val status = progress[record.dmId]?.statusCode?.let(::recordStatus)
                    ?: DownloadRecord.STATUS_FAILED
                if (status != record.status) repo.updateStatus(record.dmId, status)
            }
            delay(1_000)
        }
    }.flowOn(Dispatchers.IO)
}

internal fun safeDownloadName(name: String): String {
    val result = name.substringAfterLast('/').substringAfterLast('\\').trim()
    return result.takeUnless { it.isEmpty() || it == "." || it == ".." } ?: "download"
}

internal fun uniqueDownloadFile(directory: File, name: String, occupiedPaths: Set<String> = emptySet()): File {
    val first = File(directory, name)
    if (!first.exists() && first.absolutePath !in occupiedPaths) return first
    val dot = name.lastIndexOf('.')
    val stem = if (dot > 0) name.substring(0, dot) else name
    val extension = if (dot > 0) name.substring(dot) else ""
    for (suffix in 1..999) {
        val candidate = File(directory, "$stem ($suffix)$extension")
        if (!candidate.exists() && candidate.absolutePath !in occupiedPaths) return candidate
    }
    return File(directory, "$stem (${java.util.UUID.randomUUID()})$extension")
}

internal fun recordStatus(managerStatus: Int): Int = when (managerStatus) {
    DownloadManager.STATUS_PENDING, DownloadManager.STATUS_RUNNING -> DownloadRecord.STATUS_RUNNING
    DownloadManager.STATUS_PAUSED -> DownloadRecord.STATUS_PAUSED
    DownloadManager.STATUS_SUCCESSFUL -> DownloadRecord.STATUS_SUCCESS
    else -> DownloadRecord.STATUS_FAILED
}
