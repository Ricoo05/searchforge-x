# Algorithms and implementation

All core search code lives under `src/main/java/dev/searchforge`. Lucene is confined to
the separate `benchmarks/lucene` reference project and never serves application queries.

## Analysis and indexing

`analyzer/Analyzer.java` applies Unicode NFKC, Locale.ROOT lowercase and a Unicode
letter/number tokenizer with combining marks. It rejects malformed UTF-16. Stop words
are optional and off by default; positions count tokens before filtering. Indexed length
counts retained tokens. No stemming or semantic embeddings are used.

`index/IndexSnapshot.java` builds a hash term dictionary and sorted per-term document
maps. Each posting holds a sorted immutable position list; its length is term frequency.
Document frequency is the number of documents in that term's map. Documents and metadata
are owned immutable copies. Empty documents contribute to N with length zero.

For T retained token occurrences and D documents, indexing requires O(T log D) posting
map work and O(T + D) storage, with substantial boxed-object overhead. Each batch rebuilds
the whole live index. There is no incremental indexing performance claim.

## Queries and phrases

`query/Query.java` is a bounded precedence parser. AND binds more tightly than OR;
adjacency means OR. Parentheses and double-quoted phrases are supported. Input is bounded
at 4096 UTF-16 units, 256 lexical tokens and 32 nested parentheses. There is no NOT,
escaping, wildcard, field, stemming or prefix syntax.

`query/Search.java` recursively evaluates sets. AND intersects, OR unions. TreeSets
provide deterministic traversal, but repeated balanced-tree insertion costs O(log C)
per candidate. This favors clarity over an optimized sorted-postings iterator.

For a phrase, enumerate positions of the first retained term and binary-search each
other term's positions at the required offset. Repeated terms are checked independently.
With stop words enabled, only retained terms and their distances are enforced. Omitted
word identity and leading/trailing omitted words are not checked. Default stop-word-free
analysis preserves literal normalized token phrases.

## BM25 and top-K

For each distinct query term, use:

`idf = ln(1 + (N - df + 0.5)/(df + 0.5))`

`score += idf * tf * (1.2 + 1) / (tf + 1.2 * (1 - 0.75 + 0.75 * dl/avgdl))`

Repeated query terms contribute once. Query terms are ordered deterministically before
floating-point summation. A document matching any part of an expression is scored using
the union of all terms in that expression. The candidate expression still determines
eligibility; there is no additional Boolean boost.

A bounded Java PriorityQueue keeps the worst retained hit at its root. Equal scores
prefer smaller document IDs. Ranking C candidates across m query terms costs roughly
O(C*m*log D + C*log K); sorting the retained page window adds O(K log K). The page window
is offset+limit, not just limit. Separate page requests can see different generations.

The optimized scoring path prepares postings references and IDF once per query term,
computes length normalization once per candidate, and allocates Hit objects only for
retained candidates. It preserves the independent randomized ranking oracle. The prior
implementation is retained in Git commit `294c29b` for benchmark reproduction.

## Compression and data structure choices

`storage/SegmentCodec.java` stores ascending document ID deltas and positional deltas as
unsigned base-128 varints. Positive IDs through Long.MAX_VALUE fit in at most nine bytes.
The decoder rejects overflow and noncanonical encodings. Strings are strict UTF-8;
counts are bounded by remaining input. The full segment has a SHA-256 integrity trailer.

Hash maps suit exact vocabulary lookup; sorted maps provide deterministic postings and
binary-searchable immutable position arrays/lists support phrase lookup. Heaps suit top-K.
Tries would primarily help prefix expansion, which this API does not offer. Linked lists
would add allocation/pointer overhead without helping random position lookup. Skip pointers
and array-backed postings are plausible future improvements, not implemented features.

Compression reduces disk representation only. Queries currently use decoded in-memory
postings, so there is no direct compressed-query speedup. The benchmark compares equivalent
postings fields and reconstruction checksums with fixed-width IDs/frequencies/positions;
dictionary and original document bytes are excluded from that specific comparison.
