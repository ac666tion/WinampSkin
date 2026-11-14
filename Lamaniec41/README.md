# Łamaniec 4.1 (experimental)

A minimal Android launcher for https://www.lamaniec.pl/ using Mozilla GeckoView.

## Target device
- Android API 16 / Android 4.1 and newer
- ARMv7 (`armeabi-v7a`), intended for Samsung Galaxy Tab 2 GT-P5100
- GeckoView 118.0.20231009140911 (pre-API-21 build)

## Version 0.2.0
- Forces a device-width mobile viewport at document start.
- Re-applies the viewport if the site overwrites it.
- Adds conservative wrapping for long instructions and responsive media.
- Explicitly configures GeckoView for mobile user-agent, mobile viewport and standalone display mode.

## Build
Use JDK 17, Android SDK platform 34 + build-tools 34.0.0, and Gradle 8.7:

```bash
gradle assembleDebug
```

APK output: `app/build/outputs/apk/debug/app-debug.apk`.

## Important
This is an experimental compatibility wrapper, not an official Łamaniec app. GeckoView 118 is old and no longer receives security updates. It is intended only to test the game site on the legacy tablet, not for banking or sensitive logins. The injected CSS is intentionally conservative; the appearance must still be verified on the physical tablet.
