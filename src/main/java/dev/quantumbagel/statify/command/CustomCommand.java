package dev.quantumbagel.statify.command;

import dev.quantumbagel.statify.StatsService;
import dev.quantumbagel.statify.query.Query;
import dev.quantumbagel.statify.query.QueryException;
import dev.quantumbagel.statify.stats.CustomQueryStore;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.SortedMap;
import java.util.logging.Level;
import java.util.logging.Logger;
import org.bukkit.ChatColor;
import org.bukkit.command.Command;
import org.bukkit.command.CommandSender;
import org.bukkit.command.TabExecutor;
import org.bukkit.entity.Player;

/**
 * {@code /custom list [player]}, {@code /custom get [player] <name>}, {@code /custom set <name> <query>},
 * {@code /custom delete <name>}. Saved queries are used as {@code owner:name} in any other query.
 */
public final class CustomCommand implements TabExecutor {
    static final int MAX_QUERIES_PER_PLAYER = 50;
    private static final String NAME_PATTERN = "[a-z0-9_]{1,32}";
    private static final List<String> SUBCOMMANDS = List.of("list", "get", "set", "delete");

    private final StatsService stats;
    private final CustomQueryStore store;
    private final Logger logger;

    public CustomCommand(StatsService stats, CustomQueryStore store, Logger logger) {
        this.stats = stats;
        this.store = store;
        this.logger = logger;
    }

    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        if (args.length == 0) {
            return false;
        }
        String[] rest = Arrays.copyOfRange(args, 1, args.length);
        return switch (args[0].toLowerCase(Locale.ROOT)) {
            case "list" -> list(sender, rest);
            case "get" -> get(sender, rest);
            case "set" -> set(sender, rest);
            case "delete", "remove" -> delete(sender, rest);
            default -> false;
        };
    }

    private boolean list(CommandSender sender, String[] args) {
        if (args.length > 1) {
            return false;
        }
        String owner = args.length == 1 ? args[0] : ownName(sender);
        if (owner == null) {
            sender.sendMessage(Messages.error("From the console, name a player: /custom list <player>"));
            return true;
        }
        SortedMap<String, String> queries = store.all(owner);
        if (queries.isEmpty()) {
            sender.sendMessage(ChatColor.GRAY + owner + " has no custom queries.");
            return true;
        }
        StringBuilder out = new StringBuilder()
                .append(ChatColor.GOLD).append(ChatColor.BOLD).append(owner).append("'s custom queries");
        for (Map.Entry<String, String> query : queries.entrySet()) {
            out.append('\n').append(ChatColor.GREEN).append(owner.toLowerCase(Locale.ROOT)).append(':')
                    .append(query.getKey()).append(ChatColor.GRAY).append(" = ").append(ChatColor.WHITE)
                    .append(query.getValue());
        }
        sender.sendMessage(out.toString());
        return true;
    }

    private boolean get(CommandSender sender, String[] args) {
        String owner;
        String name;
        if (args.length == 1) {
            owner = ownName(sender);
            name = args[0];
            if (owner == null) {
                sender.sendMessage(Messages.error("From the console, name a player: /custom get <player> <name>"));
                return true;
            }
        } else if (args.length == 2) {
            owner = args[0];
            name = args[1];
        } else {
            return false;
        }
        store.lookup(owner, name).ifPresentOrElse(
                source -> sender.sendMessage(ChatColor.GREEN + owner.toLowerCase(Locale.ROOT) + ":"
                        + name.toLowerCase(Locale.ROOT) + ChatColor.GRAY + " = " + ChatColor.WHITE + source),
                () -> sender.sendMessage(Messages.error(owner + " has no custom query named " + name + ".")));
        return true;
    }

    private boolean set(CommandSender sender, String[] args) {
        if (!(sender instanceof Player player)) {
            sender.sendMessage(Messages.error("Only players can save custom queries."));
            return true;
        }
        if (args.length < 2) {
            return false;
        }
        String owner = player.getName().toLowerCase(Locale.ROOT);
        String name = args[0].toLowerCase(Locale.ROOT);
        String source = String.join(" ", Arrays.copyOfRange(args, 1, args.length));
        if (Query.STAT_CATEGORIES.contains(owner)) {
            // Queries are referenced as owner:name, which would shadow this category's stats.
            sender.sendMessage(Messages.error("Your username is also a stat category, so you can't save queries."));
            return true;
        }
        if (!name.matches(NAME_PATTERN)) {
            sender.sendMessage(Messages.error("Query names can only use letters, numbers and _ (up to 32)."));
            return true;
        }
        boolean replacing = store.lookup(owner, name).isPresent();
        if (!replacing && store.all(owner).size() >= MAX_QUERIES_PER_PLAYER) {
            sender.sendMessage(Messages.error("You already have " + MAX_QUERIES_PER_PLAYER
                    + " custom queries. Delete one first."));
            return true;
        }
        try {
            Query.compileAs(owner, name, source, store);
        } catch (QueryException e) {
            sender.sendMessage(Messages.queryError(source, e));
            return true;
        }
        try {
            store.set(owner, name, source);
        } catch (IOException e) {
            logger.log(Level.WARNING, "Could not save custom queries", e);
            sender.sendMessage(Messages.error("The server could not save your query. Ask an admin to check the log."));
            return true;
        }
        sender.sendMessage(Messages.success((replacing ? "Updated " : "Saved ") + owner + ":" + name
                + ". Try /leaderboard " + owner + ":" + name));
        return true;
    }

    private boolean delete(CommandSender sender, String[] args) {
        if (!(sender instanceof Player player)) {
            sender.sendMessage(Messages.error("Only players can delete their custom queries."));
            return true;
        }
        if (args.length != 1) {
            return false;
        }
        String owner = player.getName().toLowerCase(Locale.ROOT);
        String name = args[0].toLowerCase(Locale.ROOT);
        String key = owner + ":" + name;
        try {
            if (!store.remove(owner, name)) {
                sender.sendMessage(Messages.error("You have no custom query named " + name + "."));
                return true;
            }
        } catch (IOException e) {
            logger.log(Level.WARNING, "Could not save custom queries", e);
            sender.sendMessage(Messages.error("The server could not save the change. Ask an admin to check the log."));
            return true;
        }
        sender.sendMessage(Messages.success("Deleted " + key + "."));
        List<String> dependents = new ArrayList<>();
        for (String otherOwner : store.owners()) {
            store.all(otherOwner).forEach((otherName, source) -> {
                if (Query.directReferences(source).contains(key)) {
                    dependents.add(otherOwner + ":" + otherName);
                }
            });
        }
        if (!dependents.isEmpty()) {
            sender.sendMessage(ChatColor.YELLOW + "These queries used it and will stop working: "
                    + String.join(", ", dependents));
        }
        return true;
    }

    private static String ownName(CommandSender sender) {
        return sender instanceof Player player ? player.getName() : null;
    }

    @Override
    public List<String> onTabComplete(CommandSender sender, Command command, String label, String[] args) {
        if (args.length == 1) {
            return Completions.matching(args[0], SUBCOMMANDS);
        }
        String sub = args[0].toLowerCase(Locale.ROOT);
        String own = ownName(sender);
        List<String> ownQueries = own == null ? List.of() : new ArrayList<>(store.all(own).keySet());
        if (args.length == 2) {
            return switch (sub) {
                case "list" -> Completions.matching(args[1], stats.cachedNames().names());
                case "get" -> {
                    List<String> options = new ArrayList<>(ownQueries);
                    options.addAll(stats.cachedNames().names());
                    yield Completions.matching(args[1], options);
                }
                case "delete", "remove" -> Completions.matching(args[1], ownQueries);
                default -> List.of();
            };
        }
        if (args.length == 3 && sub.equals("get")) {
            return Completions.matching(args[2], store.all(args[1]).keySet());
        }
        if (sub.equals("set")) {
            return Completions.query(args[args.length - 1], stats, store);
        }
        return List.of();
    }
}
