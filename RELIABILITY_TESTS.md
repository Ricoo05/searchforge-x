# Executed reliability evidence

Remote follow-up: the first GitHub run passed Linux and Docker startup/worker-loss/recovery.
Windows found locked temporary child log files during JUnit cleanup. DistributedTest now
drains child pipes concurrently, joins readers and writes logs from the parent after exit;
close is idempotent. The 29-test suite passes locally after the fix. The original remote
failure is retained in evidence/ci/windows-cleanup-failure.txt; see CHECKPOINT.md for rerun status.

Windows 11, Temurin 21.0.13+9, Maven 3.9.9; 12 logical processors, about 16 GiB RAM.
Latest executed clean Java suite: 29 passed, zero failures/errors/skips. Later packaging
of unchanged Java with updated UI assets reused those tests using -DskipTests; that
packaging pass is not counted as a new test run. Python evaluator tests: 2 passed.

- EngineTest (9): Unicode/malformed input, Boolean precedence, phrase repetition/gaps,
  hand-calculated BM25, atomic batch rejection, updates/deletes, immutable readers,
  deterministic pages, duplicate detection, validation and 60 seeded scan-oracle corpora.
- ConcurrencyTest (2): four readers perform 4,000 total generation-aware checks during
  100 publications; four concurrent writers publish 200 accepted updates without loss.
- QueryExecutorTest (3): overload, reclaimed canceled admission, deadline interruption,
  worker reuse and cancellation on close. Shutdown regression repeats 100 times.
- StorageTest (9): metadata/posting persistence, updates/deletion, subprocess termination
  during segment/manifest writes, after force and publication, during recovery and cleanup,
  corruption rejection, uncertain commits, exclusive ownership, analyzer mismatch, codec
  boundaries/truncation and injected write failure. An injected IOException is not actual
  disk exhaustion. Termination scenarios are finite controlled boundaries.
- DistributedTest (5): real coordinator/worker JVMs compare scores/IDs for six query forms;
  duplicate ingestion, coordinator routing, diagnostics, worker loss/restart, partial writes,
  stale-generation rejection, malformed responses, deadlines, interruption and input rejection.
- BenchmarkCodecTest (1): fixed-width and compressed reconstruction agree on boundary IDs.

The original shutdown-race failure is retained as evidence/shutdown-race-failure.txt.
The production fix clears queued work before interrupting active work; assertions were
preserved. The latest successful verification is evidence/verification-windows-latest.txt.

Frontend: TypeScript checking and Vite production build passed. Browser checks against
real processes verified original sample ingestion, phrase results, document creation,
title display, shard selection/pause, health refresh and PARTIAL status after actual worker
termination. Saved screenshots include ui-3d-search.jpg and ui-3d-partial.jpg.

Benchmarks: repeated independent JVM trials and a separate distributed equivalence workload.
Relevance: SciFact run and independent Lucene reference; metric formulas have two Python
tests including a hand-derived two-query example. See BENCHMARKS.md for exact experiment scope.

Unexecuted: Docker Compose, remote GitHub Actions, real full-disk conditions, power loss,
kernel crashes, network filesystems, production load and user-independent competency tests.
No general crash-safety or production-readiness claim follows from the passed scenarios.
