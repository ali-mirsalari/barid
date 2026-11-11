package com.darakav.barid

import android.content.Context
import androidx.work.BackoffPolicy
import androidx.work.Constraints
import androidx.work.CoroutineWorker
import androidx.work.ExistingWorkPolicy
import androidx.work.NetworkType
import androidx.work.OneTimeWorkRequestBuilder
import androidx.work.WorkInfo
import androidx.work.WorkManager
import androidx.work.WorkerParameters
import java.util.concurrent.TimeUnit

/**
 * Delivers queued messages in the background whenever the phone is online ([Delivery]); retries with
 * backoff and survives restarts and app updates (WorkManager).
 */
class ForwardWorker(context: Context, params: WorkerParameters) : CoroutineWorker(context, params) {
    override suspend fun doWork(): Result = when (val outcome = Delivery.run(applicationContext)) {
        is Delivery.Outcome.Failed -> Result.retry()
        else -> Result.success().also { if (outcome is Delivery.Outcome.Done && MessageQueue(applicationContext).size() > 0) schedule(applicationContext) }
    }

    companion object {
        private const val NAME = "forward-sms"

        /**
         * Runs a delivery soon. A waiting retry is replaced, so a long backoff from earlier failures never
         * holds new messages back; a run in progress is followed by another rather than interrupted.
         */
        fun schedule(context: Context) {
            val manager = WorkManager.getInstance(context)
            val running = runCatching {
                manager.getWorkInfosForUniqueWork(NAME).get(2, TimeUnit.SECONDS).any { it.state == WorkInfo.State.RUNNING }
            }.getOrDefault(false)
            val request = OneTimeWorkRequestBuilder<ForwardWorker>()
                .setConstraints(Constraints.Builder().setRequiredNetworkType(NetworkType.CONNECTED).build())
                .setBackoffCriteria(BackoffPolicy.EXPONENTIAL, 30, TimeUnit.SECONDS)
                .build()
            manager.enqueueUniqueWork(NAME, if (running) ExistingWorkPolicy.APPEND_OR_REPLACE else ExistingWorkPolicy.REPLACE, request)
        }

        fun cancel(context: Context) {
            WorkManager.getInstance(context).cancelUniqueWork(NAME)
        }
    }
}
