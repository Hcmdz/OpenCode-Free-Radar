# Source: Eden AI Models API — REJECTED

Date: 2026-09-18. Status: rejected, do not implement. Revisit only if
the free catalog grows well beyond its current 8 rows with provider-
direct models not covered elsewhere.

## What was evaluated

- Discovery endpoint (documented public, no key):
  `GET https://api.edenai.run/v3/models` — verified 200/1.4MB/1060
  entries at evaluation. (The `app.edenai.run/models` page itself sits
  behind Cloudflare Turnstile and was never a candidate.)
- Schema is good — better per-entry than LiteLLM: `id`
  (`provider/model`), `capabilities.supports_function_calling`,
  `context_length`, `pricing` + `list_pricing`, `description`,
  `source`, `regions`. No `mode` field (chat would need filtering on
  `capabilities` input/output modalities).

## Why rejected

Marginal net yield. Only 8 zero-price rows (4 Cloudflare LoRAs,
2 Google Gemma, 1 Cohere, 1 Together AI) — and all but the 2 Gemma
rows already flow in via the LiteLLM source. Same ~150-line
integration cost as LiteLLM (fetcher, parser, tests, E2E, strings,
sheet row) for ~2 net models. Eden prices are also routed/margined,
second-hand next to provider-direct figures.

## Consequences

- No `edenai` source (no fetcher, no pins, no hook).
- Designated fallback: if the LiteLLM price map ever dies, this
  endpoint is the replacement candidate (stable, documented, keyless,
  rich schema). Log the switch here first.
