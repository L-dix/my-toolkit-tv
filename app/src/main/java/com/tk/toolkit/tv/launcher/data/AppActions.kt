package com.tk.toolkit.tv.launcher.data

import android.content.ActivityNotFoundException
import android.content.Context
import android.content.Intent
import android.provider.Settings
import android.widget.Toast
import androidx.core.net.toUri
import com.tk.toolkit.tv.core.ToolkitAccessibilityService

/** System-level actions the launcher can perform on an app. */
object AppActions {

    fun appInfoIntent(packageName: String): Intent =
        Intent(Settings.ACTION_APPLICATION_DETAILS_SETTINGS, "package:$packageName".toUri())
            .addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)

    fun openAppInfo(context: Context, packageName: String) =
        context.safeStart(appInfoIntent(packageName))

    fun uninstall(context: Context, packageName: String) =
        context.safeStart(
            Intent(Intent.ACTION_DELETE, "package:$packageName".toUri())
                .addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
        )

    /**
     * Opens [url] in the app [packageName] if it handles the link, else in any app that does,
     * else just opens that app.
     */
    fun openLink(context: Context, url: String, packageName: String?) {
        val pm = context.packageManager
        val view = Intent(Intent.ACTION_VIEW, url.toUri()).addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
        val link = listOfNotNull(packageName?.let { Intent(view).setPackage(it) }, view)
            .firstOrNull { it.resolveActivity(pm) != null }
        if (link != null) {
            context.safeStart(link)
            return
        }
        val app = packageName?.let { pm.getLeanbackLaunchIntentForPackage(it) ?: pm.getLaunchIntentForPackage(it) }
        if (app != null) {
            Toast.makeText(context, "Le lien ne s'ouvre pas directement : ouverture de l'appli", Toast.LENGTH_SHORT).show()
            context.safeStart(app.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK))
        } else {
            Toast.makeText(context, "Appli introuvable", Toast.LENGTH_SHORT).show()
        }
    }

    fun openSystemSettings(context: Context) =
        context.safeStart(Intent(Settings.ACTION_SETTINGS).addFlags(Intent.FLAG_ACTIVITY_NEW_TASK))

    /**
     * Google TV's own "Audio output" picker (TV speakers, HDMI, Bluetooth). It lives in SystemUI;
     * if a build doesn't have it, fall back to Display & Sound settings.
     */
    fun openAudioOutput(context: Context) {
        val picker = Intent("com.android.systemui.TV_MEDIA_OUTPUT_DIALOG")
            .setPackage("com.android.systemui")
            .addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
        if (picker.resolveActivity(context.packageManager) != null) {
            context.safeStart(picker)
        } else {
            context.safeStart(Intent(Settings.ACTION_SOUND_SETTINGS).addFlags(Intent.FLAG_ACTIVITY_NEW_TASK))
        }
    }

    /**
     * Third-party apps can't force-stop other apps directly. When the accessibility helper is
     * enabled it taps "Force stop" in system Settings for us; otherwise we open App info.
     */
    fun forceStop(context: Context, packageName: String) {
        val service = ToolkitAccessibilityService.instance
        if (service != null) {
            service.forceStop(packageName)
        } else {
            Toast.makeText(
                context,
                "Active « Toolkit » dans Paramètres › Accessibilité pour forcer l'arrêt en un clic",
                Toast.LENGTH_LONG,
            ).show()
            openAppInfo(context, packageName)
        }
    }

    fun Context.safeStart(intent: Intent) {
        try {
            startActivity(intent)
        } catch (_: ActivityNotFoundException) {
            Toast.makeText(this, "Impossible d'ouvrir cet écran", Toast.LENGTH_SHORT).show()
        } catch (_: SecurityException) {
            Toast.makeText(this, "Action non autorisée", Toast.LENGTH_SHORT).show()
        }
    }
}
