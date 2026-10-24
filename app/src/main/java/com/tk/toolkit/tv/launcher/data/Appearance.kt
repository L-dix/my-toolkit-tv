package com.tk.toolkit.tv.launcher.data

import androidx.compose.ui.graphics.Color

/** Accent color of titles, the weekday and focus marks. */
enum class AccentColor(val label: String, val color: Color) {
    Blue("Bleu", Color(0xFF8FA8FF)),
    Red("Rouge", Color(0xFFFF6B7A)),
    Bordeaux("Bordeaux", Color(0xFFC2185B)),
    Gold("Or", Color(0xFFFFC857)),
    Green("Vert", Color(0xFF7CE0A3)),
    Violet("Violet", Color(0xFFB89CFF)),
    Pink("Rose", Color(0xFFFF8FCB)),
    Cyan("Cyan", Color(0xFF6FE3FF)),
    White("Blanc", Color(0xFFF2F3F7)),
}

/** Background of the home screen. */
enum class HomeBackground(val label: String) {
    Black("Noir"),
    Halo("Halo de la couleur d'accent"),
    Aurora("Aurore (bleu et vert)"),
    Wine("Bordeaux"),
    Night("Bleu nuit"),
}

/** Dims the home screen, always or in the evening. */
enum class NightMode(val label: String) {
    Off("Désactivé"),
    Evening("Le soir (21 h – 7 h)"),
    Always("Toujours"),
}

/** Cities offered for the header weather. */
enum class WeatherCity(val label: String, val latitude: String, val longitude: String) {
    Paris("Paris", "48.8566", "2.3522"),
    Bordeaux("Bordeaux", "44.8378", "-0.5792"),
    Margaux("Margaux", "45.0400", "-0.6770"),
    Lyon("Lyon", "45.7640", "4.8357"),
    Marseille("Marseille", "43.2965", "5.3698"),
    Lille("Lille", "50.6292", "3.0573"),
    Toulouse("Toulouse", "43.6047", "1.4442"),
    Nice("Nice", "43.7102", "7.2620"),
    Nantes("Nantes", "47.2184", "-1.5536"),
    Strasbourg("Strasbourg", "48.5734", "7.7521"),
    Rennes("Rennes", "48.1173", "-1.6778"),
    Montpellier("Montpellier", "43.6108", "3.8767"),
}

/** How much weather the header shows. */
enum class WeatherDetail(val label: String) {
    Simple("Simple (température et ciel)"),
    Detailed("Détaillée (+ min/max et ressenti)"),
    Full("Complète (+ pluie et vent)"),
}

/** Everything about the home screen's look, set in Toolkit's options; defaults are the original. */
data class Appearance(
    val columns: Int = 5,
    val accent: AccentColor = AccentColor.Blue,
    val background: HomeBackground = HomeBackground.Black,
    val nightMode: NightMode = NightMode.Off,
    val showRecent: Boolean = true,
    val weatherCity: WeatherCity = WeatherCity.Paris,
    val weatherDetail: WeatherDetail = WeatherDetail.Simple,
    val screensaverOn: Boolean = true,
    val screensaverDelayMs: Long = 5 * 60_000L,
    val screensaverBottles: Boolean = true,
) {
    companion object {
        val COLUMN_CHOICES = listOf(3, 4, 5, 6, 7)

        /** Shortest delay is 30 seconds. */
        val SCREENSAVER_DELAYS = listOf(30_000L, 60_000L, 2 * 60_000L, 5 * 60_000L, 10 * 60_000L, 15 * 60_000L, 30 * 60_000L)
    }
}
