package com.tk.toolkit.tv.launcher.data

/**
 * Remote shortcuts of the home screen: hold 1 for a Kick channel, hold 3 for a YouTube playlist.
 * Both can be changed in Toolkit's options; these are the defaults.
 */
object Shortcuts {
    const val DEFAULT_KICK_CHANNEL = "ryu7z"

    /** "R&B sexy" playlist. */
    const val DEFAULT_YOUTUBE_PLAYLIST = "PL5k1xh3AerewrFvNr2i8pNJU-4QNameEt"

    /** How long 1, 2 and 3 must be held, by default. */
    const val DEFAULT_KICK_HOLD_MS = 2_000L
    const val DEFAULT_RAINBOW_HOLD_MS = 4_000L
    const val DEFAULT_PLAYLIST_HOLD_MS = 2_000L

    /** Holding Home: past the first, the quick settings; past the second, the Google TV home. */
    const val DEFAULT_HOME_QUICK_SETTINGS_MS = 600L
    const val DEFAULT_HOME_STOCK_HOME_MS = 4_000L

    /** The YouTube TV app plays a /playlist link; the watch form is a fallback. */
    fun youtubeLinks(playlist: String) = listOf(
        "https://www.youtube.com/playlist?list=$playlist",
        "https://www.youtube.com/watch?list=$playlist",
    )

    /** "ryu7z", "kick.com/ryu7z" or "https://kick.com/ryu7z?…" gives "ryu7z"; null if it isn't one. */
    fun parseKickChannel(input: String): String? {
        val name = input.trim().substringBefore('?').substringBefore('#').trimEnd('/').substringAfterLast('/')
        return name.takeIf { it.matches(Regex("[A-Za-z0-9_-]{2,40}")) }
    }

    /** Any YouTube link with list=…, or a bare playlist ID, gives the ID; null if it isn't one. */
    fun parsePlaylistId(input: String): String? {
        val text = input.trim()
        val id = Regex("[?&]list=([A-Za-z0-9_-]+)").find(text)?.groupValues?.get(1) ?: text
        return id.takeIf { it.matches(Regex("[A-Za-z0-9_-]{10,64}")) }
    }
}
