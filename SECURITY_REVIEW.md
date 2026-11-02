# Source-publication security review

PASS within stated scan scope: 208 reachable Git blobs plus 170 current files scanned
before the final report additions; no configured credential-pattern matches. npm audit
after adding Playwright reports zero advisories. OSV querybatch returned zero advisories
for all 11 resolved Maven dependencies, including test dependencies. Raw package coordinates
and results are in evidence/portfolio. A final scan is run before committing/publication.
These are time-specific advisory/pattern observations, not proof that vulnerabilities or
unknown secrets do not exist. Scanner/network errors cause failure.

Reviewed: Git history, ignored outputs, manifests, dataset provenance, JsonServer request
limits/CSP, Compose host binding, worker endpoints, CI triggers and permissions. No database,
cloud credential, real financial record or payment connection belongs to this repository.
Generated datasets, environment files, node_modules, browser reports and JARs are ignored.
Evidence logs intentionally remain because they substantiate failures and tests.

Risks: unauthenticated write APIs, trusted admin paths/endpoints, no TLS, no tenant isolation,
full-generation write amplification and finite queue/body bounds. Keep all host ports on
loopback. Do not publicly deploy this unrestricted application. CodeQL/image CVE scanning
are not executed; paid services are not enabled. Actions are pinned and dependency-update
automation is prepared, but the new remote security job is not yet observed.

Licensing: original demo text is AI-assisted project material; SciFact/BEIR permissions and
attribution are recorded and dataset files are excluded. THIRD_PARTY_NOTICES.md identifies
runtime/dev license families; it is not an exhaustive executable redistribution audit.
No root LICENSE is added: owner license choice/ownership confirmation remains required.
SECURITY.md provides an honest disclosure path without inventing a private contact address.
