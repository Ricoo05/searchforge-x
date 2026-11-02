package dev.searchforge.reference;

import com.fasterxml.jackson.databind.*;
import java.nio.file.*;
import java.text.Normalizer;
import java.util.*;
import java.util.regex.*;
import org.apache.lucene.analysis.*;
import org.apache.lucene.analysis.tokenattributes.*;
import org.apache.lucene.document.*;
import org.apache.lucene.index.*;
import org.apache.lucene.search.*;
import org.apache.lucene.search.similarities.BM25Similarity;
import org.apache.lucene.store.ByteBuffersDirectory;

/** Independent Lucene BM25 reference; never used by the SearchForge runtime. */
public final class LuceneReference {
    private static final ObjectMapper JSON = new ObjectMapper();
    private static List<String> terms(String text) {
        var matcher = Pattern.compile("[\\p{L}\\p{N}][\\p{L}\\p{N}\\p{M}]*").matcher(
                Normalizer.normalize(text, Normalizer.Form.NFKC).toLowerCase(Locale.ROOT));
        var tokens = new ArrayList<String>(); while (matcher.find()) tokens.add(matcher.group()); return tokens;
    }
    public static void main(String[] args) throws Exception {
        var analyzer = new Analyzer() {
            @Override protected TokenStreamComponents createComponents(String field) {
                var tokenizer = new Tokenizer() {
                    private final CharTermAttribute term = addAttribute(CharTermAttribute.class);
                    private final PositionIncrementAttribute increment = addAttribute(PositionIncrementAttribute.class);
                    private Iterator<String> tokens = List.<String>of().iterator();
                    @Override public void reset() throws java.io.IOException {
                        super.reset(); var text = new java.io.StringWriter(); input.transferTo(text); tokens = terms(text.toString()).iterator();
                    }
                    @Override public boolean incrementToken() {
                        if (!tokens.hasNext()) return false; clearAttributes(); term.append(tokens.next()); increment.setPositionIncrement(1); return true;
                    }
                };
                return new TokenStreamComponents(tokenizer);
            }
        };
        try (analyzer; var directory = new ByteBuffersDirectory()) {
            var config = new IndexWriterConfig(analyzer).setSimilarity(new BM25Similarity(1.2f, .75f));
            try (var writer = new IndexWriter(directory, config); var lines = Files.lines(Path.of(args[0]))) {
                for (String line : lines.toList()) {
                    var row = JSON.readTree(line); var document = new Document();
                    document.add(new StringField("id", row.path("_id").asText(), Field.Store.YES));
                    document.add(new TextField("text", row.path("title").asText("") + " " + row.path("text").asText(), Field.Store.NO));
                    writer.addDocument(document);
                }
            }
            try (var reader = DirectoryReader.open(directory); var lines = Files.lines(Path.of(args[1]))) {
                var searcher = new IndexSearcher(reader); searcher.setSimilarity(new BM25Similarity(1.2f, .75f));
                var run = new TreeMap<String, Object>();
                for (String line : lines.toList()) {
                    var row = JSON.readTree(line); var builder = new BooleanQuery.Builder();
                    for (String term : new TreeSet<>(terms(row.path("text").asText())))
                        builder.add(new TermQuery(new Term("text", term)), BooleanClause.Occur.SHOULD);
                    var hits = new ArrayList<Object>();
                    for (var hit : searcher.search(builder.build(), 100).scoreDocs)
                        hits.add(Map.of("id", searcher.storedFields().document(hit.doc).get("id"), "score", hit.score));
                    run.put(row.path("_id").asText(), hits);
                }
                JSON.writerWithDefaultPrettyPrinter().writeValue(Path.of(args[2]).toFile(), run);
                System.out.println("lucene_reference_complete documents=" + reader.numDocs() + " queries=" + run.size());
            }
        }
    }
}
