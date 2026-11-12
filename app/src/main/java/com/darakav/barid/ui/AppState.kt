package com.darakav.barid.ui

import android.Manifest
import android.content.Context
import android.content.pm.PackageManager
import android.os.PowerManager
import androidx.compose.runtime.Composable
import androidx.compose.runtime.Immutable
import androidx.compose.runtime.ReadOnlyComposable
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.res.stringResource
import androidx.core.content.ContextCompat
import com.darakav.barid.ActivityLog
import com.darakav.barid.BuildConfig
import com.darakav.barid.Bank
import com.darakav.barid.Banks
import com.darakav.barid.LogEntry
import com.darakav.barid.MessageQueue
import com.darakav.barid.Pairing
import com.darakav.barid.R
import com.darakav.barid.Settings
import com.darakav.barid.Updates
import java.text.NumberFormat
import java.util.Calendar
import java.util.Locale

/** Everything the screens show, read fresh from storage (cheap: small files and preferences). */
@Immutable
data class Snapshot(
    val pairing: Pairing?,
    val rejected: Boolean,
    val queueSize: Int,
    val lastSentAt: Long,
    val lastError: String?,
    val lastAttemptAt: Long,
    val recent: List<LogEntry>,
    val sentToday: Int,
    val smsGranted: Boolean,
    val batteryOk: Boolean,
    val update: UpdateState = UpdateState(),
) {
    val host: String get() = pairing?.host.orEmpty()
    val isDarakav: Boolean get() = pairing?.trusted == true

    companion object {
        val Empty = Snapshot(null, false, 0, 0, null, 0, emptyList(), 0, false, true)

        fun load(context: Context): Snapshot {
            val settings = Settings(context)
            val queue = MessageQueue(context)
            val log = ActivityLog(context)
            val queued = queue.ids()
            log.settle(queued)
            val recent = log.recent()
            val startOfDay = Calendar.getInstance().apply {
                set(Calendar.HOUR_OF_DAY, 0); set(Calendar.MINUTE, 0); set(Calendar.SECOND, 0); set(Calendar.MILLISECOND, 0)
            }.timeInMillis
            val power = context.getSystemService(PowerManager::class.java)
            return Snapshot(
                pairing = settings.pairing,
                rejected = settings.rejected,
                queueSize = queued.size,
                lastSentAt = settings.lastSentAt,
                lastError = settings.lastError,
                lastAttemptAt = settings.lastAttemptAt,
                recent = recent,
                sentToday = recent.count { it.status == LogEntry.Status.SENT && it.sentAt >= startOfDay },
                smsGranted = granted(context, Manifest.permission.RECEIVE_SMS),
                batteryOk = power?.isIgnoringBatteryOptimizations(context.packageName) ?: true,
                update = UpdateState(
                    store = Updates.store(context),
                    auto = settings.autoUpdate,
                    // Not offered again once installed (the next check also forgets it).
                    ready = settings.updateReady?.takeIf { Updates.newer(it, BuildConfig.VERSION_NAME) },
                    checkedAt = settings.updateCheckedAt,
                    failed = settings.updateError != null,
                    installAllowed = Updates.installAllowed(context),
                    notificationsAllowed = Updates.notificationsAllowed(context),
                    promptHidden = settings.updatePromptHidden,
                ),
            )
        }

        fun granted(context: Context, permission: String) =
            ContextCompat.checkSelfPermission(context, permission) == PackageManager.PERMISSION_GRANTED
    }
}

/** Updates as the screens show them; [store] set means a store updates Barid instead. */
@Immutable
data class UpdateState(
    val store: String? = null,
    val auto: Boolean = true,
    val ready: String? = null,
    val checkedAt: Long = 0,
    val failed: Boolean = false,
    val installAllowed: Boolean = true,
    val notificationsAllowed: Boolean = true,
    val promptHidden: Boolean = false,
) {
    val active: Boolean get() = store == null && auto
    /** Something the owner should allow so updates reach them without opening Barid. */
    val needsPermission: Boolean get() = active && (!installAllowed || !notificationsAllowed)
}

/** Locale-aware display helpers: Persian digits and the Jalali calendar in Persian. */
object Format {
    @Composable
    @ReadOnlyComposable
    fun locale(): Locale = LocalConfiguration.current.locales[0]

    fun isPersian(locale: Locale) = locale.language == "fa"

    // Formatters are costly to create (ICU loads locale data), and lists format a value per row, so
    // one of each is kept per locale. Formatters are not thread-safe: every use holds the cache lock.
    private val formatters = HashMap<String, Any>()

    @Suppress("UNCHECKED_CAST")
    private fun <T : Any> cached(kind: String, locale: Locale, create: () -> T, use: (T) -> String): String = synchronized(formatters) {
        use(formatters.getOrPut(kind + "|" + locale.toLanguageTag(), create) as T)
    }

    fun number(value: Long, locale: Locale): String =
        cached("number", locale, { NumberFormat.getIntegerInstance(locale) }) { it.format(value) }

    @android.annotation.SuppressLint("SimpleDateFormat") // explicit 24-hour pattern; digits follow the locale
    fun time(stamp: Long, locale: Locale): String = cached(
        "time", locale, { android.icu.text.SimpleDateFormat("HH:mm", android.icu.util.ULocale.forLocale(locale)) },
    ) { it.format(stamp) }

    @android.annotation.SuppressLint("SimpleDateFormat") // Jalali calendar in Persian needs an explicit ICU pattern
    fun day(stamp: Long, locale: Locale): String = cached("day", locale, {
        val uLocale = if (isPersian(locale)) android.icu.util.ULocale("fa_IR@calendar=persian") else android.icu.util.ULocale.forLocale(locale)
        android.icu.text.SimpleDateFormat(if (isPersian(locale)) "EEEE d MMMM" else "EEEE, d MMMM", uLocale)
    }) { it.format(stamp) }

    fun dayIndex(stamp: Long): Long {
        val c = Calendar.getInstance().apply { timeInMillis = stamp }
        return c.get(Calendar.YEAR) * 400L + c.get(Calendar.DAY_OF_YEAR)
    }

    /** "now", "5m", "3h", "2d" (localized); "—" when it never happened. */
    @Composable
    fun ago(stamp: Long, now: Long): String {
        if (stamp <= 0) return "—"
        val locale = locale()
        val minutes = ((now - stamp) / 60_000).coerceAtLeast(0)
        return when {
            minutes < 1 -> stringResource(R.string.rel_now)
            minutes < 60 -> stringResource(R.string.rel_minutes, number(minutes, locale))
            minutes < 60 * 24 -> stringResource(R.string.rel_hours, number(minutes / 60, locale))
            else -> stringResource(R.string.rel_days, number(minutes / (60 * 24), locale))
        }
    }

    /** Plain words for a delivery problem recorded as an HTTP status or an exception name. */
    @Composable
    fun reason(raw: String): String {
        val code = raw.removePrefix("HTTP ").toIntOrNull()
        return stringResource(
            when {
                code == 429 -> R.string.reason_busy
                code != null && code >= 500 -> R.string.reason_server
                code != null && code in 300..399 -> R.string.reason_moved
                code != null -> R.string.reason_refused
                raw.startsWith("SSL") -> R.string.reason_tls
                raw.contains("Timeout") -> R.string.reason_slow
                raw.contains("UnknownHost") || raw.contains("Connect") || raw.contains("NoRoute") -> R.string.reason_offline
                else -> R.string.reason_other
            },
        )
    }

    fun bankName(bank: Bank?, locale: Locale): String? = bank?.let { if (isPersian(locale)) it.fa else it.en }

    /** Badge text: the bank's first Persian word ("ملت", "پاسارگاد") or a unique two-letter code ("MT", "PA"). */
    fun bankBadge(bank: Bank?, locale: Locale): String {
        if (bank == null) return ""
        return if (isPersian(locale)) bank.fa.split(' ').first() else bank.abbr
    }

    /** Keeps Latin text such as a key hint in reading order inside Persian sentences. */
    fun ltr(text: String): String = LRI + text + PDI

    // Unicode left-to-right isolate and pop directional isolate, built from code points so no control
    // characters sit in the source.
    private val LRI = Char(0x2066).toString()
    private val PDI = Char(0x2069).toString()

    /** A calm, stable color per bank (not the bank's brand color). */
    fun bankColor(code: String?, dark: Boolean): Color {
        val hue = ((code ?: "").fold(7) { h, ch -> h * 31 + ch.code } and 0x7fffffff) % 360
        return Color.hsl(hue.toFloat(), if (dark) .42f else .55f, if (dark) .40f else .42f)
    }

    fun bank(code: String?): Bank? = Banks.byCode(code)
}
