# Interview preparation and honest ownership

Describe SearchForge X as an AI-assisted project. Explain which components you personally
understand, reproduced or changed. Do not claim independent implementation experience
merely because the repository contains working code. Keep an exercise log with dated
attempts, tests and debugging notes.

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

Without generated code, rebuild an inverted index, phrase matcher, BM25 scorer and top-K
selection on a tiny corpus. Write a malformed-input test and debug a corrupted segment.
Then modify an actual subsystem, predict affected tests and explain the observed failure.
Being able to repeat a written explanation is not sufficient evidence of mastery.

Personal readiness: not assessed. No independent coding/debugging attempt from the user
has been observed in this chat. Repository quality and individual competence are separate.
