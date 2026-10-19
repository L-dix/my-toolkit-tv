package com.tk.toolkit.tv.launcher.ui

import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.gestures.BringIntoViewSpec
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.drawWithContent
import androidx.compose.ui.graphics.BlendMode
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.CompositingStrategy
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.unit.Dp

/**
 * Focus scrolling that only moves when the focused item isn't fully visible, and then by the
 * least amount (plus [marginPx] so scaled-up focused cards aren't clipped). Compose's TV default
 * pivots every focus change to 30% of the container, which scrolls on every key press.
 */
@OptIn(ExperimentalFoundationApi::class)
fun minimalScrollSpec(marginPx: Float) = object : BringIntoViewSpec {
    override fun calculateScrollDistance(offset: Float, size: Float, containerSize: Float): Float {
        val leading = offset - marginPx
        val trailing = offset + size + marginPx
        return when {
            leading >= 0f && trailing <= containerSize -> 0f
            size + 2 * marginPx > containerSize -> leading
            leading < 0f -> leading
            else -> trailing - containerSize
        }
    }
}

/** Fades content out at the top and bottom edges so partially scrolled rows look intentional. */
fun Modifier.verticalFadeEdges(top: Dp, bottom: Dp): Modifier = this
    .graphicsLayer(compositingStrategy = CompositingStrategy.Offscreen)
    .drawWithContent {
        drawContent()
        val h = size.height
        val topFraction = (top.toPx() / h).coerceIn(0f, 0.5f)
        val bottomFraction = (bottom.toPx() / h).coerceIn(0f, 0.5f)
        drawRect(
            Brush.verticalGradient(
                0f to Color.Transparent,
                topFraction to Color.Black,
                1f - bottomFraction to Color.Black,
                1f to Color.Transparent,
            ),
            blendMode = BlendMode.DstIn,
        )
    }
