package com.tvbrowser.core.adblock

import android.content.Context
import androidx.hilt.work.HiltWorker
import androidx.work.CoroutineWorker
import androidx.work.Constraints
import androidx.work.ExistingPeriodicWorkPolicy
import androidx.work.NetworkType
import androidx.work.PeriodicWorkRequestBuilder
import androidx.work.WorkManager
import androidx.work.WorkerParameters
import dagger.assisted.Assisted
import dagger.assisted.AssistedInject
import java.util.concurrent.TimeUnit

@HiltWorker
class AdblockRefreshWorker @AssistedInject constructor(
    @Assisted appContext: Context,
    @Assisted params: WorkerParameters,
    private val loader: EasyListLoader,
    private val interceptor: AdBlockInterceptor,
) : CoroutineWorker(appContext, params) {

    override suspend fun doWork(): Result {
        val res = loader.refresh()
        return if (res.isSuccess) {
            interceptor.replace(parseDiskHostsSafe())
            Result.success()
        } else Result.retry()
    }

    private fun parseDiskHostsSafe(): Set<String> = runCatching {
        val file = java.io.File(applicationContext.filesDir, "adblock/hosts.txt")
        if (!file.exists()) emptySet()
        else file.bufferedReader().use { EasyListParser.parse(it) }
    }.getOrDefault(emptySet())

    companion object {
        const val UNIQUE_NAME = "adblock-refresh"

        fun schedule(context: Context) {
            val req = PeriodicWorkRequestBuilder<AdblockRefreshWorker>(7, TimeUnit.DAYS)
                .setConstraints(
                    Constraints.Builder().setRequiredNetworkType(NetworkType.CONNECTED).build()
                ).build()
            WorkManager.getInstance(context).enqueueUniquePeriodicWork(
                UNIQUE_NAME, ExistingPeriodicWorkPolicy.KEEP, req,
            )
        }
    }
}
