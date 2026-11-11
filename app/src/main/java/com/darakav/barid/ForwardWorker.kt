package com.darakav.barid

import android.content.Context
import androidx.work.BackoffPolicy
import androidx.work.Constraints
import androidx.work.CoroutineWorker
import androidx.work.ExistingWorkPolicy
import androidx.work.NetworkType
import androidx.work.OneTimeWorkRequestBuilder
import androidx.work.WorkManager
import androidx.work.WorkerParameters
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.util.concurrent.TimeUnit

/**
 * Delivers queued messages in small batches whenever the phone is online; retries with backoff,
 * survives restarts and app updates (WorkManager). A batch the server refuses as invalid is retried
 * one message at a time so a single bad message cannot block the rest.
 */
class ForwardWorker(context: Context, params: WorkerParameters) : CoroutineWorker(context, params) {
    override suspend fun doWork(): Result = withContext(Dispatchers.IO) {
        val settings = Settings(applicationContext)
        val queue = MessageQueue(applicationContext)
        val pairing = settings.pairing ?: return@withContext Result.success()
        if (settings.rejected) return@withContext Result.success()
        var rounds = 0
        while (rounds++ < 100) {
            val batch = queue.peek(Forwarder.batchSize(pairing))
            if (batch.isEmpty()) break
            when (val result = Forwarder.send(pairing, batch)) {
                Forwarder.Result.Ok -> delivered(settings, queue, batch)
                Forwarder.Result.Rejected -> {
                    settings.rejected = true
                    settings.lastError = "HTTP 401"
                    return@withContext Result.success()
                }
                Forwarder.Result.Invalid -> for (single in batch) {
                    when (Forwarder.send(pairing, listOf(single))) {
                        Forwarder.Result.Ok -> delivered(settings, queue, listOf(single))
                        Forwarder.Result.Invalid -> {
                            queue.remove(listOf(single.id))
                            ActivityLog(applicationContext).mark(listOf(single.id), LogEntry.Status.SKIPPED)
                            settings.lastError = "HTTP 400"
                        }
                        else -> return@withContext Result.retry()
                    }
                }
                is Forwarder.Result.Retry -> {
                    settings.lastError = result.reason
                    return@withContext Result.retry()
                }
            }
        }
        Result.success()
    }

    private fun delivered(settings: Settings, queue: MessageQueue, batch: List<Queued>) {
        queue.remove(batch.map { it.id })
        ActivityLog(applicationContext).mark(batch.map { it.id }, LogEntry.Status.SENT)
        settings.lastSentAt = System.currentTimeMillis()
        settings.sentCount = settings.sentCount + batch.size
        settings.lastError = null
    }

    companion object {
        private const val NAME = "forward-sms"

        fun schedule(context: Context) {
            val request = OneTimeWorkRequestBuilder<ForwardWorker>()
                .setConstraints(Constraints.Builder().setRequiredNetworkType(NetworkType.CONNECTED).build())
                .setBackoffCriteria(BackoffPolicy.EXPONENTIAL, 30, TimeUnit.SECONDS)
                .build()
            WorkManager.getInstance(context).enqueueUniqueWork(NAME, ExistingWorkPolicy.APPEND_OR_REPLACE, request)
        }

        fun cancel(context: Context) {
            WorkManager.getInstance(context).cancelUniqueWork(NAME)
        }
    }
}
