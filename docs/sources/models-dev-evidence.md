# Source: models.dev GitHub curation as pricing evidence

Date: 2026-09-19. Status: accepted (evidence record, not a fetch).

This file records what the public MIT-licensed `sst/models.dev` repository
proves about ambiguous `$0` rows. It changes no fetching: the app keeps
reading only `https://models.dev/api.json`. It does NOT revisit the
rejections in `nvidia.md` (no automated reads of NVIDIA's site) or
`opencode-data.md` (no opencode.ai page extraction).

## Method (reusable per provider)

1. `providers/<id>/provider.toml` — the provider's own API/docs endpoints.
2. `packages/core/src/sync/providers/<id>.ts` (or its absence, per
   `sync.md`) — hourly first-party sync vs hand-curated entries.
3. `commits?path=providers/<id>` — who added the `$0`, when, and with
   which pricing citation. A cost citation with live-endpoint validation
   counts as proof; a policy note counts against.

## Verdicts

- **nvidia: $0 trial, proven.** Entries cite the NVIDIA API Trial Terms
  free tier and are validated via live endpoint tests (commits `5d582f6d`,
  `b6e99de4`, `d1b84739`; refs `build.nvidia.com` catalog cards).
  Safelisted in `AGGREGATOR_SAFE_PROVIDERS`.
- **kenari: paid, proven.** Commit `83040e03`: "Cost stays 0 by policy
  (IDR prepaid wallet)". The `$0` is a wallet-billing artifact, not free.
  Mapped to PAID in `CatalogMapper`.
- **General principle** (from `sync.md`): good providers never fabricate
  zeros — `requesty` omits priceless routes, `ovhcloud` omits the cost
  section for free models. An explicit `$0` is a deliberate act, except
  under a documented local policy like Kenari's.

## Follow-up

Provider billing policies drift: a `last_updated` freshness gate would
demote stale aggregator `$0` rows automatically (kenari rows were stale
months before the wallet policy surfaced). Not implemented yet.
