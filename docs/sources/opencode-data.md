# Source: opencode.ai/data — REJECTED (§64)

Date: 2026-09-15. Status: rejected, do not implement. Revisit only if
opencode.ai publishes an explicit API or data-use permission.

## What was evaluated

- Index: `https://opencode.ai/data` (usage rankings, session/token cost,
  cache ratio, market share, geo).
- Per-model pages: `https://opencode.ai/data/<org>/<model>` (e.g.
  `nvidia/nemotron-3-ultra` shows Total spend `$0.00`, Cost input/output `-`,
  rank #07, 2.3% share — observed free-usage evidence, stronger than a
  declared `cost: 0`).
- Machine-readable with plain HTTP (server-rendered `model-metric`
  articles). No key needed.

## Why rejected (§64b)

The Terms of Use (effective Aug 15, 2026) prohibit exactly this integration:

- (5) `automatically or programmatically extracts data or Output`;
- (11) `"crawls," "scrapes," or "spiders" any page, data, or portion of or
  relating to the Services or Content (through use of manual or automated
  means)`;
- (12) `copies or stores any significant portion of the Content`.

A daily automated fetch + Room storage of page data violates all three. The
rule would also cover any undocumented JSON endpoint on the same host.

## Consequences

- No `006-opencode-data` implementation (no fetcher, no pins, no hook).
- The findings stay usable for manual reasoning (e.g. choosing pinned
  overlap models, interpreting conflicts) — human browsing is unaffected.
- Revisit if: an official API appears, or written permission is obtained.
  Log the change here first.
