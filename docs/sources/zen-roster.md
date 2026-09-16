# Zen roster cross-check (`opencode` provider ghosts)

Date: 2026-09-15. The models.dev catalog lists 31 `$0` rows under the
`opencode` provider, but Zen's own roster (`GET https://opencode.ai/zen/v1/models`,
an endpoint their docs declare publicly fetchable) serves only 7 of them —
the rest are legacy entries ("retained for compatibility").

## Rule

`ModelsDevSource` marks `$0` `opencode/*` rows absent from the live roster
with confidence `TO_VERIFY` instead of dropping them: ghosts stay visible
with a neutral chip but arrive silently (no `NEW_MODEL`), and a roster
confirmation later rings `BECAME_FREE`. A dead roster fails open — a Zen
outage changes nothing. No migration: confidence is a plain string column
that already stores `TO_VERIFY`.

## Why not the name

"Free" in the model name proves nothing: 24 of the 31 `$0` rows carry it
but are not served. Roster membership is the only live signal.

## TEMPORARY status for `opencode` $0 rows (2026-09-17)

Their docs (https://opencode.ai/docs/zen/ pricing section) state every
free model is "available for a limited time", so `CatalogMapper` maps
`opencode` `$0` rows to TEMPORARY instead of LIMITED (~32 rows on
2026-09-17; dated `conditions` still win as TRIAL). Ghosts keep
TO_VERIFY and stay silent; roster-confirmed rows keep ringing.
Pill tone is WARNING either way — labeling honesty, not a bell change.
