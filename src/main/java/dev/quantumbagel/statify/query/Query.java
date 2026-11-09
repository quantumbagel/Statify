package dev.quantumbagel.statify.query;

import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Deque;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Optional;
import java.util.Set;

/**
 * A parsed query with every custom-query reference expanded, so that only vanilla statistic
 * references remain. Custom queries can reference other custom queries; a reference loop is
 * rejected with the loop spelled out instead of recursing until the stack overflows.
 */
public final class Query {
    /** The statistic categories Minecraft records, without the {@code minecraft:} prefix. */
    public static final Set<String> STAT_CATEGORIES = Set.of(
            "custom", "mined", "crafted", "used", "broken", "picked_up", "dropped", "killed", "killed_by");

    /** Looks up saved custom queries by owner username and query name, both lowercase. */
    @FunctionalInterface
    public interface Library {
        Optional<String> lookup(String owner, String name);
    }

    private final String source;
    private final Expr expr;
    private final List<Expr.Ref> stats;

    private Query(String source, Expr expr, List<Expr.Ref> stats) {
        this.source = source;
        this.expr = expr;
        this.stats = stats;
    }

    public static Query compile(String source, Library library) throws QueryException {
        return compile(source, library, new ArrayDeque<>());
    }

    /**
     * Compiles {@code source} as if it were saved as {@code owner:name}, so a query that refers
     * back to itself is caught before it is saved.
     */
    public static Query compileAs(String owner, String name, String source, Library library) throws QueryException {
        Deque<String> path = new ArrayDeque<>();
        path.addLast(owner + ":" + name);
        Library withNew = (o, n) -> o.equals(owner) && n.equals(name) ? Optional.of(source) : library.lookup(o, n);
        return compile(source, withNew, path);
    }

    private static Query compile(String source, Library library, Deque<String> path) throws QueryException {
        Expr expanded = expand(Parser.parse(source), library, path);
        Set<Expr.Ref> refs = new LinkedHashSet<>();
        collectRefs(expanded, refs);
        if (refs.isEmpty()) {
            throw new QueryException("The query has no stats in it, like killed:zombie");
        }
        return new Query(source, expanded, List.copyOf(refs));
    }

    private static Expr expand(Expr expr, Library library, Deque<String> path) throws QueryException {
        return switch (expr) {
            case Expr.Num num -> num;
            case Expr.Ref ref -> expandRef(ref, library, path);
            case Expr.Negate negate -> new Expr.Negate(expand(negate.operand(), library, path));
            case Expr.Binary binary -> new Expr.Binary(binary.op(),
                    expand(binary.left(), library, path), expand(binary.right(), library, path));
            case Expr.Call call -> new Expr.Call(call.function(), expand(call.argument(), library, path));
        };
    }

    private static Expr expandRef(Expr.Ref ref, Library library, Deque<String> path) throws QueryException {
        Optional<String> saved = library.lookup(ref.left(), ref.right());
        if (saved.isEmpty()) {
            if (!STAT_CATEGORIES.contains(ref.left())) {
                throw new QueryException("'" + ref + "' is not a stat category or a saved custom query");
            }
            return ref;
        }
        String key = ref.toString();
        if (path.contains(key)) {
            List<String> loop = new ArrayList<>(path);
            loop = loop.subList(loop.indexOf(key), loop.size());
            throw new QueryException("Custom queries refer to each other in a loop: "
                    + String.join(" -> ", loop) + " -> " + key);
        }
        Expr parsed;
        try {
            parsed = Parser.parse(saved.get());
        } catch (QueryException e) {
            throw new QueryException("Saved query " + key + " is broken: " + e.getMessage());
        }
        path.addLast(key);
        try {
            return expand(parsed, library, path);
        } finally {
            path.removeLast();
        }
    }

    private static void collectRefs(Expr expr, Set<Expr.Ref> out) {
        switch (expr) {
            case Expr.Num ignored -> {
            }
            case Expr.Ref ref -> out.add(ref);
            case Expr.Negate negate -> collectRefs(negate.operand(), out);
            case Expr.Binary binary -> {
                collectRefs(binary.left(), out);
                collectRefs(binary.right(), out);
            }
            case Expr.Call call -> collectRefs(call.argument(), out);
        }
    }

    public String source() {
        return source;
    }

    public Expr expr() {
        return expr;
    }

    /** The distinct vanilla statistics the query reads, in the order they appear. */
    public List<Expr.Ref> stats() {
        return stats;
    }

    /** Deaths-style queries rank lowest first. */
    public boolean lowerIsBetter() {
        return stats.size() == 1 && stats.get(0).left().equals("killed_by");
    }
}
