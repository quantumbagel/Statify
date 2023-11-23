package dev.quantumbagel.statify;

import org.bukkit.ChatColor;

import java.util.ArrayList;
import java.util.Map;
import java.util.Objects;

public class VerifyUsername {
    public static void main(String[] args) {}
    public static StoresForVerify verify(String username) {
        Map<String, String> uToU = UserCacheReader.getUUIDtoUsernameDict();
        boolean isValidUsername = false;
        String legitUsername = "";
        for (
                Map.Entry<String, String> validEntry : uToU.entrySet()) {
            if (validEntry.getValue().equalsIgnoreCase(username)) {
                isValidUsername = true;
                legitUsername = validEntry.getKey();
                break;
            }
        }
        StoresForVerify ssv = new StoresForVerify();
        ssv.set(isValidUsername, legitUsername);
        return ssv;
    }
}

class StoresForVerify {
    private boolean valid;
    private String legit;

    public static void main(String[] args) {}
    public void set(boolean valid, String legit) {
        this.valid = valid;
        this.legit = legit;
    }
    public boolean getValid() {
        return this.valid;
    }
    public String getLegit() {
        return this.legit;
    }
}
