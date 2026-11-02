SEARCHFORGE X — FLAGSHIP SOFTWARE ENGINEERING PROJECT

Mission: Build an Exceptional, Interview-Defensible Distributed Search Engine

YOUR ROLE

You are acting as a team of senior specialists:

- Staff Software Engineer — Distributed Systems
- Senior Search Infrastructure Engineer
- Storage Engine Architect
- Algorithms and Data Structures Specialist
- Site Reliability Engineer
- Performance Engineering Specialist
- Rigorous Code Reviewer
- Technical Educator and Interviewer

Your mission is to design, implement, test, benchmark, document, and validate an exceptional software engineering portfolio project.

The target quality is that of a serious engineering project suitable for review by experienced engineers at Google or comparable companies.

Do not claim affiliation with Google.

Do not assume that making an impressive repository guarantees hiring.

CRITICAL CONTEXT

I am a 2026 Electronics and Telecommunication engineering graduate transitioning into software engineering.

I am currently a beginner in many of the advanced engineering concepts required for this project.

The implementation must be accompanied by a practical guide to understanding the codebase.

Therefore, your responsibilities are TWO-FOLD:

1. Build a technically impressive, correct, functioning system.
2. Make it possible for me to develop genuine understanding and eventually reproduce important components independently.

Never fabricate implementation experience.

The project must be educationally accessible without sacrificing engineering depth.

---

PART 1 — PROJECT DEFINITION

Project name:

SearchForge X — Fault-Tolerant Distributed Search Engine

Project objective:

Build a distributed document-search platform with a custom-built inverted index, persistent storage engine, relevance ranking, concurrent query execution, distributed query coordination, failure handling, and reproducible performance benchmarks.

The system must implement the essential information-retrieval machinery itself.

Do not build a superficial interface that delegates all substantive functionality to Elasticsearch, OpenSearch, Apache Lucene, Algolia, or a hosted search API.

Those systems may be used as independent reference implementations or comparison baselines, but not as the project's core search engine.

Engineering priorities

Rank requirements in this order:

1. Correctness
2. Reproducibility
3. Algorithmic depth
4. Durability and failure recovery
5. Concurrency correctness
6. Distributed-systems behavior
7. Measured performance
8. Code quality and maintainability
9. Documentation and demonstrability
10. Frontend polish

Do not prioritize visual design over correctness.

---

PART 2 — INITIAL ARCHITECTURE

Use Java 21 for the core engine.

Choose appropriate supporting technologies, prioritizing simplicity and technical defensibility.

Preferred stack:

- Java 21
- Java standard concurrency primitives
- JUnit 5
- Maven or Gradle
- Spring Boot for external APIs
- gRPC or another justified protocol for inter-node communication
- Docker Compose
- GitHub Actions
- React and TypeScript for the demonstration UI
- Python for analysis and visualization of benchmark results

Do not add infrastructure without a demonstrated need.

Proposed modules

Organize the application into cohesive modules such as:

"searchforge-analyzer"

Text normalization and tokenization.

"searchforge-index"

Inverted index creation, postings, dictionary structures and retrieval.

"searchforge-storage"

Persistent segments, commit protocol, logging, recovery and compaction.

"searchforge-query"

Query parsing, Boolean evaluation, phrase matching, scoring and top-K retrieval.

"searchforge-node"

Search-node lifecycle, index ownership, concurrency and local execution.

"searchforge-coordinator"

Distributed query fanout, result merging, timeouts and failure management.

"searchforge-api"

External ingestion, querying and administrative APIs.

"searchforge-web"

Working search interface and diagnostics.

"searchforge-benchmarks"

Performance experiments and retrieval-quality evaluation.

Use these as a starting architecture, not as mandatory packaging if a simpler organization is better.

Document important architectural decisions.

---

PART 3 — CUSTOM SEARCH ENGINE IMPLEMENTATION

3.1 Document ingestion

Implement:

- Stable document IDs
- Batch ingestion
- Document updates
- Document deletion
- Duplicate document detection
- Metadata storage
- Input validation
- Configurable indexing pipelines

Use a permitted dataset with documented provenance and licensing.

Do not indiscriminately scrape websites.

3.2 Text analysis

Implement a consistent query and document processing pipeline with:

- Unicode normalization
- Tokenization
- Case handling
- Optional stop-word filtering
- Consistent token positions
- Document length tracking
- Term frequency statistics
- Document frequency statistics

Test edge cases involving empty documents, Unicode, punctuation, repeated tokens and malformed inputs.

3.3 Inverted index

Implement a custom inverted index containing:

- Term dictionary
- Posting lists
- Document IDs
- Term frequencies
- Positional postings
- Document frequency metadata
- Corpus statistics

Select efficient data structures and justify them.

Investigate:

- Hash maps
- Sorted arrays
- Binary search
- Heaps
- Skip pointers
- Tries where useful
- Delta encoding
- Variable-length integer encoding

Implement at least one meaningful postings compression technique and compare it against an uncompressed baseline.

Measure storage overhead and query-performance tradeoffs.

Do not merely claim compression is beneficial.

3.4 Query processing

Support:

- Single-term queries
- Boolean AND
- Boolean OR
- Phrase queries
- Multi-term ranked search
- Top-K retrieval
- Stable deterministic tie-breaking
- Pagination with documented consistency limitations

Implement the algorithms directly.

Write property-based, randomized or oracle-backed correctness tests where appropriate.

3.5 Ranking

Implement BM25 correctly.

Include:

- Document-length normalization
- Corpus-wide statistics
- Term-frequency weighting
- Inverse document frequency
- Ranked results
- Top-K selection

Create small examples with manually verified results.

Compare ranked retrieval against a brute-force reference implementation to validate correctness.

A custom top-K heap implementation is acceptable when it teaches useful algorithms, but Java's standard collections may be used when appropriate.

Correct algorithm selection matters more than unnecessarily rewriting standard libraries.

---

PART 4 — PERSISTENT STORAGE ENGINE

This is a critical project differentiator.

Do not build a search system that loses its index every time the process restarts.

Implement an appropriate persistent storage architecture.

Consider immutable index segments inspired by log-structured storage designs.

Core requirements:

- Persistent on-disk index format
- Document metadata persistence
- Index versioning
- Atomic publication of committed index generations
- Durable ingestion where promised
- Crash recovery
- Segment integrity validation
- Safe document updates
- Tombstones for deletions where justified
- Segment merging or compaction
- Reader consistency during index updates

Evaluate whether a write-ahead log is the correct mechanism for the chosen architecture.

If implemented, document exactly what it guarantees.

Test crashes at relevant points during:

- Document ingestion
- Log writing
- Segment writing
- Manifest publication
- Segment merging
- Recovery

Use controlled fault injection where possible.

Do not claim full crash safety unless the supported failure scenarios are actually tested.

---

PART 5 — CONCURRENCY ENGINEERING

Implement safe concurrent query execution and indexing.

Requirements:

- Bounded worker pools
- Thread-safe data structures
- Clear ownership boundaries
- Backpressure
- Safe index-generation publication
- Concurrent readers
- Controlled writers
- Cancellation and timeout handling
- Resource cleanup

Investigate:

- Locks
- Read/write synchronization
- Immutable snapshots
- Atomic references
- Race conditions
- Deadlocks
- Memory visibility

Measure concurrency behavior under multiple workloads.

Create tests that would detect concurrency bugs rather than merely running two threads and assuming success.

Document the selected concurrency model and its limitations.

---

PART 6 — GENUINE DISTRIBUTED SEARCH

Only begin this phase once the single-node engine is correct and persistent.

Implement a genuine multi-process search deployment.

Target:

- One query coordinator
- Three independent search worker processes
- Configurable document sharding
- Docker Compose deployment

Each worker should have independently managed storage and a clearly defined shard assignment.

The coordinator must:

1. Receive search requests.
2. Identify relevant shards.
3. Dispatch requests concurrently.
4. Enforce deadlines and timeouts.
5. Collect results.
6. Merge results into a global top-K ranking.
7. Return accurate status information.
8. Handle unavailable workers according to a documented policy.

Critical ranking problem

Address the fact that naïvely combining shard-local BM25 scores can produce inconsistent global rankings.

Implement or document a defensible solution using appropriate global corpus statistics and compatible scoring.

Test the distributed results against the single-node implementation under equivalent corpus and snapshot conditions.

Differences must be investigated, not ignored.

Fault handling

Test:

- One worker becoming unavailable
- Worker recovery
- Slow workers
- Coordinator request timeout
- Invalid network responses
- Duplicate requests
- Interrupted queries
- Partial shard availability

Clearly distinguish between:

- Successful complete results
- Partial results
- Failed requests

Never silently present partial search results as complete.

Advanced stretch goal

Implement replica-aware recovery or failover only if the core sharded system is already reliable.

Do not claim strong consistency, exactly-once delivery, zero downtime or high availability without supporting architecture and tests.

---

PART 7 — PERFORMANCE OPTIMIZATION

A sophisticated project must produce measurable engineering evidence.

Implement reproducible benchmarking for:

- Index construction
- Index size
- Memory consumption
- Query latency
- Query throughput
- Concurrent searches
- Storage compression
- Distributed query overhead
- Failure behavior

Measure p50, p95 and p99 query latency where statistically appropriate.

Use repeated trials, warmup periods and controlled workload configurations.

Record hardware, JVM options, dataset versions and system conditions.

Target multiple dataset sizes, such as:

- 10,000 documents
- 100,000 documents
- 1,000,000 documents if the environment permits

These are aspirational workload sizes, NOT claims of achieved scale.

Inspect available CPU, RAM and disk before selecting realistic test sizes.

Implement benchmarking scripts so results can be reproduced.

Independent baseline comparison

Where feasible, benchmark against a standard Lucene-based implementation with equivalent corpus, analyzer, ranking behavior, workload and hardware.

Use the reference implementation only for comparisons.

Document unavoidable differences.

Never cherry-pick settings or invent performance superiority.

A slower but correct implementation with a thoughtful technical analysis is acceptable.

Meaningful optimization

Profile the engine.

Identify at least one genuine bottleneck and investigate a justified optimization.

Possible examples:

- More efficient postings traversal
- Delta-compressed document IDs
- Reducing temporary allocations
- Better top-K evaluation
- Improved buffer management
- Parallel shard execution

Preserve baseline measurements and rerun identical benchmarks after optimizing.

Only report improvements confirmed by valid measurements.

---

PART 8 — INFORMATION RETRIEVAL QUALITY

Evaluate search relevance against established datasets with relevance judgments.

Investigate suitable BEIR datasets such as:

- SciFact
- NFCorpus
- FiQA

Verify dataset permissions and record provenance.

Use compatible evaluation tooling to measure:

- nDCG@10
- Recall@K
- Precision@K
- MRR where appropriate

Separate relevance quality from performance engineering.

Do not treat fast results as necessarily relevant results.

Do not present BM25 as a neural semantic retrieval system.

Compare retrieval behavior against a suitable BM25 reference when possible.

Document failure cases where lexical retrieval performs poorly.

---

PART 9 — FUNCTIONAL DEMONSTRATION UI

Build a restrained, highly polished frontend only after the core system is working.

The frontend must be connected to real backend services.

Features:

- Search bar
- Ranked document results
- Snippets
- Search latency
- Document indexing controls
- Index statistics
- Worker health
- Shard diagnostics
- Query execution status
- Error and partial-result indicators

An optional engineering diagnostics panel may display real metrics.

Do not create decorative dashboards with fake values.

Prioritize usability and demonstrability over animations.

The demo must clearly show the actual system functioning.

---

PART 10 — TESTING REQUIREMENTS

Implement meaningful automated tests across all major boundaries.

Categories:

1. Unit tests
2. Property-based or randomized tests
3. Integration tests
4. Concurrency tests
5. Storage recovery tests
6. Distributed-system tests
7. Fault-injection tests
8. Performance regression experiments

Critical acceptance scenarios:

- Correct phrase-search results
- Correct BM25 scores on known examples
- Correct index persistence after restart
- Correct document deletion behavior
- Safe recovery from tested crash points
- Consistent ranking under equivalent single-node and distributed snapshots
- Correct identification of partial results
- No knowingly ignored corruption or silent data loss
- Reproducible build and tests on a clean environment

Build a CI pipeline that compiles and runs appropriate tests.

Do not fake external services in tests that are specifically intended to verify integration with real services.

Never remove difficult tests to force a green build.

Distinguish passed, failed, skipped and unexecuted tests.

---

PART 11 — ARCHITECTURAL QUALITY AND SECURITY

Apply professional engineering practices:

- Clear modules and interfaces
- Dependency management
- Structured logs
- Validation and error handling
- Sensible API limits
- Resource cleanup
- Safe configuration
- No hardcoded secrets
- Docker health checks
- Reproducible setup
- Useful observability

Treat the administrative indexing interface as potentially security-sensitive.

Secure or bind local-only administrative operations as appropriate.

Do not expose destructive endpoints or unrestricted ingestion publicly.

Do not deploy a paid cloud infrastructure without my authorization.

Use local resources first.

---

PART 12 — PROFESSIONAL GITHUB PRESENTATION

Create a repository that can withstand a serious technical review.

Required documents:

- README.md
- ARCHITECTURE.md
- ALGORITHMS.md
- STORAGE_ENGINE.md
- DISTRIBUTED_SYSTEM.md
- BENCHMARKS.md
- RELIABILITY_TESTS.md
- TRADEOFFS.md
- KNOWN_LIMITATIONS.md
- INTERVIEW_GUIDE.md
- RESUME_EVIDENCE.md

README must explain:

1. What problem SearchForge X solves.
2. Why the project exists.
3. What was implemented from scratch.
4. Major engineering challenges.
5. Architecture.
6. How to run the system.
7. How to run tests.
8. How to reproduce benchmarks.
9. Actual measured results.
10. Known limitations.

Create architecture diagrams and screenshots where appropriate.

Keep documentation accurate and concise enough to be useful.

Do not write marketing claims that the implementation cannot substantiate.

Do not push to GitHub or make a repository public without explicit authorization.

---

PART 13 — PERSONAL LEARNING SYSTEM

This section is mandatory.

The repository must include a structured learning path for its engineering concepts.

I need to become capable of understanding, modifying, debugging and reproducing important components.

Create:

"LEARNING_ROADMAP.md"

"JAVA_PREREQUISITES.md"

"DSA_CONCEPTS.md"

"BUILD_IT_YOURSELF.md"

"INTERVIEW_GUIDE.md"

Build a structured learning path covering:

Beginner foundations

- Java syntax
- OOP
- Collections
- Interfaces
- Exceptions
- File handling
- Threads
- Basic networking

Core DSA

- Arrays
- Hash maps
- Linked lists
- Sorting
- Binary search
- Heaps
- Trees
- Tries
- String algorithms
- Time and space complexity

Search engineering

- Inverted indexes
- Posting lists
- Ranking
- BM25
- Compression
- Query execution

Advanced software systems

- Concurrency
- Persistent storage
- Crash recovery
- Sharding
- Network failures
- Distributed result merging
- Benchmark interpretation

For every major subsystem:

1. Explain the basic concept.
2. Explain the reason it exists.
3. Reference the actual source files.
4. Trace an example through the implementation.
5. Provide a small coding exercise.
6. Provide a debugging exercise.
7. Provide an independent implementation challenge.
8. Provide realistic interview questions.

Do not confuse memorized explanations with actual understanding.

When I invoke tutoring mode, teach one lesson at a time and request my solution before revealing the answer.

The ultimate goal is for me to rebuild the essential inverted-index and retrieval functionality independently.

---

PART 14 — REALISTIC ENGINEERING MILESTONES

Execute in the following order.

Milestone 1 — Correct search engine

Deliver:

- Working single-node engine
- Custom inverted index
- Boolean and phrase search
- BM25 ranking
- Automated correctness tests
- Reproducible local demonstration

Do not proceed until correctness is demonstrated.

Milestone 2 — Durable search engine

Deliver:

- Persistent index format
- Restart recovery
- Safe updates/deletions
- Segment management
- Concurrency support
- Verified persistence and recovery tests

Do not proceed until the documented durability guarantees have been tested.

Milestone 3 — Distributed search engine

Deliver:

- Real multi-process execution
- Document sharding
- Coordinator
- Concurrent fanout
- Global top-K merging
- Failure handling
- Distributed integration tests

Do not describe networked processes as fault-tolerant unless the implemented recovery behavior supports that description.

Milestone 4 — Performance and evaluation

Deliver:

- Reproducible benchmark harness
- Real measured results
- Relevance evaluation
- At least one investigated performance optimization
- Technical analysis of findings

Milestone 5 — Portfolio completion

Deliver:

- Functional UI
- Documentation
- Diagrams
- Test instructions
- Demonstration material
- Resume-ready evidence
- Personal learning roadmap

Do not trade away correctness in order to rush into later milestones.

---

PART 15 — STRICT ENGINEERING REVIEW

At each milestone, conduct a skeptical review.

Ask:

- Does this feature actually work?
- Is the claimed behavior demonstrated by tests?
- What happens when the input is malformed?
- What happens during concurrency?
- What happens after a crash?
- Can the benchmark be reproduced?
- Is this algorithm actually efficient?
- Is the design unnecessarily complicated?
- What happens if a distributed worker fails?
- Are errors surfaced honestly?
- Could another engineer understand and maintain the code?

Fix meaningful problems before adding optional features.

Write a checkpoint report containing:

- Features completed
- Tests executed
- Tests passing
- Tests failing
- Known defects
- Important decisions
- Next priorities

Do not call work complete merely because files were generated.

---

PART 16 — FINAL QUALITY RUBRIC

Evaluate SearchForge X using the following weighted rubric.

Dimension| Weight
Algorithmic correctness and depth| 20
Storage and recovery| 15
Distributed systems and failure handling| 15
Performance and relevance evidence| 15
Automated testing| 10
Architecture and maintainability| 10
Reproducibility and documentation| 10
Functional presentation| 5
Total| 100

Ratings must be evidence-based.

A score of 90+ should only be awarded when the project genuinely demonstrates strong engineering across these categories.

Do not inflate ratings to satisfy my expectations.

Unimplemented features receive no credit.

An impressive frontend cannot compensate for an incorrect search engine.

Additionally, evaluate my personal interview readiness separately from the repository's engineering quality.

Assess personal competency through practical coding and debugging exercises.

---

PART 17 — FINAL OUTPUT

Deliver:

1. Complete source code
2. Working local execution
3. Multi-node Docker deployment
4. Automated tests
5. Reproducible benchmarks
6. Real performance results
7. Relevance evaluation
8. Functional demonstration interface
9. Technical architecture documentation
10. Learning roadmap
11. Interview preparation guide
12. Evidence-backed resume bullets
13. Honest engineering assessment

Generate three concise resume-bullet candidates backed by actual repository evidence.

Do not invent measurements.

Keep experience claims factual and supported by evidence.

My final Google-oriented resume must remain one page, and the project should only be added once the implemented capabilities justify inclusion.

---

ABSOLUTE RULES

- No fake features.
- No fake metrics.
- No fake tests.
- No fake scalability claims.
- No fabricated experience.
- No shallow framework wrappers pretending to be original systems.
- No unverified claims of production readiness.
- No unnecessary complexity solely to impress recruiters.
- No replacing engineering correctness with visual polish.
- No expensive cloud deployment without approval.
- No public publishing without approval.
- No hidden failures or silent test weakening.
- No representing planned work as finished work.

Document engineering decisions and verify implementation behavior.

Write maintainable, human-readable code.

Document meaningful tradeoffs.

Prefer a smaller, correct, deeply understood system over a massive, fragile application.

If execution is interrupted or a tool limit is reached, record accurate progress and remaining work without claiming completion.

START NOW

Inspect the current workspace and development environment.

Create the architecture and milestone acceptance tests.

Begin implementing Milestone 1 immediately.

Do not stop after producing a plan.

Your mission is to create a flagship engineering project that demonstrates real computer science, disciplined implementation, verifiable results, and genuine educational value.
