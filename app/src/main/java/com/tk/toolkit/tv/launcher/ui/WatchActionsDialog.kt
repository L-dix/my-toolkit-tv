package com.tk.toolkit.tv.launcher.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Dialog
import androidx.tv.material3.MaterialTheme
import androidx.tv.material3.Text
import com.tk.toolkit.tv.launcher.data.WatchItem
import com.tk.toolkit.tv.ui.theme.ToolkitColors

private val PanelShape = RoundedCornerShape(24.dp)

/** Long-press menu for a Continue Watching item; styled like the app menu. */
@Composable
fun WatchActionsDialog(item: WatchItem, onDismiss: () -> Unit, onMove: () -> Unit, onRemove: () -> Unit) {
    val firstAction = remember { FocusRequester() }
    LaunchedEffect(Unit) { runCatching { firstAction.requestFocus() } }

    Dialog(onDismissRequest = onDismiss) {
        Column(
            Modifier
                .width(400.dp)
                .clip(PanelShape)
                .background(ToolkitColors.SurfaceRaised)
                .border(1.dp, Color.White.copy(alpha = 0.08f), PanelShape)
                .ignoreHeldSelectKey(),
        ) {
            Column(
                Modifier
                    .fillMaxWidth()
                    .background(Brush.verticalGradient(listOf(item.glow.copy(alpha = 0.24f), item.glow.copy(alpha = 0.05f))))
                    .drawBehind {
                        drawLine(
                            Color.White.copy(alpha = 0.1f),
                            Offset(0f, size.height),
                            Offset(size.width, size.height),
                            strokeWidth = 1.dp.toPx(),
                        )
                    }
                    .padding(horizontal = 28.dp, vertical = 22.dp),
            ) {
                Text(
                    item.title,
                    style = MaterialTheme.typography.headlineSmall,
                    color = ToolkitColors.OnSurface,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                )
                Text("Continue Watching", style = MaterialTheme.typography.bodyMedium, color = ToolkitColors.OnSurfaceMuted)
            }
            Column(Modifier.padding(12.dp)) {
                ActionItem("Move", "Reorder in Continue Watching", onMove, Modifier.focusRequester(firstAction))
                if (item.removable) ActionItem("Remove", "Take it off the home screen", onRemove, danger = true)
            }
        }
    }
}
