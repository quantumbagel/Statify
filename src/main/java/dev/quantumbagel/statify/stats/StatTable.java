package dev.quantumbagel.statify.stats;

import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParseException;
import com.google.gson.JsonParser;
import java.io.IOException;
import java.io.Reader;
import java.nio.file.DirectoryStream;
import java.nio.file.Files;
import java.nio.file.NoSuchFileException;
import java.nio.file.Path;
import java.util.Collections;
import java.util.HashMap;
import java.util.Map;
import java.util.Set;
import java.util.SortedSet;
import java.util.TreeSet;
import java.util.UUID;
import java.util.function.Consumer;

/**
 * Every player's statistics, read from the world's {@code stats/<uuid>.json} files.
 * Values are {@code long} so totals across a category cannot overflow.
 */
public final class StatTable {
    private static final String NAMESPACE = "minecraft:";

    /** player -> category -> stat -> value, without the minecraft: prefix. */
    private final Map<UUID, Map<String, Map<String, Long>>> stats;

    StatTable(Map<UUID, Map<String, Map<String, Long>>> stats) {
        this.stats = stats;
    }

    public static StatTable empty() {
        return new StatTable(Map.of());
    }

    /** Loads every stat file in the directory. Unreadable files are reported to {@code warn} and skipped. */
    public static StatTable load(Path statsDir, Consumer<String> warn) throws IOException {
        Map<UUID, Map<String, Map<String, Long>>> all = new HashMap<>();
        try (DirectoryStream<Path> files = Files.newDirectoryStream(statsDir, "*.json")) {
            for (Path file : files) {
                String fileName = file.getFileName().toString();
                UUID player;
                try {
                    player = UUID.fromString(fileName.substring(0, fileName.length() - ".json".length()));
                } catch (IllegalArgumentException e) {
                    continue;
                }
                try (Reader reader = Files.newBufferedReader(file)) {
                    all.put(player, parse(JsonParser.parseReader(reader)));
                } catch (JsonParseException | IllegalStateException | IOException e) {
                    warn.accept("Skipping unreadable stat file " + fileName + ": " + e.getMessage());
                }
            }
        } catch (NoSuchFileException e) {
            return empty();
        }
        return new StatTable(all);
    }

    private static Map<String, Map<String, Long>> parse(JsonElement root) {
        Map<String, Map<String, Long>> categories = new HashMap<>();
        JsonElement statsElement = root.getAsJsonObject().get("stats");
        if (statsElement == null) {
            return categories;
        }
        for (Map.Entry<String, JsonElement> category : statsElement.getAsJsonObject().entrySet()) {
            if (!category.getKey().startsWith(NAMESPACE)) {
                continue;
            }
            Map<String, Long> values = new HashMap<>();
            JsonObject entries = category.getValue().getAsJsonObject();
            for (Map.Entry<String, JsonElement> stat : entries.entrySet()) {
                if (stat.getKey().startsWith(NAMESPACE)) {
                    values.put(stat.getKey().substring(NAMESPACE.length()), stat.getValue().getAsLong());
                }
            }
            categories.put(category.getKey().substring(NAMESPACE.length()), values);
        }
        return categories;
    }

    public Set<UUID> players() {
        return Collections.unmodifiableSet(stats.keySet());
    }

    /** A stat's value, 0 if the player never recorded it. {@code total} sums the whole category. */
    public long get(UUID player, String category, String stat) {
        Map<String, Long> values = category(player, category);
        if (stat.equals("total")) {
            return values.values().stream().mapToLong(Long::longValue).sum();
        }
        return values.getOrDefault(stat, 0L);
    }

    /** Whether the player has recorded this stat at all (or anything in the category, for {@code total}). */
    public boolean has(UUID player, String category, String stat) {
        Map<String, Long> values = category(player, category);
        return stat.equals("total") ? !values.isEmpty() : values.containsKey(stat);
    }

    public Map<String, Long> category(UUID player, String category) {
        return stats.getOrDefault(player, Map.of()).getOrDefault(category, Map.of());
    }

    /** Every stat name recorded in a category by any player, for tab completion. */
    public SortedSet<String> statNames(String category) {
        SortedSet<String> names = new TreeSet<>();
        for (Map<String, Map<String, Long>> player : stats.values()) {
            names.addAll(player.getOrDefault(category, Map.of()).keySet());
        }
        return names;
    }
}
