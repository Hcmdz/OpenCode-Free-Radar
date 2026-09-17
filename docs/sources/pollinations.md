# Source: Pollinations API — REJECTED

Date: 2026-09-15. Status: rejected, do not implement. Revisit only if
Pollinations publishes USD pricing with an explicit free tier per model.

## What was evaluated

- Text catalog: `GET https://gen.pollinations.ai/text/models` (keyless,
  documented: "Model listing endpoints work without authentication").
- Siblings: `GET /models` (all modalities), `GET /v1/models`
  (OpenAI-compatible shape), `GET /v1/models/status` (health).
- Live probe 2026-09-15: 217 text models, JSON with `name`, `pricing`
  (pollen currency), `context_length`, `input/output_modalities`,
  `supported_parameters`, `tools`, `description`.

## Why rejected

The catalog carries no USD free signal, so nothing maps to FREE/PAID:

- Prices are denominated in **pollen** (house credit), e.g. prompt
  `0.00000015` pollen/token — not comparable to the zero-USD rule.
- Only 13 models price at all-zero, and all are `community/*` aliases
  with dubious names (`opus-5-max`, `gpt-5.4-nano`, `jimmy`) — unofficial
  proxies, not genuine free models. Every official model costs pollen.
- The credit/quest billing model (free Pollen for prototypes, paid top-up)
  does not fit the radar's free-vs-paid semantics; a "free on Pollinations
  today" row would mislead.
- Terms page (`pollinations.ai/terms`) is JS-rendered and was not audited —
  moot, since the data is unusable regardless.

## Consequences

- No `00X-pollinations` implementation (no fetcher, no pins, no hook).
- Keep S1 (models.dev) + S3 (OpenRouter) + S4 (LiteLLM price map) as the
  catalog sources.
- Revisit if: per-model USD pricing with a documented free tier appears,
  or the catalog gains a first-class free flag. Log the change here first.
