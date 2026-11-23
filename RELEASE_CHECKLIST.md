# Release Checklist

- [ ] Run `./gradlew detekt spotlessCheck testDebugUnitTest` and verify zero issues.
- [ ] Run `./gradlew :benchmark:pixel6Api35BenchmarkAndroidTest` (or connected device) to refresh the Baseline Profile in `:app/src/main/baseline-prof.txt`.
- [ ] Run `./gradlew :core:design:verifyRoborazziDebug` to confirm zero visual regressions.
- [ ] Configure a release signing keystore via CI secrets (`KEYSTORE_BASE64`, `KEYSTORE_PASSWORD`, `KEY_ALIAS`, `KEY_PASSWORD`) — never commit keystores to Git.
- [ ] Build signed release AAB/APK with `./gradlew bundleRelease assembleRelease` and verify R8 full-mode shrinking preserves `youtubedl-android`, Media3, Room, and Kotlinx Serialization classes.
- [ ] Smoke-test on physical devices: Android 8.0 (API 26, fallback blur) and Android 14+ (API 34+, live Haze v2 blur), both English LTR and Arabic RTL, including `ACTION_SEND` share from TikTok and YouTube.
