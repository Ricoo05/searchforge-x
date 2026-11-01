package dev.searchforge.index;

import dev.searchforge.analyzer.Analyzer;
import java.util.*;

/** Immutable, fully owned snapshot. No mutable collection escapes construction. */
public final class IndexSnapshot {
    private final NavigableMap<Long, Document> documents;
    private final Map<String, NavigableMap<Long, List<Integer>>> postings;
    private final Map<Long, Integer> lengths;
    private final long totalLength;
    private final long generation;
    private final Set<String> stopWords;

    public IndexSnapshot(Collection<Document> source, Analyzer analyzer, long generation) {
        stopWords = analyzer.stopWords();
        var docs = new TreeMap<Long, Document>();
        var lists = new HashMap<String, NavigableMap<Long, List<Integer>>>();
        var sizes = new HashMap<Long, Integer>();
        long total = 0;
        for (Document doc : source) {
            if (docs.putIfAbsent(doc.id(), doc) != null) throw new IllegalArgumentException("Duplicate document ID");
            var tokens = analyzer.analyze(doc.text());
            sizes.put(doc.id(), tokens.size());
            total += tokens.size();
            for (var token : tokens) lists.computeIfAbsent(token.term(), ignored -> new TreeMap<>())
                    .computeIfAbsent(doc.id(), ignored -> new ArrayList<>()).add(token.position());
        }
        var frozen = new HashMap<String, NavigableMap<Long, List<Integer>>>();
        lists.forEach((term, byDoc) -> {
            var owned = new TreeMap<Long, List<Integer>>();
            byDoc.forEach((id, positions) -> owned.put(id, List.copyOf(positions)));
            frozen.put(term, Collections.unmodifiableNavigableMap(owned));
        });
        documents = Collections.unmodifiableNavigableMap(docs);
        postings = Map.copyOf(frozen);
        lengths = Map.copyOf(sizes);
        totalLength = total;
        this.generation = generation;
    }
    public NavigableMap<Long, Document> documents() { return documents; }
    public NavigableMap<Long, List<Integer>> postings(String term) {
        return postings.getOrDefault(term, Collections.emptyNavigableMap());
    }
    public Set<String> terms() { return postings.keySet(); }
    public int length(long id) { return lengths.getOrDefault(id, 0); }
    public int size() { return documents.size(); }
    public long totalLength() { return totalLength; }
    public long generation() { return generation; }
    public Set<String> stopWords() { return stopWords; }
}
