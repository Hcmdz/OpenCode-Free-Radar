# Source: NVIDIA Build catalog — REJECTED (§64)

Date: 2026-09-15 (implemented as S2, then removed the same day).
Status: rejected, do not re-add without written permission. Revisit only if
NVIDIA publishes an explicit API or data-use permission for automated reads.

## What was built (removed)

- `data/source/nvidia/` (catalog + `.md` card parser, LIMITED mapper),
  multi-source loop, pinned S1↔S2 cross-check. Removed in full; no code
  remains. S1 behavior untouched.

## Why rejected (§64b)

The NVIDIA Website Terms of Service, clause (f), prohibit "robot, spider,
scraper, crawler, data mining/gathering/extraction tools, or any other
automatic device" to access or acquire any portion of the site — which
describes the daily automated card fetch verbatim. Intent targets large-scale
abuse and our use was minimal, personal and sequential, but the project rule
requires the ToS to *allow* automated reads, and it does not.

## Consequences

- OfferSource back to S1 only; `refreshAll`/PARTIAL/mutex/registry kept
  (generic, tested, reused by future sources).
- Devices that synced S2 keep stale `nvidia-build` rows: clear app storage
  on dev devices (personal/debug stage only — no purge code shipped).
