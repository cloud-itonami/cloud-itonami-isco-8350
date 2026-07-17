# Contributing

`cloud-itonami-isco-8350` accepts contributions to the OSS actor, policy tests,
documentation, examples and open occupation blueprint.

## Development

```bash
clojure -M:test
```

Keep changes small and include tests for policy, audit, store or scope-
exclusion behavior.

## Rules

- Do not commit real crew, vessel or operating documents.
- Keep production writes and disclosures behind Deck Crew Governor.
- Treat this occupation's workflows as high-risk: add tests for permission,
  purpose, safety and audit logging.
- Never widen the closed op-allowlist to include an op that could finalize a
  mooring/cargo-handling operational decision, decide a heavy-weather
  deck-work go/no-go, or override a deck officer's safety judgment — those
  stay a permanent hard block.
- When editing the governor's scope-exclusion phrase list, phrase entries as
  finalization/execution ACTION phrases, never bare nouns — a bare-noun check
  self-trips on ordinary in-scope proposals (see
  `deckcrew.governor`/`deckcrew.advisor` docstrings and the
  `never-self-trips-on-default-mock-advisor-proposals` test).
- Document any new business-model or operator assumption in `docs/`.

## Pull Requests

PRs should describe:

- what behavior changed
- which policy invariant is affected
- how it was tested
- whether operator or certification docs need updates
