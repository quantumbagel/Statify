package dev.quantumbagel.statify.stats;

import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParseException;
import com.google.gson.JsonParser;
import java.io.IOException;
import java.io.Reader;
import java.nio.file.Files;
import java.nio.file.NoSuchFileException;
import java.nio.file.Path;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.Locale;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;

/** UUID and username lookups from the server's {@code usercache.json}. */
public final class PlayerNames {
    private final Map<UUID, String> byId;
    private final Map<String, UUID> byLowerName;

    PlayerNames(Map<UUID, String> byId) {
        this.byId = byId;
        this.byLowerName = new HashMap<>();
        byId.forEach((id, name) -> byLowerName.put(name.toLowerCase(Locale.ROOT), id));
    }

    public static PlayerNames empty() {
        return new PlayerNames(Map.of());
    }

    public static PlayerNames load(Path userCache) throws IOException {
        Map<UUID, String> byId = new HashMap<>();
        try (Reader reader = Files.newBufferedReader(userCache)) {
            for (JsonElement element : JsonParser.parseReader(reader).getAsJsonArray()) {
                JsonObject entry = element.getAsJsonObject();
                try {
                    byId.put(UUID.fromString(entry.get("uuid").getAsString()), entry.get("name").getAsString());
                } catch (IllegalArgumentException | NullPointerException ignored) {
                    // Skip malformed entries rather than losing every name.
                }
            }
        } catch (NoSuchFileException e) {
            return empty();
        } catch (JsonParseException | IllegalStateException e) {
            throw new IOException("usercache.json is not valid: " + e.getMessage(), e);
        }
        return new PlayerNames(byId);
    }

    public String name(UUID id) {
        String name = byId.get(id);
        return name != null ? name : "Unknown (" + id.toString().substring(0, 8) + ")";
    }

    public Optional<UUID> find(String name) {
        return Optional.ofNullable(byLowerName.get(name.toLowerCase(Locale.ROOT)));
    }

    public Collection<String> names() {
        return Collections.unmodifiableCollection(byId.values());
    }
}
