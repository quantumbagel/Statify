package dev.quantumbagel.statify;

import org.bukkit.ChatColor;

import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Objects;

public class GenerateLeaderboardMessage {
    final static ChatColor firstPlace = ChatColor.GOLD;
    final static ChatColor secondPlace = ChatColor.DARK_PURPLE;
    final static ChatColor thirdPlace = ChatColor.DARK_GREEN;
    final static ChatColor defaultPlace = ChatColor.DARK_GRAY;
    final static ChatColor leaderboardColor = ChatColor.GOLD;
    final static ChatColor myNameColor = ChatColor.BLUE;
    final static ChatColor otherNameColor = ChatColor.GRAY;

    public static void main(String[] args) {
    }

    public static String generateLeaderboard(LinkedHashMap<String, Double> leaderboard, String leaderboardName, String myUUID, int topX) {
        int currentPlace = 1;
        int trackIndex = 1;
        int maximumUsernameLength = 0;
        Double addendSize = null;
        HashMap<String, String> conversion = UserCacheReader.getUUIDtoUsernameDict();
        for (Map.Entry<String, Double> item : leaderboard.entrySet()) {
            int testLength = conversion.getOrDefault(item.getKey(), "Unknown Player").length();
            if (testLength > maximumUsernameLength) {
                maximumUsernameLength = testLength;
            }
        }
        StringBuilder leaderboardToSend = new StringBuilder();
        leaderboardToSend
                .append(ChatColor.BOLD)
                .append(leaderboardColor)
                .append("Leaderboard for ")
                .append(leaderboardName).append("\n")
                .append(ChatColor.RESET);
        for (Map.Entry<String, Double> item : leaderboard.entrySet()) {
            ChatColor name;
            String placeText;
            ChatColor placeColor;
            String spacesBefore = "   ";
            String addend = "";
            boolean forceTrackUpdate = false;
            for (Map.Entry<String, Double> ite : leaderboard.entrySet()) {
                if (Objects.equals(ite.getValue(), item.getValue()) && (!Objects.equals(ite.getKey(), item.getKey()))) {
                    addend = "=";
                    if (addendSize == null) {
                        forceTrackUpdate = true;
                        addendSize = ite.getValue();
                    } else if (!addendSize.equals(ite.getValue())) {
                        forceTrackUpdate = true;
                        addendSize = ite.getValue();
                    }
                    break;
                }
            }
            if (addend.isEmpty()) {
                addendSize = null;
            }
            if (Objects.equals(myUUID, item.getKey())) {
                name = myNameColor;
            } else {
                name = otherNameColor;
            }
            String playerName;
            if (addend.isEmpty() || forceTrackUpdate) {
                currentPlace = trackIndex;
            }
            if (currentPlace == 1) {
                placeText = "1st";
                placeColor = firstPlace;
            } else if (currentPlace == 2) {
                placeText = "2nd";
                placeColor = secondPlace;
            } else if (currentPlace == 3) {
                placeText = "3rd";
                placeColor = thirdPlace;
            } else {
                placeText = currentPlace + "th";
                placeColor = defaultPlace;
            }
            placeText = addend + placeText;
            Object value = item.getValue();
            if (((Double) value == Math.floor((Double) value)) && !Double.isInfinite((Double) value)) {
                value = ((Double) value).intValue();
            }
            playerName = conversion.getOrDefault(item.getKey(), "Unknown Player");
            leaderboardToSend
                    .append(placeColor)
                    .append(ChatColor.BOLD)
                    .append(placeText)
                    .append(ChatColor.RESET)
                    .append(spacesBefore)
                    .append(name)
                    .append(" ")
                    .append(playerName)
                    .append(" (")
                    .append(value).append(")")
                    .append("\n");
            trackIndex++;
            if (trackIndex == topX && leaderboard.size() != trackIndex) {
                leaderboardToSend
                        .append(ChatColor.RESET)
                        .append(ChatColor.ITALIC)
                        .append("Note: results are truncated. Showing top ")
                        .append(topX)
                        .append(" of ")
                        .append(leaderboard.size())
                        .append(".\n");
                break;
            }

        }
        return leaderboardToSend.substring(0, leaderboardToSend.length() - 1); // remove trailing newline

    }

    public static String generateUsernameBasedLeaderboard(LinkedHashMap<String, Double> leaderboard, String leaderboardName, String myUsername, int topX) {
        int currentPlace = 1;
        int trackIndex = 1;
        int maximumUsernameLength = 0;
        Double addendSize = null;
        for (Map.Entry<String, Double> item : leaderboard.entrySet()) {
            int testLength = item.getKey().length();
            if (testLength > maximumUsernameLength) {
                maximumUsernameLength = testLength;
            }
        }
        StringBuilder leaderboardToSend = new StringBuilder();
        leaderboardToSend
                .append(ChatColor.BOLD)
                .append(leaderboardColor)
                .append("Leaderboard for ")
                .append(leaderboardName).append("\n")
                .append(ChatColor.RESET);
        for (Map.Entry<String, Double> item : leaderboard.entrySet()) {
            ChatColor name;
            String placeText;
            ChatColor placeColor;
            String spacesBefore = " ";
            String addend = "";
            boolean forceTrackUpdate = false;
            for (Map.Entry<String, Double> ite : leaderboard.entrySet()) {
                if (Objects.equals(ite.getValue(), item.getValue()) && (!Objects.equals(ite.getKey(), item.getKey()))) {
                    addend = "=";
                    if (addendSize == null) {
                        forceTrackUpdate = true;
                        addendSize = ite.getValue();
                    } else if (!addendSize.equals(ite.getValue())) {
                        forceTrackUpdate = true;
                        addendSize = ite.getValue();
                    }
                    break;
                }
            }
            if (addend.isEmpty()) {
                addendSize = null;
            }
            if (Objects.equals(myUsername, item.getKey())) {
                name = myNameColor;
            } else {
                name = otherNameColor;
            }
            if (addend.isEmpty() || forceTrackUpdate) {
                currentPlace = trackIndex;
            }
            if (currentPlace == 1) {
                placeText = "1st";
                placeColor = firstPlace;
            } else if (currentPlace == 2) {
                placeText = "2nd";
                placeColor = secondPlace;
            } else if (currentPlace == 3) {
                placeText = "3rd";
                placeColor = thirdPlace;
            } else {
                placeText = currentPlace + "th";
                placeColor = defaultPlace;
            }
            placeText = addend + placeText;
            Object value = item.getValue();
            if (((Double) value == Math.floor((Double) value)) && !Double.isInfinite((Double) value)) {
                value = ((Double) value).intValue();
            }
            leaderboardToSend
                    .append(placeColor)
                    .append(ChatColor.BOLD)
                    .append(placeText)
                    .append(ChatColor.RESET)
                    .append(spacesBefore)
                    .append(name)
                    .append(" ")
                    .append(item.getKey())
                    .append(" (")
                    .append(value).append(")")
                    .append("\n");
            trackIndex++;
            if (trackIndex == topX && leaderboard.size() != trackIndex) {
                leaderboardToSend
                        .append(ChatColor.RESET)
                        .append(ChatColor.ITALIC)
                        .append("Note: results are truncated. Showing top ")
                        .append(topX)
                        .append(" of ")
                        .append(leaderboard.size())
                        .append(".\n");
                break;
            }
        }
        return leaderboardToSend.substring(0, leaderboardToSend.length() - 1); // remove trailing newline
    }
}