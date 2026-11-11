package com.darakav.barid

import org.json.JSONArray
import org.json.JSONObject
import java.time.Instant
import java.time.ZoneId
import java.time.format.DateTimeFormatter

/** Request bodies (docs/SERVERS.md): Barid's JSON batch, or a custom template filled per message. */
object Payload {
    const val DEFAULT_TEMPLATE = "{\"from\":\"{{from}}\",\"text\":\"{{text}}\",\"receivedAt\":\"{{receivedAt}}\"}"

    fun batch(items: List<Queued>): String = JSONObject().put(
        "messages",
        JSONArray().apply {
            items.forEach { put(JSONObject().put("from", it.from).put("text", it.text).put("receivedStamp", it.receivedStamp)) }
        },
    ).toString()

    /** A template that starts with `{` or `[` (not a `{{placeholder}}`) is JSON: values are escaped as JSON string content. */
    fun isJson(template: String): Boolean = template.trimStart().let { (it.startsWith("{") && !it.startsWith("{{")) || it.startsWith("[") }

    fun contentType(template: String): String =
        if (template.isEmpty() || isJson(template)) "application/json; charset=utf-8" else "text/plain; charset=utf-8"

    fun fill(template: String, item: Queued, zone: ZoneId = ZoneId.systemDefault()): String {
        val json = isJson(template)
        fun value(raw: String) = if (json) JSONObject.quote(raw).let { it.substring(1, it.length - 1) } else raw
        val receivedAt = DateTimeFormatter.ISO_OFFSET_DATE_TIME.format(Instant.ofEpochMilli(item.receivedStamp).atZone(zone))
        val values = mapOf(
            "from" to value(item.from),
            "text" to value(item.text),
            "receivedStamp" to item.receivedStamp.toString(),
            "receivedAt" to receivedAt,
        )
        // One pass, so placeholders inside message text are never expanded.
        return PLACEHOLDER.replace(template) { m -> values[m.groupValues[1]] ?: m.value }
    }

    private val PLACEHOLDER = Regex("\\{\\{(from|text|receivedStamp|receivedAt)}}")
}
