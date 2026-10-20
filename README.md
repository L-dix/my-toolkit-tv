# Toolkit for Google TV

A personal home screen (launcher) for Google TV and Android TV, built with Jetpack Compose for TV. It replaces the stock Google TV home with a clean, ad-free grid of your apps. Part of my Toolkit apps, alongside [Toolkit for Android](https://github.com/teja2495/my-toolkit-android) and [Toolkit for macOS](https://github.com/teja2495/my-toolkit-mac).

## Features

- **Clock header**: the time and date on one compact line, with quick buttons for audio output, Ambilight and system settings.
- **Audio output picker**: opens Google TV's own audio output menu to switch between TV speakers, HDMI, and Bluetooth devices.
- **Ambilight button** (Philips TVs): opens the Ambilight settings. The candidate packages are in `AMBILIGHT_PACKAGES` (or set an exact screen in `AMBILIGHT_COMPONENT`) in `Ambilight.kt`; if none is found it opens the system settings.
- **Continue Watching**: shortcuts to apps you pin (long-press OK on an app › **Pin to Continue Watching**). Empty and hidden until you pin something; pinned apps can be moved or unpinned from their own menu.
- **App grid**: apps shown as their TV banners (or a tinted icon tile), with a soft glow on focus.
- **App menu** (long-press OK on an app): move it to reorder the grid, hide it in **More Apps**, open App info, force stop, or uninstall.
- **Home button takeover**: an optional Accessibility service sends the Home button and the stock home screen to Toolkit, so you don't have to disable the stock launcher. It also powers one-click Force stop.

## Getting started

1. Build and install the app on your TV (see below).
2. Make it your home screen with one of these:
   - Turn on **Toolkit** in **Settings › System › Accessibility**. Home then opens Toolkit, and Force stop works in one click.
   - Or set it as the default home over adb:
     ```bash
     adb shell cmd package set-home-activity com.tk.toolkit.tv/.MainActivity
     ```

## Building

Open the project in Android Studio, or build from the command line:

```bash
./gradlew assembleRelease
```

The APK is written to `app/build/outputs/apk/release/app-release.apk` and installs with:

```bash
adb install -r app/build/outputs/apk/release/app-release.apk
```

Local release builds are signed with the debug key (it's a personal sideloaded app), so they install over debug builds. Builds from GitHub Actions are signed with the release key from the repository secrets (see below). Use the release build for daily use: Compose is noticeably slower in debug builds. The project targets Android 9 (API 28) and later.

## Installation sans PC

Chaque push sur `main` compile l'APK dans GitHub Actions et le publie dans une Release. La dernière version est toujours à cette adresse :

```
https://github.com/L-dix/my-toolkit-tv/releases/latest/download/toolkit-tv.apk
```

Sur la TV :

1. **Installer Downloader** : Google Play › rechercher « Downloader » (éditeur AFTVnews) › Installer.
2. **Autoriser les sources inconnues pour Downloader** : Paramètres › Applications › Sécurité et restrictions › Sources inconnues › activer **Downloader**. Si l'option n'apparaît pas, activer d'abord le mode développeur (Paramètres › Système › À propos › appuyer 7 fois sur « Build du système d'exploitation Android TV »). Sinon, Downloader propose lui-même d'ouvrir ce réglage au moment de l'installation.
3. **Télécharger l'APK** : ouvrir Downloader, saisir l'URL ci-dessus dans le champ d'adresse › Go › **Installer**. Astuce : raccourcir l'URL depuis le téléphone (par ex. avec is.gd) pour avoir moins de caractères à taper à la télécommande.
4. **Activer Toolkit comme écran d'accueil** : Paramètres › Système › Accessibilité › **Toolkit** › Activer. Le bouton Home ouvre alors Toolkit (un appui long ouvre les réglages rapides), et « Force stop » fonctionne en un clic.
   - Si l'interrupteur est grisé avec « Paramètre restreint » (Android 13 et plus, applis installées hors Play Store) : Paramètres › Applications › Toolkit › menu ⋮ ou bas de page › « Autoriser les paramètres restreints », puis recommencer.

**Mises à jour** : rouvrir Downloader et télécharger la même URL. Le nouvel APK s'installe par-dessus l'ancien (même clé de signature, numéro de version plus élevé) en conservant l'ordre des applis et les épingles.

### Revenir en arrière

- **Retrouver l'accueil Google TV** : Paramètres › Système › Accessibilité › Toolkit › Désactiver. Le bouton Home ouvre de nouveau l'accueil d'origine ; Toolkit reste installé et s'ouvre depuis la liste des applis.
- **Désinstaller Toolkit** : Paramètres › Applications › Toolkit › Désinstaller. Le service d'accessibilité disparaît avec l'appli.
- **Revenir à une version précédente** : Android refuse d'installer une version plus ancienne par-dessus une plus récente. Désinstaller d'abord Toolkit (l'ordre des applis et les épingles sont perdus), puis télécharger l'APK voulu depuis la page Releases du dépôt, par ex. `https://github.com/L-dix/my-toolkit-tv/releases/download/v1.0.5/toolkit-tv.apk`.

### Signature des Releases (à faire une fois)

Le workflow `.github/workflows/release.yml` signe l'APK avec une clé de release stable, stockée dans les secrets du dépôt : sans elle, chaque build aurait une clé différente et les mises à jour refuseraient de s'installer. Sur github.com (le navigateur du téléphone suffit) : dépôt › **Settings** › **Secrets and variables** › **Actions** › **New repository secret**, trois fois :

| Nom | Valeur |
| --- | --- |
| `ANDROID_KEYSTORE_BASE64` | le keystore (`.jks`) encodé en base64, sur une seule ligne |
| `ANDROID_KEYSTORE_PASSWORD` | le mot de passe du keystore |
| `ANDROID_KEY_ALIAS` | l'alias de la clé (`toolkit`) |

Le keystore est au format PKCS12, qui n'a qu'un seul mot de passe : il sert aussi pour la clé.

Puis onglet **Actions** › **Release APK** › **Run workflow** pour publier la première version. Sans ces secrets, le workflow compile quand même (pour vérifier le code) mais échoue sans rien publier.

Pour créer une autre clé : `keytool -genkeypair -keystore release.jks -storetype PKCS12 -alias toolkit -keyalg RSA -keysize 2048 -validity 10000`, puis `base64 -w0 release.jks`. Garder une copie du keystore et du mot de passe (gestionnaire de mots de passe) : GitHub ne permet pas de relire un secret, et une nouvelle clé oblige à désinstaller Toolkit avant d'installer la version suivante.
