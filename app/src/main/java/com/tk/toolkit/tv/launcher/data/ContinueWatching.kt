package com.tk.toolkit.tv.launcher.data

import android.content.Intent
import androidx.annotation.DrawableRes
import androidx.compose.ui.graphics.Color
import androidx.core.net.toUri
import com.tk.toolkit.tv.R

/** A show pinned to the home screen that opens straight into its streaming app. */
data class WatchItem(
    val key: String,
    val title: String,
    @DrawableRes val artwork: Int,
    val glow: Color,
    val packageName: String,
    val uri: String,
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
            key = "disney:the-simpsons",
            title = "The Simpsons",
            artwork = R.drawable.continue_the_simpsons,
            glow = Color(0xFFFFE14A),
            packageName = "com.disney.disneyplus",
            uri = "https://www.disneyplus.com/browse/entity-cac75c8f-a9e2-4d95-ac73-1cf1cc7b9568",
        ),
    )
}
