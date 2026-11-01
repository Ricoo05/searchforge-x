package dev.searchforge.distributed;
import java.net.URI;
import java.nio.file.*;
import java.util.*;
import java.util.concurrent.CountDownLatch;
public final class CoordinatorMain {
    public static void main(String[] args) throws Exception {
        if (args.length < 3) throw new IllegalArgumentException("Usage: CoordinatorMain port readyFile workerUrl...");
        var coordinator = new Coordinator(Arrays.stream(args).skip(2).map(URI::create).toList());
        var server = new JsonServer(System.getenv().getOrDefault("SEARCHFORGE_BIND", "127.0.0.1"), Integer.parseInt(args[0]));
        server.post("/search", Protocol.QueryRequest.class, coordinator::search);
        server.post("/health", Map.class, ignored -> Map.of("status", "ready", "workers", args.length - 2));
        Runtime.getRuntime().addShutdownHook(new Thread(() -> { server.close(); coordinator.close(); }));
        server.start(); Files.writeString(Path.of(args[1]), Integer.toString(server.port()));
        System.out.println("coordinator_ready port=" + server.port());
        new CountDownLatch(1).await();
    }
}
