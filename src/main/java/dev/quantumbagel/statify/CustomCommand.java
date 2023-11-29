package dev.quantumbagel.statify;

import com.google.gson.Gson;
import org.bukkit.ChatColor;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.command.ConsoleCommandSender;

import java.io.IOException;
import java.util.Arrays;
import java.util.HashMap;
import java.util.Map;

public class CustomCommand implements CommandExecutor {
    @Override
    public boolean onCommand(CommandSender commandSender, Command command, String s, String[] strings) {
        boolean isConsole = commandSender instanceof ConsoleCommandSender;
        String username = "";
        if (!isConsole) {
            username = commandSender.getName();
        }
        switch (strings[0]) {
            case "list":
                if (username.isEmpty() && strings.length > 1) {
                    username = strings[1];
                } else if (username.isEmpty()) {
                    commandSender.sendMessage(GenerateErrorMessage.generateError("Console cannot yet create custom commands, and cannot thus view them! Support for this may be added in the future!"));
                    return true;
                }
                HashMap<String, String> allFavs = CustomFavorites.getAll(username);
                StringBuilder toSend = new StringBuilder();
                toSend.append(ChatColor.GOLD).append(ChatColor.BOLD).append("List of ").append(username).append("'s custom commands:\n").append(ChatColor.RESET).append(ChatColor.GREEN);
                for (Map.Entry<String, String> item : allFavs.entrySet()) {
                    toSend.append(username).append(":").append(item.getKey()).append(" = \"").append(item.getValue()).append("\"\n");
                }
                commandSender.sendMessage(toSend.substring(0, toSend.length() - 1));
                return true;
            case "set":
                if (!isConsole) {
                    boolean success = CustomFavorites.set(username, strings[1], String.join(" ", Arrays.asList(strings).subList(2, strings.length)));
                    if (success) {
                        commandSender.sendMessage(ChatColor.GREEN + "Succesfully set command \""+strings[1]+"\" to \""+String.join(" ", Arrays.asList(strings).subList(2, strings.length))+"\"!");
                    } else {
                        commandSender.sendMessage(GenerateErrorMessage.generateError("Failed to set value due to FileIOException! This is due to a server-side file permissions error, so talk with your admin :)"));
                    }
                } else {
                    commandSender.sendMessage(GenerateErrorMessage.generateError("Console cannot yet create custom commands! Support for this may be added in the future!"));
                }
                return true;
            case "get":
                if (strings.length == 2 && !username.isEmpty()) {
                    String getVal = CustomFavorites.get(username, strings[1]);
                    if (!getVal.isEmpty()) {
                        commandSender.sendMessage(ChatColor.GREEN + username+ ":"+strings[1]+" = \"" + getVal + "\"");
                    } else {
                        commandSender.sendMessage(GenerateErrorMessage.generateError("Failed to get value due to invalid key!"));
                    }

                } else if (strings.length == 3 && !username.isEmpty()) {
                    String getVal = CustomFavorites.get(username, strings[2]);
                    if (!getVal.isEmpty()) {
                        commandSender.sendMessage(ChatColor.GREEN + username+ ":"+strings[2]+" = \"" + getVal + "\"");
                    } else {
                        commandSender.sendMessage(GenerateErrorMessage.generateError("Failed to get value due to invalid key!"));
                    }
                }
                else {
                    commandSender.sendMessage(GenerateErrorMessage.generateError("One of two things has happened:\n1. You are the console and you did not provide a username.\n2. You have an illegal amount of arguments.\nPlease read the documentation."));
                }
                return true;
            case "delete":
                if (!isConsole) {
                    boolean success = CustomFavorites.remove(username, strings[1]);
                    if (success) {
                        commandSender.sendMessage(ChatColor.GREEN + "Successfully deleted command \""+strings[1]+"\"!");
                    } else {
                        commandSender.sendMessage(GenerateErrorMessage.generateError("Key does not exist!"));
                    }
                } else {
                    commandSender.sendMessage(GenerateErrorMessage.generateError("Console cannot yet create (or delete) custom commands! Support for this may be added in the future!"));
                }
                return true;
            default:
                commandSender.sendMessage(GenerateErrorMessage.generateError("Invalid subargument (try add, get, list, or delete)!"));
        }
        return true;
    }
}
