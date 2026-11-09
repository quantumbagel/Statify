package dev.quantumbagel.statify.rank;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Map;

public final class Leaderboard {
    private Leaderboard() {
    }

    /**
     * Ranks entries by value. Non-finite values (such as dividing by a stat that is 0) are left out.
     * {@code tieOrder} only decides the display order of entries that share a place.
     */
    public static <K> List<Standing<K>> rank(Map<K, Double> values, boolean ascending, Comparator<K> tieOrder) {
        Comparator<Map.Entry<K, Double>> byValue = Map.Entry.comparingByValue();
        if (!ascending) {
            byValue = byValue.reversed();
        }
        List<Map.Entry<K, Double>> sorted = values.entrySet().stream()
                .filter(e -> e.getValue() != null && Double.isFinite(e.getValue()))
                .sorted(byValue.thenComparing(Map.Entry::getKey, tieOrder))
                .toList();

        List<Standing<K>> standings = new ArrayList<>(sorted.size());
        int place = 0;
        for (int i = 0; i < sorted.size(); i++) {
            double value = sorted.get(i).getValue();
            if (i == 0 || value != sorted.get(i - 1).getValue()) {
                place = i + 1;
            }
            boolean tied = (i > 0 && value == sorted.get(i - 1).getValue())
                    || (i + 1 < sorted.size() && value == sorted.get(i + 1).getValue());
            standings.add(new Standing<>(place, tied, sorted.get(i).getKey(), value));
        }
        return standings;
    }
}
