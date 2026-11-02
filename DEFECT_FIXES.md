# Regression fixes — 2026-11-02

Two defects were reproduced with new tests against production revision `1646f33`.

1. The JSON mapper silently converted floating-point values to integer fields. For
   example, an ingest ID of `1.9` was accepted as ID `1`, allowing a request to address
   a different document. Floating-point deletion IDs and query pagination/deadline
   fields were also accepted. The shared protocol mapper now rejects floating-point
   values for integer fields, returning HTTP 400 before the handler mutates data.
   Integer-valued JSON requests remain supported.
2. Direct calls to `GenerationStore.commit` could publish generation zero, even though
   recovery rejects non-positive committed generations. The store now rejects it
   before writing segment or manifest files. Generation zero remains valid for an
   empty, unpublished in-memory snapshot.

The three focused regression methods failed before the changes and passed afterward.
The ingest regression verifies that rejected requests preserve the original document
and generation, and that a subsequent valid integer-ID write still succeeds. The
storage regression verifies no files beyond the lock are created and that a subsequent
valid generation commits and recovers correctly. Query tests cover floating-point
values in offset, limit and timeoutMillis.

A clean local Java 21 Maven verify passed all 32 Java tests with zero skips. Existing
corruption, process-crash, concurrency and distributed tests remain enabled. See
[evidence](evidence/quality/numeric-and-generation-regressions.json) for observed
before/after results. GitHub's live main-branch checks report remote validation for
this follow-up; the older reports continue to describe their specified revisions.
