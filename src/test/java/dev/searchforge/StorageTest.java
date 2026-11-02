package dev.searchforge;

import dev.searchforge.analyzer.Analyzer;
import dev.searchforge.index.*;
import dev.searchforge.node.SearchEngine;
import dev.searchforge.storage.*;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import java.io.*;
import java.nio.file.*;
import java.util.*;
import java.util.concurrent.TimeUnit;
import static org.junit.jupiter.api.Assertions.*;

class StorageTest {
    @TempDir Path directory;
    private SearchEngine open(Path dir) throws IOException { return new SearchEngine(new Analyzer(), new GenerationStore(dir)); }
    @Test void restartPreservesMetadataPostingsUpdatesAndDeletion() throws Exception {
        try (var e = open(directory)) {
            e.update(List.of(new Document(1, "red fox", Map.of("title", "Café 東京")), new Document(2, "blue")), Set.of());
        }
        try (var e = open(directory)) {
            assertEquals("Café 東京", e.search("\"red fox\"", 0, 5).hits().getFirst().document().metadata().get("title"));
            e.update(List.of(new Document(1, "green fox")), Set.of(2L));
        }
        try (var e = open(directory)) {
            assertEquals(2, e.snapshot().generation());
            assertEquals(1, e.snapshot().size());
            assertEquals(0, e.search("red OR blue", 0, 5).totalHits());
            assertEquals(1, e.search("\"green fox\"", 0, 5).totalHits());
        }
    }
    @Test void realProcessCrashAtEveryPublicationBoundary() throws Exception {
        for (var stage : GenerationStore.Stage.values()) {
            Path dir = directory.resolve(stage.name());
            try (var e = open(dir)) { e.update(List.of(new Document(1, "original"), new Document(2, "old")), Set.of()); }
            String java = Path.of(System.getProperty("java.home"), "bin", System.getProperty("os.name").startsWith("Windows") ? "java.exe" : "java").toString();
            Path log = directory.resolve(stage.name() + ".log");
            var process = new ProcessBuilder(java, "-cp", System.getProperty("java.class.path"), CrashChild.class.getName(), dir.toString(), stage.name())
                    .redirectErrorStream(true).redirectOutput(log.toFile()).start();
            try {
                assertTrue(process.waitFor(20, TimeUnit.SECONDS), "Child timed out");
                assertEquals(73, process.exitValue(), Files.readString(log));
            } finally { if (process.isAlive()) process.destroyForcibly().waitFor(); }
            try (var e = open(dir)) {
                boolean committed = stage == GenerationStore.Stage.COMMIT_PUBLISHED;
                assertEquals(committed ? 2 : 1, e.snapshot().generation());
                assertEquals(committed ? "replacement" : "original", e.snapshot().documents().get(1L).text());
                assertEquals(committed, e.snapshot().documents().containsKey(3L));
                assertEquals(!committed, e.snapshot().documents().containsKey(2L));
                // An orphan segment/pending manifest must not prevent the next successful commit.
                e.update(List.of(new Document(4, "after recovery")), Set.of());
            }
            try (var e = open(dir)) { assertEquals(1, e.search("recovery", 0, 5).totalHits()); }
        }
    }
    @Test void committedCorruptionFailsInsteadOfFallingBack() throws Exception {
        try (var e = open(directory)) {
            e.update(List.of(new Document(1, "old")), Set.of());
            e.update(List.of(new Document(1, "new")), Set.of());
        }
        Path segment = directory.resolve("segment-2.sfx");
        byte[] bytes = Files.readAllBytes(segment); bytes[20] ^= 1; Files.write(segment, bytes);
        assertThrows(IOException.class, () -> open(directory));
        // Constructor failure must release the writer lock.
        assertThrows(IOException.class, () -> open(directory));
    }
    @Test void failedCommitRequiresRecovery() throws Exception {
        try (var e = new SearchEngine(new Analyzer(), new GenerationStore(directory, stage -> {
            if (stage == GenerationStore.Stage.COMMIT_PUBLISHED) throw new IOException("Injected post-publication failure");
        }))) {
            assertThrows(UncheckedIOException.class, () -> e.update(List.of(new Document(1, "committed")), Set.of()));
            assertThrows(IllegalStateException.class, () -> e.search("committed", 0, 5));
            assertThrows(IllegalStateException.class, () -> e.update(List.of(), Set.of()));
        }
        try (var e = open(directory)) { assertEquals(1, e.search("committed", 0, 5).totalHits()); }
    }
    @Test void exclusiveWriterAndAnalyzerConfiguration() throws Exception {
        try (var e = open(directory)) {
            e.update(List.of(new Document(1, "fox")), Set.of());
            assertThrows(java.nio.channels.OverlappingFileLockException.class, () -> new GenerationStore(directory));
        }
        assertThrows(IOException.class, () -> new SearchEngine(new Analyzer(Set.of("the")), new GenerationStore(directory)));
        try (var e = open(directory)) { assertEquals(1, e.snapshot().size()); }
    }
    @Test void varintBoundariesMalformedInputAndRandomRoundTrips() throws Exception {
        var random = new Random(123);
        var values = new ArrayList<Long>(List.of(0L, 1L, 127L, 128L, 16383L, 16384L, Long.MAX_VALUE));
        for (int i = 0; i < 1000; i++) values.add(random.nextLong() & Long.MAX_VALUE);
        for (long value : values) {
            var bytes = new ByteArrayOutputStream(); SegmentCodec.writeVar(new DataOutputStream(bytes), value);
            assertEquals(value, SegmentCodec.readVar(new DataInputStream(new ByteArrayInputStream(bytes.toByteArray()))));
        }
        for (byte[] bad : List.of(new byte[]{(byte)128}, new byte[]{(byte)128, 0}, new byte[]{-1,-1,-1,-1,-1,-1,-1,-1,-1,1}))
            assertThrows(IOException.class, () -> SegmentCodec.readVar(new DataInputStream(new ByteArrayInputStream(bad))));
    }
    @Test void segmentDeterminismTruncationAndRoundTrip() throws Exception {
        var index = new IndexSnapshot(List.of(new Document(Long.MAX_VALUE, "red red fox", Map.of("x", "y"))), new Analyzer(), 7);
        byte[] bytes = SegmentCodec.encode(index);
        assertArrayEquals(bytes, SegmentCodec.encode(index));
        var restored = SegmentCodec.decode(bytes, new Analyzer());
        assertEquals(index.documents(), restored.documents());
        assertEquals(index.postings("red"), restored.postings("red"));
        for (int end = 0; end < bytes.length; end++) {
            byte[] truncated = Arrays.copyOf(bytes, end);
            assertThrows(IOException.class, () -> SegmentCodec.decode(truncated, new Analyzer()));
        }
    }
    @Test void cleanupCrashPreservesLatestGenerationAndPinnedReader() throws Exception {
        IndexSnapshot pinned;
        try (var e = open(directory)) {
            e.update(List.of(new Document(1, "old")), Set.of()); pinned = e.snapshot();
            e.update(List.of(new Document(1, "new")), Set.of());
        }
        String java = Path.of(System.getProperty("java.home"), "bin", System.getProperty("os.name").startsWith("Windows") ? "java.exe" : "java").toString();
        var process = new ProcessBuilder(java, "-cp", System.getProperty("java.class.path"), CompactionChild.class.getName(), directory.toString())
                .redirectErrorStream(true).redirectOutput(directory.resolve("cleanup.log").toFile()).start();
        try {
            assertTrue(process.waitFor(20, TimeUnit.SECONDS)); assertEquals(74, process.exitValue());
        } finally { if (process.isAlive()) process.destroyForcibly().waitFor(); }
        try (var e = open(directory)) {
            assertEquals(2, e.snapshot().generation());
            assertEquals(1, e.search("new", 0, 5).totalHits());
            e.compact(); e.compact();
            assertFalse(Files.exists(directory.resolve("segment-1.sfx")));
            assertTrue(Files.exists(directory.resolve("segment-2.sfx")));
            assertEquals("old", pinned.documents().get(1L).text());
        }
    }
    @Test void injectedWriteFailurePreservesPriorCommittedData() throws Exception {
        try (var e = open(directory)) { e.update(List.of(new Document(1, "old")), Set.of()); }
        try (var e = new SearchEngine(new Analyzer(), new GenerationStore(directory, stage -> {
            if (stage == GenerationStore.Stage.SEGMENT_MID_WRITE) throw new IOException("Simulated write failure, not actual disk exhaustion");
        }))) {
            assertThrows(UncheckedIOException.class, () -> e.update(List.of(new Document(1, "new")), Set.of()));
        }
        try (var e = open(directory)) {
            assertEquals(1, e.snapshot().generation()); assertEquals(1, e.search("old", 0, 5).totalHits());
        }
    }
}
