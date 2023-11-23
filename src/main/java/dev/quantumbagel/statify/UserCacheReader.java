package dev.quantumbagel.statify;

import com.google.gson.Gson;
import com.google.gson.reflect.TypeToken;

import java.io.IOException;
import java.lang.reflect.Type;
import java.nio.file.Files;
import java.io.FileNotFoundException;

import java.util.HashMap;
import java.nio.file.Paths;
import java.util.List;
import java.util.Map;

import static org.bukkit.Bukkit.getServer;


public class UserCacheReader {
    static String stat_folder = getServer().getWorldContainer().getAbsolutePath();
    public static void main(String[] args) {}
    public static HashMap<String, String> getUUIDtoUsernameDict() {
        HashMap<String, String> returnMe = new HashMap<>();
        try {
            String content = Files.readString(Paths.get(stat_folder, "usercache.json"));
//
//            JsonObject reader = JsonParser.parseReader(new FileReader(stat_folder + "\\usercache.json").).getAsJsonObject();
            Gson gson = new Gson();
            Type resultType = new TypeToken<List<Map<String, String>>>(){}.getType();
            List<Map<String, String>> result = gson.fromJson(content, resultType);
            for (Map<String, String> player_pair: result) {
                returnMe.put(player_pair.get("uuid"), player_pair.get("name"));
            }
        } catch (FileNotFoundException ignored) {

        } catch (IOException e) {
            throw new RuntimeException(e);
        }
        return returnMe;
    }
}


