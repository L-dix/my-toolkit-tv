package com.tk.toolkit.tv.launcher.ui

import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.relocation.BringIntoViewRequester
import androidx.compose.foundation.relocation.bringIntoViewRequester
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.focus.onFocusChanged
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.tv.material3.Border
import androidx.tv.material3.Card
import androidx.tv.material3.CardDefaults
import androidx.tv.material3.MaterialTheme
import androidx.tv.material3.Text
import com.tk.toolkit.tv.launcher.data.WatchItem
import com.tk.toolkit.tv.ui.theme.ToolkitColors
import androidx.compose.ui.graphics.Color

private val TileShape = RoundedCornerShape(14.dp)

/** Continue Watching entry, shaped exactly like an app tile. */
@Composable
fun WatchTile(
    item: WatchItem,
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
    val tileAlpha by animateFloatAsState(if (dimmed) 0.35f else 1f, label = "dim")
    val glowAlpha by animateFloatAsState(if (focused && !dimmed) 1f else 0f, label = "glow")

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
            .accentGlow(item.glow, glowAlpha)
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
            border = CardDefaults.border(focusedBorder = if (isMoving) movingBorder() else Border.None),
            colors = CardDefaults.colors(containerColor = ToolkitColors.Surface),
        ) {
            Box(Modifier.fillMaxSize()) {
                if (item.caption == null) {
                    Image(
                        painterResource(item.artwork),
                        contentDescription = item.title,
                        contentScale = ContentScale.Crop,
                        modifier = Modifier.fillMaxSize(),
                    )
                } else {
                    LogoWithCaption(item)
                }
                if (isMoving) MoveOverlay()
            }
        }
    }
}

/**
 * Logo-only artwork (logo on black) scaled down on a black tile, with the caption set under
 * the wordmark like a subtitle.
 */
@Composable
private fun LogoWithCaption(item: WatchItem) {
    Box(Modifier.fillMaxSize().background(Color.Black)) {
        Image(
            painterResource(item.artwork),
            contentDescription = item.title,
            contentScale = ContentScale.Fit,
            modifier = Modifier
                .fillMaxWidth(0.8f)
                .align(Alignment.Center)
                .offset(y = (-8).dp),
        )
        Text(
            item.caption.orEmpty().uppercase(),
            style = MaterialTheme.typography.labelLarge.copy(fontSize = 12.sp, letterSpacing = 3.sp),
            color = Color.White.copy(alpha = 0.72f),
            modifier = Modifier
                .align(Alignment.BottomCenter)
                .padding(bottom = 16.dp),
        )
    }
}
