/*
 * Copyright 2015 Yahoo Inc.
 * Licensed under the terms of the Apache 2 license. Please see LICENSE file in the project root for terms.
 */
package com.yahoo.validatar.assertion;

import com.yahoo.validatar.common.Result;
import com.yahoo.validatar.common.TypeSystem;
import com.yahoo.validatar.common.TypedObject;
import org.testng.Assert;
import org.testng.annotations.BeforeMethod;
import org.testng.annotations.Test;

import java.math.BigDecimal;
import java.sql.Timestamp;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.stream.Collectors;

public class AssertorTest {
    private Result results;
    private Assertor assertor = new Assertor();

    private static <T> List<T> wrap(T... data) {
        List<T> asList = new ArrayList<>();
        Collections.addAll(asList, data);
        return asList;
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

    public static void addColumnToResult(Result result, String name, TypeSystem.Type type, Object... values) {
        result.addColumn(name, wrap(values).stream().map(t -> getTyped(type, t)).collect(Collectors.toList()));
    }

    public static void addRow(Result result, String name, TypedObject value) {
        result.addColumnRow(name, value);
    }

    private void addRow(String name, TypedObject value) {
        addRow(results, name, value);
    }

    private void addRow(String name, TypeSystem.Type type, Object value) {
        addRow(name, getTyped(type, value));
    }

    private void addColumnToResult(String name, TypeSystem.Type type, Object... values) {
        addColumnToResult(results, name, type, values);
    }

    @BeforeMethod
    public void setup() {
        results = new Result();
    }

    @Test
    public void testNoAssertion() {
        com.yahoo.validatar.common.Test test = new com.yahoo.validatar.common.Test();
        test.asserts = null;

        Assertor.assertAll(wrap(results), wrap(test));
        Assert.assertTrue(test.failed());
        Assert.assertEquals("No assertion was provided!", test.getMessages().get(0));

        test.asserts = new ArrayList<>();
        Assertor.assertAll(wrap(results), wrap(test));
        Assert.assertTrue(test.failed());
        Assert.assertEquals("No assertion was provided!", test.getMessages().get(0));
    }

    @Test
    public void testEmptyResultsAssertion() {
        com.yahoo.validatar.common.Test test = new com.yahoo.validatar.common.Test();
        test.asserts = new ArrayList<>();
        test.asserts.add("AV.pv_count > 1000");

        results.addColumn("AV.pv_count");

        Assertor.assertAll(wrap(results), wrap(test));
        Assert.assertTrue(test.failed());
    }

    @Test
    public void testNoLookupAssertion() {
        com.yahoo.validatar.common.Test test = new com.yahoo.validatar.common.Test();
        test.asserts = new ArrayList<>();
        test.asserts.add("100 > 1000");

        Assertor.assertAll(wrap(results), wrap(test));
        Assert.assertTrue(test.failed());
        Assert.assertEquals(test.getMessages().get(0), "Assertion 100 > 1000 was false");
        Assert.assertEquals(test.getMessages().get(1), "Result had false values: [<false, BOOLEAN>]");
        Assert.assertEquals(test.getMessages().get(2), "Examined columns: []");
        Assert.assertNotNull(test.getMessages().get(3));
    }

    @Test
    public void testOneValidNumericAssertion() {
        addRow("AV.pv_count", TypeSystem.Type.LONG, 104255L);
        addRow("AV.li_count", TypeSystem.Type.LONG, 155L);

        com.yahoo.validatar.common.Test test = new com.yahoo.validatar.common.Test();
        test.asserts = new ArrayList<>();
        test.asserts.add("AV.pv_count > 1000");

        Assertor.assertAll(wrap(results), wrap(test));
        Assert.assertFalse(test.failed());
    }

    @Test
    public void testOneValidTextAssertion() {
        addRow("largest_id", TypeSystem.Type.STRING, "104255");

        com.yahoo.validatar.common.Test test = new com.yahoo.validatar.common.Test();
        test.asserts = new ArrayList<>();
        test.asserts.add("largest_id == \"104255\"");
        Assertor.assertAll(wrap(results), wrap(test));
        Assert.assertFalse(test.failed());
    }

    @Test
    public void testWhitespaceStringAssertion() {
        addRow("largest_id", TypeSystem.Type.STRING, "  104255");

        com.yahoo.validatar.common.Test test = new com.yahoo.validatar.common.Test();
        test.asserts = new ArrayList<>();
        test.asserts.add("largest_id == \"  104255\"");
        Assertor.assertAll(wrap(results), wrap(test));
        Assert.assertFalse(test.failed());
    }

    @Test
    public void testWhitespaceAssertion() {
        addRow("largest_id", TypeSystem.Type.STRING, "104255");

        com.yahoo.validatar.common.Test test = new com.yahoo.validatar.common.Test();
        test.asserts = new ArrayList<>();
        test.asserts.add("\tlargest_id                == \"104255\"      ");
        Assertor.assertAll(wrap(results), wrap(test));
        Assert.assertFalse(test.failed());
    }

    @Test
    public void testOneNonValidNumericAssertion() {
        addRow("pv_count", TypeSystem.Type.LONG, 104255L);
        addRow("li_count", TypeSystem.Type.LONG, 155L);

        com.yahoo.validatar.common.Test test = new com.yahoo.validatar.common.Test();
        test.asserts = new ArrayList<>();
        test.asserts.add("pv_count > 1000 && li_count < 100");
        Assertor.assertAll(wrap(results), wrap(test));

        Assert.assertTrue(test.failed());
        Assert.assertEquals(test.getMessages().get(0), "Assertion pv_count > 1000 && li_count < 100 was false");
        Assert.assertEquals(test.getMessages().get(1), "Result had false values: [<false, BOOLEAN>]");
        Assert.assertEquals(test.getMessages().get(2), "Examined columns: [pv_count, li_count]");
        String actualDataMessage = test.getMessages().get(3);
        Assert.assertTrue(actualDataMessage.contains("104255"));
        Assert.assertTrue(actualDataMessage.contains("155"));
    }

    @Test
    public void testOneNegationAssertion() {
        addRow("pv_count", TypeSystem.Type.LONG, 104255L);

        com.yahoo.validatar.common.Test test = new com.yahoo.validatar.common.Test();
        test.asserts = new ArrayList<>();
        test.asserts.add("pv_count > -1000");
        Assertor.assertAll(wrap(results), wrap(test));
        Assert.assertFalse(test.failed());
    }

    @Test
    public void testOneNegationLogicalAndParanthesizedAssertion() {
        addRow("pv_count", TypeSystem.Type.LONG, 104255L);

        com.yahoo.validatar.common.Test test = new com.yahoo.validatar.common.Test();
        test.asserts = new ArrayList<>();
        test.asserts.add("!(pv_count < 1000)");

        Assertor.assertAll(wrap(results), wrap(test));
        Assert.assertFalse(test.failed());
    }

    @Test
    public void testRelationalEqualityAssertion() {
        addRow("pv_count", TypeSystem.Type.LONG, 104255L);

        com.yahoo.validatar.common.Test test = new com.yahoo.validatar.common.Test();
        test.asserts = new ArrayList<>();
        test.asserts.add("pv_count >= 104255");
        test.asserts.add("pv_count <= 104255");

        Assertor.assertAll(wrap(results), wrap(test));
        Assert.assertFalse(test.failed());
    }

    @Test
    public void testMultiplicativeAssertion() {
        addRow("pv_count", TypeSystem.Type.LONG, 104255L);

        com.yahoo.validatar.common.Test test = new com.yahoo.validatar.common.Test();
        test.asserts = new ArrayList<>();
        test.asserts.add("pv_count < 104255*2");
        test.asserts.add("pv_count > 104255/2");

        Assertor.assertAll(wrap(results), wrap(test));
        Assert.assertFalse(test.failed());
    }

    @Test
    public void testAdditiveAssertion() {
        addRow("pv_count", TypeSystem.Type.LONG, 104255L);

        com.yahoo.validatar.common.Test test = new com.yahoo.validatar.common.Test();
        test.asserts = new ArrayList<>();
        test.asserts.add("pv_count < 104255+1");
        test.asserts.add("pv_count > 104255-1");

        Assertor.assertAll(wrap(results), wrap(test));
        Assert.assertFalse(test.failed());
    }

    @Test
    public void testEqualityAssertion() {
        addRow("pv_count", TypeSystem.Type.LONG, 104255L);

        com.yahoo.validatar.common.Test test = new com.yahoo.validatar.common.Test();
        test.asserts = new ArrayList<>();
        test.asserts.add("pv_count == 104255");
        test.asserts.add("pv_count != 104255-1");

        Assertor.assertAll(wrap(results), wrap(test));
        Assert.assertFalse(test.failed());
    }

    @Test
    public void testLogicalAssertion() {
        addRow("pv_count", TypeSystem.Type.LONG, 104255L);
        addRow("li_count", TypeSystem.Type.LONG, 155L);

        com.yahoo.validatar.common.Test test = new com.yahoo.validatar.common.Test();
        test.asserts = new ArrayList<>();
        test.asserts.add("pv_count == 104255 && pv_count > li_count || pv_count == 5");
        test.asserts.add("pv_count != 104255 && pv_count > li_count || li_count == 155");

        Assertor.assertAll(wrap(results), wrap(test));

        Assert.assertFalse(test.failed());
    }

    @Test
    public void testApproxAssertion() {
        addRow("pv_count", TypeSystem.Type.LONG, 104255L);
        addRow("foo.pv_count", TypeSystem.Type.LONG, 0L);

        com.yahoo.validatar.common.Test test = new com.yahoo.validatar.common.Test();
        test.asserts = new ArrayList<>();
        test.asserts.add("approx(pv_count, 100000, 0.05)");
        Assertor.assertAll(wrap(results), wrap(test));
        Assert.assertFalse(test.failed());

        test = new com.yahoo.validatar.common.Test();
        test.asserts = new ArrayList<>();
        test.asserts.add("approx(pv_count, 100000, 0.01)");
        Assertor.assertAll(wrap(results), wrap(test));
        Assert.assertTrue(test.failed());

        test = new com.yahoo.validatar.common.Test();
        test.asserts = new ArrayList<>();
        test.asserts.add("approx(pv_count, 100000, 11241)");
        Assertor.assertAll(wrap(results), wrap(test));
        Assert.assertTrue(test.failed());

        test = new com.yahoo.validatar.common.Test();
        test.asserts = new ArrayList<>();
        test.asserts.add("approx(pv_count, pv_count, 0.01)");
        Assertor.assertAll(wrap(results), wrap(test));
        Assert.assertFalse(test.failed());

        test = new com.yahoo.validatar.common.Test();
        test.asserts = new ArrayList<>();
        test.asserts.add("approx(10000, 10010, 0.01)");
        Assertor.assertAll(wrap(results), wrap(test));
        Assert.assertFalse(test.failed());

        test = new com.yahoo.validatar.common.Test();
        test.asserts = new ArrayList<>();
        test.asserts.add("approx(pv_count, foo.pv_count, 0.10)");
        Assertor.assertAll(wrap(results), wrap(test));
        Assert.assertTrue(test.failed());
    }

    @Test
    public void testComplexAssertion() {
        addRow("pv_count", TypeSystem.Type.LONG, 104255L);
        addRow("li_count", TypeSystem.Type.LONG, 155L);

        com.yahoo.validatar.common.Test test = new com.yahoo.validatar.common.Test();
        test.asserts = new ArrayList<>();
        test.asserts.add("((pv_count == 104255 && pv_count < li_count) || (li_count*10000 > pv_count))");
        Assertor.assertAll(wrap(results), wrap(test));
        Assert.assertFalse(test.failed());
    }

    @Test
    public void testUnicodeStringAssertion() {
        addRow("str", TypeSystem.Type.STRING, "\u0001");

        com.yahoo.validatar.common.Test test = new com.yahoo.validatar.common.Test();
        test.asserts = new ArrayList<>();
        test.asserts.add("str == \"\\u0001\"");
        Assertor.assertAll(wrap(results), wrap(test));
        Assert.assertFalse(test.failed());
    }

    @Test
    public void testSingleQuoteStringAssertion() {
        addRow("str", TypeSystem.Type.STRING, "Foo's Bar");

        com.yahoo.validatar.common.Test test = new com.yahoo.validatar.common.Test();
        test.asserts = new ArrayList<>();
        test.asserts.add("str == \"Foo's Bar\"");
        Assertor.assertAll(wrap(results), wrap(test));
        Assert.assertFalse(test.failed());
    }

    @Test
    public void testDoubleQuoteStringAssertion() {
        addRow("str", TypeSystem.Type.STRING, "Foo said \"Bar\"");

        com.yahoo.validatar.common.Test test = new com.yahoo.validatar.common.Test();
        test.asserts = new ArrayList<>();
        test.asserts.add("str == 'Foo said \"Bar\"'");
        Assertor.assertAll(wrap(results), wrap(test));
        Assert.assertFalse(test.failed());
    }

    @Test
    public void testBooleanAssertion() {
        addRow("bool", TypeSystem.Type.BOOLEAN, true);

        com.yahoo.validatar.common.Test test = new com.yahoo.validatar.common.Test();
        test.asserts = new ArrayList<>();
        test.asserts.add("bool == true");
        test.asserts.add("bool != false");
        test.asserts.add("bool");
        test.asserts.add("!(!bool)");

        Assertor.assertAll(wrap(results), wrap(test));
        Assert.assertFalse(test.failed());
    }

    @Test
    public void testModulusAssertion() {
        addRow("counts", TypeSystem.Type.LONG, 29L);

        com.yahoo.validatar.common.Test test = new com.yahoo.validatar.common.Test();
        test.asserts = new ArrayList<>();
        test.asserts.add("counts % 19 == 10");

        Assertor.assertAll(wrap(results), wrap(test));
        Assert.assertFalse(test.failed());
    }

    @Test
    public void testLargeIntegerAssertion() {
        addRow("count", TypeSystem.Type.DECIMAL, new BigDecimal("123456789123456789123456789"));

        com.yahoo.validatar.common.Test test = new com.yahoo.validatar.common.Test();
        test.asserts = new ArrayList<>();
        test.asserts.add("count == 123456789123456789123456789");
        Assertor.assertAll(wrap(results), wrap(test));
        Assert.assertFalse(test.failed());
    }

    @Test
    public void testLargeDecimalAssertion() {
        addRow("count", TypeSystem.Type.DECIMAL, new BigDecimal("123456789123456789123456789.123456789123456789123456789"));

        String large = BigDecimal.valueOf(Double.MAX_VALUE).multiply(BigDecimal.TEN).toPlainString() + ".42";
        com.yahoo.validatar.common.Test test = new com.yahoo.validatar.common.Test();
        test.asserts = new ArrayList<>();
        test.asserts.add("count < " + large);
        Assertor.assertAll(wrap(results), wrap(test));
        Assert.assertFalse(test.failed());
    }

    @Test
    public void testVectorScalarAssertion() {
        addColumnToResult("count", TypeSystem.Type.LONG, 1L, 2L, 10L, 15L, 0L);

        com.yahoo.validatar.common.Test test = new com.yahoo.validatar.common.Test();
        test.asserts = new ArrayList<>();
        test.asserts.add("count < 100");
        Assertor.assertAll(wrap(results), wrap(test));
        Assert.assertFalse(test.failed());

        test = new com.yahoo.validatar.common.Test();
        test.asserts = new ArrayList<>();
        test.asserts.add("count > 100");
        Assertor.assertAll(wrap(results), wrap(test));
        Assert.assertTrue(test.failed());
    }

    @Test
    public void testVectorAssertion() {
        addColumnToResult("p_count", TypeSystem.Type.LONG, 1L, 2L, 10L, 15L, 0L);
        addColumnToResult("a_count", TypeSystem.Type.LONG, 10L, 20L, 100L, 150L, 0L);

        com.yahoo.validatar.common.Test test = new com.yahoo.validatar.common.Test();
        test.asserts = new ArrayList<>();
        test.asserts.add("p_count <= a_count");
        Assertor.assertAll(wrap(results), wrap(test));
        Assert.assertFalse(test.failed());

        test = new com.yahoo.validatar.common.Test();
        test.asserts = new ArrayList<>();
        test.asserts.add("p_count > a_count");
        Assertor.assertAll(wrap(results), wrap(test));
        Assert.assertTrue(test.failed());

        test = new com.yahoo.validatar.common.Test();
        test.asserts = new ArrayList<>();
        test.asserts.add("10 * p_count == a_count");
        Assertor.assertAll(wrap(results), wrap(test));
        Assert.assertFalse(test.failed());
    }

    @Test
    public void testVectorComplexAssertion() {
        addColumnToResult("id", TypeSystem.Type.STRING, "a", "b", "c", "d", "a");
        addColumnToResult("pv_count", TypeSystem.Type.LONG, 1L, 2L, 10L, 15L, 0L);
        addColumnToResult("li_count", TypeSystem.Type.LONG, 2L, 2L, 10L, 15L, 2042L);

        com.yahoo.validatar.common.Test test = new com.yahoo.validatar.common.Test();
        test.asserts = new ArrayList<>();
        // First and last rows have the LHS of this assert true but RHS false. Other rows have RHS false but LHS true
        // Together, the result should have true for all rows
        test.asserts.add("((id == 'a' && (pv_count*li_count <= 2)) || (li_count/2 < pv_count))");

        Assertor.assertAll(wrap(results), wrap(test));
        Assert.assertFalse(test.failed());
    }

    @Test
    public void testCartesianProductAssertion() {
        Result a = new Result("A");
        addColumnToResult(a, "country", TypeSystem.Type.STRING, "us", "au", "cn", "in", "uk", "fr", "es", "de", "ru");
        addColumnToResult(a, "region", TypeSystem.Type.STRING, "na", "au", "as", "as", "eu", "eu", "ue", "eu", "eu");
        addColumnToResult(a, "id", TypeSystem.Type.LONG, 1L, 2L, 3L, 4L, 5L, 6L, 7L, 8L, 9L);
        addColumnToResult(a, "total", TypeSystem.Type.LONG, 19L, 5L, 9L, 4L, 1200L, 90L, 120L, 9000L, 10000L);

        Result b = new Result("B");
        addColumnToResult(b, "region", TypeSystem.Type.STRING, "na", "as", "au");
        addColumnToResult(b, "count", TypeSystem.Type.LONG, 1L, 2L, -4L);

        com.yahoo.validatar.common.Test test = new com.yahoo.validatar.common.Test();
        test.asserts = new ArrayList<>();
        // This assert is meaningless but serves to show that we get B's columns in the join even though the join
        // expression did not mention B
        test.asserts.add("B.count <= 2 where A.region == 'eu'");

        Assertor.assertAll(wrap(a, b), wrap(test));
        Assert.assertFalse(test.failed());
    }

    @Test
    public void testSimpleJoinAssertion() {
        Result a = new Result("A");
        addColumnToResult(a, "country", TypeSystem.Type.STRING, "us", "au", "cn", "in", "uk", "fr", "es", "de", "ru");
        addColumnToResult(a, "region", TypeSystem.Type.STRING, "na", "au", "as", "as", "eu", "eu", "ue", "eu", "eu");
        addColumnToResult(a, "id", TypeSystem.Type.LONG, 1L, 2L, 3L, 4L, 5L, 6L, 7L, 8L, 9L);
        addColumnToResult(a, "total", TypeSystem.Type.LONG, 19L, 5L, 9L, 4L, 1200L, 90L, 120L, 9000L, 10000L);

        Result b = new Result("B");
        addColumnToResult(b, "region", TypeSystem.Type.STRING, "na", "as", "au");
        addColumnToResult(b, "count", TypeSystem.Type.LONG, 1L, 2L, -4L);

        com.yahoo.validatar.common.Test test = new com.yahoo.validatar.common.Test();
        test.asserts = new ArrayList<>();
        test.asserts.add("A.total >= B.count where A.region == B.region");
        test.asserts.add("A.region != 'eu' where A.region == B.region");

        Assertor.assertAll(wrap(a, b), wrap(test));
        Assert.assertFalse(test.failed());
    }

    @Test
    public void testComplexJoinAssertion() {
        Result a = new Result("A");
        addColumnToResult(a, "country", TypeSystem.Type.STRING, "us", "au", "cn", "in", "uk", "fr", "es", "de", "ru");
        addColumnToResult(a, "region", TypeSystem.Type.STRING, "na", "au", "as", "as", "eu", "eu", "ue", "eu", "eu");
        addColumnToResult(a, "id", TypeSystem.Type.LONG, 1L, 2L, 3L, 4L, 5L, 6L, 7L, 8L, 9L);
        addColumnToResult(a, "total", TypeSystem.Type.LONG, 19L, 5L, 9L, 4L, 1200L, 90L, 120L, 9000L, 10000L);

        Result b = new Result("B");
        addColumnToResult(b, "region", TypeSystem.Type.STRING, "na", "as", "au");
        addColumnToResult(b, "count", TypeSystem.Type.LONG, 1L, 2L, -4L);

        com.yahoo.validatar.common.Test test = new com.yahoo.validatar.common.Test();
        test.asserts = new ArrayList<>();
        test.asserts.add("A.region != 'eu' && B.count + A.total <= 20 where A.region == B.region");
        Assertor.assertAll(wrap(a, b), wrap(test));
        Assert.assertFalse(test.failed());

        test = new com.yahoo.validatar.common.Test();
        test.asserts = new ArrayList<>();
        test.asserts.add("A.region == 'na' && B.count + A.total == 20 where A.country == 'us' && B.region == 'na'");
        Assertor.assertAll(wrap(a, b), wrap(test));
        Assert.assertFalse(test.failed());

        test = new com.yahoo.validatar.common.Test();
        test.asserts = new ArrayList<>();
        test.asserts.add("A.country == 'in' where A.region == 'as' && B.count + A.total == 6");
        test.asserts.add("A.country == 'cn' && A.id == 3 where B.count + A.total == 11");
        Assertor.assertAll(wrap(a, b), wrap(test));
        Assert.assertFalse(test.failed());

        Assertor.assertAll(wrap(a, b), wrap(test));
        Assert.assertFalse(test.failed());
    }

    @Test
    public void testMultipleJoinAssertion() {
        Result a = new Result("A");
        addColumnToResult(a, "country", TypeSystem.Type.STRING, "us", "au", "cn", "in", "uk", "fr", "es", "de", "ru");
        addColumnToResult(a, "region", TypeSystem.Type.STRING, "na", "au", "as", "as", "eu", "eu", "ue", "eu", "eu");
        addColumnToResult(a, "id", TypeSystem.Type.LONG, 1L, 2L, 3L, 4L, 5L, 6L, 7L, 8L, 9L);

        Result b = new Result("B");
        addColumnToResult(b, "region", TypeSystem.Type.STRING, "na", "as", "au");
        addColumnToResult(b, "count", TypeSystem.Type.LONG, 900L, 21042L, 4042L);

        Result c = new Result("C");
        addColumnToResult(c, "key", TypeSystem.Type.STRING, "2", "6", "1");
        addColumnToResult(c, "total", TypeSystem.Type.LONG, 4242L, 20000L, 1000L);

        com.yahoo.validatar.common.Test test = new com.yahoo.validatar.common.Test();
        test.asserts = new ArrayList<>();

        // This will be the join result
        //A.region,           C.key,         B.count,       A.country,        B.region,            A.id,         C.total
        //      na,               1,             900,              us,              na,               1,            1000
        //      au,               2,            4042,              au,              au,               2,            4242
        test.asserts.add("A.region != 'eu' && approx(C.total, B.count, 0.15) where A.region == B.region && A.id == C.key");
        Assertor.assertAll(wrap(a, b, c), wrap(test));
        Assert.assertFalse(test.failed());
    }
}
