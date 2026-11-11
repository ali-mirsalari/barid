package com.darakav.barid

import android.content.Context
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlinx.coroutines.withContext

/**
 * Sends everything waiting in the queue. Used by [ForwardWorker] in the background and by the app
 * itself when it is open, so a phone that delays background work still delivers; one run at a time.
 * A batch the server refuses as invalid is retried one message at a time, so one bad message cannot
 * block the rest.
 */
object Delivery {
    sealed interface Outcome {
        /** The queue is empty (or there is nothing to send to). */
        data class Done(val sent: Int) : Outcome
        /** The server refused the key; forwarding stops until the phone is connected again. */
        data object Rejected : Outcome
        /** Something went wrong; the rest waits for the next try. */
        data class Failed(val sent: Int, val reason: String) : Outcome
    }

    private val mutex = Mutex()

    suspend fun run(context: Context): Outcome = mutex.withLock {
        withContext(Dispatchers.IO) {
            val app = context.applicationContext
            // A bug must never take the app down or lose messages: it is recorded as a failed try.
            val outcome = try {
                deliver(app)
            } catch (e: Exception) {
                Settings(app).lastError = e.javaClass.simpleName
                Outcome.Failed(0, e.javaClass.simpleName)
            } catch (e: LinkageError) {
                Settings(app).lastError = e.javaClass.simpleName
                Outcome.Failed(0, e.javaClass.simpleName)
            }
            outcome.also { ActivityLog(app).settle(MessageQueue(app).ids()) }
        }
    }

    private fun deliver(context: Context): Outcome {
        val settings = Settings(context)
        val queue = MessageQueue(context)
        val log = ActivityLog(context)
        val pairing = settings.pairing ?: return Outcome.Done(0)
        if (settings.rejected) return Outcome.Rejected
        settings.lastAttemptAt = System.currentTimeMillis()
        var sent = 0
        fun delivered(batch: List<Queued>) {
            queue.remove(batch.map { it.id })
            log.mark(batch.map { it.id }, LogEntry.Status.SENT)
            settings.lastSentAt = System.currentTimeMillis()
            settings.sentCount = settings.sentCount + batch.size
            settings.lastError = null
            sent += batch.size
        }
        fun failed(reason: String): Outcome {
            settings.lastError = reason
            return Outcome.Failed(sent, reason)
        }
        repeat(MAX_ROUNDS) {
            val batch = queue.peek(Forwarder.batchSize(pairing))
            if (batch.isEmpty()) return Outcome.Done(sent)
            when (val result = Forwarder.send(pairing, batch)) {
                Forwarder.Result.Ok -> delivered(batch)
                Forwarder.Result.Rejected -> {
                    settings.rejected = true
                    settings.lastError = "HTTP 401"
                    return Outcome.Rejected
                }
                Forwarder.Result.Invalid -> for (single in batch) {
                    when (val one = Forwarder.send(pairing, listOf(single))) {
                        Forwarder.Result.Ok -> delivered(listOf(single))
                        Forwarder.Result.Invalid -> {
                            queue.remove(listOf(single.id))
                            log.mark(listOf(single.id), LogEntry.Status.SKIPPED)
                            settings.lastError = null
                        }
                        Forwarder.Result.Rejected -> {
                            settings.rejected = true
                            return Outcome.Rejected
                        }
                        is Forwarder.Result.Retry -> return failed(one.reason)
                    }
                }
                is Forwarder.Result.Retry -> return failed(result.reason)
            }
        }
        return Outcome.Done(sent)
    }

    private const val MAX_ROUNDS = 200
}
