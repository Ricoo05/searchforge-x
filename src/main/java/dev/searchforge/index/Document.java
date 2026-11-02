package dev.searchforge.index;

import java.util.Map;
import java.util.Objects;

/** Caller-assigned positive IDs stay stable across replacements. */
public record Document(long id, String text, Map<String, String> metadata) {
  public Document {
    if (id <= 0) throw new IllegalArgumentException("Document ID must be positive");
    Objects.requireNonNull(text, "text");
    dev.searchforge.analyzer.Analyzer.validateText(text);
    if (text.length() > 1_000_000)
      throw new IllegalArgumentException("Document exceeds 1M UTF-16 units");
    metadata = Map.copyOf(metadata);
    if (metadata.size() > 100) throw new IllegalArgumentException("At most 100 metadata fields");
    for (var entry : metadata.entrySet()) {
      if (entry.getKey().length() > 256 || entry.getValue().length() > 16384)
        throw new IllegalArgumentException("Metadata field exceeds limit");
      dev.searchforge.analyzer.Analyzer.validateText(entry.getKey());
      dev.searchforge.analyzer.Analyzer.validateText(entry.getValue());
    }
  }

  public Document(long id, String text) {
    this(id, text, Map.of());
  }
}
