package dev.searchforge.node;

import dev.searchforge.query.Search;
import java.time.Duration;
import java.util.*;
import java.util.concurrent.*;

/** Bounded admission with caller cancellation and deadlines including queue wait. */
public final class QueryExecutor implements AutoCloseable {
    private final ThreadPoolExecutor workers;
    private final ScheduledThreadPoolExecutor deadlines = new ScheduledThreadPoolExecutor(1);
    private final Set<CompletableFuture<?>> pending = new HashSet<>();
    private boolean closed;
    public QueryExecutor(int threads, int queueCapacity) {
        if (threads < 1 || threads > 64 || queueCapacity < 1 || queueCapacity > 10000)
            throw new IllegalArgumentException("Invalid worker or queue bound");
        workers = new ThreadPoolExecutor(threads, threads, 0, TimeUnit.MILLISECONDS,
                new ArrayBlockingQueue<>(queueCapacity), new ThreadPoolExecutor.AbortPolicy());
        deadlines.setRemoveOnCancelPolicy(true);
    }
    public CompletableFuture<Search.Result> search(SearchEngine engine, String query, int offset, int limit, Duration timeout) {
        return submit(() -> engine.search(query, offset, limit), timeout);
    }
    public synchronized <T> CompletableFuture<T> submit(Callable<T> action, Duration timeout) {
        Objects.requireNonNull(action); Objects.requireNonNull(timeout);
        if (timeout.isNegative() || timeout.isZero() || timeout.compareTo(Duration.ofMinutes(5)) > 0)
            throw new IllegalArgumentException("Timeout must be positive and at most five minutes");
        if (closed) throw new RejectedExecutionException("Query executor closed");
        long due = System.nanoTime() + timeout.toNanos();
        var result = new CompletableFuture<T>();
        var job = new FutureTask<Void>(() -> {
            try {
                if (System.nanoTime() >= due) throw new TimeoutException("Deadline elapsed before execution");
                T value = action.call();
                if (System.nanoTime() >= due) throw new TimeoutException("Query deadline exceeded");
                result.complete(value);
            } catch (Throwable ex) { result.completeExceptionally(ex); }
            return null;
        });
        workers.execute(job); // Reject immediately if both workers and queue are full.
        pending.add(result);
        var alarm = deadlines.schedule(() -> result.completeExceptionally(new TimeoutException("Query deadline exceeded")),
                Math.max(0, due - System.nanoTime()), TimeUnit.NANOSECONDS);
        result.whenComplete((value, error) -> {
            alarm.cancel(false);
            if (error != null) job.cancel(true);
            workers.remove(job);
            synchronized (this) { pending.remove(result); }
        });
        return result;
    }
    @Override public void close() {
        synchronized (this) {
            if (closed) return;
            closed = true;
            for (var task : List.copyOf(pending)) task.cancel(true);
            workers.shutdownNow(); deadlines.shutdownNow();
        }
        try {
            if (!workers.awaitTermination(10, TimeUnit.SECONDS) || !deadlines.awaitTermination(10, TimeUnit.SECONDS))
                throw new IllegalStateException("Executor tasks failed to terminate");
        } catch (InterruptedException ex) {
            Thread.currentThread().interrupt(); throw new IllegalStateException("Interrupted during executor shutdown", ex);
        }
    }
}
