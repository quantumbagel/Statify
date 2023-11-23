package dev.quantumbagel.statify;

import com.google.gson.JsonObject;
import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.Material;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;

import java.lang.reflect.Array;
import java.util.*;
import java.util.stream.Collectors;

public class LeaderboardCommand implements CommandExecutor {
    @Override
    public boolean onCommand(CommandSender sender, Command cmd, String label, String[] args) {
        if (args.length == 0) {
            sender.sendMessage(ChatColor.RED + "" + ChatColor.BOLD + "Please provide a category!");
            return false;
        }
        String category = args[0]; // /leaderboard only uses the first argument
        String playerUUID;
        try {
            playerUUID = ((Player) sender).getUniqueId().toString();
        } catch (ClassCastException ignored) {
            playerUUID = "";
        }
        List<String> categories = Arrays.asList("custom", "killed", "mined", "broken", "used", "crafted", "picked_up", "dropped", "killed_by");
        HashMap<String, JsonObject> stats = GetStatJson.returnStatFiles();
        if (!category.contains(":")) {
            if (categories.contains(category)) {
                Map<String, String> uToU = UserCacheReader.getUUIDtoUsernameDict();
                Map<String, String> uToUReal;
                try {
                    uToUReal = uToU.entrySet().stream().collect(Collectors.toMap(Map.Entry::getValue, Map.Entry::getKey));
                } catch (IllegalStateException e){
                    sender.sendMessage(ChatColor.RED + "" + ChatColor.BOLD + "There are two statfiles that claim to be the same username! Please discuss this with your admin!");
                    sender.sendMessage(ChatColor.RED + "" + ChatColor.BOLD + "Error:" + e);
                    return true;
                }
                String usernameToCheck;
                if (!playerUUID.isEmpty()) {
                    usernameToCheck = uToU.get(playerUUID);
                } else {
                    usernameToCheck = "";
                }
                if (args.length > 1) {
                    usernameToCheck = args[1];
                }
                if (usernameToCheck.isEmpty()) {
                    sender.sendMessage(ChatColor.RED + "" + ChatColor.BOLD + "Console user, please provide a username after the category to get information about that player.");
                }
                StoresForVerify ssv = VerifyUsername.verify(usernameToCheck);
                boolean isValidUsername = ssv.getValid();
                String legitUsername = ssv.getLegit();
                if (isValidUsername) {
                    sender.sendMessage(stats.get(uToUReal.get(legitUsername))
                            .get("stats")
                            .getAsJsonObject()
                            .get("minecraft:" + category)
                            .toString());
                } else {
                    sender.sendMessage(ChatColor.RED + "" + ChatColor.BOLD + "Please use a valid username - or no username to get your own information.");
                }
            } else {
                sender.sendMessage(ChatColor.RED + "" + ChatColor.BOLD + "That's not a valid category! Try one of the following: custom, killed, mined, broken, used, crafted, picked_up, dropped, or killed_by.");
            }
        } else {
            String instruction = String.join(" ", args);
            List<String> queries = QueryParser.obtainStatsFromInstruction(instruction).stream().distinct().toList();
            if (queries.isEmpty()) {
                sender.sendMessage(ChatColor.RED + "" + ChatColor.BOLD + "You aren't using any dynamic stats - this is just the same for everyone :/");
            }
            HashMap<String, List<Integer>> hash = QueryParser.calculateStatsToReplace(instruction);
            HashMap<String, String> out = QueryParser.replaceCalculatedStats(hash, instruction);
            HashMap<String, Double> toRank = new HashMap<>();
            for (Map.Entry<String, String> item: out.entrySet()) {
                toRank.put(item.getKey(), QueryParser.doTheMath(item.getValue()));
            }
            LinkedHashMap<String, Double> ranking = GetRanking.getLeaderboard(toRank, false); // TODO: add dynamic ascending argument
            if (ranking.isEmpty()) {
                sender.sendMessage(ChatColor.RED + "" + ChatColor.BOLD + "No player has generated that stat yet! Please check for typos or just play some more Minecraft :D");
                return true;
            }
            if (queries.size() == 1) {
                sender.sendMessage(GenerateLeaderboardMessage.generateLeaderboard(ranking, queries.get(0), playerUUID));
            } else {
                sender.sendMessage(GenerateLeaderboardMessage.generateLeaderboard(ranking, "combination of " + queries.size() + " queries", playerUUID));
            }
        }
        return true;
    }
}
