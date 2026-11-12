<img src="art/icon.png" width="96" align="right">

# Folio

Bare-bones PDF viewer for Android. Scroll, pinch or double-tap to zoom, follows device rotation. Nothing else.

- Uses the platform `PdfRenderer`, no third-party libraries, no permissions.
- Opens PDFs from other apps ("Open with"), or from the system file picker.
- Android 8.0+.

## Install

Download `Folio.apk` from [Releases](../../releases/latest).

## Build

CI builds on every push (APK under the run's artifacts) and publishes a release for `v*` tags.
Locally: `gradle assembleRelease` with Android SDK 35 (unsigned config falls back to the debug key).
