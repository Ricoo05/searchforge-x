# DSA concepts in SearchForge X

Arrays/lists hold ordered token occurrences and positions. Indexing a position is O(1)
for array-backed lists; inserting in the middle shifts later elements. Exercise: construct
the positions of `a b a a`, then implement binary search for position 2.

Hash maps provide expected O(1) term lookup with hashing/equality costs. Exercise: build a
term dictionary and explain collisions. Debug a mutable key whose hash changes after insertion.

Balanced search trees maintain posting IDs in sorted order with O(log D) insertion/lookup.
Exercise: merge two sorted posting lists with a two-pointer algorithm. Compare its allocation
and complexity with this implementation's TreeSet union/intersection.

Linked lists make local node insertion cheap but require pointer traversal and extra nodes.
No subsystem uses them as its posting representation. Explain why indexed access and cache
locality usually matter more than constant-time insertion for immutable postings.

Heaps retain top-K in O(C log K) selection work. Exercise: keep the smallest three numbers
from a stream using a max heap; then map that logic to highest scores with ID tie-breaking.
Debug a comparator that retains the worst documents instead.

Sorting enables deterministic output and binary search. Exercise: sort by descending score,
ascending ID, including exact ties. Explain why sorting only by score can make pagination unstable.

Trees represent query syntax. Exercise: draw and evaluate `a OR b AND c`; write a recursive
parser with precedence. Debug missing-operand and unclosed-parenthesis errors.

Tries support prefix lookup in O(prefix length + output) traversal work. Prefix queries are
not implemented here. Build a small standalone trie and explain when its memory cost would
be justified instead of a hash dictionary.

String processing normalizes Unicode and matches token boundaries. Exercise: compare token
positions before/after stop-word removal. Explain why naive substring matching is not phrase
matching and why ASCII-only tokenization changes an international corpus.

Compression uses gaps and base-128 digits. Exercise: encode 127, 128, 16383 and 16384 by hand;
decode truncated input; prove why strictly increasing positive IDs create positive deltas.

For each exercise, state time and space in terms of documents D, token occurrences T,
candidate count C, query terms m and result window K. Distinguish expected hash behavior,
worst-case tree work, measured wall time and JVM object overhead.
