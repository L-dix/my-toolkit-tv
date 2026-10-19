package com.tk.toolkit.tv.launcher

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.tk.toolkit.tv.launcher.data.AppRepository
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
    val loaded: Boolean = false,
    /** Package currently being repositioned, if any. */
    val movingPackage: String? = null,
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
                if (current.movingPackage != null) current
                else split(all).copy(loaded = true)
            }
        }
    }

    private fun split(all: List<LauncherApp>): LauncherState {
        val hidden = prefs.hiddenPackages()
        val (hiddenApps, visible) = all.partition { it.packageName in hidden }
        return LauncherState(apps = visible, hiddenApps = hiddenApps.sortedBy { it.label.lowercase() })
    }

    fun launchIntent(app: LauncherApp) = repository.launchIntent(app)

    fun startMove(packageName: String) = _state.update { it.copy(movingPackage = packageName) }

    /** Shifts the moving app by [delta] positions, clamped to the list bounds. */
    fun moveBy(delta: Int) = _state.update { current ->
        val pkg = current.movingPackage ?: return@update current
        val from = current.apps.indexOfFirst { it.packageName == pkg }
        if (from < 0) return@update current
        val to = (from + delta).coerceIn(0, current.apps.lastIndex)
        if (to == from) return@update current
        val reordered = current.apps.toMutableList().apply { add(to, removeAt(from)) }
        current.copy(apps = reordered)
    }

    fun endMove() {
        _state.update { it.copy(movingPackage = null) }
        saveOrder()
    }

    fun setHidden(packageName: String, hidden: Boolean) {
        prefs.setHidden(packageName, hidden)
        // Unhidden apps return to their saved position.
        _state.update { current ->
            split(prefs.sort(current.apps + current.hiddenApps))
                .copy(loaded = current.loaded, movingPackage = current.movingPackage)
        }
    }

    private fun saveOrder() {
        val current = _state.value
        prefs.saveOrder((current.apps + current.hiddenApps).map { it.packageName })
    }
}
