package com.sunxin.knowledge.retrieval.search;

import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Locale;
import java.util.Set;

final class QueryText {

    private QueryText() {
    }

    static List<String> keywordTerms(String value) {
        Set<String> terms = new LinkedHashSet<>();
        for (String token : normalize(value).split("[\\s,，。；;：:、?？!！()（）\\[\\]{}<>《》\"']+")) {
            String clean = token.trim();
            if (clean.length() >= 2) {
                terms.add(clean);
            }
        }
        if (terms.isEmpty() && value != null && !value.isBlank()) {
            terms.add(normalize(value));
        }
        return new ArrayList<>(terms);
    }

    static Set<String> similaritySignals(String value) {
        String normalized = normalize(value);
        Set<String> signals = new LinkedHashSet<>();
        for (String token : keywordTerms(normalized)) {
            signals.add(token);
            addCjkBigrams(token, signals);
        }
        addAsciiWords(normalized, signals);
        addCjkBigrams(normalized, signals);
        return signals;
    }

    private static void addAsciiWords(String value, Set<String> signals) {
        for (String token : value.split("[^a-z0-9]+")) {
            if (token.length() >= 2) {
                signals.add(token);
            }
        }
    }

    private static void addCjkBigrams(String value, Set<String> signals) {
        List<Character> cjk = new ArrayList<>();
        for (int index = 0; index < value.length(); index++) {
            char current = value.charAt(index);
            if (isCjk(current)) {
                cjk.add(current);
            } else {
                addCjkBigrams(cjk, signals);
                cjk.clear();
            }
        }
        addCjkBigrams(cjk, signals);
    }

    private static void addCjkBigrams(List<Character> chars, Set<String> signals) {
        for (int index = 0; index < chars.size() - 1; index++) {
            signals.add("" + chars.get(index) + chars.get(index + 1));
        }
    }

    private static boolean isCjk(char value) {
        Character.UnicodeScript script = Character.UnicodeScript.of(value);
        return script == Character.UnicodeScript.HAN;
    }

    private static String normalize(String value) {
        if (value == null) {
            return "";
        }
        return value.toLowerCase(Locale.ROOT).trim();
    }
}
