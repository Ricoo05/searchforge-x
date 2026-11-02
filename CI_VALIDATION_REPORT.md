# CI validation

Baseline remote workflow: **Java correctness**, push/PR triggers. Run
https://github.com/Ricoo05/searchforge-x/actions/runs/41424775458 tested
9f9ea636ed3cc2d4ebdb733ccdd86a1fac544581. Windows and Ubuntu each passed 29 Java tests
and two Python tests, zero skipped. Containers passed build, health, seeded retrieval,
worker-loss PARTIAL results and persistent recovery. Detailed steps/log excerpts:
evidence/ci/github-actions-41424775458.json. The initial failed Windows run and its repair
are retained; no tests were disabled. The subsequent baseline commit changed evidence only.

Prepared changes: verified official SHA-pinned checkout v6, Java v5, Node v6, Python v6,
artifact upload v5; push-to-main/PR/manual triggers; 15â€“25 minute job bounds; full-history
credential patterns, npm audit and resolved Maven OSV checks; Python compilation; browser
tests inside the real Compose deployment; failure traces and Surefire artifacts.
CI has read-only contents permission and no privileged PR trigger. Dependabot covers npm,
Maven and actions monthly. No critical job uses continue-on-error or suppresses exit codes.

Local new validation: workflow YAML parses with PyYAML 6.0.3, all action refs are 40-digit
SHAs fetched from their official repositories, all jobs have timeouts. TypeScript/Vite
build passes. Playwright discovers and executes 10 tests: 10 pass, zero skipped, 38.8 s.
Both dependency scans and the reachable-history pattern scan pass. git diff --check passes.

**Updated remote workflow:** https://github.com/Ricoo05/searchforge-x/actions/runs/41429105232
on 48563c103d4df2767720dc44995df0f39bad1382. Windows, Ubuntu and security have passed;
containers/browser checks are running. No final success is claimed until completion.
Main branch protection now requires the two matrix jobs, containers and security with strict
status checks. Administrators retain bypass; force pushes and branch deletion are disabled.

Warnings: Vite retains a 587.18 kB lazy Three.js chunk warning. Local Playwright emitted a
NO_COLOR/FORCE_COLOR environment warning. Neither warning is suppressed. Full SAST, image
CVE scanning and a repository-wide standard formatter remain unconfigured; compiler checks,
.editorconfig and whitespace validation are narrower checks.
