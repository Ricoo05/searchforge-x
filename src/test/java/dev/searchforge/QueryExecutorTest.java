package dev.searchforge;

import dev.searchforge.node.QueryExecutor;
import org.junit.jupiter.api.Test;
import java.time.Duration;
import java.util.concurrent.*;
import static org.junit.jupiter.api.Assertions.*;

class QueryExecutorTest {
    @Test void rejectsOverloadAndCancellationReclaimsQueuedSlot() throws Exception {
        try (var executor = new QueryExecutor(1, 1)) {
            var started = new CountDownLatch(1); var release = new CountDownLatch(1);
            var first = executor.submit(() -> { started.countDown(); release.await(); return 1; }, Duration.ofSeconds(10));
            assertTrue(started.await(5, TimeUnit.SECONDS));
            var queued = executor.submit(() -> 2, Duration.ofSeconds(10));
            assertThrows(RejectedExecutionException.class, () -> executor.submit(() -> 3, Duration.ofSeconds(10)));
            queued.cancel(true);
            var replacement = executor.submit(() -> 4, Duration.ofSeconds(10));
            release.countDown();
            assertEquals(1, first.get(5, TimeUnit.SECONDS)); assertEquals(4, replacement.get(5, TimeUnit.SECONDS));
        }
    }
    @Test void deadlineInterruptsRunningTaskAndWorkerRemainsUsable() throws Exception {
        try (var executor = new QueryExecutor(1, 2)) {
            var interrupted = new CountDownLatch(1);
            var future = executor.submit(() -> {
                try { new CountDownLatch(1).await(); }
                catch (InterruptedException ex) { interrupted.countDown(); throw ex; }
                return 1;
            }, Duration.ofMillis(200));
            var error = assertThrows(ExecutionException.class, () -> future.get(5, TimeUnit.SECONDS));
            assertInstanceOf(TimeoutException.class, error.getCause());
            assertTrue(interrupted.await(5, TimeUnit.SECONDS));
            assertEquals(7, executor.submit(() -> 7, Duration.ofSeconds(5)).get(5, TimeUnit.SECONDS));
        }
    }
    @Test void closeCancelsRunningAndQueuedWork() throws Exception {
        for (int repetition = 0; repetition < 100; repetition++) {
        var executor = new QueryExecutor(1, 1);
        var started = new CountDownLatch(1);
        var running = executor.submit(() -> { started.countDown(); new CountDownLatch(1).await(); return 1; }, Duration.ofSeconds(10));
        assertTrue(started.await(5, TimeUnit.SECONDS));
        var queued = executor.submit(() -> 2, Duration.ofSeconds(10));
        executor.close();
        assertTrue(running.isCancelled()); assertTrue(queued.isCancelled());
        assertThrows(RejectedExecutionException.class, () -> executor.submit(() -> 3, Duration.ofSeconds(1)));
        }
    }
}
