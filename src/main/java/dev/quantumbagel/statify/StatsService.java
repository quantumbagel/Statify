package dev.quantumbagel.statify;

import dev.quantumbagel.statify.stats.PlayerNames;
import dev.quantumbagel.statify.stats.StatTable;
import java.io.IOException;
import java.nio.file.Path;
import java.util.logging.Logger;
import org.bukkit.Bukkit;

/**
 * Loads stat files at most every {@link #MAX_AGE_MILLIS}, instead of re-reading every file on every
 * command as Statify 1.0 did.
 */
public final class StatsService {
    private static final long MAX_AGE_MILLIS = 30_000;

    private final Logger logger;
    private StatTable table = StatTable.empty();
    private PlayerNames names = PlayerNames.empty();
    private boolean loaded;
    private long loadedAt;

    StatsService(Logger logger) {
        this.logger = logger;
    }

    /** Current stats, reloading them first if they are stale. */
    public StatTable table() {
        refreshIfStale();
        return table;
    }

    public PlayerNames names() {
        refreshIfStale();
        return names;
    }

    /** Whatever is loaded, without touching the disk. For tab completion, which runs on every keypress. */
    public StatTable cachedTable() {
        return table;
    }

    public PlayerNames cachedNames() {
        return names;
    }

    private void refreshIfStale() {
        long now = System.currentTimeMillis();
        if (loaded && now - loadedAt < MAX_AGE_MILLIS) {
            return;
        }
        // Online players' stats live in memory until the server saves them. Saving first means
        // the leaderboard includes what they did since the last autosave.
        Bukkit.savePlayers();
        Path statsDir = Bukkit.getWorlds().get(0).getWorldFolder().toPath().resolve("stats");
        try {
            table = StatTable.load(statsDir, logger::warning);
        } catch (IOException e) {
            logger.warning("Could not read stats from " + statsDir + ": " + e.getMessage());
        }
        try {
            names = PlayerNames.load(Bukkit.getWorldContainer().toPath().resolve("usercache.json"));
        } catch (IOException e) {
            logger.warning("Could not read usercache.json: " + e.getMessage());
        }
        loaded = true;
        loadedAt = now;
    }
}
