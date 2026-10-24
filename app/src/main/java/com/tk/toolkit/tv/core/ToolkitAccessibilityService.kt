package com.tk.toolkit.tv.core

import android.accessibilityservice.AccessibilityService
import android.content.ActivityNotFoundException
import android.content.Intent
import android.os.Handler
import android.os.Looper
import android.os.SystemClock
import android.view.KeyEvent
import android.view.accessibility.AccessibilityEvent
import android.view.accessibility.AccessibilityNodeInfo
import android.widget.Toast
import com.tk.toolkit.tv.MainActivity
import com.tk.toolkit.tv.launcher.data.AppActions
import com.tk.toolkit.tv.launcher.data.LauncherPrefs

/**
 * System-level helpers that need an accessibility service:
 * - Sends the Home button (and the stock Google TV home screen) to Toolkit, so it works as
 *   the launcher without disabling the stock one. Holding Home opens the quick settings, and
 *   holding it longer opens the stock home until Home is pressed again (both durations are
 *   set in Toolkit's options).
 * - Drives the system App info screen to press "Force stop" → "OK" after [forceStop].
 * - Opens a Kick channel after [openKickChannel]: Kick's TV app takes no links, so once it's
 *   open this clicks the channel's name, or the Following tab if the channel can't be found.
 */
class ToolkitAccessibilityService : AccessibilityService() {

    private enum class Step { PressForceStop, Confirm }

    private val handler = Handler(Looper.getMainLooper())

    /** Home hold durations, set in Toolkit's options (shared preferences, same process). */
    private val prefs by lazy { LauncherPrefs(this) }
    private var target: String? = null
    private var step = Step.PressForceStop
    private val timeout = Runnable { finish(null) }

    /** Set while the user asked for the stock home, so it isn't sent back to Toolkit. */
    private var stockHomeAllowed = false
    private var homeHeldForStock = false
    private val openStockHomeAfterHold = Runnable {
        homeHeldForStock = true
        openStockHome()
    }

    /** Kick channel being opened by [openKickChannel], until it's clicked or time runs out. */
    private class KickRequest(val packageName: String, val channel: String, val startedAt: Long)

    private var kick: KickRequest? = null
    private val kickFallback = Runnable { rootInActiveWindow?.let(::continueKick) }
    private val kickGiveUp = Runnable { kick = null }

    override fun onServiceConnected() {
        instance = this
    }

    override fun onDestroy() {
        if (instance === this) instance = null
        super.onDestroy()
    }

    override fun onInterrupt() = Unit

    fun forceStop(packageName: String) {
        target = packageName
        step = Step.PressForceStop
        handler.removeCallbacks(timeout)
        handler.postDelayed(timeout, TIMEOUT_MS)
        startActivity(
            AppActions.appInfoIntent(packageName)
                .addFlags(Intent.FLAG_ACTIVITY_NO_HISTORY or Intent.FLAG_ACTIVITY_EXCLUDE_FROM_RECENTS)
        )
    }

    /**
     * Starts Kick with [launch] (from its home screen), then clicks [channel] as soon as it shows
     * up, or after a few seconds without it (offline channel) the Following tab.
     */
    fun openKickChannel(packageName: String, channel: String, launch: Intent) {
        kick = KickRequest(packageName, channel, SystemClock.uptimeMillis())
        watchPackage(packageName)
        handler.removeCallbacks(kickFallback)
        handler.removeCallbacks(kickGiveUp)
        handler.postDelayed(kickFallback, KICK_CHANNEL_SEARCH_MS)
        handler.postDelayed(kickGiveUp, KICK_GIVE_UP_MS)
        startActivity(launch)
    }

    private fun continueKick(root: AccessibilityNodeInfo) {
        val request = kick ?: return
        if (root.packageName?.toString() != request.packageName) return
        val channel = root.findAccessibilityNodeInfosByText(request.channel).firstOrNull()
        val stillSearching = SystemClock.uptimeMillis() - request.startedAt < KICK_CHANNEL_SEARCH_MS
        val node = channel ?: if (stillSearching) return else root.findByText(FOLLOWING_LABELS) ?: return
        val clickable = node.clickableAncestor() ?: return
        if (clickable.performAction(AccessibilityNodeInfo.ACTION_CLICK)) {
            kick = null
            handler.removeCallbacks(kickFallback)
            handler.removeCallbacks(kickGiveUp)
        }
    }

    /** Adds [packageName] to the apps this service gets events from (accessibility_service.xml). */
    private fun watchPackage(packageName: String) {
        val info = serviceInfo ?: return
        val watched = info.packageNames ?: return // null: every app already
        if (packageName in watched) return
        info.packageNames = watched + packageName
        serviceInfo = info
    }

    override fun onKeyEvent(event: KeyEvent): Boolean {
        if (event.keyCode != KeyEvent.KEYCODE_HOME) return false
        when (event.action) {
            KeyEvent.ACTION_DOWN -> if (event.repeatCount == 0) {
                // Fires while still held, so the stock home shows up without waiting for release.
                homeHeldForStock = false
                handler.postDelayed(openStockHomeAfterHold, prefs.homeStockHomeMs)
            }
            KeyEvent.ACTION_UP -> {
                handler.removeCallbacks(openStockHomeAfterHold)
                if (!homeHeldForStock) {
                    val held = event.eventTime - event.downTime
                    if (held >= prefs.homeQuickSettingsMs) performGlobalAction(GLOBAL_ACTION_QUICK_SETTINGS) else openToolkit()
                }
            }
        }
        return true
    }

    override fun onAccessibilityEvent(event: AccessibilityEvent) {
        // Catches the stock home showing up some other way (boot, an app exiting to home).
        if (event.eventType == AccessibilityEvent.TYPE_WINDOW_STATE_CHANGED &&
            event.packageName == STOCK_LAUNCHER &&
            event.className?.toString() == STOCK_HOME_ACTIVITY
        ) {
            if (!stockHomeAllowed) openToolkit()
            return
        }
        val request = kick
        if (request != null && event.packageName?.toString() == request.packageName) {
            rootInActiveWindow?.let(::continueKick)
            return
        }
        if (target == null) return
        val root = rootInActiveWindow ?: return
        when (step) {
            Step.PressForceStop -> {
                val node = root.findByText(FORCE_STOP_LABELS) ?: return
                val clickable = node.clickableAncestor() ?: return
                if (!clickable.isEnabled || !node.isEnabled) {
                    finish("Déjà arrêtée")
                } else if (clickable.performAction(AccessibilityNodeInfo.ACTION_CLICK)) {
                    step = Step.Confirm
                }
            }
            Step.Confirm -> {
                val node = root.findByText(CONFIRM_LABELS) ?: return
                val clickable = node.clickableAncestor() ?: return
                if (clickable.performAction(AccessibilityNodeInfo.ACTION_CLICK)) {
                    finish("Arrêt forcé")
                }
            }
        }
    }

    private fun openToolkit() {
        stockHomeAllowed = false
        startActivity(
            Intent(this, MainActivity::class.java)
                .addFlags(Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_RESET_TASK_IF_NEEDED)
        )
    }

    /** The system's other home screen (Google TV's), opened on purpose, so it's left on screen. */
    private fun openStockHome() {
        val home = Intent(Intent.ACTION_MAIN).addCategory(Intent.CATEGORY_HOME)
        val candidates = packageManager.queryIntentActivities(home, 0).map { it.activityInfo }
            .filter { it.packageName != packageName && it.packageName !in SETTINGS_PACKAGES }
        val stock = candidates.firstOrNull { it.packageName == STOCK_LAUNCHER } ?: candidates.firstOrNull()
        if (stock == null) {
            Toast.makeText(this, "Accueil Google TV introuvable", Toast.LENGTH_SHORT).show()
            return
        }
        stockHomeAllowed = true
        try {
            startActivity(
                Intent(home).setClassName(stock.packageName, stock.name).addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            )
            Toast.makeText(this, "Accueil Google TV · appuie sur Home pour revenir à Toolkit", Toast.LENGTH_LONG).show()
        } catch (_: ActivityNotFoundException) {
            stockHomeAllowed = false
            Toast.makeText(this, "Impossible d'ouvrir l'accueil Google TV", Toast.LENGTH_SHORT).show()
        }
    }

    private fun finish(message: String?) {
        handler.removeCallbacks(timeout)
        val wasRunning = target != null
        target = null
        if (!wasRunning) return
        handler.postDelayed({ openToolkit() }, 250)
        Toast.makeText(this, message ?: "Échec de l'arrêt forcé : essaie depuis Infos sur l'appli", Toast.LENGTH_SHORT).show()
    }

    private fun AccessibilityNodeInfo.findByText(labels: List<String>): AccessibilityNodeInfo? {
        for (label in labels) {
            findAccessibilityNodeInfosByText(label)
                .firstOrNull { it.text?.toString().equals(label, ignoreCase = true) }
                ?.let { return it }
        }
        return null
    }

    private fun AccessibilityNodeInfo.clickableAncestor(): AccessibilityNodeInfo? {
        var node: AccessibilityNodeInfo? = this
        while (node != null && !node.isClickable) node = node.parent
        return node
    }

    companion object {
        @Volatile
        var instance: ToolkitAccessibilityService? = null
            private set

        private const val TIMEOUT_MS = 6_000L
        private const val STOCK_LAUNCHER = "com.google.android.apps.tv.launcherx"
        private const val STOCK_HOME_ACTIVITY = "com.google.android.apps.tv.launcherx.home.HomeActivity"

        /** Settings' "FallbackHome" screens, which also answer the Home intent. */
        private val SETTINGS_PACKAGES = setOf("com.android.settings", "com.android.tv.settings")
        // English and French system Settings (straight and typographic apostrophes).
        private val FORCE_STOP_LABELS = listOf("Force stop", "Forcer l'arrêt", "Forcer l’arrêt")
        private val CONFIRM_LABELS = listOf("OK")

        /** How long to look for the channel's name before falling back to Following. */
        private const val KICK_CHANNEL_SEARCH_MS = 5_000L
        private const val KICK_GIVE_UP_MS = 12_000L
        private val FOLLOWING_LABELS = listOf("Following", "Suivis", "Abonnements", "Chaînes suivies", "Suivi")
    }
}
