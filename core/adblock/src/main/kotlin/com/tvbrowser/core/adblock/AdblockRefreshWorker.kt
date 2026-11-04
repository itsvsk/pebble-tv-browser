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
) : CoroutineWorker(appContext, params) {

    override suspend fun doWork(): Result =
        if (loader.refresh().isSuccess) Result.success()
        else if (runAttemptCount < 3) Result.retry() else Result.failure()

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
