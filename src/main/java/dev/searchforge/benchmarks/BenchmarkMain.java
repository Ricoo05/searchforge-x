package dev.searchforge.benchmarks;

import dev.searchforge.analyzer.Analyzer;
import dev.searchforge.distributed.Protocol;
import dev.searchforge.index.*;
import dev.searchforge.query.*;
import dev.searchforge.storage.*;
import java.io.*;
import java.lang.management.ManagementFactory;
import java.nio.file.*;
import java.util.*;
import java.util.concurrent.*;

/** Small reproducible harness, not a replacement for JMH or a production load test. */
public final class BenchmarkMain {
  private static volatile long sink;

  public static List<Document> corpus(int count) {
    var random = new Random(20261102);
    var docs = new ArrayList<Document>();
    for (long id = 1; id <= count; id++) {
      var text = new StringBuilder("common search ");
      for (int i = 0; i < 30; i++)
        text.append('t').append((int) (Math.pow(random.nextDouble(), 2) * 512)).append(' ');
      docs.add(new Document(id, text.toString()));
    }
    return docs;
  }

  private static final String[] QUERIES = {
    "common", "t511", "t200 t300", "t1 AND t400", "\"common search\"", "t17 OR t99", "absent"
  };

  public static void main(String[] args) throws Exception {
    if (args.length != 4)
      throw new IllegalArgumentException(
          "BenchmarkMain documents samples outputJson revisionLabel");
    int count = Integer.parseInt(args[0]), samples = Integer.parseInt(args[1]);
    if (count < 1 || count > 100000 || samples < 100)
      throw new IllegalArgumentException("Invalid benchmark size");
    var docs = corpus(count);
    var analyzer = new Analyzer();
    long start = System.nanoTime();
    var index = new IndexSnapshot(docs, analyzer, 1);
    long buildNanos = System.nanoTime() - start;
    var queries = Arrays.stream(QUERIES).map(q -> Query.parse(q, analyzer)).toList();
    for (int i = 0; i < 150; i++)
      consume(Search.execute(index, queries.get(i % queries.size()), 0, 10));
    var latencies = new ArrayList<Long>();
    long runStart = System.nanoTime();
    for (int i = 0; i < samples; i++) {
      start = System.nanoTime();
      consume(Search.execute(index, queries.get(i % queries.size()), 0, 10));
      latencies.add(System.nanoTime() - start);
    }
    long queryNanos = System.nanoTime() - runStart;
    long heapUsed = ManagementFactory.getMemoryMXBean().getHeapMemoryUsage().getUsed();
    var pool = Executors.newFixedThreadPool(4);
    long concurrentStart = System.nanoTime();
    try {
      var jobs = new ArrayList<Future<Long>>();
      for (int thread = 0; thread < 4; thread++) {
        final int shift = thread;
        jobs.add(
            pool.submit(
                () -> {
                  long checksum = 0;
                  for (int i = 0; i < samples / 4; i++)
                    checksum +=
                        Search.execute(index, queries.get((i + shift) % queries.size()), 0, 10)
                            .totalHits();
                  return checksum;
                }));
      }
      for (var job : jobs) sink += job.get();
    } finally {
      pool.shutdownNow();
      if (!pool.awaitTermination(10, TimeUnit.SECONDS))
        throw new IllegalStateException("Benchmark pool stuck");
    }
    long concurrentNanos = System.nanoTime() - concurrentStart;
    start = System.nanoTime();
    byte[] encoded = SegmentCodec.encode(index);
    long encodeNanos = System.nanoTime() - start;
    start = System.nanoTime();
    var restored = SegmentCodec.decode(encoded, analyzer);
    long recoverNanos = System.nanoTime() - start;
    if (restored.size() != count) throw new AssertionError("Round trip failed");
    var compression = compression(index);
    Path storage = Files.createTempDirectory("searchforge-benchmark-");
    long commitNanos;
    try (var store = new GenerationStore(storage)) {
      start = System.nanoTime();
      store.commit(index);
      commitNanos = System.nanoTime() - start;
    } finally {
      try (var files = Files.list(storage)) {
        for (var path : files.toList()) Files.delete(path);
      }
      Files.delete(storage);
    }
    var result = new TreeMap<String, Object>();
    result.put("revisionLabel", args[3]);
    result.put("documents", count);
    result.put("seed", 20261102);
    result.put("tokensPerDocument", 32);
    result.put("warmupQueries", 150);
    result.put("samples", samples);
    result.put("queryMix", QUERIES);
    result.put("queryLatenciesNanos", latencies);
    result.put("queryElapsedNanos", queryNanos);
    result.put("concurrentQueries", samples / 4 * 4);
    result.put("concurrentThreads", 4);
    result.put("concurrentElapsedNanos", concurrentNanos);
    result.put("indexBuildNanos", buildNanos);
    result.put("heapUsedAfterQueriesBytes", heapUsed);
    result.put("segmentBytes", encoded.length);
    result.put("segmentEncodeNanos", encodeNanos);
    result.put("recoveryNanos", recoverNanos);
    result.put("forcedCommitNanos", commitNanos);
    result.put("postingsCompression", compression);
    result.put("java", System.getProperty("java.runtime.version"));
    result.put("os", System.getProperty("os.name"));
    result.put("processors", Runtime.getRuntime().availableProcessors());
    result.put("jvmArguments", ManagementFactory.getRuntimeMXBean().getInputArguments());
    result.put("timestampUtc", java.time.Instant.now().toString());
    Path output = Path.of(args[2]);
    Files.createDirectories(output.toAbsolutePath().getParent());
    Protocol.JSON.writerWithDefaultPrettyPrinter().writeValue(output.toFile(), result);
    System.out.println("benchmark_complete documents=" + count + " output=" + output);
  }

  private static void consume(Search.Result r) {
    sink += r.totalHits() + (r.hits().isEmpty() ? 0 : r.hits().getFirst().id());
  }

  public static Map<String, Object> compression(IndexSnapshot index) throws IOException {
    var output = new TreeMap<String, Object>();
    Long expectedChecksum = null;
    for (boolean compressed : List.of(false, true)) {
      var bytes = new ByteArrayOutputStream();
      var out = new DataOutputStream(bytes);
      long start = System.nanoTime();
      for (String term : new TreeSet<>(index.terms())) {
        if (compressed) SegmentCodec.writeVar(out, index.postings(term).size());
        else out.writeInt(index.postings(term).size());
        long lastId = 0;
        for (var entry : index.postings(term).entrySet()) {
          if (compressed) {
            SegmentCodec.writeVar(out, entry.getKey() - lastId);
            SegmentCodec.writeVar(out, entry.getValue().size());
          } else {
            out.writeLong(entry.getKey());
            out.writeInt(entry.getValue().size());
          }
          lastId = entry.getKey();
          int lastPosition = 0;
          for (int position : entry.getValue()) {
            if (compressed) SegmentCodec.writeVar(out, position - lastPosition);
            else out.writeInt(position);
            lastPosition = position;
          }
        }
      }
      long encode = System.nanoTime() - start;
      byte[] data = bytes.toByteArray();
      long checksum = 0;
      start = System.nanoTime();
      var in = new DataInputStream(new ByteArrayInputStream(data));
      while (in.available() > 0) {
        long df = compressed ? SegmentCodec.readVar(in) : in.readInt(), id = 0;
        for (long posting = 0; posting < df; posting++) {
          id = compressed ? id + SegmentCodec.readVar(in) : in.readLong();
          long tf = compressed ? SegmentCodec.readVar(in) : in.readInt();
          checksum += id + tf;
          long position = 0;
          for (long i = 0; i < tf; i++) {
            position = compressed ? position + SegmentCodec.readVar(in) : in.readInt();
            checksum += position;
          }
        }
      }
      long decode = System.nanoTime() - start;
      sink += checksum;
      if (expectedChecksum != null && checksum != expectedChecksum)
        throw new IOException("Postings codecs disagree");
      expectedChecksum = checksum;
      output.put(
          compressed ? "deltaVarint" : "fixedWidth",
          Map.of("bytes", data.length, "encodeNanos", encode, "decodeNanos", decode));
    }
    return output;
  }
}
