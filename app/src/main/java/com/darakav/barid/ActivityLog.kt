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

    /** Newest first. */
    fun recent(limit: Int = MAX): List<LogEntry> = synchronized(LOCK) { readAll().asReversed().take(limit) }

    fun clear() = synchronized(LOCK) { file.delete(); Unit }

    private fun readAll(): List<LogEntry> {
        if (!file.exists()) return emptyList()
        return file.readLines().mapNotNull { runCatching { LogEntry.fromJson(JSONObject(it)) }.getOrNull() }
    }

    private fun write(items: List<LogEntry>) {
        val tmp = File(file.parentFile, "activity.jsonl.tmp")
        tmp.writeText(items.joinToString("\n") { it.toJson().toString() })
        if (!tmp.renameTo(file)) { file.delete(); tmp.renameTo(file) }
    }

    companion object {
        private val LOCK = Any()
        const val MAX = 300
    }
}
