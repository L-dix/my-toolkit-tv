package com.tk.toolkit.tv.launcher.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Dialog
import androidx.tv.material3.ListItem
import androidx.tv.material3.ListItemDefaults
import androidx.tv.material3.MaterialTheme
import androidx.tv.material3.Text
import com.tk.toolkit.tv.launcher.data.LauncherApp
import com.tk.toolkit.tv.ui.theme.ToolkitColors

private val PanelShape = RoundedCornerShape(24.dp)

@Composable
fun AppActionsDialog(
    app: LauncherApp,
    isHidden: Boolean,
    isPinned: Boolean,
    onDismiss: () -> Unit,
    onMove: () -> Unit,
    onToggleHidden: () -> Unit,
    onTogglePinned: () -> Unit,
    onAppInfo: () -> Unit,
    onForceStop: () -> Unit,
    onUninstall: () -> Unit,
) {
    val firstAction = remember { FocusRequester() }
    LaunchedEffect(Unit) { runCatching { firstAction.requestFocus() } }

    Dialog(onDismissRequest = onDismiss) {
        Column(
            Modifier
                .width(400.dp)
                .clip(PanelShape)
                .background(ToolkitColors.SurfaceRaised)
                .border(1.dp, Color.White.copy(alpha = 0.08f), PanelShape)
                .ignoreHeldSelectKey(),
        ) {
            AppSummary(app)
            // Scrolls if the actions ever outgrow the screen's 540dp height.
            Column(
                Modifier.verticalScroll(rememberScrollState()).padding(12.dp),
                verticalArrangement = Arrangement.spacedBy(4.dp),
            ) {
                if (isHidden) {
                    ActionItem(
                        "Afficher", "Remettre sur l'écran d'accueil",
                        onToggleHidden, Modifier.focusRequester(firstAction),
                    )
                } else {
                    ActionItem("Déplacer", "Changer sa place sur l'écran d'accueil", onMove, Modifier.focusRequester(firstAction))
                    ActionItem("Masquer", "Ranger dans Plus d'applis", onToggleHidden)
                }
                ActionItem(
                    if (isPinned) "Désépingler" else "Épingler dans Continuer à regarder",
                    if (isPinned) "Retirer de Continuer à regarder" else "Ajouter un raccourci au-dessus des applis",
                    onTogglePinned,
                )
                ActionItem("Infos sur l'appli", "Stockage, autorisations…", onAppInfo)
                ActionItem("Forcer l'arrêt", "Fermer complètement l'appli", onForceStop)
                if (app.canUninstall) {
                    ActionItem(
                        if (app.isUpdatedSystemApp) "Désinstaller les mises à jour" else "Désinstaller",
                        if (app.isUpdatedSystemApp) "Revenir à la version d'origine" else "Supprimer de cette TV",
                        onUninstall,
                        danger = true,
                    )
                }
            }
        }
    }
}

/** Header band tinted with the app's color, set apart from the actions below. */
@Composable
private fun AppSummary(app: LauncherApp) {
    Column(
        Modifier
            .fillMaxWidth()
            .background(Brush.verticalGradient(listOf(app.accent.copy(alpha = 0.28f), app.accent.copy(alpha = 0.06f))))
            .drawBehind {
                drawLine(
                    Color.White.copy(alpha = 0.1f),
                    Offset(0f, size.height),
                    Offset(size.width, size.height),
                    strokeWidth = 1.dp.toPx(),
                )
            }
            .padding(horizontal = 28.dp, vertical = 22.dp),
    ) {
        Text(
            app.label,
            style = MaterialTheme.typography.headlineSmall,
            color = ToolkitColors.OnSurface,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
        )
        Text(
            app.packageName,
            style = MaterialTheme.typography.bodyMedium,
            color = ToolkitColors.OnSurfaceMuted,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
        )
    }
}

@Composable
internal fun ActionItem(
    title: String,
    subtitle: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    danger: Boolean = false,
) {
    val tint = if (danger) ToolkitColors.Danger else ToolkitColors.OnSurface
    ListItem(
        selected = false,
        onClick = onClick,
        modifier = modifier,
        headlineContent = { Text(title, style = MaterialTheme.typography.titleMedium) },
        supportingContent = { Text(subtitle, style = MaterialTheme.typography.bodyMedium) },
        shape = ListItemDefaults.shape(RoundedCornerShape(14.dp)),
        scale = ListItemDefaults.scale(focusedScale = 1.02f),
        colors = ListItemDefaults.colors(
            containerColor = Color.Transparent,
            contentColor = tint.copy(alpha = 0.7f),
            focusedContainerColor = tint.copy(alpha = 0.12f),
            focusedContentColor = tint,
        ),
    )
}
