package dev.searchforge;

import dev.searchforge.analyzer.Analyzer;
import dev.searchforge.storage.GenerationStore;
import java.nio.file.Path;

public final class CompactionChild {
  public static void main(String[] args) throws Exception {
    try (var store = new GenerationStore(Path.of(args[0]))) {
      store.compact(new Analyzer(), () -> Runtime.getRuntime().halt(74));
    }
    throw new AssertionError("Cleanup crash point not reached");
  }
}
