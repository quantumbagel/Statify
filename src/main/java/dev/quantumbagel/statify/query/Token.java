package dev.quantumbagel.statify.query;

record Token(Type type, String text, int position) {
    enum Type { NUMBER, WORD, COLON, PLUS, MINUS, STAR, SLASH, CARET, LPAREN, RPAREN, END }

    String describe() {
        return type == Type.END ? "end of query" : "'" + text + "'";
    }
}
