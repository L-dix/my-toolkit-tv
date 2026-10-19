package com.tk.toolkit.tv.core

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Canvas
import androidx.compose.ui.graphics.FilterQuality
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.graphics.drawscope.CanvasDrawScope
import androidx.compose.ui.unit.Density
import androidx.compose.ui.unit.IntSize
import androidx.compose.ui.unit.LayoutDirection
import com.tk.toolkit.tv.launcher.ui.HomeScreen
import com.tk.toolkit.tv.ui.theme.ToolkitColors
import com.tk.toolkit.tv.ui.theme.ToolkitTheme

/**
 * App shell. The launcher home is the root screen; future tools get their own
 * screens here and are reached from the home header.
 */
@Composable
fun ToolkitApp() {
    ToolkitTheme {
        val backdrop = remember { renderBackdrop() }
        Box(
            Modifier
                .fillMaxSize()
                .drawBehind {
                    drawImage(
                        backdrop,
                        dstSize = IntSize(size.width.toInt(), size.height.toInt()),
                        filterQuality = FilterQuality.Low,
                    )
                }
        ) {
            HomeScreen()
        }
    }
}

/**
 * The ambient glow, rendered once at low resolution and stretched to fill the screen.
 * Drawing live full-screen gradients every frame is too slow for TV GPUs.
 */
private fun renderBackdrop(): ImageBitmap {
    val w = 320
    val h = 180
    val bitmap = ImageBitmap(w, h)
    CanvasDrawScope().draw(Density(1f), LayoutDirection.Ltr, Canvas(bitmap), Size(w.toFloat(), h.toFloat())) {
        drawRect(ToolkitColors.Background)
        drawRect(
            Brush.radialGradient(
                listOf(ToolkitColors.GlowA.copy(alpha = 0.22f), ToolkitColors.GlowA.copy(alpha = 0f)),
                center = Offset(w * 0.08f, h * 0.02f),
                radius = w * 0.55f,
            )
        )
        drawRect(
            Brush.radialGradient(
                listOf(ToolkitColors.GlowB.copy(alpha = 0.16f), ToolkitColors.GlowB.copy(alpha = 0f)),
                center = Offset(w * 0.95f, h * 1.05f),
                radius = w * 0.6f,
            )
        )
    }
    return bitmap
}
