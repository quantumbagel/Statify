package dev.quantumbagel.statify.query;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.Map;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;
import org.junit.jupiter.params.provider.ValueSource;

class ParserTest {

    private static double eval(String source) throws QueryException {
        return eval(source, Map.of());
    }

    private static double eval(String source, Map<String, Double> stats) throws QueryException {
        return Evaluator.evaluate(Parser.parse(source), ref -> stats.getOrDefault(ref.toString(), 0.0));
    }

    @ParameterizedTest
    @CsvSource(delimiter = '|', value = {
            "1 + 2 * 3            | 7",
            "(1 + 2) * 3          | 9",
            "10 - 4 - 3           | 3",
            "100 / 10 / 5         | 2",
            "2 ^ 3 ^ 2            | 512",
            "-2 ^ 2               | -4",
            "2 ^ -1               | 0.5",
            "--3                  | 3",
            "+4                   | 4",
            ".5 + 1.5             | 2",
            "sqrt(16) + abs(-3)   | 7",
            "SQRT(9)              | 3",
            "round(2.5) + floor(2.9) + ceil(0.1) | 6",
            "log(1000)            | 3",
    })
    void arithmetic(String source, double expected) throws QueryException {
        assertEquals(expected, eval(source), 1e-9);
    }

    @Test
    void trigUsesDegrees() throws QueryException {
        assertEquals(1.0, eval("sin(90)"), 1e-9);
        assertEquals(1.0, eval("tan(45)"), 1e-9);
    }

    @Test
    void statReferences() throws QueryException {
        Map<String, Double> stats = Map.of("killed:zombie", 30.0, "killed:skeleton", 12.0, "custom:deaths", 6.0);
        assertEquals(7.0, eval("(killed:zombie + killed:skeleton) / custom:deaths", stats), 1e-9);
    }

    @Test
    void referencesAreLowercasedAndAllowDigits() throws QueryException {
        Expr expr = Parser.parse("2ndPlace:KDR + used:music_disc_13");
        Expr.Binary sum = (Expr.Binary) expr;
        assertEquals(new Expr.Ref("2ndplace", "kdr"), sum.left());
        assertEquals(new Expr.Ref("used", "music_disc_13"), sum.right());
    }

    @Test
    void similarStatNamesDoNotCollide() throws QueryException {
        // Statify 1.0 substituted values into the query text, so "mined:stone" also matched inside
        // "mined:stone_bricks". Parsing to a tree keeps them separate.
        Map<String, Double> stats = Map.of("mined:stone", 5.0, "mined:stone_bricks", 100.0);
        assertEquals(105.0, eval("mined:stone + mined:stone_bricks", stats), 1e-9);
    }

    @ParameterizedTest
    @ValueSource(strings = {"", "   ", "1 +", "(1 + 2", "1 + 2)", "killed", "killed:", "nope(3)", "1 $ 2",
            "3 4", "1..2", "sqrt 4"})
    void rejectsMalformedQueries(String source) {
        assertThrows(QueryException.class, () -> Parser.parse(source));
    }

    @Test
    void errorsPointAtTheProblem() {
        QueryException e = assertThrows(QueryException.class, () -> Parser.parse("killed:zombie + * 2"));
        assertEquals(16, e.position());
        assertTrue(e.getMessage().contains("'*'"));
    }
}
