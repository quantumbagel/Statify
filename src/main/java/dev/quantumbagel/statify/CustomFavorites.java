package dev.quantumbagel.statify;

import com.google.gson.*;
import com.google.gson.reflect.TypeToken;

import java.io.*;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.Map;

import static org.bukkit.Bukkit.getServer;

public class CustomFavorites {
    static final String fav_folder = getServer().getWorldContainer().getAbsolutePath() + "\\statify.json";
    static JsonObject jsonObj;

    static {
        try {
            jsonObj = JsonParser.parseReader(new FileReader(fav_folder)).getAsJsonObject();
        } catch (FileNotFoundException ignored) {
            try {
                createFile();
            } catch (IOException e) {
                throw new RuntimeException(e);
            }
        }
    }
    public static void createFile() throws IOException {
        File f = new File(fav_folder);
        if (!f.exists()) {
            f.createNewFile();
            FileWriter myWriter = new FileWriter(fav_folder);
            myWriter.write("{}");
            myWriter.close();
        }

    }
    public static HashMap<String, Map<String, String>> getFileContents() throws IOException {
        Map<String, JsonElement> currentlyThere = new JsonParser().parse(new FileReader(fav_folder)).getAsJsonObject().asMap();
        HashMap<String, Map<String, String>> toReturn = new HashMap<>();
        for (Map.Entry<String, JsonElement> item: currentlyThere.entrySet()) {
            Map<String, String> subMap = new HashMap<>();
            for (Map.Entry<String, JsonElement> entry: item.getValue().getAsJsonObject().entrySet()) {
                String val = entry.getValue().toString().replace("\"", "");
                subMap.put(entry.getKey(), val);
            }
            toReturn.put(item.getKey(), subMap);
        }
        return toReturn;
    }
    public static void dumpToFile(HashMap<String, Map<String, String>> json) throws IOException {
        try (Writer writer = new FileWriter(fav_folder)) {
            Gson gson = new GsonBuilder().create();
            gson.toJson(json, writer);
        }
    }
    public static boolean set(String username, String commandName, String commandValue) {
        try {
            HashMap<String, Map<String, String>> returned = CustomFavorites.getFileContents();
            Map<String, String> existingMap = returned.get(username);
            if (existingMap == null) {
                existingMap = new HashMap<>();
            }
            existingMap.put(commandName, commandValue);
            returned.put(username, existingMap);
            CustomFavorites.dumpToFile(returned);
            return true;

        } catch (IOException ignored) {return false;}
    }

    public static String get(String username, String commandName) {
        try {
            HashMap<String, Map<String, String>> returned = CustomFavorites.getFileContents();
            Map<String, String> existingMap = returned.get(username);
            if (existingMap == null) {
                existingMap = new HashMap<>();
            }
            // Key doesn't exist, signal that by returning an empty string
            return existingMap.getOrDefault(commandName, "");
        } catch (IOException ignored) {}
        return "";
    }
    public static boolean remove(String username, String commandName) {
        try {
            HashMap<String, Map<String, String>> returned = CustomFavorites.getFileContents();
            Map<String, String> existingMap = returned.get(username);
            if (existingMap == null) {
                existingMap = new HashMap<>();
            }
            if (!existingMap.containsKey(commandName)) {
                return false;
            }
            existingMap.remove(commandName);
            returned.put(username, existingMap);
            CustomFavorites.dumpToFile(returned);
            return true;

        } catch (IOException ignored) {return false;}
    }
    public static HashMap<String, String> getAll(String username) {
        try {
            HashMap<String, String> hashBrown = new HashMap<>();
            JsonObject parseMe = jsonObj.get(username).getAsJsonObject();
            for (Map.Entry<String, JsonElement> item: parseMe.entrySet()) {
                hashBrown.put(item.getKey(), item.getValue().toString());
            }
            return hashBrown;
        } catch (JsonParseException ignored) {
            return null;
        }
    }
}

