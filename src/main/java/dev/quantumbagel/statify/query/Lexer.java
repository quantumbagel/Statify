package dev.quantumbagel.statify.query;

import java.util.ArrayList;
import java.util.List;
import java.util.regex.Pattern;

/** Splits a query like {@code (killed:zombie + 2) / custom:deaths} into tokens. */
final class Lexer {
    private static final Pattern NUMBER = Pattern.compile("\\d+(\\.\\d*)?|\\.\\d+");

    private Lexer() {
    }

    static List<Token> tokenize(String source) throws QueryException {
        List<Token> tokens = new ArrayList<>();
        int i = 0;
        while (i < source.length()) {
            char c = source.charAt(i);
            if (Character.isWhitespace(c)) {
                i++;
                continue;
            }
            Token.Type single = switch (c) {
                case ':' -> Token.Type.COLON;
                case '+' -> Token.Type.PLUS;
                case '-' -> Token.Type.MINUS;
                case '*' -> Token.Type.STAR;
                case '/' -> Token.Type.SLASH;
                case '^' -> Token.Type.CARET;
                case '(' -> Token.Type.LPAREN;
                case ')' -> Token.Type.RPAREN;
                default -> null;
            };
            if (single != null) {
                tokens.add(new Token(single, String.valueOf(c), i));
                i++;
                continue;
            }
            if (!isWordChar(c)) {
                throw new QueryException("Unexpected character '" + c + "'", i);
            }
            // Numbers and names share one scan because usernames may start with a digit ("2ndPlace").
            int start = i;
            while (i < source.length() && isWordChar(source.charAt(i))) {
                i++;
            }
            String text = source.substring(start, i);
            boolean number = NUMBER.matcher(text).matches();
            if (!number && text.contains(".")) {
                throw new QueryException("Malformed number or name '" + text + "'", start);
            }
            tokens.add(new Token(number ? Token.Type.NUMBER : Token.Type.WORD, text, start));
        }
        tokens.add(new Token(Token.Type.END, "", source.length()));
        return tokens;
    }

    private static boolean isWordChar(char c) {
        return (c >= 'a' && c <= 'z') || (c >= 'A' && c <= 'Z') || (c >= '0' && c <= '9') || c == '_' || c == '.';
    }
}
