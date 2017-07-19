/*
 * Copyright 2015 Yahoo Inc.
 * Licensed under the terms of the Apache 2 license. Please see LICENSE file in the project root for terms.
 */
package com.yahoo.validatar.common;

import com.yahoo.validatar.common.Operations.BinaryOperation;
import com.yahoo.validatar.common.Operations.UnaryOperation;
import lombok.extern.slf4j.Slf4j;

import java.math.BigDecimal;
import java.sql.Timestamp;
import java.util.HashMap;
import java.util.Map;
import java.util.Objects;
import java.util.stream.IntStream;

/**
 * This is a class that wraps the supported types that the assertor will work with
 * when operating on data returned by an data source.
 */
@Slf4j
public class TypeSystem {
    /**
     * These are the official types we support. They correspond to Java types.
     * They all must correspond to an actual object that implements Comparable.
     */
    public enum Type {
        LONG, DOUBLE, DECIMAL, BOOLEAN, STRING, TIMESTAMP
    }

    private static final Map<Type, Operations> OPERATIONS = new HashMap<>();
    static {
        OPERATIONS.put(Type.LONG, new Operators.LongOperator());
        OPERATIONS.put(Type.DOUBLE, new Operators.DoubleOperator());
        OPERATIONS.put(Type.DECIMAL, new Operators.DecimalOperator());
        OPERATIONS.put(Type.BOOLEAN, new Operators.BooleanOperator());
        OPERATIONS.put(Type.STRING, new Operators.StringOperator());
        OPERATIONS.put(Type.TIMESTAMP, new Operators.TimestampOperator());
    }

    /**
     * Tries to convert two TypedObjects to the same type. Tries first to second or second to
     * first, in that order.  Throws an ClassCastException if neither could be done or if the
     * final Type wasn't what was passed in. Throws an NullPointerException if either argument
     * is null. If successful, first and second will be the of the same type.
     *
     * @param first  The first {@link TypedObject}.
     * @param second The first {@link TypedObject}.
     */
    public static void unifyType(TypedObject first, TypedObject second) {
        Objects.requireNonNull(first);
        Objects.requireNonNull(second);
        // Relying on the type system to return null if invalid conversions were tried and non null if not.
        TypedObject unified = OPERATIONS.get(first.type).cast(second);
        if (unified == null) {
            unified = OPERATIONS.get(second.type).cast(first);
        }

        if (unified == null) {
            throw new ClassCastException("Type unification could not be performed for " + first + " and " + second);
        }
    }

    /**
     * Perform a binary operation on two TypedObjects.
     *
     * @param operation The {@link BinaryOperation} operator to perform.
     * @param first     The LHS {@link TypedObject} of the arithmetic.
     * @param second    The RHS {@link TypedObject} of the arithmetic.
     * @return The resulting {@link TypedObject}.
     */
    public static TypedObject perform(BinaryOperation operation, TypedObject first, TypedObject second) {
        unifyType(first, second);

        // Both are now the same type, do the operation
        TypedObject result = OPERATIONS.get(first.type).dispatch(operation).apply(first, second);

        if (result == null) {
            throw new ClassCastException("Unable to perform: " + operation + " on " + first + " and " + second);
        }
        return result;
    }

    /**
     * Perform an unary operation on a TypedObject.
     *
     * @param operation The {@link UnaryOperation} operator to perform.
     * @param object    The target {@link TypedObject} of the operation.
     * @return The resulting {@link TypedObject}.
     */
    public static TypedObject perform(UnaryOperation operation, TypedObject object) {
        TypedObject result = OPERATIONS.get(object.type).dispatch(operation).apply(object);

        if (result == null) {
            throw new ClassCastException("Unable to perform: " + operation + " on " + object);
        }
        return result;
    }

    /**
     * Cast an object to the given type.
     *
     * @param toType The {@link Type} to cast the object to.
     * @param object The object to cast.
     * @return The casted object.
     */
    public static TypedObject cast(Type toType, TypedObject object) {
        Objects.requireNonNull(toType);
        return OPERATIONS.get(toType).cast(object);
    }

    /**
     * Compare two TypedObjects.
     *
     * @param first  The first {@link TypedObject} to compare.
     * @param second The second {@link TypedObject} to compare.
     * @return -1 if first is less than second, 1 if first is greater than second and 0 if first equals second.
     */
    @SuppressWarnings("unchecked")
    public static int compare(TypedObject first, TypedObject second) {
        unifyType(first, second);
        // Both are now the same type, just compare
        return first.data.compareTo(second.data);
    }

    /**
     * Performs an approximate comparison of the first and second {@link TypedObject} using the third as a percentage.
     *
     * @param first The first object.
     * @param second The second object.
     * @param percent The percentage by which the first and second be within.
     * @return The result of the operation - a boolean TypedObject.
     */
    public static TypedObject approx(TypedObject first, TypedObject second, TypedObject percent) {
        if ((Boolean) perform(BinaryOperation.GREATER, percent, asTypedObject(1L)).data ||
            (Boolean) perform(BinaryOperation.LESS, percent, asTypedObject(0L)).data) {
            throw new RuntimeException("Expected percentage for approx to be between 0 and 1. Got " + percent.data);
        }
        // max = second * (1 + percent)
        // if first > max, then not approx equal
        TypedObject max = perform(BinaryOperation.MULTIPLY, second, perform(BinaryOperation.ADD, asTypedObject(1L), percent));
        if ((Boolean) perform(BinaryOperation.GREATER, first, max).data) {
            return asTypedObject(false);
        }

        // min = second * (1 - percent)
        // if first < min, then not approx equal
        TypedObject min = perform(BinaryOperation.MULTIPLY, second, perform(BinaryOperation.SUBTRACT, asTypedObject(1L), percent));
        if ((Boolean) perform(BinaryOperation.LESS, first, min).data) {
            return asTypedObject(false);
        }
        return asTypedObject(true);
    }

    /*
     ********************************************************************************
     *                               Vector Operations                              *
     ********************************************************************************
     */

    /**
     * Tries to unify the sizes of the two {@link Column}. If one is a {@link Column#isScalar()} and the other is
     * not, the scalar is extended to be a vector of the size of the other. It is an error if both are vectors with
     * different sizes.
     *
     * @param first The first non-null column to check.
     * @param second The second non-null column to check.
     * @throws RuntimeException if the sizes cannot be unified.
     */
    public static void unifySize(Column first, Column second) {
        Objects.requireNonNull(first);
        Objects.requireNonNull(second);

        if (first.isEmpty() || second.isEmpty()) {
            log.error("Tried making two columns have the same size but one or both had no data: {}, {}", first, second);
            throw new RuntimeException("Either " + first + " or " + second + " had no data");
        }

        boolean isFirstScalar = first.isScalar();
        boolean isSecondScalar = second.isScalar();
        // If both scalar or both vectors
        if (!(isFirstScalar ^ isSecondScalar)) {
            if (first.size() != second.size()) {
                log.error("Cannot operate on two columns with different sizes: {} and {}", first, second);
                throw new RuntimeException("Cannot operate on two columns with different sizes");
            }
            // Both scalars or vectors and have the same size, we're good
            return;
        }
        // Exactly one is scalar
        int firstSize = first.size();
        int secondSize = second.size();
        Column target = isFirstScalar ? first : second;
        int numberOfValues = (isFirstScalar ? secondSize : firstSize) - 1;
        // Repeat the first element in target, 1 - the size of the other
        target.add(asColumn(target.first(), numberOfValues));
    }

    /**
     * Perform a binary operation on two Columns.
     *
     * @param operation The {@link BinaryOperation} operator to perform.
     * @param first     The LHS {@link Column} of the arithmetic.
     * @param second    The RHS {@link Column} of the arithmetic.
     * @return The resulting {@link Column}.
     */
    public static Column perform(BinaryOperation operation, Column first, Column second) {
        log.debug("Performing {} on {} and {}", operation, first, second);
        unifySize(first, second);

        Column result = new Column();
        for (int i = 0; i < first.size(); ++i) {
            result.add(perform(operation, first.get(i), second.get(i)));
        }
        return result;
    }

    /**
     * Perform an unary operation on a Column.
     *
     * @param operation The {@link UnaryOperation} operator to perform.
     * @param object    The target {@link Column} of the operation.
     * @return The resulting {@link Column}.
     */
    public static Column perform(UnaryOperation operation, Column object) {
        log.debug("Performing {} on {}", operation, object);
        return object.stream().map(t -> perform(operation, t)).collect(Column::new, Column::add, Column::add);
    }

    /**
     * Performs an approximate comparison of the first and second {@link Column} using the third as a percentage.
     *
     * @param first The first object.
     * @param second The second object.
     * @param percent The percentage by which the first and second be within.
     * @return The {@link Column} containing boolean Columns representing the result of each comparison.
     */
    public static Column approx(Column first, Column second, Column percent) {
        unifySize(first, second);
        if (!percent.isScalar() && percent.size() != first.size())  {
            log.error("Your approx percentage {} is a column but its size does not match the other columns: {} and {} ",
                      percent, first, second);
            throw new RuntimeException("The percentage column in approx has a different size from the other columns");
        }
        Column result = new Column();
        for (int i = 0; i < first.size(); ++i) {
            result.add(approx(first.get(i), second.get(i), percent.get(percent.isScalar() ? 0 : i)));
        }
        return result;
    }

    /*
     ********************************************************************************
     *                               Wrapper methods                                *
     ********************************************************************************
     */

    /**
     * Takes a Boolean and wraps it a proper TypedObject.
     *
     * @param value a {@link java.lang.Boolean} object.
     * @return The wrapped {@link TypedObject} result.
     */
    public static TypedObject asTypedObject(Boolean value) {
        return new TypedObject(value, Type.BOOLEAN);
    }

    /**
     * Takes a Long and wraps it a proper TypedObject.
     *
     * @param value a {@link java.lang.Long} object.
     * @return The wrapped {@link TypedObject} result.
     */
    public static TypedObject asTypedObject(Long value) {
        return new TypedObject(value, Type.LONG);
    }

    /**
     * Takes a Double and wraps it a proper TypedObject.
     *
     * @param value a {@link java.lang.Double} object.
     * @return The wrapped {@link TypedObject} result.
     */
    public static TypedObject asTypedObject(Double value) {
        return new TypedObject(value, Type.DOUBLE);
    }

    /**
     * Takes a Double and wraps it a proper TypedObject.
     *
     * @param value a {@link java.math.BigDecimal} object.
     * @return The wrapped {@link TypedObject} result.
     */
    public static TypedObject asTypedObject(BigDecimal value) {
        return new TypedObject(value, Type.DECIMAL);
    }

    /**
     * Takes a Timestamp and wraps it a proper TypedObject.
     *
     * @param value a {@link java.sql.Timestamp} object.
     * @return The wrapped {@link TypedObject} result.
     */
    public static TypedObject asTypedObject(Timestamp value) {
        return new TypedObject(value, Type.TIMESTAMP);
    }

    /**
     * Takes a String and wraps it a proper TypedObject.
     *
     * @param value a {@link java.lang.String} object.
     * @return The wrapped {@link TypedObject} result.
     */
    public static TypedObject asTypedObject(String value) {
        return new TypedObject(value, Type.STRING);
    }

    /**
     * Repeats the given object {@code repeat} times and makes it a {@link Column}.
     *
     * @param object The object to replicate.
     * @param repeat The number of times to replicate it.
     * @return A Column containing the replicated items
     */
    public static Column asColumn(TypedObject object, int repeat) {
        return IntStream.range(0, repeat).mapToObj(t -> new TypedObject(object.data, object.type))
                        .collect(Column::new, Column::add, Column::add);
    }
}
