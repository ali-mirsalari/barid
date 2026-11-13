package com.darakav.barid.ui.components

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxScope
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.runtime.Composable
import androidx.compose.runtime.compositionLocalOf
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawWithContent
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.BlendMode
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.CompositingStrategy
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.graphics.vector.addPathNodes
import androidx.compose.ui.graphics.vector.group
import androidx.compose.ui.graphics.vector.path
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.foundation.Image
import com.darakav.barid.ui.theme.Barid
import com.darakav.barid.ui.theme.BaridColors
import dev.chrisbanes.haze.HazeState
import dev.chrisbanes.haze.HazeStyle
import dev.chrisbanes.haze.HazeTint
import dev.chrisbanes.haze.hazeEffect
import dev.chrisbanes.haze.hazeSource

/** The backdrop that glass surfaces blur; null where nothing should be blurred (solid mode). */
val LocalHazeState = compositionLocalOf<HazeState?> { null }

/**
 * Darakav's backdrop: canvas color, four soft aurora glows (the theme's two glow hues, the accent and
 * its faint third hue)
 * and a faint grid that fades out from the top. Hidden when transparency is reduced, as on the website.
 */
@Composable
fun Backdrop(hazeState: HazeState?, modifier: Modifier = Modifier) {
    val c = Barid.colors
    val base = Modifier.fillMaxSize().background(c.canvas)
    if (c.solid) {
        Box(modifier.then(base))
        return
    }
    Box(modifier.then(base).then(if (hazeState != null) Modifier.hazeSource(hazeState) else Modifier)) {
        Canvas(Modifier.fillMaxSize()) {
            val w = size.width
            val h = size.height
            val m = maxOf(w, h)
            fun glow(color: Color, alpha: Float, x: Float, y: Float, r: Float) = drawRect(
                Brush.radialGradient(listOf(color.copy(alpha = alpha), Color.Transparent), Offset(x, y), r),
            )
            glow(c.tones.glowA, if (c.dark) .34f else .24f, w * .9f, 0f, m * .55f)
            glow(c.tones.glowB, if (c.dark) .17f else .22f, 0f, h, m * .55f)
            glow(c.accent, if (c.dark) .13f else .18f, w * .5f, h * .55f, m * .42f)
            glow(c.tones.glowC, if (c.dark) .13f else .1f, w * .1f, h * .12f, m * .3f)
        }
        Canvas(
            Modifier.fillMaxSize()
                .graphicsLayer { compositingStrategy = CompositingStrategy.Offscreen }
                .drawWithContent {
                    drawContent()
                    drawRect(
                        Brush.radialGradient(listOf(Color.Black, Color.Transparent), Offset(size.width * .7f, 0f), size.height * .55f),
                        blendMode = BlendMode.DstIn,
                    )
                },
        ) {
            val step = 36.dp.toPx()
            var x = 0f
            while (x <= size.width) { drawLine(c.grid, Offset(x, 0f), Offset(x, size.height), 1f); x += step }
            var y = 0f
            while (y <= size.height) { drawLine(c.grid, Offset(0f, y), Offset(size.width, y), 1f); y += step }
        }
    }
}

/**
 * Frosted glass: blurred backdrop (Android 12+), translucent tint, bright top edge and hairline border.
 * [blur] false keeps the look without the live blur, for long scrolling lists: the backdrop is soft
 * glows that look the same blurred or not, and re-blurring every card on every frame stutters on
 * slower phones.
 */
fun Modifier.glass(
    colors: BaridColors,
    hazeState: HazeState?,
    shape: Shape = RoundedCornerShape(18.dp),
    strong: Boolean = false,
    blur: Boolean = true,
): Modifier {
    val tint = if (strong) colors.surfaceStrong else colors.surface
    val edge = Brush.verticalGradient(0f to colors.glassEdge, .25f to colors.line, 1f to colors.line)
    val fill = if (colors.solid || hazeState == null || !blur) {
        Modifier.background(if (colors.solid) colors.surfaceSolid else tint, shape)
    } else {
        Modifier.hazeEffect(
            state = hazeState,
            style = HazeStyle(
                backgroundColor = colors.canvas,
                tint = HazeTint(tint),
                blurRadius = 22.dp,
                noiseFactor = 0f,
                // Android 8-11 cannot blur: near-solid so text stays crisp.
                fallbackTint = HazeTint(colors.surfaceSolid.copy(alpha = .92f)),
            ),
        )
    }
    return this.clip(shape).then(fill).border(1.dp, edge, shape)
}

@Composable
fun GlassCard(
    modifier: Modifier = Modifier,
    shape: Shape = RoundedCornerShape(18.dp),
    strong: Boolean = false,
    blur: Boolean = true,
    content: @Composable ColumnScope.() -> Unit,
) {
    Column(modifier.glass(Barid.colors, LocalHazeState.current, shape, strong, blur), content = content)
}

@Composable
fun GlassBox(modifier: Modifier = Modifier, shape: Shape = RoundedCornerShape(18.dp), contentAlignment: Alignment = Alignment.Center, content: @Composable BoxScope.() -> Unit) {
    Box(modifier.glass(Barid.colors, LocalHazeState.current, shape), contentAlignment = contentAlignment, content = content)
}

/** Barid's mark: an envelope in Darakav's two blues. */
val BaridMark: ImageVector = ImageVector.Builder("barid", 48.dp, 48.dp, 48f, 48f).apply {
    addPath(addPathNodes("M6 14q0-4 4-4h28q4 0 4 4v3L24 28 6 17z"), fill = SolidColor(BaridColors.BrandBlue))
    addPath(addPathNodes("M6 22l18 11 18-11v12q0 4-4 4H10q-4 0-4-4z"), fill = SolidColor(BaridColors.BrandCyan))
}.build()

/** Darakav's mark (frontend favicon.svg), for "made by" and the built-in server. */
val DarakavMark: ImageVector = ImageVector.Builder("darakav", 64.dp, 64.dp, 64f, 64f).apply {
    group(translationX = 16.88f, translationY = 7.92f, scaleX = .28f, scaleY = .28f) {
        addPath(addPathNodes("M40 79L104 114L13 168Q2 174 2 162L2 112Q2 106 8 103Z"), fill = SolidColor(BaridColors.BrandCyan))
        addPath(addPathNodes("M2 12Q2 0 12 4L99 53Q106 57 106 64L106 109Q106 114 103 115L8 61Q2 58 2 51Z"), fill = SolidColor(BaridColors.BrandBlue))
    }
}.build()

/** A mark on Darakav's navy tile. */
@Composable
fun BrandTile(mark: ImageVector, size: Dp, modifier: Modifier = Modifier, corner: Dp = size * .29f, markScale: Float = .64f) {
    Box(
        modifier.size(size).background(BaridColors.BrandNavy, RoundedCornerShape(corner)),
        contentAlignment = Alignment.Center,
    ) {
        Image(mark, contentDescription = null, modifier = Modifier.size(size * markScale))
    }
}
