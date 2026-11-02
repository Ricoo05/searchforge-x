# Engineering tradeoffs

- Full-generation rebuilds simplify atomic updates, deletions, restart and compaction.
  They amplify write cost and memory use. A future incremental design needs tombstones,
  segment-level visibility and carefully tested merging.
- Immutable reader snapshots make ownership and memory visibility clear. Old snapshots
  remain live while queries reference them; JVM garbage collection decides reclamation.
- Startup reanalysis validates stored postings against documents and analyzer configuration.
  This adds startup time and memory. Checksums alone would not detect a validly checksummed
  semantic mismatch between those structures.
- Atomic file publication avoids a WAL for this batch contract. Lack of directory fsync
  prevents a power-loss durability claim. Tested process crashes are a narrower guarantee.
- Standard collections favor readable code and clear invariants. Boxed TreeMap postings
  have poor memory locality; measurements should guide an array/iterator redesign.
- Shared statistics make shard scores comparable. Two network phases add overhead, and
  concurrent writes can invalidate the query's generation vector. There is no global
  ingestion transaction or persistent multi-request snapshot lease.
- Available-shard statistics let PARTIAL results remain interpretable within the available
  corpus. They are not directly score-comparable with a later COMPLETE response.
- Fixed queues reject overload instead of accumulating unlimited work. Correct callers
  must handle 503, cancellation and timeout; interruption is cooperative.
- HTTP JSON keeps the four-process protocol inspectable and avoids generated RPC tooling.
  Spring Boot/gRPC were preferred starting options, not mandatory; the small bounded API
  uses JDK HTTP directly. Input, identity, generation and size validation remain explicit.
- Offset pagination is simple but bounded and unstable across updates. Search-after cursors
  tied to retained snapshots would require stronger lifecycle management.
- The frontend exposes measured request time and actual worker responses. It does not
  synthesize health or throughput. A local-only unauthenticated admin interface is suitable
  for this demo; public deployment requires authentication, authorization and TLS.
