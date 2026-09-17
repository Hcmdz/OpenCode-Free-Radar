# Source: ClawLabs free-ai-models list — REJECTED

Date: 2026-09-18. Status: rejected, do not implement. Revisit only if
the list gains first-party free rows no other source covers, or for
the quota-enrichment idea below.

## What was evaluated

- Data: `GET https://raw.githubusercontent.com/ClawLabsAI/free-ai-models/main/data/models.json`
  — verified 200/11KB/28 entries, bot-updated daily (OpenRouter +
  Pollinations AI). Per-entry `id`, human `name`, `provider`,
  `context_window`, `max_output`, `modalities`, `rate_limit`, `notes`,
  `source` link.

## Why rejected

86% duplication with S3: 24 of 28 rows are OpenRouter `:free` mirrors
with identical ids to rows the direct OpenRouter fetch already owns.
The remaining 4 (Pollinations `gemini-2.0-flash`, `mistral-nemo`,
`mistral-small`, `openai-large`) do not justify a fourth source, and
the README promotes the maintainers' own routing product
(ZeroLimitAI), so the selection may not stay neutral.

## Consequences

- No `clawlabs` source (no fetcher, no pins, no hook).
- The one genuinely unique datum is curated per-model `rate_limit`
  (`40 req/min`, `10 req/min`, `unlimited (no auth)`) — our `quota`
  field is null everywhere today. A future quota-enrichment join onto
  S2 rows by id is the only sanctioned reuse; log it here first.
