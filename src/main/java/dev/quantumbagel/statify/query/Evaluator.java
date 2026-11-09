package dev.quantumbagel.statify.query;

import java.util.function.ToDoubleFunction;

/** Evaluates an expression, looking up each {@link Expr.Ref} with the given function. */
public final class Evaluator {
    private Evaluator() {
    }

    public static double evaluate(Expr expr, ToDoubleFunction<Expr.Ref> lookup) {
        return switch (expr) {
            case Expr.Num num -> num.value();
            case Expr.Ref ref -> lookup.applyAsDouble(ref);
            case Expr.Negate negate -> -evaluate(negate.operand(), lookup);
            case Expr.Binary binary ->
                    binary.op().apply(evaluate(binary.left(), lookup), evaluate(binary.right(), lookup));
            case Expr.Call call -> call.function().apply(evaluate(call.argument(), lookup));
        };
    }
}
