package com.tk.toolkit.tv.launcher.data

import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.content.pm.ApplicationInfo
import android.content.pm.ResolveInfo
import android.graphics.Bitmap
import android.graphics.drawable.Drawable
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.asImageBitmap
import androidx.core.graphics.drawable.toBitmap
import androidx.core.graphics.scale

/** Reads launchable apps from the package manager. */
class AppRepository(private val context: Context) {

    private val pm = context.packageManager

    // Bitmaps are rasterized at roughly their on-screen pixel size, which depends on the
    // resolution the TV renders its UI at (1080p on most Google TVs, 2160p on some 4K sets).
    // A focused tile is about a fifth of the screen width with 5 apps per row; sized for 4, so
    // tiles stay sharp down to 4 per row. Its icon is about a fifteenth.
    private val screenPx = context.resources.displayMetrics.let { maxOf(it.widthPixels, it.heightPixels) }
    private val bannerW = (screenPx / 4).coerceAtLeast(MIN_BANNER_W)
    private val bannerH = bannerW * 9 / 16
    private val iconPx = (screenPx / 15).coerceAtLeast(MIN_ICON_PX)

    /** Loaded apps by package, reused while the package hasn't been updated. */
    private val cache = HashMap<String, LauncherApp>()

    fun loadApps(): List<LauncherApp> {
        val leanback = query(Intent.CATEGORY_LEANBACK_LAUNCHER)
        val regular = query(Intent.CATEGORY_LAUNCHER)
        val seen = HashSet<String>()
        val result = ArrayList<LauncherApp>()
        // Prefer the leanback entry point when an app has both.
        for ((info, isLeanback) in leanback.map { it to true } + regular.map { it to false }) {
            val pkg = info.activityInfo.packageName
            if (pkg == context.packageName || !seen.add(pkg)) continue
            val updated = runCatching { pm.getPackageInfo(pkg, 0).lastUpdateTime }.getOrDefault(0L)
            val cached = cache[pkg]
            val app = if (cached != null && cached.lastUpdateTime == updated && cached.isLeanback == isLeanback) {
                cached
            } else {
                runCatching { toLauncherApp(info, isLeanback, updated) }.getOrNull() ?: continue
            }
            cache[pkg] = app
            result += app
        }
        cache.keys.retainAll(seen)
        return result
    }

    private fun query(category: String): List<ResolveInfo> =
        pm.queryIntentActivities(Intent(Intent.ACTION_MAIN).addCategory(category), 0)

    private fun toLauncherApp(info: ResolveInfo, isLeanback: Boolean, lastUpdateTime: Long): LauncherApp {
        val activity = info.activityInfo
        val appInfo = activity.applicationInfo
        val banner = activity.loadBanner(pm) ?: appInfo.loadBanner(pm)
        val iconBitmap = info.loadIcon(pm).toSafeBitmap(iconPx, iconPx)
        return LauncherApp(
            packageName = activity.packageName,
            component = ComponentName(activity.packageName, activity.name),
            label = info.loadLabel(pm).toString().trim(),
            isLeanback = isLeanback,
            isSystemApp = appInfo.flags and ApplicationInfo.FLAG_SYSTEM != 0,
            isUpdatedSystemApp = appInfo.flags and ApplicationInfo.FLAG_UPDATED_SYSTEM_APP != 0,
            banner = banner?.toSafeBitmap(bannerW, bannerH)?.asImageBitmap(),
            icon = iconBitmap.asImageBitmap(),
            accent = iconBitmap.dominantColor(),
            lastUpdateTime = lastUpdateTime,
        )
    }

    /** Rasterizes at exactly [w]x[h], roughly the on-screen size, to keep drawing cheap. */
    private fun Drawable.toSafeBitmap(w: Int, h: Int): Bitmap = toBitmap(w, h)

    /**
     * Average of the icon's opaque pixels, weighted toward saturated ones. Icons with no real
     * color (white, grey or black logos) get a deep red instead.
     */
    private fun Bitmap.dominantColor(): Color {
        val small = scale(24, 24, filter = true)
        var r = 0f; var g = 0f; var b = 0f; var total = 0f
        val hsv = FloatArray(3)
        for (x in 0 until small.width) for (y in 0 until small.height) {
            val px = small.getPixel(x, y)
            if (android.graphics.Color.alpha(px) < 200) continue
            android.graphics.Color.colorToHSV(px, hsv)
            val weight = 0.05f + hsv[1] * hsv[2]
            r += android.graphics.Color.red(px) * weight
            g += android.graphics.Color.green(px) * weight
            b += android.graphics.Color.blue(px) * weight
            total += weight
        }
        if (total == 0f) return NO_COLOR_ACCENT
        val average = android.graphics.Color.rgb(
            (r / total).toInt(), (g / total).toInt(), (b / total).toInt(),
        )
        android.graphics.Color.colorToHSV(average, hsv)
        return if (hsv[1] < MIN_ACCENT_SATURATION) NO_COLOR_ACCENT else Color(average)
    }

    fun launchIntent(app: LauncherApp): Intent =
        Intent(Intent.ACTION_MAIN)
            .addCategory(if (app.isLeanback) Intent.CATEGORY_LEANBACK_LAUNCHER else Intent.CATEGORY_LAUNCHER)
            .setComponent(app.component)
            .addFlags(Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_RESET_TASK_IF_NEEDED)

    private companion object {
        // Tuned for a 1080p UI; never rasterize smaller than this.
        const val MIN_ICON_PX = 128
        const val MIN_BANNER_W = 352

        val NO_COLOR_ACCENT = Color(0xFF8B0A1A)
        const val MIN_ACCENT_SATURATION = 0.2f
    }
}
