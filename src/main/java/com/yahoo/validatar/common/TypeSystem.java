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
import java.math.BigDecimal;
import java.sql.Timestamp;

/**
 * This is a class that wraps the supported types that the assertor will work with
 * when operating on data returned by an data source.
 */
public class TypeSystem {
    public enum Type {
        BYTE, SHORT, INTEGER, LONG, FLOAT, DOUBLE, DECIMAL, BOOLEAN, STRING, TIMESTAMP
    }

    private interface TypeComparator {
        public int compare(String first, String second);
    }

    // Java 8 should make this redundancy go away once we can assign valueOf as a reference
    public static final Map<Type, TypeComparator> COMPARATORS = new HashMap<Type, TypeComparator>();
    static {
        COMPARATORS.put(Type.BYTE, new TypeComparator() {
            public int compare(String first, String second) {
                return Byte.valueOf(first).compareTo(Byte.valueOf(second));
            }
        });
        COMPARATORS.put(Type.SHORT, new TypeComparator() {
            public int compare(String first, String second) {
                return Short.valueOf(first).compareTo(Short.valueOf(second));
            }
        });
        COMPARATORS.put(Type.INTEGER, new TypeComparator() {
            public int compare(String first, String second) {
                return Integer.valueOf(first).compareTo(Integer.valueOf(second));
            }
        });
        COMPARATORS.put(Type.LONG, new TypeComparator() {
            public int compare(String first, String second) {
                return Long.valueOf(first).compareTo(Long.valueOf(second));
            }
        });
        COMPARATORS.put(Type.FLOAT, new TypeComparator() {
            public int compare(String first, String second) {
                return Float.valueOf(first).compareTo(Float.valueOf(second));
            }
        });
        COMPARATORS.put(Type.DOUBLE, new TypeComparator() {
            public int compare(String first, String second) {
                return Double.valueOf(first).compareTo(Double.valueOf(second));
            }
        });
        COMPARATORS.put(Type.DECIMAL, new TypeComparator() {
            public int compare(String first, String second) {
                return new BigDecimal(first).compareTo(new BigDecimal(second));
            }
        });
        COMPARATORS.put(Type.BOOLEAN, new TypeComparator() {
            public int compare(String first, String second) {
                return Boolean.valueOf(first).compareTo(Boolean.valueOf(second));
            }
        });
        COMPARATORS.put(Type.STRING, new TypeComparator() {
            public int compare(String first, String second) {
                return first.compareTo(second);
            }
        });
        COMPARATORS.put(Type.TIMESTAMP, new TypeComparator() {
            public int compare(String first, String second) {
                return Timestamp.valueOf(first).compareTo(Timestamp.valueOf(second));
            }
        });
        COMPARATORS.put(null, new TypeComparator() {
            public int compare(String first, String second) {
                throw new RuntimeException("Unknown type provided for " + first + " and " + second);
            }
        });
    }

    /**
     * Compare two strings of the provided type.
     * @param first The first string to compare.
     * @param second The second string to compare.
     * @param type The Type of both first and second.
     * @return -1 if first is less than second, 1 if first is greater than second and 0 if first equals second.
     */
    public int compare(String first, String second, Type type) throws ClassCastException {
        if (first == null || second == null) {
            throw new ClassCastException("Tried to compare null arguments. Argument 1: " +
                                         first + " Argument 2: " + second);
        }
        return COMPARATORS.get(type).compare(first, second);
    }
}
