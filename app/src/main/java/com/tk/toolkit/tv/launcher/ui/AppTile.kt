package com.tk.toolkit.tv.launcher.ui

import android.view.KeyEvent
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.relocation.BringIntoViewRequester
import androidx.compose.foundation.relocation.bringIntoViewRequester
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.OpenWith
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.focus.onFocusChanged
import android.graphics.BlurMaskFilter
import androidx.compose.ui.draw.drawWithCache
import androidx.compose.ui.graphics.drawscope.drawIntoCanvas
import androidx.compose.ui.graphics.nativeCanvas
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.toArgb
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.key.KeyEventType
import androidx.compose.ui.input.key.onPreviewKeyEvent
import androidx.compose.ui.input.key.type
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.unit.dp
import androidx.tv.material3.Border
import androidx.tv.material3.Card
import androidx.tv.material3.CardDefaults
import androidx.tv.material3.Icon
import com.tk.toolkit.tv.launcher.data.LauncherApp
import com.tk.toolkit.tv.ui.theme.ToolkitColors

private val TileShape = RoundedCornerShape(14.dp)
private val IconTileTop = Color(0xFF9E0E22)
private val IconTileBottom = Color(0xFF3A030B)

@Composable
fun AppTile(
    app: LauncherApp,
    index: Int,
    columns: Int,
    isMoving: Boolean,
    dimmed: Boolean,
    focusRequester: FocusRequester,
    onFocused: () -> Unit,
    onClick: () -> Unit,
    onLongClick: () -> Unit,
    onMoveBy: (Int) -> Unit,
    onDrop: () -> Unit,
    modifier: Modifier = Modifier,
) {
    var focused by remember { mutableStateOf(false) }
    val bringIntoView = remember { BringIntoViewRequester() }
    val glow = remember(app.accent) { glowColor(app.accent) }
    val glowAlpha by animateFloatAsState(if (focused && !dimmed) 1f else 0f, label = "glow")
    val tileAlpha by animateFloatAsState(if (dimmed) 0.35f else 1f, label = "dim")

    // Keep the tile being moved focused and on screen as it changes position.
    LaunchedEffect(isMoving, index) {
        if (isMoving) {
            runCatching { focusRequester.requestFocus() }
            bringIntoView.bringIntoView()
        }
    }

    Box(
        modifier
            .alpha(tileAlpha)
            .accentGlow(glow, glowAlpha)
            .moveKeys(isMoving, columns, onMoveBy, onDrop),
    ) {
        Card(
            onClick = onClick,
            onLongClick = onLongClick,
            modifier = Modifier
                .fillMaxWidth()
                .aspectRatio(16f / 9f)
                .bringIntoViewRequester(bringIntoView)
                .focusRequester(focusRequester)
                .onFocusChanged {
                    focused = it.isFocused
                    if (it.isFocused) onFocused()
                },
            shape = CardDefaults.shape(TileShape),
            scale = CardDefaults.scale(focusedScale = if (isMoving) 1.14f else 1.08f),
            border = CardDefaults.border(
                // Focus is shown by the glow alone; the border only marks a tile being moved.
                focusedBorder = if (isMoving) movingBorder() else Border.None,
            ),
            colors = CardDefaults.colors(containerColor = ToolkitColors.Surface),
        ) {
            Box(Modifier.fillMaxSize()) {
                if (app.banner != null) {
                    Image(
                        app.banner,
                        contentDescription = app.label,
                        contentScale = ContentScale.Crop,
                        modifier = Modifier.fillMaxSize(),
                    )
                } else {
                    IconTileContent(app)
                }
                if (isMoving) MoveOverlay()
            }
        }
    }
}

/** Tile for apps without a TV banner: icon over a deep red gradient, like Toolkit's own icon. */
@Composable
private fun IconTileContent(app: LauncherApp) {
    Box(
        Modifier
            .fillMaxSize()
            .background(Brush.linearGradient(listOf(IconTileTop, IconTileBottom))),
        contentAlignment = Alignment.Center,
    ) {
        Image(
            app.icon,
            contentDescription = app.label,
            modifier = Modifier
                .size(48.dp)
                .clip(RoundedCornerShape(12.dp)),
        )
    }
}

/** While [isMoving], the arrows move the tile by one position or one row and OK drops it. */
internal fun Modifier.moveKeys(isMoving: Boolean, columns: Int, onMoveBy: (Int) -> Unit, onDrop: () -> Unit) =
    onPreviewKeyEvent { event ->
        if (!isMoving) return@onPreviewKeyEvent false
        val down = event.type == KeyEventType.KeyDown
        when (event.nativeKeyEvent.keyCode) {
            KeyEvent.KEYCODE_DPAD_LEFT -> { if (down) onMoveBy(-1); true }
            KeyEvent.KEYCODE_DPAD_RIGHT -> { if (down) onMoveBy(1); true }
            KeyEvent.KEYCODE_DPAD_UP -> { if (down) onMoveBy(-columns); true }
            KeyEvent.KEYCODE_DPAD_DOWN -> { if (down) onMoveBy(columns); true }
            KeyEvent.KEYCODE_DPAD_CENTER, KeyEvent.KEYCODE_ENTER, KeyEvent.KEYCODE_NUMPAD_ENTER -> {
                if (event.type == KeyEventType.KeyUp) onDrop()
                true
            }
            else -> false
        }
    }

internal fun movingBorder() = Border(BorderStroke(3.dp, ToolkitColors.Accent), shape = TileShape)

@Composable
internal fun MoveOverlay() {
    Box(
        Modifier
            .fillMaxSize()
            .background(Color.Black.copy(alpha = 0.5f)),
        contentAlignment = Alignment.Center,
    ) {
        Icon(Icons.Outlined.OpenWith, null, tint = Color.White, modifier = Modifier.size(32.dp))
    }
}

/** The app's accent pushed bright enough to read as light on a dark background. */
private fun glowColor(accent: Color): Color {
    val hsv = FloatArray(3)
    android.graphics.Color.colorToHSV(accent.toArgb(), hsv)
    hsv[1] = hsv[1].coerceAtMost(0.85f)
    hsv[2] = hsv[2].coerceAtLeast(0.95f)
    return Color(android.graphics.Color.HSVToColor(hsv))
}

/** Soft halo in [color] that follows the tile's rounded shape, drawn only while [alpha] > 0. */
internal fun Modifier.accentGlow(color: Color, alpha: Float) = drawWithCache {
    // Matches the focused card's 1.08 scale plus a little spread.
    val spread = size.width * 0.04f + 6.dp.toPx()
    val radius = 14.dp.toPx() * 1.08f + spread / 2
    val paint = android.graphics.Paint(android.graphics.Paint.ANTI_ALIAS_FLAG).apply {
        maskFilter = BlurMaskFilter(26.dp.toPx(), BlurMaskFilter.Blur.NORMAL)
    }
    onDrawBehind {
        if (alpha <= 0f) return@onDrawBehind
        paint.color = color.copy(alpha = 0.4f * alpha).toArgb()
        drawIntoCanvas {
            it.nativeCanvas.drawRoundRect(
                -spread, -spread, size.width + spread, size.height + spread, radius, radius, paint,
            )
        }
    }
}
