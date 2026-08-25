# RELEASE CHECKLIST // PROJECT GAMBIT

This document serves as the release and deployment guide for **GAMBIT**, an offline-first tactical turn-based strategy game on Android.

---

## 1. PRE-FLIGHT CHECKS

- [ ] **Platform Naming Sync**: Verify that `metadata.json`'s `"name"` matches `<string name="app_name">` in `strings.xml` ("Gambit").
- [ ] **Application ID**: Verify the unique `applicationId` in `app/build.gradle.kts` is properly set to `com.aistudio.gambit.twryzp`.
- [ ] **SDK Versions**:
  - `minSdk` = 26 (Android 8.0)
  - `targetSdk` = 34 (Android 14)
- [ ] **Clean Dependency Footprint**: Ensure unused imports and non-essential dependencies are commented out in `app/build.gradle.kts` to minimize APK footprint.
- [ ] **Local Storage Migrations**: Room database versions must be locked down, and any schema changes must trigger destructive or manual migration procedures.

---

## 2. COMPILING PRODUCTION BUILD

GAMBIT builds using standard Gradle tasks.

### Clean & Assemble Release Bundle

To generate a signed or unsigned release APK/Bundle:

```bash
gradle clean :app:assembleRelease
```

This generates the release artifact at:
`app/build/outputs/apk/release/app-release.apk`

---

## 3. ASSETS & SOUND POLISH

- **Sound effects** are stored in the assets module: `app/src/main/assets/`
- Ensure standard game sounds are properly configured:
  - `clash_win.mp3`
  - `clash_lose.mp3`
  - `clash_tie.mp3`
  - `game_over_win.mp3`
  - `game_over_lose.mp3`
  - `token_place.mp3`

---

## 4. LOCAL SECURITY & PRIVACY

- No server-side API keys are hardcoded in the source code.
- Dynamic or user configuration credentials should be provided via the **Secrets panel** which compiles into `.env` / `.env.example` at compile time.
- Standard proguard rules (`proguard-rules.pro`) are enabled for class-shrinking and optimization.

---

## 5. QUALITY ASSURANCE METRICS

- Run standard unit and Roborazzi tests:
  ```bash
  gradle :app:testDebugUnitTest
  ```
- Verify game engine performance budgets:
  - Startup cold latency < 2s
  - AI planning and capability variance decision time < 100ms
  - Memory consumption peak < 150MB

---
**OPERATIONS CONCLUDED. SECURE DEPLOYMENT IS IN EFFECT.**
