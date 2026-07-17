# Security Policy

This project handles ships' deck crew scheduling and logistics operating
workflows. Treat vulnerabilities as potentially high impact — including
physical-safety impact — even when the demo data is synthetic.

## Do Not Disclose Publicly

Report privately before opening public issues for:

- credential exposure
- real crew or vessel operator data exposure
- authorization bypass
- Deck Crew Governor bypass
- audit-ledger tampering
- over-disclosure in reports or exports
- unsafe robot action dispatch
- any path that lets this actor finalize a mooring/cargo-handling
  operational decision, decide a heavy-weather deck-work go/no-go, or
  override a deck officer's safety judgment

## Reporting

Use GitHub private vulnerability reporting when available for the repository.
If that is unavailable, contact the repository maintainers through the
cloud-itonami organization before publishing details.

Include:

- affected commit or version
- reproduction steps
- expected and actual behavior
- impact on crew/vessel data, policy enforcement, physical safety or audit
  logging
- suggested fix, if known

## Production Guidance

- Store secrets outside Git.
- Keep real crew/vessel operator data outside this repository.
- Run policy tests before deployment.
- Export and review audit logs regularly.
- Use least privilege for operators and service accounts.
