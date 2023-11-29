package dev.quantumbagel.statify;

import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import org.bukkit.ChatColor;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import org.checkerframework.checker.units.qual.A;

import java.util.*;
import java.util.stream.Collectors;

public class StatCommand implements CommandExecutor {
    @Override
    public boolean onCommand(CommandSender commandSender, Command command, String s, String[] strings)
    {
        String playerUUID;
        try {
            playerUUID = ((Player) commandSender).getUniqueId().toString();
        } catch (ClassCastException ignored) {
            playerUUID = "";
        }
        String category = strings[0];
        String username;
        try {
            String checkTemp = strings[1];
            username = category;
            category = checkTemp;
        } catch (ArrayIndexOutOfBoundsException ignored) {
            username = "";
        }
        Map<String, String> uToU = UserCacheReader.getUUIDtoUsernameDict();
        List<String> lowercaseUsernames = new ArrayList<>();
        List<String> normalUsernames = new ArrayList<>();
        for (String item: uToU.values()) {
            lowercaseUsernames.add(item.toLowerCase());
            normalUsernames.add(item);
        }
        boolean isCustom = false;
        String userCustomBank = "";

        if (lowercaseUsernames.contains(category.toLowerCase())) {
            isCustom = true;
            userCustomBank = normalUsernames.get(lowercaseUsernames.indexOf(category.toLowerCase()));
        }
        Map<String, String> uToUReal;
        try {
            uToUReal = uToU.entrySet().stream().collect(Collectors.toMap(Map.Entry::getValue, Map.Entry::getKey));
        } catch (IllegalStateException e){
            commandSender.sendMessage(GenerateErrorMessage.generateError("There are two statfiles that claim to be the same username! Please discuss this with your admin!"));
            commandSender.sendMessage(GenerateErrorMessage.generateError("Error:" + e));
            return true;
        }
        String usernameToCheck;
        if (!playerUUID.isEmpty()) {
            usernameToCheck = uToU.get(playerUUID);
        } else {
            usernameToCheck = "";
        }
        if (!username.isEmpty()) {
            usernameToCheck = username;
        }
        if (usernameToCheck.isEmpty()) {
            commandSender.sendMessage(GenerateErrorMessage.generateError("Console user, please provide a category after the username to get information about that player."));
            return true;
        }
        StoresForVerify ssv = VerifyUsername.verify(usernameToCheck);
        boolean isValidUsername = ssv.getValid();
        String legitUsername = ssv.getLegit();
        if (!isValidUsername) {
            commandSender.sendMessage(GenerateErrorMessage.generateError("The provided username was invalid! Usage: /stat <username> <category>"));
            return true;
        }
        HashMap<String, JsonObject> stats = GetStatJson.returnStatFiles();
        if (!isCustom) {
            JsonObject statsToAnalyze = stats.get(uToUReal.get(legitUsername))
                    .get("stats")
                    .getAsJsonObject()
                    .get("minecraft:" + category).getAsJsonObject();
            HashMap<String, Double> unsortedStats = new HashMap<>();
            for (Map.Entry<String, JsonElement> entry : statsToAnalyze.entrySet()) {
                unsortedStats.put(entry.getKey().replace("minecraft:", ""), entry.getValue().getAsDouble());
            }
            LinkedHashMap<String, Double> ranking = GetRanking.getLeaderboard(unsortedStats, false); // TODO: dynamic?
            String message = GenerateLeaderboardMessage.generateUsernameBasedLeaderboard(ranking, legitUsername + "'s " + category + " ranking", legitUsername, 10);
            commandSender.sendMessage(message);
        } else {
            commandSender.sendMessage(userCustomBank);
            HashMap<String, String> categories = CustomFavorites.getAll(userCustomBank);
            HashMap<String, Double> toRank = new HashMap<>();
            for (Map.Entry<String, String> entry: categories.entrySet()) {
                HashMap<String, List<Integer>> calculatedStats = QueryParser.calculateStatsToReplace(entry.getValue());
                HashMap<String, String> calcStat = QueryParser.replaceCalculatedStats(calculatedStats, entry.getValue());
                if (calcStat.containsKey(uToUReal.get(username))) {
                    if (calcStat.get(uToUReal.get(username)) != null) {
                        toRank.put(entry.getKey(), QueryParser.doTheMath(calcStat.get(uToUReal.get(username)).replace("\"", "")));
                    }
                }
            }
            LinkedHashMap<String, Double> ranking = GetRanking.getLeaderboard(toRank, false); // TODO: dynamic?
            String message = GenerateLeaderboardMessage.generateUsernameBasedLeaderboard(ranking, legitUsername + "'s " + category + " ranking", legitUsername, 10);
            commandSender.sendMessage(message);
        }
        return true;
    }
}
