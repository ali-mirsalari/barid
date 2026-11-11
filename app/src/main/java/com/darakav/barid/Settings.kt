package com.darakav.barid

import android.content.Context
import androidx.core.content.edit

/** App-private settings. The phone key stays in this app's storage and is excluded from backups. */
class Settings(context: Context) {
    private val prefs = context.applicationContext.getSharedPreferences("darakav-sms", Context.MODE_PRIVATE)

    var pairing: Pairing?
        get() {
            val url = prefs.getString("url", null) ?: return null
            val key = prefs.getString("key", null) ?: return null
            return Pairing(url, key, prefs.getString("label", "") ?: "")
        }
        set(value) = prefs.edit {
            if (value == null) {
                remove("url"); remove("key"); remove("label")
            } else {
                putString("url", value.url); putString("key", value.key); putString("label", value.label)
            }
            remove("rejected"); remove("lastError")
        }

    /** The server refused the key (removed in Darakav); forwarding stops until the phone is paired again. */
    var rejected: Boolean
        get() = prefs.getBoolean("rejected", false)
        set(value) = prefs.edit { putBoolean("rejected", value) }

    var lastSentAt: Long
        get() = prefs.getLong("lastSentAt", 0)
        set(value) = prefs.edit { putLong("lastSentAt", value) }

    var sentCount: Int
        get() = prefs.getInt("sentCount", 0)
        set(value) = prefs.edit { putInt("sentCount", value) }

    /** Short, content-free description of the last problem (HTTP status or error type). */
    var lastError: String?
        get() = prefs.getString("lastError", null)
        set(value) = prefs.edit { if (value == null) remove("lastError") else putString("lastError", value) }
}
