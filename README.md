# Toolkit for Google TV

A personal home screen (launcher) for Google TV and Android TV, built with Jetpack Compose for TV. It replaces the stock Google TV home with a clean, ad-free grid of your apps. Part of my Toolkit apps, alongside [Toolkit for Android](https://github.com/teja2495/my-toolkit-android) and [Toolkit for macOS](https://github.com/teja2495/my-toolkit-mac).

## Features

- **Clock header**: the time and date on one compact line, with quick buttons to sort the apps (streaming first, or A to Z), audio output and system settings.
- **Audio output picker**: opens Google TV's own audio output menu to switch between TV speakers, HDMI, and Bluetooth devices.
- **Continue Watching**: shortcuts to apps you pin (long-press OK on an app › **Pin to Continue Watching**); pinned apps can be moved or unpinned from their own menu. By default it holds YouTube, Kick and a video player (VLC, MX Player, Kodi…) when installed, and the app grid starts with the streaming apps (HBO Max, Apple TV, Netflix, Molotov, CANAL+…). Both lists are in `HomeDefaults.kt`.
- **Easter egg**: hold the **2** key for 4 seconds on the home screen for a turning rainbow border; hold it again to remove it.
- **App grid**: apps shown as their TV banners (or a tinted icon tile), with a soft glow on focus.
- **App menu** (long-press OK on an app): move it to reorder the grid, hide it in **More Apps**, open App info, force stop, or uninstall.
- **Home button takeover**: an optional Accessibility service sends the Home button and the stock home screen to Toolkit, so you don't have to disable the stock launcher. Hold Home for the quick settings, or for 4 seconds to get the Google TV home back until you press Home again. It also powers one-click Force stop.

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

Chaque push sur `main` compile les APK dans GitHub Actions et les publie dans une Release. Les dernières versions sont toujours à ces adresses :

```
https://github.com/L-dix/my-toolkit-tv/releases/latest/download/toolkit-installer.apk
https://github.com/L-dix/my-toolkit-tv/releases/latest/download/toolkit-tv.apk
```

**Pourquoi un installateur ?** Depuis Android 13 (donc sur Android TV 14), Android interdit d'activer le service d'accessibilité d'une appli installée en ouvrant un fichier APK (Downloader, gestionnaire de fichiers), et les TV n'offrent pas le réglage « Autoriser les paramètres restreints » des téléphones. Une appli installée par session, comme le font les stores, n'est pas concernée : **Toolkit Installer** installe Toolkit de cette façon. Il télécharge la dernière Release de ce dépôt et vérifie que l'APK est signé avec la même clé que lui avant de l'installer.

Sur la TV :

1. **Installer Downloader** : Google Play › rechercher « Downloader » (éditeur AFTVnews) › Installer.
2. **Autoriser les sources inconnues pour Downloader** : Paramètres › Applications › Sécurité et restrictions › Sources inconnues › activer **Downloader**. Si l'option n'apparaît pas, activer d'abord le mode développeur (Paramètres › Système › À propos › appuyer 7 fois sur « Build du système d'exploitation Android TV »).
3. **Installer Toolkit Installer** : dans Downloader, saisir l'URL de `toolkit-installer.apk` ci-dessus › Go › **Installer**.
4. **Ouvrir Toolkit Installer** (liste des applis) et suivre l'écran :
   - si Toolkit avait déjà été installé avec Downloader, **Désinstaller Toolkit** d'abord ;
   - **Ouvrir le réglage** › autoriser **Toolkit Installer** dans Sources inconnues, puis Retour ;
   - **Installer Toolkit** › confirmer l'installation.
5. **Activer Toolkit comme écran d'accueil** : bouton **Ouvrir l'accessibilité** (ou Paramètres › Système › Accessibilité) › **Toolkit** › Activer. Attention à ne pas activer TalkBack, qui lit l'écran à voix haute. Le bouton Home ouvre alors Toolkit : un appui long ouvre les réglages rapides, et **un appui de 4 secondes ouvre l'accueil Google TV d'origine** (un appui simple sur Home ramène à Toolkit). « Force stop » fonctionne en un clic.

**Mises à jour** : ouvrir Toolkit Installer › **Mettre à jour Toolkit**. Ne pas réinstaller Toolkit avec Downloader : Android bloquerait de nouveau son accessibilité. Toolkit Installer lui-même se met à jour avec Downloader (URL de `toolkit-installer.apk`).

### Revenir en arrière

- **Passer un moment sur l'accueil Google TV** : maintenir Home 4 secondes ; un appui simple sur Home ramène à Toolkit.
- **Retrouver l'accueil Google TV pour de bon** : Paramètres › Système › Accessibilité › Toolkit › Désactiver. Le bouton Home ouvre de nouveau l'accueil d'origine ; Toolkit reste installé et s'ouvre depuis la liste des applis.
- **Désinstaller Toolkit** : Paramètres › Applications › Toolkit › Désinstaller (et de même pour Toolkit Installer et Downloader). Le service d'accessibilité disparaît avec l'appli.
- **Revenir à une version précédente** : Android refuse d'installer une version plus ancienne par-dessus une plus récente. Désinstaller d'abord Toolkit (l'ordre des applis et les épingles sont perdus), puis réinstaller la version voulue. Toolkit Installer installe toujours la dernière Release : pour une version précise, la télécharger depuis la page Releases du dépôt (par ex. `https://github.com/L-dix/my-toolkit-tv/releases/download/v1.0.5/toolkit-tv.apk`), en sachant qu'installée ainsi elle aura l'accessibilité bloquée.

### Signature des Releases (à faire une fois)

Le workflow `.github/workflows/release.yml` signe les APK avec une clé de release stable, stockée dans les secrets du dépôt : sans elle, chaque build aurait une clé différente et les mises à jour refuseraient de s'installer. Sur github.com (le navigateur du téléphone suffit) : dépôt › **Settings** › **Secrets and variables** › **Actions** › **New repository secret**, trois fois :

| Nom | Valeur |
| --- | --- |
| `ANDROID_KEYSTORE_BASE64` | le keystore (`.jks`) encodé en base64, sur une seule ligne |
| `ANDROID_KEYSTORE_PASSWORD` | le mot de passe du keystore |
| `ANDROID_KEY_ALIAS` | l'alias de la clé (`toolkit`) |

Le keystore est au format PKCS12, qui n'a qu'un seul mot de passe : il sert aussi pour la clé.

Puis onglet **Actions** › **Release APK** › **Run workflow** pour publier la première version. Sans ces secrets, le workflow compile quand même (pour vérifier le code) mais échoue sans rien publier.

Pour créer une autre clé : `keytool -genkeypair -keystore release.jks -storetype PKCS12 -alias toolkit -keyalg RSA -keysize 2048 -validity 10000`, puis `base64 -w0 release.jks`. Garder une copie du keystore et du mot de passe (gestionnaire de mots de passe) : GitHub ne permet pas de relire un secret, et une nouvelle clé oblige à désinstaller Toolkit avant d'installer la version suivante.
