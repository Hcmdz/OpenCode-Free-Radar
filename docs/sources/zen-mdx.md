# Source: Zen docs pricing via GitHub (MDX, `opencode-data` enrichment)

Date: 2026-09-19. Status: accepted per §64.

## What is fetched

- `GET https://raw.githubusercontent.com/anomalyco/opencode/dev/packages/web/src/content/docs/zen.mdx`
  (the source of `https://opencode.ai/docs/zen/`, MIT-licensed repo —
  "software and associated documentation files").
- Parsed tables: Pricing (`Free`/`Free` rows) joined with Endpoints
  (display name → model id). Daily cadence inside the existing
  `opencode-data` sync; no new source partition, no new worker.

## Why this is not scraping (§64b)

- The fetch never touches `opencode.ai` pages: the opencode.ai Terms of
  Use scraping clauses cover their Services/Content, not a third-party
  host serving MIT-licensed files (same pattern as the LiteLLM price map).
- The repo license explicitly permits use/copy/merge; the copyright notice
  is recorded in `THIRD_PARTY.md`. Facts consumed: model ids + prices.
- No auth, no circumvention, one small text file per run, ETag/`304`
  via the shared Ktor cache, hash-skip when unchanged.

## Fusion rule (roster × MDX)

Roster (`/zen/v1/models`) proves *served*, MDX proves *free-priced*:

- confirmed (rings): served AND (`-free` suffix/`big-pickle`, or MDX-priced).
  The roster leads the docs by days, so a served `-free` row needs no doc.
- `TO_VERIFY` (silent): `$0` served by neither signal, or MDX-only
  (stale doc for an unserved model).
- Roster/MDX free ids missing from models.dev are synthesized as silent
  `TO_VERIFY` rows (`conditions`/`quota` null, provenance in `sourceUrl`).

## Pins: N/A (verified 2026-09-19)

S1 `opencode/*` and S2 OpenRouter namespaces are disjoint
(`opencode/mimo-v2.5-free` vs `xiaomi/…`, `nvidia/…:free`); pins require
exact `remoteId` match, never fuzzy — so no new pin. The pre-existing
bothub↔nvidia pin stands. Revisit only with hand-verified exact pairs.
