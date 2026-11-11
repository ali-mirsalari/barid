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
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.Font
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.sp
import com.darakav.barid.R

/** Darakav's color presets, with the website's exact light and dark accents (frontend palette-state.ts). */
enum class Palette(val id: String, val light: Color, val dark: Color) {
    YOU("you", Color(0xFF3F6837), Color(0xFFA4D397)),
    SLATE("slate", Color(0xFF4F6079), Color(0xFFB4C5E2)),
    OCEAN("ocean", Color(0xFF285BC5), Color(0xFF91B8FF)),
    IRIS("iris", Color(0xFF7450B6), Color(0xFFC6A8FF)),
    TEAL("teal", Color(0xFF087978), Color(0xFF77D4CD)),
    CLAY("clay", Color(0xFFA34F36), Color(0xFFF0B39A)),
    FOREST("forest", Color(0xFF276B50), Color(0xFF8AD1AC));

    companion object {
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
) {
    val accentSoft: Color get() = accent.copy(alpha = if (dark) .16f else .12f)

    companion object {
        val BrandBlue = Color(0xFF0069FE)
        val BrandCyan = Color(0xFF00A4FE)
        val BrandNavy = Color(0xFF07182B)
        val Violet = Color(0xFF7C5CF6)

        fun light(accent: Color, solid: Boolean) = BaridColors(
            dark = false, accent = accent, onAccent = Color.White,
            canvas = Color(0xFFEEF2F8), surfaceSolid = Color.White,
            surface = if (solid) Color.White else Color.White.copy(alpha = .6f),
            surfaceStrong = if (solid) Color.White else Color.White.copy(alpha = .78f),
            line = Color(0xFF1E293B).copy(alpha = .1f), glassEdge = Color.White.copy(alpha = .8f),
            text = Color(0xFF252C38), muted = Color(0xFF5D6879),
            success = Color(0xFF236242), successSurface = Color(0xFFE3F0E8),
            warning = Color(0xFF81551C), warningSurface = Color(0xFFFFF3DB),
            danger = Color(0xFFA43B36), dangerSurface = Color(0xFFFBECEA),
            grid = Color(0xFF0F172A).copy(alpha = .05f), solid = solid,
        )

        fun dark(accent: Color, solid: Boolean) = BaridColors(
            dark = true, accent = accent, onAccent = Color(0xFF0A101B),
            canvas = Color(0xFF0A101B), surfaceSolid = Color(0xFF151B27),
            surface = if (solid) Color(0xFF151B27) else Color(0xFF161D2B).copy(alpha = .55f),
            surfaceStrong = if (solid) Color(0xFF151B27) else Color(0xFF18202F).copy(alpha = .74f),
            line = Color.White.copy(alpha = .09f), glassEdge = Color.White.copy(alpha = .07f),
            text = Color(0xFFE7EBF3), muted = Color(0xFFAAB4C5),
            success = Color(0xFF91D1AF), successSurface = Color(0xFF20372C),
            warning = Color(0xFFF0CA88), warningSurface = Color(0xFF40351F),
            danger = Color(0xFFF2A5A0), dangerSurface = Color(0xFF3B2527),
            grid = Color.White.copy(alpha = .035f), solid = solid,
        )
    }
}

val LocalBaridColors = staticCompositionLocalOf { BaridColors.light(Palette.SLATE.light, false) }

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
    val accent = if (palette == Palette.YOU && Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
        if (dark) dynamicDarkColorScheme(context).primary else dynamicLightColorScheme(context).primary
    } else if (dark) palette.dark else palette.light
    val colors = if (dark) BaridColors.dark(accent, reduceTransparency) else BaridColors.light(accent, reduceTransparency)
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
