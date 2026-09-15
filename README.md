# OpenCode Free Radar

Native Android app that tracks AI models free to use with
[OpenCode](https://opencode.ai) and notifies you when new free offers appear.

## Features

- Daily catalog sync from public model indexes (starting with models.dev)
- Free / paid / unknown pricing status per model
- OpenCode compatibility flags (tool calling, vision, reasoning)
- Price history and change log per model, stored on device
- Favorites, filters, offline-first Room database
- Daily background sync via WorkManager, alerts on new freebies
- English, French, and Arabic UI with RTL layout support
- Material 3 dynamic color with dark and high-contrast themes

## Tech stack

- Kotlin 2.4.20, Jetpack Compose + Material 3 Expressive
- Single `:app` module, MVI with StateFlow, Navigation 3
- Koin (DI), Room 3.0 (source of truth), Ktor (network)
- DataStore (settings), WorkManager (daily sync)
- JUnit 5 + Turbine + AssertK (unit), Compose UI tests + orchestrator (E2E)
- minSdk 29, targetSdk 36, JDK 17, AGP 9.4.0

## Getting started

Prerequisites: JDK 17 and the Android SDK with platform 37.

```bash
./gradlew :app:assembleDebug
./gradlew :app:installDebug
```

Run checks:

```bash
./gradlew :app:testDebugUnitTest :app:lintDebug
./gradlew :app:connectedDebugAndroidTest   # needs a running emulator
```

## Release signing

Release builds sign with a keystore that lives **outside** this repository.
Without it, every task still configures and debug builds work fine, but the
release output ships unsigned. To sign releases, create a properties file at:

```text
<your-keystore-dir>/gradle.properties
```

with these keys (values stay on your machine, never committed):

```properties
RELEASE_STORE_FILE=<path-to-your-keystore>
RELEASE_STORE_[RELEASE_STORE_PASSWORD]
RELEASE_KEY_ALIAS=[ALIAS]
RELEASE_KEY_[RELEASE_KEY_PASSWORD]
```

Then point `keystoreFichier` in `app/build.gradle.kts` at your file and run:

```bash
./gradlew :app:assembleRelease
```

## Privacy

All data stays on device. The app fetches the public model catalog over
HTTPS, stores it in a local Room database, and runs syncs in the background.
No account, no analytics, no third-party tracking SDK.

## License

Copyright holders are listed in the git history.
This program is free software under the GNU General Public License v3.0 or
later. See [LICENSE](LICENSE) for the full text.
