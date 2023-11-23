package dev.quantumbagel.statify;

import org.bukkit.ChatColor;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;

import java.util.Map;

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
        String cat = strings[0];
        String outer = cat.split(":", -1)[0];
        String inner;
        try {
            inner = cat.split(":", -1)[1];
        } catch (ArrayIndexOutOfBoundsException ignored) {
            inner = "";
        }
        Map<String, String> uToU = UserCacheReader.getUUIDtoUsernameDict();
        String usernameToCheck;
        if (!playerUUID.isEmpty()) {
            usernameToCheck = uToU.get(playerUUID);
        } else {
            usernameToCheck = "";
        }
        if (!inner.isEmpty()) {
            usernameToCheck = inner;
        }
        if (usernameToCheck.isEmpty()) {
            commandSender.sendMessage(ChatColor.RED + "" + ChatColor.BOLD + "Console user, please provide a username after the category to get information about that player.");
            return true;
        }
        StoresForVerify ssv = VerifyUsername.verify(usernameToCheck);
        boolean isValidUsername = ssv.getValid();
        String legitUsername = ssv.getLegit();
        if (!isValidUsername) {
            commandSender.sendMessage(ChatColor.RED + "" + ChatColor.BOLD + "The provided username was invalid! Usage: /stat <username>:<category>");
            return true;
        }
        commandSender.sendMessage(String.valueOf(isValidUsername));
        commandSender.sendMessage(legitUsername);
        commandSender.sendMessage(outer);
        commandSender.sendMessage(inner);
        commandSender.sendMessage(usernameToCheck);
        return true;
    }
}
