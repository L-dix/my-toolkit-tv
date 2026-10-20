package com.tk.toolkit.tv.launcher

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.tk.toolkit.tv.launcher.data.AppRepository
import com.tk.toolkit.tv.launcher.data.ContinueWatching
import com.tk.toolkit.tv.launcher.data.HomeDefaults
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

    private val _rainbow = MutableStateFlow(prefs.rainbowBorder)

    /** Easter egg: rainbow border around the home screen, toggled by holding 2 for 4 seconds. */
    val rainbow: StateFlow<Boolean> = _rainbow.asStateFlow()

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
            pinnedApps = pinnedPackages(all).mapNotNull { byPackage[it] },
        )
    }

    /** The user's pins, or the default ones until the user changes them. */
    private fun pinnedPackages(all: List<LauncherApp>): List<String> =
        prefs.pinnedPackages() ?: HomeDefaults.resolve(HomeDefaults.PINNED, all)

    /** Re-splits the loaded apps after a hide or pin change, keeping the rest of the state. */
    private fun resplit() = _state.update { current ->
        split(prefs.sort(current.apps + current.hiddenApps))
            .copy(loaded = current.loaded, movingKey = current.movingKey)
    }

    fun toggleRainbow() {
        val on = !_rainbow.value
        _rainbow.value = on
        prefs.rainbowBorder = on
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
        val moved = _state.value.movingKey
        _state.update { it.copy(movingKey = null) }
        saveOrder(moved)
    }

    fun setHidden(packageName: String, hidden: Boolean) {
        prefs.setHidden(packageName, hidden)
        // Unhidden apps return to their saved position.
        resplit()
    }

    /** Replaces the user's app order: streaming apps first (the default), or all A to Z. */
    fun sortApps(alphabetical: Boolean) {
        val current = _state.value
        if (current.movingKey != null) return
        val all = current.apps + current.hiddenApps
        val sorted = if (alphabetical) all.sortedBy { it.label.lowercase() } else prefs.defaultOrder(all)
        prefs.saveOrder(sorted.map { it.packageName })
        resplit()
    }

    /** Newly pinned apps go to the end of Continue Watching. */
    fun setPinned(packageName: String, pinned: Boolean) {
        val current = _state.value
        val others = pinnedPackages(current.apps + current.hiddenApps) - packageName
        prefs.savePinned(if (pinned) others + packageName else others)
        resplit()
    }

    private fun saveOrder(movedKey: String?) {
        val current = _state.value
        prefs.saveOrder((current.apps + current.hiddenApps).map { it.packageName })
        // Only a moved pin saves the pins, so the defaults stay live (e.g. pin Kick once it's
        // installed) until the user actually changes Continue Watching.
        if (current.pinnedApps.none { ContinueWatching.key(it.packageName) == movedKey }) return
        // Keeps pinned apps that aren't installed right now after the visible ones.
        val visible = current.pinnedApps.map { it.packageName }
        prefs.savePinned(visible + prefs.pinnedPackages().orEmpty().filter { it !in visible })
    }
}
