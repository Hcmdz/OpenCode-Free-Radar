# Source: NVIDIA Build catalog (S2, `nvidia-build`)

Admission: accepted per §64 (see `docs/decisions/ADR-008-nvidia-source.md`).

## Endpoints (all public, no key)

| Purpose | URL |
|---|---|
| Discovery index (server-rendered, no JS) | `https://build.nvidia.com/models.md` |
| Model card | `https://build.nvidia.com/qc69jvmznzxy/<slug>.md` |
| Free-tier rule (quota provenance) | `https://build.nvidia.com/models` (FAQ) |
| Canonical per-model page | `https://build.nvidia.com/<publisher>/<slug>` (frontmatter `canonical`) |

Cards carry YAML frontmatter (`title, publisher, type, updated, description,
canonical`) plus `Model Summary` / `Specifications` sections. Card body shape
varies by author (NVIDIA vs third party) — every field parses fail-closed.

## Bot protection (proven 2026-09-15)

The host challenges default HTTP clients (anti-bot interstitial) and
rate-limits aggressively. The source therefore sends a browser User-Agent
(see `NvidiaBuildSource`) and treats every per-card failure as skip +
keep-last-valid (a fully empty result fails safe via the empty-catalog guard).
Live NIM endpoints are never called.

## Mapping to offers

| Card fact | Offer field | Confidence |
|---|---|---|
| title / slug | name / modelId (`nvidia` provider) | OFFICIAL context |
| canonical | officialUrl + sourceUrl | OFFICIAL |
| Context Length row (`1M`, `131,072 tokens`) | contextLength (max credible, capped) | OFFICIAL context |
| License row | conditions (with free-tier note) | OFFICIAL context |
| tool/function-calling signals (negation-aware) | supportsTools | OFFICIAL context |
| vision/multimodal signals | supportsVision | OFFICIAL context |
| FAQ: free to prototype, ~40 RPM, no per-token billing | freeStatus LIMITED + quota `~40 RPM` | AUTOMATICALLY_DETECTED |
| missing anything | null / UNKNOWN, never invented | — |

The headline claim (LIMITED) is FAQ-derived, so the whole offer ships at
AUTOMATICALLY_DETECTED. Prices are always UNKNOWN — this source alone can
never yield FREE.

## Scope

Chat/text/code/agent cards only (`NvidiaSignalLists`, exclusion wins).
Exclusion cases are unit-tested; a missed model is accepted over a wrong offer.

## Terms note

Reads are unauthenticated public card fetches at daily + manual cadence. The
trial service itself is governed by the NVIDIA API Trial Terms of Service; the
app never uses playground endpoints and stores no key.
