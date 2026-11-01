# SearchForge X architecture

Status: Milestone 1 locally verified; initial persistent-generation implementation verified
against the documented process-crash tests. Real multi-process search passes local
single-node equivalence and fault tests; Docker deployment remains unexecuted.

Java 21, one Maven artifact, package boundaries for analyzer, index, query and node.
Split deployment artifacts when inter-process execution requires them. Avoid empty modules.

Documents -> Analyzer -> immutable positional IndexSnapshot -> Query AST -> candidate sets
-> BM25 -> bounded top-K heap -> deterministic page.

The writer constructs a complete replacement snapshot before publishing an atomic reference.
Readers capture one generation. Failed batches publish nothing. This deliberately simple
baseline rebuilds the index on each batch; it is not the final scalable storage design.
IDs are positive caller-assigned longs; replacements preserve identity. Equal content with
different IDs is presently permitted. Metadata is immutable and not searchable.

Scoring: k1=1.2, b=.75, positive Robertson IDF. N includes empty documents; document
length is retained token count. Repeated query terms contribute once. Every matching
document is scored against the union of query terms. Ties use ascending document ID.

Query grammar: uppercase AND/OR, parentheses, double-quoted phrases; AND precedes OR.
Adjacent operands imply OR. Punctuation inside a bare operand produces an OR of its
analyzed tokens. No NOT, escaping, prefix, wildcard or field syntax. Unicode NFKC and
Locale.ROOT lowercase are shared by documents and queries. No stemming. Stop words
are off by default; enabling them preserves original positions but does not enforce
the identity of omitted words in phrase matches. Leading/trailing omitted words do
not constrain phrase boundaries. Empty analyzed operands match nothing.

Pagination is offset-based over one captured snapshot. Separate requests can see
different generations and consequently skip/repeat documents during updates.

## Storage decision

Versioned full immutable segments, checksum validation and atomic commit manifests are
implemented. No WAL: acknowledgment follows full-segment publication. Explicit process
crash tests pass; power-loss durability remains unclaimed. See STORAGE_ENGINE.md.
QueryExecutor bounds workers and queued requests, rejects overload, includes queue time
in deadlines and interrupts canceled work. Cancellation remains cooperative; some bulk
collection operations and analysis may finish before the next interruption check.

WorkerMain and CoordinatorMain implement two-phase statistics/scoring over HTTP JSON.
Shared corpus statistics and a checked generation vector preserve comparable BM25 scores.
See DISTRIBUTED_SYSTEM.md for the complete/partial/failed contract and consistency limits.

## Milestone gates

1. Correctness: known BM25 example, independent randomized document-scan oracle,
   phrases with repeated terms and gaps, malformed queries, updates/deletions,
   immutable snapshots, deterministic top-K and reproducible CLI demo.
2. Durability: actual persistent postings/metadata, restart equivalence, corruption
   rejection, tested crash boundaries, safe compaction, reader/writer stress tests.
3. Distribution: independent processes, single-node equivalence on fixed snapshots,
   global statistics, deadlines, malformed responses, explicit complete/partial/failed status.
4. Evidence: licensed corpus, reproducible measurements and relevance judgments,
   profiling and an optimization compared to its retained baseline.
5. Portfolio: real connected UI, technical documentation, teaching exercises and
   resume claims supported by repository evidence.

Do not mark a gate passed merely because its files exist.
