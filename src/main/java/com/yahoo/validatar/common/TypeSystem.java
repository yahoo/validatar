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

import java.util.Map;
import java.util.HashMap;
import java.sql.Timestamp;
import java.math.BigDecimal;

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
     * These are the arithmetic operations we will support.
     */
    public enum ArithmeticOperator {
        ADD, SUBTRACT, MULTIPLY, DIVIDE, MODULUS
    }

    private interface TypeArithmetic {
        /**
         * Takes two of the same TypedObjects and performs an arithmetic on them.
         * Returns null if it cannot, else it returns a new TypedObject with the
         * result with the same type the inputs.
         */
        public TypedObject perform(TypedObject first, TypedObject second);
    }

    private interface TypeConvertor {
        /**
         * Takes a TypedObject and tries to convert it to a different Type.
         * Returns true iff it succeeds. source.type is then the new Type.
         */
        public boolean convert(TypedObject source);
    }

    /**
     * The mapping of an ArithmeticOperator to its implementation.
     */
    private static final Map<ArithmeticOperator, TypeArithmetic> ARITHMETIC = new HashMap<>();
    static {
        ARITHMETIC.put(ArithmeticOperator.ADD, new TypeArithmetic() {
            public TypedObject perform(TypedObject first, TypedObject second) {
                switch(first.type) {
                    case STRING: {
                        String data = (String) first.data + (String) second.data;
                        return new TypedObject(data, Type.STRING);
                    }
                    case LONG: {
                        Long data = (Long) first.data + (Long) second.data;
                        return new TypedObject(data, Type.LONG);
                    }
                    case DOUBLE: {
                        Double data = (Double) first.data + (Double) second.data;
                        return new TypedObject(data, Type.DOUBLE);
                    }
                    case DECIMAL: {
                        BigDecimal data = ((BigDecimal) first.data).add((BigDecimal) second.data);
                        return new TypedObject(data, Type.DECIMAL);
                    }
                    case TIMESTAMP: {
                        Timestamp data = new Timestamp(((Timestamp) first.data).getTime() + ((Timestamp) second.data).getTime());
                        return new TypedObject(data, Type.TIMESTAMP);
                    }
                    case BOOLEAN:
                    default:
                        return null;
                }
            }
        });
        ARITHMETIC.put(ArithmeticOperator.SUBTRACT, new TypeArithmetic() {
            public TypedObject perform(TypedObject first, TypedObject second) {
                switch(first.type) {
                    case LONG: {
                        Long data = (Long) first.data - (Long) second.data;
                        return new TypedObject(data, Type.LONG);
                    }
                    case DOUBLE: {
                        Double data = (Double) first.data - (Double) second.data;
                        return new TypedObject(data, Type.DOUBLE);
                    }
                    case DECIMAL: {
                        BigDecimal data = ((BigDecimal) first.data).subtract((BigDecimal) second.data);
                        return new TypedObject(data, Type.DECIMAL);
                    }
                    case TIMESTAMP: {
                        Timestamp data = new Timestamp(((Timestamp) first.data).getTime() - ((Timestamp) second.data).getTime());
                        return new TypedObject(data, Type.TIMESTAMP);
                    }
                    case STRING:
                    case BOOLEAN:
                    default:
                        return null;
                }
            }
        });
        ARITHMETIC.put(ArithmeticOperator.MULTIPLY, new TypeArithmetic() {
            public TypedObject perform(TypedObject first, TypedObject second) {
                switch(first.type) {
                    case LONG: {
                        Long data = (Long) first.data * (Long) second.data;
                        return new TypedObject(data, Type.LONG);
                    }
                    case DOUBLE: {
                        Double data = (Double) first.data * (Double) second.data;
                        return new TypedObject(data, Type.DOUBLE);
                    }
                    case DECIMAL: {
                        BigDecimal data = ((BigDecimal) first.data).multiply((BigDecimal) second.data);
                        return new TypedObject(data, Type.DECIMAL);
                    }
                    case TIMESTAMP: {
                        Timestamp data = new Timestamp(((Timestamp) first.data).getTime() * ((Timestamp) second.data).getTime());
                        return new TypedObject(data, Type.TIMESTAMP);
                    }
                    case STRING:
                    case BOOLEAN:
                    default:
                        return null;
                }
            }
        });
        ARITHMETIC.put(ArithmeticOperator.DIVIDE, new TypeArithmetic() {
            public TypedObject perform(TypedObject first, TypedObject second) {
                switch(first.type) {
                    case LONG: {
                        Long data = (Long) first.data / (Long) second.data;
                        return new TypedObject(data, Type.LONG);
                    }
                    case DOUBLE: {
                        Double data = (Double) first.data / (Double) second.data;
                        return new TypedObject(data, Type.DOUBLE);
                    }
                    case DECIMAL: {
                        BigDecimal data = ((BigDecimal) first.data).divide((BigDecimal) second.data);
                        return new TypedObject(data, Type.DECIMAL);
                    }
                    case TIMESTAMP: {
                        Timestamp data = new Timestamp(((Timestamp) first.data).getTime() / ((Timestamp) second.data).getTime());
                        return new TypedObject(data, Type.TIMESTAMP);
                    }
                    case STRING:
                    case BOOLEAN:
                    default:
                        return null;
                }
            }
        });
        ARITHMETIC.put(ArithmeticOperator.MODULUS, new TypeArithmetic() {
            public TypedObject perform(TypedObject first, TypedObject second) {
                switch(first.type) {
                    case LONG: {
                        Long data = (Long) first.data % (Long) second.data;
                        return new TypedObject(data, Type.LONG);
                    }
                    case TIMESTAMP:
                        Timestamp data = new Timestamp(((Timestamp) first.data).getTime() % ((Timestamp) second.data).getTime());
                        return new TypedObject(data, Type.TIMESTAMP);
                    case STRING:
                    case DOUBLE:
                    case DECIMAL:
                        BigDecimal[] results = ((BigDecimal) first.data).divideAndRemainder((BigDecimal) second.data);
                        return new TypedObject(results[1], Type.DECIMAL);
                    case BOOLEAN:
                    default:
                        return null;
                }
            }
        });
    }

    /**
     * The mapping of Type to its convertor.
     *
     * In general, we don't want lossy casting, or strange casting like a boolean to a short etc.
     * But we will follow the basic Java widening primitive rules.
     * https://docs.oracle.com/javase/specs/jls/se7/html/jls-5.html
     *
     * Exceptions:
     * Timestamp to and from Long will do a millis since epoch
     */
    private static final Map<Type, TypeConvertor> CONVERTORS = new HashMap<>();
    static {
        CONVERTORS.put(Type.LONG, new TypeConvertor() {
            public boolean convert(TypedObject source) {
                switch(source.type) {
                    case STRING:
                        source.data = Long.valueOf((String) source.data);
                        source.type = Type.LONG;
                        return true;
                    case LONG:
                        return true;
                    case TIMESTAMP:
                        source.data = ((Timestamp) source.data).getTime();
                        source.type = Type.LONG;
                        return true;
                    case DOUBLE:
                    case DECIMAL:
                    case BOOLEAN:
                    default:
                        return false;
                }
            }
        });
        CONVERTORS.put(Type.DOUBLE, new TypeConvertor() {
            public boolean convert(TypedObject source) {
                switch(source.type) {
                    case STRING:
                        source.data = Double.valueOf((String) source.data);
                        source.type = Type.DOUBLE;
                        return true;
                    case DOUBLE:
                        return true;
                    case LONG:
                        source.data = ((Long) source.data).doubleValue();
                        source.type = Type.DOUBLE;
                        return true;
                    case DECIMAL:
                    case BOOLEAN:
                    case TIMESTAMP:
                    default:
                        return false;
                }
            }
        });
        CONVERTORS.put(Type.DECIMAL, new TypeConvertor() {
            public boolean convert(TypedObject source) {
                switch(source.type) {
                    case STRING:
                        source.data = new BigDecimal((String) source.data);
                        source.type = Type.DECIMAL;
                        return true;
                    case LONG:
                        source.data = BigDecimal.valueOf((Long) source.data);
                        source.type = Type.DECIMAL;
                        return true;
                    case DOUBLE:
                        source.data = BigDecimal.valueOf((Double) source.data);
                        source.type = Type.DECIMAL;
                        return true;
                    case DECIMAL:
                        return true;
                    case TIMESTAMP:
                        source.data = BigDecimal.valueOf(((Timestamp) source.data).getTime());
                        source.type = Type.DECIMAL;
                        return true;
                    case BOOLEAN:
                    default:
                        return false;
                }
            }
        });
        CONVERTORS.put(Type.BOOLEAN, new TypeConvertor() {
            public boolean convert(TypedObject source) {
                switch(source.type) {
                    case STRING:
                        source.data = Boolean.valueOf((String) source.data);
                        source.type = Type.BOOLEAN;
                        return true;
                    case BOOLEAN:
                        return true;
                    case LONG:
                    case DOUBLE:
                    case DECIMAL:
                    case TIMESTAMP:
                    default:
                        return false;
                }
            }
        });
        CONVERTORS.put(Type.STRING, new TypeConvertor() {
            public boolean convert(TypedObject source) {
                switch(source.type) {
                    case STRING:
                        return true;
                    case LONG:
                        source.data = ((Long) source.data).toString();
                        source.type = Type.STRING;
                        return true;
                    case DOUBLE:
                        source.data = ((Double) source.data).toString();
                        source.type = Type.STRING;
                        return true;
                    case DECIMAL:
                        source.data = ((BigDecimal) source.data).toString();
                        source.type = Type.STRING;
                        return true;
                    case BOOLEAN:
                        source.data = ((Boolean) source.data).toString();
                        source.type = Type.STRING;
                        return true;
                    case TIMESTAMP:
                    default:
                        return false;
                }
            }
        });
        CONVERTORS.put(Type.TIMESTAMP, new TypeConvertor() {
            public boolean convert(TypedObject source) {
                switch(source.type) {
                    case LONG:
                        source.data = new Timestamp((Long) source.data);
                        source.type = Type.TIMESTAMP;
                        return true;
                    case TIMESTAMP:
                        return true;
                    case STRING:
                    case DOUBLE:
                    case DECIMAL:
                    case BOOLEAN:
                    default:
                        return false;
                }
            }
        });
    }

    /**
     * Tries to convert two TypedObjects to the same type. Tries first to second or second to
     * first, in that order.  Throws an ClassCastException if neither could be done or if the
     * final Type wasn't what was passed in. Throws an NullPointerException if either argument
     * is null. If successful, first and second will be the of the same type.
     * @param first The first object.
     * @param second The first object.
     */
    public static void unifyType(TypedObject first, TypedObject second) {
        if (first == null || second == null) {
            throw new NullPointerException("Cannot operate on null arguments. Argument 1: " + first + " Argument 2: " + second);
        }
        // Relying on the type system to return false if invalid conversions were tried and true if it was converted.
        boolean isUnified = CONVERTORS.get(first.type).convert(second);
        if (!isUnified) {
            isUnified = CONVERTORS.get(second.type).convert(first);
        }

        if (!isUnified) {
            throw new ClassCastException("Type conversion could not be performed for types: " + first.type + " and " + second.type +
                                          " with values: " + first.data.toString() + " and " + second.data.toString());
        }
    }

    /**
     * Perform arithmetic on two TypedObjects.
     *
     * @param operator The {@link com.yahoo.validatar.common.TypeSystem.ArithmeticOperator} operator to perform.
     * @param first The LHS of the arithmetic
     * @param second The RHS of the arithmetic
     * @return The resulting TypedObject.
     */
    public static TypedObject doArithmetic(ArithmeticOperator operator, TypedObject first, TypedObject second) {
        unifyType(first, second);
        // Both are now the same type, do the arithmetic
        TypedObject result = ARITHMETIC.get(operator).perform(first, second);

        if (result == null) {
            throw new ClassCastException("Unable to perform operation: " + operator + " on " +
                                         first.data.toString() + " and " + second.data.toString());
        }
        return result;
    }

    /**
     * Compare two TypedObjects.
     *
     * @param first The first object to compare.
     * @param second The second object to compare.
     * @return -1 if first is less than second, 1 if first is greater than second and 0 if first equals second.
     */
    @SuppressWarnings("unchecked")
    public static int compare(TypedObject first, TypedObject second) {
        unifyType(first, second);
        // Both are now the same type, just compare
        return first.data.compareTo(second.data);
    }

    private static void checkType(TypedObject object, Type type) {
        if (object == null) {
            throw new NullPointerException("Cannot operate on null argument");
        }
        if (object.type != type) {
            throw new ClassCastException("Input: " + object.data.toString() + " was not of the expected type: " + type);
        }
    }

    /*
     ********************************************************************************
     *                                Helper methods                                *
     ********************************************************************************
     */

    /**
     * Helper method that returns the sum of the first and second.
     *
     * @param first A {@link com.yahoo.validatar.common.TypedObject} object.
     * @param second A {@link com.yahoo.validatar.common.TypedObject} object.
     * @return The {@link com.yahoo.validatar.common.TypedObject} result.
     */
    public static TypedObject add(TypedObject first, TypedObject second) {
        return doArithmetic(ArithmeticOperator.ADD, first, second);
    }

    /**
     * Helper method that returns the difference of the first and second.
     *
     * @param first The LHS {@link com.yahoo.validatar.common.TypedObject} object.
     * @param second The RHS {@link com.yahoo.validatar.common.TypedObject} object.
     * @return The {@link com.yahoo.validatar.common.TypedObject} result.
     */
    public static TypedObject subtract(TypedObject first, TypedObject second) {
        return doArithmetic(ArithmeticOperator.SUBTRACT, first, second);
    }

    /**
     * Helper method that returns the product of the first and second.
     *
     * @param first The LHS {@link com.yahoo.validatar.common.TypedObject} object.
     * @param second The RHS {@link com.yahoo.validatar.common.TypedObject} object.
     * @return The {@link com.yahoo.validatar.common.TypedObject} result.
     */
    public static TypedObject multiply(TypedObject first, TypedObject second) {
        return doArithmetic(ArithmeticOperator.MULTIPLY, first, second);
    }

    /**
     * Helper method that returns the quotient of the first divided by second.
     *
     * @param first The LHS {@link com.yahoo.validatar.common.TypedObject} object.
     * @param second The RHS {@link com.yahoo.validatar.common.TypedObject} object.
     * @return The {@link com.yahoo.validatar.common.TypedObject} result.
     */
    public static TypedObject divide(TypedObject first, TypedObject second) {
        return doArithmetic(ArithmeticOperator.DIVIDE, first, second);
    }

    /**
     * Helper method that returns the modulus of the first and second.
     *
     * @param first The LHS {@link com.yahoo.validatar.common.TypedObject} object.
     * @param second The RHS {@link com.yahoo.validatar.common.TypedObject} object.
     * @return The {@link com.yahoo.validatar.common.TypedObject} result.
     */
    public static TypedObject modulus(TypedObject first, TypedObject second) {
        return doArithmetic(ArithmeticOperator.MODULUS, first, second);
    }

    /**
     * Helper method that negates the input.
     *
     * @param input a {@link com.yahoo.validatar.common.TypedObject} object.
     * @return The {@link com.yahoo.validatar.common.TypedObject} result.
     */
    public static TypedObject negate(TypedObject input) {
        // We'll use our narrowest numeric type - LONG. STRING would work too.
        return doArithmetic(ArithmeticOperator.MULTIPLY, new TypedObject(Long.valueOf(-1), Type.LONG), input);
    }

    /**
     * Helper method that returns true iff first equals second.
     *
     * @param first The LHS {@link com.yahoo.validatar.common.TypedObject} object.
     * @param second The RHS {@link com.yahoo.validatar.common.TypedObject} object.
     * @return The {@link com.yahoo.validatar.common.TypedObject} result.
     */
    public static TypedObject isEqualTo(TypedObject first, TypedObject second) {
        return asTypedObject(compare(first, second) == 0);
    }

    /**
     * Helper method that returns true iff first not equals second.
     *
     * @param first The LHS {@link com.yahoo.validatar.common.TypedObject} object.
     * @param second The RHS {@link com.yahoo.validatar.common.TypedObject} object.
     * @return The {@link com.yahoo.validatar.common.TypedObject} result.
     */
    public static TypedObject isNotEqualTo(TypedObject first, TypedObject second) {
        return asTypedObject(compare(first, second) != 0);
    }

    /**
     * Helper method that returns true iff first is less than second.
     *
     * @param first The LHS {@link com.yahoo.validatar.common.TypedObject} object.
     * @param second The RHS {@link com.yahoo.validatar.common.TypedObject} object.
     * @return The {@link com.yahoo.validatar.common.TypedObject} result.
     */
    public static TypedObject isLessThan(TypedObject first, TypedObject second) {
        return asTypedObject(compare(first, second) < 0);
    }

    /**
     * Helper method that returns true iff first is less than or equal to second.
     *
     * @param first The LHS {@link com.yahoo.validatar.common.TypedObject} object.
     * @param second The RHS {@link com.yahoo.validatar.common.TypedObject} object.
     * @return The {@link com.yahoo.validatar.common.TypedObject} result.
     */
    public static TypedObject isLessThanOrEqual(TypedObject first, TypedObject second) {
        return asTypedObject(compare(first, second) <= 0);
    }

    /**
     * Helper method that returns true iff first is greater than second.
     *
     * @param first The LHS {@link com.yahoo.validatar.common.TypedObject} object.
     * @param second The RHS {@link com.yahoo.validatar.common.TypedObject} object.
     * @return The {@link com.yahoo.validatar.common.TypedObject} result.
     */
    public static TypedObject isGreaterThan(TypedObject first, TypedObject second) {
        return asTypedObject(compare(first, second) > 0);
    }

    /**
     * Helper method that returns true iff first is greater than or equal to second.
     *
     * @param first The LHS {@link com.yahoo.validatar.common.TypedObject} object.
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
        checkType(input, Type.BOOLEAN);
        return asTypedObject(!(Boolean) input.data);
    }

    /**
     * Helper method to do logical and.
     *
     * @param first The LHS {@link com.yahoo.validatar.common.TypedObject} object.
     * @param second The RHS {@link com.yahoo.validatar.common.TypedObject} object.
     * @return The {@link com.yahoo.validatar.common.TypedObject} result.
     */
    public static TypedObject logicalAnd(TypedObject first, TypedObject second) {
        checkType(first, Type.BOOLEAN);
        checkType(second, Type.BOOLEAN);
        return asTypedObject((Boolean) first.data && (Boolean) second.data);
    }

    /**
     * Helper method to do logical or.
     *
     * @param first The LHS {@link com.yahoo.validatar.common.TypedObject} object.
     * @param second The RHS {@link com.yahoo.validatar.common.TypedObject} object.
     * @return The {@link com.yahoo.validatar.common.TypedObject} result.
     */
    public static TypedObject logicalOr(TypedObject first, TypedObject second) {
        checkType(first, Type.BOOLEAN);
        checkType(second, Type.BOOLEAN);
        return asTypedObject((Boolean) first.data || (Boolean) second.data);
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
