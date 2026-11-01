# Executed reliability evidence

Local environment: Windows, Temurin 21.0.13+9, Maven 3.9.9, 12 logical processors,
approximately 16 GiB physical RAM. Dependencies and toolchains are local to the workspace.
Run `mvn clean verify` from the repository with Java 21 configured.

Latest executed suite: 26 tests, zero failures, zero errors, zero skipped (2026-11-02 IST).
This count includes methods containing repeated scenarios; it is not a count of independent
statistical trials or proof that all possible failures have been covered.

- EngineTest (9): Unicode normalization, malformed UTF-16, phrase gaps/repetition,
  Boolean precedence, syntax/limits, hand-calculated BM25, atomic batch rejection,
  updates/deletion, immutable readers, deterministic pagination, exact duplicate
  detection, metadata validation, and 60 seeded random corpora with an independent oracle.
- ConcurrencyTest (1): 4 readers perform 1,000 checks each while a writer publishes
  100 two-document generations. Returned generation determines expected membership.
- QueryExecutorTest (3): bounded queue rejection, cancellation frees admission,
  timed-out work is interrupted, worker reuse, and shutdown cancels pending work.
- StorageTest (8): persistent metadata and phrase results, updates/deletions, four
  actual JVM termination boundaries including cleanup, committed corruption rejection,
  ambiguous commit recovery, exclusive ownership, analyzer mismatch, deterministic
  segment encoding, truncation rejection and varint boundary/random round trips.
- DistributedTest (5): four real JVMs match single-node IDs/scores for six query forms,
  duplicate ingestion, worker loss/restart, stale-generation rejection, invalid network
  responses, slow workers, interrupted fanout, and malformed JSON/input rejection.

The packaged-JAR demo also passed with four independent processes and the expected
two phrase matches. All task-owned demo/test JVMs were stopped afterward.

GitHub Actions configuration exists for Windows and Linux. Remote CI has not run;
local Windows results do not establish Linux filesystem behavior.

Unexecuted: Docker Compose, power-loss scenarios, full-disk faults, performance regression,
relevance evaluation and frontend end-to-end tests. There are no benchmark claims yet.
