# Learn SearchForge X

This repository is AI-assisted. Passing its tests demonstrates software behavior;
it does not demonstrate your personal ability. Keep a learning log of exercises you
complete without generated solutions. In tutoring mode, request one lesson and submit
your solution before asking for the reference answer.

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
8. Storage and distributed systems: begin these lessons only when those subsystems
   exist and their recovery/ranking tests pass. Do not memorize planned architecture
   as though it were implemented.

Mastery checks: explain a change, predict affected tests, implement it unaided,
introduce and diagnose a bug, then rebuild the essential component from a blank file.
