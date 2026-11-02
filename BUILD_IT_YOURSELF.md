# Independent exercises: Milestone 1

## Analyzer
Source: src/main/java/dev/searchforge/analyzer/Analyzer.java.
Concept: identical normalization makes query and document vocabulary compatible.
Trace: `RED the fox` with stop word `the` produces red@0, fox@2.
Exercise: add tests for decomposed accents and non-Latin words.
Debug: compact positions after removing stop words and identify incorrect phrase matches.
Rebuild: implement a tokenizer and position tracking without reading this source.
Interview: why is lowercasing with the host locale dangerous?

## Inverted index
Source: src/main/java/dev/searchforge/index/IndexSnapshot.java.
Concept: map each term to documents and occurrences so retrieval need not scan every text.
Trace: ID 7 `red red fox` produces red->{7:[0,1]}, fox->{7:[2]}, length(7)=3.
Exercise: compute the document frequency and total occurrences of each term.
Debug: deliberately expose a mutable positions list, mutate it, and explain the failure.
Rebuild: implement index construction, replacement and deletion independently.
Interview: compare hash lookup, sorted dictionary lookup and prefix search with a trie.

## Boolean and phrase queries
Sources: query/Query.java and query/Search.java under src/main/java/dev/searchforge.
Concept: expression trees combine sets; positional offsets distinguish phrases.
Trace: `a OR b AND c` becomes OR(a,AND(b,c)).
Exercise: produce a corpus that distinguishes this from AND(OR(a,b),c).
Debug: remove phrase offset checks and find a false positive.
Rebuild: write a precedence parser and phrase matcher without consulting this code.
Interview: why do repeated phrase terms need more than document-level set intersection?

## BM25 and top-K
Source: query/Search.java. Test: src/test/java/dev/searchforge/EngineTest.java.
Concept: rarity, frequency saturation and length normalization contribute to lexical ranking.
Trace: the test's red example uses N=4, df=2, avgdl=2, tf=2, dl=3.
Exercise: vary k1 and b on paper and predict which documents benefit.
Debug: reverse the heap comparator and inspect which hits survive.
Rebuild: score by a brute-force document scan, sort all results, then replace sorting with a heap.
Interview: why can't a coordinator blindly merge scores computed with different shard statistics?

## Publication and concurrency
Source: node/SearchEngine.java. Test: ConcurrencyTest.java.
Concept: a writer builds privately, then atomically publishes an immutable snapshot.
Trace: a malformed document throws before current.set, preserving the prior generation.
Exercise: capture an old snapshot, update the engine and verify the old snapshot is unchanged.
Debug: publish before building all postings and design a reader test that exposes mixed state.
Rebuild: implement atomic batch replacement and a barrier-coordinated concurrency test.
Interview: what does this guarantee within a process, and what does it fail to guarantee after a crash?

## Persistent segments and compression
Sources: storage/SegmentCodec.java and storage/GenerationStore.java.
Concept: immutable committed generations separate private writes from visible data.
Trace: update generation 1 -> write segment 2 -> force pending manifest -> atomic commit
marker publication -> reader publication. An error after the commit point is uncertain.
Exercise: encode IDs 1, 3, 130 and repeated positions using deltas and base-128 varints.
Debug: truncate a segment halfway through a varint and require a clear failure.
Rebuild: implement a checksummed file format and an atomic manifest protocol on a small
corpus; kill a subprocess at each write boundary. State the actual failure model.
Interview: why does forcing file content not establish durable directory-entry publication?

## Compaction and recovery
Source: storage/GenerationStore.java; tests: StorageTest, CrashChild and CompactionChild.
Concept: recovery selects only published generations; reclamation must not invalidate them.
Trace: keep generation 2, remove generation 1's commit marker, then its data. Readers with
the old immutable snapshot continue using their in-memory data.
Exercise: retain a reader while compacting and verify its phrase results remain unchanged.
Debug: delete the newest segment and show that recovery fails rather than silently rolling back.
Rebuild: write a recovery scanner that distinguishes committed, pending and orphan data.
Interview: how does this full-generation design differ from LSM-style incremental merging?

## Distributed ranking and network failure
Sources: distributed/Coordinator.java, WorkerMain.java, Protocol.java and DistributedTest.
Concept: the coordinator fans out statistics, then scores using a compatible generation vector.
Trace: three shard statistics -> one global N/total length/df map -> local top-K -> global
top-K. A changed worker UUID or generation invalidates that scoring phase.
Exercise: construct a corpus where local IDF scores produce a different global ordering.
Debug: stop a worker, observe PARTIAL; change a generation between phases, observe FAILED.
Rebuild: write a three-process aggregator with timeouts and explicit partial-result status.
Interview: is a write with no acknowledgment known to be absent? Explain UNKNOWN and replay.

## Benchmarks and relevance
Sources: benchmarks/BenchmarkMain.java, NetworkBenchmarkMain.java, EvaluateMain.java;
scripts/evaluate.py and scripts/summarize_benchmarks.py.
Concept: latency, throughput, space and relevance measure different properties.
Trace: seeded corpus -> warmup -> recorded samples -> independent JVM trials -> raw JSON
-> median trial statistics. Relevance uses separate judgments and macro-averaged metrics.
Exercise: calculate nDCG and reciprocal rank on a five-document ranking by hand.
Debug: include warmup in a timed workload or evaluate on only successful queries and show
how the reported conclusion changes. Do not silently remove difficult observations.
Rebuild: implement a tiny independently tested evaluator and a benchmark that preserves
input hashes, machine/JVM configuration and all raw observations.
Interview: what can three trials and 350 samples establish, and what remains uncertain?

## Connected demonstration interface
Sources: web/src/main.tsx, web/src/style.css, distributed/JsonServer.java.
Concept: interface state must follow actual backend responses and preserve failure meaning.
Trace: submit a document -> routed ingestion acknowledgments -> diagnostics refresh ->
query -> COMPLETE/PARTIAL/FAILED status -> escaped text results and measured round-trip time.
Exercise: index text containing HTML and verify it is rendered as text.
Debug: show stale results after a failed request, then fix the request-state transition.
Rebuild: create a minimal search form that distinguishes network failure, zero matches and
partial results. Show unknown health as unknown rather than an invented healthy count.
Interview: why is available-worker document count different from total cluster document count?
