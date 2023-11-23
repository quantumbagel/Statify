package dev.quantumbagel.statify;

import com.google.gson.JsonObject;
import com.google.gson.JsonParser;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileReader;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import static org.bukkit.Bukkit.getConsoleSender;
//import static org.bukkit.Bukkit.getServer;

public class GetStatJson {
    // static String stat_folder = getServer().getWorldContainer().getAbsolutePath() + "/world/stats";
    static String stat_folder = "C:\\Users\\Julian Reder\\Documents\\spigot\\world\\stats";
    public static void main(String[] args) {}
    public static HashMap<String, JsonObject> returnStatFiles() {
        HashMap<String, JsonObject> statFiles = new HashMap<>();
        File[] statList = new File(stat_folder).listFiles();
        if (statList == null) {
            return new HashMap<>();
        }
        for (File f: statList) {
            try {
                statFiles.put(f.getName().substring(0, f.getName().length() - 5), (JsonObject) JsonParser.parseReader(new FileReader(stat_folder + "/" + f.getName())));
            }
            catch(FileNotFoundException ignored) {}
        }
        return statFiles;
    }

}
