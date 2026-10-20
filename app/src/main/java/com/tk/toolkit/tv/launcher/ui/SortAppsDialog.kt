package com.tk.toolkit.tv.launcher.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Dialog
import androidx.tv.material3.MaterialTheme
import androidx.tv.material3.Text
import com.tk.toolkit.tv.ui.theme.ToolkitColors

private val PanelShape = RoundedCornerShape(24.dp)

/** Header menu that re-sorts the whole app grid; styled like the app menu. */
@Composable
fun SortAppsDialog(onDismiss: () -> Unit, onSortDefault: () -> Unit, onSortAlphabetical: () -> Unit) {
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
            Column(Modifier.padding(start = 28.dp, end = 28.dp, top = 22.dp)) {
                Text("Trier les applis", style = MaterialTheme.typography.headlineSmall, color = ToolkitColors.OnSurface)
                Text(
                    "Remplace l'ordre actuel",
                    style = MaterialTheme.typography.bodyMedium,
                    color = ToolkitColors.OnSurfaceMuted,
                )
            }
            Column(Modifier.padding(12.dp)) {
                ActionItem(
                    "Streaming d'abord",
                    "YouTube, Kick, HBO Max, Apple TV… puis de A à Z",
                    onSortDefault,
                    Modifier.focusRequester(firstAction),
                )
                ActionItem("Ordre alphabétique", "Toutes les applis de A à Z", onSortAlphabetical)
            }
        }
    }
}
