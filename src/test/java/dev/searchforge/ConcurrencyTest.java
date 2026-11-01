package dev.searchforge;

import dev.searchforge.index.Document;
import dev.searchforge.node.SearchEngine;
import org.junit.jupiter.api.Test;
import java.util.*;
import java.util.concurrent.*;
import static org.junit.jupiter.api.Assertions.*;

class ConcurrencyTest {
    @Test void readersNeverObservePartiallyPublishedBatch() throws Exception {
        var engine = new SearchEngine();
        var start = new CyclicBarrier(5);
        var pool = Executors.newFixedThreadPool(5);
        try {
            var tasks = new ArrayList<Future<?>>();
            tasks.add(pool.submit(() -> {
                start.await(10, TimeUnit.SECONDS);
                for (int generation = 1; generation <= 100; generation++) {
                    String word = generation % 2 == 0 ? "even" : "odd";
                    engine.update(List.of(new Document(1, word), new Document(2, word)), Set.of());
                }
                return null;
            }));
            for (int r = 0; r < 4; r++) tasks.add(pool.submit(() -> {
                start.await(10, TimeUnit.SECONDS);
                for (int iteration = 0; iteration < 1000; iteration++) {
                    var result = engine.search("even", 0, 10);
                    int expected = result.generation() != 0 && result.generation() % 2 == 0 ? 2 : 0;
                    assertEquals(expected, result.totalHits());
                    assertEquals(expected, result.hits().size());
                    for (var hit : result.hits()) assertEquals("even", hit.document().text());
                }
                return null;
            }));
            for (var task : tasks) task.get(20, TimeUnit.SECONDS);
            assertEquals(100, engine.snapshot().generation());
        } finally {
            pool.shutdownNow();
            assertTrue(pool.awaitTermination(10, TimeUnit.SECONDS));
        }
    }
}
