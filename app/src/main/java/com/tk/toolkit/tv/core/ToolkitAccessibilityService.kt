package com.tk.toolkit.tv.core

import android.accessibilityservice.AccessibilityService
import android.content.ActivityNotFoundException
import android.content.Intent
import android.os.Handler
import android.os.Looper
import android.os.PowerManager
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
 * - Puts the TV to sleep when the sleep timer set with [setSleepTimer] runs out.
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

    /** Sleep timer: chosen length in minutes and when it ends (wall clock), or null when off. */
    var sleepMinutes: Int? = null
        private set
    var sleepAt: Long? = null
        private set
    private var sleepDialogOpen = false
    private val sleepWarning = Runnable {
        Toast.makeText(this, "La TV se met en veille dans 1 minute", Toast.LENGTH_LONG).show()
    }
    private val goToSleep = Runnable { sleepNow() }

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

    /** Starts, changes or (with null) cancels the sleep timer. */
    fun setSleepTimer(minutes: Int?) {
        handler.removeCallbacks(goToSleep)
        handler.removeCallbacks(sleepWarning)
        sleepMinutes = minutes
        if (minutes == null) {
            sleepAt = null
            return
        }
        val delayMs = minutes * 60_000L
        sleepAt = System.currentTimeMillis() + delayMs
        handler.postDelayed(goToSleep, delayMs)
        if (delayMs > 60_000L) handler.postDelayed(sleepWarning, delayMs - 60_000L)
    }

    /**
     * Apps can't press the power button, so: the lock-screen action, which sleeps most TVs; if
     * the screen is still on a few seconds later, the power menu, then its sleep entry.
     */
    private fun sleepNow() {
        sleepMinutes = null
        sleepAt = null
        performGlobalAction(GLOBAL_ACTION_LOCK_SCREEN)
        handler.postDelayed({
            if (getSystemService(PowerManager::class.java)?.isInteractive != true) return@postDelayed
            sleepDialogOpen = true
            SYSTEM_DIALOG_PACKAGES.forEach(::watchPackage)
            performGlobalAction(GLOBAL_ACTION_POWER_DIALOG)
            handler.postDelayed({ sleepDialogOpen = false }, SLEEP_DIALOG_WAIT_MS)
        }, LOCK_CHECK_MS)
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
        if (sleepDialogOpen) {
            val node = rootInActiveWindow?.findByText(SLEEP_LABELS)
            if (node?.clickableAncestor()?.performAction(AccessibilityNodeInfo.ACTION_CLICK) == true) {
                sleepDialogOpen = false
            }
            return
        }
        // Catches the stock home showing up some other way (boot, an app exiting to home).
        if (event.eventType == AccessibilityEvent.TYPE_WINDOW_STATE_CHANGED &&
            event.packageName == STOCK_LAUNCHER &&
            event.className?.toString() == STOCK_HOME_ACTIVITY
        ) {
            if (!stockHomeAllowed) openToolkit()
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

        private const val LOCK_CHECK_MS = 3_000L
        private const val SLEEP_DIALOG_WAIT_MS = 10_000L
        private val SYSTEM_DIALOG_PACKAGES = listOf("android", "com.android.systemui")
        /** Power menu entries that put the TV in standby, most specific first. */
        private val SLEEP_LABELS = listOf("Mise en veille", "Veille", "Sleep", "Standby", "Éteindre", "Power off")
    }
}
