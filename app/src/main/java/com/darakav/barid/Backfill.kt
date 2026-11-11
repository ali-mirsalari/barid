package com.darakav.barid

import android.content.Context
import android.provider.Telephony

/** Queues bank messages already in the inbox (needs READ_SMS); Darakav skips ones it already has. */
object Backfill {
    const val DAYS = 60

    fun run(context: Context): Int {
        val since = System.currentTimeMillis() - DAYS * 24L * 60 * 60 * 1000
        val queue = MessageQueue(context)
        var count = 0
        context.contentResolver.query(
            Telephony.Sms.Inbox.CONTENT_URI,
            arrayOf(Telephony.TextBasedSmsColumns.ADDRESS, Telephony.TextBasedSmsColumns.BODY, Telephony.TextBasedSmsColumns.DATE),
            "${Telephony.TextBasedSmsColumns.DATE} >= ?",
            arrayOf(since.toString()),
            "${Telephony.TextBasedSmsColumns.DATE} ASC",
        )?.use { cursor ->
            while (cursor.moveToNext()) {
                val from = cursor.getString(0) ?: ""
                val body = cursor.getString(1) ?: continue
                if (!SmsFilter.shouldForward(from, body)) continue
                queue.add(from, body, cursor.getLong(2))
                count++
            }
        }
        if (count > 0) ForwardWorker.schedule(context)
        return count
    }
}
