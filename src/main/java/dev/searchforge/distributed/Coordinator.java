package dev.searchforge.distributed;

import dev.searchforge.node.QueryExecutor;
import dev.searchforge.query.Search;
import java.io.IOException;
import java.net.URI;
import java.net.http.*;
import java.nio.ByteBuffer;
import java.time.Duration;
import java.util.*;
import java.util.concurrent.*;

/** Two-phase fanout; all scoring uses the same statistics and checked generation vector. */
public final class Coordinator implements AutoCloseable {
    private final List<URI> workers;
    private final HttpClient client = HttpClient.newBuilder().connectTimeout(Duration.ofSeconds(2)).build();
    private final QueryExecutor fanout;
    private final Semaphore admission = new Semaphore(8);
    public Coordinator(List<URI> workers) {
        if (workers.isEmpty() || workers.size() > 64 || new HashSet<>(workers).size() != workers.size())
            throw new IllegalArgumentException("Require 1..64 unique worker URLs in shard order");
        for (var uri : workers) if (!Set.of("http", "https").contains(uri.getScheme()) || uri.getHost() == null)
            throw new IllegalArgumentException("Invalid worker URL");
        this.workers = List.copyOf(workers);
        fanout = new QueryExecutor(Math.min(16, workers.size() * 2), 512);
    }
    public Protocol.DistributedResult search(Protocol.QueryRequest request) throws InterruptedException {
        var analyzer = new dev.searchforge.analyzer.Analyzer();
        var parsed = dev.searchforge.query.Query.parse(request.query(), analyzer);
        var expectedTerms = Search.statistics(new dev.searchforge.index.IndexSnapshot(List.of(), analyzer, 0), parsed).documentFrequencies().keySet();
        if (!admission.tryAcquire()) throw new RejectedExecutionException("Coordinator overloaded");
        try {
            long deadline = System.nanoTime() + TimeUnit.MILLISECONDS.toNanos(request.timeoutMillis());
            var statsTasks = new ArrayList<CompletableFuture<Protocol.ShardStats>>();
            // Reserve approximately half the request budget for scoring after statistics.
            long statsDeadline = System.nanoTime() + TimeUnit.MILLISECONDS.toNanos(Math.max(1, request.timeoutMillis() / 2));
            for (URI worker : workers) statsTasks.add(call(worker, "/stats", new Protocol.StatsRequest(request.query()), Protocol.ShardStats.class, statsDeadline));
            var stats = new TreeMap<Integer, Protocol.ShardStats>();
            var missing = new TreeSet<Integer>();
            try {
                for (int shard = 0; shard < workers.size(); shard++) {
                    try {
                        var value = statsTasks.get(shard).get();
                        if (value.shard() != shard || value.shards() != workers.size() || value.generation() < 0 || value.epoch() == null || value.statistics() == null)
                            throw new IOException("Invalid shard identity");
                        if (value.statistics().documents() > Integer.MAX_VALUE
                                || value.statistics().totalLength() > value.statistics().documents() * 1_000_000L)
                            throw new IOException("Implausible corpus statistics");
                        // Validate term keys against the coordinator's identical default analyzer.
                        if (!value.statistics().documentFrequencies().keySet().equals(expectedTerms)) throw new IOException("Statistics vocabulary mismatch");
                        stats.put(shard, value);
                    } catch (ExecutionException | CancellationException | IOException ex) { missing.add(shard); }
                }
            } finally { statsTasks.forEach(task -> task.cancel(true)); }
            if (stats.isEmpty() || System.nanoTime() >= deadline) return failed(missing);
            long documents = 0, length = 0;
            var dfs = new TreeMap<String, Long>();
            for (var shard : stats.values()) {
                documents = Math.addExact(documents, shard.statistics().documents());
                length = Math.addExact(length, shard.statistics().totalLength());
                shard.statistics().documentFrequencies().forEach((term, df) -> dfs.merge(term, df, Math::addExact));
            }
            var global = new Search.Statistics(documents, length, dfs);
            var searches = new TreeMap<Integer, CompletableFuture<Protocol.ShardResult>>();
            for (var entry : stats.entrySet()) {
                var shard = entry.getValue();
                int remaining = (int)Math.max(1, TimeUnit.NANOSECONDS.toMillis(deadline - System.nanoTime()));
                searches.put(entry.getKey(), call(workers.get(entry.getKey()), "/search",
                        new Protocol.ShardQuery(request.query(), request.offset() + request.limit(), shard.epoch(), shard.generation(), global, remaining),
                        Protocol.ShardResult.class, deadline));
            }
            var hits = new ArrayList<Search.Hit>(); var seen = new HashSet<Long>();
            var generations = new TreeMap<Integer, Long>(); long total = 0;
            try {
                for (var entry : searches.entrySet()) {
                    int shardId = entry.getKey(); var expected = stats.get(shardId);
                    try {
                        var value = entry.getValue().get();
                        if (value.shard() != shardId || !expected.epoch().equals(value.epoch()) || value.result() == null
                                || value.result().generation() != expected.generation()) throw new IOException("Shard generation changed");
                        var result = value.result();
                        if (result.totalHits() < 0 || result.totalHits() > expected.statistics().documents()
                                || result.hits().size() != Math.min(result.totalHits(), request.offset() + request.limit()))
                            throw new IOException("Invalid result cardinality");
                        Search.Hit prior = null;
                        for (var hit : result.hits()) {
                            if (hit.id() <= 0 || hit.id() % workers.size() != shardId || !seen.add(hit.id())
                                    || !Double.isFinite(hit.score()) || hit.score() < 0 || hit.document() == null || hit.document().id() != hit.id()
                                    || (prior != null && Search.BEST_FIRST.compare(prior, hit) > 0)) throw new IOException("Invalid shard hit");
                            hits.add(hit); prior = hit;
                        }
                        total += result.totalHits(); generations.put(shardId, expected.generation());
                    } catch (ExecutionException | CancellationException | IOException ex) {
                        missing.add(shardId);
                        // A failed scoring phase invalidates this attempt; no mixed generations are returned.
                        return failed(missing);
                    }
                }
            } finally { searches.values().forEach(task -> task.cancel(true)); }
            if (System.nanoTime() > deadline) return failed(missing);
            hits.sort(Search.BEST_FIRST);
            int from = Math.min(request.offset(), hits.size()), to = Math.min(request.offset() + request.limit(), hits.size());
            return new Protocol.DistributedResult(missing.isEmpty() ? "COMPLETE" : "PARTIAL", missing.isEmpty() ? "all_shards" : "available_shards",
                    List.copyOf(missing), Map.copyOf(generations), total, List.copyOf(hits.subList(from, to)));
        } finally { admission.release(); }
    }
    private Protocol.DistributedResult failed(Set<Integer> missing) {
        return new Protocol.DistributedResult("FAILED", "none", List.copyOf(missing), Map.of(), 0, List.of());
    }
    private <T> CompletableFuture<T> call(URI worker, String path, Object body, Class<T> type, long deadline) {
        long remaining = deadline - System.nanoTime();
        if (remaining <= 0) return CompletableFuture.failedFuture(new TimeoutException());
        try {
            return fanout.submit(() -> {
                long left = deadline - System.nanoTime();
                if (left <= 0) throw new TimeoutException();
                var request = HttpRequest.newBuilder(worker.resolve(path)).timeout(Duration.ofNanos(left))
                        .header("Content-Type", "application/json").POST(HttpRequest.BodyPublishers.ofByteArray(Protocol.JSON.writeValueAsBytes(body))).build();
                var response = client.send(request, ignored -> new LimitedBody());
                if (response.statusCode() != 200) throw new IOException("Worker HTTP " + response.statusCode());
                return Protocol.JSON.readValue(response.body(), type);
            }, Duration.ofNanos(remaining));
        } catch (RejectedExecutionException ex) { return CompletableFuture.failedFuture(ex); }
    }
    private static final class LimitedBody implements HttpResponse.BodySubscriber<byte[]> {
        private final HttpResponse.BodySubscriber<byte[]> delegate = HttpResponse.BodySubscribers.ofByteArray();
        private Flow.Subscription subscription; private long length; private boolean rejected;
        public CompletionStage<byte[]> getBody() { return delegate.getBody(); }
        public void onSubscribe(Flow.Subscription s) { subscription = s; delegate.onSubscribe(s); }
        public void onNext(List<ByteBuffer> buffers) {
            if (rejected) return;
            for (var b : buffers) length += b.remaining();
            if (length > 8 * 1024 * 1024) { rejected = true; subscription.cancel(); delegate.onError(new IOException("Worker response exceeds 8 MiB")); }
            else delegate.onNext(buffers);
        }
        public void onError(Throwable error) { if (!rejected) delegate.onError(error); }
        public void onComplete() { if (!rejected) delegate.onComplete(); }
    }
    @Override public void close() { fanout.close(); client.close(); }
}
