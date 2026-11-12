# Xenamp Classic — privacy-focused Android fork

This is a personal fork of **Xenamp**, the open-source Winamp 2.x-inspired Android player originally created by Shaji Khan in [djshaji/WinampSkin](https://github.com/djshaji/WinampSkin).

The original project is licensed under GPL-2.0. This fork keeps the original Winamp-style interface and skin engine, while removing unnecessary services and improving compatibility with older Android devices.

## Goals of this fork

- Preserve the classic Winamp 2.x appearance, playlist, and .wsz skin support.
- Minimum Android version: **Android 4.1 (API 16)**.
- Remove Firebase Analytics and Crashlytics.
- Do not include advertising SDKs or analytics SDKs.
- Keep permissions limited to media/network use: INTERNET, ACCESS_NETWORK_STATE (used by the streaming engine), and READ_EXTERNAL_STORAGE only up to API 18 for legacy Android 4.1–4.3 file managers. There is no storage-write permission. On Android 4.4+, selected audio is accessed through the Android file picker and its granted URI permission.
- Use Android's legacy Equalizer audio effect rather than API-28 DynamicsProcessing.
- Use Java's built-in ZIP support for Winamp skin archives.
- Do not load a remote featured-music feed or include demo tracks.

Internet access is used only for functions explicitly requested in the app, such as a manually entered stream or the online skin browser/download. Local playback does not require an account or an internet connection.

## Android 4.1 notes

Android 4.1 predates Android's Storage Access Framework. On these devices, **Open File** uses the system's ACTION_GET_CONTENT picker. Selected content URIs are copied into the app's private storage so saved playlist entries remain usable after the temporary picker grant expires.

The system folder picker is only available on Android 5.0 and newer. On Android 4.1, use **Open File** to select tracks.

## Build

The project retains the original compatible build toolchain:

- JDK 11 for Gradle
- Gradle 7.0.2
- Android Gradle Plugin 7.0.2
- Android SDK platform 30
- Minimum SDK 16

Build locally by running:

~~~bash
./gradlew --no-daemon clean assembleDebug
~~~

GitHub Actions builds a debug APK and uploads it as the artifact **xenamp-android-4.1-debug**.

## Package

This fork uses application ID **com.ac666tion.xenamp**, so it can be installed alongside the original Xenamp package.

## Attribution and disclaimer

Xenamp is an independent Winamp 2.x-inspired player; it is not the official Winamp application. Winamp and related marks belong to their respective owners. Original project code and artwork remain attributed to their authors. This fork is provided without warranty.