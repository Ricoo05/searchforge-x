# CI validation — executed results

Workflow: Java correctness. Triggers: push to main, pull requests and manual dispatch.
Verified source/test/workflow commit: b6f22eca664feab19237e5b2951a0c7ae4b04c82.
Run: https://github.com/Ricoo05/searchforge-x/actions/runs/41431024879.

| Job | Actual result |
|---|---|
| test (windows-latest) | PASS: frontend build, 30 Java tests, two Python tests, zero skips |
| test (ubuntu-latest) | PASS: frontend build, 30 Java tests, two Python tests, zero skips |
| containers | PASS: image build/tests, health, real ingestion/query, worker loss/recovery, 10 Chromium tests |
| security | PASS within scope: history credential patterns, npm audit, Maven OSV, Python compilation |

Evidence: evidence/portfolio/github-actions-41431024879.json contains real job/step statuses
and log excerpts. CI retained Surefire reports and browser reports/failure traces. New tests
do not intercept responses. The container probe observed COMPLETE → PARTIAL → COMPLETE.

Actions are pinned to verified immutable official commits. Permissions are contents:read;
normal PR triggers do not receive deployment secrets. Jobs have 15–25 minute timeouts.
Surefire forks have a 180-second bound and Docker prints fork dumps on failure. Concurrency
supersedes outdated runs for the same ref. No critical job uses continue-on-error. Dependabot
proposes updates; no upgrades are automatically merged. Branch requirements and review
protection are described in REPOSITORY_PROTECTION.md.

Historical evidence is retained: initial Windows log-handle failure, its passing repair,
and stalled/cancelled run 41429105232. The latter is not reported passing. It stalled during
DistributedTest; exact stack/root cause was unavailable. HTTP-client shutdown was made
bounded and a real stalled-body cancellation test added. That test also passes with the old
close implementation on this desktop; a deterministic reproduction of the remote stall
is not claimed. The new complete Docker run establishes the observed successful outcome.

Local validation also passed: 30 Java tests, two metric tests, 10 Chromium tests; final
anonymous public checkout passed all 30 Java tests. PyYAML parsing verified workflow syntax,
SHA refs and job timeouts. The new CI itself has now been observed remotely.

Warnings/limits: lazy Three.js chunk 587.18 kB (146.19 kB gzip), local color-environment
warning, finite test scenarios. The historical run above predates the expanded quality gates. Spotless, Prettier, Ruff,
CodeQL and runtime container CVE scanning are now configured; see QUALITY_CHECKS.md
and QUALITY_VALIDATION.md for their scope and actual outcomes. No security certification.

Subsequent evidence/ownership documentation does not alter this tested application. The
live main badge/checks identify later verification runs; no static passing badge is used.
