package dev.quantumbagel.statify.command;

import static org.junit.jupiter.api.Assertions.assertEquals;

import java.util.List;
import java.util.Map;
import java.util.Set;
import org.junit.jupiter.api.Test;

class QueryCompleterTest {
    private static final Set<String> LEFTS = Set.of("killed", "killed_by", "mined", "bagel");
    private static final Map<String, List<String>> RIGHTS = Map.of(
            "killed", List.of("zombie", "zombie_villager", "skeleton"),
            "bagel", List.of("kdr"));

    private static List<String> complete(String arg) {
        return QueryCompleter.complete(arg, LEFTS, left -> RIGHTS.getOrDefault(left, List.of()));
    }

    @Test
    void completesCategoriesWithAColon() {
        assertEquals(List.of("killed:", "killed_by:"), complete("kil"));
        assertEquals(List.of("bagel:"), complete("B"));
    }

    @Test
    void completesStatNames() {
        assertEquals(List.of("killed:zombie", "killed:zombie_villager"), complete("killed:zom"));
        assertEquals(List.of("bagel:kdr"), complete("bagel:"));
    }

    @Test
    void keepsWhateverComesBeforeTheWord() {
        assertEquals(List.of("(killed:skeleton"), complete("(killed:sk"));
        assertEquals(List.of("sqrt(mined:"), complete("sqrt(mi"));
    }

    @Test
    void nothingForUnknownCategories() {
        assertEquals(List.of(), complete("nope:"));
    }
}
