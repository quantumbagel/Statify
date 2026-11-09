package dev.quantumbagel.statify.stats;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

class StatTableTest {
    private static final UUID ALICE = UUID.fromString("11111111-1111-1111-1111-111111111111");
    private static final UUID BOB = UUID.fromString("22222222-2222-2222-2222-222222222222");

    @TempDir
    Path dir;

    @Test
    void loadsStatFiles() throws IOException {
        Files.writeString(dir.resolve(ALICE + ".json"), """
                {"stats": {
                  "minecraft:mined": {"minecraft:stone": 3000000000, "minecraft:dirt": 5},
                  "minecraft:killed": {"minecraft:zombie": 7}
                }, "DataVersion": 3953}
                """);
        Files.writeString(dir.resolve(BOB + ".json"), """
                {"stats": {"minecraft:killed": {"minecraft:zombie": 2}}}
                """);
        Files.writeString(dir.resolve("not-a-uuid.json"), "{}");
        Files.writeString(dir.resolve(UUID.randomUUID() + ".json"), "{ broken");

        List<String> warnings = new ArrayList<>();
        StatTable table = StatTable.load(dir, warnings::add);

        assertEquals(Set.of(ALICE, BOB), table.players());
        assertEquals(1, warnings.size());
        assertEquals(3_000_000_000L, table.get(ALICE, "mined", "stone"), "values beyond int range survive");
        assertEquals(3_000_000_005L, table.get(ALICE, "mined", "total"));
        assertEquals(0, table.get(BOB, "mined", "stone"));
        assertTrue(table.has(BOB, "killed", "zombie"));
        assertFalse(table.has(BOB, "mined", "total"));
        assertEquals(Set.of("dirt", "stone"), table.statNames("mined"));
    }

    @Test
    void missingDirectoryIsEmpty() throws IOException {
        assertTrue(StatTable.load(dir.resolve("nope"), w -> { }).players().isEmpty());
    }

    @Test
    void playerNamesAreCaseInsensitive() throws IOException {
        Path cache = dir.resolve("usercache.json");
        Files.writeString(cache, """
                [{"name": "quantumbagel", "uuid": "%s", "expiresOn": "2026-12-01 00:00:00 -0500"},
                 {"name": "broken", "uuid": "nope"}]
                """.formatted(ALICE));
        PlayerNames names = PlayerNames.load(cache);
        assertEquals(Optional.of(ALICE), names.find("QuantumBagel"));
        assertEquals("quantumbagel", names.name(ALICE));
        assertTrue(names.name(BOB).startsWith("Unknown"));
        assertEquals(Optional.empty(), names.find("broken"));
    }
}
