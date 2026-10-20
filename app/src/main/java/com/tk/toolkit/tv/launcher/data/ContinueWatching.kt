package com.tk.toolkit.tv.launcher.data

/**
 * Continue Watching: apps the user pins from an app's long-press menu, shown above the app grid
 * in their own order. Nothing is pinned by default, so the section stays hidden until then.
 */
object ContinueWatching {
    private const val KEY_PREFIX = "pin:"

    /** Grid key of a pinned app, distinct from the key of the same app's tile under Apps. */
    fun key(packageName: String) = KEY_PREFIX + packageName
}
