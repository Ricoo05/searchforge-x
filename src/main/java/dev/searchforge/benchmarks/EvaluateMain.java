package dev.searchforge.benchmarks;

import dev.searchforge.analyzer.Analyzer;
import dev.searchforge.distributed.Protocol;
import dev.searchforge.index.*;
import dev.searchforge.query.*;
import java.nio.file.*;
import java.util.*;

/** Reads BEIR JSONL, writes deterministic top-100 runs; metric computation is separate. */
public final class EvaluateMain {
  public static void main(String[] args) throws Exception {
    if (args.length != 3)
      throw new IllegalArgumentException("EvaluateMain corpus.jsonl queries.jsonl output.json");
    var documents = new ArrayList<Document>();
    var external = new HashMap<Long, String>();
    try (var lines = Files.lines(Path.of(args[0]))) {
      for (String line : lines.toList()) {
        var row = Protocol.JSON.readTree(line);
        long id = documents.size() + 1L;
        documents.add(
            new Document(id, row.path("title").asText("") + " " + row.path("text").asText()));
        external.put(id, row.path("_id").asText());
      }
    }
    var analyzer = new Analyzer();
    var index = new IndexSnapshot(documents, analyzer, 1);
    var run = new TreeMap<String, Object>();
    try (var lines = Files.lines(Path.of(args[1]))) {
      for (String line : lines.toList()) {
        var row = Protocol.JSON.readTree(line);
        // Natural-language claims are analyzed as bag-of-words, not Boolean syntax.
        var query = new Query.Terms(analyzer.analyze(row.path("text").asText()), false);
        var result = Search.execute(index, query, 0, 100);
        var hits =
            result.hits().stream()
                .map(hit -> Map.of("id", external.get(hit.id()), "score", hit.score()))
                .toList();
        run.put(row.path("_id").asText(), hits);
      }
    }
    Protocol.JSON.writerWithDefaultPrettyPrinter().writeValue(Path.of(args[2]).toFile(), run);
    System.out.println(
        "evaluation_run_complete documents=" + index.size() + " queries=" + run.size());
  }
}
