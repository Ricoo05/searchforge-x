package dev.searchforge;

import dev.searchforge.analyzer.Analyzer;
import dev.searchforge.index.Document;
import dev.searchforge.node.SearchEngine;
import dev.searchforge.storage.GenerationStore;
import java.nio.file.Path;
import java.util.*;

/** Separate JVM deliberately terminates without close/shutdown hooks. */
public final class CrashChild {
  public static void main(String[] args) throws Exception {
    var target = GenerationStore.Stage.valueOf(args[1]);
    try (var engine =
        new SearchEngine(
            new Analyzer(),
            new GenerationStore(
                Path.of(args[0]),
                stage -> {
                  if (stage == target) Runtime.getRuntime().halt(73);
                }))) {
      engine.update(List.of(new Document(1, "replacement"), new Document(3, "new")), Set.of(2L));
    }
    throw new AssertionError("Crash hook not reached");
  }
}
