package dev.searchforge;

import dev.searchforge.index.Document;
import dev.searchforge.node.SearchEngine;
import java.util.*;

public final class Demo {
    public static void main(String[] args) {
        var engine = new SearchEngine();
        engine.update(List.of(new Document(1, "Distributed search uses an inverted index."),
                new Document(2, "An inverted index maps terms to documents."),
                new Document(3, "Storage recovery requires careful testing.")), Set.of());
        String query = args.length == 0 ? "\"inverted index\"" : String.join(" ", args);
        var result = engine.search(query, 0, 10);
        System.out.printf(Locale.ROOT, "query=%s generation=%d matches=%d%n", query, result.generation(), result.totalHits());
        for (var hit : result.hits()) System.out.printf(Locale.ROOT, "%d %.6f %s%n", hit.id(), hit.score(), hit.document().text());
    }
}
