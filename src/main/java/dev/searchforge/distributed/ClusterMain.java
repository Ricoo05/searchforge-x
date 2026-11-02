package dev.searchforge.distributed;

import java.nio.file.*;
import java.util.*;
import java.util.concurrent.*;

/** Foreground local supervisor; every search component runs in an independent JVM. */
public final class ClusterMain {
  public static void main(String[] args) throws Exception {
    if (args.length != 2)
      throw new IllegalArgumentException(
          "ClusterMain dataDirectory coordinatorPort (workers use port+1..3)");
    Path root = Path.of(args[0]).toAbsolutePath();
    Files.createDirectories(root);
    int port = Integer.parseInt(args[1]);
    if (port < 1024 || port > 65532) throw new IllegalArgumentException("Require port 1024..65532");
    var children = new CopyOnWriteArrayList<Process>();
    Runnable stop =
        () -> {
          for (var child : children) child.destroy();
          for (var child : children) {
            try {
              if (!child.waitFor(5, TimeUnit.SECONDS)) child.destroyForcibly().waitFor();
            } catch (InterruptedException ex) {
              child.destroyForcibly();
              Thread.currentThread().interrupt();
            }
          }
        };
    Runtime.getRuntime().addShutdownHook(new Thread(stop));
    try {
      var workers = new ArrayList<String>();
      for (int shard = 0; shard < 3; shard++) {
        Path ready = root.resolve("worker" + shard + ".ready");
        Files.deleteIfExists(ready);
        start(
            children,
            root,
            "worker" + shard,
            WorkerMain.class,
            "" + shard,
            "3",
            "" + (port + shard + 1),
            root.resolve("shard" + shard).toString(),
            ready.toString());
        awaitReady(children.getLast(), ready);
        workers.add("http://127.0.0.1:" + (port + shard + 1));
      }
      Path ready = root.resolve("coordinator.ready");
      Files.deleteIfExists(ready);
      var coordinatorArgs = new ArrayList<>(List.of("" + port, ready.toString()));
      coordinatorArgs.addAll(workers);
      start(
          children,
          root,
          "coordinator",
          CoordinatorMain.class,
          coordinatorArgs.toArray(String[]::new));
      awaitReady(children.getLast(), ready);
      System.out.println(
          "SearchForge X: http://127.0.0.1:" + port + " (Ctrl+C stops task-owned processes)");
      var reported = new HashSet<Integer>();
      while (children.getLast().isAlive()) {
        for (int shard = 0; shard < 3; shard++)
          if (!children.get(shard).isAlive() && reported.add(shard))
            System.err.println(
                "worker_unavailable shard="
                    + shard
                    + "; remaining services stay running; restart cluster to recover");
        Thread.sleep(1000);
      }
      throw new IllegalStateException("Coordinator exited; inspect logs in " + root);
    } finally {
      stop.run();
    }
  }

  private static void start(
      List<Process> children, Path root, String name, Class<?> main, String... args)
      throws Exception {
    String java =
        Path.of(
                System.getProperty("java.home"),
                "bin",
                System.getProperty("os.name").startsWith("Windows") ? "java.exe" : "java")
            .toString();
    var command =
        new ArrayList<>(
            List.of(
                java, "-Xmx512m", "-cp", System.getProperty("java.class.path"), main.getName()));
    command.addAll(List.of(args));
    children.add(
        new ProcessBuilder(command)
            .redirectErrorStream(true)
            .redirectOutput(root.resolve(name + ".log").toFile())
            .start());
  }

  private static void awaitReady(Process child, Path ready) throws Exception {
    long deadline = System.nanoTime() + TimeUnit.SECONDS.toNanos(20);
    while (child.isAlive() && System.nanoTime() < deadline) {
      if (Files.exists(ready) && !Files.readString(ready).isBlank()) return;
      Thread.sleep(50);
    }
    throw new IllegalStateException("Process startup failed: " + ready);
  }
}
