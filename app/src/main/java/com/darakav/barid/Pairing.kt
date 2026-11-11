package com.darakav.barid

import org.json.JSONObject
import java.net.URI
import java.net.URLDecoder
import java.nio.charset.StandardCharsets
import java.util.Base64

/** Where and as which phone to forward messages; created by Darakav → Bank SMS → Phones. */
data class Pairing(val url: String, val key: String, val label: String) {
    val encrypted: Boolean get() = url.startsWith("https://")

    companion object {
        private const val PREFIX = "darakav-sms:1:"

        /**
         * Reads a setup code (`darakav-sms:1:<base64url JSON {u,k,n}>`) or a pairing link
         * (`darakav://pair?c=<setup code>`). Returns null for anything else.
         */
        fun parse(input: String?): Pairing? {
            var text = input?.trim() ?: return null
            if (text.startsWith("darakav://")) {
                val query = runCatching { URI(text).rawQuery }.getOrNull() ?: return null
                val code = query.split('&').firstOrNull { it.startsWith("c=") }?.substring(2) ?: return null
                text = URLDecoder.decode(code, StandardCharsets.UTF_8.name())
            }
            if (!text.startsWith(PREFIX)) return null
            val json = runCatching {
                String(Base64.getUrlDecoder().decode(text.substring(PREFIX.length)), StandardCharsets.UTF_8)
            }.getOrNull() ?: return null
            val obj = runCatching { JSONObject(json) }.getOrNull() ?: return null
            val url = obj.optString("u", "")
            val key = obj.optString("k", "")
            val label = obj.optString("n", "")
            if (!(url.startsWith("https://") || url.startsWith("http://"))) return null
            if (!url.endsWith("/api/sms/ingest")) return null
            if (!key.startsWith("dkv_") || key.length !in 40..80) return null
            return Pairing(url, key, label.take(64))
        }
    }
}
