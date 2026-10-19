package com.tk.toolkit.tv.launcher.data

import android.content.ComponentName
import androidx.compose.runtime.Immutable
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.ImageBitmap

/** An app that can be launched from the home screen. */
@Immutable
data class LauncherApp(
    val packageName: String,
    val component: ComponentName,
    val label: String,
    val isLeanback: Boolean,
    val isSystemApp: Boolean,
    val isUpdatedSystemApp: Boolean,
    /** TV banner (16:9), when the app ships one. */
    val banner: ImageBitmap?,
    val icon: ImageBitmap,
    /** Dominant icon color, used to tint the tile for apps without a banner. */
    val accent: Color,
    /** Install/update time, used to know when cached icons are stale. */
    val lastUpdateTime: Long,
) {
    val canUninstall: Boolean get() = !isSystemApp || isUpdatedSystemApp
}
