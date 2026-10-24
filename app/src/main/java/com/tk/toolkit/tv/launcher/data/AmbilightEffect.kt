package com.tk.toolkit.tv.launcher.data

import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.lerp
import kotlinx.coroutines.NonCancellable
import kotlinx.coroutines.delay
import kotlinx.coroutines.withContext
import org.json.JSONObject
import kotlin.math.PI
import kotlin.math.cos
import kotlin.math.roundToInt

/**
 * The hold-2 border animation played on the Ambilight LEDs only (nothing on screen), dimmed to
 * [intensity] percent, while the screensaver runs. Uses the TV's local API ([PhilipsTv]).
 */
object AmbilightEffect {

    private const val FRAME_MS = 120L
    private const val MAX_FAILURES = 15

    /**
     * Runs until cancelled (or [maxMs], or the TV stops answering), then gives the LEDs back to
     * the TV with its previous Ambilight setting. Blocking calls: run on an IO dispatcher.
     */
    suspend fun run(tv: PhilipsTv, settings: () -> BorderSettings, intensity: () -> Int, maxMs: Long) {
        val topology = tv.topology()
        val previous = runCatching { tv.currentConfiguration() }.getOrNull()
        val leds = ledPositions(topology)
        check(leds.isNotEmpty()) { "aucune LED Ambilight trouvée" }
        try {
            tv.setMode("manual")
            val start = System.currentTimeMillis()
            var failures = 0
            while (System.currentTimeMillis() - start < maxMs && failures < MAX_FAILURES) {
                val border = settings()
                val elapsed = System.currentTimeMillis() - start
                val progress = (elapsed % border.speed.periodMs).toFloat() / border.speed.periodMs
                val payload = payload(topology, leds, border, progress, intensity() / 100f)
                failures = if (runCatching { tv.setPixels(payload) }.isSuccess) 0 else failures + 1
                delay(FRAME_MS)
            }
        } finally {
            withContext(NonCancellable) {
                runCatching { tv.setMode("internal") }
                previous?.let { runCatching { tv.restoreConfiguration(it) } }
            }
        }
    }

    /** Each LED as (layer side, index, position around the screen from 0 to 1, clockwise). */
    private data class Led(val side: String, val index: Int, val position: Float)

    private fun ledPositions(topology: JSONObject): List<Led> {
        val counts = SIDES.associateWith { topology.optInt(it, 0) }
        val total = counts.values.sum().toFloat()
        if (total == 0f) return emptyList()
        var offset = 0
        return SIDES.flatMap { side ->
            val count = counts.getValue(side)
            (0 until count).map { i -> Led(side, i, (offset + i + 0.5f) / total) }.also { offset += count }
        }
    }

    private fun payload(topology: JSONObject, leds: List<Led>, border: BorderSettings, progress: Float, brightness: Float): String {
        val colors = border.palette.colors
        val sides = JSONObject()
        for (led in leds) {
            val color = colorAt(border.style, colors, led.position, progress) * brightness
            val side = sides.optJSONObject(led.side) ?: JSONObject().also { sides.put(led.side, it) }
            side.put(
                led.index.toString(),
                JSONObject()
                    .put("r", (color.red * 255).roundToInt())
                    .put("g", (color.green * 255).roundToInt())
                    .put("b", (color.blue * 255).roundToInt()),
            )
        }
        // Every layer gets the same colors; sides without LEDs are left out (the TV rejects them).
        val layers = topology.optInt("layers", 1).coerceAtLeast(1)
        return JSONObject().apply { for (layer in 1..layers) put("layer$layer", sides) }.toString()
    }

    /** Same looks as the on-screen border, for an LED at [position] around the screen. */
    private fun colorAt(style: BorderStyle, colors: List<Color>, position: Float, progress: Float): Color = when (style) {
        BorderStyle.Rotation -> sample(colors, position + progress)
        BorderStyle.Comet -> comet(colors, position, progress, count = 1)
        BorderStyle.TwinComets -> comet(colors, position, progress, count = 2)
        BorderStyle.Breathing -> sample(colors, position) * (0.3f + 0.7f * (0.5f - 0.5f * cos(2 * PI * progress).toFloat()))
        BorderStyle.Wave -> sample(colors, triangle(position * 2 + progress * 2))
        BorderStyle.Disco -> colors[(progress * colors.size).toInt().coerceAtMost(colors.lastIndex)]
    }

    /** A bright head moving clockwise with a fading tail behind it. */
    private fun comet(colors: List<Color>, position: Float, progress: Float, count: Int): Color {
        val segment = 1f / count
        val behind = (((progress - position) % segment) + segment) % segment / segment // 0 at the head
        val tail = 0.6f
        if (behind > tail) return Color.Black
        return sample(colors, 1f - behind / tail) * (1f - behind / tail)
    }

    /** The palette as a closed loop, sampled at [x] (any value, wrapped to 0..1). */
    private fun sample(colors: List<Color>, x: Float): Color {
        val wrapped = ((x % 1f) + 1f) % 1f * colors.size
        val i = wrapped.toInt().coerceAtMost(colors.lastIndex)
        return lerp(colors[i], colors[(i + 1) % colors.size], wrapped - i)
    }

    private fun triangle(x: Float): Float {
        val t = ((x % 2f) + 2f) % 2f
        return if (t <= 1f) t else 2f - t
    }

    private operator fun Color.times(factor: Float) =
        Color(red * factor, green * factor, blue * factor, alpha)

    /** Clockwise from the bottom left: up the left side, along the top, down the right, back along the bottom. */
    private val SIDES = listOf("left", "top", "right", "bottom")
}
