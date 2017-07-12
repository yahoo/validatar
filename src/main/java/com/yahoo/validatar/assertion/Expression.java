package com.yahoo.validatar.assertion;

import com.yahoo.validatar.common.Column;
import com.yahoo.validatar.common.TypedObject;
import lombok.RequiredArgsConstructor;

import java.util.function.Function;

/**
 * This class wraps an tree of Expressions to evaluate later with some context (data). Essentially a State Monad with custom
 * binds per operation.
 */
@RequiredArgsConstructor
public class Expression {
    @FunctionalInterface
    public interface UnaryStateOperation {
        Column apply(Column input);
    }

    @FunctionalInterface
    public interface BinaryStateOperation {
        Column apply(Column a, Column b);
    }

    private final Function<Dataset, Column> expression;

    /**
     * Evaluates the expression with the given data dataset and returns the result.
     *
     * @param dataset The {@link Dataset} that is the data for this expression.
     * @return The resulting {@link Column}.
     */
    public Column evaluate(Dataset dataset) {
        return expression.apply(dataset);
    }

    /**
     * Evaluates the expression with the no data context and returns the result.
     *
     * @return The resulting {@link Column}.
     */
    public Column evaluate() {
        return expression.apply(null);
    }

    /**
     * Composes a {@link UnaryStateOperation} onto a given {@link Expression}.
     *
     * @param operation The operation to chain onto.
     * @param expression The expression to apply the operation on.
     * @return The new expression.
     */
    public static Expression compose(UnaryStateOperation operation, Expression expression) {
        return new Expression(data -> operation.apply(expression.evaluate(data)));
    }

    /**
     * Composes a {@link BinaryStateOperation} onto two given {@link Expression}.
     *
     * @param operation The operation to chain onto.
     * @param a The first expression to apply the operation on.
     * @param b The second expression to apply the operation on.
     * @return The new expression.
     */
    public static Expression compose(BinaryStateOperation operation, Expression a, Expression b) {
        return new Expression(data -> operation.apply(a.evaluate(data), b.evaluate(data)));
    }

    /**
     * Wraps a {@link Column} as an Expression. Evaluating that returns the Column.
     *
     * @param input The Column to wrap.
     * @return An Expression.
     */
    public static Expression wrap(Column input) {
        return new Expression(d -> input);
    }

    /**
     * Wraps a {@link TypedObject} as an Expression. Evaluating that returns it as a scalar Column.
     *
     * @param input The TypedObject to wrap.
     * @return An Expression.
     */
    public static Expression wrap(TypedObject input) {
        return wrap(new Column(input));
    }
}
