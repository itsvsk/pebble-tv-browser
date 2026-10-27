package com.tvbrowser.core.downloads

import android.app.DownloadManager
import android.content.Context
import android.content.ClipData
import android.content.Intent
import android.net.Uri
import android.os.Environment
import android.webkit.CookieManager
import android.webkit.URLUtil
import android.webkit.WebSettings
import android.webkit.MimeTypeMap
import android.widget.Toast
import androidx.core.content.ContextCompat
import com.tvbrowser.core.db.entity.DownloadRecord
import com.tvbrowser.core.db.repo.DownloadRepository
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.NonCancellable
import kotlinx.coroutines.currentCoroutineContext
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.flowOn
import kotlinx.coroutines.isActive
import kotlinx.coroutines.withContext
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
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
    private val retryMutex = Mutex()

    /** The app-owned folder used for new downloads; it may be unavailable when storage is unmounted. */
    fun defaultDirectory(): File? = context.getExternalFilesDir(Environment.DIRECTORY_DOWNLOADS)

    suspend fun enqueue(
        url: String,
        userAgent: String?,
        contentDisposition: String?,
        mime: String?,
        targetDir: File? = null,
        filenameHint: String? = null,
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
                val filename = safeDownloadName(filenameHint ?: URLUtil.guessFileName(url, contentDisposition, mime))
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

    /** A retry is a fresh request with current cookies, never persisted credentials. */
    suspend fun retry(dmId: Long): Long = retryMutex.withLock {
        val record = withContext(Dispatchers.IO) { repo.get(dmId) }
            ?: throw IOException("This download is no longer in the list")
        require(record.status == DownloadRecord.STATUS_FAILED) { "Only failed downloads can be retried" }
        withContext(NonCancellable) {
            val id = withContext(Dispatchers.Main) {
                enqueue(record.url, WebSettings.getDefaultUserAgent(context), null, record.mime, filenameHint = record.filename)
            }
            try {
                withContext(Dispatchers.IO) { repo.delete(dmId) }
            } catch (error: Exception) {
                cancel(id)
                throw error
            }
            withContext(Dispatchers.IO) { runCatching { dm.remove(dmId) } }
            id
        }
    }

    suspend fun cancel(dmId: Long) = withContext(Dispatchers.IO) {
        enqueueMutex.withLock {
            dm.remove(dmId)
            repo.delete(dmId)
        }
    }

    suspend fun remove(dmId: Long) = withContext(Dispatchers.IO) { dm.remove(dmId) }

    fun openFile(record: DownloadRecord): Boolean {
        val file = File(record.localPath)
        if (!file.isFile) return false
        return runCatching {
            val uri = androidx.core.content.FileProvider.getUriForFile(
                context, "${context.packageName}.fileprovider", file,
            )
            context.startActivity(downloadChooserIntent(uri, record.mime, record.filename))
        }.isSuccess
    }

    /** Called by DownloadManager's completion broadcast, even when Downloads is not visible. */
    suspend fun refreshCompletion(dmId: Long): Boolean = withContext(Dispatchers.IO) {
        enqueueMutex.withLock {
            val record = repo.get(dmId) ?: return@withLock false
            val outcome = dm.query(DownloadManager.Query().setFilterById(dmId))?.use { cursor ->
                if (!cursor.moveToFirst()) return@use null
                recordStatus(cursor.getInt(cursor.getColumnIndexOrThrow(DownloadManager.COLUMN_STATUS))) to
                    cursor.getInt(cursor.getColumnIndexOrThrow(DownloadManager.COLUMN_REASON))
            } ?: return@withLock false
            val (status, reason) = outcome
            if (status != DownloadRecord.STATUS_SUCCESS && status != DownloadRecord.STATUS_FAILED) {
                return@withLock false
            }
            applyStatus(record, status, reason)
        }
    }

    // Serialized with enqueue/cancel so duplicate broadcasts and progress polling alert only once.
    private suspend fun applyStatus(record: DownloadRecord, status: Int, reason: Int?): Boolean {
        if (status == DownloadRecord.STATUS_FAILED && record.status == status && record.failureReason == null && reason != null) {
            repo.updateStatus(record.dmId, status, reason)
        }
        if (status == record.status || record.status == DownloadRecord.STATUS_SUCCESS ||
            record.status == DownloadRecord.STATUS_FAILED) return false
        val message = when (status) {
            DownloadRecord.STATUS_SUCCESS -> "Download complete: ${record.filename}. Open Downloads to view it."
            DownloadRecord.STATUS_FAILED -> "Download failed: ${record.filename}. Check Downloads."
            else -> null
        }
        // Updating the row restarts the screen's flatMapLatest collector. Finish the alert
        // with the update so cancellation cannot consume a completion without displaying it.
        withContext(NonCancellable) {
            repo.updateStatus(record.dmId, status, if (status == DownloadRecord.STATUS_FAILED) reason else null)
            if (message != null) withContext(Dispatchers.Main) {
                Toast.makeText(context, message, Toast.LENGTH_LONG).show()
            }
        }
        return true
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
                enqueueMutex.withLock {
                    repo.get(record.dmId)?.let { latest -> applyStatus(latest, status, progress[record.dmId]?.reason) }
                }
            }
            delay(1_000)
        }
    }.flowOn(Dispatchers.IO)
}

/** Always ask, even if a default player exists; grant access only to this file. */
fun downloadChooserIntent(uri: Uri, mime: String?, filename: String): Intent {
    val type = mime?.substringBefore(';')?.trim()?.takeUnless { it.isBlank() || it == "application/octet-stream" }
        ?: MimeTypeMap.getSingleton().getMimeTypeFromExtension(filename.substringAfterLast('.', "").lowercase())
        ?: "*/*"
    val view = Intent(Intent.ACTION_VIEW).apply {
        setDataAndType(uri, type)
        clipData = ClipData.newRawUri(filename, uri)
        addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
    }
    return Intent.createChooser(view, "Open with").addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
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
