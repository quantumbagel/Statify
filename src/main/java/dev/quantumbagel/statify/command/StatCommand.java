package dev.quantumbagel.statify.command;

import dev.quantumbagel.statify.StatsService;
import dev.quantumbagel.statify.query.Evaluator;
import dev.quantumbagel.statify.query.Query;
import dev.quantumbagel.statify.query.QueryException;
import dev.quantumbagel.statify.rank.Leaderboard;
import dev.quantumbagel.statify.rank.LeaderboardFormatter;
import dev.quantumbagel.statify.stats.CustomQueryStore;
import dev.quantumbagel.statify.stats.PlayerNames;
import dev.quantumbagel.statify.stats.StatTable;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Optional;
import java.util.SortedMap;
import java.util.TreeSet;
import java.util.UUID;
import org.bukkit.ChatColor;
import org.bukkit.command.Command;
import org.bukkit.command.CommandSender;
import org.bukkit.command.TabExecutor;
import org.bukkit.entity.Player;

/**
 * {@code /stat [player] <category|owner> [count]}: one player's top stats in a category
 * ({@code /stat mined}), or their result for each of someone's custom queries ({@code /stat quantumbagel}).
 */
public final class StatCommand implements TabExecutor {
    private final StatsService stats;
    private final CustomQueryStore customQueries;

    public StatCommand(StatsService stats, CustomQueryStore customQueries) {
        this.stats = stats;
        this.customQueries = customQueries;
    }

    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        List<String> rest = new ArrayList<>(Arrays.asList(args));
        int rows = LeaderboardCommand.DEFAULT_ROWS;
        if (rest.size() >= 2 && rest.get(rest.size() - 1).matches("\\d{1,4}")) {
            rows = Math.clamp(Integer.parseInt(rest.remove(rest.size() - 1)), 1, LeaderboardCommand.MAX_ROWS);
        }
        if (rest.isEmpty() || rest.size() > 2) {
            return false;
        }

        PlayerNames names = stats.names();
        StatTable table = stats.table();
        UUID target;
        if (rest.size() == 2) {
            Optional<UUID> found = names.find(rest.get(0));
            if (found.isEmpty()) {
                sender.sendMessage(Messages.error("No player named " + rest.get(0) + " has played on this server."));
                return true;
            }
            target = found.get();
        } else if (sender instanceof Player player) {
            target = player.getUniqueId();
        } else {
            sender.sendMessage(Messages.error("From the console, name a player: /" + label + " <player> <category>"));
            return true;
        }
        String targetName = names.name(target);
        if (!table.players().contains(target)) {
            sender.sendMessage(Messages.error(targetName + " has no stats yet."));
            return true;
        }

        String what = rest.get(rest.size() - 1).toLowerCase(Locale.ROOT);
        if (Query.STAT_CATEGORIES.contains(what)) {
            Map<String, Double> values = new HashMap<>();
            table.category(target, what).forEach((stat, value) -> values.put(stat, value.doubleValue()));
            sender.sendMessage(LeaderboardFormatter.format(targetName + "'s top " + what + " stats",
                    Leaderboard.rank(values, false, Comparator.naturalOrder()), s -> s, s -> false, rows));
            return true;
        }

        SortedMap<String, String> saved = customQueries.all(what);
        if (saved.isEmpty()) {
            sender.sendMessage(Messages.error("'" + what + "' is not a category or a player with custom queries. "
                    + "Categories: " + String.join(", ", new TreeSet<>(Query.STAT_CATEGORIES))));
            return true;
        }
        Map<String, Double> values = new HashMap<>();
        int broken = 0;
        for (String name : saved.keySet()) {
            try {
                Query query = Query.compile(what + ":" + name, customQueries);
                values.put(what + ":" + name, Evaluator.evaluate(query.expr(),
                        ref -> table.get(target, ref.left(), ref.right())));
            } catch (QueryException e) {
                broken++;
            }
        }
        StringBuilder message = new StringBuilder(LeaderboardFormatter.format(
                targetName + "'s results for " + what + "'s custom queries",
                Leaderboard.rank(values, false, Comparator.naturalOrder()), s -> s, s -> false, rows));
        if (broken > 0) {
            message.append('\n').append(ChatColor.GRAY).append(broken)
                    .append(broken == 1 ? " query is" : " queries are").append(" broken; see /custom list ")
                    .append(what);
        }
        sender.sendMessage(message.toString());
        return true;
    }

    @Override
    public List<String> onTabComplete(CommandSender sender, Command command, String label, String[] args) {
        TreeSet<String> options = new TreeSet<>(Query.STAT_CATEGORIES);
        options.addAll(customQueries.owners());
        if (args.length == 1) {
            options.addAll(stats.cachedNames().names());
            return Completions.matching(args[0], options);
        }
        if (args.length == 2) {
            return Completions.matching(args[1], options);
        }
        return List.of();
    }
}
