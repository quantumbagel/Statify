package dev.quantumbagel.statify;

import org.bukkit.plugin.java.JavaPlugin;

public class Statify extends JavaPlugin {
    @Override
    public void onEnable() {
        this.getCommand("leaderboard").setExecutor(new LeaderboardCommand());
        this.getCommand("stat").setExecutor(new StatCommand());
    }
}


