# Security policy

SearchForge X is a portfolio search implementation for trusted local environments. It is
not a hosted multi-tenant service. Administrative ingestion and worker APIs have no
authentication or TLS. Keep host ports bound to 127.0.0.1; do not expose them to visitors.
Do not index confidential data for a public demonstration. The supplied corpus is synthetic.

Report suspected vulnerabilities through GitHub private vulnerability reporting if it is
available on this repository. If that channel is unavailable, use a non-sensitive issue
asking the maintainer to arrange a private channel; do not put exploit payloads, secrets or
private indexed documents into public issues. No response SLA or supported-version promise
is made. Updates target the default branch; assess compatibility before upgrading.

Checks: npm audit (production and development dependency graph), resolved Maven coordinates
queried against OSV, and credential-pattern scans of reachable Git history/current files.
Scanner unavailability fails CI rather than producing a clean result. Patterns are bounded
and may miss unfamiliar secrets. Advisory databases may lag vulnerabilities. These checks
are not an independent security certification, SAST coverage or proof of zero vulnerabilities.

Actions use immutable commits and read-only contents permissions, with normal PR triggers.
Untrusted PR code is not given deployment secrets. No public hosting is configured.
GitHub secret scanning and secret push protection are enabled for this public repository.
CodeQL and Trivy image scanning are configured on pushes, PRs and weekly schedules.
See QUALITY_CHECKS.md for scope and QUALITY_VALIDATION.md for executed results.
No paid security service is enabled.
