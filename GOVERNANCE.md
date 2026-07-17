# Governance

`cloud-itonami-isco-8350` is an OSS open-occupation blueprint. Governance covers
both code and the operator model.

## Maintainers

Maintainers may merge changes that preserve these invariants:

- the Advisor cannot directly dispatch robot actions or perform a deck
  operation.
- Deck Crew Governor remains independent of the advisor.
- hard policy violations cannot be overridden by human approval.
- the closed op-allowlist never grows to include an op that could finalize a
  mooring/cargo-handling operational decision, decide a heavy-weather
  deck-work go/no-go, or override a deck officer's safety judgment.
- every commit, hold and approval path is auditable.
- real crew/vessel operator data stays outside Git.

## Decision Records

Architecture decisions live in `docs/adr/`. Changes to the trust model,
storage contract, public business model, operator certification or license
should add or update an ADR.

## Operator Governance

Anyone may fork and operate independently. itonami.cloud certification is a
separate trust mark and should require security, audit, support and
data-flow review.

Certified operators can lose certification for:

- bypassing policy checks
- widening the op-allowlist to include a mooring/cargo-handling finalization,
  heavy-weather go/no-go, or deck-officer-override op
- mishandling crew/vessel operator data
- misrepresenting certification status
- failing to respond to security incidents
- hiding material changes to customer-facing operation
