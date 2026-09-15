# Third-Party Components

This project ships no bundled binaries or vendored libraries. All
third-party code arrives as versioned Gradle dependencies declared in
`gradle/libs.versions.toml`. Versions below are synced with that catalog.

## Dependency Licenses

| Library | Version | License | SPDX |
|---|---|---|---|
| Kotlin stdlib | 2.4.20 | Apache 2.0 | `Apache-2.0` |
| Jetpack Compose (BOM alpha) | 2026.09.00 | Apache 2.0 | `Apache-2.0` |
| Material 3 Expressive | 1.5.0-alpha28 | Apache 2.0 | `Apache-2.0` |
| Navigation 3 | 1.1.4 | Apache 2.0 | `Apache-2.0` |
| AndroidX Lifecycle | 2.11.0 | Apache 2.0 | `Apache-2.0` |
| AndroidX Core / AppCompat | 1.19.0 / 1.8.0 | Apache 2.0 | `Apache-2.0` |
| Room | 3.0.0 | Apache 2.0 | `Apache-2.0` |
| Ktor (client, OkHttp engine) | 3.5.2 | Apache 2.0 | `Apache-2.0` |
| kotlinx.serialization / coroutines | 1.11.0 / 1.11.0 | Apache 2.0 | `Apache-2.0` |
| Koin (BOM) | 4.2.2 | Apache 2.0 | `Apache-2.0` |
| DataStore Preferences | 1.2.1 | Apache 2.0 | `Apache-2.0` |
| WorkManager | 2.11.2 | Apache 2.0 | `Apache-2.0` |
| Coil 3 | 3.5.0 | Apache 2.0 | `Apache-2.0` |
| Kermit (logging) | 2.1.0 | Apache 2.0 | `Apache-2.0` |
| Espresso / Test orchestrator | 3.7.0 / 1.6.1 | Apache 2.0 | `Apache-2.0` |
| Turbine | 1.1.0 | Apache 2.0 | `Apache-2.0` |
| AssertK | 0.28.1 | Apache 2.0 | `Apache-2.0` |
| JUnit 5 (test only, not shipped) | 5.14.4 | EPL-2.0 | `EPL-2.0` |

## Notes

- JUnit 5 is used for local unit tests only and is not packaged in the APK.
- Remaining catalog entries (activity-compose, SQLite driver, window-size
  class, adaptive navigation suite, test runner, ui-test, KSP-adjacent
  compiler artifacts) are Apache 2.0 AndroidX/KotlinX artifacts under the
  same terms as the rows above.
- All Apache 2.0 dependencies include their respective license files in the
  AAR/APK.
- Data sources are public HTTPS model catalogs (models.dev, OpenRouter);
  they are services, not bundled software — see `docs/sources/`.
