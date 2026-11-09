package dev.quantumbagel.statify.rank;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.Comparator;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import org.bukkit.ChatColor;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;

class LeaderboardTest {

    private static List<Standing<String>> rank(Map<String, Double> values, boolean ascending) {
        return Leaderboard.rank(values, ascending, Comparator.naturalOrder());
    }

    @Test
    void tiesSharePlacesAndSkipAhead() {
        Map<String, Double> values = new LinkedHashMap<>();
        values.put("d", 5.0);
        values.put("a", 9.0);
        values.put("c", 7.0);
        values.put("b", 7.0);
        values.put("e", 1.0);
        List<Standing<String>> standings = rank(values, false);
        assertEquals(List.of(
                new Standing<>(1, false, "a", 9.0),
                new Standing<>(2, true, "b", 7.0),
                new Standing<>(2, true, "c", 7.0),
                new Standing<>(4, false, "d", 5.0),
                new Standing<>(5, false, "e", 1.0)), standings);
    }

    @Test
    void ascendingAndUndefinedValues() {
        Map<String, Double> values = new HashMap<>();
        values.put("few deaths", 1.0);
        values.put("many deaths", 40.0);
        values.put("divided by zero", Double.POSITIVE_INFINITY);
        values.put("zero over zero", Double.NaN);
        List<Standing<String>> standings = rank(values, true);
        assertEquals(List.of("few deaths", "many deaths"), standings.stream().map(Standing::key).toList());
    }

    @Test
    void showsExactlyTheRequestedNumberOfRows() {
        // Statify 1.0 stopped one row early, so "top 10" showed 9.
        Map<String, Double> values = new HashMap<>();
        for (int i = 0; i < 12; i++) {
            values.put("p" + (char) ('a' + i), (double) i);
        }
        String message = ChatColor.stripColor(LeaderboardFormatter.format(
                "Top", rank(values, false), s -> s, s -> false, 10));
        assertEquals(10, message.lines().filter(line -> line.matches("=?\\d+\\w\\w .*")).count());
        assertTrue(message.endsWith("Showing top 10 of 12."));
    }

    @Test
    void viewerBelowTheCutoffStillSeesTheirRow() {
        Map<String, Double> values = Map.of("a", 3.0, "b", 2.0, "me", 1.0);
        String message = ChatColor.stripColor(LeaderboardFormatter.format(
                "Top", rank(values, false), s -> s, "me"::equals, 2));
        assertTrue(message.contains("...\n3rd me (1)"));
    }

    @Test
    void emptyBoard() {
        String message = ChatColor.stripColor(LeaderboardFormatter.format("Top", List.<Standing<String>>of(),
                s -> s, s -> false, 10));
        assertFalse(message.contains("Showing"));
        assertTrue(message.contains("Nobody"));
    }

    @ParameterizedTest
    @CsvSource({"1,1st", "2,2nd", "3,3rd", "4,4th", "11,11th", "12,12th", "13,13th", "21,21st", "22,22nd",
            "101,101st", "111,111th", "112,112th"})
    void ordinals(int n, String expected) {
        assertEquals(expected, LeaderboardFormatter.ordinal(n));
    }

    @ParameterizedTest
    @CsvSource(delimiter = '|', value = {
            "0              | 0",
            "-0.0           | 0",
            "1234567        | 1,234,567",
            "3000000000     | 3,000,000,000",
            "2.5            | 2.5",
            "0.3333333      | 0.33",
    })
    void values(double value, String expected) {
        // Statify 1.0 converted whole values with intValue(), so anything over 2,147,483,647 was clamped.
        assertEquals(expected, LeaderboardFormatter.formatValue(value));
    }
}
