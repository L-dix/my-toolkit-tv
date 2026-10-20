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
import androidx.compose.material.icons.outlined.Speaker
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.produceState
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
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
import com.tk.toolkit.tv.ui.theme.Heros
import com.tk.toolkit.tv.ui.theme.ToolkitColors
import kotlinx.coroutines.delay
import java.time.LocalDateTime
import java.time.format.DateTimeFormatter
import java.util.Locale

private data class ClockText(val time: String, val meridiem: String?, val weekday: String, val date: String)

@Composable
fun ClockHeader(
    onOpenKickChannel: () -> Unit,
    onSortApps: () -> Unit,
    onOpenAudioOutput: () -> Unit,
    onOpenSettings: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val context = LocalContext.current
    val clock by produceState(clockText(DateFormat.is24HourFormat(context))) {
        while (true) {
            value = clockText(DateFormat.is24HourFormat(context))
            delay(1_000L - System.currentTimeMillis() % 1_000L)
        }
    }

    // One compact line, so the rows below get the height.
    Row(modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
        Row(verticalAlignment = Alignment.Bottom) {
            Text(
                clock.time,
                style = MaterialTheme.typography.displaySmall.copy(
                    fontFamily = Heros,
                    fontWeight = FontWeight.Normal,
                    fontSize = 30.sp,
                    letterSpacing = (-0.5).sp,
                    brush = Brush.verticalGradient(listOf(ToolkitColors.OnSurface, ToolkitColors.Accent)),
                ),
            )
            clock.meridiem?.let {
                Text(
                    it,
                    style = MaterialTheme.typography.titleMedium.copy(fontFamily = Heros, fontWeight = FontWeight.Normal, fontSize = 13.sp),
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
                    fontFamily = Heros,
                    fontWeight = FontWeight.Bold,
                    fontSize = 11.sp,
                    letterSpacing = 1.5.sp,
                ),
                color = ToolkitColors.Accent,
            )
            Text(
                clock.date,
                style = MaterialTheme.typography.bodyLarge.copy(fontFamily = Heros, fontWeight = FontWeight.Normal, fontSize = 14.sp, letterSpacing = 0.sp),
                color = ToolkitColors.OnSurfaceMuted,
            )
        }
        Spacer(Modifier.weight(1f))
        HeaderButton("Kick · ryu7z", onOpenKickChannel) {
            Text("🥵", fontSize = 20.sp)
        }
        Spacer(Modifier.width(12.dp))
        HeaderButton(Icons.Outlined.SortByAlpha, "Trier les applis", onSortApps)
        Spacer(Modifier.width(12.dp))
        HeaderButton(Icons.Outlined.Speaker, "Sortie audio", onOpenAudioOutput)
        Spacer(Modifier.width(12.dp))
        HeaderButton(Icons.Outlined.Settings, "Paramètres", onOpenSettings)
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
