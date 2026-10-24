package com.tk.toolkit.tv.launcher.ui

import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.drawWithCache
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import com.tk.toolkit.tv.launcher.data.HomeBackground

/**
 * The home screen's background (Toolkit's options). Drawn in its own layer, so scrolling the
 * grid above doesn't redraw these full-screen gradients.
 */
@Composable
fun HomeBackdrop(background: HomeBackground, accent: Color, modifier: Modifier = Modifier) {
    if (background == HomeBackground.Black) return
    Spacer(
        modifier
            .fillMaxSize()
            .graphicsLayer()
            .drawWithCache {
                val w = size.width
                val h = size.height
                fun glow(color: Color, alpha: Float, x: Float, y: Float, radius: Float) = Brush.radialGradient(
                    listOf(color.copy(alpha = alpha), color.copy(alpha = 0f)),
                    center = Offset(w * x, h * y),
                    radius = w * radius,
                )
                val layers = when (background) {
                    HomeBackground.Black -> emptyList()
                    HomeBackground.Halo -> listOf(glow(accent, 0.20f, 0.08f, 0.02f, 0.55f), glow(accent, 0.10f, 0.95f, 1.05f, 0.6f))
                    // The original Toolkit backdrop.
                    HomeBackground.Aurora -> listOf(
                        glow(Color(0xFF3B3FD8), 0.22f, 0.08f, 0.02f, 0.55f),
                        glow(Color(0xFF0E8F8A), 0.16f, 0.95f, 1.05f, 0.6f),
                    )
                    HomeBackground.Wine -> listOf(
                        glow(Color(0xFF8B0A1A), 0.38f, 0.10f, 0.0f, 0.6f),
                        glow(Color(0xFF4A0711), 0.45f, 0.90f, 1.05f, 0.7f),
                    )
                    HomeBackground.Night -> listOf(
                        Brush.verticalGradient(listOf(Color(0xFF0B1638), Color.Black)),
                        glow(Color(0xFF3949AB), 0.12f, 0.85f, 0.0f, 0.5f),
                    )
                }
                onDrawBehind { layers.forEach { drawRect(it) } }
            },
    )
}
