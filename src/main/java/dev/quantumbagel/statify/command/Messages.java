package dev.quantumbagel.statify.command;

import dev.quantumbagel.statify.query.QueryException;
import org.bukkit.ChatColor;

final class Messages {
    private Messages() {
    }

    static String error(String message) {
        return ChatColor.RED + message;
    }

    static String success(String message) {
        return ChatColor.GREEN + message;
    }

    /** The error, then the query with everything from the problem onward underlined in red. */
    static String queryError(String source, QueryException e) {
        StringBuilder out = new StringBuilder(error(e.getMessage()));
        int at = e.position();
        if (at >= 0 && at <= source.length()) {
            out.append('\n').append(ChatColor.GRAY).append(source, 0, at)
                    .append(ChatColor.RED).append(ChatColor.UNDERLINE)
                    .append(at == source.length() ? " " : source.substring(at));
        }
        return out.toString();
    }
}
