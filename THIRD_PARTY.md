# Third-Party Components

No third-party code is vendored into this repository: no `app/libs/` jars, no
`jniLibs/`, no prebuilt binaries. Every dependency arrives as a versioned
Gradle coordinate declared in `gradle/libs.versions.toml`.

Versions below are the ones the **resolved runtime graph** actually carries,
read from `app/build/reports/licensee/androidDebug/artifacts.json`. Where they
differ from the catalog, the graph wins — Gradle conflict resolution upgrades
past the declared pin. `androidx.sqlite:sqlite` is the one such case: the
catalog declares `2.7.0`, and the shipped version is `2.7.1`, pulled up by
Room. It reaches the APK transitively, not through a declared dependency.

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
| Licensee Gradle plugin (licence gate) | 1.14.1 | Apache 2.0 | `Apache-2.0` |

## Notice obligations

Measured against the built release APK, not assumed:

* **Apache-2.0 and BSD-3-Clause texts already ship**, as `LICENSE.txt` files
  under `META-INF/` in the APK: nine copies of the Apache-2.0 text (10,175 bytes
  each, from the AndroidX AARs) and the BSD-3-Clause text (1,434 bytes) from
  `androidx.datastore:datastore-preferences-external-protobuf`. They survive
  because the build sets no `packaging { resources { excludes } }`, so AGP keeps
  AAR `META-INF/**`.
* **No MIT text ships anywhere in the APK.** Three bundled components are MIT
  and their notice is missing.

The three MIT components ship code — R8 retains 89 `com.materialkolor.*`, 36
`com.github.ajalt.colormath.*`, and 18 `org.slf4j.*` classes, per
`app/build/outputs/mapping/release/mapping.txt`.

Their notices are reproduced in full in `app/src/main/res/raw/open_source_notices.txt`
and shown in-app from Settings → About → Open source notices. Each body is copied
byte-for-byte from the upstream `LICENSE` at the released tag, never retyped.

Whether these licences bind the maintainer is a judgement for the maintainer;
this section reports what is present and what is not.

## Licence gate

`app.cash.licensee` runs over the resolved graph and is wired into `check`, so a
dependency introducing a licence outside the allow-list fails the build. The
allow-list in `app/build.gradle.kts` is the measured set: `Apache-2.0`, `MIT`,
`BSD-3-Clause`, plus two `allowUrl` entries for POMs that declare MIT by URL
instead of SPDX (Material Kolor and SLF4J).

The plugin registers tasks per Android variant (`licenseeAndroidDebug`,
`licenseeAndroidRelease`), so **the gate covers the runtime graph only** — the
"Build and test only" table above is not machine-checked. Licensee resolves the
licence of a POM that has no `<licenses>` block by following its parent POM,
which is why SLF4J and Guava need no `allowDependency` entry.

Adding a dependency means re-running `./gradlew :app:licensee`; if it reports an
unresolved licence, add it to the allow-list *and* to the table above, and add
its notice text to the in-app file if the licence requires one.

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