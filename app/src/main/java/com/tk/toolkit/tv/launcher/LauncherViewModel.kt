package com.tk.toolkit.tv.launcher

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.tk.toolkit.tv.launcher.data.AppRepository
import com.tk.toolkit.tv.launcher.data.ContinueWatching
import com.tk.toolkit.tv.launcher.data.LauncherApp
import com.tk.toolkit.tv.launcher.data.LauncherPrefs
import com.tk.toolkit.tv.launcher.data.WatchItem
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

data class LauncherState(
    /** Apps shown on the home grid, in the user's order. */
    val apps: List<LauncherApp> = emptyList(),
    /** Apps the user hid, shown under "More Apps", alphabetically. */
    val hiddenApps: List<LauncherApp> = emptyList(),
    /** Continue Watching items whose app is installed and that the user hasn't removed. */
    val watchItems: List<WatchItem> = emptyList(),
    val loaded: Boolean = false,
    /** App package or Continue Watching key currently being repositioned, if any. */
    val movingKey: String? = null,
)

class LauncherViewModel(application: Application) : AndroidViewModel(application) {

    private val repository = AppRepository(application)
    private val prefs = LauncherPrefs(application)

    private val _state = MutableStateFlow(LauncherState())
    val state: StateFlow<LauncherState> = _state.asStateFlow()

    private var refreshJob: Job? = null
    private val refreshDispatcher = Dispatchers.IO.limitedParallelism(1)

    init {
        refresh()
    }

    fun refresh() {
        refreshJob?.cancel()
        refreshJob = viewModelScope.launch {
            val all = withContext(refreshDispatcher) { prefs.sort(repository.loadApps()) }
            val watch = withContext(refreshDispatcher) { visibleWatchItems() }
            _state.update { current ->
                // Don't reshuffle under the user while they're moving something.
                if (current.movingKey != null) current
                else split(all).copy(loaded = true, watchItems = watch)
            }
        }
    }

    private fun split(all: List<LauncherApp>): LauncherState {
        val hidden = prefs.hiddenPackages()
        val (hiddenApps, visible) = all.partition { it.packageName in hidden }
        return LauncherState(apps = visible, hiddenApps = hiddenApps.sortedBy { it.label.lowercase() })
    }

    private fun visibleWatchItems(): List<WatchItem> {
        val removed = prefs.removedWatchItems()
        return prefs.sortWatchItems(ContinueWatching.items)
            .filter { it.key !in removed && repository.isInstalled(it.packageName) }
    }

    fun removeWatchItem(item: WatchItem) {
        if (!item.removable) return
        prefs.removeWatchItem(item.key)
        _state.update { current -> current.copy(watchItems = current.watchItems - item) }
    }

    fun launchIntent(app: LauncherApp) = repository.launchIntent(app)

    /** Starts moving an app (by package) or a Continue Watching item (by key). */
    fun startMove(key: String) = _state.update { it.copy(movingKey = key) }

    /** Shifts the moving tile by [delta] positions within its own section, clamped to its bounds. */
    fun moveBy(delta: Int) = _state.update { current ->
        val key = current.movingKey ?: return@update current
        val appIndex = current.apps.indexOfFirst { it.packageName == key }
        if (appIndex >= 0) return@update current.copy(apps = current.apps.shifted(appIndex, delta))
        val watchIndex = current.watchItems.indexOfFirst { it.key == key }
        if (watchIndex >= 0) return@update current.copy(watchItems = current.watchItems.shifted(watchIndex, delta))
        current
    }

    private fun <T> List<T>.shifted(from: Int, delta: Int): List<T> {
        val to = (from + delta).coerceIn(0, lastIndex)
        return if (to == from) this else toMutableList().apply { add(to, removeAt(from)) }
    }

    fun endMove() {
        _state.update { it.copy(movingKey = null) }
        saveOrder()
    }

    fun setHidden(packageName: String, hidden: Boolean) {
        prefs.setHidden(packageName, hidden)
        // Unhidden apps return to their saved position.
        _state.update { current ->
            split(prefs.sort(current.apps + current.hiddenApps))
                .copy(loaded = current.loaded, movingKey = current.movingKey, watchItems = current.watchItems)
        }
    }

    private fun saveOrder() {
        val current = _state.value
        prefs.saveOrder((current.apps + current.hiddenApps).map { it.packageName })
        // Keeps removed and not-installed items' positions after the visible ones.
        val visible = current.watchItems.map { it.key }
        prefs.saveWatchOrder(visible + prefs.sortWatchItems(ContinueWatching.items).map { it.key }.filter { it !in visible })
    }
}
