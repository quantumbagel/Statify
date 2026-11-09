package dev.quantumbagel.statify.query;

import java.util.List;

/**
 * Recursive-descent parser for the query language.
 *
 * <pre>
 * expression := term (('+' | '-') term)*
 * term       := unary (('*' | '/') unary)*
 * unary      := ('+' | '-') unary | power
 * power      := primary ('^' unary)?          right-associative, binds tighter than unary minus
 * primary    := NUMBER | WORD ':' WORD | WORD '(' expression ')' | '(' expression ')'
 * </pre>
 */
public final class Parser {
    private final List<Token> tokens;
    private int index;

    private Parser(List<Token> tokens) {
        this.tokens = tokens;
    }

    public static Expr parse(String source) throws QueryException {
        if (source == null || source.isBlank()) {
            throw new QueryException("The query is empty");
        }
        Parser parser = new Parser(Lexer.tokenize(source));
        Expr expr = parser.expression();
        Token trailing = parser.peek();
        if (trailing.type() != Token.Type.END) {
            throw new QueryException("Unexpected " + trailing.describe(), trailing.position());
        }
        return expr;
    }

    private Expr expression() throws QueryException {
        Expr left = term();
        while (true) {
            if (accept(Token.Type.PLUS)) {
                left = new Expr.Binary(Expr.Op.ADD, left, term());
            } else if (accept(Token.Type.MINUS)) {
                left = new Expr.Binary(Expr.Op.SUBTRACT, left, term());
            } else {
                return left;
            }
        }
    }

    private Expr term() throws QueryException {
        Expr left = unary();
        while (true) {
            if (accept(Token.Type.STAR)) {
                left = new Expr.Binary(Expr.Op.MULTIPLY, left, unary());
            } else if (accept(Token.Type.SLASH)) {
                left = new Expr.Binary(Expr.Op.DIVIDE, left, unary());
            } else {
                return left;
            }
        }
    }

    private Expr unary() throws QueryException {
        if (accept(Token.Type.PLUS)) {
            return unary();
        }
        if (accept(Token.Type.MINUS)) {
            return new Expr.Negate(unary());
        }
        return power();
    }

    private Expr power() throws QueryException {
        Expr base = primary();
        if (accept(Token.Type.CARET)) {
            return new Expr.Binary(Expr.Op.POWER, base, unary());
        }
        return base;
    }

    private Expr primary() throws QueryException {
        Token token = next();
        switch (token.type()) {
            case NUMBER:
                return new Expr.Num(Double.parseDouble(token.text()));
            case LPAREN: {
                Expr inner = expression();
                expect(Token.Type.RPAREN, "Missing ')'");
                return inner;
            }
            case WORD:
                if (accept(Token.Type.COLON)) {
                    Token right = next();
                    if (right.type() != Token.Type.WORD && right.type() != Token.Type.NUMBER) {
                        throw new QueryException("Expected a name after '" + token.text() + ":'", right.position());
                    }
                    return new Expr.Ref(token.text().toLowerCase(), right.text().toLowerCase());
                }
                if (peek().type() == Token.Type.LPAREN) {
                    Expr.Function function = Expr.Function.byName(token.text());
                    if (function == null) {
                        throw new QueryException("Unknown function '" + token.text() + "'", token.position());
                    }
                    next();
                    Expr argument = expression();
                    expect(Token.Type.RPAREN, "Missing ')' after the argument to " + token.text());
                    return new Expr.Call(function, argument);
                }
                throw new QueryException("'" + token.text() + "' needs a category, like killed:" + token.text(),
                        token.position());
            default:
                throw new QueryException("Unexpected " + token.describe(), token.position());
        }
    }

    private Token peek() {
        return tokens.get(index);
    }

    private Token next() {
        Token token = tokens.get(index);
        if (token.type() != Token.Type.END) {
            index++;
        }
        return token;
    }

    private boolean accept(Token.Type type) {
        if (peek().type() == type) {
            index++;
            return true;
        }
        return false;
    }

    private void expect(Token.Type type, String message) throws QueryException {
        if (!accept(type)) {
            throw new QueryException(message, peek().position());
        }
    }
}
