package dev.searchforge;

import static org.junit.jupiter.api.Assertions.*;

import dev.searchforge.analyzer.Analyzer;
import dev.searchforge.index.Document;
import dev.searchforge.node.SearchEngine;
import dev.searchforge.query.Search;
import java.util.*;
import org.junit.jupiter.api.Test;

class EngineTest {
  private static List<Long> ids(Search.Result result) {
    return result.hits().stream().map(Search.Hit::id).toList();
  }

  private static Set<Long> matches(SearchEngine engine, String q) {
    return new TreeSet<>(ids(engine.search(q, 0, 1000)));
  }

  private static SearchEngine corpus() {
    var engine = new SearchEngine();
    engine.update(
        List.of(
            new Document(1, "red red fox"),
            new Document(2, "red fox jumps"),
            new Document(3, "blue fox"),
            new Document(4, "")),
        Set.of());
    return engine;
  }

  @Test
  void unicodeNormalizationAndGaps() {
    var analyzer = new Analyzer(Set.of("the"));
    assertEquals(
        List.of(
            new Analyzer.Token("café", 0),
            new Analyzer.Token("fox", 2),
            new Analyzer.Token("東京", 3)),
        analyzer.analyze("ＣＡＦÉ the FOX—東京"));
    assertEquals(analyzer.analyze("café"), analyzer.analyze("cafe\u0301"));
    assertEquals(List.of(), analyzer.analyze("... 🦊"));
    assertThrows(IllegalArgumentException.class, () -> analyzer.analyze("\ud800"));
    assertThrows(IllegalArgumentException.class, () -> analyzer.analyze("\udc00"));
  }

  @Test
  void booleanPrecedenceAndPhrases() {
    var e = corpus();
    assertEquals(Set.of(1L, 2L), matches(e, "red AND fox"));
    assertEquals(Set.of(1L, 2L, 3L), matches(e, "red OR blue AND fox"));
    assertEquals(Set.of(2L), matches(e, "(red OR blue) AND jumps"));
    assertEquals(Set.of(1L), matches(e, "\"red red\""));
    assertEquals(Set.of(1L, 2L), matches(e, "\"red fox\""));
    assertEquals(Set.of(), matches(e, "\"fox red\""));
    assertEquals(Set.of(), matches(e, ""));
    assertEquals(Set.of(), matches(e, "absent"));
    assertEquals(matches(e, "red OR blue"), matches(e, "red blue"));
  }

  @Test
  void stopWordsPreservePhraseDistance() {
    var e = new SearchEngine(new Analyzer(Set.of("the")));
    e.update(List.of(new Document(1, "red the fox"), new Document(2, "red fox")), Set.of());
    assertEquals(Set.of(1L), matches(e, "\"red the fox\""));
    assertEquals(Set.of(2L), matches(e, "\"red fox\""));
  }

  @Test
  void malformedQueriesAndLimitsAreRejected() {
    var e = corpus();
    for (String q :
        List.of("AND red", "red OR", "red AND", "()", "(red", "red)", "\"red", "red OR OR fox"))
      assertThrows(IllegalArgumentException.class, () -> e.search(q, 0, 10), q);
    assertThrows(IllegalArgumentException.class, () -> e.search("x".repeat(4097), 0, 10));
    assertThrows(IllegalArgumentException.class, () -> e.search("red", 0, 0));
    assertThrows(IllegalArgumentException.class, () -> e.search("red", 10000, 1));
  }

  @Test
  void knownBm25Example() {
    // N=4, df=2 => idf=ln(2); total indexed tokens=8 => avgdl=2.
    // tf=2, dl=3 => denominator=2+1.2*(.25+.75*1.5)=3.65.
    double expected = Math.log(2) * 4.4 / 3.65;
    var result = corpus().search("red", 0, 10);
    assertEquals(1L, result.hits().getFirst().id());
    assertEquals(expected, result.hits().getFirst().score(), 1e-12);
    assertEquals(result, corpus().search("red red", 0, 10));
  }

  @Test
  void updatesDeletesAndFailedBatchesAreAtomic() {
    var e = corpus();
    var old = e.snapshot();
    assertThrows(
        IllegalArgumentException.class,
        () ->
            e.update(List.of(new Document(9, "valid"), new Document(9, "ambiguous")), Set.of(1L)));
    assertSame(old, e.snapshot());
    e.update(List.of(new Document(1, "green")), Set.of(2L));
    assertEquals(Set.of(), matches(e, "red"));
    assertEquals(Set.of(1L), matches(e, "green"));
    assertEquals(4, old.size());
    assertEquals(2, old.postings("red").size());
    assertThrows(UnsupportedOperationException.class, () -> old.postings("red").get(1L).add(7));
    assertThrows(
        IllegalArgumentException.class,
        () -> e.update(List.of(new Document(1, "x"), new Document(1, "y")), Set.of()));
  }

  @Test
  void tiesAndPaginationAreDeterministic() {
    var e = new SearchEngine();
    e.update(
        List.of(new Document(3, "fox"), new Document(1, "fox"), new Document(2, "fox")), Set.of());
    assertEquals(List.of(1L, 2L, 3L), ids(e.search("fox", 0, 10)));
    assertEquals(List.of(2L), ids(e.search("fox", 1, 1)));
    assertTrue(e.search("fox", 9, 1).hits().isEmpty());
  }

  @Test
  void duplicateDetectionAndMetadataValidation() {
    var e = new SearchEngine();
    e.update(List.of(new Document(2, "same"), new Document(1, "same")), Set.of());
    assertEquals(List.of(1L, 2L), e.duplicatesOf("same"));
    assertEquals(List.of(), e.duplicatesOf("SAME"));
    assertThrows(
        IllegalArgumentException.class, () -> new Document(3, "text", Map.of("title", "\ud800")));
    assertThrows(
        IllegalArgumentException.class,
        () -> new Document(3, "text", Map.of("title", "x".repeat(16385))));
  }

  @Test
  void randomizedSearchMatchesIndependentDocumentScan() {
    var random = new Random(8675309);
    String[] words = {"a", "b", "c", "d", "e"};
    for (int trial = 0; trial < 60; trial++) {
      var texts = new TreeMap<Long, List<String>>();
      var docs = new ArrayList<Document>();
      for (long id = 1; id <= 40; id++) {
        var tokens = new ArrayList<String>();
        for (int n = random.nextInt(20); n > 0; n--)
          tokens.add(words[random.nextInt(words.length)]);
        texts.put(id, tokens);
        docs.add(new Document(id, String.join(" ", tokens)));
      }
      var e = new SearchEngine();
      e.update(docs, Set.of());
      String a = words[random.nextInt(5)], b = words[random.nextInt(5)];
      var and = new TreeSet<Long>();
      var or = new TreeSet<Long>();
      var phrase = new TreeSet<Long>();
      texts.forEach(
          (id, tokens) -> {
            if (tokens.contains(a) && tokens.contains(b)) and.add(id);
            if (tokens.contains(a) || tokens.contains(b)) or.add(id);
            for (int i = 1; i < tokens.size(); i++)
              if (tokens.get(i - 1).equals(a) && tokens.get(i).equals(b)) phrase.add(id);
          });
      assertEquals(and, matches(e, a + " AND " + b));
      assertEquals(or, matches(e, a + " OR " + b));
      assertEquals(phrase, matches(e, "\"" + a + " " + b + "\""));
      // Reference scores use document scans, no production postings or scoring helper.
      var terms = new TreeSet<>(List.of(a, b));
      double avg = texts.values().stream().mapToInt(List::size).average().orElseThrow();
      var reference = new ArrayList<Search.Hit>();
      for (long id : or) {
        double score = 0;
        for (String term : terms) {
          long df = texts.values().stream().filter(t -> t.contains(term)).count();
          long tf = texts.get(id).stream().filter(term::equals).count();
          double idf = Math.log((40.0 + 1) / (df + .5));
          score += idf * (tf * 2.2) / (tf + .3 + .9 * texts.get(id).size() / avg);
        }
        reference.add(new Search.Hit(id, score, docs.get((int) id - 1)));
      }
      reference.sort(
          Comparator.comparingDouble(Search.Hit::score)
              .reversed()
              .thenComparingLong(Search.Hit::id));
      var actual = e.search(a + " " + b, 2, 7);
      var expected =
          reference.subList(Math.min(2, reference.size()), Math.min(9, reference.size()));
      assertEquals(expected.stream().map(Search.Hit::id).toList(), ids(actual));
      for (int i = 0; i < expected.size(); i++)
        assertEquals(expected.get(i).score(), actual.hits().get(i).score(), 1e-12);
    }
  }
}
