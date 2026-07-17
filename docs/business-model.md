# Business Model: Deck Crew Scheduling and Logistics Coordination

## Classification

- Repository: `cloud-itonami-isco-8350`
- ISCO-08: `8350`
- Occupation: Ships' Deck Crews and Related Workers
- Social impact: maritime-safety, crew-welfare, logistics-transparency

## Customer

- vessel operators
- shipping/crewing agencies
- independent deck crews

## Offer

- watch/duty scheduling coordination
- service-record (deck-work/watch-log) logging
- safety-concern escalation (equipment defect, weather hazard, crew fatigue)
- deck-equipment supply-order coordination (mooring lines, safety gear)

## Revenue

- monthly retainer
- per-vessel scheduling fee

## Trust Controls

- crew-member and vessel provenance verified before any action
- no ledger record without an independently registered crew member and
  vessel
- safety concerns always escalate to human sign-off regardless of confidence
- supply orders above the registered cost threshold always escalate to human
  sign-off
- this actor coordinates crew scheduling and logistics ONLY — it never
  finalizes a mooring/cargo-handling operational decision, decides a
  heavy-weather deck-work go/no-go, or overrides a deck officer's safety
  judgment; those are a permanent, non-overridable hard block enforced by a
  closed op-allowlist and an independent scope-exclusion check
- scheduling and logistics records are auditable, not editable
