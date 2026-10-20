package com.tk.toolkit.tv.launcher.ui

import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Apps
import androidx.compose.runtime.Composable
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
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.unit.dp
import androidx.tv.material3.Border
import androidx.tv.material3.Card
import androidx.tv.material3.CardDefaults
import androidx.tv.material3.Icon
import androidx.tv.material3.MaterialTheme
import androidx.tv.material3.Text
import com.tk.toolkit.tv.ui.theme.ToolkitColors

private val TileShape = RoundedCornerShape(14.dp)

/** Home grid entry that opens the hidden apps. */
@Composable
fun MoreAppsTile(
    dimmed: Boolean,
    focusRequester: FocusRequester,
    onFocused: () -> Unit,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    var focused by remember { mutableStateOf(false) }
    val tileAlpha by animateFloatAsState(if (dimmed) 0.35f else 1f, label = "dim")
    val glowAlpha by animateFloatAsState(if (focused && !dimmed) 1f else 0f, label = "glow")

    Box(modifier.alpha(tileAlpha).accentGlow(ToolkitColors.Accent, glowAlpha)) {
        Card(
            onClick = onClick,
            modifier = Modifier
                .fillMaxWidth()
                .aspectRatio(16f / 9f)
                .focusRequester(focusRequester)
                .onFocusChanged {
                    focused = it.isFocused
                    if (it.isFocused) onFocused()
                },
            shape = CardDefaults.shape(TileShape),
            scale = CardDefaults.scale(focusedScale = 1.08f),
            border = CardDefaults.border(
                border = Border(BorderStroke(1.dp, ToolkitColors.OnSurface.copy(alpha = 0.1f)), shape = TileShape),
                focusedBorder = Border.None,
            ),
            colors = CardDefaults.colors(containerColor = ToolkitColors.Surface),
        ) {
            Box(
                Modifier
                    .fillMaxSize()
                    .background(
                        Brush.linearGradient(listOf(ToolkitColors.SurfaceRaised, ToolkitColors.Surface))
                    ),
                contentAlignment = Alignment.Center,
            ) {
                Column(
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.spacedBy(4.dp),
                ) {
                    Icon(
                        Icons.Outlined.Apps,
                        contentDescription = null,
                        tint = ToolkitColors.OnSurface,
                        modifier = Modifier.size(28.dp),
                    )
                    Text(
                        "Plus d'applis",
                        style = MaterialTheme.typography.bodyMedium,
                        color = ToolkitColors.OnSurface,
                    )
                }
            }
        }
    }
}
