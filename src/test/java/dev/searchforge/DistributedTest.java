package dev.searchforge;

import dev.searchforge.distributed.*;
import dev.searchforge.index.Document;
import dev.searchforge.node.SearchEngine;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import java.io.*;
import java.net.*;
import java.net.http.*;
import java.nio.file.*;
import java.time.Duration;
import java.util.*;
import java.util.concurrent.*;
import static org.junit.jupiter.api.Assertions.*;

class DistributedTest {
    @TempDir Path directory;
    static final class Child implements AutoCloseable {
        final Process process;
        final URI uri;
        final Capture capture;
        final Path log;
        boolean closed;
        Child(Process process, URI uri, Capture capture, Path log) {
            this.process = process; this.uri = uri; this.capture = capture; this.log = log;
        }
        URI uri() { return uri; }
        @Override public synchronized void close() throws Exception {
            if (closed) return;
            process.destroy();
            if (!process.waitFor(5, TimeUnit.SECONDS)) { process.destroyForcibly(); assertTrue(process.waitFor(5, TimeUnit.SECONDS)); }
            capture.finish(log);
            closed = true;
        }
    }
    // Keep OS-inherited child handles away from temporary log files on Windows.
    // Drain the pipe concurrently, join it, then write a parent-owned diagnostic file.
    record Capture(Thread reader, ByteArrayOutputStream output) {
        static Capture start(Process process) {
            var output = new ByteArrayOutputStream();
            var reader = Thread.ofVirtual().start(() -> {
                try (var input = process.getInputStream()) { input.transferTo(output); }
                catch (IOException ignored) { /* Killing a child may close its pipe during a read. */ }
            });
            return new Capture(reader, output);
        }
        void finish(Path log) throws Exception {
            reader.join(5000);
            assertFalse(reader.isAlive(), "Child log reader did not stop");
            Files.write(log, output.toByteArray());
        }
    }
    private Child start(String name, Class<?> main, String... args) throws Exception {
        Path ready = directory.resolve(name + ".ready"); Files.deleteIfExists(ready);
        var command = new ArrayList<String>();
        command.add(Path.of(System.getProperty("java.home"), "bin", System.getProperty("os.name").startsWith("Windows") ? "java.exe" : "java").toString());
        command.addAll(List.of("-cp", System.getProperty("java.class.path"), main.getName()));
        command.addAll(List.of(args));
        Path log = directory.resolve(name + ".log");
        var process = new ProcessBuilder(command).redirectErrorStream(true).start();
        var capture = Capture.start(process);
        long until = System.nanoTime() + TimeUnit.SECONDS.toNanos(15);
        while (System.nanoTime() < until && process.isAlive()) {
            if (Files.exists(ready)) {
                String port = Files.readString(ready).trim();
                if (!port.isEmpty()) return new Child(process, URI.create("http://127.0.0.1:" + port), capture, log);
            }
            Thread.sleep(20);
        }
        process.destroyForcibly().waitFor();
        capture.finish(log);
        fail("Child failed startup: " + Files.readString(log)); return null;
    }
    private Child worker(int shard, int port) throws Exception {
        return start("worker" + shard, WorkerMain.class, "" + shard, "3", "" + port,
                directory.resolve("data" + shard).toString(), directory.resolve("worker" + shard + ".ready").toString());
    }
    private <T> T post(HttpClient client, URI base, String path, Object body, Class<T> type) throws Exception {
        var request = HttpRequest.newBuilder(base.resolve(path)).timeout(Duration.ofSeconds(10)).header("Content-Type", "application/json")
                .POST(HttpRequest.BodyPublishers.ofByteArray(Protocol.JSON.writeValueAsBytes(body))).build();
        var response = client.send(request, HttpResponse.BodyHandlers.ofByteArray());
        assertEquals(200, response.statusCode(), new String(response.body(), java.nio.charset.StandardCharsets.UTF_8));
        return Protocol.JSON.readValue(response.body(), type);
    }
    @Test void fourRealProcessesMatchSingleNodeThenReportWorkerLossAndRecovery() throws Exception {
        try (var w0 = worker(0, 0); var w1 = worker(1, 0); var w2 = worker(2, 0); var client = HttpClient.newHttpClient(); var local = new SearchEngine()) {
            var docs = new ArrayList<Document>();
            for (long id = 1; id <= 90; id++) docs.add(new Document(id,
                    id % 3 == 0 ? "rare rare red fox" : id % 3 == 1 ? "blue fox jumps " + "blue ".repeat((int)(id % 7)) : "red fox red"));
            local.update(docs, Set.of());
            List<Child> workers = List.of(w0, w1, w2);
            for (int shard = 0; shard < 3; shard++) {
                final int selected = shard;
                var batch = new Protocol.Batch(docs.stream().filter(d -> d.id() % 3 == selected).toList(), Set.of());
                post(client, workers.get(shard).uri(), "/ingest", batch, Map.class);
                post(client, workers.get(shard).uri(), "/ingest", batch, Map.class); // idempotent state on duplicate request
            }
            try (var coordinator = start("coordinator", CoordinatorMain.class, "0", directory.resolve("coordinator.ready").toString(),
                    w0.uri().toString(), w1.uri().toString(), w2.uri().toString())) {
                var routed = post(client, coordinator.uri(), "/ingest", new Protocol.Batch(docs, Set.of()), Map.class);
                assertEquals("COMPLETE", routed.get("status"));
                var diagnostics = post(client, coordinator.uri(), "/diagnostics", Map.of(), List.class);
                assertEquals(3, diagnostics.size());
                for (Object row : diagnostics) assertEquals(30, ((Number)((Map<?, ?>)row).get("documents")).intValue());
                for (String query : List.of("red fox", "rare OR blue", "\"red fox\"", "red AND fox", "absent", "(rare OR blue) AND fox")) {
                    var expected = local.search(query, 3, 12);
                    var actual = post(client, coordinator.uri(), "/search", new Protocol.QueryRequest(query, 3, 12, 5000), Protocol.DistributedResult.class);
                    assertEquals("COMPLETE", actual.status()); assertEquals(expected.totalHits(), actual.totalHits());
                    assertEquals(expected.hits().stream().map(h -> h.id()).toList(), actual.hits().stream().map(h -> h.id()).toList());
                    for (int i = 0; i < expected.hits().size(); i++) assertEquals(expected.hits().get(i).score(), actual.hits().get(i).score(), 1e-12);
                }
                int restartPort = w1.uri().getPort(); w1.close();
                var partial = post(client, coordinator.uri(), "/search", new Protocol.QueryRequest("fox", 0, 10, 5000), Protocol.DistributedResult.class);
                assertEquals("PARTIAL", partial.status()); assertEquals(List.of(1), partial.missingShards());
                assertEquals("available_shards", partial.scoringScope()); assertEquals(60, partial.totalHits());
                var partialWrite = post(client, coordinator.uri(), "/ingest", new Protocol.Batch(List.of(new Document(90, "fox"), new Document(91, "fox")), Set.of()), Map.class);
                assertEquals("PARTIAL", partialWrite.get("status"));
                var writeOutcomes = (List<?>)partialWrite.get("shards");
                assertEquals("ACKNOWLEDGED", ((Map<?, ?>)writeOutcomes.get(0)).get("status"));
                assertEquals("UNKNOWN", ((Map<?, ?>)writeOutcomes.get(1)).get("status"));
                try (var restarted = worker(1, restartPort)) {
                    var restored = post(client, coordinator.uri(), "/search", new Protocol.QueryRequest("fox", 0, 10, 5000), Protocol.DistributedResult.class);
                    assertEquals("COMPLETE", restored.status()); assertEquals(90, restored.totalHits());
                }
            }
        }
    }
    @Test void workerRejectsStaleGenerationAfterUpdate() throws Exception {
        try (var worker = worker(0, 0); var client = HttpClient.newHttpClient()) {
            var stats = post(client, worker.uri(), "/stats", new Protocol.StatsRequest("fox"), Protocol.ShardStats.class);
            post(client, worker.uri(), "/ingest", new Protocol.Batch(List.of(new Document(3, "fox")), Set.of()), Map.class);
            var query = new Protocol.ShardQuery("fox", 10, stats.epoch(), stats.generation(), stats.statistics(), 1000);
            var request = HttpRequest.newBuilder(worker.uri().resolve("/search")).timeout(Duration.ofSeconds(5))
                    .header("Content-Type", "application/json")
                    .POST(HttpRequest.BodyPublishers.ofByteArray(Protocol.JSON.writeValueAsBytes(query))).build();
            assertEquals(409, client.send(request, HttpResponse.BodyHandlers.discarding()).statusCode());
        }
    }
    @Test void invalidNetworkResponseAndSlowWorkerProduceFailedStatusWithinDeadline() throws Exception {
        try (var server = new JsonServer("127.0.0.1", 0)) {
            server.post("/stats", Protocol.StatsRequest.class, ignored -> Map.of("invalid", true)); server.start();
            try (var coordinator = new Coordinator(List.of(URI.create("http://127.0.0.1:" + server.port())))) {
                assertEquals("FAILED", coordinator.search(new Protocol.QueryRequest("fox", 0, 10, 1000)).status());
            }
        }
        try (var server = new JsonServer("127.0.0.1", 0)) {
            server.post("/stats", Protocol.StatsRequest.class, ignored -> { Thread.sleep(1500); return Map.of(); }); server.start();
            try (var coordinator = new Coordinator(List.of(URI.create("http://127.0.0.1:" + server.port())))) {
                long start = System.nanoTime();
                assertEquals("FAILED", coordinator.search(new Protocol.QueryRequest("fox", 0, 10, 200)).status());
                assertTrue(System.nanoTime() - start < TimeUnit.SECONDS.toNanos(2));
            }
        }
    }
    @Test void interruptedCoordinatorCancelsFanout() throws Exception {
        var entered = new CountDownLatch(1); var interrupted = new CountDownLatch(1);
        try (var server = new JsonServer("127.0.0.1", 0)) {
            server.post("/stats", Protocol.StatsRequest.class, ignored -> { entered.countDown(); Thread.sleep(2000); return Map.of(); });
            server.start();
            try (var coordinator = new Coordinator(List.of(URI.create("http://127.0.0.1:" + server.port())))) {
                var caller = Executors.newSingleThreadExecutor();
                try {
                    var task = caller.submit(() -> {
                        try { coordinator.search(new Protocol.QueryRequest("fox", 0, 10, 10000)); }
                        catch (InterruptedException ex) { interrupted.countDown(); Thread.currentThread().interrupt(); }
                    });
                    assertTrue(entered.await(5, TimeUnit.SECONDS)); task.cancel(true);
                    assertTrue(interrupted.await(5, TimeUnit.SECONDS));
                } finally { caller.shutdownNow(); assertTrue(caller.awaitTermination(5, TimeUnit.SECONDS)); }
            }
        }
    }
    @Test void stalledResponseBodyDoesNotBlockCoordinatorShutdown() throws Exception {
        var release = new CountDownLatch(1);
        var entered = new CountDownLatch(1);
        var server = com.sun.net.httpserver.HttpServer.create(new InetSocketAddress("127.0.0.1", 0), 0);
        var handlers = Executors.newCachedThreadPool();
        server.setExecutor(handlers);
        server.createContext("/stats", exchange -> {
            try (exchange) {
                exchange.sendResponseHeaders(200, 1024);
                exchange.getResponseBody().write('{'); exchange.getResponseBody().flush();
                entered.countDown();
                try { release.await(10, TimeUnit.SECONDS); }
                catch (InterruptedException ex) { Thread.currentThread().interrupt(); }
            }
        });
        server.start();
        var coordinator = new Coordinator(List.of(URI.create("http://127.0.0.1:" + server.getAddress().getPort())));
        try {
            assertEquals("FAILED", coordinator.search(new Protocol.QueryRequest("fox", 0, 10, 1000)).status());
            assertTrue(entered.await(2, TimeUnit.SECONDS), "Server must actually start a stalled body");
            assertTimeoutPreemptively(Duration.ofSeconds(7), coordinator::close);
        } finally {
            release.countDown(); server.stop(0); handlers.shutdownNow();
            assertTrue(handlers.awaitTermination(5, TimeUnit.SECONDS)); coordinator.close();
        }
    }
    @Test void boundaryRejectsNullMissingFieldsAndInvalidQuery() throws Exception {
        try (var server = new JsonServer("127.0.0.1", 0); var client = HttpClient.newHttpClient()) {
            server.post("/search", Protocol.QueryRequest.class, request -> {
                dev.searchforge.query.Query.parse(request.query(), new dev.searchforge.analyzer.Analyzer()); return Map.of();
            }); server.start();
            for (String body : List.of("null", "{}", "{\"query\":\"AND fox\",\"offset\":0,\"limit\":10,\"timeoutMillis\":1000}",
                    "{\"query\":\"fox\",\"query\":\"cat\",\"offset\":0,\"limit\":10,\"timeoutMillis\":1000}")) {
                var request = HttpRequest.newBuilder(URI.create("http://127.0.0.1:" + server.port() + "/search"))
                        .timeout(Duration.ofSeconds(5)).header("Content-Type", "application/json").POST(HttpRequest.BodyPublishers.ofString(body)).build();
                assertEquals(400, client.send(request, HttpResponse.BodyHandlers.discarding()).statusCode());
            }
            var plain = HttpRequest.newBuilder(URI.create("http://127.0.0.1:" + server.port() + "/search"))
                    .timeout(Duration.ofSeconds(5)).POST(HttpRequest.BodyPublishers.ofString("{}" )).build();
            assertEquals(415, client.send(plain, HttpResponse.BodyHandlers.discarding()).statusCode());
        }
    }
}
