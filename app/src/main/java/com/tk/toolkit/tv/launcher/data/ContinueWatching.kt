package com.tk.toolkit.tv.launcher.data

import android.content.Intent
import androidx.annotation.DrawableRes
import androidx.compose.ui.graphics.Color
import androidx.core.net.toUri
import com.tk.toolkit.tv.R

/** A show or list pinned to the home screen that opens straight into its streaming app. */
data class WatchItem(
    val key: String,
    val title: String,
    @DrawableRes val artwork: Int,
    val glow: Color,
    /** App that opens [uri]; the item is only shown while it's installed. */
    val packageName: String,
    val uri: String,
    /** Caption drawn over the artwork, for artwork that is only a logo. */
    val caption: String? = null,
    val removable: Boolean = true,
) {
    fun intent(): Intent =
        Intent(Intent.ACTION_VIEW, uri.toUri())
            .setPackage(packageName)
            .addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
}

object ContinueWatching {
    // Hand-picked for now.
    val items = listOf(
        WatchItem(
            key = "youtube:watch-later",
            title = "YouTube Watch Later",
            artwork = R.drawable.continue_youtube_watch_later,
            glow = Color(0xFFFF4E45),
            packageName = "com.google.android.youtube.tv",
            // The TV app plays a /playlist link; its own "browse" route opens the playlist page.
            uri = "https://www.youtube.com/tv?c=VLWL",
            caption = "Watch Later",
            removable = false,
        ),
        WatchItem(
            key = "disney:the-simpsons",
            title = "The Simpsons",
            artwork = R.drawable.continue_the_simpsons,
            glow = Color(0xFFFFE14A),
            packageName = "com.disney.disneyplus",
            uri = "https://www.disneyplus.com/browse/entity-cac75c8f-a9e2-4d95-ac73-1cf1cc7b9568",
        ),
        WatchItem(
            key = "disney:futurama",
            title = "Futurama",
            artwork = R.drawable.continue_futurama,
            glow = Color(0xFF9BE15D),
            packageName = "com.disney.disneyplus",
            uri = "https://www.disneyplus.com/browse/entity-85bf4cc1-cd8b-4469-ad87-7289217a0b74",
        ),
    )
}
