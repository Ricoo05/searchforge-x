# Engineering checkpoint — 2026-11-02 IST

Milestones 1–3: custom search, persistent full generations and real three-worker search
are implemented and locally tested. The final core test suite has 29 methods passing,
zero failures/errors/skips. Some methods run many scenarios, not just one assertion.

Milestone 4: completed local experiments at 10,000/100,000 synthetic documents with three
independent JVM trials per variant/size, warmups, raw latency samples, heap-used observations,
storage/compression timing, JFR profiles and a measured scoring optimization. A separate
10,000-document distributed benchmark validates every result against single-node IDs/scores.
SciFact relevance uses all 300 BEIR test queries and an independent Lucene reference.
Python metric tests: 2 passed. Detailed numbers and limits are in BENCHMARKS.md.

Milestone 5: the connected React/TypeScript interface now uses the requested paper/charcoal/
copper design system, compact search explorer, document studio and interactive Three.js
diagnostics. The final browser regression passed 18 checks including 12 responsive screens
at 1440/1280/1024/768/390/320 pixels. Final axe observations: zero violations and incomplete
rules. Real isolated process failures and delayed actual requests cover partial/failed/
uncertain, empty, connecting and pending states. The original seven-document production
corpus, generations and all six baseline query results are unchanged. All 29 protected
backend/API source hashes match. See UI_UX_TRANSFORMATION_REPORT.md for evidence and limits.

Failure found and fixed: executor shutdown could allow queued work to run after the active
task was canceled. The queue is detached before cancellation; the strict regression now
repeats 100 times. The original failed verification log remains in evidence/.

Open environment verification: Docker Compose cannot run locally because Docker and WSL are
absent. The private repository is https://github.com/Ricoo05/searchforge-x. The first remote
run (41424165028, commit 00debad) passed Linux correctness and Docker Compose startup,
ingestion/search, worker-loss partial results and persistent recovery. Windows found a test
cleanup issue with child-inherited temporary log handles. The harness now drains process
pipes and writes parent-owned logs after joining readers; close is idempotent. The original
failure excerpt is retained. The follow-up run 41424775458 on commit 9f9ea63 passed all three
jobs: Windows, Ubuntu and containers. Both OS jobs passed 29 Java tests and two Python tests;
the container job verified COMPLETE, PARTIAL after worker loss, then COMPLETE after recovery.
Full job/step status and log excerpts are retained in
evidence/ci/github-actions-41424775458.json. Real disk-full conditions and power-loss safety
remain unverified.

Important decisions: full immutable generation publication without a WAL; checksums plus
startup semantic verification; shared-statistics two-phase distributed scoring; explicit
partial/failed/unknown outcomes; local-only admin API; code-native 3D as a functional view
of actual health, without inventing metric streams.

Known limits: full rebuild/write on batch, boxed in-memory postings, 256 MiB segment cap,
static sharding, no replicas/global ingestion transaction/authentication/TLS, small experiment
scope. These prevent claims of production readiness, high availability or broad scalability.

The implementation, local verification, requested GitHub upload and remote CI verification
are complete. The evidence-only follow-up commit does not change the tested application or
test sources. Personal interview readiness is not assessed from repository output;
independent learning exercises remain the user's separate objective.

UI-specific unforced checks: actual deadline expiry, WebGL context loss and OS-level reduced
motion preference; formal screen-reader/cross-browser use. Implementations are preserved
and reviewed, but these checks are not represented as executed.


## Portfolio follow-up: bounded cancellation shutdown

Run 41429105232 stalled inside the Docker build's DistributedTest after an IOException log;
it was cancelled and is not a passing run. A stack dump was unavailable, so a causal link
between that stall and HTTP-client shutdown is not established. The coordinator's unbounded
HttpClient.close() was replaced by shutdownNow plus a five-second termination bound.
A synchronized stalled-response-body test exercises real socket cancellation; all 30 Java
tests pass locally, zero skipped. An isolated run of this new test also passes with the old
close implementation on this desktop, so it is not claimed to reproduce the remote hang.
Surefire forks now have a 180-second timeout and Docker prints fork dumps on failure.

Current verification source: b6f22ec, workflow run
https://github.com/Ricoo05/searchforge-x/actions/runs/41431024879. Its final outcome must be
observed rather than inferred from local passes. Earlier unreviewed Dependabot PR runs were
cancelled to prioritize main; no upgrades were merged and cancelled runs are not green checks.
Main requires Windows, Ubuntu, containers and security statuses; administrator bypass remains
enabled. Original source is MIT-licensed; public anonymous access and clean-clone verification
are recorded in evidence/portfolio. GitHub secret scanning and push protection are enabled.


Final observed portfolio run: 41431024879 on b6f22ec passed all four required jobs.
Windows/Ubuntu each passed 30 Java and two Python tests, zero skipped; Docker passed
real worker loss/recovery and all 10 Chromium checks. Public anonymous access, MIT licensing,
dependency scans and owner-only write access/review protection were verified. See
FINAL_GITHUB_STATUS.md and evidence/portfolio/github-actions-41431024879.json.
