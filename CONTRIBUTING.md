# Contributing to OpenCode Free Radar

## Getting Started

1. Fork the repository
2. Clone your fork
3. Open in Android Studio (latest stable)
4. Sync Gradle and build

## Requirements

- Android Studio latest stable
- JDK 17+
- Android SDK with platform 37 (compileSdk 37)
- Device or emulator running Android 9+ (minSdk 29)

## Build

```bash
# Debug build
./gradlew :app:assembleDebug
./gradlew :app:installDebug   # needs a running emulator or device
```

## Checks

Run these before opening a pull request:

```bash
./gradlew :app:testDebugUnitTest :app:lintDebug
./gradlew :app:connectedDebugAndroidTest   # needs a running emulator
```

## Project Rules

- Single `:app` module with Koin for dependency injection — never introduce
  a second DI framework.
- Room is the source of truth with `exportSchema=true`. Never edit a shipped
  migration — always add a new one, with a migration test.
- Never commit secrets: keystores, `local.properties`, API keys, passwords.
- Write source code and comments in English. Localized UI strings stay in
  `values*/strings.xml` in their target language (`en`, `fr`, `ar`).

## Code Style

- Kotlin with official conventions
- Jetpack Compose for UI, Material 3 design system
- Follow existing patterns in the codebase; smallest correct change wins

## Commits

This project uses Conventional Commits (`feat:`, `fix:`, `docs:`, …) with an
optional scope, e.g. `fix(dashboard): blend search field into docked search bar`.
Keep commits atomic — one concern per commit.

## Pull Requests

1. Create a feature branch from `main`
2. Make your changes
3. Run the checks above and fix any issues
4. Write a clear description using the PR template
5. Open the PR against `main`

## Issues

- Use the provided issue templates
- Include device model, Android version, and app version
- Steps to reproduce for bug reports
- For security vulnerabilities, see [SECURITY.md](SECURITY.md) — do not open
  a public issue
