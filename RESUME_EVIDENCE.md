# Resume evidence and candidate wording

These are candidates for an AI-assisted project section after you can explain and modify
the relevant components. They do not assert unaided authorship or production experience.
Keep the eventual resume to one page and choose only the strongest one or two project bullets.

1. "SearchForge X — AI-assisted Java 21 search engine with custom positional indexing,
   Boolean/phrase retrieval, BM25 ranking, and deterministic top-K selection."
   Evidence: analyzer/, index/, query/, EngineTest and the independent randomized oracle.
2. "Implemented and tested immutable index generations and a three-worker search
   coordinator, with checksummed recovery, shared corpus statistics and explicit partial results."
   Mark the project as AI-assisted in its heading/description. Evidence: storage/,
   distributed/, StorageTest and four-process DistributedTest. Recovery is limited to
   documented process-crash scenarios; no production availability claim.
3. "Benchmarked 10,000/100,000 synthetic-document workloads and evaluated BM25 retrieval
   on 300 SciFact queries against an independent Lucene reference."
   Evidence: raw evidence/benchmarks JSON, SciFact provenance/runs/metrics and separate
   benchmarks/lucene source. These are experimental corpus sizes, not deployed scale.

Do not add "built independently", "production-ready", "millions of users/documents",
"high availability", "exactly once", or general speedup claims. Cite measured numbers
only with their exact workload and reproduction instructions. Verify final test counts
from the latest clean build before using a count in application material.
