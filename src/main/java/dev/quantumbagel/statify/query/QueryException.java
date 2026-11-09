package dev.quantumbagel.statify.query;

/** A query that cannot be parsed or expanded. The message is safe to show to players. */
public class QueryException extends Exception {
    private final int position;

    public QueryException(String message) {
        this(message, -1);
    }

    public QueryException(String message, int position) {
        super(message);
        this.position = position;
    }

    /** Character offset in the query where the problem was found, or -1 if it has none. */
    public int position() {
        return position;
    }
}
