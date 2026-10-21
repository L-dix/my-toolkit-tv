package com.tk.toolkit.tv.launcher.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalSoftwareKeyboardController
import androidx.compose.ui.text.TextRange
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.TextFieldValue
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import androidx.compose.ui.unit.sp
import androidx.tv.material3.Button
import androidx.tv.material3.ListItem
import androidx.tv.material3.ListItemDefaults
import androidx.tv.material3.MaterialTheme
import androidx.tv.material3.OutlinedButton
import androidx.tv.material3.Text
import com.tk.toolkit.tv.launcher.ShortcutSettings
import com.tk.toolkit.tv.launcher.data.Shortcuts
import com.tk.toolkit.tv.ui.theme.ClockFont
import com.tk.toolkit.tv.ui.theme.ToolkitColors

private val PanelShape = RoundedCornerShape(24.dp)

private enum class Field { Kick, Playlist, Font }

/** Toolkit's own options: the clock font and what the held 1 and 3 keys open. Styled like the app menu. */
@Composable
fun OptionsDialog(
    settings: ShortcutSettings,
    clockFont: ClockFont,
    onClockFont: (ClockFont) -> Unit,
    onDismiss: () -> Unit,
    onKickChannel: (String) -> Unit,
    onYoutubePlaylist: (String) -> Unit,
    onHoldDurations: (kickMs: Long, rainbowMs: Long, playlistMs: Long) -> Unit,
) {
    var editing by remember { mutableStateOf<Field?>(null) }
    val context = LocalContext.current
    val versions = remember { installedVersions(context) }
    val firstAction = remember { FocusRequester() }
    LaunchedEffect(Unit) { runCatching { firstAction.requestFocus() } }

    Dialog(onDismissRequest = onDismiss) {
        Column(
            Modifier
                .width(440.dp)
                .clip(PanelShape)
                .background(ToolkitColors.SurfaceRaised)
                .border(1.dp, Color.White.copy(alpha = 0.08f), PanelShape)
                .ignoreHeldSelectKey(),
        ) {
            Column(Modifier.padding(start = 28.dp, end = 28.dp, top = 22.dp)) {
                Text("Options de Toolkit", style = MaterialTheme.typography.headlineSmall, color = ToolkitColors.OnSurface)
                Text(
                    "Affichage et raccourcis de la télécommande",
                    style = MaterialTheme.typography.bodyMedium,
                    color = ToolkitColors.OnSurfaceMuted,
                )
                Text(versions, style = MaterialTheme.typography.bodyMedium, color = ToolkitColors.OnSurfaceMuted)
            }
            Column(
                Modifier
                    .heightIn(max = 420.dp)
                    .verticalScroll(rememberScrollState())
                    .padding(12.dp),
            ) {
                ActionItem(
                    "Police de l'heure et de la date",
                    clockFont.label,
                    { editing = Field.Font },
                    Modifier.focusRequester(firstAction),
                )
                ActionItem(
                    "Chaîne Kick · touche 1",
                    "kick.com/${settings.kickChannel}",
                    { editing = Field.Kick },
                )
                ActionItem(
                    "Playlist YouTube · touche 3",
                    "youtube.com/playlist?list=${settings.youtubePlaylist}",
                    { editing = Field.Playlist },
                )
                // OK steps through the durations, so no extra dialog is needed.
                ActionItem("Durée d'appui · touche 1 (Kick)", holdLabel(settings.kickHoldMs), {
                    onHoldDurations(nextHold(settings.kickHoldMs), settings.rainbowHoldMs, settings.playlistHoldMs)
                })
                ActionItem("Durée d'appui · touche 2 (arc-en-ciel)", holdLabel(settings.rainbowHoldMs), {
                    onHoldDurations(settings.kickHoldMs, nextHold(settings.rainbowHoldMs), settings.playlistHoldMs)
                })
                ActionItem("Durée d'appui · touche 3 (YouTube)", holdLabel(settings.playlistHoldMs), {
                    onHoldDurations(settings.kickHoldMs, settings.rainbowHoldMs, nextHold(settings.playlistHoldMs))
                })
            }
        }
    }

    when (editing) {
        Field.Kick -> TextInputDialog(
            title = "Chaîne Kick",
            hint = "Colle le lien de la chaîne (kick.com/…) ou son nom",
            initial = "https://kick.com/${settings.kickChannel}",
            parse = Shortcuts::parseKickChannel,
            onDismiss = { editing = null },
            onSave = { onKickChannel(it); editing = null },
        )
        Field.Playlist -> TextInputDialog(
            title = "Playlist YouTube",
            hint = "Colle le lien de la playlist (youtube.com/playlist?list=…)",
            initial = "https://youtube.com/playlist?list=${settings.youtubePlaylist}",
            parse = Shortcuts::parsePlaylistId,
            onDismiss = { editing = null },
            onSave = { onYoutubePlaylist(it); editing = null },
        )
        Field.Font -> FontPickerDialog(
            current = clockFont,
            onDismiss = { editing = null },
            onPick = { onClockFont(it); editing = null },
        )
        null -> Unit
    }
}

/** "Toolkit 1.0.16 · Installer 1.0.12", to spot an app that wasn't updated. */
private fun installedVersions(context: android.content.Context): String {
    fun version(pkg: String) = runCatching { context.packageManager.getPackageInfo(pkg, 0).versionName }.getOrNull()
    val installer = version("com.tk.toolkit.tv.installer")?.let { "Installer $it" } ?: "Installer absent"
    return "Toolkit ${version(context.packageName) ?: "?"} · $installer"
}

private val HoldSteps = listOf(500L, 1_000L, 1_500L, 2_000L, 3_000L, 4_000L, 5_000L)

private fun nextHold(current: Long): Long = HoldSteps.firstOrNull { it > current } ?: HoldSteps.first()

private fun holdLabel(ms: Long): String {
    val seconds = if (ms % 1_000L == 0L) "${ms / 1_000L}" else "%.1f".format(ms / 1_000f).replace('.', ',')
    return "$seconds s · OK pour changer"
}

/** Each font previewed as the header would show it; the current one is checked. */
@Composable
private fun FontPickerDialog(current: ClockFont, onDismiss: () -> Unit, onPick: (ClockFont) -> Unit) {
    val currentItem = remember { FocusRequester() }
    LaunchedEffect(Unit) { currentItem.requestFocusWhenReady() }

    Dialog(onDismissRequest = onDismiss) {
        Column(
            Modifier
                .width(440.dp)
                .clip(PanelShape)
                .background(ToolkitColors.SurfaceRaised)
                .border(1.dp, Color.White.copy(alpha = 0.08f), PanelShape)
                .ignoreHeldSelectKey(),
        ) {
            Text(
                "Police de l'heure et de la date",
                style = MaterialTheme.typography.headlineSmall,
                color = ToolkitColors.OnSurface,
                modifier = Modifier.padding(start = 28.dp, end = 28.dp, top = 22.dp),
            )
            Column(
                Modifier
                    .heightIn(max = 400.dp)
                    .verticalScroll(rememberScrollState())
                    .padding(12.dp),
            ) {
                ClockFont.entries.forEach { font ->
                    ListItem(
                        selected = font == current,
                        onClick = { onPick(font) },
                        modifier = if (font == current) Modifier.focusRequester(currentItem) else Modifier,
                        headlineContent = {
                            Text(
                                "21:34",
                                style = MaterialTheme.typography.titleLarge.copy(
                                    fontFamily = font.family,
                                    fontWeight = font.timeWeight,
                                    fontSize = 24.sp,
                                ),
                            )
                        },
                        supportingContent = {
                            Text(
                                if (font == current) "${font.label} · actuelle" else font.label,
                                style = MaterialTheme.typography.bodyMedium.copy(
                                    fontFamily = font.family,
                                    fontWeight = font.dateWeight,
                                ),
                            )
                        },
                        shape = ListItemDefaults.shape(RoundedCornerShape(14.dp)),
                        scale = ListItemDefaults.scale(focusedScale = 1.02f),
                        colors = ListItemDefaults.colors(
                            containerColor = Color.Transparent,
                            contentColor = ToolkitColors.OnSurface.copy(alpha = 0.7f),
                            selectedContainerColor = ToolkitColors.Accent.copy(alpha = 0.12f),
                            selectedContentColor = ToolkitColors.OnSurface,
                            focusedContainerColor = ToolkitColors.OnSurface.copy(alpha = 0.12f),
                            focusedContentColor = ToolkitColors.OnSurface,
                        ),
                    )
                }
            }
        }
    }
}

/**
 * One-line text entry with the TV keyboard. Text typed or pasted from the Google TV phone app
 * lands here; [parse] turns it into the value to save, or null to flag it as not recognized.
 */
@Composable
private fun TextInputDialog(
    title: String,
    hint: String,
    initial: String,
    parse: (String) -> String?,
    onDismiss: () -> Unit,
    onSave: (String) -> Unit,
) {
    var text by remember { mutableStateOf(TextFieldValue(initial, TextRange(0, initial.length))) }
    var invalid by remember { mutableStateOf(false) }
    val field = remember { FocusRequester() }
    val keyboard = LocalSoftwareKeyboardController.current
    LaunchedEffect(Unit) {
        field.requestFocusWhenReady()
        keyboard?.show()
    }
    fun save() {
        val value = parse(text.text)
        if (value == null) invalid = true else onSave(value)
    }

    Dialog(onDismissRequest = onDismiss, properties = DialogProperties(usePlatformDefaultWidth = false)) {
        Column(
            Modifier
                .width(600.dp)
                .clip(PanelShape)
                .background(ToolkitColors.SurfaceRaised)
                .border(1.dp, Color.White.copy(alpha = 0.08f), PanelShape)
                .padding(28.dp),
        ) {
            Text(title, style = MaterialTheme.typography.headlineSmall, color = ToolkitColors.OnSurface)
            Text(hint, style = MaterialTheme.typography.bodyMedium, color = ToolkitColors.OnSurfaceMuted)
            val fieldShape = RoundedCornerShape(12.dp)
            BasicTextField(
                value = text,
                onValueChange = {
                    text = it
                    invalid = false
                },
                singleLine = true,
                textStyle = MaterialTheme.typography.bodyLarge.copy(color = ToolkitColors.OnSurface),
                cursorBrush = SolidColor(ToolkitColors.Accent),
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Uri, imeAction = ImeAction.Done),
                keyboardActions = KeyboardActions(onDone = { save() }),
                modifier = Modifier
                    .padding(top = 18.dp)
                    .fillMaxWidth()
                    .focusRequester(field)
                    .background(ToolkitColors.Background, fieldShape)
                    .border(
                        1.dp,
                        if (invalid) ToolkitColors.Danger else Color.White.copy(alpha = 0.2f),
                        fieldShape,
                    )
                    .padding(horizontal = 16.dp, vertical = 12.dp),
            )
            if (invalid) {
                Text(
                    "Lien non reconnu",
                    style = MaterialTheme.typography.bodyMedium,
                    color = ToolkitColors.Danger,
                    modifier = Modifier.padding(top = 8.dp),
                )
            }
            Row(Modifier.padding(top = 20.dp), horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                Button(onClick = { save() }) { Text("Enregistrer") }
                OutlinedButton(onClick = onDismiss) { Text("Annuler") }
            }
        }
    }
}
