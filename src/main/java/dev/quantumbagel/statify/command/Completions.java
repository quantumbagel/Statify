package dev.quantumbagel.statify.command;

import dev.quantumbagel.statify.StatsService;
import dev.quantumbagel.statify.query.Query;
import dev.quantumbagel.statify.stats.CustomQueryStore;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.Locale;
import java.util.Set;
import java.util.TreeSet;

/** Shared completion sources, built from cached data so they never touch the disk. */
final class Completions {
    private Completions() {
    }

    static List<String> query(String arg, StatsService stats, CustomQueryStore store) {
        Set<String> lefts = new TreeSet<>(Query.STAT_CATEGORIES);
        lefts.addAll(store.owners());
        return QueryCompleter.complete(arg, lefts, left -> {
            if (Query.STAT_CATEGORIES.contains(left)) {
                Set<String> names = new TreeSet<>(stats.cachedTable().statNames(left));
                names.add("total");
                return names;
            }
            return store.all(left).keySet();
        });
    }

    static List<String> matching(String prefix, Collection<String> options) {
        String lower = prefix.toLowerCase(Locale.ROOT);
        List<String> out = new ArrayList<>();
        for (String option : options) {
            if (option.toLowerCase(Locale.ROOT).startsWith(lower)) {
                out.add(option);
            }
        }
        out.sort(String.CASE_INSENSITIVE_ORDER);
        return out;
    }
}
