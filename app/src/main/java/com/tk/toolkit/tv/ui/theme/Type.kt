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

/**
 * Choices for the clock header's font, in Toolkit's options. The system ones come with the TV;
 * the bold sans-serifs are bundled (SIL Open Font License, see third_party/fonts) as a single
 * bold weight, used for the time and the date alike.
 */
enum class ClockFont(
    val label: String,
    val family: FontFamily,
    val timeWeight: FontWeight = FontWeight.Bold,
    val dateWeight: FontWeight = FontWeight.Bold,
) {
    Helvetica("Helvetica", Heros, FontWeight.Normal, FontWeight.Normal),
    HelveticaBold("Helvetica gras", Heros),
    System("Système", FontFamily.SansSerif, FontWeight.Normal, FontWeight.Normal),
    SystemBold("Système gras", FontFamily.SansSerif),
    SystemBlack("Système noir", FontFamily.SansSerif, FontWeight.Black, FontWeight.Bold),
    Thin("Fine", FontFamily.SansSerif, FontWeight.Thin, FontWeight.Light),
    Condensed("Condensée", systemFamily("sans-serif-condensed"), FontWeight.Normal, FontWeight.Normal),
    CondensedBold("Condensée grasse", systemFamily("sans-serif-condensed")),
    Anton("Anton", bundled(R.font.clock_anton)),
    ArchivoBlack("Archivo Black", bundled(R.font.clock_archivo_black)),
    Barlow("Barlow", bundled(R.font.clock_barlow)),
    BarlowCondensed("Barlow Condensed", bundled(R.font.clock_barlow_condensed)),
    Bebas("Bebas Neue", bundled(R.font.clock_bebas)),
    DmSans("DM Sans", bundled(R.font.clock_dm_sans)),
    Exo2("Exo 2", bundled(R.font.clock_exo2)),
    Figtree("Figtree", bundled(R.font.clock_figtree)),
    Inter("Inter", bundled(R.font.clock_inter)),
    Jakarta("Plus Jakarta Sans", bundled(R.font.clock_jakarta)),
    Kanit("Kanit", bundled(R.font.clock_kanit)),
    Lexend("Lexend", bundled(R.font.clock_lexend)),
    Manrope("Manrope", bundled(R.font.clock_manrope)),
    Montserrat("Montserrat", bundled(R.font.clock_montserrat)),
    Nunito("Nunito", bundled(R.font.clock_nunito)),
    Oswald("Oswald", bundled(R.font.clock_oswald)),
    Outfit("Outfit", bundled(R.font.clock_outfit)),
    Poppins("Poppins", bundled(R.font.clock_poppins)),
    Rubik("Rubik", bundled(R.font.clock_rubik)),
    Sora("Sora", bundled(R.font.clock_sora)),
    SpaceGrotesk("Space Grotesk", bundled(R.font.clock_space_grotesk)),
    Teko("Teko", bundled(R.font.clock_teko)),
    Urbanist("Urbanist", bundled(R.font.clock_urbanist)),
    WorkSans("Work Sans", bundled(R.font.clock_work_sans)),
    Serif("Serif", FontFamily.Serif, FontWeight.Normal, FontWeight.Normal),
    Monospace("Machine à écrire", FontFamily.Monospace, FontWeight.Normal, FontWeight.Normal),
    ;

    companion object {
        val Default = Helvetica

        fun fromName(name: String?): ClockFont = entries.firstOrNull { it.name == name } ?: Default
    }
}

private fun bundled(font: Int) = FontFamily(Font(font, FontWeight.Bold))

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
