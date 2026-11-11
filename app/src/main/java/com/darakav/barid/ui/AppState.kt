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
import com.darakav.barid.Bank
import com.darakav.barid.Banks
import com.darakav.barid.LogEntry
import com.darakav.barid.MessageQueue
import com.darakav.barid.Pairing
import com.darakav.barid.R
import com.darakav.barid.Settings
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
    val recent: List<LogEntry>,
    val sentToday: Int,
    val smsGranted: Boolean,
    val batteryOk: Boolean,
) {
    val host: String get() = pairing?.host.orEmpty()
    val isDarakav: Boolean get() = pairing?.trusted == true

    companion object {
        val Empty = Snapshot(null, false, 0, 0, null, emptyList(), 0, false, true)

        fun load(context: Context): Snapshot {
            val settings = Settings(context)
            val recent = ActivityLog(context).recent()
            val startOfDay = Calendar.getInstance().apply {
                set(Calendar.HOUR_OF_DAY, 0); set(Calendar.MINUTE, 0); set(Calendar.SECOND, 0); set(Calendar.MILLISECOND, 0)
            }.timeInMillis
            val power = context.getSystemService(PowerManager::class.java)
            return Snapshot(
                pairing = settings.pairing,
                rejected = settings.rejected,
                queueSize = MessageQueue(context).size(),
                lastSentAt = settings.lastSentAt,
                lastError = settings.lastError,
                recent = recent,
                sentToday = recent.count { it.status == LogEntry.Status.SENT && it.sentAt >= startOfDay },
                smsGranted = granted(context, Manifest.permission.RECEIVE_SMS),
                batteryOk = power?.isIgnoringBatteryOptimizations(context.packageName) ?: true,
            )
        }

        fun granted(context: Context, permission: String) =
            ContextCompat.checkSelfPermission(context, permission) == PackageManager.PERMISSION_GRANTED
    }
}

/** Locale-aware display helpers: Persian digits and the Jalali calendar in Persian. */
object Format {
    @Composable
    @ReadOnlyComposable
    fun locale(): Locale = LocalConfiguration.current.locales[0]

    fun isPersian(locale: Locale) = locale.language == "fa"

    fun number(value: Long, locale: Locale): String = NumberFormat.getIntegerInstance(locale).format(value)

    @android.annotation.SuppressLint("SimpleDateFormat") // explicit 24-hour pattern; digits follow the locale
    fun time(stamp: Long, locale: Locale): String =
        android.icu.text.SimpleDateFormat("HH:mm", android.icu.util.ULocale.forLocale(locale)).format(stamp)

    @android.annotation.SuppressLint("SimpleDateFormat") // Jalali calendar in Persian needs an explicit ICU pattern
    fun day(stamp: Long, locale: Locale): String {
        val uLocale = if (isPersian(locale)) android.icu.util.ULocale("fa_IR@calendar=persian") else android.icu.util.ULocale.forLocale(locale)
        val pattern = if (isPersian(locale)) "EEEE d MMMM" else "EEEE, d MMMM"
        return android.icu.text.SimpleDateFormat(pattern, uLocale).format(stamp)
    }

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
