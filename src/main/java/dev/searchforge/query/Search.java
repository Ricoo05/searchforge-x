package dev.searchforge.query;

import dev.searchforge.index.*;
import java.util.*;

public final class Search {
  private Search() {}

  public record Hit(long id, double score, Document document) {}

  public record Result(long generation, int totalHits, List<Hit> hits) {
    public Result {
      hits = List.copyOf(hits);
    }
  }

  public static final Comparator<Hit> BEST_FIRST =
      Comparator.comparingDouble(Hit::score).reversed().thenComparingLong(Hit::id);

  public static Result execute(IndexSnapshot index, Query query, int offset, int limit) {
    return execute(index, query, offset, limit, statistics(index, query));
  }

  public record Statistics(
      long documents, long totalLength, Map<String, Long> documentFrequencies) {
    public Statistics {
      documentFrequencies = Map.copyOf(documentFrequencies);
      if (documents < 0 || totalLength < 0 || (documents == 0 && totalLength != 0))
        throw new IllegalArgumentException("Invalid corpus statistics");
      for (long df : documentFrequencies.values())
        if (df < 0 || df > documents)
          throw new IllegalArgumentException("Invalid document frequency");
    }
  }

  public static Statistics statistics(IndexSnapshot index, Query query) {
    var terms = new TreeSet<String>();
    collectTerms(query, terms);
    var frequencies = new TreeMap<String, Long>();
    for (String term : terms) frequencies.put(term, (long) index.postings(term).size());
    return new Statistics(index.size(), index.totalLength(), frequencies);
  }

  public static Result execute(
      IndexSnapshot index, Query query, int offset, int limit, Statistics statistics) {
    if (offset < 0 || limit < 1 || limit > 1000 || offset > 10000 - limit)
      throw new IllegalArgumentException("Require limit 1..1000 and offset+limit <=10000");
    Set<Long> candidates = evaluate(index, query);
    var terms = new TreeSet<String>();
    collectTerms(query, terms);
    // Prepare corpus-wide constants once per term rather than once per candidate.
    var prepared = new ArrayList<PreparedTerm>();
    for (String term : terms) {
      var postings = index.postings(term);
      Long df = statistics.documentFrequencies().get(term);
      if (df == null
          || df < postings.size()
          || statistics.documents() < index.size()
          || statistics.totalLength() < index.totalLength())
        throw new IllegalArgumentException("Incompatible global statistics");
      prepared.add(
          new PreparedTerm(postings, Math.log1p((statistics.documents() - df + 0.5) / (df + 0.5))));
    }
    double averageLength =
        statistics.documents() == 0
            ? 0
            : (double) statistics.totalLength() / statistics.documents();
    var heap = new PriorityQueue<Hit>(BEST_FIRST.reversed());
    int capacity = offset + limit;
    for (long id : candidates) {
      if (Thread.currentThread().isInterrupted())
        throw new java.util.concurrent.CancellationException();
      double score = 0;
      double normalization = 1.2 * (1 - 0.75 + 0.75 * index.length(id) / averageLength);
      for (var term : prepared) {
        var positions = term.postings().get(id);
        int tf = positions == null ? 0 : positions.size();
        if (tf > 0) score += term.idf() * tf * (1.2 + 1) / (tf + normalization);
      }
      if (heap.size() < capacity) heap.add(new Hit(id, score, index.documents().get(id)));
      else if (score > heap.peek().score()
          || (score == heap.peek().score() && id < heap.peek().id())) {
        heap.poll();
        heap.add(new Hit(id, score, index.documents().get(id)));
      }
    }
    var sorted = new ArrayList<>(heap);
    sorted.sort(BEST_FIRST);
    return new Result(
        index.generation(),
        candidates.size(),
        sorted.subList(Math.min(offset, sorted.size()), sorted.size()));
  }

  private record PreparedTerm(NavigableMap<Long, List<Integer>> postings, double idf) {}

  public static double bm25(int tf, long df, long count, int length, double averageLength) {
    if (tf == 0) return 0;
    double idf = Math.log1p((count - df + 0.5) / (df + 0.5));
    double k1 = 1.2, b = 0.75;
    return idf * tf * (k1 + 1) / (tf + k1 * (1 - b + b * length / averageLength));
  }

  private static void collectTerms(Query q, Set<String> terms) {
    switch (q) {
      case Query.Terms t -> t.tokens().forEach(token -> terms.add(token.term()));
      case Query.And a -> {
        collectTerms(a.left(), terms);
        collectTerms(a.right(), terms);
      }
      case Query.Or o -> {
        collectTerms(o.left(), terms);
        collectTerms(o.right(), terms);
      }
    }
  }

  private static Set<Long> evaluate(IndexSnapshot index, Query query) {
    checkInterrupted();
    return switch (query) {
      case Query.And a -> {
        Set<Long> left = evaluate(index, a.left());
        left.retainAll(evaluate(index, a.right()));
        yield left;
      }
      case Query.Or o -> {
        Set<Long> left = evaluate(index, o.left());
        left.addAll(evaluate(index, o.right()));
        yield left;
      }
      case Query.Terms t -> {
        var ids = new TreeSet<Long>();
        if (!t.tokens().isEmpty()) {
          if (t.phrase()) {
            var first = t.tokens().getFirst();
            for (var entry : index.postings(first.term()).entrySet()) {
              checkInterrupted();
              for (int start : entry.getValue()) {
                checkInterrupted();
                boolean match = true;
                for (var token : t.tokens()) {
                  var positions =
                      index.postings(token.term()).getOrDefault(entry.getKey(), List.of());
                  if (Collections.binarySearch(
                          positions, start + token.position() - first.position())
                      < 0) {
                    match = false;
                    break;
                  }
                }
                if (match) {
                  ids.add(entry.getKey());
                  break;
                }
              }
            }
          } else for (var token : t.tokens()) ids.addAll(index.postings(token.term()).keySet());
        }
        yield ids;
      }
    };
  }

  private static void checkInterrupted() {
    if (Thread.currentThread().isInterrupted())
      throw new java.util.concurrent.CancellationException();
  }
}
