# SearchForge X portfolio audit — 2026-11-02

Scope: SearchForge X only. FINOS-Core belongs to the separate chat. Baseline commit:
8861c82; remote https://github.com/Ricoo05/searchforge-x (private). This audit was recorded
before changes for this mission. Existing UI/UX and search contracts will be preserved.

| Area | Classification | Evidence / correction |
|---|---|---|
| Java correctness | PASS | 29 tests, zero skips, Windows/Ubuntu run 41424775458 on 9f9ea63 |
| Frontend build | PASS | Locked React/Three/Vite/TypeScript build in both remote OS jobs |
| Distributed runtime | PASS | Separate worker/coordinator JVM tests; real Docker loss/recovery |
| Storage/crash handling | PASS within scope | StorageTest subprocess interruption/corruption cases; no power-loss claim |
| Browser behavior | PASS locally | 18 CUA checks; browser-regression.json; not yet automated in GitHub CI |
| Browser CI | FAIL / missing | Add actual Chromium tests against real coordinator and workers |
| Dependencies | PASS for npm snapshot | npm audit --json: 0 reported advisories; Maven advisory scan missing |
| Git history secrets | NOT TESTED comprehensively | Earlier current-tree pattern check only; scan reachable history |
| CI hardening | FAIL / incomplete | Mutable action tags, no job timeouts/manual trigger/report artifacts |
| Repository structure | PASS | src/, web/, scripts/, benchmarks/, evidence/; generated builds ignored |
| Security/contribution guidance | FAIL / missing | Add SECURITY.md, CONTRIBUTING.md, environment example |
| License | BLOCKED | Owner's license selection unconfirmed; do not invent a grant |
| Data licensing | PASS within scope | Original demo samples, SciFact attribution/provenance; datasets excluded |
| Clean visitor clone | BLOCKED | Private visibility prevents anonymous access; separately verify private clean clone |
| Public demo | NOT APPLICABLE for deployment now | No auth on write APIs; use verified loopback Compose and real screenshots |
| Public recruiter access | BLOCKED | New brief requires explicit public-visibility approval after review |

Observed commands: `git status --short` (clean), `git log -5 --oneline`, `git remote -v`,
`npm audit --json` in web, inspection of manifests, lockfile, Java HTTP boundary, Compose,
CI, provenance and existing documentation. There is no database or migration subsystem.
Existing source-based capabilities are substantiated by EngineTest, StorageTest,
ConcurrencyTest, QueryExecutorTest and DistributedTest, plus retained benchmark runs.

Security boundary: administrative APIs are unauthenticated and intended for a trusted local
environment. Host mappings bind 127.0.0.1; container-internal 0.0.0.0 is intentional.
No public service will be deployed with those unrestricted writes. Static assets have CSP
and nosniff headers. Query JSON, document length, body size, safe IDs, worker queues and
timeouts are bounded, but these bounds do not establish resistance to hostile public load.

Corrections are confined to repository readiness, automation and documentation unless a
new failing case demonstrates a genuine implementation defect. Follow-up reports distinguish
executed baseline checks from newly configured checks awaiting execution/publication.
