package com.tk.toolkit.tv.launcher.ui

import android.graphics.Matrix
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.drawWithCache
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.LinearGradientShader
import androidx.compose.ui.graphics.ShaderBrush
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.graphics.SweepGradientShader
import androidx.compose.ui.graphics.TileMode
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.unit.dp
import com.tk.toolkit.tv.launcher.data.BorderSettings
import com.tk.toolkit.tv.launcher.data.BorderStyle
import kotlin.math.PI
import kotlin.math.cos

/**
 * Easter egg: an animated outline around the screen, styled by [settings] (Toolkit's options).
 * The animation only redraws this layer; it never recomposes the home screen.
 */
@Composable
fun RainbowBorder(settings: BorderSettings, modifier: Modifier = Modifier) {
    val progress by rememberInfiniteTransition(label = "border").animateFloat(
        initialValue = 0f,
        targetValue = 1f,
        animationSpec = infiniteRepeatable(tween(durationMillis = settings.speed.periodMs, easing = LinearEasing)),
        label = "progress",
    )
    Spacer(
        modifier
            .fillMaxSize()
            .drawWithCache {
                val middle = Offset(size.width / 2, size.height / 2)
                val colors = settings.palette.colors
                val loop = colors + colors.first()
                val shader = when (settings.style) {
                    BorderStyle.Rotation, BorderStyle.Breathing -> SweepGradientShader(middle, loop)
                    BorderStyle.Comet -> cometGradient(colors, 1).let { (c, stops) -> SweepGradientShader(middle, c, stops) }
                    BorderStyle.TwinComets -> cometGradient(colors, 2).let { (c, stops) -> SweepGradientShader(middle, c, stops) }
                    // Mirrored, so sliding by twice the gradient's length loops seamlessly.
                    BorderStyle.Wave -> LinearGradientShader(Offset.Zero, middle, loop, tileMode = TileMode.Mirror)
                    BorderStyle.Disco -> null
                }
                val shaderBrush = shader?.let { ShaderBrush(it) }
                val matrix = Matrix()
                val line = settings.thickness.dp.dp.toPx()
                val corner = CornerRadius(20.dp.toPx())
                onDrawBehind {
                    val p = progress
                    var width = line
                    var alpha = 1f
                    // Moving the gradient rather than the outline keeps the frame square to the screen.
                    when (settings.style) {
                        BorderStyle.Rotation, BorderStyle.Comet, BorderStyle.TwinComets ->
                            matrix.setRotate(p * 360f, middle.x, middle.y)
                        BorderStyle.Breathing -> {
                            val pulse = (0.5 - 0.5 * cos(2 * PI * p)).toFloat()
                            alpha = 0.3f + 0.7f * pulse
                            width = line * (0.7f + 0.6f * pulse)
                            matrix.setRotate(p * 90f, middle.x, middle.y)
                        }
                        BorderStyle.Wave -> matrix.setTranslate(p * size.width, p * size.height)
                        BorderStyle.Disco -> Unit
                    }
                    shader?.setLocalMatrix(matrix)
                    val brush: Brush = shaderBrush
                        ?: SolidColor(colors[(p * colors.size).toInt().coerceAtMost(colors.lastIndex)])
                    val glow = settings.glow.alpha
                    if (glow > 0f) outline(brush, width * 3.2f, glow * alpha, corner)
                    outline(brush, width, alpha, corner)
                }
            },
    )
}

private fun androidx.compose.ui.graphics.drawscope.DrawScope.outline(
    brush: Brush,
    width: Float,
    alpha: Float,
    corner: CornerRadius,
) = drawRoundRect(
    brush,
    topLeft = Offset(width / 2, width / 2),
    size = Size(size.width - width, size.height - width),
    cornerRadius = corner,
    alpha = alpha,
    style = Stroke(width),
)

/**
 * [count] comets evenly spaced around a sweep gradient: each fades in from transparent through
 * the palette to a sharp head, then the next one starts.
 */
private fun cometGradient(colors: List<Color>, count: Int): Pair<List<Color>, List<Float>> {
    val stopColors = mutableListOf<Color>()
    val stops = mutableListOf<Float>()
    val segment = 1f / count
    repeat(count) { i ->
        val start = i * segment
        stopColors += colors.first().copy(alpha = 0f)
        stops += start
        stopColors += colors.first().copy(alpha = 0f)
        stops += start + segment * 0.4f
        colors.forEachIndexed { j, color ->
            stopColors += color
            stops += start + segment * (0.4f + 0.6f * (j + 1) / colors.size)
        }
    }
    return stopColors to stops
}
