package com.yahoo.validatar.common;

import java.util.Objects;
import java.util.function.BinaryOperator;
import java.util.function.UnaryOperator;

import static com.yahoo.validatar.common.TypeSystem.asTypedObject;
import static com.yahoo.validatar.common.TypeSystem.compare;

/**
 * This defines the various operations we will support and provides default implementations for all of them. A particular
 * type specific implementation can implement this to provide its own specific logic.
 */
public interface Operations {
    /**
     * These are the unary operations we will support.
     */
    enum UnaryOperation {
        CAST, NOT
    }

    /**
     * These are the binary operations we will support.
     */
    enum BinaryOperation {
        ADD, SUBTRACT, MULTIPLY, DIVIDE, MODULUS, EQUAL, NOT_EQUAL, GREATER, LESS, GREATER_EQUAL, LESS_EQUAL, OR, AND
    }

    /**
     * Adds two TypedObjects.
     *
     * @param first The first object.
     * @param second The second object.
     * @return The result object.
     */
    default TypedObject add(TypedObject first, TypedObject second) {
        return null;
    }

    /**
     * Subtracts two TypedObjects.
     *
     * @param first The first object.
     * @param second The second object.
     * @return The result object.
     */
    default TypedObject subtract(TypedObject first, TypedObject second) {
        return null;
    }

    /**
     * Multiplies two TypedObjects.
     *
     * @param first The first object.
     * @param second The second object.
     * @return The result object.
     */
    default TypedObject multiply(TypedObject first, TypedObject second) {
        return null;
    }

    /**
     * Divides two TypedObjects.
     *
     * @param first The first object.
     * @param second The second object.
     * @return The result object.
     */
    default TypedObject divide(TypedObject first, TypedObject second) {
        return null;
    }

    /**
     * Finds the integer remainder after division two TypedObjects.
     *
     * @param first The first object.
     * @param second The second object.
     * @return The result object.
     */
    default TypedObject modulus(TypedObject first, TypedObject second) {
        return null;
    }

    /**
     * Checks to see if the two {@link TypedObject} are equal.
     *
     * @param first The first object.
     * @param second The second object.
     * @return The result TypedObject of type {@link TypeSystem.Type#BOOLEAN}.
     */
    default TypedObject equal(TypedObject first, TypedObject second) {
        return asTypedObject(compare(first, second) == 0);
    }

    /**
     * Checks to see if the two {@link TypedObject} are not equal.
     *
     * @param first The first object.
     * @param second The second object.
     * @return The result TypedObject of type {@link TypeSystem.Type#BOOLEAN}.
     */
    default TypedObject notEqual(TypedObject first, TypedObject second) {
        return asTypedObject(compare(first, second) != 0);
    }

    /**
     * Checks to see if the first {@link TypedObject} is greater than the second.
     *
     * @param first The first object.
     * @param second The second object.
     * @return The result TypedObject of type {@link TypeSystem.Type#BOOLEAN}.
     */
    default TypedObject greater(TypedObject first, TypedObject second) {
        return asTypedObject(compare(first, second) > 0);
    }

    /**
     * Checks to see if the first {@link TypedObject} is less than the second.
     *
     * @param first The first object.
     * @param second The second object.
     * @return The result TypedObject of type {@link TypeSystem.Type#BOOLEAN}.
     */
    default TypedObject less(TypedObject first, TypedObject second) {
        return asTypedObject(compare(first, second) < 0);
    }

    /**
     * Checks to see if the first {@link TypedObject} is greater than or equal to the second.
     *
     * @param first The first object.
     * @param second The second object.
     * @return The result TypedObject of type {@link TypeSystem.Type#BOOLEAN}.
     */
    default TypedObject greaterEqual(TypedObject first, TypedObject second) {
        return asTypedObject(compare(first, second) >= 0);
    }

    /**
     * Checks to see if the first {@link TypedObject} is less than or equal to the second.
     *
     * @param first The first object.
     * @param second The second object.
     * @return The result TypedObject of type {@link TypeSystem.Type#BOOLEAN}.
     */
    default TypedObject lessEqual(TypedObject first, TypedObject second) {
        return asTypedObject(compare(first, second) <= 0);
    }

    /**
     * Logical ors two TypedObjects.
     *
     * @param first The first object.
     * @param second The second object.
     * @return The result object.
     */
    default TypedObject or(TypedObject first, TypedObject second) {
        return null;
    }

    /**
     * Logical ands two TypedObjects.
     *
     * @param first The first object.
     * @param second The second object.
     * @return The result object.
     */
    default TypedObject and(TypedObject first, TypedObject second) {
        return null;
    }

    /**
     * Logical negates a TypedObject.
     *
     * @param object The object.
     * @return The result object.
     */
    default TypedObject not(TypedObject object) {
        return null;
    }

    /**
     * Casts a TypedObject into its given type.
     *
     * @param object The object.
     * @return The result object.
     */
    default TypedObject cast(TypedObject object) {
        return null;
    }

    /**
     * Given a BinaryOperation, finds the operator for it. Null if it cannot.
     *
     * @param operation The operation
     * @return The result binary operator that can be applied.
     */
    default BinaryOperator<TypedObject> dispatch(BinaryOperation operation) {
        // Can assign to a return value and return it, getting rid of the unreachable default...
        Objects.requireNonNull(operation);
        switch (operation) {
            case ADD:
                return this::add;
            case SUBTRACT:
                return this::subtract;
            case MULTIPLY:
                return this::multiply;
            case DIVIDE:
                return this::divide;
            case MODULUS:
                return this::modulus;
            case EQUAL:
                return this::equal;
            case NOT_EQUAL:
                return this::notEqual;
            case GREATER:
                return this::greater;
            case LESS:
                return this::less;
            case GREATER_EQUAL:
                return this::greaterEqual;
            case LESS_EQUAL:
                return this::lessEqual;
            case OR:
                return this::or;
            case AND:
                return this::and;
            default:
                return null;
        }
    }

    /**
     * Given a UnaryOperation, finds the operator for it. Null if it cannot.
     *
     * @param operation The operation.
     * @return The result unary operator that can be applied.
     */
    default UnaryOperator<TypedObject> dispatch(UnaryOperation operation) {
        Objects.requireNonNull(operation);
        switch (operation) {
            case NOT:
                return this::not;
            case CAST:
                return this::cast;
            default:
                return null;
        }
    }
}
