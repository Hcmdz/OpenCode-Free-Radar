# Third-Party Components

No third-party code is vendored into this repository: no `app/libs/` jars, no
`jniLibs/`, no prebuilt binaries. Every dependency arrives as a versioned
Gradle coordinate declared in `gradle/libs.versions.toml`, and the versions
below match that catalog.

The APK does ship native code — two libraries, each pulled in transitively by
an AndroidX artifact and reproduced for `arm64-v8a`, `armeabi-v7a`, `x86`, and
`x86_64`:

| Native library | Origin | Licence |
|---|---|---|
| `libandroidx.graphics.path.so` | `androidx.graphics:graphics-shapes` | Apache 2.0 |
| `libdatastore_shared_counter.so` | `androidx.datastore:datastore-preferences` | Apache 2.0 |

Both were inspected for statically linked third-party engines: no OpenSSL, no
zlib, no libcurl, no SQLite symbols. Nothing is linked in beyond what its own
AndroidX POM declares.

## Shipped at runtime

| Library | Version | Licence | SPDX |
|---|---|---|---|
| Kotlin stdlib | 2.4.20 | Apache 2.0 | `Apache-2.0` |
| Jetpack Compose (BOM alpha) | 2026.09.01 | Apache 2.0 | `Apache-2.0` |
| Material 3 Expressive | 1.5.0-alpha29 | Apache 2.0 | `Apache-2.0` |
| Compose Material icons (extended) | 1.7.8 | Apache 2.0 | `Apache-2.0` |
| Navigation 3 | 1.1.7 | Apache 2.0 | `Apache-2.0` |
| AndroidX Activity Compose | 1.13.0 | Apache 2.0 | `Apache-2.0` |
| AndroidX Lifecycle | 2.11.0 | Apache 2.0 | `Apache-2.0` |
| AndroidX Core / AppCompat | 1.19.1 / 1.8.0 | Apache 2.0 | `Apache-2.0` |
| AndroidX Graphics Shapes | 1.1.0 | Apache 2.0 | `Apache-2.0` |
| Koin (BOM, Android, Compose) | 4.2.2 | Apache 2.0 | `Apache-2.0` |
| Touchlab Stately (via Koin) | 2.1.0 | Apache 2.0 | `Apache-2.0` |
| Room 3 | 3.0.3 | Apache 2.0 | `Apache-2.0` |
| Room (transitive via WorkManager) | 2.7.0 | Apache 2.0 | `Apache-2.0` |
| AndroidX SQLite | 2.7.1 | Apache 2.0 | `Apache-2.0` |
| Ktor (client core, OkHttp engine, content negotiation) | 3.6.0 | Apache 2.0 | `Apache-2.0` |
| OkHttp | 5.5.0 | Apache 2.0 | `Apache-2.0` |
| Okio | 3.18.2 | Apache 2.0 | `Apache-2.0` |
| kotlinx.serialization JSON / coroutines | 1.11.0 | Apache 2.0 | `Apache-2.0` |
| kotlinx-io | 0.9.1 | Apache 2.0 | `Apache-2.0` |
| DataStore Preferences | 1.2.1 | Apache 2.0 | `Apache-2.0` |
| DataStore protobuf runtime | 1.2.1 | BSD 3-Clause | `BSD-3-Clause` |
| WorkManager | 2.12.0 | Apache 2.0 | `Apache-2.0` |
| JSpecify annotations | 1.0.0 | Apache 2.0 | `Apache-2.0` |
| Guava ListenableFuture | 1.0 | Apache 2.0 | `Apache-2.0` |
| **Material Kolor** (dynamic color) | 5.0.1 | **MIT** | `MIT` |
| **colormath** (via Kolor) | 3.6.1 | **MIT** | `MIT` |
| **SLF4J API** (via Ktor) | 2.0.19 | **MIT** | `MIT` |

## Build and test only

None of these are packaged in the APK.

| Library | Version | Licence | SPDX |
|---|---|---|---|
| JUnit 5 | 6.1.3 | Eclipse Public License 2.0 | `EPL-2.0` |
| Turbine | 1.2.1 | Apache 2.0 | `Apache-2.0` |
| AssertK | 0.28.1 | Apache 2.0 | `Apache-2.0` |
| Espresso / test orchestrator / test runner | 3.7.0 / 1.6.1 / 1.7.0 | Apache 2.0 | `Apache-2.0` |
| Compose UI test | 1.13.0-alpha03 | Apache 2.0 | `Apache-2.0` |
| Room testing / WorkManager testing | 3.0.3 / 2.12.0 | Apache 2.0 | `Apache-2.0` |
| AndroidX security lint checks | 1.0.4 | Apache 2.0 | `Apache-2.0` |

## Notice obligations

Apache-2.0 §4(d), MIT, and BSD-3-Clause each require the licence notice to
travel with the distribution. **The release APK does not carry them**: R8 drops
`META-INF` from AARs, and the shipped `classes.dex` contains zero occurrences of
any licence text. No `NOTICE` file exists in this repository, and the app has no
in-app notices screen — so the three MIT components and the BSD-3-Clause
DataStore protobuf runtime are currently shipped without their notice text
reachable by a user.

Reproducing those notices in an app-reachable location is an open obligation,
tracked separately from this inventory. This file is the source inventory, not
the discharge.

## Notes

- Licence identifiers above were read from each artifact's POM metadata or from
  the licence file inside the jar, not inferred. Two artifacts carry no `<licenses>`
  block at all: SLF4J API 2.0.19 (MIT, read from `META-INF/LICENSE.txt` inside
  the jar) and Guava ListenableFuture 1.0 (Apache 2.0, inherited from the
  `guava-parent` POM).
- Room 3.0.3 is the direct dependency. Room 2.7.0 arrives transitively through
  WorkManager, so both are in the APK; they are distinct coordinates and both
  Apache 2.0.
- Data sources are public HTTPS model catalogs (models.dev, OpenRouter, LiteLLM
  price map). They are services, not bundled software — see `docs/sources/`.
- OpenCode docs source (`packages/web/src/content/docs/zen.mdx`,
  `anomalyco/opencode`, MIT © 2025 opencode): fetched live from
  `raw.githubusercontent.com`, never bundled; pricing facts joined with the
  Endpoints table to confirm Zen free models — see `docs/sources/zen-mdx.md`.