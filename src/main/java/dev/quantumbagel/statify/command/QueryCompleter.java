package dev.quantumbagel.statify.command;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.Locale;
import java.util.function.Function;

/**
 * Tab completion for the last word of a query. Completes the {@code left} side from categories
 * and custom query owners, and the {@code right} side from known stats or that owner's queries.
 */
final class QueryCompleter {
    private static final int MAX_SUGGESTIONS = 60;

    private QueryCompleter() {
    }

    /**
     * @param arg   the argument being typed, e.g. {@code (killed:zom}
     * @param lefts every valid left side (categories and owners)
     * @param rights the valid right sides for a given left side
     */
    static List<String> complete(String arg, Collection<String> lefts, Function<String, Collection<String>> rights) {
        int start = arg.length();
        while (start > 0 && isNameChar(arg.charAt(start - 1))) {
            start--;
        }
        String before = arg.substring(0, start);
        String word = arg.substring(start).toLowerCase(Locale.ROOT);
        List<String> out = new ArrayList<>();
        int colon = word.indexOf(':');
        if (colon < 0) {
            for (String left : lefts) {
                if (left.startsWith(word)) {
                    out.add(before + left + ":");
                }
            }
        } else {
            String left = word.substring(0, colon);
            String partial = word.substring(colon + 1);
            for (String right : rights.apply(left)) {
                if (right.startsWith(partial)) {
                    out.add(before + left + ":" + right);
                }
            }
        }
        out.sort(null);
        return out.size() > MAX_SUGGESTIONS ? out.subList(0, MAX_SUGGESTIONS) : out;
    }

    private static boolean isNameChar(char c) {
        return Character.isLetterOrDigit(c) || c == '_' || c == ':';
    }
}
