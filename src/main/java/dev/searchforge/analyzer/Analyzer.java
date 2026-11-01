package dev.searchforge.analyzer;

import java.text.Normalizer;
import java.util.*;
import java.util.regex.Pattern;

/** Shared document/query analysis. Positions count tokens before stop-word removal. */
public final class Analyzer {
    private static final Pattern WORD = Pattern.compile("[\\p{L}\\p{N}][\\p{L}\\p{N}\\p{M}]*");
    private final Set<String> stopWords;
    public record Token(String term, int position) {}
    public Analyzer() { this(Set.of()); }
    public Analyzer(Set<String> stopWords) {
        var normalized = new HashSet<String>();
        for (String word : stopWords) { validateText(word); normalized.add(normalize(word)); }
        this.stopWords = Set.copyOf(normalized);
    }
    private static String normalize(String text) {
        return Normalizer.normalize(text, Normalizer.Form.NFKC).toLowerCase(Locale.ROOT);
    }
    public Set<String> stopWords() { return stopWords; }
    public List<Token> analyze(String text) {
        validateText(text);
        var matcher = WORD.matcher(normalize(text));
        var result = new ArrayList<Token>();
        int position = 0;
        while (matcher.find()) {
            String term = matcher.group();
            if (!stopWords.contains(term)) result.add(new Token(term, position));
            position++;
        }
        return List.copyOf(result);
    }
    public static void validateText(String text) {
        Objects.requireNonNull(text, "text");
        // Reject malformed UTF-16 instead of silently indexing replacement characters.
        for (int i = 0; i < text.length(); i++) {
            char c = text.charAt(i);
            if (Character.isHighSurrogate(c)) {
                if (++i >= text.length() || !Character.isLowSurrogate(text.charAt(i)))
                    throw new IllegalArgumentException("Unpaired surrogate");
            } else if (Character.isLowSurrogate(c)) throw new IllegalArgumentException("Unpaired surrogate");
        }
    }
}
