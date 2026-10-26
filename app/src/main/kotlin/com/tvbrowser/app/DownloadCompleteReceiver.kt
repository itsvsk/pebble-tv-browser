package com.tvbrowser.app

import android.app.DownloadManager
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.util.Log
import com.tvbrowser.core.downloads.DownloadManagerRepo
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withTimeout
import javax.inject.Inject

/** DownloadManager sends this protected system broadcast for successful and failed downloads. */
@AndroidEntryPoint
class DownloadCompleteReceiver : BroadcastReceiver() {
    @Inject lateinit var downloads: DownloadManagerRepo

    override fun onReceive(context: Context, intent: Intent) {
        if (intent.action != DownloadManager.ACTION_DOWNLOAD_COMPLETE) return
        val id = intent.getLongExtra(DownloadManager.EXTRA_DOWNLOAD_ID, -1L)
        if (id < 0) return
        val pending = goAsync()
        CoroutineScope(Dispatchers.IO).launch {
            try {
                withTimeout(8_000) { downloads.refreshCompletion(id) }
            } catch (error: Exception) {
                if (error is CancellationException) throw error
                Log.w("PebbleDownloads", "Could not refresh a completed download", error)
            } finally {
                pending.finish()
            }
        }
    }
}
