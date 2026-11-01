# Astra → Sol 6.1 continuation

The core has reached an initial tested handoff checkpoint. This is NOT a completed
portfolio project or a claim of production readiness. Read CHECKPOINT.md and the
storage/distributed contracts before editing. Preserve the original user brief's milestone
order and honesty requirements. Do not push or publish without user authorization.

Implemented: custom single-node search; persistent full generations and process-crash
tests; bounded query execution; real multi-process sharding/coordinator; shared-statistics
BM25; explicit partial/failed results; network fault and recovery tests; local demo.

Remaining required work:

1. Run Docker Compose in an available Docker environment. Config exists but is untested.
   Do not install a system service or claim deployment success implicitly.
2. Milestone 4: reproducible benchmark harness, original/appropriately licensed dataset,
   multiple warmup/trial runs, latency percentiles, memory/index-size evidence, compression
   versus fixed-width baseline, actual profile, measured optimization, relevance judgments
   and nDCG/recall/precision/MRR where appropriate. Keep raw results and machine settings.
3. Extend stress/fault coverage as findings justify. In particular, mid-write process kills,
   disk-full errors, recovery interruption, and multiple concurrent writers deserve coverage.
   The existing tests cover four explicit process-crash boundaries, not every crash point.
4. Milestone 5: connected React/TypeScript interface, ingestion routing/status, worker
   diagnostics, actual screenshots, complete learning and interview material.
5. Finish ALGORITHMS, BENCHMARKS, TRADEOFFS, KNOWN_LIMITATIONS, JAVA_PREREQUISITES,
   DSA_CONCEPTS, INTERVIEW_GUIDE and RESUME_EVIDENCE with actual source references/evidence.
   Assess repository quality and personal readiness separately. No invented resume metrics.

Local toolchain: workspace work/toolchains/jdk-21.0.13+9 and apache-maven-3.9.9.
Maven cache: workspace work/maven-repository. Scripts/verify-local.ps1 detects these
without changing machine-wide configuration. Scripts/demo-cluster.ps1 exercises the
packaged JAR with four separate JVMs and cleans up only its own child processes.

Known design tradeoffs: full rebuild/write on each batch; startup reanalysis for validation;
256 MiB segment limit; no globally atomic cross-shard writes; retries are caller-driven;
partial scores use available-corpus statistics; no replication, authentication or TLS.
Power loss and production scale are unverified. Local ports only by default.

Use Sol 6.1 High for continuation. Return to Astra if measurements or expanded tests reveal
a difficult correctness or recovery issue. Model choice never replaces the acceptance tests.
