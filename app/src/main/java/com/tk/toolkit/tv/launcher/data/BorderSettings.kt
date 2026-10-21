package com.tk.toolkit.tv.launcher.data

import androidx.compose.ui.graphics.Color

/** How the screen border of the hold-2 easter egg moves. */
enum class BorderStyle(val label: String) {
    Rotation("Arc-en-ciel tournant"),
    Comet("Comète"),
    TwinComets("Double comète"),
    Breathing("Respiration"),
    Wave("Vague"),
    Disco("Disco"),
}

/** Color shades of the border, cycled through by every style. */
enum class BorderPalette(val label: String, val colors: List<Color>) {
    Rainbow("Arc-en-ciel", 0xFFFF1744, 0xFFFF9100, 0xFFFFEA00, 0xFF00E676, 0xFF00E5FF, 0xFF2979FF, 0xFFD500F9),
    Fire("Feu", 0xFFFF1744, 0xFFFF6D00, 0xFFFFD600, 0xFFFF3D00),
    Ocean("Océan", 0xFF00B0FF, 0xFF1DE9B6, 0xFF2962FF, 0xFF00E5FF),
    Neon("Néon", 0xFFFF00E5, 0xFF00E5FF, 0xFF7C4DFF),
    Sunset("Coucher de soleil", 0xFFFF4081, 0xFFFF9100, 0xFFFFD180, 0xFF7C4DFF),
    Vaporwave("Vaporwave", 0xFFFF71CE, 0xFF01CDFE, 0xFF05FFA1, 0xFFB967FF),
    Forest("Forêt", 0xFF00E676, 0xFF76FF03, 0xFF00C853, 0xFFC6FF00),
    Ice("Glace", 0xFFFFFFFF, 0xFF80D8FF, 0xFF40C4FF, 0xFFB3E5FC),
    Gold("Or", 0xFFFFD54F, 0xFFFFAB00, 0xFFFFF8E1, 0xFFFF8F00),
    DeepRed("Rouge profond", 0xFFFF1744, 0xFF8B0A1A, 0xFFFF5252, 0xFFB71C1C),
    Monochrome("Noir et blanc", 0xFFFFFFFF, 0xFF616161, 0xFFE0E0E0, 0xFF212121),
    ;

    constructor(label: String, vararg argb: Long) : this(label, argb.map { Color(it) })
}

enum class BorderThickness(val label: String, val dp: Float) {
    Thin("Fine", 3f),
    Normal("Normale", 5f),
    Thick("Épaisse", 9f),
    Huge("Très épaisse", 14f),
}

/** Duration of one full cycle of the animation. */
enum class BorderSpeed(val label: String, val periodMs: Int) {
    Slow("Lente", 12_000),
    Normal("Normale", 6_000),
    Fast("Rapide", 3_000),
    Turbo("Turbo", 1_200),
}

/** Soft halo drawn under the line. */
enum class BorderGlow(val label: String, val alpha: Float) {
    None("Aucun", 0f),
    Soft("Léger", 0.3f),
    Strong("Fort", 0.6f),
}

/** Everything about the hold-2 border; the defaults are the original turning rainbow. */
data class BorderSettings(
    val style: BorderStyle = BorderStyle.Rotation,
    val palette: BorderPalette = BorderPalette.Rainbow,
    val thickness: BorderThickness = BorderThickness.Normal,
    val speed: BorderSpeed = BorderSpeed.Normal,
    val glow: BorderGlow = BorderGlow.Soft,
)
