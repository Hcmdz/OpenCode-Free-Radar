# Source: LiteLLM price map (S4, `litellm`)

Admission: accepted per local ADR-013.

## Endpoint (only)

| Purpose | URL |
|---|---|
| Model price map (public raw file, keyless) | `GET https://raw.githubusercontent.com/BerriAI/litellm/main/model_prices_and_context_window.json` |

4113 keys / 105 zero-cost chat entries at admission (2026-09-18), 2.6MB.
Synced live every run, never bundled. Ktor `HttpCache` serves 304s from
the file ETag, so unchanged days cost one conditional request. Explicitly
NOT sources: the LiteLLM repo docs, issues, or any other file.

## Terms analysis

- The file is served publicly without account, key, or signing — the
  models.dev pattern (machine-shaped data over plain HTTP), not page
  scraping. No inference calls (the paid parts never trigger).
- Courtesy policy shared with the other sources: stock HTTP client,
  daily + manual cadence, `Retry-After` backoff on 429 (WorkManager
  exponential retry already in place), conditional requests.

## Mapping to offers

| File fact | Offer field |
|---|---|
| key (e.g. `gemini/gemini-exp-1114`) | modelId / name (no display name in the file — the key is shown raw) |
| `litellm_provider` | providerId / remoteId (`<provider>/<key>`) |
| `input_cost_per_token` / `output_cost_per_token` == `0`/`0` | FREE via the shared zero-price rule (never FREE on missing) |
| `max_input_tokens` / `max_output_tokens` | contextLength / maxOutputTokens |
| `supports_function_calling` | supportsTools (shared tools-gated compat rule) |
| `supports_vision` / `supports_response_schema` | supportsVision / supportsStructuredOutput |
| no quota/conditions/homepage in the file | quota/conditions/officialUrl stay null |

## `openrouter`-provider exclusion

269 of the 4113 entries carry `litellm_provider=openrouter` with keys of
the form `openrouter/<author>/<slug>` — a proxy mirror of the catalog
`OpenRouterSource` already fetches directly (fresher and richer:
descriptions, endpoints). They are dropped at parse time (105 zero-cost
→ 89 ingested) instead of doubling every row. Verified zero id
collisions with S1 provider names at admission.

## Local/self-hosted policy

29 `ollama` + 5 `lemonade` + 6 `sagemaker` zero-cost rows: zero cost
means "no meter", not "free hosted offer". They are ingested (no data
loss) but hidden behind the `showLocal` sheet switch (default off) via
`LOCAL_PROVIDERS` in `domain/model` — a pure providerId predicate, no
schema change. Revealed rows carry a neutral `Local` pill next to the
source pill (list cards + details hero).

## Deletion semantics

Shared with all sources (see `openrouter.md`): `missedSyncs` counter,
MODEL_REMOVED + delete at ≥2 consecutive absences (favorites never
deleted); failed fetch removes nothing; >50% shrink fails closed. The
empty-parse guard means a parser regression can never wipe the cache.

## Cross-source check

Pin-based and source-agnostic (`crossCheck`, exact `remoteId` match,
never fuzzy): new pins may link LiteLLM twins of S1/S3 rows as they are
reviewed. No pins shipped at admission.
