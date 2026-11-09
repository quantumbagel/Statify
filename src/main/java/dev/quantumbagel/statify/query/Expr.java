package dev.quantumbagel.statify.query;

import java.util.function.DoubleUnaryOperator;

/** Syntax tree for a query. */
public sealed interface Expr {

    record Num(double value) implements Expr {
    }

    /**
     * {@code left:right}. Either a vanilla statistic ({@code killed:zombie}, {@code mined:total})
     * or a reference to a saved custom query ({@code quantumbagel:kdr}); {@link Expander} decides which.
     */
    record Ref(String left, String right) implements Expr {
        @Override
        public String toString() {
            return left + ":" + right;
        }
    }

    record Negate(Expr operand) implements Expr {
    }

    record Binary(Op op, Expr left, Expr right) implements Expr {
    }

    record Call(Function function, Expr argument) implements Expr {
    }

    enum Op {
        ADD, SUBTRACT, MULTIPLY, DIVIDE, POWER;

        double apply(double a, double b) {
            return switch (this) {
                case ADD -> a + b;
                case SUBTRACT -> a - b;
                case MULTIPLY -> a * b;
                case DIVIDE -> a / b;
                case POWER -> Math.pow(a, b);
            };
        }
    }

    /** Functions take one argument. Trig functions use degrees, as in Statify 1.0. */
    enum Function {
        SQRT(Math::sqrt),
        ABS(Math::abs),
        SIN(x -> Math.sin(Math.toRadians(x))),
        COS(x -> Math.cos(Math.toRadians(x))),
        TAN(x -> Math.tan(Math.toRadians(x))),
        LN(Math::log),
        LOG(Math::log10),
        FLOOR(Math::floor),
        CEIL(Math::ceil),
        ROUND(x -> (double) Math.round(x));

        private final DoubleUnaryOperator body;

        Function(DoubleUnaryOperator body) {
            this.body = body;
        }

        double apply(double x) {
            return body.applyAsDouble(x);
        }

        static Function byName(String name) {
            for (Function f : values()) {
                if (f.name().equalsIgnoreCase(name)) {
                    return f;
                }
            }
            return null;
        }
    }
}
