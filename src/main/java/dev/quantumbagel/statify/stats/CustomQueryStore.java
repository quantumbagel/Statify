package dev.quantumbagel.statify.stats;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.JsonParseException;
import com.google.gson.reflect.TypeToken;
import dev.quantumbagel.statify.query.Query;
import java.io.IOException;
import java.io.Reader;
import java.io.Writer;
import java.lang.reflect.Type;
import java.nio.file.Files;
import java.nio.file.NoSuchFileException;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.util.Collections;
import java.util.Locale;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.SortedMap;
import java.util.TreeMap;

/**
 * Saved custom queries, keyed by lowercase owner username and query name, kept in memory and
 * written back to a JSON file on every change.
 */
public final class CustomQueryStore implements Query.Library {
    private static final Gson GSON = new GsonBuilder().setPrettyPrinting().create();
    private static final Type FILE_TYPE = new TypeToken<Map<String, Map<String, String>>>() {
    }.getType();

    private final Path file;
    private final Map<String, SortedMap<String, String>> queries = new TreeMap<>();

    private CustomQueryStore(Path file) {
        this.file = file;
    }

    /** Opens the store, creating nothing on disk until the first change. */
    public static CustomQueryStore open(Path file) throws IOException {
        CustomQueryStore store = new CustomQueryStore(file);
        store.importFrom(file);
        return store;
    }

    /**
     * Adds every query from a file in the store's format (including Statify 1.0's statify.json)
     * that is not already present. Returns how many were added.
     */
    public int importFrom(Path source) throws IOException {
        Map<String, Map<String, String>> loaded;
        try (Reader reader = Files.newBufferedReader(source)) {
            loaded = GSON.fromJson(reader, FILE_TYPE);
        } catch (NoSuchFileException e) {
            return 0;
        } catch (JsonParseException e) {
            throw new IOException(source.getFileName() + " is not valid JSON: " + e.getMessage(), e);
        }
        if (loaded == null) {
            return 0;
        }
        int added = 0;
        for (Map.Entry<String, Map<String, String>> owner : loaded.entrySet()) {
            if (owner.getValue() == null) {
                continue;
            }
            for (Map.Entry<String, String> query : owner.getValue().entrySet()) {
                SortedMap<String, String> mine = queries.computeIfAbsent(key(owner.getKey()), k -> new TreeMap<>());
                if (query.getValue() != null && mine.putIfAbsent(key(query.getKey()), query.getValue()) == null) {
                    added++;
                }
            }
        }
        return added;
    }

    @Override
    public Optional<String> lookup(String owner, String name) {
        return Optional.ofNullable(queries.getOrDefault(key(owner), Collections.emptySortedMap()).get(key(name)));
    }

    /** One owner's queries, sorted by name. */
    public SortedMap<String, String> all(String owner) {
        return Collections.unmodifiableSortedMap(queries.getOrDefault(key(owner), Collections.emptySortedMap()));
    }

    public Set<String> owners() {
        return Collections.unmodifiableSet(queries.keySet());
    }

    public void set(String owner, String name, String source) throws IOException {
        queries.computeIfAbsent(key(owner), k -> new TreeMap<>()).put(key(name), source);
        save();
    }

    /** Returns false if there was no such query. */
    public boolean remove(String owner, String name) throws IOException {
        SortedMap<String, String> mine = queries.get(key(owner));
        if (mine == null || mine.remove(key(name)) == null) {
            return false;
        }
        if (mine.isEmpty()) {
            queries.remove(key(owner));
        }
        save();
        return true;
    }

    /** Writes to a temporary file first so a crash mid-write cannot corrupt the saved queries. */
    public void save() throws IOException {
        Files.createDirectories(file.toAbsolutePath().getParent());
        Path temp = file.resolveSibling(file.getFileName() + ".tmp");
        try (Writer writer = Files.newBufferedWriter(temp)) {
            GSON.toJson(queries, FILE_TYPE, writer);
        }
        Files.move(temp, file, StandardCopyOption.REPLACE_EXISTING, StandardCopyOption.ATOMIC_MOVE);
    }

    private static String key(String s) {
        return s.toLowerCase(Locale.ROOT);
    }
}
