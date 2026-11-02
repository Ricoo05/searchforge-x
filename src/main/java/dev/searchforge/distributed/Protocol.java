package dev.searchforge.distributed;

import com.fasterxml.jackson.core.StreamReadFeature;
import com.fasterxml.jackson.databind.*;
import com.fasterxml.jackson.databind.json.JsonMapper;
import dev.searchforge.index.Document;
import dev.searchforge.query.Search;
import java.util.*;

public final class Protocol {
  private Protocol() {}

  public static final ObjectMapper JSON =
      JsonMapper.builder()
          .disable(DeserializationFeature.ACCEPT_FLOAT_AS_INT)
          .enable(DeserializationFeature.FAIL_ON_TRAILING_TOKENS)
          .enable(DeserializationFeature.FAIL_ON_NULL_FOR_PRIMITIVES)
          .enable(DeserializationFeature.FAIL_ON_MISSING_CREATOR_PROPERTIES)
          .enable(DeserializationFeature.FAIL_ON_NULL_CREATOR_PROPERTIES)
          .enable(StreamReadFeature.STRICT_DUPLICATE_DETECTION)
          .build();

  public record QueryRequest(String query, int offset, int limit, int timeoutMillis) {
    public QueryRequest {
      Objects.requireNonNull(query);
      if (query.length() > 4096
          || offset < 0
          || limit < 1
          || offset + (long) limit > 1000
          || timeoutMillis < 1
          || timeoutMillis > 30000) throw new IllegalArgumentException("Invalid query limits");
    }
  }

  public record StatsRequest(String query) {}

  public record ShardStats(
      int shard, int shards, String epoch, long generation, Search.Statistics statistics) {}

  public record ShardQuery(
      String query,
      int topK,
      String epoch,
      long generation,
      Search.Statistics statistics,
      int timeoutMillis) {}

  public record ShardResult(int shard, String epoch, Search.Result result) {}

  public record Batch(List<Document> upserts, Set<Long> deletes) {
    public Batch {
      upserts = List.copyOf(upserts);
      deletes = Set.copyOf(deletes);
      if (upserts.size() + deletes.size() > 1000)
        throw new IllegalArgumentException("Batch exceeds 1000 operations");
    }
  }

  public record DistributedResult(
      String status,
      String scoringScope,
      List<Integer> missingShards,
      Map<Integer, Long> generations,
      long totalHits,
      List<Search.Hit> hits) {}
}
