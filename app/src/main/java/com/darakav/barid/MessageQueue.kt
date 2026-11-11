package com.darakav.barid

import android.content.Context
import org.json.JSONObject
import java.io.File
import java.util.UUID

/** One message waiting to be delivered. */
data class Queued(val id: String, val from: String, val text: String, val receivedStamp: Long) {
    fun toJson(): JSONObject = JSONObject()
        .put("id", id).put("from", from).put("text", text).put("receivedStamp", receivedStamp)

    companion object {
        fun fromJson(o: JSONObject) =
            Queued(o.getString("id"), o.optString("from", ""), o.getString("text"), o.optLong("receivedStamp", 0))
    }
}

/**
 * Messages not yet delivered, one JSON object per line in app-private storage, so nothing is lost
 * while offline or after a restart. The server ignores repeats, so a retried batch is harmless.
 */
class MessageQueue(context: Context) {
    private val file = File(context.applicationContext.filesDir, "queue.jsonl")
    private val seen = Seen(context)

    /** Returns the new item's ID, or null for a repeat: already waiting, or forwarded before ([Seen]). */
    fun add(from: String, text: String, receivedStamp: Long): String? = synchronized(LOCK) {
        val items = readAll()
        val key = Seen.key(from, text)
        if (items.any { Seen.key(it.from, it.text) == key }) return null
        if (!seen.firstTime(from, text)) return null
        val id = UUID.randomUUID().toString()
        // Bounded: an offline phone keeps the newest messages.
        write((items + Queued(id, from, text, receivedStamp)).takeLast(MAX))
        id
    }

    fun peek(limit: Int): List<Queued> = synchronized(LOCK) { readAll().take(limit) }

    fun remove(ids: Collection<String>) {
        synchronized(LOCK) { write(readAll().filterNot { it.id in ids }) }
    }

    fun size(): Int = synchronized(LOCK) { readAll().size }

    fun ids(): Set<String> = synchronized(LOCK) { readAll().mapTo(HashSet()) { it.id } }

    fun clear() {
        synchronized(LOCK) { file.delete() }
    }

    private fun readAll(): List<Queued> {
        if (!file.exists()) return emptyList()
        return file.readLines().mapNotNull { line ->
            runCatching { Queued.fromJson(JSONObject(line)) }.getOrNull()
        }
    }

    private fun write(items: List<Queued>) {
        val tmp = File(file.parentFile, "queue.jsonl.tmp")
        tmp.writeText(items.joinToString("\n") { it.toJson().toString() })
        if (!tmp.renameTo(file)) { file.delete(); tmp.renameTo(file) }
    }

    companion object {
        private val LOCK = Any()
        const val MAX = 2000
    }
}
