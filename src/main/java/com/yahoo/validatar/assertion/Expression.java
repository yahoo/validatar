/*
 * Copyright 2017 Yahoo Inc.
 * Licensed under the terms of the Apache 2 license. Please see LICENSE file in the project root for terms.
 */
package com.yahoo.validatar.assertion;

import com.yahoo.validatar.common.Column;
import com.yahoo.validatar.common.Result;
import com.yahoo.validatar.common.TypedObject;
import lombok.RequiredArgsConstructor;

import java.util.function.Function;

/**
 * This class wraps an tree of Expressions to evaluate later with some context (data). Essentially a State Monad.
 */
@RequiredArgsConstructor
public class Expression {
    private final Function<Result, Column> expression;

    /**
     * Evaluates the expression with the given data result and returns the result.
     *
     * @param result The {@link Result} that is the data for this expression.
     * @return The resulting {@link Column}.
     */
    public Column evaluate(Result result) {
        return expression.apply(result);
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
     * Composes a {@link UnaryColumnOperation} onto a given {@link Expression}. Monadic lift.
     *
     * @param operation The operation to chain onto.
     * @param expression The expression to apply the operation on.
     * @return The new expression.
     */
    public static Expression compose(UnaryColumnOperation operation, Expression expression) {
        return new Expression(data -> operation.apply(expression.evaluate(data)));
    }

    /**
     * Composes a {@link BinaryColumnOperation} onto two given {@link Expression}. Monadic lift.
     *
     * @param operation The operation to chain onto.
     * @param a The first expression to apply the operation on.
     * @param b The second expression to apply the operation on.
     * @return The new expression.
     */
    public static Expression compose(BinaryColumnOperation operation, Expression a, Expression b) {
        return new Expression(data -> operation.apply(a.evaluate(data), b.evaluate(data)));
    }

    /**
     * Wraps a {@link Column} as an Expression. Evaluating that returns the Column. Monadic unit.
     *
     * @param input The Column to wrap.
     * @return An Expression.
     */
    public static Expression wrap(Column input) {
        return new Expression(d -> input);
    }

    /**
     * Wraps a {@link TypedObject} as an Expression. Evaluating that returns it as a scalar Column. Monadic unit
     * for scalar Column.
     *
     * @param input The TypedObject to wrap.
     * @return An Expression.
     */
    public static Expression wrap(TypedObject input) {
        return wrap(new Column(input));
    }

    /*
     ********************************************************************************
     *              Helper functional interfaces to simplify typing                 *
     ********************************************************************************
     */

    @FunctionalInterface
    public interface UnaryColumnOperation {
        /**
         * Applies the operation on the given {@link Column}.
         *
         * @param input The column to apply the operation on.
         * @return The resulting column.
         */
        Column apply(Column input);
    }

    @FunctionalInterface
    public interface BinaryColumnOperation {
        /**
         * Applies the operation on the given {@link Column}.
         *
         * @param a The first column to apply the operation on.
         * @param b The second column to apply the operation on.
         * @return The resulting column.
         */
        Column apply(Column a, Column b);
    }

}
