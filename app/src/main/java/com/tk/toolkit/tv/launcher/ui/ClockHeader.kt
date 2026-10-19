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
import androidx.compose.material.icons.outlined.Speaker
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.produceState
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.tv.material3.Icon
import androidx.tv.material3.IconButton
import androidx.tv.material3.IconButtonDefaults
import androidx.tv.material3.MaterialTheme
import androidx.tv.material3.Text
import com.tk.toolkit.tv.ui.theme.ToolkitColors
import kotlinx.coroutines.delay
import java.time.LocalDateTime
import java.time.format.DateTimeFormatter

private data class ClockText(val time: String, val meridiem: String?, val weekday: String, val date: String)

@Composable
fun ClockHeader(onOpenAudioOutput: () -> Unit, onOpenSettings: () -> Unit, modifier: Modifier = Modifier) {
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
                    brush = Brush.verticalGradient(listOf(ToolkitColors.OnSurface, ToolkitColors.Accent)),
                ),
            )
            clock.meridiem?.let {
                Text(
                    it,
                    style = MaterialTheme.typography.titleMedium,
                    color = ToolkitColors.OnSurfaceMuted,
                    modifier = Modifier.padding(start = 6.dp, bottom = 6.dp),
                )
            }
        }
        Spacer(
            Modifier
                .padding(horizontal = 20.dp)
                .size(width = 1.dp, height = 32.dp)
                .background(ToolkitColors.OnSurface.copy(alpha = 0.15f)),
        )
        Column {
            Text(clock.weekday.uppercase(), style = MaterialTheme.typography.labelLarge, color = ToolkitColors.Accent)
            Text(clock.date, style = MaterialTheme.typography.bodyLarge, color = ToolkitColors.OnSurfaceMuted)
        }
        Spacer(Modifier.weight(1f))
        HeaderButton(Icons.Outlined.Speaker, "Audio output", onOpenAudioOutput)
        Spacer(Modifier.width(12.dp))
        HeaderButton(Icons.Outlined.Settings, "Settings", onOpenSettings)
    }
}

@Composable
private fun HeaderButton(icon: ImageVector, description: String, onClick: () -> Unit) {
    IconButton(
        onClick = onClick,
        modifier = Modifier.size(44.dp),
        colors = IconButtonDefaults.colors(
            containerColor = ToolkitColors.OnSurface.copy(alpha = 0.06f),
            contentColor = ToolkitColors.OnSurfaceMuted,
            focusedContainerColor = ToolkitColors.OnSurface,
            focusedContentColor = ToolkitColors.Background,
        ),
    ) {
        Icon(icon, contentDescription = description, modifier = Modifier.size(22.dp))
    }
}

private fun clockText(is24Hour: Boolean): ClockText {
    val now = LocalDateTime.now()
    return ClockText(
        time = now.format(DateTimeFormatter.ofPattern(if (is24Hour) "HH:mm" else "h:mm")),
        meridiem = if (is24Hour) null else now.format(DateTimeFormatter.ofPattern("a")).uppercase(),
        weekday = now.format(DateTimeFormatter.ofPattern("EEEE")),
        date = now.format(DateTimeFormatter.ofPattern("MMMM d")),
    )
}
