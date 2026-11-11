package com.darakav.barid

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.provider.Telephony

/**
 * Receives every incoming SMS (system broadcast, also while the app is closed), keeps bank
 * transaction messages, notes them in the [ActivityLog] and hands them to [ForwardWorker]. Long messages arrive in parts; parts
 * from the same sender are joined in order.
 */
class SmsReceiver : BroadcastReceiver() {
    override fun onReceive(context: Context, intent: Intent) {
        if (intent.action != Telephony.Sms.Intents.SMS_RECEIVED_ACTION) return
        val settings = Settings(context)
        if (settings.pairing == null || settings.rejected) return
        val parts = Telephony.Sms.Intents.getMessagesFromIntent(intent) ?: return
        val queue = MessageQueue(context)
        val log = ActivityLog(context)
        var added = false
        parts.filterNotNull().groupBy { it.displayOriginatingAddress ?: it.originatingAddress ?: "" }.forEach { (from, messages) ->
            val text = messages.joinToString("") { it.displayMessageBody ?: it.messageBody ?: "" }
            if (!SmsFilter.shouldForward(from, text)) return@forEach
            val stamp = messages.minOf { it.timestampMillis }.takeIf { it > 0 } ?: System.currentTimeMillis()
            val id = queue.add(from, text, stamp) ?: return@forEach
            log.add(id, SmsSummary.read(from, text), stamp)
            added = true
        }
        if (added) ForwardWorker.schedule(context)
    }
}
