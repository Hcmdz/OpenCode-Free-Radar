# Changelog

All notable changes to OpenCode Free Radar.

Versions follow [semver](https://semver.org/); full history lives in [GitHub Releases](https://github.com/Hcmdz/OpenCode-Free-Radar/releases).

## Unreleased

- **A favorite survives a catalog migration** — a model that moved to another pinned catalog lost its favorite flag: the carry-over read the per-source snapshot, so the migrated row was not in the favorites set and the upsert replaced the stored row. The carry-over now reads the whole table

## v0.5.1

- **Free-to-paid withdrawals are no longer masked** — a cross-source disagreement used to demote the row to unverified and silence the withdrawal it caused; the expiry test no longer depends on confidence, so the row that was shown as free rings when it stops being so
- **Catalog migrations no longer hide withdrawals** — a model that moves between pinned catalogs is compared against itself instead of being treated as a newcomer
- **Gated providers now ring** — a model that was free and lands behind a paywall (`*-plan`, gitlab) announces the loss instead of disappearing
- **Offer terms are tracked** — a change to a trial's conditions or its temporary flag is recorded in the model's history
- **Anti-flap window tightened to 30 days** — a flickering free tier re-alerts sooner, and history is pruned at the same horizon
- **Dependency refresh** — Kotlin 2.4.21, Gradle 9.8.1 (three security advisories), Compose BOM alpha 2026.10.00, Material 3 1.5.0-beta01

## v0.5.0

- **Licence texts travel with the app** — the notice previously named each licence but linked out instead of reproducing it, which does not satisfy Apache-2.0 §4(d) in a distributed APK. Settings → About → Open source notices now lists every bundled component and reproduces the full text of each licence
- **One file per licence, not per component** — 273 resolved artifacts under three licences embed three texts, so Apache-2.0 is carried once instead of 265 times
- **An unmapped licence fails the build** — a licence id with no body used to degrade to a quietly shorter notice; it now stops the build
- **Notices read correctly in Arabic** — licence prose and artifact identifiers are Latin, so both the list and the body are laid out left-to-right instead of inheriting the locale's right-to-left paragraph direction

## v0.4.3

- **Third-party notices in-app** — Settings → About → Open source notices lists the licence of every bundled component. Three of them (Material Kolor, colormath, SLF4J) are MIT and require their notice text to travel with the app; it previously did not
- **Dependency licences are gated in CI** — a new dependency carrying a licence outside the measured allow-list fails the build instead of shipping unnoticed
- **Expiry alerts name the offers that lapsed** — the alert body listed a generic count and nothing else; the models that stopped being free are now listed by name
- **Alert hints sit under their own switch** — the expiry hint had drifted below the *new freebies* switch, describing the wrong setting

## v0.4.2

- **In-app updates install again** — the APK downloaded and then nothing happened: no prompt, no error, no crash. The install session was committed with an immutable status receiver, which the platform rejects outright. The confirmation prompt now appears

## v0.4.1

- **Expiry alerts on their own switch** — "Alert me about new free offers" no longer bundles expiry alerts; a second toggle follows new freebies without pinging you when an offer turns paid or lapses. On by default, so existing installs keep today's behaviour
- **New launcher icon** — the radar artwork is redrawn as a full-bleed adaptive icon, and a line-art monochrome layer keeps themed icons working on Android 13+

## v0.4.0

- **Auto-sync on a schedule** — pick the network constraint (Wi-Fi, Wi-Fi on battery, battery only, always) and the cadence from 8 to 72 hours; the choice survives restarts
- **Alerts on by default** — a sync that spots new free models raises a notification, and the permission is asked once at first launch instead of being buried in settings
- **Favorites pinned to the top** — a starred model stays above the rest of the list whatever sort is active, and models tied on the same key keep a stable order
- **Self-update reported its outcome — but did not install** — this release's changelog originally claimed the update flow was repaired. It was not: the download completed and the install silently did nothing. The actual fix landed in v0.4.2

## v0.3.1

- **Confirmed-only free views** — unverified $0 rows demote silently, gated tiers leave the free views, one shared confirmed-free definition
- **Zen free detection** — live roster × pricing doc fusion, synth rows for catalog lag, expiry alerts on trial end
- **Remembered filters** — dashboard filters, sort, local switch, and recent searches survive restarts
- **Pricing evidence** — Kenari $0 rows mapped to paid (IDR prepaid-wallet policy proven via models.dev history); evidence log in `docs/sources/models-dev-evidence.md`

## v0.3.0

- **Third catalog source** — LiteLLM price map with local/self-hosted rows hidden by default and marked with a Local pill
- **Stronger cross-check** — free models confirmed seen on both sources before alerting
- **Dashboard rework** — draggable floating filter button replaces the filter bar; one-tap status chips; compact token counts and pinned top bar; reset escape hatch for empty states; filter position is retained

## v0.2.0

- **In-app updates** — update check via GitHub Releases with sideload gated on host allowlist and unknown-sources consent
- **Data-sparing sync** — Wi-Fi-only mode, metered-data guard, auto first sync on install; unchanged catalogs skipped
- **Expressive redesign** — Material 3 Expressive overhaul with fullscreen content
- **Dashboard polish** — title bar, favorites filter, sort, loading skeleton; snapshot card opens on notification tap; sync age refreshes every minute
- **Build** — Room 3.0.3, Navigation 3 1.1.7

## v0.1.0

- **Initial release** — models.dev catalog source, multi-source sync with PARTIAL state and cross-check, OpenRouter source
- **Event notifications** — global toggle, 3 events, 1 summary per sync
- **Safe offline core** — Room v1→v2 with absence counter and absence-gated removal; degraded mode instead of catalog wipe on empty responses
- **App shell** — pull-to-refresh, collapsible settings, about section, adaptive launcher icon
- **Sources hygiene** — NVIDIA Build source dropped over website ToS; rejections documented in `docs/sources/`
