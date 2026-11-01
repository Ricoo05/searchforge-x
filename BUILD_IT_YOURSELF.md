# Independent exercises: Milestone 1

## Analyzer
Source: src/main/java/dev/searchforge/analyzer/Analyzer.java.
Concept: identical normalization makes query and document vocabulary compatible.
Trace: `RED the fox` with stop word `the` produces red@0, fox@2.
Exercise: add tests for decomposed accents and non-Latin words.
Debug: compact positions after removing stop words and identify incorrect phrase matches.
Rebuild: implement a tokenizer and position tracking without reading this source.
Interview: why is lowercasing with the host locale dangerous?

## Inverted index
Source: src/main/java/dev/searchforge/index/IndexSnapshot.java.
Concept: map each term to documents and occurrences so retrieval need not scan every text.
Trace: ID 7 `red red fox` produces red->{7:[0,1]}, fox->{7:[2]}, length(7)=3.
Exercise: compute the document frequency and total occurrences of each term.
Debug: deliberately expose a mutable positions list, mutate it, and explain the failure.
Rebuild: implement index construction, replacement and deletion independently.
Interview: compare hash lookup, sorted dictionary lookup and prefix search with a trie.

## Boolean and phrase queries
Sources: query/Query.java and query/Search.java under src/main/java/dev/searchforge.
Concept: expression trees combine sets; positional offsets distinguish phrases.
Trace: `a OR b AND c` becomes OR(a,AND(b,c)).
Exercise: produce a corpus that distinguishes this from AND(OR(a,b),c).
Debug: remove phrase offset checks and find a false positive.
Rebuild: write a precedence parser and phrase matcher without consulting this code.
Interview: why do repeated phrase terms need more than document-level set intersection?

## BM25 and top-K
Source: query/Search.java. Test: src/test/java/dev/searchforge/EngineTest.java.
Concept: rarity, frequency saturation and length normalization contribute to lexical ranking.
Trace: the test's red example uses N=4, df=2, avgdl=2, tf=2, dl=3.
Exercise: vary k1 and b on paper and predict which documents benefit.
Debug: reverse the heap comparator and inspect which hits survive.
Rebuild: score by a brute-force document scan, sort all results, then replace sorting with a heap.
Interview: why can't a coordinator blindly merge scores computed with different shard statistics?

## Publication and concurrency
Source: node/SearchEngine.java. Test: ConcurrencyTest.java.
Concept: a writer builds privately, then atomically publishes an immutable snapshot.
Trace: a malformed document throws before current.set, preserving the prior generation.
Exercise: capture an old snapshot, update the engine and verify the old snapshot is unchanged.
Debug: publish before building all postings and design a reader test that exposes mixed state.
Rebuild: implement atomic batch replacement and a barrier-coordinated concurrency test.
Interview: what does this guarantee within a process, and what does it fail to guarantee after a crash?
