package dev.quantumbagel.statify.stats;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import java.util.Optional;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

class CustomQueryStoreTest {
    @TempDir
    Path dir;

    @Test
    void changesAreVisibleImmediatelyAndPersist() throws IOException {
        Path file = dir.resolve("plugins/Statify/custom-queries.json");
        CustomQueryStore store = CustomQueryStore.open(file);
        assertFalse(Files.exists(file), "nothing is written until something changes");

        store.set("QuantumBagel", "KDR", "killed:zombie / custom:deaths");
        // Statify 1.0 cached the file once at startup, so new queries did not work until a restart.
        assertEquals(Optional.of("killed:zombie / custom:deaths"), store.lookup("quantumbagel", "kdr"));

        CustomQueryStore reopened = CustomQueryStore.open(file);
        assertEquals(List.of("kdr"), List.copyOf(reopened.all("QUANTUMBAGEL").keySet()));

        assertTrue(reopened.remove("quantumbagel", "kdr"));
        assertFalse(reopened.remove("quantumbagel", "kdr"));
        assertTrue(CustomQueryStore.open(file).owners().isEmpty());
    }

    @Test
    void importsStatify1Files() throws IOException {
        Path legacy = dir.resolve("statify.json");
        Files.writeString(legacy, """
                {"QuantumBagel": {"Kills": "killed:zombie + killed:skeleton"}, "empty": null}
                """);
        CustomQueryStore store = CustomQueryStore.open(dir.resolve("custom-queries.json"));
        store.set("quantumbagel", "kills", "killed:zombie");

        assertEquals(0, store.importFrom(legacy), "existing queries win");
        assertEquals(Optional.of("killed:zombie"), store.lookup("quantumbagel", "kills"));

        store.remove("quantumbagel", "kills");
        assertEquals(1, store.importFrom(legacy));
        assertEquals(Optional.of("killed:zombie + killed:skeleton"), store.lookup("quantumbagel", "kills"));
    }

    @Test
    void corruptFilesAreReportedNotOverwritten() throws IOException {
        Path file = dir.resolve("custom-queries.json");
        Files.writeString(file, "{ not json");
        assertThrows(IOException.class, () -> CustomQueryStore.open(file));
        assertEquals("{ not json", Files.readString(file));
    }
}
