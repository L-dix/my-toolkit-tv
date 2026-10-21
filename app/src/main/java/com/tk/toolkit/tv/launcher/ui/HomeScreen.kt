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
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.key.KeyEventType
import androidx.compose.ui.input.key.onPreviewKeyEvent
import androidx.compose.ui.input.key.type
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
import com.tk.toolkit.tv.launcher.data.HomeDefaults
import com.tk.toolkit.tv.launcher.data.LauncherApp
import com.tk.toolkit.tv.launcher.data.Shortcuts
import com.tk.toolkit.tv.ui.theme.ToolkitColors
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

private const val COLUMNS = 5
private const val MORE_APPS_KEY = "toolkit:more-apps"
private const val WATCH_TITLE_KEY = "toolkit:continue-watching-title"
private const val APPS_TITLE_KEY = "toolkit:apps-title"

/** The pending "held long enough" timer of a held digit key. */
private class HoldTimer(var job: Job? = null)

@OptIn(ExperimentalFoundationApi::class)
@Composable
fun HomeScreen(viewModel: LauncherViewModel = viewModel()) {
    val state by viewModel.state.collectAsStateWithLifecycle()
    val rainbow by viewModel.rainbow.collectAsStateWithLifecycle()
    val weather by viewModel.weather.collectAsStateWithLifecycle()
    val clockFont by viewModel.clockFont.collectAsStateWithLifecycle()
    val border by viewModel.border.collectAsStateWithLifecycle()
    val keyHold = remember { HoldTimer() }
    val context = LocalContext.current
    val gridState = rememberLazyGridState()
    val scope = rememberCoroutineScope()
    val focusRequesters = remember { mutableMapOf<String, FocusRequester>() }
    fun requesterFor(key: String) = focusRequesters.getOrPut(key) { FocusRequester() }

    var menuApp by remember { mutableStateOf<LauncherApp?>(null) }
    var menuPinnedApp by remember { mutableStateOf<LauncherApp?>(null) }
    var showHidden by remember { mutableStateOf(false) }
    var showSort by remember { mutableStateOf(false) }
    var showOptions by remember { mutableStateOf(false) }
    var lastFocusedKey by rememberSaveable { mutableStateOf<String?>(null) }
    var lastFocusedIndex by rememberSaveable { mutableStateOf(0) }
    // Not saved: every fresh start begins on the first app.
    var initialFocusPlaced by remember { mutableStateOf(false) }

    val scrollSpec = with(LocalDensity.current) { remember { minimalScrollSpec(28.dp.toPx()) } }
    val pinnedApps = state.pinnedApps
    val watchKeys = pinnedApps.map { ContinueWatching.key(it.packageName) }
    val gridKeys = watchKeys + state.apps.map { it.packageName } +
        if (state.hiddenApps.isNotEmpty()) listOf(MORE_APPS_KEY) else emptyList()

    // This is the home screen: Back never leaves it, it only cancels a move.
    BackHandler { if (state.movingKey != null) viewModel.endMove() }

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

    // Held digit keys: 1 opens the Kick channel, 3 the YouTube playlist, and 2 (easter egg)
    // turns the rainbow border on or off; how long to hold each is set in Toolkit's options.
    val heldDigits = Modifier.onPreviewKeyEvent { event ->
        val shortcuts = viewModel.shortcuts.value
        val (holdMs, action) = when (event.nativeKeyEvent.keyCode) {
            android.view.KeyEvent.KEYCODE_1, android.view.KeyEvent.KEYCODE_NUMPAD_1 -> shortcuts.kickHoldMs to {
                val channel = viewModel.shortcuts.value.kickChannel
                AppActions.openInApp(context, viewModel.packageOf(HomeDefaults.KICK), Shortcuts.kickLinks(channel), "Kick")
            }
            android.view.KeyEvent.KEYCODE_3, android.view.KeyEvent.KEYCODE_NUMPAD_3 -> shortcuts.playlistHoldMs to {
                val playlist = viewModel.shortcuts.value.youtubePlaylist
                AppActions.openInApp(context, viewModel.packageOf(HomeDefaults.YOUTUBE), Shortcuts.youtubeLinks(playlist), "YouTube")
            }
            android.view.KeyEvent.KEYCODE_2, android.view.KeyEvent.KEYCODE_NUMPAD_2 -> shortcuts.rainbowHoldMs to {
                viewModel.toggleRainbow()
            }
            else -> return@onPreviewKeyEvent false
        }
        when (event.type) {
            KeyEventType.KeyDown -> if (event.nativeKeyEvent.repeatCount == 0) {
                keyHold.job?.cancel()
                keyHold.job = scope.launch {
                    delay(holdMs)
                    action()
                }
            }
            KeyEventType.KeyUp -> keyHold.job?.cancel()
        }
        true
    }

    Box(Modifier.fillMaxSize().then(heldDigits)) {
        Column(Modifier.fillMaxSize().padding(top = 28.dp)) {
            ClockHeader(
                font = clockFont,
                weather = weather,
                onOpenOptions = { if (state.movingKey == null) showOptions = true },
                onSortApps = { if (state.movingKey == null) showSort = true },
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
                    // Hidden when nothing is pinned.
                    if (pinnedApps.isNotEmpty()) {
                        item(key = WATCH_TITLE_KEY, span = { GridItemSpan(maxLineSpan) }) {
                            SectionTitle("Continuer à regarder", accentBar = true)
                        }
                        itemsIndexed(pinnedApps, key = { _, app -> ContinueWatching.key(app.packageName) }) { index, app ->
                            val key = ContinueWatching.key(app.packageName)
                            val isMoving = key == state.movingKey
                            AppTile(
                                app = app,
                                index = index,
                                columns = COLUMNS,
                                isMoving = isMoving,
                                dimmed = state.movingKey != null && !isMoving,
                                focusRequester = requesterFor(key),
                                onFocused = {
                                    lastFocusedKey = key
                                    lastFocusedIndex = watchKeys.indexOf(key)
                                    // The first row brings its section title into view too.
                                    if (index < COLUMNS && gridState.canScrollBackward) {
                                        scope.launch { gridState.animateScrollToItem(0) }
                                    }
                                },
                                onClick = {
                                    if (state.movingKey == null) context.safeStart(viewModel.launchIntent(app))
                                },
                                onLongClick = { if (state.movingKey == null) menuPinnedApp = app },
                                onMoveBy = viewModel::moveBy,
                                onDrop = viewModel::endMove,
                                modifier = Modifier.animateItem(
                                    fadeInSpec = null,
                                    fadeOutSpec = null,
                                    placementSpec = if (state.movingKey != null) spring(stiffness = Spring.StiffnessMediumLow) else null,
                                ),
                            )
                        }
                    }
                    // Full width, so it also starts the apps on their own row.
                    item(key = APPS_TITLE_KEY, span = { GridItemSpan(maxLineSpan) }) {
                        SectionTitle("Applications", accentBar = false)
                    }
                    itemsIndexed(state.apps, key = { _, app -> app.packageName }) { index, app ->
                        val isMoving = app.packageName == state.movingKey
                        AppTile(
                            app = app,
                            index = index,
                            columns = COLUMNS,
                            isMoving = isMoving,
                            dimmed = state.movingKey != null && !isMoving,
                            focusRequester = requesterFor(app.packageName),
                            onFocused = {
                                lastFocusedKey = app.packageName
                                lastFocusedIndex = watchKeys.size + index
                            },
                            onClick = {
                                if (state.movingKey == null) context.safeStart(viewModel.launchIntent(app))
                            },
                            onLongClick = { if (state.movingKey == null) menuApp = app },
                            onMoveBy = viewModel::moveBy,
                            onDrop = viewModel::endMove,
                            modifier = Modifier.animateItem(
                                fadeInSpec = null,
                                fadeOutSpec = null,
                                // Slide only while moving; hiding/unhiding applies instantly.
                                placementSpec = if (state.movingKey != null) spring(stiffness = Spring.StiffnessMediumLow) else null,
                            ),
                        )
                    }
                    if (state.hiddenApps.isNotEmpty()) {
                        item(key = MORE_APPS_KEY) {
                            MoreAppsTile(
                                dimmed = state.movingKey != null,
                                focusRequester = requesterFor(MORE_APPS_KEY),
                                onFocused = {
                                    lastFocusedKey = MORE_APPS_KEY
                                    lastFocusedIndex = watchKeys.size + state.apps.size
                                },
                                onClick = { if (state.movingKey == null) showHidden = true },
                            )
                        }
                    }
                }
            }
        }

        if (rainbow) RainbowBorder(border)

        AnimatedVisibility(
            visible = state.movingKey != null,
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

    if (showOptions) {
        val shortcuts by viewModel.shortcuts.collectAsStateWithLifecycle()
        OptionsDialog(
            settings = shortcuts,
            clockFont = clockFont,
            rainbowOn = rainbow,
            border = border,
            onDismiss = { showOptions = false },
            onClockFont = viewModel::setClockFont,
            onShortcuts = viewModel::setShortcuts,
            onToggleRainbow = viewModel::toggleRainbow,
            onBorder = viewModel::setBorder,
        )
    }

    if (showSort) {
        SortAppsDialog(
            onDismiss = { showSort = false },
            onSortDefault = { showSort = false; viewModel.sortApps(alphabetical = false) },
            onSortAlphabetical = { showSort = false; viewModel.sortApps(alphabetical = true) },
        )
    }

    menuPinnedApp?.let { app ->
        WatchActionsDialog(
            app = app,
            onDismiss = { menuPinnedApp = null },
            onMove = { menuPinnedApp = null; viewModel.startMove(ContinueWatching.key(app.packageName)) },
            onRemove = { menuPinnedApp = null; viewModel.setPinned(app.packageName, false) },
        )
    }

    menuApp?.let { app ->
        val isHidden = state.hiddenApps.any { it.packageName == app.packageName }
        val isPinned = state.pinnedApps.any { it.packageName == app.packageName }
        val dismiss = { menuApp = null }
        AppActionsDialog(
            app = app,
            isHidden = isHidden,
            isPinned = isPinned,
            onDismiss = dismiss,
            onMove = { dismiss(); viewModel.startMove(app.packageName) },
            onToggleHidden = { dismiss(); viewModel.setHidden(app.packageName, !isHidden) },
            onTogglePinned = { dismiss(); viewModel.setPinned(app.packageName, !isPinned) },
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
        "Flèches pour déplacer  ·  OK pour valider",
        style = MaterialTheme.typography.bodyMedium,
        color = ToolkitColors.OnSurface,
        modifier = Modifier
            .background(ToolkitColors.SurfaceRaised.copy(alpha = 0.95f), shape)
            .border(1.dp, Color.White.copy(alpha = 0.1f), shape)
            .padding(horizontal = 22.dp, vertical = 12.dp),
    )
}
