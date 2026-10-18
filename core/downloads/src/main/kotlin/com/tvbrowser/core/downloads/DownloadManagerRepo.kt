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
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.flowOn
import kotlinx.coroutines.isActive
import kotlinx.coroutines.withContext
import java.io.File
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class DownloadManagerRepo @Inject constructor(
    @ApplicationContext private val context: Context,
    private val repo: DownloadRepository,
) {

    private val dm by lazy { ContextCompat.getSystemService(context, DownloadManager::class.java)!! }

    suspend fun enqueue(
        url: String,
        userAgent: String?,
        contentDisposition: String?,
        mime: String?,
        targetDir: File? = null,
    ): Long {
        val filename = URLUtil.guessFileName(url, contentDisposition, mime)
        val dir = targetDir ?: context.getExternalFilesDir(Environment.DIRECTORY_DOWNLOADS)
        val dest = File(dir, filename)

        val req = DownloadManager.Request(Uri.parse(url))
            .setTitle(filename)
            .setMimeType(mime)
            .setNotificationVisibility(DownloadManager.Request.VISIBILITY_VISIBLE_NOTIFY_COMPLETED)
            .setDestinationUri(Uri.fromFile(dest))
            .setAllowedOverMetered(true)
            .setAllowedOverRoaming(true)
        if (!userAgent.isNullOrBlank()) req.addRequestHeader("User-Agent", userAgent)
        CookieManager.getInstance().getCookie(url)?.let { req.addRequestHeader("Cookie", it) }

        val id = dm.enqueue(req)
        repo.upsert(
            DownloadRecord(
                dmId = id, url = url, filename = filename, localPath = dest.absolutePath,
                mime = mime, status = DownloadRecord.STATUS_RUNNING,
            )
        )
        return id
    }

    suspend fun cancel(dmId: Long) {
        remove(dmId)
        repo.delete(dmId)
    }

    suspend fun remove(dmId: Long) = withContext(Dispatchers.IO) { dm.remove(dmId) }

    fun openFile(record: DownloadRecord) {
        val file = File(record.localPath)
        val uri = androidx.core.content.FileProvider.getUriForFile(
            context, "${context.packageName}.fileprovider", file,
        )
        val intent = android.content.Intent(android.content.Intent.ACTION_VIEW).apply {
            setDataAndType(uri, record.mime ?: "*/*")
            addFlags(android.content.Intent.FLAG_GRANT_READ_URI_PERMISSION)
            addFlags(android.content.Intent.FLAG_ACTIVITY_NEW_TASK)
        }
        runCatching { context.startActivity(intent) }
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

internal fun recordStatus(managerStatus: Int): Int = when (managerStatus) {
    DownloadManager.STATUS_PENDING, DownloadManager.STATUS_RUNNING -> DownloadRecord.STATUS_RUNNING
    DownloadManager.STATUS_PAUSED -> DownloadRecord.STATUS_PAUSED
    DownloadManager.STATUS_SUCCESSFUL -> DownloadRecord.STATUS_SUCCESS
    else -> DownloadRecord.STATUS_FAILED
}
