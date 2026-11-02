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
absent. GitHub upload was requested; remote verification is pending the first push. CI now
includes Windows/Linux correctness and actual Compose startup/worker-loss/recovery checks.
Container/Linux filesystem behavior, real disk-full
conditions and power-loss safety remain unverified. No hidden failing test is ignored.

Important decisions: full immutable generation publication without a WAL; checksums plus
startup semantic verification; shared-statistics two-phase distributed scoring; explicit
partial/failed/unknown outcomes; local-only admin API; code-native 3D as a functional view
of actual health, without inventing metric streams.

Known limits: full rebuild/write on batch, boxed in-memory postings, 256 MiB segment cap,
static sharding, no replicas/global ingestion transaction/authentication/TLS, small experiment
scope. These prevent claims of production readiness, high availability or broad scalability.

The remaining completion gate is an actual Docker/remote-CI run in a suitable environment,
followed by resolving any findings. Personal interview readiness is not assessed from
repository output; independent learning exercises remain the user's separate objective.

UI-specific unforced checks: actual deadline expiry, WebGL context loss and OS-level reduced
motion preference; formal screen-reader/cross-browser use. Implementations are preserved
and reviewed, but these checks are not represented as executed.
