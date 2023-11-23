package dev.quantumbagel.statify;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import java.util.*;
import java.util.stream.Collectors;

public class GetRanking {
    public static void main(String[] args) {}
    public static LinkedHashMap<String, Integer> getRanking(String outer, String inner, boolean ascending) {
        HashMap<String, Integer> unsortedStats = new HashMap<>(){};
        HashMap<String, JsonObject> stats = GetStatJson.returnStatFiles();
        if (!Objects.equals(inner, "total")) {
            for (Map.Entry<String, JsonObject> sf : stats.entrySet()) {
                try {
                    unsortedStats.put(sf.getKey(), Integer.parseInt(
                            sf.getValue().get("stats")
                                    .getAsJsonObject()
                                    .get("minecraft:" + outer)
                                    .getAsJsonObject()
                                    .get("minecraft:" + inner)
                                    .toString()));
                } catch (Exception ignored) {}
            }
        } else {

            for (Map.Entry<String, JsonObject> sf : stats.entrySet()) {
                try {
                    int count = 0;
                    for (Map.Entry<String, JsonElement> entry :sf.getValue().get("stats")
                                  .getAsJsonObject()
                                  .get("minecraft:" + outer)
                                  .getAsJsonObject().entrySet()) {
                        count += entry.getValue().getAsInt();
                    }
                    unsortedStats.put(sf.getKey(), count);

                } catch (Exception ignored) {}
            }
        }
        return getLeaderboard(ascending, unsortedStats);
    }
    public static LinkedHashMap<String, Integer> getLeaderboard(boolean ascending, HashMap<String, Integer> unsortedStats) {
        if (unsortedStats.isEmpty()) {
            return new LinkedHashMap<>();
        }
        LinkedHashMap<String, Integer> leaderboard;
        if (ascending) {
            leaderboard = unsortedStats
                    .entrySet()
                    .stream()
                    .sorted(Map.Entry.comparingByValue())
                    .collect(Collectors.toMap(Map.Entry::getKey, Map.Entry::getValue, (oldValue, newValue) -> oldValue, LinkedHashMap::new));

        } else {
            leaderboard = unsortedStats
                    .entrySet().stream()
                    .sorted(Map.Entry.comparingByValue(Comparator.reverseOrder()))
                    .collect(Collectors.toMap(Map.Entry::getKey, Map.Entry::getValue, (oldValue, newValue) -> oldValue, LinkedHashMap::new));
        }
        return leaderboard;
    }
    public static LinkedHashMap<String, Double> getLeaderboard(HashMap<String, Double> unsortedStats, boolean ascending) {
        if (unsortedStats.isEmpty()) {
            return new LinkedHashMap<>();
        }
        LinkedHashMap<String, Double> leaderboard;
        if (ascending) {
            leaderboard = unsortedStats
                    .entrySet()
                    .stream()
                    .sorted(Map.Entry.comparingByValue())
                    .collect(Collectors.toMap(Map.Entry::getKey, Map.Entry::getValue, (oldValue, newValue) -> oldValue, LinkedHashMap::new));

        } else {
            leaderboard = unsortedStats
                    .entrySet().stream()
                    .sorted(Map.Entry.comparingByValue(Comparator.reverseOrder()))
                    .collect(Collectors.toMap(Map.Entry::getKey, Map.Entry::getValue, (oldValue, newValue) -> oldValue, LinkedHashMap::new));
        }
        return leaderboard;
    }
}
