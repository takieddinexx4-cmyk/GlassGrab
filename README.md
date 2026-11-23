# GlassGrab

GlassGrab is a privacy-first, multi-module Android video downloader built with Kotlin, Jetpack Compose, Material 3 Expressive, Haze v2 frosted glass UI, Navigation 3, Room, WorkManager, Media3 ExoPlayer, and `youtubedl-android` (`yt-dlp` + FFmpeg + Aria2c).

- **Original quality (144p–8K)** with FFmpeg merging for separate video + audio streams (H.264, VP9, AV1, HEVC; MP4/MKV/WebM) and audio-only presets (MP3/M4A/Opus).
- **TikTok without watermark**: prefers watermark-free API streams and falls back to the secondary web extractor if the primary path fails.
- **Zero ads, zero trackers, zero accounts**: all history stays on device in Room, and saved media goes straight to `Movies/GlassGrab` (or `Music/GlassGrab`) via scoped `MediaStore`.
- **RTL & localization**: full English and Arabic (`ar`) support with per-app locale switching.
- **DRM policy**: GlassGrab does not bypass DRM (Netflix, Disney+, Hulu, Prime Video, etc.) and clearly reports those links as unsupported.

## Upload to GitHub (No Manual Unzipping)

You do not need to unzip `GlassGrab.zip` on your phone or PC. Use the companion `extract-and-build.yml` workflow:

1. Create a new empty repository on GitHub (public or private).
2. In the GitHub web UI, click **Add file → Create new file**, name it `.github/workflows/extract-and-build.yml`, paste the contents of `extract-and-build.yml`, and commit.
3. Click **Add file → Upload files**, upload `GlassGrab.zip` directly to the repository root, and commit.
4. Open the **Actions** tab and wait for the **Extract & Build GlassGrab** workflow run to finish (it automatically unzips the project, deletes `GlassGrab.zip`, commits the files, and builds the debug APK in the same run).
5. Click the finished workflow run, scroll to **Artifacts**, and download **GlassGrab-debug-apk** to install `app-debug.apk` on your Android device.

## Local Build & Test

Requirements: JDK 21, Android SDK with API 37.1 (`compileSdk 37.1`), Gradle 9.6.0.

```bash
# Bootstrap wrapper if gradle-wrapper.jar is not yet generated
gradle wrapper --gradle-version 9.6.0

# Build debug APK
./gradlew assembleDebug

# Run unit tests (JUnit 5, MockK, Turbine)
./gradlew testDebugUnitTest

# Static quality checks
./gradlew detekt spotlessCheck
```

## Architecture

| Module | Responsibility |
| --- | --- |
| `:app` | Single-Activity Compose host, Navigation 3 (`NavDisplay`), `ACTION_SEND` share receiver, Hilt + WorkManager init, R8 rules |
| `:core:common` | `GlassResult`, `GlassError`, coroutine dispatchers, global `CoroutineExceptionHandler`, `DeviceCapabilities`, formatters |
| `:core:domain` | Pure models (`MediaItemInfo`, `MediaFormat`, `DownloadTask`, `UserPreferences`), repository contracts, use cases |
| `:core:engine` | `youtubedl-android` (`library`, `ffmpeg`, `aria2c`) wrapper, `YtDlpJsonParser`, TikTok fallback strategy, retry/timeout |
| `:core:data` | Room database, ProtoBuf DataStore (`kotlinx-serialization-protobuf`), scoped `MediaStoreExporter`, `DownloadWorker`, `EngineUpdateWorker` |
| `:core:design` | All design tokens, dark/light/AMOLED/dynamic themes, Haze v2 `GlassCard` with fallback, `AnimatedMeshBackground`, `WavyProgressIndicator`, `GlassFloatingBottomBar`, debug `DesignGalleryScreen` |
| `:feature:home` | URL input, clipboard auto-detect banner, shared link ingestion, platform badges |
| `:feature:formats` | Modal bottom sheet for 144p–8K video, audio-only, subtitles, thumbnail, and playlist/batch multi-select |
| `:feature:downloads` | Live queue manager with pause/resume/cancel/retry and wavy progress |
| `:feature:library` | Searchable, sortable download history with share, gallery open, delete, and built-in player launch |
| `:feature:player` | Media3 ExoPlayer with glass overlay header |
| `:feature:settings` | Theme, dynamic color, English/Arabic RTL language, default quality, parallel downloads, Wi-Fi-only, yt-dlp update |
| `:benchmark` | Baseline Profile generator and cold-startup Macrobenchmark |

## Verified Dependency Versions (November 2026)

| Component | Version | Notes |
| --- | --- | --- |
| Gradle / AGP | 9.6.0 / 9.5.0 | Built-in Kotlin, `compileSdk` 37.1 (`minorApiLevel = 1`) |
| Kotlin / KSP | 2.4.21 / 2.3.12 | K2 compiler, Java 21 toolchain |
| Compose BOM / Material 3 | 2026.11.01 / 1.5.0 | `MotionScheme.expressive()` |
| Haze v2 | 2.1.0 | `haze` + `haze-blur` (`Modifier.hazeBlur`) |
| Navigation 3 | 1.2.2 | `navigation3-runtime` + `navigation3-ui` |
| Hilt / AndroidX Hilt | 2.60.1 / 1.3.0 | KSP codegen + WorkManager integration |
| Room / DataStore / WorkManager | 2.8.5 / 1.3.0 / 2.12.0 | Scoped storage + `dataSync` foreground service |
| Media3 / Coil 3 | 1.11.1 / 3.4.0 | ExoPlayer + OkHttp image loading |
| `youtubedl-android` (junkfood02) | 0.18.1 | `library` + `ffmpeg` + `aria2c` |
| Coroutines / Serialization | 1.11.0 / 1.12.0 | JSON + ProtoBuf |
| JUnit 5 / MockK / Turbine / Roborazzi | 5.11.4 / 1.14.12 / 1.2.1 / 1.78.0 | Unit + Compose UI + screenshot testing |
| Spotless / Detekt / LeakCanary | 8.11.0 / 1.23.8 / 2.14 | Formatting, lint, debug leak detection |
