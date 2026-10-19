# Toolkit for Google TV

A personal home screen (launcher) for Google TV and Android TV, built with Jetpack Compose for TV. It replaces the stock Google TV home with a clean, ad-free grid of your apps. Part of my Toolkit apps, alongside [Toolkit for Android](https://github.com/teja2495/my-toolkit-android) and [Toolkit for macOS](https://github.com/teja2495/my-toolkit-mac).

## Features

- **Clock header**: the time and date on one compact line, with quick buttons for audio output and system settings.
- **Audio output picker**: opens Google TV's own audio output menu to switch between TV speakers, HDMI, and Bluetooth devices.
- **Continue Watching**: pinned shows that open straight into their streaming app. The list is hard-coded in `ContinueWatching.kt` for now.
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

Release builds are signed with the local debug key (it's a personal sideloaded app), so they install over debug builds. Use the release build for daily use: Compose is noticeably slower in debug builds. The project targets Android 9 (API 28) and later.
