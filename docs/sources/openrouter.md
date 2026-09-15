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
