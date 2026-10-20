package com.tk.toolkit.tv.launcher.data

import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.provider.Settings
import android.widget.Toast
import com.tk.toolkit.tv.launcher.data.AppActions.safeStart

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
        val intent = findIntent(context.packageManager)
        if (intent != null) {
            context.safeStart(intent)
        } else {
            Toast.makeText(context, "Ambilight settings not found, opening Settings", Toast.LENGTH_SHORT).show()
            AppActions.openSystemSettings(context)
        }
    }

    // The flag-int PackageManager overloads are deprecated since API 33 but still work, and minSdk is 28.
    @Suppress("DEPRECATION")
    private fun findIntent(pm: PackageManager): Intent? {
        ComponentName.unflattenFromString(AMBILIGHT_COMPONENT)?.let { component ->
            val intent = Intent(Intent.ACTION_MAIN).setComponent(component).addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            if (intent.resolveActivity(pm) != null) return intent
        }
        val installed = AMBILIGHT_PACKAGES.filter { pm.isInstalled(it) }
        // A dedicated Ambilight screen beats a package's main screen, whatever the package order.
        for (pkg in installed) {
            pm.ambilightActivity(pkg)?.let { return it }
        }
        for (pkg in installed) {
            pm.mainIntent(pkg)?.let { return it }
        }
        // Any other app with "ambilight" in its package name, e.g. on a newer firmware.
        val others = pm.getInstalledApplications(0).map { it.packageName }
            .filter { it.contains("ambilight", ignoreCase = true) && it !in installed }
        return others.firstNotNullOfOrNull { pm.mainIntent(it) }
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

    /** An exported, enabled activity of [packageName] whose class name mentions Ambilight. */
    @Suppress("DEPRECATION")
    private fun PackageManager.ambilightActivity(packageName: String): Intent? {
        val activities = try {
            getPackageInfo(packageName, PackageManager.GET_ACTIVITIES).activities
        } catch (_: PackageManager.NameNotFoundException) {
            null
        } ?: return null
        val match = activities.firstOrNull {
            it.exported && it.enabled && it.permission == null && it.name.contains("ambi", ignoreCase = true)
        } ?: return null
        return Intent(Intent.ACTION_MAIN)
            .setComponent(ComponentName(match.packageName, match.name))
            .addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
    }
}
