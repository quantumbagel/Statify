package dev.quantumbagel.statify.command;

import dev.quantumbagel.statify.StatsService;
import dev.quantumbagel.statify.query.Evaluator;
import dev.quantumbagel.statify.query.Query;
import dev.quantumbagel.statify.query.QueryException;
import dev.quantumbagel.statify.rank.Leaderboard;
import dev.quantumbagel.statify.rank.LeaderboardFormatter;
import dev.quantumbagel.statify.rank.Standing;
import dev.quantumbagel.statify.stats.CustomQueryStore;
import dev.quantumbagel.statify.stats.PlayerNames;
import dev.quantumbagel.statify.stats.StatTable;
import java.util.Arrays;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import org.bukkit.command.Command;
import org.bukkit.command.CommandSender;
import org.bukkit.command.TabExecutor;
import org.bukkit.entity.Player;

/** {@code /leaderboard [count] <query>} ranks every player by a stat or an expression of stats. */
public final class LeaderboardCommand implements TabExecutor {
    static final int DEFAULT_ROWS = 10;
    static final int MAX_ROWS = 50;

    private final StatsService stats;
    private final CustomQueryStore customQueries;

    public LeaderboardCommand(StatsService stats, CustomQueryStore customQueries) {
        this.stats = stats;
        this.customQueries = customQueries;
    }

    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        if (args.length == 0) {
            return false;
        }
        int rows = DEFAULT_ROWS;
        int queryStart = 0;
        // "/lb 5 killed:zombie" shows 5 rows, but "/lb 5 * killed:zombie" is a query.
        if (args.length > 1 && args[0].matches("\\d{1,4}") && !startsWithOperator(args[1])) {
            rows = Math.clamp(Integer.parseInt(args[0]), 1, MAX_ROWS);
            queryStart = 1;
        }
        String source = String.join(" ", Arrays.copyOfRange(args, queryStart, args.length));

        Query query;
        try {
            query = Query.compile(source, customQueries);
        } catch (QueryException e) {
            sender.sendMessage(Messages.queryError(source, e));
            return true;
        }

        StatTable table = stats.table();
        PlayerNames names = stats.names();
        Map<UUID, Double> values = new HashMap<>();
        for (UUID player : table.players()) {
            // Players who have none of the stats are left off, not listed with 0.
            boolean hasAny = query.stats().stream().anyMatch(ref -> table.has(player, ref.left(), ref.right()));
            if (hasAny) {
                values.put(player, Evaluator.evaluate(query.expr(),
                        ref -> table.get(player, ref.left(), ref.right())));
            }
        }
        List<Standing<UUID>> standings = Leaderboard.rank(values, query.lowerIsBetter(),
                Comparator.comparing(names::name, String.CASE_INSENSITIVE_ORDER));
        UUID viewer = sender instanceof Player player ? player.getUniqueId() : null;
        sender.sendMessage(LeaderboardFormatter.format("Leaderboard for " + source, standings, names::name,
                id -> id.equals(viewer), rows));
        return true;
    }

    private static boolean startsWithOperator(String arg) {
        return !arg.isEmpty() && "+-*/^".indexOf(arg.charAt(0)) >= 0;
    }

    @Override
    public List<String> onTabComplete(CommandSender sender, Command command, String label, String[] args) {
        return Completions.query(args[args.length - 1], stats, customQueries);
    }
}
