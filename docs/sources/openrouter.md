# Source: OpenRouter Models API (S3, `openrouter`)

Admission: accepted per §64 (see local ADR-009).

## Endpoint (only)

| Purpose | URL |
|---|---|
| Model catalog (documented, keyless) | `GET https://openrouter.ai/api/v1/models` |

446 models / 20 `:free` at admission (2026-09-15), 738KB, ~0.3s. Explicitly
NOT sources: collections pages, the `:free` Router (non-deterministic,
simulation-only), the playground, per-model web pages.

## Terms analysis (ToS Aug 31, 2026)

- §7(5) bans scraping/crawling the *Site*. This integration calls only the
  documented public API — the models.dev pattern (explicit programmatic
  interface), not page scraping.
- No account, no key, no credits, no inference calls (the paid parts of the
  ToS never trigger).
- Courtesy policy: browser UA, daily + manual cadence, `Retry-After` backoff
  on 429 (WorkManager exponential retry already in place), bounded payloads.

## Mapping to offers

| API fact | Offer field |
|---|---|
| `id` (`provider/model[:variant]`) | providerId / modelId / remoteId |
| `pricing.prompt/completion` decimal strings | input/outputPrice (`toDoubleOrNull`, unparseable → null) |
| `:free` + `0`/`0` → FREE; non-zero → PAID; missing → UNKNOWN | freeStatus (never FREE on missing) |
| `context_length`, `top_provider.max_completion_tokens` | contextLength / maxOutputTokens |
| `tools` in `supported_parameters` | supportsTools (shared tools-gated compat rule) |
| `image` in `architecture.input_modalities` | supportsVision (null when absent) |
| `hugging_face_id` | officialUrl (`huggingface.co/<id>`) |
| `per_request_limits` (uniformly null today) | quota stays UNKNOWN |
| `expiration_date` | conditions (`Free trial ends <date>.`) |

Known free-tier shape (doc-level, not per-model observed): ~20 RPM and
50/1000-per-day caps. Applied to quota only if ever observed per model.

## Deletion semantics (Story 2)

`missedSyncs` counter (Room v2): present → 0; missing on success → +1, kept;
MODEL_REMOVED + delete at ≥2 consecutive absences (favorites never deleted);
failed fetch → nothing; >50% shrink → FAILED (`shrunk-catalog`), no mutation.

## Cross-source check vs models.dev (S1)

After every `refreshAll`, pinned same-model pairs are compared
(`crossCheck` in `domain/usecase`, pins in `OVERLAP_PINS` — static, reviewed,
exact `remoteId` match, never fuzzy):

- both sides usable-free (`FREE`, `LIMITED`, `TRIAL`, `TEMPORARY` — group
  agreement, e.g. `FREE` vs `TRIAL` with an `expiration_date`) → both rows
  `CROSS_CHECKED` (Details shows "Verified · 2 sources");
- usable-free vs `PAID`/`EXPIRED` → both rows `TO_VERIFY` (silent, no bell);
- `UNKNOWN` on either side → pin skipped (missing data is not a disagreement);
- pin with only one side present → a usable-free survivor is demoted to
  `TO_VERIFY` (delisted elsewhere stays unconfirmed);
- a side whose source has no real fetch inside the 6h freshness window is
  skipped (`skipped-metered` proves nothing, `FAILED` runs are passed over,
  `skipped-hash`/`skipped-fresh` attest); stale pins fail open;
- Zen-roster ghosts are never promoted by a pin (roster wins), but a
  disagreement still demotes them.

`updateConfidence` touches only the confidence column: confidence is not
freshness (`verifiedAt` is left untouched so list ordering by verification
age does not move on a cross-check).
