package com.darakav.barid.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextDirection
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.darakav.barid.LogEntry
import com.darakav.barid.R
import com.darakav.barid.SmsSummary
import com.darakav.barid.ui.Format
import com.darakav.barid.ui.Snapshot
import com.darakav.barid.ui.components.BaridIcons
import com.darakav.barid.ui.components.BaridMark
import com.darakav.barid.ui.components.BrandTile
import com.darakav.barid.ui.components.DarakavMark
import com.darakav.barid.ui.components.Divider
import com.darakav.barid.ui.components.GlassCard
import com.darakav.barid.ui.components.GlassIconButton
import com.darakav.barid.ui.components.IconTile
import com.darakav.barid.ui.components.PrimaryButton
import com.darakav.barid.ui.components.QuietButton
import com.darakav.barid.ui.components.SecondaryButton
import com.darakav.barid.ui.components.StatusPill
import com.darakav.barid.ui.components.Tone
import com.darakav.barid.ui.theme.Barid

/** Home: is it working, did it send, is anything stuck. */
@Composable
fun HomeScreen(
    state: Snapshot,
    now: Long,
    xiaomi: Boolean,
    importing: Boolean,
    testing: Boolean,
    onSettings: () -> Unit,
    onSeeAll: () -> Unit,
    onAllowSms: () -> Unit,
    onBattery: () -> Unit,
    onAutostart: () -> Unit,
    onReconnect: () -> Unit,
    onImport: () -> Unit,
    onTest: () -> Unit,
) {
    val c = Barid.colors
    LazyColumn(
        Modifier.fillMaxSize(),
        contentPadding = PaddingValues(start = 16.dp, end = 16.dp, bottom = 24.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        item {
            Row(Modifier.fillMaxWidth().statusBarsPadding().padding(top = 8.dp, bottom = 2.dp), verticalAlignment = Alignment.CenterVertically) {
                BrandTile(BaridMark, 36.dp, corner = 11.dp)
                Text(
                    stringResource(R.string.app_name), style = MaterialTheme.typography.titleLarge, color = c.text,
                    modifier = Modifier.weight(1f).padding(horizontal = 10.dp),
                )
                GlassIconButton(BaridIcons.Settings, stringResource(R.string.cd_settings), onSettings)
            }
        }
        item { Hero(state, now) }
        if (state.rejected) item {
            Notice(BaridIcons.Power, c.danger, c.dangerSurface, stringResource(R.string.card_rejected_title), stringResource(R.string.card_rejected_body)) {
                PrimaryButton(stringResource(R.string.action_connect_again), onReconnect, Modifier.fillMaxWidth())
            }
        } else if (!state.smsGranted) item {
            Notice(BaridIcons.Message, c.accent, c.accentSoft, stringResource(R.string.card_sms_title), stringResource(R.string.card_sms_body)) {
                PrimaryButton(stringResource(R.string.action_allow_sms), onAllowSms, Modifier.fillMaxWidth())
            }
        }
        if (!state.batteryOk && !state.rejected) item {
            Notice(BaridIcons.Battery, c.warning, c.warningSurface, stringResource(R.string.card_battery_title), stringResource(R.string.card_battery_body)) {
                Row(horizontalArrangement = Arrangement.spacedBy(4.dp), modifier = Modifier.padding(start = 36.dp)) {
                    QuietButton(stringResource(R.string.action_allow), onBattery)
                    if (xiaomi) QuietButton(stringResource(R.string.action_autostart), onAutostart)
                }
            }
        }
        item {
            Row(Modifier.fillMaxWidth().padding(top = 4.dp, start = 4.dp, end = 4.dp), verticalAlignment = Alignment.CenterVertically) {
                Text(stringResource(R.string.recent), style = MaterialTheme.typography.titleSmall, color = c.text, modifier = Modifier.weight(1f))
                if (state.recent.size > 5) QuietButton(stringResource(R.string.see_all), onSeeAll, Modifier.heightIn(max = 32.dp))
            }
        }
        item {
            GlassCard(Modifier.fillMaxWidth()) {
                if (state.recent.isEmpty()) {
                    Column(Modifier.fillMaxWidth().padding(20.dp), horizontalAlignment = Alignment.CenterHorizontally) {
                        IconTile(BaridIcons.Message, c.muted, c.line)
                        Spacer(Modifier.height(10.dp))
                        Text(stringResource(R.string.recent_empty_title), style = MaterialTheme.typography.titleSmall, color = c.text)
                        Text(
                            stringResource(R.string.recent_empty_body), style = MaterialTheme.typography.bodySmall, color = c.muted,
                            textAlign = androidx.compose.ui.text.style.TextAlign.Center,
                        )
                    }
                } else {
                    state.recent.take(5).forEachIndexed { i, entry ->
                        if (i > 0) Divider()
                        EntryRow(entry)
                    }
                }
            }
        }
        item {
            Row(Modifier.fillMaxWidth().navigationBarsPadding(), horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                SecondaryButton(stringResource(R.string.action_import), onImport, Modifier.weight(1f), BaridIcons.Download, enabled = !state.rejected, busy = importing)
                SecondaryButton(stringResource(R.string.action_test), onTest, Modifier.weight(1f), BaridIcons.Pulse, enabled = !state.rejected, busy = testing)
            }
        }
    }
}

@Composable
private fun Hero(state: Snapshot, now: Long) {
    val c = Barid.colors
    val locale = Format.locale()
    GlassCard(Modifier.fillMaxWidth(), shape = RoundedCornerShape(22.dp)) {
        Column(Modifier.padding(18.dp)) {
            when {
                state.rejected -> StatusPill(stringResource(R.string.status_stopped), Tone.DANGER)
                !state.smsGranted -> StatusPill(stringResource(R.string.status_needs_sms), Tone.WAIT)
                state.queueSize > 0 && state.lastError != null -> StatusPill(stringResource(R.string.status_waiting_network), Tone.WAIT)
                else -> StatusPill(stringResource(R.string.status_forwarding), Tone.OK)
            }
            Spacer(Modifier.height(12.dp))
            val server = if (state.isDarakav) stringResource(R.string.darakav) else state.host
            Text(stringResource(R.string.connected_to, server), style = MaterialTheme.typography.headlineSmall, color = c.text)
            Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.padding(top = 2.dp)) {
                if (state.isDarakav) {
                    BrandTile(DarakavMark, 16.dp, corner = 4.dp, markScale = .9f)
                    Spacer(Modifier.width(6.dp))
                }
                Text(
                    listOf(state.host, state.pairing?.label.orEmpty()).filter { it.isNotBlank() }.joinToString(" · "),
                    style = MaterialTheme.typography.bodySmall, color = c.muted, maxLines = 1, overflow = TextOverflow.Ellipsis,
                )
            }
            Spacer(Modifier.height(14.dp))
            Box(Modifier.fillMaxWidth().height(1.dp).background(c.line))
            Row(Modifier.fillMaxWidth().padding(top = 12.dp)) {
                Stat(Format.number(state.sentToday.toLong(), locale), stringResource(R.string.stat_sent_today), Modifier.weight(1f), first = true)
                Stat(Format.number(state.queueSize.toLong(), locale), stringResource(R.string.stat_waiting), Modifier.weight(1f))
                Stat(Format.ago(state.lastSentAt, now), stringResource(R.string.stat_last_sent), Modifier.weight(1f))
            }
        }
    }
}

@Composable
private fun Stat(value: String, label: String, modifier: Modifier, first: Boolean = false) {
    val c = Barid.colors
    Row(modifier) {
        if (!first) Box(Modifier.width(1.dp).height(44.dp).background(c.line))
        Column(Modifier.padding(start = if (first) 0.dp else 12.dp)) {
            Text(value, style = MaterialTheme.typography.titleLarge.copy(fontSize = MaterialTheme.typography.titleLarge.fontSize * 1.1f), color = c.text, maxLines = 1)
            Text(label, style = MaterialTheme.typography.labelSmall, color = c.muted, maxLines = 1, overflow = TextOverflow.Ellipsis)
        }
    }
}

@Composable
private fun Notice(icon: ImageVector, tint: Color, background: Color, title: String, body: String, actions: @Composable () -> Unit) {
    val c = Barid.colors
    GlassCard(Modifier.fillMaxWidth()) {
        Column(Modifier.padding(14.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
            Row(verticalAlignment = Alignment.Top) {
                IconTile(icon, tint, background, Modifier.size(32.dp))
                Column(Modifier.padding(start = 12.dp)) {
                    Text(title, style = MaterialTheme.typography.titleSmall, color = c.text)
                    Text(body, style = MaterialTheme.typography.bodySmall, color = c.muted)
                }
            }
            actions()
        }
    }
}

/** One forwarded message: bank badge, kind and time with status, signed amount. */
@Composable
fun EntryRow(entry: LogEntry) {
    val c = Barid.colors
    val locale = Format.locale()
    val bank = Format.bank(entry.bank)
    val kind = when (entry.direction) {
        SmsSummary.Direction.IN -> stringResource(R.string.kind_deposit)
        SmsSummary.Direction.OUT -> stringResource(R.string.kind_withdrawal)
        null -> stringResource(R.string.kind_bank_message)
    }
    val title = Format.bankName(bank, locale)?.let { "$it · $kind" } ?: kind
    Row(Modifier.fillMaxWidth().padding(horizontal = 14.dp, vertical = 11.dp), verticalAlignment = Alignment.CenterVertically) {
        Box(
            Modifier.size(36.dp).clip(RoundedCornerShape(11.dp))
                .background(if (bank != null) Format.bankColor(bank.code, c.dark) else c.line),
            contentAlignment = Alignment.Center,
        ) {
            if (bank != null) {
                val badge = Format.bankBadge(bank, locale)
                val size = when { badge.length <= 3 -> 11.5f; badge.length <= 5 -> 10f; else -> 8.5f }
                Text(badge, style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Bold, fontSize = size.sp, lineHeight = (size + 2).sp), color = Color.White, maxLines = 1, softWrap = false)
            }
            else androidx.compose.material3.Icon(BaridIcons.Bank, null, Modifier.size(17.dp), tint = c.muted)
        }
        Column(Modifier.weight(1f).padding(horizontal = 12.dp)) {
            Text(title, style = MaterialTheme.typography.titleSmall, color = c.text, maxLines = 1, overflow = TextOverflow.Ellipsis)
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text(Format.time(entry.receivedAt, locale) + " · ", style = MaterialTheme.typography.labelSmall, color = c.muted)
                when (entry.status) {
                    LogEntry.Status.SENT -> Text(stringResource(R.string.entry_sent), style = MaterialTheme.typography.labelSmall, color = c.muted)
                    LogEntry.Status.WAITING -> StatusPill(stringResource(R.string.entry_waiting), Tone.WAIT, dot = false)
                    LogEntry.Status.SKIPPED -> StatusPill(stringResource(R.string.entry_skipped), Tone.NEUTRAL, dot = false)
                }
            }
        }
        if (entry.amount != null) {
            Column(horizontalAlignment = Alignment.End) {
                val sign = when (entry.direction) { SmsSummary.Direction.IN -> "+"; SmsSummary.Direction.OUT -> "−"; null -> "" }
                Text(
                    sign + Format.number(entry.amount, locale),
                    style = MaterialTheme.typography.titleSmall.copy(textDirection = TextDirection.Ltr),
                    color = if (entry.direction == SmsSummary.Direction.IN) c.success else c.text,
                )
                Text(
                    stringResource(if (entry.toman) R.string.unit_toman else R.string.unit_rial),
                    style = MaterialTheme.typography.labelSmall, color = c.muted,
                )
            }
        }
    }
}
