package com.tk.toolkit.tv.core

import android.accessibilityservice.AccessibilityService
import android.accessibilityservice.AccessibilityServiceInfo
import android.accessibilityservice.GestureDescription
import android.graphics.Path
import android.graphics.Rect
import android.content.ActivityNotFoundException
import android.content.Intent
import android.os.Handler
import android.os.Looper
import android.os.PowerManager
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
 * - Puts the TV to sleep when the sleep timer set with [setSleepTimer] runs out.
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

    /** Steps of opening a Kick channel: on Kick's home screen, else in the Following tab. */
    private enum class KickStep { FindOnScreen, OpenFollowing, FindInFollowing }

    /** Kick channel being opened by [openKickChannel], until it's clicked or time runs out. */
    private class KickRequest(val packageName: String, val channel: String, val startedAt: Long) {
        var step = KickStep.FindOnScreen
        var stepAt = startedAt
        val log = mutableListOf<String>()
    }

    private var kick: KickRequest? = null

    /** Kick doesn't always send events while loading, so its screen is also checked regularly. */
    private val kickTick = object : Runnable {
        override fun run() {
            if (kick == null) return
            rootInActiveWindow?.let(::continueKick)
            if (kick != null) handler.postDelayed(this, KICK_TICK_MS)
        }
    }

    /** What the last Kick shortcut did and saw, shown in Toolkit's options to fine-tune it. */
    var lastKickReport: List<String> = emptyList()
        private set

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

    /**
     * Starts Kick with [launch] (from its home screen), then taps [channel] as soon as it shows
     * up; else taps the Following tab and the channel there; if it isn't there either, Following
     * stays open. Kick's TV app takes no links, so this is the only way in.
     */
    fun openKickChannel(packageName: String, channel: String, launch: Intent) {
        val now = SystemClock.uptimeMillis()
        kick = KickRequest(packageName, channel, now).also {
            it.log += "Ouverture de Kick ($packageName)"
            it.log += if (canTap()) {
                "Toucher l'écran : autorisé"
            } else {
                "Toucher l'écran : NON autorisé, désactive puis réactive Toolkit dans Paramètres › Accessibilité"
            }
        }
        watchPackage(packageName)
        handler.removeCallbacks(kickTick)
        handler.postDelayed(kickTick, KICK_TICK_MS)
        startActivity(launch)
    }

    private fun continueKick(root: AccessibilityNodeInfo) {
        val request = kick ?: return
        if (root.packageName?.toString() != request.packageName) return
        val now = SystemClock.uptimeMillis()
        val sinceStep = now - request.stepAt
        fun advance(step: KickStep) {
            request.log += "${(now - request.startedAt) / 100 / 10.0} s : ${request.step} → $step"
            request.step = step
            request.stepAt = now
        }
        if (now - request.startedAt > KICK_GIVE_UP_MS) return finishKick(root, "Abandon : rien trouvé à cliquer")

        // The channel's name anywhere on screen wins at every step.
        root.findContaining(listOf(request.channel)).firstOrNull { !it.isEditable }?.let {
            if (it.click()) return finishKick(root, "Chaîne « ${request.channel} » ouverte (${request.step})")
        }
        when (request.step) {
            KickStep.FindOnScreen -> if (sinceStep > KICK_ON_SCREEN_MS) advance(KickStep.OpenFollowing)
            KickStep.OpenFollowing -> {
                val following = root.findByText(FOLLOWING_LABELS) ?: root.findContaining(FOLLOWING_LABELS).firstOrNull()
                when {
                    following == null -> if (sinceStep > KICK_STEP_MS) finishKick(root, "Onglet Following introuvable")
                    following.click() -> advance(KickStep.FindInFollowing)
                    sinceStep > KICK_STEP_MS -> finishKick(root, "Following trouvé, mais le toucher n'a pas pu être envoyé")
                }
            }
            KickStep.FindInFollowing -> if (sinceStep > KICK_FOLLOWING_MS) {
                finishKick(root, "« ${request.channel} » absent des Suivis : onglet Suivis laissé ouvert")
            }
        }
    }

    /** Ends the Kick shortcut and keeps a report of the screen for Toolkit's options. */
    private fun finishKick(root: AccessibilityNodeInfo, outcome: String) {
        val request = kick ?: return
        kick = null
        handler.removeCallbacks(kickTick)
        lastKickReport = listOf(outcome) + request.log + listOf("— Écran de Kick —") + root.describe()
    }

    /** One line per node with text, a description or an ID: what the Kick automation can aim at. */
    private fun AccessibilityNodeInfo.describe(): List<String> {
        val lines = mutableListOf<String>()
        fun visit(node: AccessibilityNodeInfo?, depth: Int) {
            if (node == null || lines.size >= KICK_REPORT_LINES) return
            val text = node.text?.toString().orEmpty()
            val description = node.contentDescription?.toString().orEmpty()
            val id = node.viewIdResourceName?.substringAfter(":id/").orEmpty()
            if (text.isNotEmpty() || description.isNotEmpty() || id.isNotEmpty()) {
                val mark = when {
                    node.isEditable -> "✎"
                    node.isClickable -> "▶"
                    node.isFocusable -> "◦"
                    else -> "·"
                }
                lines += "  ".repeat(depth.coerceAtMost(6)) + "$mark " +
                    listOf(text, description.takeIf { it != text }.orEmpty(), id.let { if (it.isEmpty()) "" else "#$it" })
                        .filter { it.isNotEmpty() }.joinToString(" | ")
            }
            for (i in 0 until node.childCount) visit(node.getChild(i), depth + 1)
        }
        visit(this, 0)
        return lines
    }

    /** Nodes whose text or description contains one of [labels] (ignoring case). */
    private fun AccessibilityNodeInfo.findContaining(labels: List<String>): List<AccessibilityNodeInfo> =
        labels.flatMap { findAccessibilityNodeInfosByText(it) }

    /** Whether the system lets this service tap the screen (granted when the service is enabled). */
    private fun canTap(): Boolean =
        (serviceInfo?.capabilities ?: 0) and AccessibilityServiceInfo.CAPABILITY_CAN_PERFORM_GESTURES != 0

    /**
     * Clicks this node or its nearest clickable parent; apps like Kick mark nothing as clickable,
     * so failing that, taps the middle of the node on screen (a gesture, like TV "mouse" apps).
     */
    private fun AccessibilityNodeInfo.click(): Boolean {
        if (clickableAncestor()?.performAction(AccessibilityNodeInfo.ACTION_CLICK) == true) return true
        if (performAction(AccessibilityNodeInfo.ACTION_CLICK)) return true
        val bounds = Rect().also { getBoundsInScreen(it) }
        if (bounds.isEmpty) return false
        val point = Path().apply { moveTo(bounds.exactCenterX(), bounds.exactCenterY()) }
        val tap = GestureDescription.Builder()
            .addStroke(GestureDescription.StrokeDescription(point, 0L, TAP_MS))
            .build()
        return dispatchGesture(tap, null, null)
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
        val kickRequest = kick
        if (kickRequest != null && event.packageName?.toString() == kickRequest.packageName) {
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

        private const val LOCK_CHECK_MS = 3_000L
        private const val SLEEP_DIALOG_WAIT_MS = 10_000L
        private val SYSTEM_DIALOG_PACKAGES = listOf("android", "com.android.systemui")
        /** Power menu entries that put the TV in standby, most specific first. */
        private val SLEEP_LABELS = listOf("Mise en veille", "Veille", "Sleep", "Standby", "Éteindre", "Power off")

        private const val KICK_TICK_MS = 700L
        /** How long to look for the channel on Kick's home screen before trying its search. */
        private const val KICK_ON_SCREEN_MS = 3_000L
        private const val KICK_STEP_MS = 3_500L
        private const val KICK_FOLLOWING_MS = 6_000L
        private const val KICK_GIVE_UP_MS = 18_000L
        private const val KICK_REPORT_LINES = 120
        private const val TAP_MS = 60L
        private val FOLLOWING_LABELS = listOf("Following", "Suivis", "Abonnements", "Chaînes suivies", "Suivi")
    }
}
