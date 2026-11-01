package dev.searchforge.query;

import dev.searchforge.analyzer.Analyzer;
import java.util.*;

/** AND binds more tightly than OR; adjacency means OR. Operators are uppercase. */
public sealed interface Query {
    record Terms(List<Analyzer.Token> tokens, boolean phrase) implements Query {
        public Terms { tokens = List.copyOf(tokens); }
    }
    record And(Query left, Query right) implements Query {}
    record Or(Query left, Query right) implements Query {}

    static Query parse(String input, Analyzer analyzer) { return new Parser(input, analyzer).parse(); }

    final class Parser {
        private final List<String> parts = new ArrayList<>();
        private final Analyzer analyzer;
        private int cursor;
        private int depth;
        Parser(String input, Analyzer analyzer) {
            Objects.requireNonNull(input);
            if (input.length() > 4096) throw new IllegalArgumentException("Query exceeds 4096 characters");
            this.analyzer = analyzer;
            for (int i = 0; i < input.length();) {
                char c = input.charAt(i);
                if (Character.isWhitespace(c)) { i++; continue; }
                if (c == '(' || c == ')') { parts.add(String.valueOf(c)); i++; continue; }
                int start = i++;
                if (c == '"') {
                    while (i < input.length() && input.charAt(i) != '"') i++;
                    if (i == input.length()) throw new IllegalArgumentException("Unclosed phrase");
                    i++;
                } else {
                    while (i < input.length() && !Character.isWhitespace(input.charAt(i))
                            && "()\"".indexOf(input.charAt(i)) < 0) i++;
                }
                parts.add(input.substring(start, i));
                if (parts.size() > 256) throw new IllegalArgumentException("Too many query tokens");
            }
        }
        Query parse() {
            if (parts.isEmpty()) return new Terms(List.of(), false);
            Query result = or();
            if (cursor != parts.size()) throw new IllegalArgumentException("Unexpected closing parenthesis");
            return result;
        }
        private boolean at(String s) { return cursor < parts.size() && parts.get(cursor).equals(s); }
        private Query or() {
            Query value = and();
            while (cursor < parts.size() && !at(")")) {
                if (at("OR")) cursor++;
                value = new Or(value, and());
            }
            return value;
        }
        private Query and() {
            Query value = atom();
            while (at("AND")) { cursor++; value = new And(value, atom()); }
            return value;
        }
        private Query atom() {
            if (cursor >= parts.size()) throw new IllegalArgumentException("Missing query operand");
            String part = parts.get(cursor++);
            if (part.equals("(")) {
                if (++depth > 32) throw new IllegalArgumentException("Query nesting exceeds 32");
                Query value = or();
                if (!at(")")) throw new IllegalArgumentException("Unclosed parenthesis");
                cursor++; depth--; return value;
            }
            if (Set.of(")", "AND", "OR").contains(part)) throw new IllegalArgumentException("Missing query operand");
            boolean phrase = part.startsWith("\"");
            return new Terms(analyzer.analyze(phrase ? part.substring(1, part.length() - 1) : part), phrase);
        }
    }
}
