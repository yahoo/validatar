/*
 * Copyright 2015 Yahoo Inc.
 * Licensed under the terms of the Apache 2 license. Please see LICENSE file in the project root for terms.
 */
package com.yahoo.validatar;

import com.yahoo.validatar.common.Column;
import com.yahoo.validatar.common.Query;
import com.yahoo.validatar.common.Result;
import com.yahoo.validatar.common.TestSuite;
import com.yahoo.validatar.common.TypeSystem;
import com.yahoo.validatar.common.TypedObject;
import com.yahoo.validatar.parse.yaml.YAML;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.math.BigDecimal;
import java.sql.Timestamp;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Map;

public class TestHelpers {
    public static Query getQueryFrom(String file, String name) throws FileNotFoundException {
        return getTestSuiteFrom(file).queries.stream().filter(q -> name.equals(q.name)).findAny().get();
    }

    public static TestSuite getTestSuiteFrom(String file) throws FileNotFoundException {
        File testFile = new File(TestHelpers.class.getClassLoader().getResource(file).getFile());
        return new YAML().parse(new FileInputStream(testFile));
    }

    public static <T> List<T> wrap(T... data) {
        List<T> asList = new ArrayList<>();
        Collections.addAll(asList, data);
        return asList;
    }

    public static boolean boolify(TypedObject type) {
        return (Boolean) type.data;
    }

    public static boolean boolify(Column data) {
        for (TypedObject object : data) {
            if (!boolify(object)) {
                return false;
            }
        }
        return true;
    }

    public static TypedObject getTyped(TypeSystem.Type type, Object value) {
        switch (type) {
            case STRING:
                return new TypedObject((String) value, TypeSystem.Type.STRING);
            case LONG:
                return new TypedObject((Long) value, TypeSystem.Type.LONG);
            case DOUBLE:
                return new TypedObject((Double) value, TypeSystem.Type.DOUBLE);
            case DECIMAL:
                return new TypedObject((BigDecimal) value, TypeSystem.Type.DECIMAL);
            case TIMESTAMP:
                return new TypedObject((Timestamp) value, TypeSystem.Type.TIMESTAMP);
            case BOOLEAN:
                return new TypedObject((Boolean) value, TypeSystem.Type.BOOLEAN);
            default:
                throw new RuntimeException("Unknown type");
        }
    }

    public static Column asColumn(TypeSystem.Type type, Object... data) {
        Column column = new Column();
        for (Object object : data) {
            column.add(getTyped(type, object));
        }
        return column;
    }

    public static boolean isEqual(Column actual, Column expected) {
        if (actual == null && expected == null) {
            return true;
        }
        if (actual == null ^ expected == null) {
            return false;
        }
        if (actual.size() != expected.size()) {
            return false;
        }
        for (int i = 0; i < expected.size(); ++i) {
            TypedObject expectedItem = expected.get(i);
            TypedObject actualItem = actual.get(i);
            if (expectedItem.type != actualItem.type) {
                return false;
            }
            if (expectedItem.data.compareTo(actualItem.data) != 0) {
                return false;
            }
        }
        return true;
    }

    public static boolean isEqual(Result actual, Result expected) {
        if (actual == null && expected == null) {
            return true;
        }
        if (actual == null ^ expected == null) {
            return false;
        }
        if (!expected.getNamespace().equals(actual.getNamespace())) {
            return false;
        }
        if (expected.numberOfRows() != actual.numberOfRows()) {
            return false;
        }
        Map<String, Column> expectedData = expected.getColumns();
        Map<String, Column> actualData = actual.getColumns();
        if (expectedData.keySet().size() != actualData.keySet().size()) {
            return false;
        }
        return expectedData.entrySet().stream().allMatch(e -> isEqual(actualData.get(e.getKey()), e.getValue()));
    }
}
