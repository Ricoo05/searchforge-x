package dev.searchforge.node;

import dev.searchforge.analyzer.Analyzer;
import dev.searchforge.index.*;
import dev.searchforge.query.*;
import java.util.*;
import java.util.concurrent.atomic.AtomicReference;
import dev.searchforge.storage.GenerationStore;
import java.io.IOException;

/** One serialized writer; readers capture a complete immutable generation. */
public final class SearchEngine implements AutoCloseable {
    private final Analyzer analyzer;
    private final AtomicReference<IndexSnapshot> current;
    private final GenerationStore store;
    private volatile boolean unavailable;
    public SearchEngine() { this(new Analyzer()); }
    public SearchEngine(Analyzer analyzer) {
        this.analyzer = Objects.requireNonNull(analyzer);
        store = null;
        current = new AtomicReference<>(new IndexSnapshot(List.of(), analyzer, 0));
    }
    public SearchEngine(Analyzer analyzer, GenerationStore store) throws IOException {
        this.analyzer = Objects.requireNonNull(analyzer);
        this.store = Objects.requireNonNull(store);
        try { current = new AtomicReference<>(store.recover(analyzer)); }
        catch (IOException | RuntimeException ex) { store.close(); throw ex; }
    }
    public IndexSnapshot snapshot() { ensureAvailable(); return current.get(); }
    public synchronized long update(List<Document> upserts, Set<Long> deletes) {
        ensureAvailable();
        var prior = current.get();
        var docs = new TreeMap<>(prior.documents());
        var seen = new HashSet<Long>();
        for (long id : deletes) {
            if (id <= 0) throw new IllegalArgumentException("Invalid deleted ID");
            docs.remove(id);
        }
        for (Document doc : upserts) {
            if (!seen.add(doc.id()) || deletes.contains(doc.id()))
                throw new IllegalArgumentException("Ambiguous batch: repeated or deleted upsert ID");
            docs.put(doc.id(), doc);
        }
        var next = new IndexSnapshot(docs.values(), analyzer, Math.incrementExact(prior.generation()));
        if (store != null) {
            try { store.commit(next); }
            catch (IOException ex) {
                unavailable = true;
                throw new java.io.UncheckedIOException("Commit outcome may be uncertain; reopen and recover", ex);
            }
        }
        current.set(next);
        return next.generation();
    }
    public Search.Result search(String query, int offset, int limit) {
        ensureAvailable();
        return Search.execute(current.get(), Query.parse(query, analyzer), offset, limit);
    }
    private void ensureAvailable() { if (unavailable) throw new IllegalStateException("Engine closed or requires recovery"); }
    public synchronized void compact() throws IOException {
        ensureAvailable();
        if (store != null) store.compact(analyzer, () -> {});
    }
    /** Exact text equality; reports IDs instead of silently discarding user documents. */
    public List<Long> duplicatesOf(String text) {
        ensureAvailable();
        Analyzer.validateText(text);
        return current.get().documents().values().stream().filter(d -> d.text().equals(text)).map(Document::id).toList();
    }
    @Override public synchronized void close() throws IOException {
        unavailable = true;
        if (store != null) store.close();
    }
}
