package com.tk.toolkit.tv.launcher.data

/**
 * The home screen's default layout, used until the user changes it: apps pinned to Continue
 * Watching, and apps placed first in the grid. Each entry is the first installed app matching
 * one of its package names, else one of its labels (ignoring case, and also matching a longer
 * name such as "Kick: Live Streaming" for "Kick"); entries with no installed match are skipped.
 * Edit these lists to change the defaults.
 */
object HomeDefaults {

    class AppMatch(val packages: List<String> = emptyList(), val labels: List<String> = emptyList())

    val KICK = AppMatch(labels = listOf("Kick"))

    /** Channel opened in Kick by the 🥵 button of the header. */
    const val KICK_CHANNEL_URL = "https://kick.com/ryu7z"

    /** Pinned to Continue Watching, in this order, until the user pins, unpins or moves a pin. */
    val PINNED = listOf(
        AppMatch(packages = listOf("com.google.android.youtube.tv"), labels = listOf("YouTube")),
        KICK,
        // A player for video files (MP4…): the first one installed.
        AppMatch(
            packages = listOf(
                "org.videolan.vlc",
                "com.mxtech.videoplayer.pro",
                "com.mxtech.videoplayer.ad",
                "org.xbmc.kodi",
                "com.brouken.player",
                "org.courville.nova",
            ),
            labels = listOf("VLC", "MX Player", "Kodi", "Just Player", "Nova Video Player"),
        ),
    )

    /**
     * First in the app grid, in this order; the other apps follow alphabetically. Also the
     * "Streaming first" order of the Sort apps menu.
     */
    val FIRST_APPS = listOf(
        PINNED[0], // YouTube
        PINNED[1], // Kick
        AppMatch(packages = listOf("com.wbd.stream"), labels = listOf("HBO Max", "Max")),
        AppMatch(packages = listOf("com.apple.atve.androidtv.appletv"), labels = listOf("Apple TV")),
        AppMatch(packages = listOf("com.netflix.ninja"), labels = listOf("Netflix")),
        // Philips' own media browser (USB drives, network shares).
        AppMatch(packages = listOf("org.droidtv.contentexplorer"), labels = listOf("Multimédia", "Multimedia")),
        PINNED[2], // Video player
        AppMatch(packages = listOf("tv.molotov.app"), labels = listOf("Molotov", "Molotov TV")),
        AppMatch(packages = listOf("com.canal.android.canal"), labels = listOf("CANAL+", "myCANAL")),
        AppMatch(packages = listOf("com.disney.disneyplus"), labels = listOf("Disney+")),
        AppMatch(packages = listOf("com.amazon.amazonvideo.livingroom"), labels = listOf("Prime Video")),
        AppMatch(labels = listOf("Paramount+")),
        AppMatch(labels = listOf("france.tv")),
        AppMatch(labels = listOf("TF1+")),
        AppMatch(labels = listOf("M6+")),
        AppMatch(labels = listOf("ARTE")),
    )

    /** Package names of the installed apps matching [matches], in order. */
    fun resolve(matches: List<AppMatch>, apps: List<LauncherApp>): List<String> {
        val installed = apps.mapTo(HashSet()) { it.packageName }
        return matches.mapNotNull { match ->
            match.packages.firstOrNull { it in installed }
                ?: apps.firstOrNull { app -> match.labels.any { app.label.isNamed(it) } }?.packageName
        }.distinct()
    }

    /** "Kick", "KICK" or "Kick: Live Streaming" for "Kick", but not "Kickboxing". */
    private fun String.isNamed(name: String): Boolean =
        startsWith(name, ignoreCase = true) && (length == name.length || !this[name.length].isLetterOrDigit())
}
