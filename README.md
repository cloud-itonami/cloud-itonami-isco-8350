# cloud-itonami-isco-8350

Open Occupation Blueprint for **ISCO-08 8350**: Ships' Deck Crews and Related Workers.

This repository designs a forkable OSS business for deck-crew scheduling and
logistics coordination: a crew-scheduling and equipment-logistics robot
manages watch/duty schedules, service-record logging and supply coordination
under a governor-gated actor, so a vessel operator keeps its own crew
scheduling and audit records instead of renting a closed maritime crewing
SaaS.

**Maturity: `:implemented`.** `src/deckcrew/` implements the `DeckCrewActor`
as a `langgraph.graph/state-graph` (`deckcrew.actor`) wired to a
`Deck Crew Advisor` (`deckcrew.advisor`) and an independent `DeckCrewGovernor`
(`deckcrew.governor`), following the itonami actor pattern
(ADR-2607121000): `:intake -> :advise -> :govern -> :decide -+-> :commit
(:ok?) +-> :request-approval (:escalate?, human-in-the-loop interrupt)
+-> :hold (:hard?)`. 20 tests / 45 assertions green (`kbb -M:test`).

HARD invariants (always hold, never overridable): crew-member and vessel
provenance (both must be independently verified/registered before any
action), no-actuation (`:effect` must be `:propose`), the crew member's
registered vessel assignment must match the vessel named in the proposal, a
closed op-allowlist (`:log-service-record`, `:schedule-crew-operation`,
`:flag-safety-concern`, `:coordinate-supply-order` — no other op is ever
accepted), and a scope-exclusion check that permanently blocks any proposal
whose free text describes directly finalizing a mooring/cargo-handling
operational decision, commencing/authorizing deck work in heavy weather, or
overriding a deck officer's safety judgment. Always-escalate (human sign-off
regardless of confidence, mapping this repo's Trust Controls in
[`docs/business-model.md`](docs/business-model.md)): `:flag-safety-concern`
(always) and `:coordinate-supply-order` above the registered cost threshold.

## Robotics premise

All cloud-itonami verticals are designed on the premise that a **robot performs
the physical domain work**. Here a crew-scheduling and equipment-logistics
robot performs watch/duty scheduling, service-record logging and supply-order
coordination under an actor that proposes actions and an independent **Deck
Crew Governor** that gates them. The governor never dispatches hardware
itself and never performs a deck operation; `:high`/`:safety-critical`
actions (a flagged safety concern, or a supply order above the registered
cost threshold) require human sign-off.

**This actor coordinates crew scheduling and logistics ONLY.** It never
performs an on-deck vessel operation itself. Mooring, cargo securing and
general seamanship — and any go/no-go decision to carry out deck work in
heavy weather — stay with the human deck crew and the deck officer's own
judgment. The closed op-allowlist and the governor's scope-exclusion check
make it structurally impossible for this actor to finalize a
mooring/cargo-handling operational decision, decide a heavy-weather
go/no-go, or override a deck officer's safety judgment — those are always a
hard, permanent block, never an op this actor can propose or auto-commit.

## Core Contract

```text
crew roster + vessel assignment + watch/duty request + supply request
        |
        v
Deck Crew Advisor -> Deck Crew Governor -> log/schedule/coordinate, or human sign-off
        |
        v
robot actions (gated) + operating records + audit ledger
```

No automated advice can dispatch a robot action the governor refuses,
finalize a mooring/cargo-handling operational decision, decide a
heavy-weather go/no-go, override a deck officer's safety judgment, or
suppress an operating record without governor approval and audit evidence.

## Capability layer

Resolves via [`kotoba-lang/occupation`](https://github.com/kotoba-lang/occupation)
(ISCO-08 `8350`). Required capabilities:

- :robotics
- :identity
- :audit-ledger

See [`docs/business-model.md`](docs/business-model.md) and
[`docs/operator-guide.md`](docs/operator-guide.md).

## License

AGPL-3.0-or-later.
