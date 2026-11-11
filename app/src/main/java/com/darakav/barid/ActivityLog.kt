package com.darakav.barid

import android.content.Context
import org.json.JSONObject
import java.io.File

/**
 * The "Recent" list: what was forwarded and whether it was sent. Holds only display facts (bank,
 * direction, amount, times, status), never the message text or sender number.
 */
data class LogEntry(
    val id: String,
    val bank: String?,
    val direction: SmsSummary.Direction?,
    val amount: Long?,
    val toman: Boolean,
    val receivedAt: Long,
    val status: Status,
    val sentAt: Long = 0,
) {
    enum class Status { WAITING, SENT, SKIPPED }

    fun toJson(): JSONObject = JSONObject().put("id", id).put("bank", bank).put("dir", direction?.name)
        .put("amount", amount).put("toman", toman).put("at", receivedAt).put("status", status.name).put("sentAt", sentAt)

    companion object {
        fun fromJson(o: JSONObject) = LogEntry(
            id = o.getString("id"),
            bank = o.optString("bank").ifEmpty { null },
            direction = o.optString("dir").ifEmpty { null }?.let { runCatching { SmsSummary.Direction.valueOf(it) }.getOrNull() },
            amount = if (o.has("amount") && !o.isNull("amount")) o.getLong("amount") else null,
            toman = o.optBoolean("toman"),
            receivedAt = o.optLong("at"),
            status = runCatching { Status.valueOf(o.getString("status")) }.getOrDefault(Status.WAITING),
            sentAt = o.optLong("sentAt"),
        )
    }
}

class ActivityLog(context: Context) {
    private val file = File(context.applicationContext.filesDir, "activity.jsonl")

    fun add(id: String, summary: SmsSummary, receivedAt: Long) = synchronized(LOCK) {
        val entry = LogEntry(id, summary.bank, summary.direction, summary.amount, summary.toman, receivedAt, LogEntry.Status.WAITING)
        write((readAll() + entry).sortedBy { it.receivedAt }.takeLast(MAX))
    }

    fun mark(ids: Collection<String>, status: LogEntry.Status, at: Long = System.currentTimeMillis()) = synchronized(LOCK) {
        if (ids.isEmpty()) return@synchronized
        write(readAll().map { if (it.id in ids) it.copy(status = status, sentAt = at) else it })
    }

    /**
     * Marks as sent the entries still shown as waiting whose message is no longer queued: delivery can
     * finish between queueing a message and listing it, which would otherwise leave it waiting forever.
     * Safe because messages are always queued before they are listed.
     */
    fun settle(queued: Set<String>) = synchronized(LOCK) {
        val all = readAll()
        if (all.none { it.status == LogEntry.Status.WAITING && it.id !in queued }) return@synchronized
        val now = System.currentTimeMillis()
        write(all.map { if (it.status == LogEntry.Status.WAITING && it.id !in queued) it.copy(status = LogEntry.Status.SENT, sentAt = now) else it })
    }

    /** Newest first. */
    fun recent(limit: Int = MAX): List<LogEntry> = synchronized(LOCK) { readAll().asReversed().take(limit) }

    fun clear() = synchronized(LOCK) { file.delete(); Unit }

    private fun readAll(): List<LogEntry> {
        if (!file.exists()) return emptyList()
        return distinct(file.readLines().mapNotNull { runCatching { LogEntry.fromJson(JSONObject(it)) }.getOrNull() })
    }

    private fun write(items: List<LogEntry>) {
        val tmp = File(file.parentFile, "activity.jsonl.tmp")
        tmp.writeText(items.joinToString("\n") { it.toJson().toString() })
        if (!tmp.renameTo(file)) { file.delete(); tmp.renameTo(file) }
    }

    companion object {
        private val LOCK = Any()

        /**
         * One entry per message: copies (the same SMS imported again before repeats were detected) share
         * the received time, bank, direction and amount. The sent copy wins, then the waiting one.
         */
        fun distinct(entries: List<LogEntry>): List<LogEntry> {
            val rank = mapOf(LogEntry.Status.SENT to 0, LogEntry.Status.WAITING to 1, LogEntry.Status.SKIPPED to 2)
            val best = entries.groupBy { listOf(it.receivedAt, it.bank, it.direction, it.amount) }
                .mapValues { (_, copies) -> copies.minBy { rank.getValue(it.status) }.id }
            val keep = best.values.toSet()
            return entries.filter { it.id in keep }
        }
        const val MAX = 300
    }
}
