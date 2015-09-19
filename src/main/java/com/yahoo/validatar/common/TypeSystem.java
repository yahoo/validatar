/*
 * Copyright 2014-2015 Yahoo! Inc.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package com.yahoo.validatar.common;

import java.math.BigDecimal;
import java.sql.Timestamp;
import java.util.HashMap;
import java.util.Map;
import java.util.Objects;
import java.util.function.BinaryOperator;
import java.util.function.UnaryOperator;

/**
 * This is a class that wraps the supported types that the assertor will work with
 * when operating on data returned by an data source.
 */
public class TypeSystem {
    /**
     * These are the official types we support. They correspond to Java types.
     * They all must correspond to an actual object that implements Comparable.
     */
    public enum Type {
        LONG, DOUBLE, DECIMAL, BOOLEAN, STRING, TIMESTAMP
    }

    /**
     * These are the binary operations we will support.
     */
    public enum BinaryOperation {
        ADD, SUBTRACT, MULTIPLY, DIVIDE, MODULUS, OR, AND
    }

    /**
     * These are the unary operations we will support.
     */
    public enum UnaryOperation {
        CAST, NEGATE
    }

    /*
     * The mapping of Type to its operations.
     * <p/>
     * In general, we don't want lossy casting, or strange casting like a boolean to a short etc.
     * But we will follow the basic Java widening primitive rules.
     * https://docs.oracle.com/javase/specs/jls/se7/html/jls-5.html
     * <p/>
     * Exceptions:
     * Timestamp to and from Long will do a millis since epoch
     */
    public interface Operations {
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
        default TypedObject negate(TypedObject object) {
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
                case NEGATE:
                    return this::negate;
                case CAST:
                    return this::cast;
                default:
                    return null;
            }
        }
    }

    private static Map<Type, Operations> operations = new HashMap<>();
    static {
        operations.put(Type.LONG, new Operations() {
            public TypedObject add(TypedObject first, TypedObject second) {
                return asTypedObject((Long) first.data + (Long) second.data);
            }

            public TypedObject subtract(TypedObject first, TypedObject second) {
                return asTypedObject((Long) first.data - (Long) second.data);
            }

            public TypedObject multiply(TypedObject first, TypedObject second) {
                return asTypedObject((Long) first.data * (Long) second.data);
            }

            public TypedObject divide(TypedObject first, TypedObject second) {
                return asTypedObject((Long) first.data / (Long) second.data);
            }

            public TypedObject modulus(TypedObject first, TypedObject second) {
                return asTypedObject((Long) first.data % (Long) second.data);
            }

            public TypedObject cast(TypedObject object) {
                switch (object.type) {
                    case STRING:
                        object.data = Long.valueOf((String) object.data);
                        break;
                    case LONG:
                        break;
                    case TIMESTAMP:
                        object.data = ((Timestamp) object.data).getTime();
                        break;
                    case DOUBLE:
                    case DECIMAL:
                    case BOOLEAN:
                        return null;
                }
                object.type = Type.LONG;
                return object;
            }
        });

        operations.put(Type.DOUBLE, new Operations() {
            public TypedObject add(TypedObject first, TypedObject second) {
                return asTypedObject((Double) first.data + (Double) second.data);
            }

            public TypedObject subtract(TypedObject first, TypedObject second) {
                return asTypedObject((Double) first.data - (Double) second.data);
            }

            public TypedObject multiply(TypedObject first, TypedObject second) {
                return asTypedObject((Double) first.data * (Double) second.data);
            }

            public TypedObject divide(TypedObject first, TypedObject second) {
                return asTypedObject((Double) first.data / (Double) second.data);
            }

            public TypedObject cast(TypedObject object) {
                switch (object.type) {
                    case STRING:
                        object.data = Double.valueOf((String) object.data);
                        break;
                    case DOUBLE:
                        break;
                    case LONG:
                        object.data = ((Long) object.data).doubleValue();
                        break;
                    case DECIMAL:
                    case BOOLEAN:
                    case TIMESTAMP:
                        return null;
                }
                object.type = Type.DOUBLE;
                return object;
            }
        });

        operations.put(Type.DECIMAL, new Operations() {
            public TypedObject add(TypedObject first, TypedObject second) {
                return asTypedObject(((BigDecimal) first.data).add((BigDecimal) second.data));
            }

            public TypedObject subtract(TypedObject first, TypedObject second) {
                return asTypedObject(((BigDecimal) first.data).subtract((BigDecimal) second.data));
            }

            public TypedObject multiply(TypedObject first, TypedObject second) {
                return asTypedObject(((BigDecimal) first.data).multiply((BigDecimal) second.data));
            }

            public TypedObject divide(TypedObject first, TypedObject second) {
                return asTypedObject(((BigDecimal) first.data).divide((BigDecimal) second.data));
            }

            public TypedObject modulus(TypedObject first, TypedObject second) {
                return asTypedObject(((BigDecimal) first.data).divideAndRemainder((BigDecimal) second.data)[1]);
            }

            public TypedObject cast(TypedObject object) {
                switch (object.type) {
                    case STRING:
                        object.data = new BigDecimal((String) object.data);
                        break;
                    case LONG:
                        object.data = BigDecimal.valueOf((Long) object.data);
                        break;
                    case DOUBLE:
                        object.data = BigDecimal.valueOf((Double) object.data);
                        break;
                    case DECIMAL:
                        break;
                    case TIMESTAMP:
                        object.data = BigDecimal.valueOf(((Timestamp) object.data).getTime());
                        break;
                    case BOOLEAN:
                        return null;
                }
                object.type = Type.DECIMAL;
                return object;
            }
        });

        operations.put(Type.BOOLEAN, new Operations() {
            public TypedObject or(TypedObject first, TypedObject second) {
                return asTypedObject((Boolean) first.data || (Boolean) second.data);
            }

            public TypedObject and(TypedObject first, TypedObject second) {
                return asTypedObject((Boolean) first.data && (Boolean) second.data);
            }

            public TypedObject negate(TypedObject object) {
                return asTypedObject(!(Boolean) object.data);
            }

            public TypedObject cast(TypedObject object) {
                switch (object.type) {
                    case STRING:
                        object.data = Boolean.valueOf((String) object.data);
                        break;
                    case BOOLEAN:
                        break;
                    case LONG:
                    case DOUBLE:
                    case DECIMAL:
                    case TIMESTAMP:
                        return null;
                }
                object.type = Type.BOOLEAN;
                return object;
            }
        });

        operations.put(Type.STRING, new Operations() {
            public TypedObject add(TypedObject first, TypedObject second) {
                return asTypedObject((String) first.data + (String) second.data);
            }

            public TypedObject cast(TypedObject object) {
                switch (object.type) {
                    case STRING:
                        break;
                    case LONG:
                        object.data = ((Long) object.data).toString();
                        break;
                    case DOUBLE:
                        object.data = ((Double) object.data).toString();
                        break;
                    case DECIMAL:
                        object.data = ((BigDecimal) object.data).toString();
                        break;
                    case BOOLEAN:
                        object.data = ((Boolean) object.data).toString();
                        break;
                    case TIMESTAMP:
                        return null;
                }
                object.type = Type.STRING;
                return object;
            }
        });

        operations.put(Type.TIMESTAMP, new Operations() {
            public TypedObject add(TypedObject first, TypedObject second) {
                return asTypedObject(new Timestamp(((Timestamp) first.data).getTime() + ((Timestamp) second.data).getTime()));
            }

            public TypedObject subtract(TypedObject first, TypedObject second) {
                return asTypedObject(new Timestamp(((Timestamp) first.data).getTime() - ((Timestamp) second.data).getTime()));
            }

            public TypedObject multiply(TypedObject first, TypedObject second) {
                return asTypedObject(new Timestamp(((Timestamp) first.data).getTime() * ((Timestamp) second.data).getTime()));
            }

            public TypedObject divide(TypedObject first, TypedObject second) {
                return asTypedObject(new Timestamp(((Timestamp) first.data).getTime() / ((Timestamp) second.data).getTime()));
            }

            public TypedObject modulus(TypedObject first, TypedObject second) {
                return asTypedObject(new Timestamp(((Timestamp) first.data).getTime() % ((Timestamp) second.data).getTime()));
            }

            public TypedObject cast(TypedObject object) {
                switch (object.type) {
                    case LONG:
                        object.data = new Timestamp((Long) object.data);
                        break;
                    case TIMESTAMP:
                        break;
                    case STRING:
                    case DOUBLE:
                    case DECIMAL:
                    case BOOLEAN:
                        return null;
                }
                object.type = Type.TIMESTAMP;
                return object;
            }
        });
    }

    /**
     * Tries to convert two TypedObjects to the same type. Tries first to second or second to
     * first, in that order.  Throws an ClassCastException if neither could be done or if the
     * final Type wasn't what was passed in. Throws an NullPointerException if either argument
     * is null. If successful, first and second will be the of the same type.
     *
     * @param first  The first {@link com.yahoo.validatar.common.TypedObject}.
     * @param second The first {@link com.yahoo.validatar.common.TypedObject}.
     */
    public static void unifyType(TypedObject first, TypedObject second) {
        if (first == null || second == null) {
            throw new NullPointerException("Cannot operate on null arguments. Argument 1: " + first + " Argument 2: " + second);
        }
        // Relying on the type system to return null if invalid conversions were tried and non null if not.
        TypedObject unified = operations.get(first.type).cast(second);
        if (unified == null) {
            unified = operations.get(second.type).cast(first);
        }

        if (unified == null) {
            throw new ClassCastException("Type unification could not be performed for " + first + " and " + second);
        }
    }

    /**
     * Perform a binary operation on two TypedObjects.
     *
     * @param operation The {@link com.yahoo.validatar.common.TypeSystem.BinaryOperation} operator to perform.
     * @param first     The LHS {@link com.yahoo.validatar.common.TypedObject} of the arithmetic.
     * @param second    The RHS {@link com.yahoo.validatar.common.TypedObject} of the arithmetic.
     * @return The resulting {@link com.yahoo.validatar.common.TypedObject}.
     */
    public static TypedObject perform(BinaryOperation operation, TypedObject first, TypedObject second) {
        unifyType(first, second);

        // Both are now the same type, do the operation
        TypedObject result = operations.get(first.type).dispatch(operation).apply(first, second);

        if (result == null) {
            throw new ClassCastException("Unable to perform: " + operation + " on " + first + " and " + second);
        }
        return result;
    }

    /**
     * Perform an unary operation on two TypedObjects.
     *
     * @param operation The {@link com.yahoo.validatar.common.TypeSystem.UnaryOperation} operator to perform.
     * @param object    The target {@link com.yahoo.validatar.common.TypedObject} of the operation.
     * @return The resulting {@link com.yahoo.validatar.common.TypedObject}.
     */
    public static TypedObject perform(UnaryOperation operation, TypedObject object) {
        TypedObject result = operations.get(object.type).dispatch(operation).apply(object);

        if (result == null) {
            throw new ClassCastException("Unable to perform: " + operation + " on " + object);
        }
        return result;
    }

    /**
     * Compare two TypedObjects.
     *
     * @param first  The first {@link com.yahoo.validatar.common.TypedObject} to compare.
     * @param second The second {@link com.yahoo.validatar.common.TypedObject} to compare.
     * @return -1 if first is less than second, 1 if first is greater than second and 0 if first equals second.
     */
    @SuppressWarnings("unchecked")
    public static int compare(TypedObject first, TypedObject second) {
        unifyType(first, second);
        // Both are now the same type, just compare
        return first.data.compareTo(second.data);
    }

    /*
     ********************************************************************************
     *                                Helper methods                                *
     ********************************************************************************
     */

    /**
     * Helper method that returns the sum of the first and second.
     *
     * @param first  A {@link com.yahoo.validatar.common.TypedObject} object.
     * @param second A {@link com.yahoo.validatar.common.TypedObject} object.
     * @return The {@link com.yahoo.validatar.common.TypedObject} result.
     */
    public static TypedObject add(TypedObject first, TypedObject second) {
        return perform(BinaryOperation.ADD, first, second);
    }

    /**
     * Helper method that returns the difference of the first and second.
     *
     * @param first  The LHS {@link com.yahoo.validatar.common.TypedObject} object.
     * @param second The RHS {@link com.yahoo.validatar.common.TypedObject} object.
     * @return The {@link com.yahoo.validatar.common.TypedObject} result.
     */
    public static TypedObject subtract(TypedObject first, TypedObject second) {
        return perform(BinaryOperation.SUBTRACT, first, second);
    }

    /**
     * Helper method that returns the product of the first and second.
     *
     * @param first  The LHS {@link com.yahoo.validatar.common.TypedObject} object.
     * @param second The RHS {@link com.yahoo.validatar.common.TypedObject} object.
     * @return The {@link com.yahoo.validatar.common.TypedObject} result.
     */
    public static TypedObject multiply(TypedObject first, TypedObject second) {
        return perform(BinaryOperation.MULTIPLY, first, second);
    }

    /**
     * Helper method that returns the quotient of the first divided by second.
     *
     * @param first  The LHS {@link com.yahoo.validatar.common.TypedObject} object.
     * @param second The RHS {@link com.yahoo.validatar.common.TypedObject} object.
     * @return The {@link com.yahoo.validatar.common.TypedObject} result.
     */
    public static TypedObject divide(TypedObject first, TypedObject second) {
        return perform(BinaryOperation.DIVIDE, first, second);
    }

    /**
     * Helper method that returns the modulus of the first and second.
     *
     * @param first  The LHS {@link com.yahoo.validatar.common.TypedObject} object.
     * @param second The RHS {@link com.yahoo.validatar.common.TypedObject} object.
     * @return The {@link com.yahoo.validatar.common.TypedObject} result.
     */
    public static TypedObject modulus(TypedObject first, TypedObject second) {
        return perform(BinaryOperation.MODULUS, first, second);
    }

    /**
     * Helper method that negates the input.
     *
     * @param input a {@link com.yahoo.validatar.common.TypedObject} object.
     * @return The {@link com.yahoo.validatar.common.TypedObject} result.
     */
    public static TypedObject negate(TypedObject input) {
        // We'll use our narrowest numeric type - LONG. STRING would work too.
        return perform(BinaryOperation.MULTIPLY, new TypedObject(Long.valueOf(-1), Type.LONG), input);
    }

    /**
     * Helper method that returns true iff first equals second.
     *
     * @param first  The LHS {@link com.yahoo.validatar.common.TypedObject} object.
     * @param second The RHS {@link com.yahoo.validatar.common.TypedObject} object.
     * @return The {@link com.yahoo.validatar.common.TypedObject} result.
     */
    public static TypedObject isEqualTo(TypedObject first, TypedObject second) {
        return asTypedObject(compare(first, second) == 0);
    }

    /**
     * Helper method that returns true iff first not equals second.
     *
     * @param first  The LHS {@link com.yahoo.validatar.common.TypedObject} object.
     * @param second The RHS {@link com.yahoo.validatar.common.TypedObject} object.
     * @return The {@link com.yahoo.validatar.common.TypedObject} result.
     */
    public static TypedObject isNotEqualTo(TypedObject first, TypedObject second) {
        return asTypedObject(compare(first, second) != 0);
    }

    /**
     * Helper method that returns true iff first is less than second.
     *
     * @param first  The LHS {@link com.yahoo.validatar.common.TypedObject} object.
     * @param second The RHS {@link com.yahoo.validatar.common.TypedObject} object.
     * @return The {@link com.yahoo.validatar.common.TypedObject} result.
     */
    public static TypedObject isLessThan(TypedObject first, TypedObject second) {
        return asTypedObject(compare(first, second) < 0);
    }

    /**
     * Helper method that returns true iff first is less than or equal to second.
     *
     * @param first  The LHS {@link com.yahoo.validatar.common.TypedObject} object.
     * @param second The RHS {@link com.yahoo.validatar.common.TypedObject} object.
     * @return The {@link com.yahoo.validatar.common.TypedObject} result.
     */
    public static TypedObject isLessThanOrEqual(TypedObject first, TypedObject second) {
        return asTypedObject(compare(first, second) <= 0);
    }

    /**
     * Helper method that returns true iff first is greater than second.
     *
     * @param first  The LHS {@link com.yahoo.validatar.common.TypedObject} object.
     * @param second The RHS {@link com.yahoo.validatar.common.TypedObject} object.
     * @return The {@link com.yahoo.validatar.common.TypedObject} result.
     */
    public static TypedObject isGreaterThan(TypedObject first, TypedObject second) {
        return asTypedObject(compare(first, second) > 0);
    }

    /**
     * Helper method that returns true iff first is greater than or equal to second.
     *
     * @param first  The LHS {@link com.yahoo.validatar.common.TypedObject} object.
     * @param second The RHS {@link com.yahoo.validatar.common.TypedObject} object.
     * @return The {@link com.yahoo.validatar.common.TypedObject} result.
     */
    public static TypedObject isGreaterThanOrEqual(TypedObject first, TypedObject second) {
        return asTypedObject(compare(first, second) >= 0);
    }

    /**
     * Helper method to do logical negation.
     *
     * @param input a {@link com.yahoo.validatar.common.TypedObject} object.
     * @return The {@link com.yahoo.validatar.common.TypedObject} result.
     */
    public static TypedObject logicalNegate(TypedObject input) {
        return perform(UnaryOperation.NEGATE, input);
    }

    /**
     * Helper method to do logical and.
     *
     * @param first  The LHS {@link com.yahoo.validatar.common.TypedObject} object.
     * @param second The RHS {@link com.yahoo.validatar.common.TypedObject} object.
     * @return The {@link com.yahoo.validatar.common.TypedObject} result.
     */
    public static TypedObject logicalAnd(TypedObject first, TypedObject second) {
        return perform(BinaryOperation.AND, first, second);
    }

    /**
     * Helper method to do logical or.
     *
     * @param first  The LHS {@link com.yahoo.validatar.common.TypedObject} object.
     * @param second The RHS {@link com.yahoo.validatar.common.TypedObject} object.
     * @return The {@link com.yahoo.validatar.common.TypedObject} result.
     */
    public static TypedObject logicalOr(TypedObject first, TypedObject second) {
        return perform(BinaryOperation.OR, first, second);
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
     * @return The wrapped {@link com.yahoo.validatar.common.TypedObject} result.
     */
    public static TypedObject asTypedObject(Boolean value) {
        return new TypedObject(value, Type.BOOLEAN);
    }

    /**
     * Takes a Long and wraps it a proper TypedObject.
     *
     * @param value a {@link java.lang.Long} object.
     * @return The wrapped {@link com.yahoo.validatar.common.TypedObject} result.
     */
    public static TypedObject asTypedObject(Long value) {
        return new TypedObject(value, Type.LONG);
    }

    /**
     * Takes a Double and wraps it a proper TypedObject.
     *
     * @param value a {@link java.lang.Double} object.
     * @return The wrapped {@link com.yahoo.validatar.common.TypedObject} result.
     */
    public static TypedObject asTypedObject(Double value) {
        return new TypedObject(value, Type.DOUBLE);
    }

    /**
     * Takes a Double and wraps it a proper TypedObject.
     *
     * @param value a {@link java.math.BigDecimal} object.
     * @return The wrapped {@link com.yahoo.validatar.common.TypedObject} result.
     */
    public static TypedObject asTypedObject(BigDecimal value) {
        return new TypedObject(value, Type.DECIMAL);
    }

    /**
     * Takes a Timestamp and wraps it a proper TypedObject.
     *
     * @param value a {@link java.sql.Timestamp} object.
     * @return The wrapped {@link com.yahoo.validatar.common.TypedObject} result.
     */
    public static TypedObject asTypedObject(Timestamp value) {
        return new TypedObject(value, Type.TIMESTAMP);
    }

    /**
     * Takes a String and wraps it a proper TypedObject.
     *
     * @param value a {@link java.lang.String} object.
     * @return The wrapped {@link com.yahoo.validatar.common.TypedObject} result.
     */
    public static TypedObject asTypedObject(String value) {
        return new TypedObject(value, Type.STRING);
    }
}
