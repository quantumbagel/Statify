package dev.quantumbagel.statify.query;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.List;
import java.util.Map;
import java.util.Optional;
import org.junit.jupiter.api.Test;

class QueryTest {

    /** owner -> name -> source */
    private static Query.Library library(Map<String, Map<String, String>> saved) {
        return (owner, name) -> Optional.ofNullable(saved.getOrDefault(owner, Map.of()).get(name));
    }

    private static final Query.Library NONE = (owner, name) -> Optional.empty();

    private static double eval(Query query, Map<String, Double> stats) {
        return Evaluator.evaluate(query.expr(), ref -> stats.getOrDefault(ref.toString(), 0.0));
    }

    @Test
    void customQueriesExpandInline() throws QueryException {
        Query.Library lib = library(Map.of("bagel", Map.of(
                "kills", "killed:zombie + killed:skeleton",
                "kdr", "bagel:kills / custom:deaths")));
        Query query = Query.compile("bagel:kdr * 100", lib);
        assertEquals(List.of(new Expr.Ref("killed", "zombie"), new Expr.Ref("killed", "skeleton"),
                new Expr.Ref("custom", "deaths")), query.stats());
        assertEquals(500.0, eval(query, Map.of("killed:zombie", 8.0, "killed:skeleton", 2.0, "custom:deaths", 2.0)));
    }

    @Test
    void loopsAreReportedWithTheirPath() {
        Query.Library lib = library(Map.of("a", Map.of("x", "a:y + 1", "y", "b:z"), "b", Map.of("z", "a:x")));
        QueryException e = assertThrows(QueryException.class, () -> Query.compile("a:x", lib));
        assertEquals("Custom queries refer to each other in a loop: a:x -> a:y -> b:z -> a:x", e.getMessage());
    }

    @Test
    void usingTheSameQueryTwiceIsNotALoop() throws QueryException {
        Query.Library lib = library(Map.of("a", Map.of("k", "killed:zombie")));
        assertEquals(20.0, eval(Query.compile("a:k + a:k", lib), Map.of("killed:zombie", 10.0)));
    }

    @Test
    void compileAsCatchesSelfReferenceBeforeSaving() {
        QueryException e = assertThrows(QueryException.class,
                () -> Query.compileAs("a", "x", "a:x + 1", NONE));
        assertTrue(e.getMessage().contains("a:x -> a:x"));
    }

    @Test
    void compileAsCatchesLoopsThroughExistingQueries() {
        Query.Library lib = library(Map.of("a", Map.of("y", "a:x * 2")));
        assertThrows(QueryException.class, () -> Query.compileAs("a", "x", "a:y", lib));
    }

    @Test
    void compileAsSeesTheNewDefinitionNotTheOldOne() throws QueryException {
        Query.Library lib = library(Map.of("a", Map.of("x", "a:x")));
        Query query = Query.compileAs("a", "x", "mined:stone", lib);
        assertEquals(List.of(new Expr.Ref("mined", "stone")), query.stats());
    }

    @Test
    void unknownCategoriesAreRejected() {
        QueryException e = assertThrows(QueryException.class, () -> Query.compile("kiled:zombie", NONE));
        assertTrue(e.getMessage().contains("kiled:zombie"));
    }

    @Test
    void brokenSavedQueriesNameTheCulprit() {
        Query.Library lib = library(Map.of("a", Map.of("x", "killed:zombie +")));
        QueryException e = assertThrows(QueryException.class, () -> Query.compile("a:x", lib));
        assertTrue(e.getMessage().startsWith("Saved query a:x is broken"));
    }

    @Test
    void queriesNeedAtLeastOneStat() {
        assertThrows(QueryException.class, () -> Query.compile("1 + 2", NONE));
    }

    @Test
    void deathStatsRankLowestFirst() throws QueryException {
        assertTrue(Query.compile("killed_by:zombie", NONE).lowerIsBetter());
        assertFalse(Query.compile("killed:zombie", NONE).lowerIsBetter());
        assertFalse(Query.compile("killed:zombie / killed_by:zombie", NONE).lowerIsBetter());
    }
}
