# GAMBIT // RELEASE DEPLOYMENT CHECKLIST

This checklist verifies the production readiness of GAMBIT, an Android 2D turn-based calculated-risk strategy game. Follow these steps prior to submitting the final APK/AAB bundle to the Google Play Console.

---

## 1. ARCHITECTURE & CODE COMPLIANCE
- [x] **Layer Boundaries**: The presentation layer depends on the domain layer, with zero direct dependency on the data layer.
- [x] **Dependency Injection**: Hilt modules strictly configure and inject singletons (`UserProfileRepository`, `GameHistoryRepository`, etc.).
- [x] **No Forbidden Tech**: Cleaned out all traces of old XML layout files, Fragments, and RxJava. Using 100% Jetpack Compose and Coroutines.
- [x] **Type-Safe Routing**: All navigation routes in `GambitApp.kt` use safe Compose Navigation keys.

---

## 2. PRODUCTION COMPILATION & PERFORMANCE
- [x] **Code Obfuscation**: Enabled `isMinifyEnabled = true` and `isShrinkResources = true` in `app/build.gradle.kts` to optimize space.
- [x] **ProGuard Safety Rules**: Verified that `proguard-rules.pro` keeps domain/data reflection models intact to prevent Firestore mapping crashes.
- [x] **APK Size Verification**: Commented out all unused/extraneous camera and permission libraries in Gradle to keep download size minimal.
- [x] **Performance Budgets**:
    - AI planner calculation completes in **< 5ms** locally.
    - Clashes resolve sequentially with smooth Material transitions.
    - Screen density scales appropriately using WindowSize classes.

---

## 3. PLAY CONSOLE ASSETS & IDENTITY
- [x] **Launcher Icon**: Scalable vector launcher icon registered inside `AndroidManifest.xml` via `ic_launcher` and `ic_launcher_round`.
- [x] **Splash Screen Theme**: Configured `Theme.Gambit.Splash` with a full-screen dark background to avoid bright-white flashes on boot.
- [x] **Product Name Sync**: Set `app_name` in `strings.xml` and synced the workspace metadata identity in `metadata.json`.

---

## 4. LOCAL PLAYER SYSTEM INITIALIZATION
- [x] **Onboarding Verification**: Onboarding completed state successfully records to Jetpack DataStore (`onboardingCompleted = true`).
- [x] **Callsign Security**: Validation checks in `Validators.kt` enforce 3–20 character limits for commander callsigns.
- [x] **Profile Integration**: Display name updates sync seamlessly between the UI, cached room instances, and Firestore collections.

---

## 5. RE-ENGAGEMENT & PLAYBACK TESTING
- [x] **Offline Capabilities**: Full fallback gracefully allows local solo battles when offline (validated via `ConnectionStatusBanner`).
- [x] **Audio Controls**: Sliders for master volume and switches for sound effects are connected to the central settings state engine.
