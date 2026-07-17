# Operator Guide

## First Deployment

1. Define the operator's vessel roster and crew-registration process.
2. Define crew-member and vessel verification/registration process.
3. Run synthetic scheduling and logistics operating cases.
4. Enable human-reviewed sign-off for `:high`/`:safety-critical` actions
   (safety-concern flags, over-threshold supply orders).
5. Measure operating outcomes and audit coverage.

## Minimum Production Controls

- crew-member and vessel provenance log
- safety-critical escalation path (equipment defect, weather hazard, crew
  fatigue)
- provenance for all operating records
- human review for high-risk cases (safety concerns, over-threshold supply
  orders)
- audit export for all gated actions
- confirmation that this actor never proposes to finalize a
  mooring/cargo-handling operational decision, decide a heavy-weather
  deck-work go/no-go, or override a deck officer's safety judgment — those
  are outside this actor's closed op-allowlist entirely, by design

## Certification

Certified operators must prove that the governor gates every safety-critical
robot action, that safety-critical risks escalate to humans, and that no
deployment configuration can add an op to the allowlist that would let this
actor finalize a mooring/cargo-handling decision or a heavy-weather deck-work
go/no-go, or override a deck officer's safety judgment.
