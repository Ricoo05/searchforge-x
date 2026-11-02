# Known limitations

The project is an educational engineering portfolio implementation with AI assistance.
It has not undergone production validation. Tests demonstrate finite scenarios, not an
absence of bugs. Passing the repository tests does not establish personal interview readiness.

Storage: full rebuild/write on every batch, 256 MiB segment cap, in-memory postings,
multiple decode copies, startup reanalysis, no WAL, no incremental multi-segment merging.
Compaction consolidates by retaining the newest already-full generation. There is no
power-loss, kernel-crash, network-filesystem or real disk-exhaustion guarantee.

Distribution: static modulo sharding, no replication/rebalancing/failover, no cross-shard
atomic writes, no exactly-once delivery, no global snapshot transaction. A write without
an acknowledgment is UNKNOWN and may have committed. Concurrent generation changes can
fail a query. HTTP 200 can contain PARTIAL/FAILED; clients must inspect application status.

Queries: no NOT, escaping, prefix, wildcard, field queries, spelling correction, stemming
or semantic retrieval. Stop-word-filtered phrases only enforce retained terms and gaps.
Local page window is capped at 10,000; the distributed page window is capped at 1,000.
Page requests can skip/repeat results under ingestion. Returned distributed snippets
are a leading 512-code-point excerpt, not query-centered highlighted passages.

Operations: no TLS/authentication; host loopback only by default. Docker is absent locally;
remote Linux and Compose startup/worker-loss/recovery have passed. Consult CHECKPOINT.md
for the Windows follow-up and exact tested commit. No application hosting is deployed.
The caller must not expose worker/admin ports publicly. Single writer ownership relies on
local filesystem locks. Administrator paths and configured worker endpoints are trusted.

Evidence: synthetic workloads do not establish production scale or relevance. Desktop
background activity is not fully controlled. Latency samples are small for p99 inference;
reported tails are observations. Heap-used observations are not peak process RSS. Relevance
is evaluated on one small scientific dataset, with binary judgments and no parameter tuning.
Lucene uses quantized norms and float scoring, so rank differences must be investigated
without assuming either implementation is universally superior.
