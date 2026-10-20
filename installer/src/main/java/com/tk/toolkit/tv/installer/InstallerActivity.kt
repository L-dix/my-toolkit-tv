package com.tk.toolkit.tv.installer

import android.app.Activity
import android.app.PendingIntent
import android.content.ActivityNotFoundException
import android.content.Intent
import android.content.pm.PackageInstaller
import android.content.pm.PackageManager
import android.graphics.Color
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.provider.Settings
import android.view.Gravity
import android.view.View
import android.widget.Button
import android.widget.LinearLayout
import android.widget.TextView
import java.io.File
import java.net.HttpURLConnection
import java.net.URL
import kotlin.concurrent.thread

/**
 * Installs Toolkit from the repository's latest GitHub Release through a PackageInstaller
 * session, the way app stores do. Android 13+ blocks the accessibility service of apps
 * installed by opening an APK file (Downloader, file managers), but not of apps installed
 * through a session, and Toolkit needs its accessibility service to take over the Home button.
 */
class InstallerActivity : Activity() {

    private lateinit var message: TextView
    private lateinit var status: TextView
    private lateinit var primary: Button
    private lateinit var secondary: Button
    private var busy = false

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        val density = resources.displayMetrics.density
        val pad = (48 * density).toInt()
        val gap = (20 * density).toInt()
        val title = TextView(this).apply {
            text = getString(R.string.app_name)
            textSize = 30f
            setTextColor(Color.WHITE)
        }
        message = TextView(this).apply {
            textSize = 18f
            setTextColor(Color.WHITE)
            setPadding(0, gap, 0, gap)
        }
        status = TextView(this).apply {
            textSize = 16f
            setTextColor(Color.rgb(0x8F, 0xA8, 0xFF))
            setPadding(0, 0, 0, gap)
        }
        primary = Button(this).apply { textSize = 18f }
        secondary = Button(this).apply { textSize = 18f }
        val buttons = LinearLayout(this).apply {
            orientation = LinearLayout.HORIZONTAL
            addView(primary)
            addView(secondary)
        }
        setContentView(
            LinearLayout(this).apply {
                orientation = LinearLayout.VERTICAL
                gravity = Gravity.CENTER_VERTICAL
                setPadding(pad, pad, pad, pad)
                setBackgroundColor(Color.BLACK)
                addView(title)
                addView(message)
                addView(status)
                addView(buttons)
            }
        )
        handleInstallStatus(intent)
    }

    override fun onNewIntent(intent: Intent) {
        super.onNewIntent(intent)
        handleInstallStatus(intent)
    }

    override fun onResume() {
        super.onResume()
        // Back from uninstalling Toolkit or from the "unknown sources" setting.
        if (!busy) refresh()
    }

    private fun refresh() {
        val installer = toolkitInstaller()
        when {
            installer != null && installer != packageName -> show(
                "Toolkit a été installé par une autre appli${if (installer.isEmpty()) "" else " ($installer)"} : " +
                    "Android bloque alors son service d'accessibilité.\n\n" +
                    "Désinstalle-le, puis reviens ici pour le réinstaller.",
                "Désinstaller Toolkit", ::uninstallToolkit,
            )
            !packageManager.canRequestPackageInstalls() -> show(
                "Autorise d'abord Toolkit Installer à installer des applis : " +
                    "Paramètres › Applications › Sécurité et restrictions › Sources inconnues › Toolkit Installer.",
                "Ouvrir le réglage", ::openUnknownSources,
            )
            installer == packageName -> show(
                "Toolkit est installé. Pour en faire l'écran d'accueil : " +
                    "Paramètres › Système › Accessibilité › Toolkit › Activer.\n\n" +
                    "Pour les mises à jour, reviens ici : le bouton installe la dernière version publiée.",
                "Mettre à jour Toolkit", ::downloadAndInstall,
                "Ouvrir l'accessibilité", ::openAccessibility,
            )
            else -> show(
                "Installe Toolkit depuis la dernière version publiée sur GitHub.",
                "Installer Toolkit", ::downloadAndInstall,
            )
        }
    }

    private fun show(
        text: String,
        primaryLabel: String,
        primaryAction: () -> Unit,
        secondaryLabel: String? = null,
        secondaryAction: (() -> Unit)? = null,
    ) {
        message.text = text
        primary.text = primaryLabel
        primary.isEnabled = true
        primary.setOnClickListener { primaryAction() }
        secondary.visibility = if (secondaryLabel != null) View.VISIBLE else View.GONE
        secondary.text = secondaryLabel
        secondary.setOnClickListener { secondaryAction?.invoke() }
        primary.requestFocus()
    }

    /** Package that installed Toolkit, "" if unknown, or null if Toolkit isn't installed. */
    @Suppress("DEPRECATION")
    private fun toolkitInstaller(): String? =
        try {
            if (Build.VERSION.SDK_INT >= 30) {
                val info = packageManager.getInstallSourceInfo(TOOLKIT)
                info.initiatingPackageName ?: info.installingPackageName ?: ""
            } else {
                packageManager.getInstallerPackageName(TOOLKIT) ?: ""
            }
        } catch (_: PackageManager.NameNotFoundException) {
            null
        }

    private fun downloadAndInstall() {
        busy = true
        primary.isEnabled = false
        status.text = "Téléchargement…"
        thread {
            val result = runCatching {
                val apk = File(cacheDir, "toolkit-tv.apk")
                download(BuildConfig.TOOLKIT_APK_URL, apk)
                check(isToolkitSignedLikeMe(apk)) { "l'APK téléchargé n'est pas signé avec la clé de Toolkit" }
                install(apk)
            }
            runOnUiThread {
                result
                    .onSuccess { status.text = "Installation…" }
                    .onFailure {
                        busy = false
                        status.text = "Échec : ${it.message ?: it}"
                        refresh()
                    }
            }
        }
    }

    private fun download(url: String, target: File) {
        val connection = URL(url).openConnection() as HttpURLConnection
        connection.connectTimeout = 15_000
        connection.readTimeout = 30_000
        try {
            check(connection.responseCode == HttpURLConnection.HTTP_OK) { "téléchargement : HTTP ${connection.responseCode}" }
            connection.inputStream.use { input -> target.outputStream().use { input.copyTo(it) } }
        } finally {
            connection.disconnect()
        }
    }

    /** The APK is Toolkit, signed with the same key as this installer (both come from the same CI). */
    @Suppress("DEPRECATION")
    private fun isToolkitSignedLikeMe(apk: File): Boolean {
        val flags = PackageManager.GET_SIGNING_CERTIFICATES
        val archive = packageManager.getPackageArchiveInfo(apk.path, flags) ?: return false
        if (archive.packageName != TOOLKIT) return false
        val theirs = archive.signingInfo?.apkContentsSigners ?: return false
        val mine = packageManager.getPackageInfo(packageName, flags).signingInfo?.apkContentsSigners ?: return false
        return theirs.toSet() == mine.toSet()
    }

    private fun install(apk: File) {
        val installer = packageManager.packageInstaller
        val params = PackageInstaller.SessionParams(PackageInstaller.SessionParams.MODE_FULL_INSTALL).apply {
            setAppPackageName(TOOLKIT)
            setSize(apk.length())
        }
        val sessionId = installer.createSession(params)
        installer.openSession(sessionId).use { session ->
            session.openWrite("toolkit-tv.apk", 0, apk.length()).use { out ->
                apk.inputStream().use { it.copyTo(out) }
                session.fsync(out)
            }
            // The system fills in the result, so the PendingIntent must be mutable.
            val result = PendingIntent.getActivity(
                this,
                sessionId,
                Intent(this, InstallerActivity::class.java).setAction(ACTION_INSTALL_STATUS),
                PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_MUTABLE,
            )
            session.commit(result.intentSender)
        }
    }

    @Suppress("DEPRECATION")
    private fun handleInstallStatus(intent: Intent?) {
        if (intent?.action != ACTION_INSTALL_STATUS) return
        when (val code = intent.getIntExtra(PackageInstaller.EXTRA_STATUS, PackageInstaller.STATUS_FAILURE)) {
            PackageInstaller.STATUS_PENDING_USER_ACTION -> {
                // Android asks the user to confirm; the final status comes back here afterwards.
                val confirm = if (Build.VERSION.SDK_INT >= 33) {
                    intent.getParcelableExtra(Intent.EXTRA_INTENT, Intent::class.java)
                } else {
                    intent.getParcelableExtra<Intent>(Intent.EXTRA_INTENT)
                }
                if (confirm != null) start(confirm)
                return
            }
            PackageInstaller.STATUS_SUCCESS -> status.text = "Toolkit est installé."
            else -> status.text = "Échec de l'installation : " +
                (intent.getStringExtra(PackageInstaller.EXTRA_STATUS_MESSAGE) ?: "code $code")
        }
        busy = false
        refresh()
    }

    private fun uninstallToolkit() =
        start(Intent(Intent.ACTION_DELETE, Uri.parse("package:$TOOLKIT")))

    private fun openUnknownSources() =
        start(Intent(Settings.ACTION_MANAGE_UNKNOWN_APP_SOURCES, Uri.parse("package:$packageName")))

    private fun openAccessibility() = start(Intent(Settings.ACTION_ACCESSIBILITY_SETTINGS))

    private fun start(intent: Intent) {
        try {
            startActivity(intent)
        } catch (_: ActivityNotFoundException) {
            status.text = "Impossible d'ouvrir cet écran : passe par les Paramètres de la TV."
        } catch (_: SecurityException) {
            status.text = "Android refuse d'ouvrir cet écran : passe par les Paramètres de la TV."
        }
    }

    private companion object {
        const val TOOLKIT = "com.tk.toolkit.tv"
        const val ACTION_INSTALL_STATUS = "com.tk.toolkit.tv.installer.INSTALL_STATUS"
    }
}
