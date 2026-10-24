package com.tk.toolkit.tv.launcher.ui

import android.text.format.DateFormat
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Settings
import androidx.compose.material.icons.outlined.SortByAlpha
import androidx.compose.material.icons.outlined.Tune
import androidx.compose.material.icons.outlined.Speaker
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.produceState
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.text.rememberTextMeasurer
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.tv.material3.Icon
import androidx.tv.material3.IconButton
import androidx.tv.material3.IconButtonDefaults
import androidx.tv.material3.MaterialTheme
import androidx.tv.material3.Text
import com.tk.toolkit.tv.launcher.data.Weather
import com.tk.toolkit.tv.launcher.data.WeatherDetail
import com.tk.toolkit.tv.ui.theme.ClockFont
import com.tk.toolkit.tv.ui.theme.ToolkitColors
import kotlinx.coroutines.delay
import java.time.LocalDateTime
import java.time.format.DateTimeFormatter
import java.util.Locale

internal data class ClockText(val time: String, val meridiem: String?, val weekday: String, val date: String)

@Composable
fun ClockHeader(
    font: ClockFont,
    weather: Weather?,
    weatherDetail: WeatherDetail,
    onOpenOptions: () -> Unit,
    onSortApps: () -> Unit,
    onOpenAudioOutput: () -> Unit,
    onOpenSettings: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val clock = rememberClock()

    val timeStyle = MaterialTheme.typography.displaySmall.copy(
        fontFamily = font.family,
        fontWeight = font.timeWeight,
        fontSize = 30.sp,
        letterSpacing = (-0.5).sp,
        // Same-width digits in fonts that have them.
        fontFeatureSettings = "tnum",
        brush = Brush.verticalGradient(listOf(ToolkitColors.OnSurface, ToolkitColors.Accent)),
    )
    // The time gets the width of its widest possible value ("00:00" … "99:99"), so the date and
    // weather next to it don't shift when the digits change, whatever the font.
    val measurer = rememberTextMeasurer()
    val density = LocalDensity.current
    val timeWidth = remember(font, measurer) {
        val widest = (0..9).maxOf { d -> measurer.measure("$d$d:$d$d", timeStyle).size.width }
        with(density) { widest.toDp() }
    }

    // One compact line, so the rows below get the height.
    Row(modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
        Row(verticalAlignment = Alignment.Bottom) {
            Text(clock.time, style = timeStyle, maxLines = 1, softWrap = false, modifier = Modifier.width(timeWidth))
            clock.meridiem?.let {
                Text(
                    it,
                    style = MaterialTheme.typography.titleMedium.copy(fontFamily = font.family, fontWeight = font.dateWeight, fontSize = 13.sp),
                    color = ToolkitColors.OnSurfaceMuted,
                    modifier = Modifier.padding(start = 5.dp, bottom = 5.dp),
                )
            }
        }
        Spacer(
            Modifier
                .padding(horizontal = 16.dp)
                .size(width = 1.dp, height = 26.dp)
                .background(ToolkitColors.OnSurface.copy(alpha = 0.15f)),
        )
        Column {
            Text(
                clock.weekday.uppercase(),
                style = MaterialTheme.typography.labelLarge.copy(
                    fontFamily = font.family,
                    fontWeight = FontWeight.Bold,
                    fontSize = 11.sp,
                    letterSpacing = 1.5.sp,
                ),
                color = ToolkitColors.Accent,
            )
            Text(
                clock.date,
                style = MaterialTheme.typography.bodyLarge.copy(fontFamily = font.family, fontWeight = font.dateWeight, fontSize = 14.sp, letterSpacing = 0.sp),
                color = ToolkitColors.OnSurfaceMuted,
            )
        }
        weather?.let { WeatherBlock(it, weatherDetail, font) }
        Spacer(Modifier.weight(1f))
        HeaderButton(Icons.Outlined.Tune, "Options de Toolkit", onOpenOptions)
        Spacer(Modifier.width(12.dp))
        HeaderButton(Icons.Outlined.SortByAlpha, "Trier les applis", onSortApps)
        Spacer(Modifier.width(12.dp))
        HeaderButton(Icons.Outlined.Speaker, "Sortie audio", onOpenAudioOutput)
        Spacer(Modifier.width(12.dp))
        HeaderButton(Icons.Outlined.Settings, "Paramètres", onOpenSettings)
    }
}

/** "☁️ 12°" over "Paris · Couvert", set apart from the date by the same divider. */
@Composable
private fun WeatherBlock(weather: Weather, detail: WeatherDetail, font: ClockFont) {
    val top = when (detail) {
        WeatherDetail.Simple -> "${weather.emoji} ${weather.temperature}°"
        else -> "${weather.emoji} ${weather.temperature}°  ·  ${weather.min}° / ${weather.max}°"
    }
    val bottom = when (detail) {
        WeatherDetail.Simple -> listOf(weather.city, weather.description)
        WeatherDetail.Detailed -> listOf(weather.city, weather.description, "ressenti ${weather.feelsLike}°")
        WeatherDetail.Full -> listOf(weather.city, weather.description, "💧 ${weather.rainChance} %", "💨 ${weather.wind} km/h")
    }.filter { it.isNotEmpty() }.joinToString(" · ")
    Spacer(
        Modifier
            .padding(horizontal = 16.dp)
            .size(width = 1.dp, height = 26.dp)
            .background(ToolkitColors.OnSurface.copy(alpha = 0.15f)),
    )
    Column {
        Text(
            top,
            style = MaterialTheme.typography.labelLarge.copy(
                fontFamily = font.family,
                fontWeight = FontWeight.Bold,
                fontSize = 13.sp,
                letterSpacing = 0.sp,
            ),
            color = ToolkitColors.OnSurface,
        )
        Text(
            bottom,
            style = MaterialTheme.typography.bodyLarge.copy(fontFamily = font.family, fontWeight = font.dateWeight, fontSize = 14.sp, letterSpacing = 0.sp),
            color = ToolkitColors.OnSurfaceMuted,
        )
    }
}

@Composable
private fun HeaderButton(icon: ImageVector, description: String, onClick: () -> Unit) {
    HeaderButton(description, onClick) {
        Icon(icon, contentDescription = null, modifier = Modifier.size(22.dp))
    }
}

@Composable
private fun HeaderButton(description: String, onClick: () -> Unit, content: @Composable () -> Unit) {
    IconButton(
        onClick = onClick,
        modifier = Modifier.size(44.dp).semantics { contentDescription = description },
        colors = IconButtonDefaults.colors(
            containerColor = ToolkitColors.OnSurface.copy(alpha = 0.06f),
            contentColor = ToolkitColors.OnSurfaceMuted,
            focusedContainerColor = ToolkitColors.OnSurface,
            focusedContentColor = ToolkitColors.Background,
        ),
    ) {
        content()
    }
}

/** The current time and date, updated on each second. */
@Composable
internal fun rememberClock(): ClockText {
    val context = LocalContext.current
    val clock by produceState(clockText(DateFormat.is24HourFormat(context))) {
        while (true) {
            value = clockText(DateFormat.is24HourFormat(context))
            delay(1_000L - System.currentTimeMillis() % 1_000L)
        }
    }
    return clock
}

private fun clockText(is24Hour: Boolean): ClockText {
    val now = LocalDateTime.now()
    return ClockText(
        time = now.format(DateTimeFormatter.ofPattern(if (is24Hour) "HH:mm" else "h:mm")),
        meridiem = if (is24Hour) null else now.format(DateTimeFormatter.ofPattern("a")).uppercase(),
        weekday = now.format(DateTimeFormatter.ofPattern("EEEE")),
        // "October 20" in English, "20 octobre" in French.
        date = now.format(DateTimeFormatter.ofPattern(DateFormat.getBestDateTimePattern(Locale.getDefault(), "MMMMd"))),
    )
}
