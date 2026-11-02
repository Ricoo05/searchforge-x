# Distributed retrieval v1

Three independent WorkerMain JVM processes own separate durable directories. IDs map
to shards by `id % shardCount`; workers reject incorrectly routed ingestion. Assignment
is static; changing shard counts requires explicit reindexing. CoordinatorMain is a
fourth independent JVM. The protocol is bounded JSON over HTTP using Java 21 HttpServer
and HttpClient. Jackson handles serialization only; indexing and retrieval remain custom.
This avoids introducing a framework or generated RPC layer for the initial four-process
deployment. Services bind to 127.0.0.1 by default.

## Query consistency and ranking

1. Parse/validate the query and fan out statistics requests concurrently.
2. Each worker captures an immutable snapshot and returns its generation, process UUID,
   document count, total retained length and document frequencies for query terms.
3. Sum compatible statistics. Every participating worker receives the same values.
4. Each worker verifies its UUID/generation still match. It captures that snapshot and
   computes local top-(offset+limit) with the shared BM25 statistics.
5. Coordinator validates shard identity, generation, result count, finite scores,
   sorted order, ID assignment and uniqueness, then merges and slices the global page.

This uses a vector of shard generations, not a globally atomic ingestion transaction.
Single-node equivalence is tested on an equivalent static corpus. Concurrent ingestion
between phases can fail a request; there is no automatic retry or cross-request snapshot
pinning. A captured in-memory snapshot remains safe if indexing occurs after capture.

## Status contract

- COMPLETE: every configured shard contributed both compatible statistics and results.
- PARTIAL: some shards failed the statistics phase; all remaining shards completed scoring.
  `scoringScope=available_shards`; ranking and totalHits apply only to that available corpus.
- FAILED: no usable statistics, the deadline expired, or a scoring-phase shard failed or
  changed generation. Returns no hits. No incompatible result sets are merged.

`missingShards` identifies observed unavailable/invalid contributors. A FAILED attempt's
missing list need not include tasks canceled before their outcome was known. Generations
describe only successful results. HTTP 200 can carry PARTIAL or FAILED, so consumers must
inspect status. Admission overload is HTTP 503; malformed input is HTTP 400. Health means
process readiness; coordinator health does not imply every worker is available.

Deadlines include queuing. Approximately half the budget is reserved for statistics and
half for scoring. A coordinator permits eight concurrent queries. Fanout workers, queues,
HTTP workers, connection limits and response sizes are bounded. Query cancellation is
cooperative. There is no replication or automatic failover: worker recovery means restarting
the process against its existing storage and rerunning the query. No high-availability claim.

Duplicate ingestion replaces the same IDs, preserving searchable state; generations may
advance. This is not exactly-once delivery. Partial multi-shard ingestion is not transactional.
The coordinator routes ingestion batches to their modulo-assigned workers and reports
per-shard ACKNOWLEDGED or UNKNOWN outcomes. A missing acknowledgment is not proof that
the write was absent. Ambiguous duplicate/delete-upsert IDs are rejected before dispatch.

## Endpoints

All endpoints use POST and JSON. Workers: /stats, /search, /ingest, /health.
Coordinator: /search, /ingest, /diagnostics and /health; GET / serves the built React app.
All JSON endpoints require application/json; no cross-origin browser access is enabled.
Public query fields: query, offset, limit, timeoutMillis.
Require offset+limit <=1000 and timeoutMillis 1..30000. Worker ingress batches have at most
1000 operations and a 4 MiB body limit. Returned text is the first 512 Unicode code points;
only title metadata is included in distributed snippets (bounded to 128 code points).

No authentication or TLS is implemented. This is a trusted local development deployment.
Compose publishes ports only on host loopback; internal containers use a private bridge.
Do not expose administrative endpoints publicly. Services have no destructive HTTP endpoint.

## Run

Full build: `npm ci --prefix web`, `npm run build --prefix web`, then `mvn clean verify`.
Windows workspace shortcut: `./scripts/build.ps1`. Java-only verification is
`./scripts/verify-local.ps1`. To launch the full local app: `./scripts/run-local.ps1`.
Its foreground supervisor keeps the coordinator/remaining workers running if one worker
exits, so PARTIAL behavior remains demonstrable; restart the cluster to recover that worker.
Run the automated four-process demonstration: `./scripts/demo-cluster.ps1`.
It seeds original sample documents, checks COMPLETE with two phrase matches, prints
real JSON results, and terminates its own processes. Data/logs remain in workspace work/.

Manual worker command (change shard, port, data directory and ready file for each worker):
`java -cp target/searchforge-x-0.1.0-SNAPSHOT.jar dev.searchforge.distributed.WorkerMain 0 3 8091 data/shard0 data/worker0.ready`

Coordinator:
`java -cp target/searchforge-x-0.1.0-SNAPSHOT.jar dev.searchforge.distributed.CoordinatorMain 8090 data/coordinator.ready http://127.0.0.1:8091 http://127.0.0.1:8092 http://127.0.0.1:8093`

Compose configuration: `docker compose up --build`. Docker is unavailable locally, but
GitHub's Ubuntu container job built the image, passed health checks, indexed/search-tested
three documents, stopped worker1 to verify PARTIAL results and restarted it to verify
persistent recovery. See CHECKPOINT.md for the exact run and tested revision.

Tests: DistributedTest launches real workers/coordinator, compares six query forms with
single-node scores/IDs, replays ingestion, stops and restarts a worker, verifies stale
generation rejection, tests malformed HTTP responses and deadlines, and interrupts fanout.
Additional network-fault servers are deliberate fault injectors, not replacements for the
real-process equivalence test.

Diagnostics report actual worker generations, documents, vocabulary and retained tokens.
The interface labels aggregate counts as covering responding workers. Coordinator health
alone still indicates process readiness, not full shard availability.
