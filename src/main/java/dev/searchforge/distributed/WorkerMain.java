package dev.searchforge.distributed;

import dev.searchforge.analyzer.Analyzer;
import dev.searchforge.node.*;
import dev.searchforge.query.*;
import dev.searchforge.storage.GenerationStore;
import java.nio.file.*;
import java.time.Duration;
import java.util.*;
import java.util.concurrent.*;

public final class WorkerMain {
    public static void main(String[] args) throws Exception {
        if (args.length != 5) throw new IllegalArgumentException("Usage: WorkerMain shard shardCount port dataDirectory readyFile");
        int shard = Integer.parseInt(args[0]), shards = Integer.parseInt(args[1]);
        if (shards < 1 || shards > 64 || shard < 0 || shard >= shards) throw new IllegalArgumentException("Invalid shard assignment");
        var analyzer = new Analyzer();
        var engine = new SearchEngine(analyzer, new GenerationStore(Path.of(args[3])));
        if (engine.snapshot().documents().keySet().stream().anyMatch(id -> id % shards != shard)) {
            engine.close(); throw new IllegalArgumentException("Stored documents conflict with shard assignment");
        }
        var queries = new QueryExecutor(4, 32);
        var server = new JsonServer(System.getenv().getOrDefault("SEARCHFORGE_BIND", "127.0.0.1"), Integer.parseInt(args[2]));
        String epoch = UUID.randomUUID().toString();
        server.post("/stats", Protocol.StatsRequest.class, request -> {
            var index = engine.snapshot();
            return new Protocol.ShardStats(shard, shards, epoch, index.generation(), Search.statistics(index, Query.parse(request.query(), analyzer)));
        });
        server.post("/search", Protocol.ShardQuery.class, request -> {
            if (request.timeoutMillis() < 1 || request.timeoutMillis() > 30000) throw new IllegalArgumentException("Invalid timeout");
            var future = queries.submit(() -> {
                var index = engine.snapshot();
                if (!epoch.equals(request.epoch()) || index.generation() != request.generation()) throw new IllegalStateException("Snapshot changed");
                var result = Search.execute(index, Query.parse(request.query(), analyzer), 0, request.topK(), request.statistics());
                var snippets = result.hits().stream().map(hit -> {
                    String text = hit.document().text();
                    int end = text.offsetByCodePoints(0, Math.min(512, text.codePointCount(0, text.length())));
                    return new Search.Hit(hit.id(), hit.score(), new dev.searchforge.index.Document(hit.id(), text.substring(0, end)));
                }).toList();
                return new Protocol.ShardResult(shard, epoch, new Search.Result(result.generation(), result.totalHits(), snippets));
            }, Duration.ofMillis(request.timeoutMillis()));
            try { return future.get(); }
            catch (ExecutionException ex) {
                if (ex.getCause() instanceof Exception cause) throw cause;
                throw ex;
            } catch (InterruptedException ex) { future.cancel(true); Thread.currentThread().interrupt(); throw ex; }
        });
        server.post("/ingest", Protocol.Batch.class, request -> {
            for (var document : request.upserts()) if (document.id() % shards != shard) throw new IllegalArgumentException("Document belongs to another shard");
            for (long id : request.deletes()) if (id <= 0 || id % shards != shard) throw new IllegalArgumentException("Document belongs to another shard");
            return Map.of("generation", engine.update(request.upserts(), request.deletes()));
        });
        server.post("/health", Map.class, ignored -> Map.of("status", "ready", "shard", shard, "generation", engine.snapshot().generation()));
        Runtime.getRuntime().addShutdownHook(new Thread(() -> {
            server.close(); queries.close();
            try { engine.close(); } catch (Exception ex) { System.err.println("close_failed"); }
        }));
        server.start(); Files.writeString(Path.of(args[4]), Integer.toString(server.port()));
        System.out.printf(Locale.ROOT, "worker_ready shard=%d port=%d%n", shard, server.port());
        new CountDownLatch(1).await();
    }
}
