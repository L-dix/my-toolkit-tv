package com.tk.toolkit.tv.launcher.ui

import androidx.activity.compose.BackHandler
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutVertically
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.spring
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.gestures.LocalBringIntoViewSpec
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.GridItemSpan
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.lazy.grid.itemsIndexed
import androidx.compose.foundation.lazy.grid.rememberLazyGridState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.tv.material3.MaterialTheme
import androidx.tv.material3.Text
import com.tk.toolkit.tv.launcher.LauncherViewModel
import com.tk.toolkit.tv.launcher.data.AppActions
import com.tk.toolkit.tv.launcher.data.AppActions.safeStart
import com.tk.toolkit.tv.launcher.data.ContinueWatching
import com.tk.toolkit.tv.launcher.data.LauncherApp
import com.tk.toolkit.tv.ui.theme.ToolkitColors

private const val COLUMNS = 5
private const val MORE_APPS_KEY = "toolkit:more-apps"
private const val WATCH_TITLE_KEY = "toolkit:continue-watching-title"
private const val APPS_TITLE_KEY = "toolkit:apps-title"

@OptIn(ExperimentalFoundationApi::class)
@Composable
fun HomeScreen(viewModel: LauncherViewModel = viewModel()) {
    val state by viewModel.state.collectAsStateWithLifecycle()
    val context = LocalContext.current
    val gridState = rememberLazyGridState()
    val focusRequesters = remember { mutableMapOf<String, FocusRequester>() }
    fun requesterFor(key: String) = focusRequesters.getOrPut(key) { FocusRequester() }

    var menuApp by remember { mutableStateOf<LauncherApp?>(null) }
    var showHidden by remember { mutableStateOf(false) }
    var lastFocusedKey by rememberSaveable { mutableStateOf<String?>(null) }
    var lastFocusedIndex by rememberSaveable { mutableStateOf(0) }
    // Not saved: every fresh start begins on the first app.
    var initialFocusPlaced by remember { mutableStateOf(false) }

    val scrollSpec = with(LocalDensity.current) { remember { minimalScrollSpec(28.dp.toPx()) } }
    val watchItems = ContinueWatching.items
    val watchKeys = watchItems.map { it.key }
    val gridKeys = watchKeys + state.apps.map { it.packageName } +
        if (state.hiddenApps.isNotEmpty()) listOf(MORE_APPS_KEY) else emptyList()

    // This is the home screen: Back never leaves it, it only cancels a move.
    BackHandler { if (state.movingPackage != null) viewModel.endMove() }

    // Initial focus, and keeping focus in the grid when the focused tile goes away
    // (hidden, uninstalled, or "More Apps" emptied).
    LaunchedEffect(gridKeys) {
        if (!state.loaded || gridKeys.isEmpty()) return@LaunchedEffect
        // The system focuses the first tile (Continue Watching) before the apps load, so once
        // they're in, move to the first app; after that only repair focus that was lost.
        val index = if (initialFocusPlaced) {
            if (lastFocusedKey in gridKeys) return@LaunchedEffect
            lastFocusedIndex
        } else {
            watchKeys.size
        }
        initialFocusPlaced = true
        val target = gridKeys[index.coerceIn(0, gridKeys.lastIndex)]
        requesterFor(target).requestFocusWhenReady()
    }

    Box(Modifier.fillMaxSize()) {
        Column(Modifier.fillMaxSize().padding(top = 28.dp)) {
            ClockHeader(
                onOpenAudioOutput = { AppActions.openAudioOutput(context) },
                onOpenSettings = { AppActions.openSystemSettings(context) },
                modifier = Modifier.padding(horizontal = 48.dp),
            )

            CompositionLocalProvider(LocalBringIntoViewSpec provides scrollSpec) {
                LazyVerticalGrid(
                    columns = GridCells.Fixed(COLUMNS),
                    state = gridState,
                    // Generous padding so the enlarged focused/moving tile is never clipped at the edges.
                    contentPadding = PaddingValues(horizontal = 56.dp, vertical = 28.dp),
                    horizontalArrangement = Arrangement.spacedBy(20.dp),
                    verticalArrangement = Arrangement.spacedBy(24.dp),
                    modifier = Modifier
                        .fillMaxSize()
                        .verticalFadeEdges(top = 20.dp, bottom = 28.dp),
                ) {
                    if (watchItems.isNotEmpty()) {
                        item(key = WATCH_TITLE_KEY, span = { GridItemSpan(maxLineSpan) }) {
                            SectionTitle("Continue Watching", accentBar = true)
                        }
                        items(watchItems, key = { it.key }) { item ->
                            WatchTile(
                                item = item,
                                dimmed = state.movingPackage != null,
                                focusRequester = requesterFor(item.key),
                                onFocused = {
                                    lastFocusedKey = item.key
                                    lastFocusedIndex = watchKeys.indexOf(item.key)
                                },
                                onClick = { if (state.movingPackage == null) context.safeStart(item.intent()) },
                            )
                        }
                    }
                    // Full width, so it also starts the apps on their own row.
                    item(key = APPS_TITLE_KEY, span = { GridItemSpan(maxLineSpan) }) {
                        SectionTitle("Apps", accentBar = false)
                    }
                    itemsIndexed(state.apps, key = { _, app -> app.packageName }) { index, app ->
                        val isMoving = app.packageName == state.movingPackage
                        AppTile(
                            app = app,
                            index = index,
                            columns = COLUMNS,
                            isMoving = isMoving,
                            dimmed = state.movingPackage != null && !isMoving,
                            focusRequester = requesterFor(app.packageName),
                            onFocused = {
                                lastFocusedKey = app.packageName
                                lastFocusedIndex = watchKeys.size + index
                            },
                            onClick = {
                                if (state.movingPackage == null) context.safeStart(viewModel.launchIntent(app))
                            },
                            onLongClick = { if (state.movingPackage == null) menuApp = app },
                            onMoveBy = viewModel::moveBy,
                            onDrop = viewModel::endMove,
                            modifier = Modifier.animateItem(
                                fadeInSpec = null,
                                fadeOutSpec = null,
                                // Slide only while moving; hiding/unhiding applies instantly.
                                placementSpec = if (state.movingPackage != null) spring(stiffness = Spring.StiffnessMediumLow) else null,
                            ),
                        )
                    }
                    if (state.hiddenApps.isNotEmpty()) {
                        item(key = MORE_APPS_KEY) {
                            MoreAppsTile(
                                dimmed = state.movingPackage != null,
                                focusRequester = requesterFor(MORE_APPS_KEY),
                                onFocused = {
                                    lastFocusedKey = MORE_APPS_KEY
                                    lastFocusedIndex = watchKeys.size + state.apps.size
                                },
                                onClick = { if (state.movingPackage == null) showHidden = true },
                            )
                        }
                    }
                }
            }
        }

        AnimatedVisibility(
            visible = state.movingPackage != null,
            enter = fadeIn() + slideInVertically { it },
            exit = fadeOut() + slideOutVertically { it },
            modifier = Modifier
                .align(Alignment.BottomCenter)
                .padding(bottom = 24.dp),
        ) {
            MoveHint()
        }
    }

    if (showHidden && state.hiddenApps.isNotEmpty()) {
        HiddenAppsDialog(
            apps = state.hiddenApps,
            onDismiss = { showHidden = false },
            onLaunch = { app ->
                showHidden = false
                context.safeStart(viewModel.launchIntent(app))
            },
            onLongPress = { menuApp = it },
        )
    }

    menuApp?.let { app ->
        val isHidden = state.hiddenApps.any { it.packageName == app.packageName }
        val dismiss = { menuApp = null }
        AppActionsDialog(
            app = app,
            isHidden = isHidden,
            onDismiss = dismiss,
            onMove = { dismiss(); viewModel.startMove(app.packageName) },
            onToggleHidden = { dismiss(); viewModel.setHidden(app.packageName, !isHidden) },
            onAppInfo = { dismiss(); AppActions.openAppInfo(context, app.packageName) },
            onForceStop = { dismiss(); AppActions.forceStop(context, app.packageName) },
            onUninstall = { dismiss(); AppActions.uninstall(context, app.packageName) },
        )
    }
}

@Composable
private fun SectionTitle(text: String, accentBar: Boolean) {
    // Sits closer to its row than the grid's row spacing would put it.
    Row(Modifier.offset(y = 8.dp), verticalAlignment = Alignment.CenterVertically) {
        if (accentBar) {
            Spacer(
                Modifier
                    .size(width = 4.dp, height = 16.dp)
                    .background(
                        Brush.verticalGradient(listOf(ToolkitColors.Accent, ToolkitColors.GlowB)),
                        RoundedCornerShape(50),
                    ),
            )
            Spacer(Modifier.width(10.dp))
        }
        Text(
            text,
            style = MaterialTheme.typography.titleLarge.copy(fontSize = 18.sp),
            color = ToolkitColors.OnSurface,
        )
    }
}

@Composable
private fun MoveHint() {
    val shape = RoundedCornerShape(50)
    Text(
        "Use the arrows to move  ·  Press OK to drop",
        style = MaterialTheme.typography.bodyMedium,
        color = ToolkitColors.OnSurface,
        modifier = Modifier
            .background(ToolkitColors.SurfaceRaised.copy(alpha = 0.95f), shape)
            .border(1.dp, Color.White.copy(alpha = 0.1f), shape)
            .padding(horizontal = 22.dp, vertical = 12.dp),
    )
}
