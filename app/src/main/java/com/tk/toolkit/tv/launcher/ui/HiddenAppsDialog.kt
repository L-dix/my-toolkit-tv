package com.tk.toolkit.tv.launcher.ui

import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.gestures.LocalBringIntoViewSpec
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.itemsIndexed
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import androidx.tv.material3.MaterialTheme
import androidx.tv.material3.Text
import com.tk.toolkit.tv.launcher.data.LauncherApp
import com.tk.toolkit.tv.ui.theme.ToolkitColors

private const val HIDDEN_COLUMNS = 4
private val PanelShape = RoundedCornerShape(28.dp)

/** Popup listing hidden apps. Click launches; long press opens the app's actions. */
@OptIn(ExperimentalFoundationApi::class)
@Composable
fun HiddenAppsDialog(
    apps: List<LauncherApp>,
    onDismiss: () -> Unit,
    onLaunch: (LauncherApp) -> Unit,
    onLongPress: (LauncherApp) -> Unit,
) {
    val focusRequesters = remember { mutableMapOf<String, FocusRequester>() }
    val lastFocused = remember { arrayOfNulls<String>(1) }
    val scrollSpec = with(LocalDensity.current) { remember { minimalScrollSpec(16.dp.toPx()) } }

    Dialog(onDismissRequest = onDismiss, properties = DialogProperties(usePlatformDefaultWidth = false)) {
        // Focus the first app on open, and again if the focused app gets unhidden.
        LaunchedEffect(apps.map { it.packageName }) {
            val keys = apps.map { it.packageName }
            if (keys.isEmpty() || lastFocused[0] in keys) return@LaunchedEffect
            focusRequesters.getOrPut(keys.first()) { FocusRequester() }.requestFocusWhenReady()
        }
        Column(
            Modifier
                .width(800.dp)
                .clip(PanelShape)
                .background(ToolkitColors.SurfaceRaised)
                .border(1.dp, Color.White.copy(alpha = 0.08f), PanelShape)
                .padding(top = 28.dp),
        ) {
            Text(
                "Plus d'applis",
                style = MaterialTheme.typography.titleLarge,
                color = ToolkitColors.OnSurface,
                modifier = Modifier.padding(horizontal = 28.dp),
            )
            Text(
                "Masquées sur l'écran d'accueil · appui long pour les réafficher",
                style = MaterialTheme.typography.bodyMedium,
                color = ToolkitColors.OnSurfaceMuted,
                modifier = Modifier.padding(top = 4.dp, start = 28.dp, end = 28.dp),
            )
            CompositionLocalProvider(LocalBringIntoViewSpec provides scrollSpec) {
                LazyVerticalGrid(
                    columns = GridCells.Fixed(HIDDEN_COLUMNS),
                    // Room for the focused tile's glow.
                    contentPadding = PaddingValues(horizontal = 28.dp, vertical = 28.dp),
                    horizontalArrangement = Arrangement.spacedBy(20.dp),
                    verticalArrangement = Arrangement.spacedBy(24.dp),
                    modifier = Modifier.heightIn(max = 380.dp),
                ) {
                    itemsIndexed(apps, key = { _, app -> app.packageName }) { index, app ->
                        AppTile(
                            app = app,
                            index = index,
                            columns = HIDDEN_COLUMNS,
                            isMoving = false,
                            dimmed = false,
                            focusRequester = focusRequesters.getOrPut(app.packageName) { FocusRequester() },
                            onFocused = { lastFocused[0] = app.packageName },
                            onClick = { onLaunch(app) },
                            onLongClick = { onLongPress(app) },
                            onMoveBy = {},
                            onDrop = {},
                            modifier = Modifier.animateItem(),
                        )
                    }
                }
            }
        }
    }
}
