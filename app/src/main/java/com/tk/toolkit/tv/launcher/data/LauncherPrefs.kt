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

    /** Kick channel opened by holding 1. */
    var kickChannel: String
        get() = prefs.getString(KEY_KICK_CHANNEL, null) ?: Shortcuts.DEFAULT_KICK_CHANNEL
        set(value) = prefs.edit { putString(KEY_KICK_CHANNEL, value) }

    /** YouTube playlist ID played by holding 3. */
    var youtubePlaylist: String
        get() = prefs.getString(KEY_YOUTUBE_PLAYLIST, null) ?: Shortcuts.DEFAULT_YOUTUBE_PLAYLIST
        set(value) = prefs.edit { putString(KEY_YOUTUBE_PLAYLIST, value) }

    /** How long 1 (Kick), 2 (rainbow) and 3 (YouTube) must be held. */
    var kickHoldMs: Long
        get() = prefs.getLong(KEY_KICK_HOLD, Shortcuts.DEFAULT_KICK_HOLD_MS)
        set(value) = prefs.edit { putLong(KEY_KICK_HOLD, value) }
    var rainbowHoldMs: Long
        get() = prefs.getLong(KEY_RAINBOW_HOLD, Shortcuts.DEFAULT_RAINBOW_HOLD_MS)
        set(value) = prefs.edit { putLong(KEY_RAINBOW_HOLD, value) }
    var playlistHoldMs: Long
        get() = prefs.getLong(KEY_PLAYLIST_HOLD, Shortcuts.DEFAULT_PLAYLIST_HOLD_MS)
        set(value) = prefs.edit { putLong(KEY_PLAYLIST_HOLD, value) }

    /** How long Home must be held for the quick settings, and for the Google TV home. */
    var homeQuickSettingsMs: Long
        get() = prefs.getLong(KEY_HOME_QUICK, Shortcuts.DEFAULT_HOME_QUICK_SETTINGS_MS)
        set(value) = prefs.edit { putLong(KEY_HOME_QUICK, value) }
    var homeStockHomeMs: Long
        get() = prefs.getLong(KEY_HOME_STOCK, Shortcuts.DEFAULT_HOME_STOCK_HOME_MS)
        set(value) = prefs.edit { putLong(KEY_HOME_STOCK, value) }

    /** Name of the clock header's ClockFont. */
    var clockFont: String?
        get() = prefs.getString(KEY_CLOCK_FONT, null)
        set(value) = prefs.edit { putString(KEY_CLOCK_FONT, value) }

    /** Home screen look, weather and screensaver; each part falls back to its default. */
    var appearance: Appearance
        get() {
            val default = Appearance()
            return Appearance(
                columns = prefs.getInt(KEY_COLUMNS, default.columns),
                accent = prefs.getString(KEY_ACCENT, null).toEnum(default.accent),
                background = prefs.getString(KEY_BACKGROUND, null).toEnum(default.background),
                nightMode = prefs.getString(KEY_NIGHT_MODE, null).toEnum(default.nightMode),
                showRecent = prefs.getBoolean(KEY_SHOW_RECENT, default.showRecent),
                weatherCity = prefs.getString(KEY_WEATHER_CITY, null).toEnum(default.weatherCity),
                weatherDetail = prefs.getString(KEY_WEATHER_DETAIL, null).toEnum(default.weatherDetail),
                screensaverOn = prefs.getBoolean(KEY_SAVER_ON, default.screensaverOn),
                screensaverDelayMs = prefs.getLong(KEY_SAVER_DELAY, default.screensaverDelayMs),
                screensaverBottles = prefs.getBoolean(KEY_SAVER_BOTTLES, default.screensaverBottles),
            )
        }
        set(value) = prefs.edit {
            putInt(KEY_COLUMNS, value.columns)
            putString(KEY_ACCENT, value.accent.name)
            putString(KEY_BACKGROUND, value.background.name)
            putString(KEY_NIGHT_MODE, value.nightMode.name)
            putBoolean(KEY_SHOW_RECENT, value.showRecent)
            putString(KEY_WEATHER_CITY, value.weatherCity.name)
            putString(KEY_WEATHER_DETAIL, value.weatherDetail.name)
            putBoolean(KEY_SAVER_ON, value.screensaverOn)
            putLong(KEY_SAVER_DELAY, value.screensaverDelayMs)
            putBoolean(KEY_SAVER_BOTTLES, value.screensaverBottles)
        }

    /** Apps launched from Toolkit, most recent first. */
    fun recentPackages(): List<String> =
        prefs.getString(KEY_RECENT, null)?.split('\n')?.filter { it.isNotBlank() }.orEmpty()

    fun recordLaunch(packageName: String) {
        val updated = (listOf(packageName) + (recentPackages() - packageName)).take(MAX_RECENT)
        prefs.edit { putString(KEY_RECENT, updated.joinToString("\n")) }
    }

    /** Look of the hold-2 border; each part falls back to its default. */
    var borderSettings: BorderSettings
        get() {
            val default = BorderSettings()
            return BorderSettings(
                style = prefs.getString(KEY_BORDER_STYLE, null).toEnum(default.style),
                palette = prefs.getString(KEY_BORDER_PALETTE, null).toEnum(default.palette),
                thickness = prefs.getString(KEY_BORDER_THICKNESS, null).toEnum(default.thickness),
                speed = prefs.getString(KEY_BORDER_SPEED, null).toEnum(default.speed),
                glow = prefs.getString(KEY_BORDER_GLOW, null).toEnum(default.glow),
            )
        }
        set(value) = prefs.edit {
            putString(KEY_BORDER_STYLE, value.style.name)
            putString(KEY_BORDER_PALETTE, value.palette.name)
            putString(KEY_BORDER_THICKNESS, value.thickness.name)
            putString(KEY_BORDER_SPEED, value.speed.name)
            putString(KEY_BORDER_GLOW, value.glow.name)
        }

    private inline fun <reified E : Enum<E>> String?.toEnum(default: E): E =
        enumValues<E>().firstOrNull { it.name == this } ?: default

    /** Easter egg: the rainbow border around the home screen. */
    var rainbowBorder: Boolean
        get() = prefs.getBoolean(KEY_RAINBOW, false)
        set(value) = prefs.edit { putBoolean(KEY_RAINBOW, value) }

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
        const val KEY_RAINBOW = "rainbow_border"
        const val KEY_KICK_CHANNEL = "kick_channel"
        const val KEY_CLOCK_FONT = "clock_font"
        const val KEY_KICK_HOLD = "kick_hold_ms"
        const val KEY_RAINBOW_HOLD = "rainbow_hold_ms"
        const val KEY_PLAYLIST_HOLD = "playlist_hold_ms"
        const val KEY_HOME_QUICK = "home_quick_settings_ms"
        const val KEY_HOME_STOCK = "home_stock_home_ms"
        const val KEY_COLUMNS = "columns"
        const val KEY_ACCENT = "accent"
        const val KEY_BACKGROUND = "background"
        const val KEY_NIGHT_MODE = "night_mode"
        const val KEY_SHOW_RECENT = "show_recent"
        const val KEY_WEATHER_CITY = "weather_city"
        const val KEY_WEATHER_DETAIL = "weather_detail"
        const val KEY_SAVER_ON = "screensaver_on"
        const val KEY_SAVER_DELAY = "screensaver_delay_ms"
        const val KEY_SAVER_BOTTLES = "screensaver_bottles"
        const val KEY_RECENT = "recent"
        const val MAX_RECENT = 10
        const val KEY_BORDER_STYLE = "border_style"
        const val KEY_BORDER_PALETTE = "border_palette"
        const val KEY_BORDER_THICKNESS = "border_thickness"
        const val KEY_BORDER_SPEED = "border_speed"
        const val KEY_BORDER_GLOW = "border_glow"
        const val KEY_YOUTUBE_PLAYLIST = "youtube_playlist"
    }
}
