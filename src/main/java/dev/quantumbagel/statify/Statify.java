package dev.quantumbagel.statify;

import dev.quantumbagel.statify.command.CustomCommand;
import dev.quantumbagel.statify.command.LeaderboardCommand;
import dev.quantumbagel.statify.command.StatCommand;
import dev.quantumbagel.statify.stats.CustomQueryStore;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.InvalidPathException;
import java.nio.file.Path;
import java.util.LinkedHashSet;
import java.util.Objects;
import java.util.Set;
import org.bukkit.command.PluginCommand;
import org.bukkit.command.TabExecutor;
import org.bukkit.plugin.java.JavaPlugin;

public final class Statify extends JavaPlugin {

    @Override
    public void onEnable() {
        Path file = getDataFolder().toPath().resolve("custom-queries.json");
        CustomQueryStore customQueries;
        try {
            customQueries = CustomQueryStore.open(file);
        } catch (IOException e) {
            getLogger().severe("Could not read " + file + ": " + e.getMessage()
                    + ". Statify is disabled so it does not overwrite your saved queries. Fix or move the file.");
            getServer().getPluginManager().disablePlugin(this);
            return;
        }
        importStatify1Queries(customQueries);

        StatsService stats = new StatsService(getLogger());
        register("leaderboard", new LeaderboardCommand(stats, customQueries));
        register("stat", new StatCommand(stats, customQueries));
        register("custom", new CustomCommand(stats, customQueries, getLogger()));
    }

    private void register(String name, TabExecutor executor) {
        PluginCommand command = Objects.requireNonNull(getCommand(name), name + " is missing from plugin.yml");
        command.setExecutor(executor);
        command.setTabCompleter(executor);
    }

    /**
     * Statify 1.0 saved queries to the server folder path plus "\statify.json". On Windows that is
     * statify.json in the server folder; elsewhere the backslash ended up in the file name.
     * Each file found is imported once and then renamed so deleted queries do not come back.
     */
    private void importStatify1Queries(CustomQueryStore customQueries) {
        Set<Path> candidates = new LinkedHashSet<>();
        candidates.add(getServer().getWorldContainer().toPath().resolve("statify.json"));
        try {
            candidates.add(Path.of(getServer().getWorldContainer().getAbsolutePath() + "\\statify.json"));
        } catch (InvalidPathException ignored) {
            // Not a valid path on this system, so 1.0 could not have written it either.
        }
        for (Path legacy : candidates) {
            if (!Files.isRegularFile(legacy)) {
                continue;
            }
            try {
                int added = customQueries.importFrom(legacy);
                customQueries.save();
                Files.move(legacy, legacy.resolveSibling(legacy.getFileName() + ".imported"));
                getLogger().info("Imported " + added + " custom queries from " + legacy);
            } catch (IOException e) {
                getLogger().warning("Could not import Statify 1.0 queries from " + legacy + ": " + e.getMessage());
            }
        }
    }
}
