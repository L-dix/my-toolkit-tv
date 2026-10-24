package com.tk.toolkit.tv.launcher.ui

import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.layout.Layout
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.drawText
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.rememberTextMeasurer
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.tv.material3.Text
import com.tk.toolkit.tv.launcher.data.Weather
import com.tk.toolkit.tv.ui.theme.ClockFont
import com.tk.toolkit.tv.ui.theme.ToolkitColors
import kotlinx.coroutines.delay
import kotlin.math.roundToInt
import kotlin.random.Random

/** How often the screensaver's content drifts to a new spot, so no pixel stays lit. */
private const val DRIFT_EVERY_MS = 20_000L
private const val DRIFT_MS = 8_000

/**
 * Toolkit's screensaver, shown over the home screen after a while without input: the time,
 * the weather and (optionally) a few Bordeaux bottles on black, slowly drifting around the
 * screen to spare the OLED panel. Any key dismisses it (handled by the home screen).
 */
@Composable
fun Screensaver(font: ClockFont, weather: Weather?, bottles: Boolean) {
    var target by remember { mutableStateOf(Offset(0.5f, 0.5f)) }
    LaunchedEffect(Unit) {
        while (true) {
            target = Offset(Random.nextFloat(), Random.nextFloat())
            delay(DRIFT_EVERY_MS)
        }
    }
    val x by animateFloatAsState(target.x, tween(DRIFT_MS), label = "x")
    val y by animateFloatAsState(target.y, tween(DRIFT_MS), label = "y")

    // Positions are only read while placing, so drifting relayouts without recomposing.
    Layout(
        content = { SaverContent(font, weather, bottles) },
        modifier = Modifier.fillMaxSize().background(Color.Black),
    ) { measurables, constraints ->
        val content = measurables.first().measure(constraints.copy(minWidth = 0, minHeight = 0))
        layout(constraints.maxWidth, constraints.maxHeight) {
            content.place(
                ((constraints.maxWidth - content.width) * x).roundToInt(),
                ((constraints.maxHeight - content.height) * y).roundToInt(),
            )
        }
    }
}

@Composable
private fun SaverContent(font: ClockFont, weather: Weather?, bottles: Boolean) {
    val clock = rememberClock()
    Row(
        Modifier.padding(48.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(48.dp),
    ) {
        Column {
            Text(
                clock.time,
                style = TextStyle(fontFamily = font.family, fontWeight = font.timeWeight, fontSize = 84.sp),
                color = ToolkitColors.OnSurface.copy(alpha = 0.85f),
            )
            Text(
                "${clock.weekday} ${clock.date}",
                style = TextStyle(fontFamily = font.family, fontWeight = font.dateWeight, fontSize = 22.sp),
                color = ToolkitColors.Accent.copy(alpha = 0.85f),
            )
            weather?.let {
                Row(Modifier.padding(top = 18.dp), verticalAlignment = Alignment.CenterVertically) {
                    Text(it.emoji, fontSize = 44.sp)
                    Column(Modifier.padding(start = 14.dp)) {
                        Text(
                            "${it.temperature}°  ·  ${it.min}° / ${it.max}°",
                            style = TextStyle(fontFamily = font.family, fontWeight = font.dateWeight, fontSize = 24.sp),
                            color = ToolkitColors.OnSurface.copy(alpha = 0.8f),
                        )
                        Text(
                            "${it.city} · ${it.description}",
                            style = TextStyle(fontFamily = font.family, fontWeight = font.dateWeight, fontSize = 16.sp),
                            color = ToolkitColors.OnSurfaceMuted,
                        )
                    }
                }
            }
        }
        if (bottles) {
            Row(horizontalArrangement = Arrangement.spacedBy(18.dp), verticalAlignment = Alignment.Bottom) {
                WineBottle("2000", Modifier.size(width = 70.dp, height = 240.dp))
                WineBottle("2005", Modifier.size(width = 78.dp, height = 268.dp))
                WineBottle("2015", Modifier.size(width = 70.dp, height = 240.dp))
            }
        }
    }
}

/**
 * A Bordeaux bottle drawn in code (straight body, high shoulders, foil capsule) with a cream
 * label in the spirit of Château Margaux's. Our own drawing, not their label or a photo.
 */
@Composable
private fun WineBottle(vintage: String, modifier: Modifier = Modifier) {
    val measurer = rememberTextMeasurer()
    Box(modifier) {
        Canvas(Modifier.fillMaxSize()) {
            val w = size.width
            val h = size.height
            val neck = w * 0.3f
            val neckLeft = (w - neck) / 2
            val shoulderTop = h * 0.30f
            val bodyTop = h * 0.40f
            val radius = w * 0.12f

            val bottle = Path().apply {
                moveTo(neckLeft, 0f)
                lineTo(neckLeft + neck, 0f)
                lineTo(neckLeft + neck, shoulderTop)
                cubicTo(neckLeft + neck, bodyTop - h * 0.04f, w, bodyTop - h * 0.05f, w, bodyTop)
                lineTo(w, h - radius)
                quadraticTo(w, h, w - radius, h)
                lineTo(radius, h)
                quadraticTo(0f, h, 0f, h - radius)
                lineTo(0f, bodyTop)
                cubicTo(0f, bodyTop - h * 0.05f, neckLeft, bodyTop - h * 0.04f, neckLeft, shoulderTop)
                close()
            }
            // Dark green glass, lit from the left.
            drawPath(
                bottle,
                Brush.horizontalGradient(
                    0f to Color(0xFF0B1A10),
                    0.22f to Color(0xFF1F3A26),
                    0.45f to Color(0xFF0A140D),
                    1f to Color(0xFF050906),
                ),
            )
            drawRoundRect(
                Color.White.copy(alpha = 0.10f),
                topLeft = Offset(w * 0.12f, bodyTop + h * 0.03f),
                size = Size(w * 0.06f, h * 0.45f),
                cornerRadius = CornerRadius(w * 0.03f),
            )
            // Deep red foil capsule over the neck.
            drawRect(
                Brush.horizontalGradient(listOf(Color(0xFF5A0A16), Color(0xFF9E1B2C), Color(0xFF4A0711))),
                topLeft = Offset(neckLeft - w * 0.01f, 0f),
                size = Size(neck + w * 0.02f, h * 0.19f),
            )
            drawLine(
                Color(0xFFD4AF37),
                Offset(neckLeft - w * 0.01f, h * 0.17f),
                Offset(neckLeft + neck + w * 0.01f, h * 0.17f),
                strokeWidth = h * 0.006f,
            )

            // Cream label with a thin gold frame.
            val labelTop = h * 0.56f
            val labelLeft = w * 0.08f
            val labelSize = Size(w * 0.84f, h * 0.30f)
            drawRect(Color(0xFFF1E7CF), topLeft = Offset(labelLeft, labelTop), size = labelSize)
            drawRect(
                Color(0xFFB8963E),
                topLeft = Offset(labelLeft + w * 0.04f, labelTop + w * 0.04f),
                size = Size(labelSize.width - w * 0.08f, labelSize.height - w * 0.08f),
                style = Stroke(width = h * 0.003f),
            )
            val ink = Color(0xFF3B2A14)
            val lines = listOf(
                Triple("CHÂTEAU", 0.20f, 7.5f),
                Triple("MARGAUX", 0.38f, 9.5f),
                Triple("Premier Grand Cru", 0.58f, 5.5f),
                Triple(vintage, 0.78f, 8f),
            )
            for ((text, at, sizeSp) in lines) {
                val layout = measurer.measure(
                    text,
                    TextStyle(
                        color = ink,
                        fontFamily = FontFamily.Serif,
                        fontWeight = if (sizeSp > 7f) FontWeight.Bold else FontWeight.Normal,
                        fontSize = sizeSp.sp,
                        textAlign = TextAlign.Center,
                    ),
                )
                drawText(
                    layout,
                    topLeft = Offset(
                        labelLeft + (labelSize.width - layout.size.width) / 2,
                        labelTop + labelSize.height * at - layout.size.height / 2,
                    ),
                )
            }
        }
    }
}
