package com.darakav.barid

import android.content.Context
import androidx.core.content.edit

/** App-private settings. The phone key stays in this app's storage and is excluded from backups. */
class Settings(context: Context) {
    private val prefs = context.applicationContext.getSharedPreferences("barid", Context.MODE_PRIVATE)

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

    /** The server refused the key (phone removed there); forwarding stops until the phone is paired again. */
    var rejected: Boolean
        get() = prefs.getBoolean("rejected", false)
        set(value) = prefs.edit { putBoolean("rejected", value) }

    var lastSentAt: Long
        get() = prefs.getLong("lastSentAt", 0)
        set(value) = prefs.edit { putLong("lastSentAt", value) }

    var sentCount: Int
        get() = prefs.getInt("sentCount", 0)
        set(value) = prefs.edit { putInt("sentCount", value) }

    /** "system", "light" or "dark". */
    var themeMode: String
        get() = prefs.getString("themeMode", "system") ?: "system"
        set(value) = prefs.edit { putString("themeMode", value) }

    /** "you" (Material You, Android 12+) or one of Darakav's presets; null until chosen. */
    var palette: String?
        get() = prefs.getString("palette", null)
        set(value) = prefs.edit { putString("palette", value) }

    var reduceTransparency: Boolean
        get() = prefs.getBoolean("reduceTransparency", false)
        set(value) = prefs.edit { putBoolean("reduceTransparency", value) }

    /** Short, content-free description of the last problem (HTTP status or error type). */
    var lastError: String?
        get() = prefs.getString("lastError", null)
        set(value) = prefs.edit { if (value == null) remove("lastError") else putString("lastError", value) }
}
