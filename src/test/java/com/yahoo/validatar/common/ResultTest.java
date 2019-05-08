/*
 * Copyright 2015 Yahoo Inc.
 * Licensed under the terms of the Apache 2 license. Please see LICENSE file in the project root for terms.
 */
package com.yahoo.validatar.common;

import com.yahoo.validatar.common.TypeSystem.Type;
import org.testng.Assert;
import org.testng.annotations.Test;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

import static com.yahoo.validatar.TestHelpers.asColumn;
import static com.yahoo.validatar.TestHelpers.isEqual;
import static java.util.Arrays.asList;
import static java.util.Collections.emptyList;
import static java.util.Collections.singletonList;

public class ResultTest {
    @Test
    public void testPrefix() {
        Result result = new Result("foo");
        result.addColumn("a", singletonList(new TypedObject(1L, Type.LONG)));

        Assert.assertEquals(result.getColumn("a").size(), 1);
        Assert.assertEquals(result.getColumn("a").get(0).data, Long.valueOf(1L));
        Assert.assertEquals(result.getColumns().get("foo.a").get(0).data, Long.valueOf(1L));
    }

    @Test
    public void testAddRow() {
        Result result = new Result();
        result.addColumnRow("a", new TypedObject(2L, Type.LONG));
        Assert.assertEquals(result.getColumn("a").get(0).data, Long.valueOf(2L));
        result.addColumnRow("a", new TypedObject(3L, Type.LONG));
        Assert.assertEquals(result.getColumn("a").get(1).data, Long.valueOf(3L));
    }

    @Test
    public void testAddColumn() {
        Result result = new Result();

        result.addColumn("a");
        Assert.assertTrue(result.getColumn("a").getValues().isEmpty());

        result.addColumnRow("a", new TypedObject(2L, Type.LONG));
        Assert.assertEquals(result.getColumn("a").get(0).data, Long.valueOf(2L));
    }

    @Test
    public void testAddAllData() {
        Result result = new Result();

        result.addColumns(null);
        Assert.assertTrue(result.getColumns().isEmpty());

        Map<String, List<TypedObject>> data = new HashMap<>();
        data.put("a", asList(new TypedObject(4L, Type.LONG),
                new TypedObject(false, Type.BOOLEAN)));
        data.put("c", asList(new TypedObject(1L, Type.LONG)));
        result.addColumns(data);
        Assert.assertEquals(result.getColumn("a").get(0).data, Long.valueOf(4L));
        Assert.assertEquals(result.getColumn("a").get(1).data, Boolean.valueOf(false));
        Assert.assertEquals(result.getColumn("c").get(0).data, Long.valueOf(1L));
        Assert.assertNull(result.getColumn("b"));
    }

    @Test
    public void testMergeNull() {
        Result result = new Result("Bar");
        result.merge(null);
        Assert.assertTrue(result.getColumns().isEmpty());
    }

    @Test
    public void testMerge() {
        Result result = new Result("Bar");
        result.addColumnRow("a", new TypedObject(2L, Type.LONG));
        Assert.assertEquals(result.getColumn("a").get(0).data, Long.valueOf(2L));

        Result anotherResult = new Result("Foo");
        anotherResult.addColumnRow("a", new TypedObject(3L, Type.LONG));
        Assert.assertEquals(anotherResult.getColumn("a").get(0).data, Long.valueOf(3L));

        result.merge(anotherResult);
        // You can't get anotherResult.a anymore
        Assert.assertEquals(result.getColumn("a").get(0).data, Long.valueOf(2L));
        // You have to get the map
        Map<String, Column> results = result.getColumns();
        Assert.assertEquals(results.size(), 2);
        Assert.assertEquals(results.get("Foo.a").size(), 1);
        Assert.assertEquals(results.get("Foo.a").get(0).data, Long.valueOf(3L));
        Assert.assertEquals(results.get("Bar.a").size(), 1);
        Assert.assertEquals(results.get("Bar.a").get(0).data, Long.valueOf(2L));
    }

    @Test
    public void testNumberOfRows() {
        Result result = new Result();
        Assert.assertEquals(result.numberOfRows(), 0);

        result = new Result(emptyList());
        Assert.assertEquals(result.numberOfRows(), 0);

        result = new Result("A");
        Assert.assertEquals(result.numberOfRows(), 0);

        result = new Result(asList("a", "b", "c"));
        Assert.assertEquals(result.numberOfRows(), 0);

        result = new Result("A");
        result.addColumn("a", asColumn(Type.BOOLEAN, false, true));
        result.addColumn("b", asColumn(Type.LONG, 25L, 1L));
        Assert.assertEquals(result.numberOfRows(), 2);
    }

    @Test
    public void testDataAddition() {
        Result result = new Result("A");
        result.addColumn("a", asColumn(Type.LONG, 1L, 4L));
        result.addQualifiedColumn("B.foo", asColumn(Type.BOOLEAN, true, false));

        Assert.assertTrue(result.hasColumn("a"));
        Assert.assertTrue(result.hasQualifiedColumn("A.a"));
        Assert.assertTrue(result.hasQualifiedColumn("B.foo"));

        Assert.assertEquals(result.numberOfRows(), 2);
        Map<String, TypedObject> row = new HashMap<>();
        row.put("A.a", new TypedObject(23L, Type.LONG));
        row.put("B.foo", new TypedObject(true, Type.BOOLEAN));
        result.addQualifiedRow(row);
        Assert.assertEquals(result.numberOfRows(), 3);

        // This result is now no longer a matrix
        result.addColumnRow("a", new TypedObject(42L, Type.LONG));
        result.addColumnRow("foo", new TypedObject(0.42, Type.DECIMAL));
        // New column
        Assert.assertTrue(result.hasQualifiedColumn("A.foo"));
        Assert.assertNotNull(result.toString());

        Map<String, TypedObject> rowTwo = result.getRowSafe(2);
        Assert.assertEquals(rowTwo.get("A.a").data, 23L);
        Assert.assertEquals(rowTwo.get("A.foo").data, "");
        Assert.assertEquals(rowTwo.get("B.foo").data, true);

        Map<String, TypedObject> rowThree = result.getRowSafe(3);
        Assert.assertEquals(rowThree.get("A.a").data, 42L);
        Assert.assertEquals(rowThree.get("A.foo").data, "");
        Assert.assertEquals(rowThree.get("B.foo").data, "");

        Map<String, TypedObject> rowZero = result.getRowSafe(0);
        Assert.assertEquals(rowZero.get("A.a").data, 1L);
        Assert.assertEquals(rowZero.get("A.foo").data, 0.42);
        Assert.assertEquals(rowZero.get("B.foo").data, true);
    }

    @Test
    public void testCartesianProductNoInput() {
        Result result = null;

        result = Result.cartesianProduct(null);
        Assert.assertEquals(result.numberOfRows(), 0);

        result = Result.cartesianProduct(emptyList());
        Assert.assertEquals(result.numberOfRows(), 0);
    }

    @Test
    public void testCartesianProductOneInput() {
        Result result = new Result("A");
        result.addColumn("a", asColumn(Type.LONG, 1L, 4L));
        result.addColumn("b", asColumn(Type.LONG, 4L, 2L));

        Result actual = Result.cartesianProduct(singletonList(result));

        Map<String, TypedObject> row = actual.getRow(0);
        Assert.assertEquals(row.get("A.a").data, 1L);
        Assert.assertEquals(row.get("A.b").data, 4L);

        Assert.assertEquals(result.numberOfRows(), 2);
        Assert.assertEquals(actual.numberOfRows(), 2);

        // It's a copy
        result.addColumnRow("a", new TypedObject(42L, Type.LONG));
        result.addColumnRow("b", new TypedObject(84L, Type.LONG));

        Assert.assertEquals(result.numberOfRows(), 3);
        Assert.assertEquals(actual.numberOfRows(), 2);
    }

    @Test
    public void testCartesianProductTwoInputs() {
        Result first = new Result("A");
        first.addColumn("a", asColumn(Type.LONG, 1L, 4L));
        first.addColumn("b", asColumn(Type.BOOLEAN, true, true));

        Result second = new Result("B");
        second.addColumn("a", asColumn(Type.LONG, 2L, 8L, 84L));
        second.addColumn("c", asColumn(Type.STRING, "foo", "bar", "baz"));

        Result actual = Result.cartesianProduct(asList(first, second));

        Result expected = new Result();
        expected.addColumn("A.a", asColumn(Type.LONG, 1L, 1L, 1L, 4L, 4L, 4L));
        expected.addColumn("A.b", asColumn(Type.BOOLEAN, true, true, true, true, true, true));
        expected.addColumn("B.a", asColumn(Type.LONG, 2L, 8L, 84L, 2L, 8L, 84L));
        expected.addColumn("B.c", asColumn(Type.STRING, "foo", "bar", "baz", "foo", "bar", "baz"));

        Assert.assertTrue(isEqual(actual, expected));
    }

    @Test
    public void testCartesianProductThreeInputs() {
        Result first = new Result("A");
        first.addColumn("a", asColumn(Type.LONG, 1L, 4L));
        first.addColumn("b", asColumn(Type.BOOLEAN, true, false));

        Result second = new Result("B");
        second.addColumn("a", asColumn(Type.LONG, 2L, 8L));
        second.addColumn("c", asColumn(Type.STRING, "foo", "bar"));

        Result third = new Result("C");
        third.addColumn("d", asColumn(Type.DOUBLE, 42.0, 84.0));
        third.addColumn("f", asColumn(Type.STRING, "baz", "qux"));

        Result actual = Result.cartesianProduct(asList(first, second, third));

        Result expected = new Result();
        expected.addColumn("A.a", asColumn(Type.LONG, 1L, 1L, 1L, 1L, 4L, 4L, 4L, 4L));
        expected.addColumn("A.b", asColumn(Type.BOOLEAN, true, true, true, true, false, false, false, false));
        expected.addColumn("B.a", asColumn(Type.LONG, 2L, 2L, 8L, 8L, 2L, 2L, 8L, 8L));
        expected.addColumn("B.c", asColumn(Type.STRING, "foo", "foo", "bar", "bar", "foo", "foo", "bar", "bar"));
        expected.addColumn("C.d", asColumn(Type.DOUBLE, 42.0, 84.0, 42.0, 84.0, 42.0, 84.0, 42.0, 84.0));
        expected.addColumn("C.f", asColumn(Type.STRING, "baz", "qux", "baz", "qux", "baz", "qux", "baz", "qux"));

        Assert.assertTrue(isEqual(actual, expected));
    }

    @Test
    public void testGettingRowsSafely() {
        Result result = new Result("A");
        result.addColumn("a", asColumn(Type.LONG, 1L, 4L, 10L, 20L, 1L));
        result.addColumn("b", asColumn(Type.BOOLEAN, true, null, false));

        Map<String, TypedObject> rowTwo = result.getRowSafe(1);
        Assert.assertEquals(rowTwo.get("A.a").data, 4L);
        Assert.assertEquals(rowTwo.get("A.b").data, Result.NULL);

        Map<String, TypedObject> rowFive = result.getRowSafe(4);
        Assert.assertEquals(rowFive.get("A.a").data, 1L);
        Assert.assertEquals(rowFive.get("A.b").data, Result.EMPTY_RESULT);

        Map<String, TypedObject> rowTen = result.getRowSafe(9);
        Assert.assertEquals(rowTen.get("A.a").data, Result.EMPTY_RESULT);
        Assert.assertEquals(rowTen.get("A.b").data, Result.EMPTY_RESULT);
    }
}
