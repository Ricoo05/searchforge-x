package dev.searchforge;

import static org.junit.jupiter.api.Assertions.*;

import dev.searchforge.index.Document;
import dev.searchforge.node.SearchEngine;
import java.util.*;
import java.util.concurrent.*;
import org.junit.jupiter.api.Test;

class ConcurrencyTest {
  @Test
  void concurrentWritersNeverLoseAcceptedUpdates() throws Exception {
    var engine = new SearchEngine();
    var start = new CyclicBarrier(4);
    var pool = Executors.newFixedThreadPool(4);
    try {
      var jobs = new ArrayList<Future<?>>();
      for (int writer = 0; writer < 4; writer++) {
        final int selected = writer;
        jobs.add(
            pool.submit(
                () -> {
                  start.await(5, TimeUnit.SECONDS);
                  for (int sequence = 1; sequence <= 50; sequence++)
                    engine.update(
                        List.of(new Document(selected * 50L + sequence, "accepted")), Set.of());
                  return null;
                }));
      }
      for (var job : jobs) job.get(20, TimeUnit.SECONDS);
      assertEquals(200, engine.snapshot().generation());
      assertEquals(200, engine.search("accepted", 0, 1000).totalHits());
    } finally {
      pool.shutdownNow();
      assertTrue(pool.awaitTermination(5, TimeUnit.SECONDS));
    }
  }

  @Test
  void readersNeverObservePartiallyPublishedBatch() throws Exception {
    var engine = new SearchEngine();
    var start = new CyclicBarrier(5);
    var pool = Executors.newFixedThreadPool(5);
    try {
      var tasks = new ArrayList<Future<?>>();
      tasks.add(
          pool.submit(
              () -> {
                start.await(10, TimeUnit.SECONDS);
                for (int generation = 1; generation <= 100; generation++) {
                  String word = generation % 2 == 0 ? "even" : "odd";
                  engine.update(List.of(new Document(1, word), new Document(2, word)), Set.of());
                }
                return null;
              }));
      for (int r = 0; r < 4; r++)
        tasks.add(
            pool.submit(
                () -> {
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
