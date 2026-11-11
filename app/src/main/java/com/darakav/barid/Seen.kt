package com.darakav.barid

import android.content.Context
import java.io.File
import java.security.MessageDigest

/**
 * Fingerprints of messages already queued for the current server, so the same SMS is forwarded once:
 * operators sometimes deliver a message twice, and importing again finds messages sent before. Only a
 * one-way hash of sender and normalized text is kept (never the text), for [DAYS] days.
 *
 * Two messages are the same when sender and text match after normalization, whatever their times;
 * bank messages carry their own time and balance, so identical text means a repeat. Darakav's server
 * applies the same rule.
 */
class Seen(context: Context) {
    private val file = File(context.applicationContext.filesDir, "seen.txt")

    /** Records the message and returns true the first time; false for a repeat. */
    fun firstTime(from: String, text: String, now: Long = System.currentTimeMillis()): Boolean = synchronized(LOCK) {
        val key = key(from, text)
        val entries = readAll(now)
        if (entries.containsKey(key)) return false
        entries[key] = now
        write(entries)
        true
    }

    /** Forgets one message (it was dropped before delivery, so a later copy may be sent). */
    fun forget(from: String, text: String) = synchronized(LOCK) {
        val entries = readAll(System.currentTimeMillis())
        if (entries.remove(key(from, text)) != null) write(entries)
    }

    fun clear() = synchronized(LOCK) { file.delete(); Unit }

    private fun readAll(now: Long): LinkedHashMap<String, Long> {
        val map = LinkedHashMap<String, Long>()
        if (!file.exists()) return map
        val oldest = now - DAYS * 24L * 60 * 60 * 1000
        file.forEachLine { line ->
            val space = line.indexOf(' ')
            if (space <= 0) return@forEachLine
            val at = line.substring(space + 1).toLongOrNull() ?: return@forEachLine
            if (at >= oldest) map[line.substring(0, space)] = at
        }
        return map
    }

    private fun write(entries: Map<String, Long>) {
        val tmp = File(file.parentFile, "seen.txt.tmp")
        tmp.writeText(entries.entries.toList().takeLast(MAX).joinToString("\n") { "${it.key} ${it.value}" })
        if (!tmp.renameTo(file)) { file.delete(); tmp.renameTo(file) }
    }

    companion object {
        private val LOCK = Any()
        const val DAYS = 120
        const val MAX = 5000
        private val SPACES = Regex("\\s+")

        /** Sender (case-insensitive) and text with Persian/Arabic forms and spacing unified. */
        fun key(from: String, text: String): String {
            val normalized = SmsFilter.normalize(text).replace(SPACES, " ").trim()
            val digest = MessageDigest.getInstance("SHA-256")
                .digest((from.trim().lowercase() + "\n" + normalized).toByteArray(Charsets.UTF_8))
            return digest.take(16).joinToString("") { "%02x".format(it) }
        }
    }
}
