package com.darakav.barid.ui.components

import androidx.compose.animation.animateColorAsState
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsPressedAsState
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.RowScope
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Switch
import androidx.compose.material3.SwitchDefaults
import androidx.compose.material3.Text
import androidx.compose.material3.ripple
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.scale
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.lerp
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.role
import androidx.compose.ui.semantics.selected
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.darakav.barid.ui.theme.Barid

private val ButtonShape = RoundedCornerShape(13.dp)

@Composable
private fun pressScale(source: MutableInteractionSource): Float {
    val pressed by source.collectIsPressedAsState()
    return if (pressed) .98f else 1f
}

/** Darakav's primary button: accent gradient, inner highlight and a soft accent glow. */
@Composable
fun PrimaryButton(
    text: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    icon: ImageVector? = null,
    enabled: Boolean = true,
    busy: Boolean = false,
) {
    val c = Barid.colors
    val source = remember { MutableInteractionSource() }
    val top = lerp(c.accent, Color.White, .16f)
    Row(
        modifier
            .scale(pressScale(source))
            .shadow(if (enabled) 10.dp else 0.dp, ButtonShape, ambientColor = c.accent, spotColor = c.accent)
            .clip(ButtonShape)
            .background(Brush.verticalGradient(listOf(top, c.accent)))
            .border(1.dp, Brush.verticalGradient(listOf(Color.White.copy(alpha = .3f), c.accent.copy(alpha = .7f))), ButtonShape)
            .clickable(source, ripple(color = c.onAccent), enabled = enabled && !busy, role = Role.Button, onClick = onClick)
            .heightIn(min = 50.dp)
            .padding(horizontal = 18.dp, vertical = 12.dp),
        horizontalArrangement = Arrangement.spacedBy(8.dp, Alignment.CenterHorizontally),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        if (busy) {
            CircularProgressIndicator(Modifier.size(18.dp), color = c.onAccent, strokeWidth = 2.dp)
        } else if (icon != null) {
            Icon(icon, null, Modifier.size(18.dp), tint = c.onAccent)
        }
        Text(text, style = MaterialTheme.typography.labelLarge, color = c.onAccent.copy(alpha = if (enabled) 1f else .6f), textAlign = TextAlign.Center)
    }
}

/** Secondary button: strong glass with a hairline border. */
@Composable
fun SecondaryButton(
    text: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    icon: ImageVector? = null,
    enabled: Boolean = true,
    busy: Boolean = false,
    color: Color? = null,
) {
    val c = Barid.colors
    val source = remember { MutableInteractionSource() }
    val tint = color ?: c.text
    Row(
        modifier
            .scale(pressScale(source))
            .glass(c, LocalHazeState.current, ButtonShape, strong = true)
            .clickable(source, ripple(), enabled = enabled && !busy, role = Role.Button, onClick = onClick)
            .heightIn(min = 48.dp)
            .padding(horizontal = 14.dp, vertical = 11.dp),
        horizontalArrangement = Arrangement.spacedBy(8.dp, Alignment.CenterHorizontally),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        if (busy) CircularProgressIndicator(Modifier.size(16.dp), color = tint, strokeWidth = 2.dp)
        else if (icon != null) Icon(icon, null, Modifier.size(17.dp), tint = tint)
        Text(text, style = MaterialTheme.typography.labelLarge, color = tint.copy(alpha = if (enabled) 1f else .5f), maxLines = 1)
    }
}

/** Text-only action. */
@Composable
fun QuietButton(text: String, onClick: () -> Unit, modifier: Modifier = Modifier, color: Color? = null) {
    val c = Barid.colors
    Box(
        modifier.clip(ButtonShape).clickable(role = Role.Button, onClick = onClick).heightIn(min = 44.dp).padding(horizontal = 12.dp),
        contentAlignment = Alignment.Center,
    ) {
        Text(text, style = MaterialTheme.typography.labelLarge, color = color ?: c.accent)
    }
}

/** Round glass icon button (top bars). */
@Composable
fun GlassIconButton(icon: ImageVector, contentDescription: String, onClick: () -> Unit, modifier: Modifier = Modifier) {
    val c = Barid.colors
    Box(
        modifier.size(42.dp).glass(c, LocalHazeState.current, RoundedCornerShape(13.dp)).clickable(role = Role.Button, onClick = onClick)
            .semantics { },
        contentAlignment = Alignment.Center,
    ) {
        Icon(icon, contentDescription, Modifier.size(20.dp), tint = c.text)
    }
}

enum class Tone { OK, WAIT, DANGER, NEUTRAL }

@Composable
fun StatusPill(text: String, tone: Tone, modifier: Modifier = Modifier, dot: Boolean = true) {
    val c = Barid.colors
    val (fg, bg) = when (tone) {
        Tone.OK -> c.success to c.successSurface
        Tone.WAIT -> c.warning to c.warningSurface
        Tone.DANGER -> c.danger to c.dangerSurface
        Tone.NEUTRAL -> c.muted to c.line
    }
    Row(
        modifier.clip(CircleShape).background(bg).padding(horizontal = 10.dp, vertical = 3.dp),
        horizontalArrangement = Arrangement.spacedBy(6.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        if (dot) Box(Modifier.size(7.dp).background(fg, CircleShape).border(3.dp, fg.copy(alpha = .22f), CircleShape))
        Text(text, style = MaterialTheme.typography.labelMedium, color = fg)
    }
}

/** A row of options in a recessed track; the selected one is raised. */
@Composable
fun <T> Segmented(options: List<Pair<T, String>>, selected: T, onSelect: (T) -> Unit, modifier: Modifier = Modifier) {
    val c = Barid.colors
    Row(modifier.clip(RoundedCornerShape(11.dp)).background(c.line).padding(3.dp), horizontalArrangement = Arrangement.spacedBy(2.dp)) {
        options.forEach { (value, label) ->
            val on = value == selected
            val bg by animateColorAsState(if (on) c.surfaceSolid else Color.Transparent, label = "segment")
            Box(
                Modifier.clip(RoundedCornerShape(9.dp)).background(bg)
                    .clickable(role = Role.RadioButton) { onSelect(value) }
                    .semantics { this.selected = on; role = Role.RadioButton }
                    .padding(horizontal = 11.dp, vertical = 5.dp),
                contentAlignment = Alignment.Center,
            ) {
                Text(label, style = MaterialTheme.typography.labelSmall, color = if (on) c.text else c.muted)
            }
        }
    }
}

@Composable
fun BaridSwitch(checked: Boolean, onCheckedChange: (Boolean) -> Unit) {
    val c = Barid.colors
    Switch(
        checked = checked,
        onCheckedChange = onCheckedChange,
        colors = SwitchDefaults.colors(
            checkedTrackColor = c.accent, checkedThumbColor = c.onAccent, checkedBorderColor = Color.Transparent,
            uncheckedTrackColor = c.line, uncheckedThumbColor = Color.White, uncheckedBorderColor = Color.Transparent,
        ),
    )
}

/** Small rounded icon tile used in lists. */
@Composable
fun IconTile(icon: ImageVector, tint: Color, background: Color, modifier: Modifier = Modifier) {
    Box(modifier.size(36.dp).clip(RoundedCornerShape(11.dp)).background(background), contentAlignment = Alignment.Center) {
        Icon(icon, null, Modifier.size(18.dp), tint = tint)
    }
}

/** A settings-style row: optional leading content, title and subtitle, optional trailing content. */
@Composable
fun ListRow(
    title: String,
    modifier: Modifier = Modifier,
    subtitle: String? = null,
    titleColor: Color? = null,
    onClick: (() -> Unit)? = null,
    leading: (@Composable () -> Unit)? = null,
    trailing: (@Composable RowScope.() -> Unit)? = null,
) {
    val c = Barid.colors
    Row(
        modifier.fillMaxWidth()
            .then(if (onClick != null) Modifier.clickable(onClick = onClick) else Modifier)
            .heightIn(min = 52.dp)
            .padding(horizontal = 16.dp, vertical = 10.dp),
        horizontalArrangement = Arrangement.spacedBy(12.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        leading?.invoke()
        androidx.compose.foundation.layout.Column(Modifier.weight(1f)) {
            Text(title, style = MaterialTheme.typography.bodyMedium, color = titleColor ?: c.text)
            if (subtitle != null) Text(subtitle, style = MaterialTheme.typography.bodySmall, color = c.muted)
        }
        trailing?.invoke(this)
    }
}

@Composable
fun Divider() {
    Box(Modifier.fillMaxWidth().padding(horizontal = 0.dp).heightIn(min = 1.dp, max = 1.dp).background(Barid.colors.line))
}
