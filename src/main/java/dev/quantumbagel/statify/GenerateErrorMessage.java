package dev.quantumbagel.statify;

import org.bukkit.ChatColor;

public class GenerateErrorMessage {
    final static boolean isBold = true;

    static final ChatColor errorColor = ChatColor.RED;

    public static String generateError(String error) {
        if (isBold) {
            return ChatColor.BOLD + "" + errorColor + error;
        }
        return errorColor + error;
    }
}
