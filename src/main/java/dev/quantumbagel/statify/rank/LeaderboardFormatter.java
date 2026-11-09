package dev.quantumbagel.statify.rank;

import java.text.DecimalFormat;
import java.text.DecimalFormatSymbols;
import java.util.List;
import java.util.Locale;
import java.util.function.Function;
import java.util.function.Predicate;
import org.bukkit.ChatColor;

/** Turns standings into a chat message. */
public final class LeaderboardFormatter {
    private static final ChatColor TITLE = ChatColor.GOLD;
    private static final ChatColor FIRST = ChatColor.GOLD;
    private static final ChatColor SECOND = ChatColor.DARK_PURPLE;
    private static final ChatColor THIRD = ChatColor.DARK_GREEN;
    private static final ChatColor OTHER_PLACE = ChatColor.DARK_GRAY;
    private static final ChatColor HIGHLIGHT = ChatColor.BLUE;
    private static final ChatColor NAME = ChatColor.GRAY;

    private LeaderboardFormatter() {
    }

    /**
     * Shows the top {@code limit} rows. If a highlighted entry (usually the viewer) is below the
     * cutoff, its row is added after the cutoff so they can still see where they stand.
     */
    public static <K> String format(String title, List<Standing<K>> standings, Function<K, String> name,
                                    Predicate<K> highlight, int limit) {
        StringBuilder out = new StringBuilder()
                .append(TITLE).append(ChatColor.BOLD).append(title).append(ChatColor.RESET);
        if (standings.isEmpty()) {
            return out.append('\n').append(ChatColor.GRAY).append("Nobody has this stat yet.").toString();
        }
        int shown = Math.min(limit, standings.size());
        for (int i = 0; i < shown; i++) {
            appendRow(out, standings.get(i), name, highlight);
        }
        if (shown < standings.size()) {
            for (int i = shown; i < standings.size(); i++) {
                if (highlight.test(standings.get(i).key())) {
                    out.append('\n').append(ChatColor.DARK_GRAY).append("...");
                    appendRow(out, standings.get(i), name, highlight);
                    break;
                }
            }
            out.append('\n').append(ChatColor.GRAY).append(ChatColor.ITALIC)
                    .append("Showing top ").append(shown).append(" of ").append(standings.size()).append('.');
        }
        return out.toString();
    }

    private static <K> void appendRow(StringBuilder out, Standing<K> standing, Function<K, String> name,
                                      Predicate<K> highlight) {
        ChatColor placeColor = switch (standing.place()) {
            case 1 -> FIRST;
            case 2 -> SECOND;
            case 3 -> THIRD;
            default -> OTHER_PLACE;
        };
        out.append('\n')
                .append(placeColor).append(ChatColor.BOLD)
                .append(standing.tied() ? "=" : "").append(ordinal(standing.place()))
                .append(ChatColor.RESET).append(' ')
                .append(highlight.test(standing.key()) ? HIGHLIGHT : NAME).append(name.apply(standing.key()))
                .append(ChatColor.WHITE).append(" (").append(formatValue(standing.value())).append(')');
    }

    /** 1st, 2nd, 3rd, 4th, ..., 11th, 12th, 13th, ..., 21st, 22nd, ... */
    public static String ordinal(int n) {
        int lastTwo = n % 100;
        if (lastTwo >= 11 && lastTwo <= 13) {
            return n + "th";
        }
        return n + switch (n % 10) {
            case 1 -> "st";
            case 2 -> "nd";
            case 3 -> "rd";
            default -> "th";
        };
    }

    /** Whole numbers without decimals, others to two places, with thousands separators. */
    public static String formatValue(double value) {
        DecimalFormat format = new DecimalFormat("#,##0.##", DecimalFormatSymbols.getInstance(Locale.US));
        String text = format.format(value);
        return text.equals("-0") ? "0" : text;
    }
}
