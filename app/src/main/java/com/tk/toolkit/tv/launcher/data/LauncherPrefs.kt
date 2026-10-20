package com.tk.toolkit.tv.launcher.data

import android.content.Context
import androidx.core.content.edit

/** Persists the user's launcher customizations: app order, hidden apps and pinned Continue Watching apps. */
class LauncherPrefs(context: Context) {

    private val prefs = context.getSharedPreferences("launcher", Context.MODE_PRIVATE)

    fun loadOrder(): List<String> =
        prefs.getString(KEY_ORDER, null)?.split('\n')?.filter { it.isNotBlank() }.orEmpty()

    fun saveOrder(packages: List<String>) {
        prefs.edit { putString(KEY_ORDER, packages.joinToString("\n")) }
    }

    fun hiddenPackages(): Set<String> = prefs.getStringSet(KEY_HIDDEN, null).orEmpty()

    fun setHidden(packageName: String, hidden: Boolean) {
        val updated = hiddenPackages().toMutableSet().apply { if (hidden) add(packageName) else remove(packageName) }
        prefs.edit { putStringSet(KEY_HIDDEN, updated) }
    }

    /**
     * Packages pinned to Continue Watching, in the user's order, or null until the user first
     * pins, unpins or moves one (the defaults in [HomeDefaults.PINNED] apply until then).
     */
    fun pinnedPackages(): List<String>? =
        prefs.getString(KEY_PINNED, null)?.split('\n')?.filter { it.isNotBlank() }

    fun savePinned(packages: List<String>) {
        prefs.edit { putString(KEY_PINNED, packages.joinToString("\n")) }
    }

    /**
     * Apps in saved order first, then any apps not yet ordered: those in [HomeDefaults.FIRST_APPS]
     * in that order, then the rest alphabetically.
     */
    fun sort(apps: List<LauncherApp>): List<LauncherApp> {
        val rank = loadOrder().withIndex().associate { (i, pkg) -> pkg to i }
        val (known, fresh) = apps.partition { it.packageName in rank }
        return known.sortedBy { rank.getValue(it.packageName) } + defaultOrder(fresh)
    }

    /** [HomeDefaults.FIRST_APPS] in that order, then the rest alphabetically. */
    fun defaultOrder(apps: List<LauncherApp>): List<LauncherApp> {
        val firstRank = HomeDefaults.resolve(HomeDefaults.FIRST_APPS, apps).withIndex().associate { (i, pkg) -> pkg to i }
        return apps.sortedWith(compareBy({ firstRank[it.packageName] ?: Int.MAX_VALUE }, { it.label.lowercase() }))
    }

    private companion object {
        const val KEY_ORDER = "order"
        const val KEY_HIDDEN = "hidden"
        const val KEY_PINNED = "pinned"
    }
}
