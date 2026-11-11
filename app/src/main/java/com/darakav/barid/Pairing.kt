package com.darakav.barid

import org.json.JSONObject
import java.net.URI
import java.net.URLDecoder
import java.net.URLEncoder
import java.nio.charset.StandardCharsets
import java.util.Base64

/**
 * Where and how to forward messages. Created by scanning a setup code (Darakav's or any server's, see
 * docs/SERVERS.md) or by filling in "Use another server".
 *
 * @property key secret sent with every request; empty for servers without one.
 * @property header header that carries [key]; empty means `Authorization: Bearer <key>`.
 * @property template body for one message per request with `{{from}}`, `{{text}}`, `{{receivedStamp}}`
 *   and `{{receivedAt}}`; empty means Barid's JSON batch.
 * @property darakav paired from a Darakav setup code (Darakav's checks and responses apply).
 */
data class Pairing(
    val url: String,
    val key: String,
    val label: String,
    val header: String = "",
    val template: String = "",
    val darakav: Boolean = false,
) {
    val encrypted: Boolean get() = url.startsWith("https://")
    val host: String get() = runCatching { URI(url).host }.getOrNull().orEmpty()

    /** Darakav's own server over HTTPS: the only destination connected without asking first. */
    val trusted: Boolean get() = darakav && encrypted && (host == "darakav.com" || host.endsWith(".darakav.com"))

    /** The header name and value carrying the key, or null when there is no key. */
    val auth: Pair<String, String>?
        get() = when {
            key.isEmpty() -> null
            header.isEmpty() -> "Authorization" to "Bearer $key"
            else -> header to key
        }

    /** Barid's public setup code for this pairing (`barid:1:…`). */
    fun encode(): String {
        val obj = JSONObject().put("u", url)
        if (key.isNotEmpty()) obj.put("k", key)
        if (label.isNotEmpty()) obj.put("n", label)
        if (header.isNotEmpty()) obj.put("h", header)
        if (template.isNotEmpty()) obj.put("t", template)
        return BARID_PREFIX + Base64.getUrlEncoder().withoutPadding().encodeToString(obj.toString().toByteArray(StandardCharsets.UTF_8))
    }

    /** `barid://pair?c=<setup code>` for this pairing. */
    fun link(): String = "barid://pair?c=" + URLEncoder.encode(encode(), StandardCharsets.UTF_8.name())

    /** The first invalid field of a custom pairing. */
    enum class Field { URL, KEY, HEADER, TEMPLATE }

    companion object {
        private const val DARAKAV_PREFIX = "darakav-sms:1:"
        private const val BARID_PREFIX = "barid:1:"
        private val HEADER_NAME = Regex("[A-Za-z0-9!#$%&'*+.^_`|~-]{1,64}")
        private val FORBIDDEN_HEADERS = setOf("host", "content-length", "content-type", "connection", "transfer-encoding", "user-agent")
        const val MAX_TEMPLATE = 4000

        /**
         * Reads a setup code (`darakav-sms:1:…` or `barid:1:…`, base64url JSON) or a pairing link
         * (`darakav://pair?c=…`, `barid://pair?c=…`). Returns null for anything else.
         */
        fun parse(input: String?): Pairing? {
            var text = input?.trim() ?: return null
            if (text.startsWith("darakav://") || text.startsWith("barid://")) {
                val query = runCatching { URI(text).rawQuery }.getOrNull() ?: return null
                val code = query.split('&').firstOrNull { it.startsWith("c=") }?.substring(2) ?: return null
                text = runCatching { URLDecoder.decode(code, StandardCharsets.UTF_8.name()) }.getOrNull()?.trim() ?: return null
            }
            val darakav = text.startsWith(DARAKAV_PREFIX)
            if (!darakav && !text.startsWith(BARID_PREFIX)) return null
            val obj = runCatching {
                val payload = text.substring(if (darakav) DARAKAV_PREFIX.length else BARID_PREFIX.length)
                JSONObject(String(Base64.getUrlDecoder().decode(payload), StandardCharsets.UTF_8))
            }.getOrNull() ?: return null
            val url = obj.optString("u", "")
            val key = obj.optString("k", "")
            val label = obj.optString("n", "").take(64)
            if (darakav) {
                if (!(url.startsWith("https://") || url.startsWith("http://"))) return null
                if (!url.endsWith("/api/sms/ingest")) return null
                if (!key.startsWith("dkv_") || key.length !in 40..80) return null
                return Pairing(url, key, label, darakav = true)
            }
            return custom(url, key, label, obj.optString("h", ""), obj.optString("t", ""))
        }

        /** A pairing with any server, or null when [problem] finds an invalid field. */
        fun custom(url: String, key: String, label: String, header: String, template: String): Pairing? {
            val u = url.trim()
            val k = key.trim()
            val h = header.trim()
            if (problem(u, k, h, template) != null) return null
            return Pairing(u, k, label.trim().take(64), h, template.trim())
        }

        fun problem(url: String, key: String, header: String, template: String): Field? {
            val uri = runCatching { URI(url.trim()) }.getOrNull()
            if (uri == null || uri.scheme?.lowercase() !in setOf("http", "https") || uri.host.isNullOrEmpty() || url.length > 2048) return Field.URL
            val k = key.trim()
            if (k.length > 1024 || k.any { it.isISOControl() }) return Field.KEY
            val h = header.trim()
            if (h.isNotEmpty()) {
                if (!HEADER_NAME.matches(h) || h.lowercase() in FORBIDDEN_HEADERS) return Field.HEADER
                if (k.isEmpty()) return Field.KEY
            }
            val t = template.trim()
            if (t.isNotEmpty() && (t.length > MAX_TEMPLATE || "{{text}}" !in t)) return Field.TEMPLATE
            return null
        }
    }
}
