package com.tk.toolkit.tv.launcher

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.tk.toolkit.tv.launcher.data.AppRepository
import com.tk.toolkit.tv.launcher.data.ContinueWatching
import com.tk.toolkit.tv.launcher.data.LauncherApp
import com.tk.toolkit.tv.launcher.data.LauncherPrefs
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
    /** Installed apps pinned to Continue Watching, in the user's order. */
    val pinnedApps: List<LauncherApp> = emptyList(),
    val loaded: Boolean = false,
    /** App package or pinned app's Continue Watching key currently being repositioned, if any. */
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
            _state.update { current ->
                // Don't reshuffle under the user while they're moving something.
                if (current.movingKey != null) current
                else split(all).copy(loaded = true)
            }
        }
    }

    private fun split(all: List<LauncherApp>): LauncherState {
        val hidden = prefs.hiddenPackages()
        val (hiddenApps, visible) = all.partition { it.packageName in hidden }
        // Pinned apps stay pinned while uninstalled, and come back if reinstalled.
        val byPackage = all.associateBy { it.packageName }
        return LauncherState(
            apps = visible,
            hiddenApps = hiddenApps.sortedBy { it.label.lowercase() },
            pinnedApps = prefs.pinnedPackages().mapNotNull { byPackage[it] },
        )
    }

    /** Re-splits the loaded apps after a hide or pin change, keeping the rest of the state. */
    private fun resplit() = _state.update { current ->
        split(prefs.sort(current.apps + current.hiddenApps))
            .copy(loaded = current.loaded, movingKey = current.movingKey)
    }

    fun launchIntent(app: LauncherApp) = repository.launchIntent(app)

    /** Starts moving an app (by package) or a pinned app (by its Continue Watching key). */
    fun startMove(key: String) = _state.update { it.copy(movingKey = key) }

    /** Shifts the moving tile by [delta] positions within its own section, clamped to its bounds. */
    fun moveBy(delta: Int) = _state.update { current ->
        val key = current.movingKey ?: return@update current
        val appIndex = current.apps.indexOfFirst { it.packageName == key }
        if (appIndex >= 0) return@update current.copy(apps = current.apps.shifted(appIndex, delta))
        val pinIndex = current.pinnedApps.indexOfFirst { ContinueWatching.key(it.packageName) == key }
        if (pinIndex >= 0) return@update current.copy(pinnedApps = current.pinnedApps.shifted(pinIndex, delta))
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
        resplit()
    }

    fun setPinned(packageName: String, pinned: Boolean) {
        prefs.setPinned(packageName, pinned)
        resplit()
    }

    private fun saveOrder() {
        val current = _state.value
        prefs.saveOrder((current.apps + current.hiddenApps).map { it.packageName })
        // Keeps pinned apps that aren't installed right now after the visible ones.
        val visible = current.pinnedApps.map { it.packageName }
        prefs.savePinned(visible + prefs.pinnedPackages().filter { it !in visible })
    }
}
