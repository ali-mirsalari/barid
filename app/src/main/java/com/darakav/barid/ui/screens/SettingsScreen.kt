package com.darakav.barid.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalUriHandler
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.selected
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.draw.drawWithCache
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.RoundRect
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.drawscope.clipRect
import androidx.compose.ui.unit.dp
import com.darakav.barid.ui.theme.BaridColors
import com.darakav.barid.BuildConfig
import com.darakav.barid.LogEntry
import com.darakav.barid.R
import com.darakav.barid.ui.Format
import com.darakav.barid.ui.Snapshot
import com.darakav.barid.ui.components.BaridIcons
import com.darakav.barid.ui.components.BaridSwitch
import com.darakav.barid.ui.components.BrandTile
import com.darakav.barid.ui.components.DarakavMark
import com.darakav.barid.ui.components.Divider
import com.darakav.barid.ui.components.GlassCard
import com.darakav.barid.ui.components.GlassIconButton
import com.darakav.barid.ui.components.IconTile
import com.darakav.barid.ui.components.ListRow
import com.darakav.barid.ui.components.QuietButton
import com.darakav.barid.ui.components.Segmented
import com.darakav.barid.ui.theme.Barid
import com.darakav.barid.ui.theme.Palette

@Composable
fun ScreenTitle(title: String, onBack: () -> Unit) {
    Row(Modifier.fillMaxWidth().statusBarsPadding().padding(top = 8.dp, bottom = 4.dp), verticalAlignment = Alignment.CenterVertically) {
        GlassIconButton(BaridIcons.Back, stringResource(R.string.cd_back), onBack)
        Text(title, style = MaterialTheme.typography.titleLarge, color = Barid.colors.text, modifier = Modifier.padding(horizontal = 12.dp))
    }
}

@Composable
internal fun SectionLabel(text: String) {
    Text(
        text, style = MaterialTheme.typography.labelSmall, color = Barid.colors.muted,
        modifier = Modifier.padding(start = 16.dp, end = 16.dp, top = 12.dp, bottom = 2.dp),
    )
}

@Composable
private fun paletteName(p: Palette): String = stringResource(
    when (p) {
        Palette.YOU -> R.string.palette_you
        Palette.SLATE -> R.string.palette_slate
        Palette.OCEAN -> R.string.palette_ocean
        Palette.IRIS -> R.string.palette_iris
        Palette.TEAL -> R.string.palette_teal
        Palette.CLAY -> R.string.palette_clay
        Palette.FOREST -> R.string.palette_forest
    },
)

@OptIn(ExperimentalLayoutApi::class)
@Composable
fun SettingsScreen(
    state: Snapshot,
    themeMode: String,
    palette: Palette,
    reduceTransparency: Boolean,
    language: String,
    onBack: () -> Unit,
    onThemeMode: (String) -> Unit,
    onPalette: (Palette) -> Unit,
    onReduceTransparency: (Boolean) -> Unit,
    onLanguage: (String) -> Unit,
    onAbout: () -> Unit,
    onConnectOther: () -> Unit,
    onEditServer: () -> Unit,
    onDisconnect: () -> Unit,
    checkingUpdates: Boolean = false,
    onAutoUpdate: (Boolean) -> Unit = {},
    onCheckUpdates: () -> Unit = {},
    onInstallUpdate: () -> Unit = {},
) {
    val c = Barid.colors
    var confirm by remember { mutableStateOf(false) }
    LazyColumn(Modifier.fillMaxSize(), contentPadding = PaddingValues(start = 16.dp, end = 16.dp, bottom = 24.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
        item { ScreenTitle(stringResource(R.string.settings_title), onBack) }
        item {
            GlassCard(Modifier.fillMaxWidth()) {
                SectionLabel(stringResource(R.string.section_appearance))
                ListRow(stringResource(R.string.theme), trailing = {
                    Segmented(
                        listOf("system" to stringResource(R.string.option_system), "light" to stringResource(R.string.theme_light), "dark" to stringResource(R.string.theme_dark)),
                        themeMode, onThemeMode,
                    )
                })
                Divider()
                val note = if (palette == Palette.YOU) " · " + stringResource(R.string.palette_you_note) else ""
                ListRow(stringResource(R.string.color), subtitle = paletteName(palette) + note)
                FlowRow(Modifier.fillMaxWidth().padding(start = 16.dp, end = 16.dp, bottom = 14.dp), horizontalArrangement = Arrangement.spacedBy(8.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Palette.choices.forEach { p -> Swatch(p, p == palette, paletteName(p)) { onPalette(p) } }
                }
                Divider()
                ListRow(
                    stringResource(R.string.reduce_transparency), subtitle = stringResource(R.string.reduce_transparency_body),
                    onClick = { onReduceTransparency(!reduceTransparency) },
                    trailing = { BaridSwitch(reduceTransparency, onReduceTransparency) },
                )
            }
        }
        item {
            GlassCard(Modifier.fillMaxWidth()) {
                SectionLabel(stringResource(R.string.section_language))
                ListRow(stringResource(R.string.app_language), trailing = {
                    Segmented(listOf("" to stringResource(R.string.option_system), "fa" to "فارسی", "en" to "English"), language, onLanguage)
                })
            }
        }
        item {
            GlassCard(Modifier.fillMaxWidth()) {
                SectionLabel(stringResource(R.string.section_connection))
                val pairing = state.pairing
                if (pairing != null) {
                    ListRow(
                        if (state.isDarakav) stringResource(R.string.darakav) else state.host,
                        subtitle = listOf(
                            pairing.label,
                            if (pairing.key.isEmpty()) stringResource(R.string.no_key) else stringResource(R.string.key_hint, Format.ltr("••••" + pairing.key.takeLast(4))),
                        ).filter { it.isNotBlank() }.joinToString(" · "),
                        onClick = if (pairing.darakav) null else onEditServer,
                        trailing = if (pairing.darakav) null else { { Icon(BaridIcons.Forward, null, Modifier.size(16.dp), tint = c.muted) } },
                        leading = {
                            if (state.isDarakav) BrandTile(DarakavMark, 32.dp, corner = 9.dp, markScale = .9f)
                            else IconTile(BaridIcons.Server, c.accent, c.accentSoft, Modifier.size(32.dp))
                        },
                    )
                    if (!pairing.encrypted) {
                        Text(
                            stringResource(R.string.unencrypted_warning), style = MaterialTheme.typography.bodySmall, color = c.warning,
                            modifier = Modifier.padding(start = 16.dp, end = 16.dp, bottom = 8.dp),
                        )
                    }
                    Divider()
                }
                ListRow(stringResource(R.string.connect_other), onClick = onConnectOther, trailing = {
                    Icon(BaridIcons.Forward, null, Modifier.size(16.dp), tint = c.muted)
                })
                if (pairing != null) {
                    Divider()
                    ListRow(stringResource(R.string.disconnect), titleColor = c.danger, onClick = { confirm = true })
                }
            }
        }
        item {
            val update = state.update
            GlassCard(Modifier.fillMaxWidth()) {
                SectionLabel(stringResource(R.string.section_updates))
                if (update.store != null) {
                    ListRow(stringResource(R.string.updates_by_store), subtitle = stringResource(R.string.updates_by_store_body))
                } else {
                    ListRow(
                        stringResource(R.string.auto_update), subtitle = stringResource(R.string.auto_update_body),
                        onClick = { onAutoUpdate(!update.auto) },
                        trailing = { BaridSwitch(update.auto, onAutoUpdate) },
                    )
                    Divider()
                    val ready = update.ready
                    val subtitle = when {
                        checkingUpdates -> stringResource(R.string.update_checking)
                        ready != null -> stringResource(R.string.update_ready_row, Format.ltr(ready))
                        update.failed -> stringResource(R.string.update_failed_row)
                        update.checkedAt > 0 -> stringResource(R.string.update_latest_row, Format.ago(update.checkedAt, System.currentTimeMillis()))
                        else -> stringResource(R.string.version_line, Format.ltr(BuildConfig.VERSION_NAME))
                    }
                    ListRow(
                        stringResource(if (ready != null) R.string.action_install_update else R.string.check_updates), subtitle = subtitle,
                        onClick = if (checkingUpdates) null else if (ready != null) onInstallUpdate else onCheckUpdates,
                        trailing = { Icon(BaridIcons.Refresh, null, Modifier.size(16.dp), tint = c.muted) },
                    )
                }
            }
        }
        item {
            GlassCard(Modifier.fillMaxWidth().navigationBarsPadding()) {
                ListRow(
                    stringResource(R.string.about_title), subtitle = stringResource(R.string.version_line, Format.ltr(BuildConfig.VERSION_NAME)),
                    onClick = onAbout,
                    leading = { IconTile(BaridIcons.Shield, c.accent, c.accentSoft, Modifier.size(32.dp)) },
                    trailing = { Icon(BaridIcons.Forward, null, Modifier.size(16.dp), tint = c.muted) },
                )
            }
        }
    }
    if (confirm) {
        AlertDialog(
            onDismissRequest = { confirm = false },
            title = { Text(stringResource(R.string.disconnect_title)) },
            text = { Text(stringResource(R.string.disconnect_body)) },
            confirmButton = { QuietButton(stringResource(R.string.action_disconnect), { confirm = false; onDisconnect() }, color = c.danger) },
            dismissButton = { QuietButton(stringResource(R.string.action_cancel), { confirm = false }) },
            containerColor = c.surfaceSolid,
            shape = RoundedCornerShape(22.dp),
        )
    }
}

@Composable
private fun Swatch(palette: Palette, selected: Boolean, name: String, onClick: () -> Unit) {
    val c = Barid.colors
    val fill = if (palette == Palette.YOU) {
        Brush.sweepGradient(listOf(Color(0xFF3F6837), Color(0xFFB3CF8F), Color(0xFF355F80), Color(0xFFC9A46A), Color(0xFF3F6837)))
    } else {
        // The theme's two glow hues, which now tint the whole app, not only its accent.
        Brush.linearGradient(listOf(palette.tones.glowA, palette.tones.glowB))
    }
    Box(
        Modifier.size(40.dp)
            .then(if (selected) Modifier.border(2.dp, c.text, RoundedCornerShape(14.dp)).padding(4.dp) else Modifier.padding(4.dp))
            .clip(RoundedCornerShape(11.dp)).background(fill)
            .clickable(role = Role.RadioButton, onClick = onClick)
            .semantics { contentDescription = name; this.selected = selected },
        contentAlignment = Alignment.Center,
    ) {
        if (selected) Icon(BaridIcons.Check, null, Modifier.size(16.dp), tint = Color.White)
    }
}

/** All forwarded messages, grouped by day (Jalali in Persian). */
@Composable
fun ActivityScreen(entries: List<LogEntry>, onBack: () -> Unit) {
    val locale = Format.locale()
    // Flat rows so each message is its own lazy item: an imported day can hold dozens.
    val rows = remember(entries) {
        entries.groupBy { Format.dayIndex(it.receivedAt) }.values.flatMap { day ->
            listOf<Any>(day.first().receivedAt) + day.mapIndexed { i, e -> Segment(e, first = i == 0, last = i == day.lastIndex) }
        }
    }
    LazyColumn(Modifier.fillMaxSize(), contentPadding = PaddingValues(start = 16.dp, end = 16.dp, bottom = 24.dp)) {
        item { ScreenTitle(stringResource(R.string.activity_title), onBack) }
        items(rows, key = { if (it is Segment) it.entry.id else "day-$it" }, contentType = { if (it is Segment) "entry" else "day" }) { row ->
            if (row is Segment) {
                Box(Modifier.fillMaxWidth().cardSegment(Barid.colors, row.first, row.last)) {
                    Column {
                        if (!row.first) Divider()
                        EntryRow(row.entry)
                    }
                }
            } else {
                Text(
                    Format.day(row as Long, locale), style = MaterialTheme.typography.labelMedium, color = Barid.colors.muted,
                    modifier = Modifier.padding(start = 4.dp, top = 16.dp, bottom = 6.dp),
                )
            }
        }
    }
}

private class Segment(val entry: LogEntry, val first: Boolean, val last: Boolean)

/**
 * One row of a card split into lazy rows: the card's rounded outline, tint and bright top edge, cut to
 * this row (a taller rounded rectangle, clipped), so the rows read as one glass card. No live blur.
 */
private fun Modifier.cardSegment(c: BaridColors, first: Boolean, last: Boolean): Modifier = this.drawWithCache {
    val radius = 18.dp.toPx()
    val over = radius * 2
    val top = if (first) 0f else -over
    val bottom = if (last) size.height else size.height + over
    val stroke = 1.dp.toPx()
    val outline = RoundRect(stroke / 2, top + stroke / 2, size.width - stroke / 2, bottom - stroke / 2, CornerRadius(radius))
    val path = Path().apply { addRoundRect(outline) }
    val fill = if (c.solid) c.surfaceSolid else c.surface
    val edge = if (first) Brush.verticalGradient(0f to c.glassEdge, .5f to c.line, startY = 0f, endY = radius * 2) else SolidColor(c.line)
    onDrawBehind {
        clipRect {
            drawPath(path, fill)
            drawPath(path, edge, style = Stroke(stroke))
        }
    }
}

private const val SOURCE_URL = "https://github.com/ali-mirsalari/barid"

/** What Barid sends and keeps, the license and the third-party notices. */
@Composable
fun AboutScreen(onBack: () -> Unit) {
    val c = Barid.colors
    val uri = LocalUriHandler.current
    LazyColumn(Modifier.fillMaxSize(), contentPadding = PaddingValues(start = 16.dp, end = 16.dp, bottom = 24.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
        item { ScreenTitle(stringResource(R.string.about_title), onBack) }
        item {
            GlassCard(Modifier.fillMaxWidth()) {
                SectionLabel(stringResource(R.string.section_privacy))
                listOf(R.string.about_privacy_1, R.string.about_privacy_2, R.string.about_privacy_3, R.string.about_privacy_4, R.string.about_privacy_5).forEach {
                    Row(Modifier.padding(horizontal = 16.dp, vertical = 6.dp)) {
                        Icon(BaridIcons.Check, null, Modifier.padding(top = 3.dp).size(16.dp), tint = c.success)
                        Text(stringResource(it), style = MaterialTheme.typography.bodySmall, color = c.text, modifier = Modifier.padding(start = 10.dp))
                    }
                }
                Box(Modifier.padding(bottom = 10.dp))
            }
        }
        item {
            GlassCard(Modifier.fillMaxWidth()) {
                SectionLabel(stringResource(R.string.section_open_source))
                Text(stringResource(R.string.about_open_source_body), style = MaterialTheme.typography.bodySmall, color = c.text, modifier = Modifier.padding(horizontal = 16.dp, vertical = 6.dp))
                ListRow(stringResource(R.string.action_view_source), subtitle = "github.com/ali-mirsalari/barid", onClick = { uri.openUri(SOURCE_URL) },
                    leading = { IconTile(BaridIcons.Code, c.accent, c.accentSoft, Modifier.size(32.dp)) },
                    trailing = { Icon(BaridIcons.Forward, null, Modifier.size(16.dp), tint = c.muted) })
            }
        }
        item {
            GlassCard(Modifier.fillMaxWidth()) {
                SectionLabel(stringResource(R.string.section_licenses))
                listOf(
                    "Barid" to "GNU GPL v3.0",
                    "Vazirmatn" to "SIL Open Font License 1.1",
                    "ZXing" to "Apache License 2.0",
                    "Haze" to "Apache License 2.0",
                    "AndroidX, Jetpack Compose, CameraX" to "Apache License 2.0",
                ).forEachIndexed { i, (name, license) ->
                    if (i > 0) Divider()
                    ListRow(name, subtitle = license)
                }
            }
        }
        item {
            GlassCard(Modifier.fillMaxWidth().navigationBarsPadding()) {
                ListRow(stringResource(R.string.made_by_darakav), subtitle = "darakav.com", onClick = { uri.openUri("https://darakav.com") },
                    leading = { BrandTile(DarakavMark, 32.dp, corner = 9.dp, markScale = .9f) },
                    trailing = { Icon(BaridIcons.Forward, null, Modifier.size(16.dp), tint = c.muted) })
            }
        }
    }
}
