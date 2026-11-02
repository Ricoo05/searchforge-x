# Learn SearchForge X

Use this roadmap to practise understanding, modifying and debugging the search engine.
Keep a learning log of completed exercises, tests and debugging notes. Work through
each lesson before consulting its reference answer.

1. Java prerequisites: values, methods, records, interfaces, exceptions, collections.
2. Analyzer: trace normalization and positions through Analyzer.java. Explain why
   phrase search needs positions and why removing a stop word must preserve gaps.
3. Index: trace `red red fox` into IndexSnapshot.java. Predict document frequency
   versus term frequency. Build a tiny index yourself using maps and lists.
4. Query: draw the tree for `a OR b AND c` from Query.java. Evaluate it on paper.
5. Ranking: reproduce the known BM25 example in EngineTest.java without a calculator
   library implementing BM25. Explain every factor and the deterministic tie rule.
6. Top-K: trace Search.java's heap for five candidates and K=2; explain why its root
   is the worst retained result. Compare full sorting with bounded selection.
7. Snapshots: trace SearchEngine.update. Explain why construction failure cannot
   modify the published index and why a reader holds one consistent generation.
8. Storage: follow segment encoding, manifest publication and recovery in STORAGE_ENGINE.md.
   Independently implement a varint codec and a small committed-generation scanner.
9. Distribution: trace shared statistics and generation checks in DISTRIBUTED_SYSTEM.md.
   Reproduce worker loss/recovery, then explain partial scores and uncertain write outcomes.
10. Evidence: reproduce one benchmark trial and one SciFact query; manually calculate a
    relevance metric and explain why the Lucene reference can differ slightly.
11. Product behavior: trace the real API into web/src/main.tsx. Index a document, search
    it, simulate an unavailable worker and verify the visible status remains accurate.

Suggested order: complete JAVA_PREREQUISITES first, then DSA_CONCEPTS, then the subsystem
exercises in BUILD_IT_YOURSELF. Move forward based on demonstrated mastery rather than
calendar time. INTERVIEW_GUIDE provides checks, not answers to memorize.

Mastery checks: explain a change, predict affected tests, implement it unaided,
introduce and diagnose a bug, then rebuild the essential component from a blank file.
