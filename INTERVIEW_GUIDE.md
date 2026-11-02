# Interview preparation

Prepare a walkthrough of the components you can explain, modify and debug.
Keep an exercise log with dated attempts, tests and debugging notes.

## A defensible two-minute walkthrough

The engine builds positional postings from normalized documents, evaluates Boolean/phrase
queries, scores candidates with BM25 and selects top-K. A writer constructs an immutable
generation and publishes it atomically. Durable mode forces a full segment before a
commit marker; tested process crashes recover old or new complete data. Three worker JVMs
own modulo shards. The coordinator collects corpus statistics, checks each worker's
generation/identity and merges comparable shard scores. Partial results are labeled.

Use this walkthrough only once you can trace a real example through each source file.
Follow with a limitation you can explain and a result from the raw evidence you can reproduce.

## Questions you should answer with code and tests

- What is a posting? Distinguish tf, df, document length and average length.
- Why do phrases need positions? Demonstrate repeated terms and stop-word gaps.
- Derive the BM25 example in EngineTest; explain saturation and length normalization.
- Why is the heap root the worst retained hit? Prove the global top-K merge property.
- What happens if a batch fails after deleting a document from its private working map?
- Which line is the disk commit point? What does an IOException after it mean?
- Why is there no WAL? What would an incremental-segment design change?
- What do file checksums detect, and what is outside their protection?
- Why are process-crash tests insufficient for a power-loss claim?
- Explain memory publication, writer ownership and immutable snapshot reclamation.
- Reproduce the shutdown race and justify its fix without weakening the assertion.
- Why can shard-local BM25 statistics change global ranking? Show a counterexample.
- Are shard generations a globally atomic snapshot? What does concurrent ingestion do?
- Why can a PARTIAL result's scores change when the missing shard returns?
- Explain an UNKNOWN write outcome and a safe ID-based retry; distinguish idempotent state
  replacement from exactly-once delivery.
- Which benchmark operations were timed, what was warmed up, and what does p99 mean with
  350 observations? How do background work and GC limit the inference?
- Explain a relevance failure and why lexical BM25 cannot resolve every paraphrase.

## Practical interview checks

Rebuild an inverted index, phrase matcher, BM25 scorer and top-K
selection on a tiny corpus. Write a malformed-input test and debug a corrupted segment.
Then modify an actual subsystem, predict affected tests and explain the observed failure.
Being able to repeat a written explanation is not sufficient evidence of mastery.

Use the practical checks to assess interview readiness and identify topics to practise.

## Fifteen evidence-backed answers to defend

1. **Why store positions rather than just frequencies?** A frequency cannot distinguish
   `red fox` from `fox red`. Phrase matching checks relative retained positions, including
   analyzer gaps. Trace the repeated-term/phrase cases in EngineTest and ALGORITHMS.md.
2. **What does normalization guarantee?** The analyzer applies NFKC and case normalization
   to indexing and queries. It does not implement stemming or semantic equivalence. Verify
   Unicode fixtures in EngineTest; do not promise every language-specific convention.
3. **What controls BM25 saturation?** The term contribution uses IDF times the saturated
   tf numerator over tf plus length-normalized k1. k1=1.2 and b=.75 here. Recalculate the
   independently derived EngineTest case and compare Search.java, rather than memorizing scores.
4. **Why is the heap root the worst retained result?** It allows O(log K) replacement when
   a better candidate arrives. The final comparator orders score descending then ID ascending;
   deterministic ties are checked against the ranking oracle in EngineTest.
5. **Can updates partially mutate a published index?** Writers build a private replacement
   generation; concurrent readers retain an immutable snapshot. Failed construction does not
   publish the private working state. ConcurrencyTest exercises readers during replacement.
6. **Where is durable commit?** An atomic move publishes commit-N.manifest after forcing
   the segment and pending manifest. An exception after publication may still mean committed
   data; StorageTest's staged subprocess crashes verify old/new generations. See STORAGE_ENGINE.md.
7. **Why no WAL?** Every update persists a complete generation. This simplifies recovery
   but amplifies writes and bounds practical scale. A WAL/incremental merge design would
   require additional replay, ordering and reclamation invariants; it is not implemented.
8. **Do checksums prove crash safety?** They detect changed bytes, not durable hardware
   ordering or correct application semantics. Recovery also validates segment structure.
   Process termination tests do not establish power-loss or full-disk guarantees.
9. **How are stale distributed snapshots rejected?** Each worker response carries epoch
   and generation identity. A scoring request must match the statistics phase; otherwise
   it is rejected instead of mixing incompatible scores. DistributedTest verifies HTTP 409.
10. **Why share global statistics?** Local df and average length can give the same term
    different score scales across shards. Coordinator gathers compatible corpus statistics
    before fanout. DistributedTest compares IDs and scores with a single-node oracle.
11. **Are all shard generations globally atomic?** No. Generation checks establish each
    phase's compatibility, not a transactional snapshot across independent shards. Concurrent
    ingestion can make a request fail; the documented design does not hide that limitation.
12. **What does UNKNOWN ingestion mean?** No acknowledgment was obtained, so the write may
    have committed. Stable-ID replacement gives idempotent state on retry, not exactly-once
    execution or cross-shard atomicity. Real worker loss verifies mixed acknowledgment outcomes.
13. **What was the executor shutdown race?** Interrupting active work could let queued work
    begin before cancellation. Detach queued tasks before interrupting/canceling pending work.
    QueryExecutorTest repeats the regression 100 times; the original failure log is retained.
14. **Why is p99 weak evidence here?** Each trial uses 350 measured queries after warmup;
    only a few observations determine the tail. Workload, JVM, GC and desktop activity limit
    inference. BENCHMARKS.md retains three independent trials and avoids production claims.
15. **Does the Lucene comparison prove superiority?** No. nDCG@10 differs by about 0.00059
    on one 300-query scientific set. Exact lengths/doubles versus quantized norms/floats can
    change ties. Inspect per-query failures and reproduce both runs before interpreting them.

Hands-on checks: add a Unicode normalization regression; alter the heap tie-break and explain
the failing oracle; corrupt one copied segment and trace rejection; add an interrupted-writer
test; reproduce distributed partial results on disposable data; trace one request through
the coordinator's two phases. Record your own attempts before consulting reference answers.
