package com.tk.toolkit.tv.launcher.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
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
import androidx.compose.ui.platform.LocalSoftwareKeyboardController
import androidx.compose.ui.text.TextRange
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.TextFieldValue
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import androidx.tv.material3.Button
import androidx.tv.material3.MaterialTheme
import androidx.tv.material3.OutlinedButton
import androidx.tv.material3.Text
import com.tk.toolkit.tv.launcher.ShortcutSettings
import com.tk.toolkit.tv.launcher.data.Shortcuts
import com.tk.toolkit.tv.ui.theme.ToolkitColors

private val PanelShape = RoundedCornerShape(24.dp)

private enum class Field { Kick, Playlist }

/** Toolkit's own options: what the held 1 and 3 keys open. Styled like the app menu. */
@Composable
fun OptionsDialog(
    settings: ShortcutSettings,
    onDismiss: () -> Unit,
    onKickChannel: (String) -> Unit,
    onYoutubePlaylist: (String) -> Unit,
) {
    var editing by remember { mutableStateOf<Field?>(null) }
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
                    "Raccourcis de la télécommande",
                    style = MaterialTheme.typography.bodyMedium,
                    color = ToolkitColors.OnSurfaceMuted,
                )
            }
            Column(Modifier.padding(12.dp)) {
                ActionItem(
                    "Chaîne Kick · touche 1",
                    "kick.com/${settings.kickChannel}",
                    { editing = Field.Kick },
                    Modifier.focusRequester(firstAction),
                )
                ActionItem(
                    "Playlist YouTube · touche 3",
                    "youtube.com/playlist?list=${settings.youtubePlaylist}",
                    { editing = Field.Playlist },
                )
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
        null -> Unit
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
