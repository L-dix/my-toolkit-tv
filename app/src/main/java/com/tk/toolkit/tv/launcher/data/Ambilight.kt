package com.tk.toolkit.tv.launcher.data

import android.content.ActivityNotFoundException
import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.widget.Toast

/**
 * Opens Philips' Ambilight settings. TP Vision doesn't document where they live and it changes
 * between TV generations, so this tries the places below in order and falls back to the
 * system Settings. To find the right screen on your TV, open the Ambilight menu and run
 * `adb shell dumpsys activity activities | grep -i resumed`, then put the component in
 * [AMBILIGHT_COMPONENT] or its package first in [AMBILIGHT_PACKAGES].
 */
object Ambilight {

    /** Exact screen to open, as "package/activity" (e.g. "org.droidtv.settings/.AmbilightActivity"). Empty to search. */
    const val AMBILIGHT_COMPONENT = ""

    /**
     * Philips / TP Vision packages that may hold the Ambilight settings, most likely first.
     * Each is searched for an activity with "ambi" in its name, then opened at its main screen.
     */
    val AMBILIGHT_PACKAGES = listOf(
        "org.droidtv.ambilight",
        "org.droidtv.ambilightsettings",
        "com.tpvision.ambilight",
        "com.tpvision.ambilightsettings",
        "org.droidtv.ambihuecommon",
        "org.droidtv.settings",
        "org.droidtv.tvsystemui",
    )

    fun open(context: Context) {
        // Candidates are guesses: an activity can still refuse to start (not exported to us,
        // or its intent filter doesn't match), so move on to the next one when it does.
        for (intent in candidates(context.packageManager)) {
            try {
                context.startActivity(intent)
                return
            } catch (_: ActivityNotFoundException) {
            } catch (_: SecurityException) {
            }
        }
        Toast.makeText(context, "Ambilight settings not found, opening Settings", Toast.LENGTH_SHORT).show()
        AppActions.openSystemSettings(context)
    }

    // The flag-int PackageManager overloads are deprecated since API 33 but still work, and minSdk is 28.
    @Suppress("DEPRECATION")
    private fun candidates(pm: PackageManager): Sequence<Intent> = sequence {
        ComponentName.unflattenFromString(AMBILIGHT_COMPONENT)?.let {
            yield(Intent(Intent.ACTION_MAIN).setComponent(it).addFlags(Intent.FLAG_ACTIVITY_NEW_TASK))
        }
        val installed = AMBILIGHT_PACKAGES.filter { pm.isInstalled(it) }
        // A dedicated Ambilight screen beats a package's main screen, whatever the package order.
        for (pkg in installed) yieldAll(pm.ambilightActivities(pkg))
        for (pkg in installed) pm.mainIntent(pkg)?.let { yield(it) }
        // Any other app with "ambilight" in its package name, e.g. on a newer firmware.
        pm.getInstalledApplications(0).asSequence()
            .map { it.packageName }
            .filter { it.contains("ambilight", ignoreCase = true) && it !in installed }
            .forEach { pkg -> pm.mainIntent(pkg)?.let { yield(it) } }
    }

    @Suppress("DEPRECATION")
    private fun PackageManager.isInstalled(packageName: String): Boolean =
        try {
            getApplicationInfo(packageName, 0).enabled
        } catch (_: PackageManager.NameNotFoundException) {
            false
        }

    private fun PackageManager.mainIntent(packageName: String): Intent? =
        (getLeanbackLaunchIntentForPackage(packageName) ?: getLaunchIntentForPackage(packageName))
            ?.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)

    /** Exported, enabled activities of [packageName] whose class name mentions Ambilight. */
    @Suppress("DEPRECATION")
    private fun PackageManager.ambilightActivities(packageName: String): List<Intent> {
        val activities = try {
            getPackageInfo(packageName, PackageManager.GET_ACTIVITIES).activities
        } catch (_: PackageManager.NameNotFoundException) {
            null
        } ?: return emptyList()
        return activities
            .filter { it.exported && it.enabled && it.permission == null && it.name.contains("ambi", ignoreCase = true) }
            .map {
                Intent(Intent.ACTION_MAIN)
                    .setComponent(ComponentName(it.packageName, it.name))
                    .addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            }
    }
}
