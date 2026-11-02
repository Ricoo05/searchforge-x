package dev.searchforge;
import dev.searchforge.analyzer.Analyzer;
import dev.searchforge.benchmarks.BenchmarkMain;
import dev.searchforge.index.*;
import org.junit.jupiter.api.Test;
import java.util.*;
import static org.junit.jupiter.api.Assertions.*;
class BenchmarkCodecTest {
    @Test void independentPostingsCodecsReconstructEquivalentBoundaryCorpus() throws Exception {
        var index = new IndexSnapshot(List.of(new Document(1, "a a b"), new Document(128, "a b"), new Document(Long.MAX_VALUE, "b b a")), new Analyzer(), 1);
        var report = BenchmarkMain.compression(index); // Internally compares reconstructed checksums.
        assertTrue(report.containsKey("fixedWidth")); assertTrue(report.containsKey("deltaVarint"));
    }
}
