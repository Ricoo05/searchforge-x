package dev.searchforge.storage;

import dev.searchforge.analyzer.Analyzer;
import dev.searchforge.index.IndexSnapshot;
import java.io.*;
import java.nio.ByteBuffer;
import java.nio.channels.*;
import java.nio.file.*;
import java.util.*;

/** Exclusive owner of a directory; atomic immutable commit markers are the commit point. */
public final class GenerationStore implements AutoCloseable {
  public enum Stage {
    SEGMENT_MID_WRITE,
    SEGMENT_FORCED,
    MANIFEST_MID_WRITE,
    MANIFEST_FORCED,
    COMMIT_PUBLISHED,
    RECOVERY_SEGMENT_READ
  }

  @FunctionalInterface
  public interface Fault {
    void at(Stage stage) throws IOException;
  }

  private final Path directory;
  private final FileChannel lockChannel;
  private final FileLock lock;
  private final Fault fault;
  private boolean closed;

  public GenerationStore(Path directory) throws IOException {
    this(directory, ignored -> {});
  }

  public GenerationStore(Path directory, Fault fault) throws IOException {
    this.directory = directory.toAbsolutePath();
    this.fault = fault;
    Files.createDirectories(this.directory);
    lockChannel =
        FileChannel.open(
            this.directory.resolve("writer.lock"),
            StandardOpenOption.CREATE,
            StandardOpenOption.WRITE);
    FileLock acquired;
    try {
      acquired = lockChannel.tryLock();
      if (acquired == null) throw new IOException("Index directory is already open");
    } catch (IOException | RuntimeException ex) {
      lockChannel.close();
      throw ex;
    }
    lock = acquired;
  }

  public synchronized IndexSnapshot recover(Analyzer analyzer) throws IOException {
    ensureOpen();
    Path newest = null;
    long generation = 0;
    try (var files = Files.newDirectoryStream(directory, "commit-*.manifest")) {
      for (Path file : files) {
        String name = file.getFileName().toString();
        long candidate;
        try {
          candidate = Long.parseLong(name.substring(7, name.length() - 9));
        } catch (NumberFormatException ex) {
          throw new IOException("Invalid commit name", ex);
        }
        if (candidate <= 0) throw new IOException("Invalid commit generation");
        if (candidate > generation) {
          newest = file;
          generation = candidate;
        }
      }
    }
    if (newest == null) return new IndexSnapshot(List.of(), analyzer, 0);
    byte[] manifest = Files.readAllBytes(newest);
    if (manifest.length != 44) throw new IOException("Invalid commit manifest length");
    var buffer = ByteBuffer.wrap(manifest);
    if (buffer.getInt() != 0x53464d31 || buffer.getLong() != generation)
      throw new IOException("Invalid commit manifest");
    byte[] checksum = new byte[32];
    buffer.get(checksum);
    Path segment = directory.resolve("segment-" + generation + ".sfx");
    if (Files.size(segment) > 256L * 1024 * 1024)
      throw new IOException("Segment exceeds v1 256 MiB limit");
    byte[] bytes = Files.readAllBytes(segment);
    if (!java.security.MessageDigest.isEqual(checksum, SegmentCodec.digest(bytes)))
      throw new IOException("Committed segment checksum mismatch");
    fault.at(Stage.RECOVERY_SEGMENT_READ);
    var snapshot = SegmentCodec.decode(bytes, analyzer);
    if (snapshot.generation() != generation) throw new IOException("Generation mismatch");
    return snapshot;
  }

  public synchronized void commit(IndexSnapshot snapshot) throws IOException {
    ensureOpen();
    long generation = snapshot.generation();
    if (generation < 1) throw new IOException("Committed generation must be positive");
    Path commit = directory.resolve("commit-" + generation + ".manifest");
    if (Files.exists(commit)) throw new IOException("Generation already committed");
    byte[] bytes = SegmentCodec.encode(snapshot);
    if (bytes.length > 256 * 1024 * 1024) throw new IOException("Segment exceeds v1 256 MiB limit");
    Path segment = directory.resolve("segment-" + generation + ".sfx");
    writeForced(segment, bytes, Stage.SEGMENT_MID_WRITE);
    fault.at(Stage.SEGMENT_FORCED);
    byte[] manifest =
        ByteBuffer.allocate(44)
            .putInt(0x53464d31)
            .putLong(generation)
            .put(SegmentCodec.digest(bytes))
            .array();
    Path pending = directory.resolve("pending-" + generation + ".manifest");
    writeForced(pending, manifest, Stage.MANIFEST_MID_WRITE);
    fault.at(Stage.MANIFEST_FORCED);
    Files.move(pending, commit, StandardCopyOption.ATOMIC_MOVE);
    fault.at(Stage.COMMIT_PUBLISHED);
  }

  private void writeForced(Path path, byte[] bytes, Stage midpoint) throws IOException {
    try (var channel =
        FileChannel.open(
            path,
            StandardOpenOption.CREATE,
            StandardOpenOption.TRUNCATE_EXISTING,
            StandardOpenOption.WRITE)) {
      var prefix = ByteBuffer.wrap(bytes, 0, bytes.length / 2);
      while (prefix.hasRemaining()) channel.write(prefix);
      fault.at(midpoint);
      var buffer = ByteBuffer.wrap(bytes, bytes.length / 2, bytes.length - bytes.length / 2);
      while (buffer.hasRemaining()) channel.write(buffer);
      channel.force(true);
    }
  }

  private void ensureOpen() throws IOException {
    if (closed) throw new IOException("Store closed");
  }

  /** Each generation already consolidates live documents. Reclaim obsolete generations only. */
  public synchronized void compact(Analyzer analyzer, Runnable afterManifestRemoval)
      throws IOException {
    IndexSnapshot live =
        recover(analyzer); // Verify the newest generation before deleting anything.
    if (live.generation() == 0) return;
    try (var files = Files.newDirectoryStream(directory, "commit-*.manifest")) {
      for (Path file : files) {
        long generation =
            Long.parseLong(
                file.getFileName()
                    .toString()
                    .substring(7, file.getFileName().toString().length() - 9));
        if (generation < live.generation()) {
          Files.delete(file); // Remove old publication before its data; latest remains intact.
          afterManifestRemoval.run();
        }
      }
    }
    try (var files = Files.newDirectoryStream(directory, "segment-*.sfx")) {
      for (Path file : files) {
        String name = file.getFileName().toString();
        long generation;
        try {
          generation = Long.parseLong(name.substring(8, name.length() - 4));
        } catch (NumberFormatException ex) {
          throw new IOException("Invalid segment name", ex);
        }
        if (generation != live.generation()) Files.delete(file);
      }
    }
    try (var files = Files.newDirectoryStream(directory, "pending-*.manifest")) {
      for (Path file : files) Files.delete(file);
    }
  }

  @Override
  public synchronized void close() throws IOException {
    if (!closed) {
      closed = true;
      try {
        lock.release();
      } finally {
        lockChannel.close();
      }
    }
  }
}
