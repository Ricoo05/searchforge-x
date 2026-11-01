# Engineering checkpoint — 2026-11-02 IST

Milestone 1: locally passed. Custom positional index, Unicode analysis, Boolean/phrase
queries, BM25, deterministic top-K/pagination, atomic batch updates and packaged demo.

Milestone 2 foundation: implemented and locally verified. Persistent full generations,
delta/varint postings, checksums, exclusive ownership, restart recovery, safe deletion,
obsolete-generation compaction, immutable readers, bounded execution and cancellation.
Scope of the durability claim is the tested process-crash boundaries only.

Milestone 3 core: locally verified with three worker JVMs and a coordinator JVM. Shared
statistics produce identical IDs/scores to single-node search for the tested static corpus.
Worker loss is explicit PARTIAL, restarting recovers COMPLETE, and incompatible generations
are rejected. Docker configuration is prepared but untested because Docker is absent.

Tests: 26 passed, 0 failed, 0 errors, 0 skipped. Actual subprocess termination covers
three write-publication stages and one cleanup stage. Maven verify succeeded.
CI is configured but unexecuted remotely. Packaged four-process demo passed as well.
Raw logs are retained under evidence/. No benchmarks or relevance measurements exist.

Review findings addressed: metadata UTF-16 could otherwise corrupt UTF-8 round trips;
analyzer configuration must persist even when a corpus happens not to contain stop words;
an uncertain commit must poison the current writer until recovery; cleanup must remove
old publication before old data; query queues require explicit capacity and rejection.

Known limitations: full rebuild/write per batch, memory-heavy startup validation, no
power-loss claim, no UI, no replication/authentication, no evaluation corpus yet.
No outstanding failing test is being ignored. Coverage is finite, not a claim of no bugs.

Next priorities: verify Docker in an available environment; reproducible performance
and relevance evaluation; investigate a measured optimization; connected UI and complete
learning/portfolio documents. See HANDOFF.md for remaining acceptance work.

Model handoff: the initial core has reached a tested handoff point for Sol 6.1 High.
Return to Astra if subsequent testing exposes difficult recovery/concurrency/ranking issues.
This checkpoint is not a declaration that the entire project is complete.
