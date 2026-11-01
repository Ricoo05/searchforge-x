# Storage engine v1

Implemented sources: storage/SegmentCodec.java, storage/GenerationStore.java and
node/SearchEngine.java under src/main/java/dev/searchforge.

Each generation is a complete immutable segment of live documents, metadata, analyzer
stop words, term dictionary and positional postings. IDs and positions are delta coded
with unsigned base-128 varints. Strings are length-prefixed strict UTF-8. The format
starts with SFX1 and a generation number; a SHA-256 trailer detects corruption.
Term and metadata ordering is deterministic. Retained token lengths and corpus
statistics are reconstructed from documents at startup, and every persisted posting
is checked against that reconstruction. This intentionally trades startup speed for
a strong consistency check. A future format can load validated postings directly.

## Commit protocol

1. Build an immutable candidate index without modifying the published reader snapshot.
2. Write segment-N.sfx; force its contents and file metadata.
3. Write pending-N.manifest containing SFM1, N, and the complete segment's SHA-256; force it.
4. Atomically move the pending file to commit-N.manifest. This is the commit point.
5. Publish the in-memory snapshot, then acknowledge the update.

There is no WAL. Full-segment publication is sufficient for the present batch-oriented
contract, though it incurs substantial write amplification. Updates replace the old
document and deletes omit it from the next full segment; tombstones are unnecessary
for this architecture. All prior generations remain until explicit compaction.

Recovery selects the greatest committed generation and verifies its manifest, segment
checksum, format, analyzer configuration and postings. A corrupt or missing committed
segment raises an error; recovery never silently falls back to an older generation.
Unpublished segment and pending files are ignored and can be overwritten by a retry.
If a commit returns an I/O error, the engine refuses further operations until reopened:
the error may have occurred after publication, making its outcome uncertain.

## Ownership and cleanup

A file lock prevents multiple processes opening the same directory. One writer serializes
updates. Queries hold immutable in-memory snapshots, so obsolete disk segments can be
reclaimed without invalidating readers. Every write already consolidates all live data;
compact() verifies the newest generation, removes obsolete commit markers, then removes
obsolete segments and pending files. This is whole-generation consolidation/reclamation,
not an incremental multi-segment merge algorithm.

## Tested scope and limits

Tests terminate independent JVMs after segment force, pending-manifest force, committed
manifest publication, and obsolete-manifest removal. Restart produces the appropriate
old or new complete generation, and subsequent writes succeed. See RELIABILITY_TESTS.md.

This supports the tested process-crash scenarios on the local Windows filesystem.
Power loss, kernel crashes, network filesystems, disk-full behavior, and storage hardware
failures have not been demonstrated. Directory entries are not explicitly fsynced;
do not infer power-loss durability from file force alone. Atomic move support is required;
there is no non-atomic fallback. Filesystem paths must be trusted local directories.

Segments are capped at 256 MiB; decoding and verification hold multiple copies in memory.
The cap is a format-v1 operational limit, not a tested scale claim. Configured stop words
must exactly match on reopening. Future analyzer changes require a new format/migration.

References: [Java 21 Files.move](https://docs.oracle.com/en/java/javase/21/docs/api/java.base/java/nio/file/Files.html)
and [FileChannel.force](https://docs.oracle.com/en/java/javase/21/docs/api/java.base/java/nio/channels/FileChannel.html).
