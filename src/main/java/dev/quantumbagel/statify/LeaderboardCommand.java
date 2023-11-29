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
            sender.sendMessage(GenerateErrorMessage.generateError("Please provide a category!"));
            return false;
        }
        String playerUUID;
        try {
            playerUUID = ((Player) sender).getUniqueId().toString();
        } catch (ClassCastException ignored) {
            playerUUID = "";
        }
//        List<String> categories = Arrays.asList("custom", "killed", "mined", "broken", "used", "crafted", "picked_up", "dropped", "killed_by");
//        HashMap<String, JsonObject> stats = GetStatJson.returnStatFiles();
        int topX = 10;
        String instruction = "";
        try {
            topX = Integer.parseInt(args[0]);
        } catch (NumberFormatException ignored) { instruction = String.join(" ", args); }
        if (instruction.isEmpty() && (args[1].startsWith("-")
                || args[1].startsWith("*")
                || args[1].startsWith("/")
                || args[1].startsWith("+"))) {
            instruction = String.join(" ", args);

        }
        if (instruction.isEmpty()) {
            String[] otherArgs = Arrays.copyOfRange(args, 1, args.length);
            instruction = String.join(" ", otherArgs);
        }
        List<String> queries = QueryParser.obtainStatsFromInstruction(instruction).stream().distinct().toList();
        boolean oskeh = true;
        for (String s: queries) {
            if (!s.contains(":")) {
                oskeh = false;
                break;
            }
        }
        if (!oskeh) {
            sender.sendMessage(GenerateErrorMessage.generateError("There are no dynamic queries in this input :/"));
            return true;
        }
        if (queries.isEmpty()) {
            sender.sendMessage(GenerateErrorMessage.generateError("There are no valid queries in this input!"));
        }
        HashMap<String, List<Integer>> hash = QueryParser.calculateStatsToReplace(instruction);
        HashMap<String, String> out = QueryParser.replaceCalculatedStats(hash, instruction);
        HashMap<String, Double> toRank = new HashMap<>();
        for (Map.Entry<String, String> item: out.entrySet()) {
            toRank.put(item.getKey(), QueryParser.doTheMath(item.getValue()));
        }
        boolean isAscending = queries.size() == 1 && Objects.equals(queries.get(0).split(":", -1)[0], "killed_by");

        LinkedHashMap<String, Double> ranking = GetRanking.getLeaderboard(toRank, isAscending); // TODO: add dynamic ascending argument
        if (ranking.isEmpty()) {
            sender.sendMessage(GenerateErrorMessage.generateError("One of two things have happened!\n1. No player has generated one of the required stats.\n2. One of the custom stats failed to compile (typo in custom stat, similar error as 1., or even infinite recursion)\nPlease check for typos or just play some more Minecraft :D"));
            return true;
        }
        if (queries.size() == 1) {
            sender.sendMessage(GenerateLeaderboardMessage.generateLeaderboard(ranking, queries.get(0), playerUUID, topX));
        } else {
            sender.sendMessage(GenerateLeaderboardMessage.generateLeaderboard(ranking, "combination of " + queries.size() + " queries", playerUUID, topX));
        }
        return true;
    }
}
