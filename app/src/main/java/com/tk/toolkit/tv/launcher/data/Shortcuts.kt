package com.tk.toolkit.tv.launcher.data

/**
 * Remote shortcuts of the home screen: hold 1 for a Kick channel, hold 3 for a YouTube playlist.
 * Both can be changed in Toolkit's options; these are the defaults.
 */
object Shortcuts {
    const val DEFAULT_KICK_CHANNEL = "ryu7z"

    /** "R&B sexy" playlist. */
    const val DEFAULT_YOUTUBE_PLAYLIST = "PL5k1xh3AerewrFvNr2i8pNJU-4QNameEt"

    /** Kick doesn't document its deep links, so these forms are tried in order, inside Kick. */
    fun kickLinks(channel: String) = listOf(
        "https://kick.com/$channel",
        "https://www.kick.com/$channel",
        "kick://$channel",
        "kick://channel/$channel",
        "kick://kick.com/$channel",
    )

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
