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
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.ShaderBrush
import androidx.compose.ui.graphics.SweepGradientShader
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.unit.dp

private val RainbowColors = listOf(
    Color(0xFFFF1744),
    Color(0xFFFF9100),
    Color(0xFFFFEA00),
    Color(0xFF00E676),
    Color(0xFF00E5FF),
    Color(0xFF2979FF),
    Color(0xFFD500F9),
    Color(0xFFFF1744),
)

/** Easter egg: a rainbow outline around the screen whose colors keep turning. */
@Composable
fun RainbowBorder(modifier: Modifier = Modifier) {
    val angle by rememberInfiniteTransition(label = "rainbow").animateFloat(
        initialValue = 0f,
        targetValue = 360f,
        animationSpec = infiniteRepeatable(tween(durationMillis = 6_000, easing = LinearEasing)),
        label = "angle",
    )
    Spacer(
        modifier
            .fillMaxSize()
            .drawWithCache {
                val shader = SweepGradientShader(center, RainbowColors)
                val brush = ShaderBrush(shader)
                val matrix = Matrix()
                val line = 5.dp.toPx()
                val glow = 16.dp.toPx()
                val corner = CornerRadius(20.dp.toPx())
                onDrawBehind {
                    // Turning the gradient rather than the outline keeps the frame square to the
                    // screen; reading the angle here only redraws, it doesn't recompose.
                    matrix.setRotate(angle, center.x, center.y)
                    shader.setLocalMatrix(matrix)
                    drawRoundRect(
                        brush,
                        topLeft = Offset(glow / 2, glow / 2),
                        size = Size(size.width - glow, size.height - glow),
                        cornerRadius = corner,
                        alpha = 0.3f,
                        style = Stroke(glow),
                    )
                    drawRoundRect(
                        brush,
                        topLeft = Offset(line / 2, line / 2),
                        size = Size(size.width - line, size.height - line),
                        cornerRadius = corner,
                        style = Stroke(line),
                    )
                }
            },
    )
}
