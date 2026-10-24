package com.tk.toolkit.tv.launcher.data

import android.content.Context
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.launch

/**
 * Runs the screensaver's Ambilight (Toolkit's animation or a TV style) for whoever notices the
 * screensaver: the home screen for Toolkit's own, the accessibility service for the TV's
 * (it hears the system's "dreaming started/stopped" broadcasts). One effect at a time.
 */
object AmbilightController {

    private const val MAX_MS = 3 * 60 * 60_000L

    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.IO)
    private var job: Job? = null

    /** The last error of an effect, shown in the options; null when the last one went fine. */
    @Volatile
    var lastError: String? = null
        private set

    /** Starts [choice] (by default the one in the options) for at most [maxMs]. */
    fun start(context: Context, choice: SaverAmbilight? = null, maxMs: Long = MAX_MS) {
        val prefs = LauncherPrefs(context.applicationContext)
        val tv = PhilipsTv(context.applicationContext, prefs)
        val pick = choice ?: prefs.appearance.saverAmbilight
        if (!tv.isPaired || pick == SaverAmbilight.Tv) return
        job?.cancel()
        job = scope.launch {
            lastError = null
            try {
                val style = pick.style
                if (style == null) {
                    // Read on every frame, so changes in the options apply right away.
                    AmbilightEffect.run(tv, { prefs.borderSettings }, { prefs.appearance.ambilightIntensity }, maxMs)
                } else {
                    AmbilightEffect.runStyle(tv, style, pick.setting, maxMs)
                }
            } catch (e: CancellationException) {
                throw e
            } catch (e: Exception) {
                lastError = e.message ?: e.toString()
            }
        }
    }

    fun stop() {
        job?.cancel()
        job = null
    }
}
