# SearchForge X

An educational search engine built with AI assistance. The intended project explores
custom indexing, ranking, storage recovery and distributed retrieval. Current source
implements a single-node engine with optional persistent generations, tested process-crash
recovery, bounded query execution, and a real three-worker search coordinator.
This is a tested core checkpoint; the complete portfolio brief remains unfinished.

## Build and run

Requires JDK 21 and Maven 3.9.9 or compatible Maven 3.9.x.

```sh
mvn clean verify
java -jar target/searchforge-x-0.1.0-SNAPSHOT.jar
java -jar target/searchforge-x-0.1.0-SNAPSHOT.jar "recovery OR search"
```

The demonstration corpus consists of three original synthetic sentences in Demo.java.
It is a correctness demonstration, not a relevance or scale benchmark.

Implemented from scratch: Unicode analysis, positional inverted index, Boolean and
phrase evaluation, BM25 and heap-based top-K. Uses Java collections, not Lucene or
an external search service. See ARCHITECTURE.md for semantics and milestone gates.

Persistence uses GenerationStore through SearchEngine's durable constructor. The default
demo remains in-memory. Storage guarantees and restrictions are in STORAGE_ENGINE.md.

Current limits: full rebuild on batch update; 256 MiB segment limit; no UI,
licensed relevance corpus, or benchmark claims. The distributed JSON API is local-only
by default. Docker configuration is prepared but untested. Do not deploy publicly.

See DISTRIBUTED_SYSTEM.md for the HTTP API and `scripts/demo-cluster.ps1` for a reproducible
four-process demonstration. Run `scripts/verify-local.ps1` to use this workspace's Java/Maven.
Latest local suite: 26 passed, 0 failed, 0 errors, 0 skipped. Raw evidence is in evidence/.
CHECKPOINT.md and HANDOFF.md identify exactly what is complete and what remains.
