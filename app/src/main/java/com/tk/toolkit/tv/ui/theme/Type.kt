package com.tk.toolkit.tv.ui.theme

import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.Font
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.font.Typeface
import androidx.compose.ui.unit.sp
import androidx.tv.material3.Typography
import com.tk.toolkit.tv.R

/**
 * Toolkit Heros, a subset of TeX Gyre Heros (a free Helvetica-like face, GUST Font License,
 * see third_party/tex-gyre-heros), used for the clock header.
 */
val Heros = FontFamily(
    Font(R.font.toolkit_heros_regular, FontWeight.Normal),
    Font(R.font.toolkit_heros_bold, FontWeight.Bold),
)

/** Choices for the clock header's font, in Toolkit's options. The system ones come with the TV. */
enum class ClockFont(val label: String, val family: FontFamily, val timeWeight: FontWeight) {
    Helvetica("Helvetica", Heros, FontWeight.Normal),
    System("Système", FontFamily.SansSerif, FontWeight.Normal),
    Thin("Fine", FontFamily.SansSerif, FontWeight.Thin),
    Condensed("Condensée", systemFamily("sans-serif-condensed"), FontWeight.Normal),
    Serif("Serif", FontFamily.Serif, FontWeight.Normal),
    Monospace("Machine à écrire", FontFamily.Monospace, FontWeight.Normal),
    ;

    companion object {
        val Default = Helvetica

        fun fromName(name: String?): ClockFont = entries.firstOrNull { it.name == name } ?: Default
    }
}

private fun systemFamily(name: String) =
    FontFamily(Typeface(android.graphics.Typeface.create(name, android.graphics.Typeface.NORMAL)))

val Typography = Typography(
    displayLarge = TextStyle(fontWeight = FontWeight.Light, fontSize = 64.sp, letterSpacing = (-1.5).sp),
    displaySmall = TextStyle(fontWeight = FontWeight.Light, fontSize = 40.sp, letterSpacing = (-0.8).sp),
    titleLarge = TextStyle(fontWeight = FontWeight.Medium, fontSize = 22.sp),
    titleMedium = TextStyle(fontWeight = FontWeight.Medium, fontSize = 16.sp),
    bodyLarge = TextStyle(fontWeight = FontWeight.Normal, fontSize = 16.sp, letterSpacing = 0.2.sp),
    bodyMedium = TextStyle(fontWeight = FontWeight.Normal, fontSize = 14.sp, letterSpacing = 0.2.sp),
    labelLarge = TextStyle(fontWeight = FontWeight.SemiBold, fontSize = 14.sp, letterSpacing = 2.4.sp),
    labelMedium = TextStyle(fontWeight = FontWeight.Medium, fontSize = 12.sp, letterSpacing = 1.6.sp),
)
