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
//import java.math.BigDecimal;
//import java.sql.Timestamp;

/**
 * This is a class that wraps the supported types that the assertor will work with
 * when operating on data returned by an data source.
 */
public class TypeSystem {
    /** These are the official types we support. They correspond to Java types. */
    public enum Type {
        CHARACTER, LONG, DOUBLE, DECIMAL, BOOLEAN, STRING, TIMESTAMP
    }

    private interface TypeConvertor {
        public boolean convert(TypedObject source);
    }

    /*
     * In general, we don't want lossy casting, or strange casting like a boolean to a short etc.
     * We'll follow the basic Java widening primitive rules.
     * Exceptions:
     */
    public static final Map<Type, TypeConvertor> CONVERTORS = new HashMap<>();
    static {
        CONVERTORS.put(Type.CHARACTER, new TypeConvertor() {
            public boolean convert(TypedObject source) {
                switch (source.type) {
                    case STRING: {
                        String input = (String) source.data;
                        if (input.length() == 1) {
                            source.data = (Character) input.charAt(0);
                            return true;
                        }
                        return false;
                    }
                    case CHARACTER:
                        return true;
                    case LONG:
                        source.data = (long) ((Character) source.data).charValue();
                        return true;
                    case DOUBLE:
                        source.data = (long) ((Character) source.data).charValue();
                    case DECIMAL:
                    case BOOLEAN:
                    case TIMESTAMP:
                    default:
                        return false;
                }
            }
        });
        CONVERTORS.put(Type.LONG, new TypeConvertor() {
            public boolean convert(TypedObject source) {
                switch (source.type) {
                    case STRING:
                        source.data = Long.valueOf((String) source.data);
                    case CHARACTER:
                    case LONG:
                    case DOUBLE:
                    case DECIMAL:
                    case BOOLEAN:
                    case TIMESTAMP:
                    default:
                        return false;
                }
            }
        });
        CONVERTORS.put(Type.DOUBLE, new TypeConvertor() {
            public boolean convert(TypedObject source) {
                switch (source.type) {
                    case STRING:
                    case CHARACTER:
                    case LONG:
                    case DOUBLE:
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
                switch (source.type) {
                    case STRING:
                    case CHARACTER:
                    case LONG:
                    case DOUBLE:
                    case DECIMAL:
                    case BOOLEAN:
                    case TIMESTAMP:
                    default:
                        return false;
                }
            }
        });
        CONVERTORS.put(Type.BOOLEAN, new TypeConvertor() {
            public boolean convert(TypedObject source) {
                switch (source.type) {
                    case STRING:
                    case CHARACTER:
                    case LONG:
                    case DOUBLE:
                    case DECIMAL:
                    case BOOLEAN:
                    case TIMESTAMP:
                    default:
                        return false;
                }
            }
        });
        CONVERTORS.put(Type.STRING, new TypeConvertor() {
            public boolean convert(TypedObject source) {
                switch (source.type) {
                    case STRING:
                    case CHARACTER:
                    case LONG:
                    case DOUBLE:
                    case DECIMAL:
                    case BOOLEAN:
                    case TIMESTAMP:
                    default:
                        return false;
                }
            }
        });
        CONVERTORS.put(Type.TIMESTAMP, new TypeConvertor() {
            public boolean convert(TypedObject source) {
                switch (source.type) {
                    case STRING:
                    case CHARACTER:
                    case LONG:
                    case DOUBLE:
                    case DECIMAL:
                    case BOOLEAN:
                    case TIMESTAMP:
                    default:
                        return false;
                }
            }
        });
    }

    /**
     * Compare two TypedObjects.
     * @param first The first object to compare.
     * @param second The second object to compare.
     * @return -1 if first is less than second, 1 if first is greater than second and 0 if first equals second.
     */
    public int compare(TypedObject first, TypedObject second) {
        if (first == null || second == null) {
            throw new ClassCastException("Tried to compare null arguments. Argument 1: " +
                    first + " Argument 2: " + second);
        }
        return 0;
    }

    /**
     * Helper method that returns true iff first equals second.
     */
    public boolean isEqualTo(TypedObject first, TypedObject second) {
        return compare(first, second) == 0;
    }

    /**
     * Helper method that returns true iff first is less than second.
     */
    public boolean isLessThan(TypedObject first, TypedObject second) {
        return compare(first, second) == -1;
    }

    /**
     * Helper method that returns true iff first is less than or equal to second.
     */
    public boolean isLessThanOrEqual(TypedObject first, TypedObject second) {
        int compared = compare(first, second);
        return compared == -1 || compared == 0;
    }

    /**
     * Helper method that returns true iff first is greater than second.
     */
    public boolean isGreaterThan(TypedObject first, TypedObject second) {
        return compare(first, second) == 1;
    }

    /**
     * Helper method that returns true iff first is greater than or equal to second.
     */
    public boolean isGreaterThanOrEqual(TypedObject first, TypedObject second) {
        int compared = compare(first, second);
        return compared == 1 || compared == 0;
    }

}
/*
   private interface TypeComparator {
   public int compare(String first, String second);
   }

// Java 8 should make this redundancy go away once we can assign valueOf as a reference
public static final Map<Type, TypeComparator> COMPARATORS = new HashMap<>();
static {
COMPARATORS.put(Type.BYTE, new TypeComparator() {
public int compare(String first, String second) {
return Byte.valueOf(first).compareTo(Byte.valueOf(second));
}
});
COMPARATORS.put(Type.SHORT, new TypeComparator() { public int compare(String first, String second) {
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
*/
