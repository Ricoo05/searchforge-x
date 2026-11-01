package dev.searchforge.distributed;

import com.sun.net.httpserver.*;
import java.io.*;
import java.net.*;
import java.util.Map;
import java.util.concurrent.*;

/** Local-development HTTP boundary. Fixed connection workers and bounded backlog. */
public final class JsonServer implements AutoCloseable {
    static {
        // These JDK 21 properties use seconds (verified against the bundled JDK source).
        System.setProperty("sun.net.httpserver.maxReqTime", "10");
        System.setProperty("sun.net.httpserver.maxRspTime", "40");
        System.setProperty("sun.net.httpserver.maxReqHeaderSize", "8192");
        System.setProperty("sun.net.httpserver.maxConnections", "256");
    }
    @FunctionalInterface public interface Handler<T> { Object handle(T request) throws Exception; }
    private final HttpServer server;
    private final ThreadPoolExecutor workers = new ThreadPoolExecutor(8, 8, 0, TimeUnit.SECONDS,
            new ArrayBlockingQueue<>(64), new ThreadPoolExecutor.AbortPolicy());
    public JsonServer(String bind, int port) throws IOException {
        server = HttpServer.create(new InetSocketAddress(bind, port), 64); server.setExecutor(workers);
    }
    public <T> void post(String path, Class<T> type, Handler<T> handler) {
        server.createContext(path, exchange -> {
            try (exchange) {
                if (!exchange.getRequestURI().getPath().equals(path)) { respond(exchange, 404, Map.of("error", "Unknown endpoint")); return; }
                if (!exchange.getRequestMethod().equals("POST")) { respond(exchange, 405, Map.of("error", "POST required")); return; }
                byte[] body = exchange.getRequestBody().readNBytes(4 * 1024 * 1024 + 1);
                if (body.length > 4 * 1024 * 1024) { respond(exchange, 413, Map.of("error", "Request exceeds 4 MiB")); return; }
                try {
                    T request = Protocol.JSON.readValue(body, type);
                    if (request == null) throw new IllegalArgumentException("Null request");
                    respond(exchange, 200, handler.handle(request));
                }
                catch (com.fasterxml.jackson.core.JsonProcessingException | IllegalArgumentException ex) { respond(exchange, 400, Map.of("error", "Invalid request")); }
                catch (RejectedExecutionException ex) { respond(exchange, 503, Map.of("error", "Worker overloaded")); }
                catch (IllegalStateException ex) { respond(exchange, 409, Map.of("error", "Snapshot unavailable; retry query")); }
                catch (TimeoutException ex) { respond(exchange, 504, Map.of("error", "Deadline exceeded")); }
                catch (Exception ex) {
                    System.err.println("request_error type=" + ex.getClass().getSimpleName());
                    respond(exchange, 500, Map.of("error", "Request failed"));
                }
            }
        });
    }
    public int port() { return server.getAddress().getPort(); }
    public void start() { server.start(); }
    private static void respond(HttpExchange exchange, int status, Object value) throws IOException {
        byte[] bytes = Protocol.JSON.writeValueAsBytes(value);
        exchange.getResponseHeaders().set("Content-Type", "application/json; charset=utf-8");
        exchange.sendResponseHeaders(status, bytes.length); exchange.getResponseBody().write(bytes);
    }
    @Override public void close() {
        server.stop(0); workers.shutdownNow();
        try { if (!workers.awaitTermination(10, TimeUnit.SECONDS)) throw new IllegalStateException("HTTP workers still running"); }
        catch (InterruptedException ex) { Thread.currentThread().interrupt(); }
    }
}
