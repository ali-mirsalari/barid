package com.darakav.barid.ui.theme

import android.os.Build
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Typography
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.dynamicDarkColorScheme
import androidx.compose.material3.dynamicLightColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.Immutable
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.lerp
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.Font
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.sp
import com.darakav.barid.R

/**
 * Darakav's color presets: the website's exact light and dark accents (frontend palette-state.ts) and
 * its theme tones (frontend styles.css `--theme-*`). Like the website, every theme tints the glow,
 * chrome and surfaces, not only the accent: [glowA] and [glowB] are the two glow hues, [glowC] a faint
 * third, [deep] the dark chrome, [canvas]/[canvasDark] the page and [night]/[nightSolid] the dark
 * surfaces. Ocean keeps the original brand look; the brand colors stay on the logos only.
 */
@Immutable
data class ThemeTones(
    val glowA: Color, val glowB: Color, val glowC: Color, val deep: Color,
    val canvas: Color, val canvasDark: Color, val night: Color, val nightSolid: Color,
)

enum class Palette(val id: String, val light: Color, val dark: Color, val tones: ThemeTones) {
    YOU("you", Color(0xFF3F6837), Color(0xFFA4D397), tones(0xFF3F6837, 0xFF8CC63F, 0xFF355F80, 0xFF0F1F14, 0xFFF0F3EE, 0xFF0B110C, 0xFF19211A, 0xFF151B16)),
    SLATE("slate", Color(0xFF4F6079), Color(0xFFB4C5E2), tones(0xFF5D6F8C, 0xFF93A3BB, 0xFF8A8FA3, 0xFF171C25, 0xFFEFF1F4, 0xFF0D1015, 0xFF1B1F27, 0xFF171A21)),
    OCEAN("ocean", Color(0xFF285BC5), Color(0xFF91B8FF), tones(0xFF0069FE, 0xFF00A4FE, 0xFF7C5CF6, 0xFF07182B, 0xFFEEF2F8, 0xFF0A101B, 0xFF161D2B, 0xFF151B27)),
    IRIS("iris", Color(0xFF7450B6), Color(0xFFC6A8FF), tones(0xFF7A4DFF, 0xFFC084FC, 0xFF5B8CFF, 0xFF1A1133, 0xFFF2F0F9, 0xFF0E0B19, 0xFF1E1830, 0xFF191426)),
    TEAL("teal", Color(0xFF087978), Color(0xFF77D4CD), tones(0xFF0D9488, 0xFF22C5D6, 0xFF3B82F6, 0xFF062523, 0xFFEDF5F4, 0xFF081413, 0xFF13221F, 0xFF111C1B)),
    CLAY("clay", Color(0xFFA34F36), Color(0xFFF0B39A), tones(0xFFD0603C, 0xFFF0A33F, 0xFFE0457B, 0xFF29150F, 0xFFF7F1EE, 0xFF140D0B, 0xFF251A16, 0xFF1F1613)),
    FOREST("forest", Color(0xFF276B50), Color(0xFF8AD1AC), tones(0xFF2F8A57, 0xFF8CC63F, 0xFF14A89A, 0xFF0C2219, 0xFFEFF4F0, 0xFF0A120E, 0xFF16221C, 0xFF131C17));

    companion object {
        /** Tones for Material You: the wallpaper's primary and tertiary hues over lightly tinted neutrals. */
        fun dynamicTones(primary: Color, primaryLight: Color, tertiary: Color) = ThemeTones(
            glowA = primary, glowB = primaryLight, glowC = tertiary,
            deep = lerp(Color(0xFF0B0E14), primary, .16f),
            canvas = lerp(Color(0xFFF1F2F5), primary, .04f), canvasDark = lerp(Color(0xFF0B0D12), primary, .05f),
            night = lerp(Color(0xFF1B1E26), primary, .07f), nightSolid = lerp(Color(0xFF171A21), primary, .06f),
        )

        val dynamicAvailable: Boolean get() = Build.VERSION.SDK_INT >= Build.VERSION_CODES.S
        val choices: List<Palette> get() = if (dynamicAvailable) entries else entries.filter { it != YOU }
        fun of(id: String?): Palette = entries.firstOrNull { it.id == id && (it != YOU || dynamicAvailable) }
            ?: if (dynamicAvailable) YOU else SLATE
    }
}

/** Darakav's design tokens (frontend styles.css), resolved for one theme and accent. */
@Immutable
data class BaridColors(
    val dark: Boolean,
    val accent: Color,
    val onAccent: Color,
    val canvas: Color,
    val surfaceSolid: Color,
    val surface: Color,
    val surfaceStrong: Color,
    val line: Color,
    val glassEdge: Color,
    val text: Color,
    val muted: Color,
    val success: Color,
    val successSurface: Color,
    val warning: Color,
    val warningSurface: Color,
    val danger: Color,
    val dangerSurface: Color,
    val grid: Color,
    val solid: Boolean,
    val tones: ThemeTones,
) {
    val accentSoft: Color get() = accent.copy(alpha = if (dark) .16f else .12f)

    companion object {
        /** Brand colors: the Barid and Darakav marks only; everything else follows the theme tones. */
        val BrandBlue = Color(0xFF0069FE)
        val BrandCyan = Color(0xFF00A4FE)
        val BrandNavy = Color(0xFF07182B)

        fun light(accent: Color, solid: Boolean, tones: ThemeTones = Palette.OCEAN.tones) = BaridColors(
            dark = false, accent = accent, onAccent = Color.White,
            canvas = tones.canvas, surfaceSolid = Color.White,
            surface = if (solid) Color.White else Color.White.copy(alpha = .6f),
            surfaceStrong = if (solid) Color.White else Color.White.copy(alpha = .78f),
            line = Color(0xFF1E293B).copy(alpha = .1f), glassEdge = Color.White.copy(alpha = .8f),
            text = Color(0xFF252C38), muted = Color(0xFF5D6879),
            success = Color(0xFF236242), successSurface = Color(0xFFE3F0E8),
            warning = Color(0xFF81551C), warningSurface = Color(0xFFFFF3DB),
            danger = Color(0xFFA43B36), dangerSurface = Color(0xFFFBECEA),
            grid = Color(0xFF0F172A).copy(alpha = .05f), solid = solid, tones = tones,
        )

        fun dark(accent: Color, solid: Boolean, tones: ThemeTones = Palette.OCEAN.tones) = BaridColors(
            dark = true, accent = accent, onAccent = tones.canvasDark,
            canvas = tones.canvasDark, surfaceSolid = tones.nightSolid,
            surface = if (solid) tones.nightSolid else tones.night.copy(alpha = .55f),
            surfaceStrong = if (solid) tones.nightSolid else tones.night.copy(alpha = .74f),
            line = Color.White.copy(alpha = .09f), glassEdge = Color.White.copy(alpha = .07f),
            text = Color(0xFFE7EBF3), muted = Color(0xFFAAB4C5),
            success = Color(0xFF91D1AF), successSurface = Color(0xFF20372C),
            warning = Color(0xFFF0CA88), warningSurface = Color(0xFF40351F),
            danger = Color(0xFFF2A5A0), dangerSurface = Color(0xFF3B2527),
            grid = Color.White.copy(alpha = .035f), solid = solid, tones = tones,
        )
    }
}

private fun tones(a: Long, b: Long, c: Long, deep: Long, canvas: Long, canvasDark: Long, night: Long, nightSolid: Long) =
    ThemeTones(Color(a), Color(b), Color(c), Color(deep), Color(canvas), Color(canvasDark), Color(night), Color(nightSolid))

val LocalBaridColors = staticCompositionLocalOf { BaridColors.light(Palette.SLATE.light, false, Palette.SLATE.tones) }

val Vazirmatn = FontFamily(
    Font(R.font.vazirmatn_regular, FontWeight.Normal),
    Font(R.font.vazirmatn_medium, FontWeight.Medium),
    Font(R.font.vazirmatn_semibold, FontWeight.SemiBold),
    Font(R.font.vazirmatn_bold, FontWeight.Bold),
)

private fun typography(): Typography {
    val base = TextStyle(fontFamily = Vazirmatn)
    return Typography(
        displaySmall = base.copy(fontSize = 30.sp, lineHeight = 40.sp, fontWeight = FontWeight.Bold),
        headlineSmall = base.copy(fontSize = 20.sp, lineHeight = 30.sp, fontWeight = FontWeight.Bold),
        titleLarge = base.copy(fontSize = 18.sp, lineHeight = 28.sp, fontWeight = FontWeight.Bold),
        titleMedium = base.copy(fontSize = 15.sp, lineHeight = 24.sp, fontWeight = FontWeight.SemiBold),
        titleSmall = base.copy(fontSize = 13.5.sp, lineHeight = 22.sp, fontWeight = FontWeight.SemiBold),
        bodyLarge = base.copy(fontSize = 15.sp, lineHeight = 26.sp),
        bodyMedium = base.copy(fontSize = 13.5.sp, lineHeight = 23.sp),
        bodySmall = base.copy(fontSize = 12.sp, lineHeight = 20.sp),
        labelLarge = base.copy(fontSize = 14.sp, lineHeight = 20.sp, fontWeight = FontWeight.SemiBold),
        labelMedium = base.copy(fontSize = 12.sp, lineHeight = 18.sp, fontWeight = FontWeight.SemiBold),
        labelSmall = base.copy(fontSize = 11.sp, lineHeight = 16.sp, fontWeight = FontWeight.Medium),
    )
}

object Barid {
    val colors: BaridColors @Composable get() = LocalBaridColors.current
}

@Composable
fun BaridTheme(themeMode: String, palette: Palette, reduceTransparency: Boolean, content: @Composable () -> Unit) {
    val dark = when (themeMode) {
        "light" -> false
        "dark" -> true
        else -> isSystemInDarkTheme()
    }
    val context = LocalContext.current
    val dynamic = palette == Palette.YOU && Build.VERSION.SDK_INT >= Build.VERSION_CODES.S
    val accent: Color
    val tones: ThemeTones
    if (dynamic) {
        val lightScheme = dynamicLightColorScheme(context)
        val darkScheme = dynamicDarkColorScheme(context)
        accent = if (dark) darkScheme.primary else lightScheme.primary
        tones = Palette.dynamicTones(lightScheme.primary, darkScheme.primary, lightScheme.tertiary)
    } else {
        accent = if (dark) palette.dark else palette.light
        tones = palette.tones
    }
    val colors = if (dark) BaridColors.dark(accent, reduceTransparency, tones) else BaridColors.light(accent, reduceTransparency, tones)
    val scheme = (if (dark) darkColorScheme() else lightColorScheme()).copy(
        primary = colors.accent,
        onPrimary = colors.onAccent,
        primaryContainer = colors.accentSoft,
        onPrimaryContainer = colors.text,
        secondary = colors.accent,
        onSecondary = colors.onAccent,
        background = colors.canvas,
        onBackground = colors.text,
        surface = colors.surfaceSolid,
        onSurface = colors.text,
        surfaceVariant = colors.line,
        onSurfaceVariant = colors.muted,
        surfaceContainerHigh = colors.surfaceSolid,
        surfaceContainerHighest = colors.surfaceSolid,
        outline = colors.muted.copy(alpha = .5f),
        outlineVariant = colors.line,
        error = colors.danger,
        onError = colors.onAccent,
    )
    CompositionLocalProvider(LocalBaridColors provides colors) {
        MaterialTheme(colorScheme = scheme, typography = typography(), content = content)
    }
}
