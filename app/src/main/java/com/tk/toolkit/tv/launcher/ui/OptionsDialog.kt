package com.tk.toolkit.tv.launcher.ui

import androidx.compose.foundation.background
import com.google.zxing.qrcode.decoder.ErrorCorrectionLevel
import com.google.zxing.qrcode.QRCodeWriter
import com.google.zxing.EncodeHintType
import com.google.zxing.BarcodeFormat
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.geometry.Offset
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.Canvas
import androidx.compose.ui.focus.onFocusChanged
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.focusable
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
import com.tk.toolkit.tv.core.ToolkitAccessibilityService
import com.tk.toolkit.tv.launcher.data.AccentColor
import com.tk.toolkit.tv.launcher.data.AmbilightController
import com.tk.toolkit.tv.launcher.data.AppActions
import com.tk.toolkit.tv.launcher.data.Appearance
import com.tk.toolkit.tv.launcher.data.BorderGlow
import com.tk.toolkit.tv.launcher.data.HomeBackground
import com.tk.toolkit.tv.launcher.data.NightMode
import com.tk.toolkit.tv.launcher.data.SaverAmbilight
import com.tk.toolkit.tv.launcher.data.ScreensaverMode
import com.tk.toolkit.tv.launcher.data.WeatherCity
import com.tk.toolkit.tv.launcher.data.WeatherDetail
import com.tk.toolkit.tv.launcher.data.BorderPalette
import com.tk.toolkit.tv.launcher.data.BorderSettings
import com.tk.toolkit.tv.launcher.data.BorderSpeed
import com.tk.toolkit.tv.launcher.data.BorderStyle
import com.tk.toolkit.tv.launcher.data.BorderThickness
import com.tk.toolkit.tv.launcher.data.Shortcuts
import com.tk.toolkit.tv.ui.theme.ClockFont
import com.tk.toolkit.tv.ui.theme.ToolkitColors

private val PanelShape = RoundedCornerShape(24.dp)

private enum class Field { Kick, Playlist, Font, KickReport, AmbilightPin, Backup, Restore }

/**
 * Toolkit's own options: clock font, remote shortcuts and their hold durations, and the hold-2
 * border animation. Styled like the app menu; most entries step to their next value on OK.
 */
@Composable
fun OptionsDialog(
    settings: ShortcutSettings,
    clockFont: ClockFont,
    rainbowOn: Boolean,
    border: BorderSettings,
    appearance: Appearance,
    ambilightStatus: String,
    onDismiss: () -> Unit,
    onAmbilightPair: (onCodeShown: () -> Unit) -> Unit,
    onAmbilightPin: (String) -> Unit,
    onAmbilightTest: () -> Unit,
    onAmbilightForget: () -> Unit,
    onTestScreensaver: () -> Unit,
    onExportBackup: () -> String,
    onImportBackup: (String) -> Boolean,
    onAppearance: (Appearance) -> Unit,
    onClockFont: (ClockFont) -> Unit,
    onShortcuts: (ShortcutSettings) -> Unit,
    onToggleRainbow: () -> Unit,
    onBorder: (BorderSettings) -> Unit,
) {
    var editing by remember { mutableStateOf<Field?>(null) }
    val context = LocalContext.current
    val versions = remember { installedVersions(context) }
    val firstAction = remember { FocusRequester() }
    LaunchedEffect(Unit) { runCatching { firstAction.requestFocus() } }

    Dialog(onDismissRequest = onDismiss) {
        Column(
            Modifier
                .width(460.dp)
                .clip(PanelShape)
                .background(ToolkitColors.SurfaceRaised)
                .border(1.dp, Color.White.copy(alpha = 0.08f), PanelShape)
                .ignoreHeldSelectKey(),
        ) {
            Column(Modifier.padding(start = 28.dp, end = 28.dp, top = 22.dp)) {
                Text("Options de Toolkit", style = MaterialTheme.typography.headlineSmall, color = ToolkitColors.OnSurface)
                Text(versions, style = MaterialTheme.typography.bodyMedium, color = ToolkitColors.OnSurfaceMuted)
            }
            Column(
                Modifier
                    .heightIn(max = 420.dp)
                    .verticalScroll(rememberScrollState())
                    .padding(12.dp),
            ) {
                SectionLabel("Affichage")
                ActionItem(
                    "Police de l'heure et de la date",
                    clockFont.label,
                    { editing = Field.Font },
                    Modifier.focusRequester(firstAction),
                )
                ActionItem("Applis par ligne", stepLabel("${appearance.columns}"), {
                    onAppearance(appearance.copy(columns = next(Appearance.COLUMN_CHOICES, appearance.columns)))
                })
                ActionItem("Couleur d'accent", stepLabel(appearance.accent.label), {
                    onAppearance(appearance.copy(accent = next(AccentColor.entries, appearance.accent)))
                })
                ActionItem("Fond", stepLabel(appearance.background.label), {
                    onAppearance(appearance.copy(background = next(HomeBackground.entries, appearance.background)))
                })
                ActionItem("Mode nuit (assombrit l'accueil)", stepLabel(appearance.nightMode.label), {
                    onAppearance(appearance.copy(nightMode = next(NightMode.entries, appearance.nightMode)))
                })
                ActionItem(
                    "Rangée « Récemment utilisées »",
                    stepLabel(if (appearance.showRecent) "Affichée" else "Masquée"),
                    { onAppearance(appearance.copy(showRecent = !appearance.showRecent)) },
                )

                SectionLabel("Météo")
                ActionItem("Ville", stepLabel(appearance.weatherCity.label), {
                    onAppearance(appearance.copy(weatherCity = next(WeatherCity.entries, appearance.weatherCity)))
                })
                ActionItem("Informations", stepLabel(appearance.weatherDetail.label), {
                    onAppearance(appearance.copy(weatherDetail = next(WeatherDetail.entries, appearance.weatherDetail)))
                })
                ActionItem("Revenir à la météo d'origine", "Paris, affichage simple", {
                    onAppearance(appearance.copy(weatherCity = WeatherCity.Paris, weatherDetail = WeatherDetail.Simple))
                })

                SectionLabel("Économiseur d'écran")
                ActionItem("Économiseur", stepLabel(if (appearance.screensaverOn) "Activé" else "Désactivé"), {
                    onAppearance(appearance.copy(screensaverOn = !appearance.screensaverOn))
                })
                ActionItem("Lequel", stepLabel(appearance.screensaverMode.label), {
                    onAppearance(appearance.copy(screensaverMode = next(ScreensaverMode.entries, appearance.screensaverMode)))
                })
                ActionItem(
                    "Régler l'économiseur de la TV",
                    "Son délai et son contenu (il démarre seul, Toolkit ne peut pas le lancer)",
                    { AppActions.openScreensaverSettings(context) },
                )
                ActionItem("Délai de l'économiseur Toolkit", holdLabel(appearance.screensaverDelayMs).let(::minutesLabel), {
                    onAppearance(
                        appearance.copy(screensaverDelayMs = nextStep(Appearance.SCREENSAVER_DELAYS, appearance.screensaverDelayMs))
                    )
                })
                ActionItem("Lancer l'économiseur maintenant", "Pour tester, sans attendre le délai", onTestScreensaver)
                ActionItem(
                    "Bouteilles de Château Margaux",
                    stepLabel(if (appearance.screensaverBottles) "Affichées" else "Masquées"),
                    { onAppearance(appearance.copy(screensaverBottles = !appearance.screensaverBottles)) },
                )

                SectionLabel("Ambilight pendant l'économiseur")
                ActionItem("Associer Toolkit à l'Ambilight", ambilightStatus, {
                    onAmbilightPair { editing = Field.AmbilightPin }
                })
                ActionItem("Ambilight pendant l'économiseur", stepLabel(appearance.saverAmbilight.label), {
                    onAppearance(appearance.copy(saverAmbilight = next(SaverAmbilight.entries, appearance.saverAmbilight)))
                })
                ActionItem("Puissance de l'animation Toolkit", stepLabel("${appearance.ambilightIntensity} %"), {
                    onAppearance(
                        appearance.copy(ambilightIntensity = next(Appearance.AMBILIGHT_INTENSITIES, appearance.ambilightIntensity))
                    )
                })
                ActionItem(
                    "Tester 15 secondes",
                    AmbilightController.lastError?.let { "Dernière erreur : $it" }
                        ?: "Le choix ci-dessus (l'animation Toolkit s'il est « inchangé »)",
                    onAmbilightTest,
                )
                ActionItem("Oublier l'association", "À refaire si la TV a été réinitialisée", onAmbilightForget)

                SectionLabel("Sauvegarde")
                ActionItem("Sauvegarder mes réglages", "Un QR code et un texte à garder (notes de l'iPhone)", {
                    editing = Field.Backup
                })
                ActionItem("Restaurer des réglages", "Colle le texte sauvegardé", { editing = Field.Restore })

                SectionLabel("Minuterie de veille")
                SleepTimerItem()

                SectionLabel("Raccourcis")
                ActionItem("Chaîne Kick · touche 1", "kick.com/${settings.kickChannel}", { editing = Field.Kick })
                ActionItem(
                    "Diagnostic Kick",
                    ToolkitAccessibilityService.instance?.lastKickReport?.firstOrNull() ?: "Maintiens 1, puis reviens ici",
                    { editing = Field.KickReport },
                )
                ActionItem(
                    "Playlist YouTube · touche 3",
                    "youtube.com/playlist?list=${settings.youtubePlaylist}",
                    { editing = Field.Playlist },
                )

                SectionLabel("Durées d'appui")
                ActionItem("Touche 1 · Kick", holdLabel(settings.kickHoldMs), {
                    onShortcuts(settings.copy(kickHoldMs = nextStep(DigitHoldSteps, settings.kickHoldMs)))
                })
                ActionItem("Touche 2 · animation", holdLabel(settings.rainbowHoldMs), {
                    onShortcuts(settings.copy(rainbowHoldMs = nextStep(DigitHoldSteps, settings.rainbowHoldMs)))
                })
                ActionItem("Touche 3 · YouTube", holdLabel(settings.playlistHoldMs), {
                    onShortcuts(settings.copy(playlistHoldMs = nextStep(DigitHoldSteps, settings.playlistHoldMs)))
                })
                ActionItem("Home · réglages rapides", holdLabel(settings.homeQuickSettingsMs), {
                    onShortcuts(settings.copy(homeQuickSettingsMs = nextStep(QuickSettingsSteps, settings.homeQuickSettingsMs)))
                })
                ActionItem("Home · accueil Google TV", holdLabel(settings.homeStockHomeMs), {
                    onShortcuts(settings.copy(homeStockHomeMs = nextStep(StockHomeSteps, settings.homeStockHomeMs)))
                })

                // The border is drawn behind this dialog, so changes show live around the screen.
                SectionLabel("Animation de la touche 2")
                ActionItem("Afficher l'animation", if (rainbowOn) "Activée" else "Désactivée", onToggleRainbow)
                ActionItem("Style", stepLabel(border.style.label), {
                    onBorder(border.copy(style = next(BorderStyle.entries, border.style)))
                })
                ActionItem("Couleurs", stepLabel(border.palette.label), {
                    onBorder(border.copy(palette = next(BorderPalette.entries, border.palette)))
                })
                ActionItem("Épaisseur", stepLabel(border.thickness.label), {
                    onBorder(border.copy(thickness = next(BorderThickness.entries, border.thickness)))
                })
                ActionItem("Vitesse", stepLabel(border.speed.label), {
                    onBorder(border.copy(speed = next(BorderSpeed.entries, border.speed)))
                })
                ActionItem("Halo", stepLabel(border.glow.label), {
                    onBorder(border.copy(glow = next(BorderGlow.entries, border.glow)))
                })
                ActionItem("Rétablir l'animation par défaut", "Arc-en-ciel tournant, normal", { onBorder(BorderSettings()) })
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
            onSave = { onShortcuts(settings.copy(kickChannel = it)); editing = null },
        )
        Field.Playlist -> TextInputDialog(
            title = "Playlist YouTube",
            hint = "Colle le lien de la playlist (youtube.com/playlist?list=…)",
            initial = "https://youtube.com/playlist?list=${settings.youtubePlaylist}",
            parse = Shortcuts::parsePlaylistId,
            onDismiss = { editing = null },
            onSave = { onShortcuts(settings.copy(youtubePlaylist = it)); editing = null },
        )
        Field.AmbilightPin -> TextInputDialog(
            title = "Code de la TV",
            hint = "Saisis le code à 4 chiffres affiché sur la TV",
            initial = "",
            parse = { input -> input.trim().takeIf { it.matches(Regex("\\d{4,8}")) } },
            onDismiss = { editing = null },
            onSave = { onAmbilightPin(it); editing = null },
        )
        Field.Backup -> BackupDialog(code = remember { onExportBackup() }, onDismiss = { editing = null })
        Field.Restore -> TextInputDialog(
            title = "Restaurer des réglages",
            hint = "Colle le texte commençant par TK1: (clavier de l'appli Google TV sur l'iPhone)",
            initial = "",
            parse = { input -> input.trim().takeIf { it.startsWith("TK1:") } },
            onDismiss = { editing = null },
            onSave = { code ->
                val restored = onImportBackup(code)
                android.widget.Toast.makeText(
                    context,
                    if (restored) "Réglages restaurés" else "Ce texte n'est pas une sauvegarde valide",
                    android.widget.Toast.LENGTH_LONG,
                ).show()
                editing = null
            },
        )
        Field.KickReport -> ReportDialog(
            title = "Diagnostic Kick",
            lines = ToolkitAccessibilityService.instance?.lastKickReport.orEmpty()
                .ifEmpty { listOf("Rien pour l'instant : maintiens 1 sur l'accueil, attends que Kick s'ouvre, puis reviens ici.") },
            onDismiss = { editing = null },
        )
        Field.Font -> FontPickerDialog(
            current = clockFont,
            onDismiss = { editing = null },
            onPick = { onClockFont(it); editing = null },
        )
        null -> Unit
    }
}

/**
 * The settings backup as a QR code, to scan with a phone and keep the text it holds, and as text.
 * Restoring takes that text back, typed or pasted with the Google TV phone app.
 */
@Composable
private fun BackupDialog(code: String, onDismiss: () -> Unit) {
    val matrix = remember(code) {
        QRCodeWriter().encode(
            code,
            BarcodeFormat.QR_CODE,
            0,
            0,
            mapOf(EncodeHintType.ERROR_CORRECTION to ErrorCorrectionLevel.L, EncodeHintType.MARGIN to 2),
        )
    }
    val close = remember { FocusRequester() }
    LaunchedEffect(Unit) { close.requestFocusWhenReady() }
    Dialog(onDismissRequest = onDismiss, properties = DialogProperties(usePlatformDefaultWidth = false)) {
        Row(
            Modifier
                .width(820.dp)
                .clip(PanelShape)
                .background(ToolkitColors.SurfaceRaised)
                .border(1.dp, Color.White.copy(alpha = 0.08f), PanelShape)
                .padding(24.dp),
            horizontalArrangement = Arrangement.spacedBy(24.dp),
        ) {
            Canvas(Modifier.size(300.dp).background(Color.White)) {
                val cell = size.width / matrix.width
                for (y in 0 until matrix.height) for (x in 0 until matrix.width) {
                    if (matrix[x, y]) {
                        drawRect(Color.Black, topLeft = Offset(x * cell, y * cell), size = Size(cell + 0.5f, cell + 0.5f))
                    }
                }
            }
            Column(Modifier.weight(1f)) {
                Text("Sauvegarde des réglages", style = MaterialTheme.typography.headlineSmall, color = ToolkitColors.OnSurface)
                Text(
                    "Scanne le QR code avec l'appareil photo de l'iPhone, puis copie le texte dans tes notes. " +
                        "Pour restaurer : Options › Restaurer des réglages, et colle ce texte avec le clavier " +
                        "de l'appli Google TV. Tout est inclus sauf l'association Ambilight.",
                    style = MaterialTheme.typography.bodyMedium,
                    color = ToolkitColors.OnSurfaceMuted,
                    modifier = Modifier.padding(top = 8.dp),
                )
                Text(
                    code,
                    style = MaterialTheme.typography.bodyMedium.copy(fontSize = 9.sp),
                    color = ToolkitColors.OnSurfaceMuted,
                    maxLines = 6,
                    overflow = TextOverflow.Ellipsis,
                    modifier = Modifier.padding(top = 12.dp),
                )
                Button(onClick = onDismiss, modifier = Modifier.padding(top = 16.dp).focusRequester(close)) { Text("Fermer") }
            }
        }
    }
}

/** Read-only list (here, what the Kick shortcut saw), scrolled line by line with the arrows. */
@Composable
private fun ReportDialog(title: String, lines: List<String>, onDismiss: () -> Unit) {
    val first = remember { FocusRequester() }
    LaunchedEffect(Unit) { first.requestFocusWhenReady() }
    Dialog(onDismissRequest = onDismiss, properties = DialogProperties(usePlatformDefaultWidth = false)) {
        Column(
            Modifier
                .width(760.dp)
                .clip(PanelShape)
                .background(ToolkitColors.SurfaceRaised)
                .border(1.dp, Color.White.copy(alpha = 0.08f), PanelShape)
                .padding(24.dp),
        ) {
            Text(title, style = MaterialTheme.typography.headlineSmall, color = ToolkitColors.OnSurface)
            Text(
                "Prends une photo de cette liste pour Claude",
                style = MaterialTheme.typography.bodyMedium,
                color = ToolkitColors.OnSurfaceMuted,
            )
            LazyColumn(Modifier.padding(top = 12.dp).heightIn(max = 400.dp)) {
                itemsIndexed(lines) { index, line ->
                    var focused by remember { mutableStateOf(false) }
                    Text(
                        line,
                        style = MaterialTheme.typography.bodyMedium.copy(fontSize = 12.sp),
                        color = if (focused) ToolkitColors.OnSurface else ToolkitColors.OnSurfaceMuted,
                        modifier = Modifier
                            .fillMaxWidth()
                            .then(if (index == 0) Modifier.focusRequester(first) else Modifier)
                            .onFocusChanged { focused = it.isFocused }
                            .focusable(),
                    )
                }
            }
        }
    }
}

@Composable
private fun SectionLabel(text: String) {
    Text(
        text.uppercase(),
        style = MaterialTheme.typography.labelMedium,
        color = ToolkitColors.Accent,
        modifier = Modifier.padding(start = 16.dp, top = 14.dp, bottom = 4.dp),
    )
}

/** "Toolkit 1.0.16 · Installer 1.0.12", to spot an app that wasn't updated. */
private fun installedVersions(context: android.content.Context): String {
    fun version(pkg: String) = runCatching { context.packageManager.getPackageInfo(pkg, 0).versionName }.getOrNull()
    val installer = version("com.tk.toolkit.tv.installer")?.let { "Installer $it" } ?: "Installer absent"
    return "Toolkit ${version(context.packageName) ?: "?"} · $installer"
}

private val DigitHoldSteps = listOf(500L, 1_000L, 1_500L, 2_000L, 3_000L, 4_000L, 5_000L)
private val QuickSettingsSteps = listOf(300L, 400L, 500L, 600L, 800L, 1_000L, 1_500L, 2_000L)
private val StockHomeSteps = listOf(1_500L, 2_000L, 3_000L, 4_000L, 5_000L, 6_000L, 8_000L, 10_000L)

/** The value after [current] in [steps], wrapping around; a value not in the list restarts. */
private fun nextStep(steps: List<Long>, current: Long): Long = steps.firstOrNull { it > current } ?: steps.first()

private fun <T> next(values: List<T>, current: T): T = values[(values.indexOf(current) + 1) % values.size]

private fun stepLabel(value: String) = "$value · OK pour changer"

/** "90 s · OK…" reads better as "1,5 min · OK…": seconds up to a minute, then minutes. */
private fun minutesLabel(secondsLabel: String): String {
    val seconds = secondsLabel.substringBefore(" s").replace(',', '.').toFloatOrNull() ?: return secondsLabel
    if (seconds < 60f) return secondsLabel
    val minutes = seconds / 60f
    val text = if (minutes % 1f == 0f) "${minutes.toInt()} min" else "%.1f min".format(minutes).replace('.', ',')
    return stepLabel(text)
}

private val SleepSteps = listOf<Int?>(null, 30, 60, 90, 120)

/** Off → 30 → 60 → 90 → 120 min → off; runs in the accessibility service, so it keeps going in other apps. */
@Composable
private fun SleepTimerItem() {
    val context = LocalContext.current
    val service = ToolkitAccessibilityService.instance
    var minutes by remember { mutableStateOf(service?.sleepMinutes) }
    var endsAt by remember { mutableStateOf(service?.sleepAt) }
    val label = endsAt?.let {
        val time = java.text.SimpleDateFormat("HH:mm", java.util.Locale.FRANCE).format(java.util.Date(it))
        "Veille à $time ($minutes min)"
    } ?: "Désactivée"
    ActionItem("Mettre la TV en veille", stepLabel(label), {
        val running = ToolkitAccessibilityService.instance
        if (running == null) {
            android.widget.Toast.makeText(
                context,
                "Active « Toolkit » dans Paramètres › Accessibilité pour la minuterie",
                android.widget.Toast.LENGTH_LONG,
            ).show()
        } else {
            val nextMinutes = next(SleepSteps, minutes)
            running.setSleepTimer(nextMinutes)
            minutes = running.sleepMinutes
            endsAt = running.sleepAt
        }
    })
}

private fun holdLabel(ms: Long): String {
    val seconds = if (ms % 1_000L == 0L) "${ms / 1_000L}" else "%.1f".format(ms / 1_000f).replace('.', ',')
    return stepLabel("$seconds s")
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
