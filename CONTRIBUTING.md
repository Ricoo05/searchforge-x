# Contributing

Read ARCHITECTURE.md, KNOWN_LIMITATIONS.md and SECURITY.md before proposing changes.
Use Java 21, Maven 3.9.x, Node 24/npm, Python 3.12+, and Docker Compose for container checks.
Run `npm ci --prefix web`, `npm run build --prefix web`, `mvn --batch-mode clean verify`,
and `python -m unittest discover -s scripts -p 'test_*.py'` from the repository root.
Run the formatter and lint checks in QUALITY_CHECKS.md before committing.

For a disposable container test: `docker compose up --build -d --wait`, then
`python scripts/container_smoke.py --seed`. Stop worker1 and run the probe with
`--missing-shard 1`; start it and run the normal probe to verify recovery. These probes
write IDs 1–3, so use fresh disposable volumes, never a personal data directory.
Use `docker compose down` to stop; add `--volumes` only to discard that disposable test data.

Browser CI tests use real services: from web run `npx playwright install chromium firefox webkit` and
`npx playwright test` while the disposable cluster is running, explicitly setting
SFX_BROWSER_URL to its loopback URL (PowerShell: `$env:SFX_BROWSER_URL='http://127.0.0.1:8090'`).
The tests refuse implicit local or external targets. They write IDs
10000–10024, 11000 and 9100. Run them only against an isolated cluster. Browser reports and failure
traces are generated under web/playwright-report and web/test-results and are ignored.

Run `npm audit --prefix web --audit-level=low`. For Java dependencies:
`mvn org.apache.maven.plugins:maven-dependency-plugin:3.8.1:list "-DoutputFile=target/dependency-audit.txt"`,
then `python scripts/portfolio_security.py dependencies`. Run the history scan with
`python scripts/portfolio_security.py secrets`. Network/database failures are not clean scans.

Keep edits focused. Preserve deterministic rankings, immutable publication, failure labels,
and current UI behavior. Add meaningful regression cases for defects. Explain the trigger,
behavioral change, validation and limitations in a PR. Never weaken assertions or add
continue-on-error to make CI green. Do not commit credentials, datasets, build directories
or artificial activity. Document sources of third-party material.

Original project source uses the MIT license in LICENSE. Third-party components and dataset
material retain their upstream terms; consult THIRD_PARTY_NOTICES.md and DATA_PROVENANCE.md.
