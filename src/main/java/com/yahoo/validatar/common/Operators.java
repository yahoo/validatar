/*
 * Copyright 2017 Yahoo Inc.
 * Licensed under the terms of the Apache 2 license. Please see LICENSE file in the project root for terms.
 */
package com.yahoo.validatar.common;

import java.math.BigDecimal;
import java.sql.Timestamp;

import static com.yahoo.validatar.common.TypeSystem.asTypedObject;

/**
 * Contains the various type specific {@link Operations}.
 * <p>
 * In general, we don't want lossy casting, or strange casting like a boolean to a short etc.
 * But we will follow the basic Java widening primitive rules.
 * https://docs.oracle.com/javase/specs/jls/se7/html/jls-5.html
 * </p>
 * Exceptions:
 * Timestamp to and from Long will do a millis since epoch
 */
public class Operators {
    public static class BooleanOperator implements Operations {
        @Override
        public TypedObject or(TypedObject first, TypedObject second) {
            return asTypedObject((Boolean) first.data || (Boolean) second.data);
        }

        @Override
        public TypedObject and(TypedObject first, TypedObject second) {
            return asTypedObject((Boolean) first.data && (Boolean) second.data);
        }

        @Override
        public TypedObject not(TypedObject object) {
            return asTypedObject(!(Boolean) object.data);
        }

        @Override
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
            object.type = TypeSystem.Type.BOOLEAN;
            return object;
        }
    }

    public static class LongOperator implements Operations {
        @Override
        public TypedObject add(TypedObject first, TypedObject second) {
            return asTypedObject((Long) first.data + (Long) second.data);
        }

        @Override
        public TypedObject subtract(TypedObject first, TypedObject second) {
            return asTypedObject((Long) first.data - (Long) second.data);
        }

        @Override
        public TypedObject multiply(TypedObject first, TypedObject second) {
            return asTypedObject((Long) first.data * (Long) second.data);
        }

        @Override
        public TypedObject divide(TypedObject first, TypedObject second) {
            return asTypedObject((Long) first.data / (Long) second.data);
        }

        @Override
        public TypedObject modulus(TypedObject first, TypedObject second) {
            return asTypedObject((Long) first.data % (Long) second.data);
        }

        @Override
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
            object.type = TypeSystem.Type.LONG;
            return object;
        }
    }
    public static class DoubleOperator implements Operations {
        @Override
        public TypedObject add(TypedObject first, TypedObject second) {
            return asTypedObject((Double) first.data + (Double) second.data);
        }

        @Override
        public TypedObject subtract(TypedObject first, TypedObject second) {
            return asTypedObject((Double) first.data - (Double) second.data);
        }

        @Override
        public TypedObject multiply(TypedObject first, TypedObject second) {
            return asTypedObject((Double) first.data * (Double) second.data);
        }

        @Override
        public TypedObject divide(TypedObject first, TypedObject second) {
            return asTypedObject((Double) first.data / (Double) second.data);
        }

        @Override
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
            object.type = TypeSystem.Type.DOUBLE;
            return object;
        }
    }

    public static class StringOperator implements Operations {
        @Override
        public TypedObject add(TypedObject first, TypedObject second) {
            return asTypedObject((String) first.data + (String) second.data);
        }

        @Override
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
            object.type = TypeSystem.Type.STRING;
            return object;
        }
    }

    public static class DecimalOperator implements Operations {
        @Override
        public TypedObject add(TypedObject first, TypedObject second) {
            return asTypedObject(((BigDecimal) first.data).add((BigDecimal) second.data));
        }

        @Override
        public TypedObject subtract(TypedObject first, TypedObject second) {
            return asTypedObject(((BigDecimal) first.data).subtract((BigDecimal) second.data));
        }

        @Override
        public TypedObject multiply(TypedObject first, TypedObject second) {
            return asTypedObject(((BigDecimal) first.data).multiply((BigDecimal) second.data));
        }

        @Override
        public TypedObject divide(TypedObject first, TypedObject second) {
            return asTypedObject(((BigDecimal) first.data).divide((BigDecimal) second.data));
        }

        @Override
        public TypedObject modulus(TypedObject first, TypedObject second) {
            return asTypedObject(((BigDecimal) first.data).divideAndRemainder((BigDecimal) second.data)[1]);
        }

        @Override
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
            object.type = TypeSystem.Type.DECIMAL;
            return object;
        }
    }

    public static class TimestampOperator implements Operations {
        @Override
        public TypedObject add(TypedObject first, TypedObject second) {
            return asTypedObject(new Timestamp(((Timestamp) first.data).getTime() + ((Timestamp) second.data).getTime()));
        }

        @Override
        public TypedObject subtract(TypedObject first, TypedObject second) {
            return asTypedObject(new Timestamp(((Timestamp) first.data).getTime() - ((Timestamp) second.data).getTime()));
        }

        @Override
        public TypedObject multiply(TypedObject first, TypedObject second) {
            return asTypedObject(new Timestamp(((Timestamp) first.data).getTime() * ((Timestamp) second.data).getTime()));
        }

        @Override
        public TypedObject divide(TypedObject first, TypedObject second) {
            return asTypedObject(new Timestamp(((Timestamp) first.data).getTime() / ((Timestamp) second.data).getTime()));
        }

        @Override
        public TypedObject modulus(TypedObject first, TypedObject second) {
            return asTypedObject(new Timestamp(((Timestamp) first.data).getTime() % ((Timestamp) second.data).getTime()));
        }

        @Override
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
            object.type = TypeSystem.Type.TIMESTAMP;
            return object;
        }
    }
}
