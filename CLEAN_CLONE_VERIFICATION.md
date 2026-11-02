# Anonymous clean-clone verification — PASS within stated scope

Public revision: d32c04feaa22ba1bbca4ff4e97a41c8d456fe007. Command:
`git -c credential.helper= -c http.extraHeader= clone https://github.com/Ricoo05/searchforge-x.git work/portfolio-public-clone`,
with GIT_TERMINAL_PROMPT=0. No GitHub credentials were supplied. A separate directory contains
only repository files plus generated dependencies/build outputs, no original application data.

Prerequisites: Java 21, Maven 3.9.9, Node 24, Python 3.12 and Playwright's downloaded Chromium.
`npm ci --offline --no-audit --no-fund` succeeded from a verified package cache;
`npm run build` passed. `mvn --batch-mode --no-transfer-progress clean verify` passed all
29 Java tests, zero skips. Two Python evaluator tests and all 10 Chromium tests passed
(19.9 s) from the public checkout. The built JAR started three workers/coordinator at 8690;
real ingestion, phrase retrieval and built frontend serving passed.

Evidence: evidence/portfolio/public-clean-clone.json and public-access.json; the public-clone
browser log is retained with portfolio evidence. No files were copied from working node_modules.
Maven and npm caches are ordinary dependency caches, not private configuration.

Network limitation: the initial normal npm install stalled on a registry tarball. A retry
failed honestly with ENOTCACHED. Rapier's mirror tarball was accepted only after SHA-512
matched the committed official lockfile; Three's cache entry was likewise integrity checked.
The lockfile still uses official registry URLs. This proves source reproducibility using
verified cached dependencies, not reliable registry access from every network. Baseline
remote Windows/Ubuntu/Docker builds additionally resolved dependencies on fresh runners.

The earlier private checkout first packaged before a successful frontend retry; its UI check
correctly returned 404. Repackaging after the frontend build fixed that execution-order error.
The final public clone follows frontend-build-before-Maven order and serves the actual UI.
No repository functionality was replaced or stubbed to obtain a pass. Test data is isolated;
the original user cluster at 8090 was not written by these checks.
