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

public class AssertorTest {
    private Result results;
    private Assertor assertor = new Assertor();

    private <T> List<T> wrap(T... data) {
        List<T> asList = new ArrayList<>();
        Collections.addAll(asList, data);
        return asList;
    }

    private TypedObject getTyped(TypeSystem.Type type, Object value) {
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

    @BeforeMethod
    public void setup() {
        results = new Result();
    }

    private void addToResult(String name, TypedObject value) {
        results.addColumnRow(name, value);
    }

    private void addToResult(String name, TypeSystem.Type type, Object value) {
        addToResult(name, getTyped(type, value));
    }

    private void addToResult(String name, TypeSystem.Type type, Object... values) {
        for (Object value : values) {
            addToResult(name, type, value);
        }
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
        Assert.assertEquals("100 > 1000 was false for these values {}", test.getMessages().get(0));
    }

    @Test
    public void testOneValidNumericAssertion() {
        addToResult("AV.pv_count", TypeSystem.Type.LONG, 104255L);
        addToResult("AV.li_count", TypeSystem.Type.LONG, 155L);

        com.yahoo.validatar.common.Test test = new com.yahoo.validatar.common.Test();
        test.asserts = new ArrayList<>();
        test.asserts.add("AV.pv_count > 1000");

        Assertor.assertAll(wrap(results), wrap(test));
        Assert.assertFalse(test.failed());
    }

    @Test
    public void testOneValidTextAssertion() {
        addToResult("largest_id", TypeSystem.Type.STRING, "104255");

        com.yahoo.validatar.common.Test test = new com.yahoo.validatar.common.Test();
        test.asserts = new ArrayList<>();
        test.asserts.add("largest_id == \"104255\"");
        Assertor.assertAll(wrap(results), wrap(test));
        Assert.assertFalse(test.failed());
    }

    @Test
    public void testWhitespaceStringAssertion() {
        addToResult("largest_id", TypeSystem.Type.STRING, "  104255");

        com.yahoo.validatar.common.Test test = new com.yahoo.validatar.common.Test();
        test.asserts = new ArrayList<>();
        test.asserts.add("largest_id == \"  104255\"");
        Assertor.assertAll(wrap(results), wrap(test));
        Assert.assertFalse(test.failed());
    }

    @Test
    public void testWhitespaceAssertion() {
        addToResult("largest_id", TypeSystem.Type.STRING, "104255");

        com.yahoo.validatar.common.Test test = new com.yahoo.validatar.common.Test();
        test.asserts = new ArrayList<>();
        test.asserts.add("\tlargest_id                == \"104255\"      ");
        Assertor.assertAll(wrap(results), wrap(test));
        Assert.assertFalse(test.failed());
    }

    @Test
    public void testOneNonValidNumericAssertion() {
        addToResult("pv_count", TypeSystem.Type.LONG, 104255L);
        addToResult("li_count", TypeSystem.Type.LONG, 155L);

        com.yahoo.validatar.common.Test test = new com.yahoo.validatar.common.Test();
        test.asserts = new ArrayList<>();
        test.asserts.add("pv_count > 1000 && li_count < 100");
        Assertor.assertAll(wrap(results), wrap(test));

        Assert.assertTrue(test.failed());
        Assert.assertEquals(test.getMessages().get(0),
                            "pv_count > 1000 && li_count < 100 was false for these values " +
                            "{pv_count=<Type: LONG, Value: 104255>, li_count=<Type: LONG, Value: 155>}");
    }

    @Test
    public void testOneNegationAssertion() {
        addToResult("pv_count", TypeSystem.Type.LONG, 104255L);

        com.yahoo.validatar.common.Test test = new com.yahoo.validatar.common.Test();
        test.asserts = new ArrayList<>();
        test.asserts.add("pv_count > -1000");
        Assertor.assertAll(wrap(results), wrap(test));
        Assert.assertFalse(test.failed());
    }

    @Test
    public void testOneNegationLogicalAndParanthesizedAssertion() {
        addToResult("pv_count", TypeSystem.Type.LONG, 104255L);

        com.yahoo.validatar.common.Test test = new com.yahoo.validatar.common.Test();
        test.asserts = new ArrayList<>();
        test.asserts.add("!(pv_count < 1000)");

        Assertor.assertAll(wrap(results), wrap(test));
        Assert.assertFalse(test.failed());
    }

    @Test
    public void testRelationalEqualityAssertion() {
        addToResult("pv_count", TypeSystem.Type.LONG, 104255L);

        com.yahoo.validatar.common.Test test = new com.yahoo.validatar.common.Test();
        test.asserts = new ArrayList<>();
        test.asserts.add("pv_count >= 104255");
        test.asserts.add("pv_count <= 104255");

        Assertor.assertAll(wrap(results), wrap(test));
        Assert.assertFalse(test.failed());
    }

    @Test
    public void testMultiplicativeAssertion() {
        addToResult("pv_count", TypeSystem.Type.LONG, 104255L);

        com.yahoo.validatar.common.Test test = new com.yahoo.validatar.common.Test();
        test.asserts = new ArrayList<>();
        test.asserts.add("pv_count < 104255*2");
        test.asserts.add("pv_count > 104255/2");

        Assertor.assertAll(wrap(results), wrap(test));
        Assert.assertFalse(test.failed());
    }

    @Test
    public void testAdditiveAssertion() {
        addToResult("pv_count", TypeSystem.Type.LONG, 104255L);

        com.yahoo.validatar.common.Test test = new com.yahoo.validatar.common.Test();
        test.asserts = new ArrayList<>();
        test.asserts.add("pv_count < 104255+1");
        test.asserts.add("pv_count > 104255-1");

        Assertor.assertAll(wrap(results), wrap(test));
        Assert.assertFalse(test.failed());
    }

    @Test
    public void testEqualityAssertion() {
        addToResult("pv_count", TypeSystem.Type.LONG, 104255L);

        com.yahoo.validatar.common.Test test = new com.yahoo.validatar.common.Test();
        test.asserts = new ArrayList<>();
        test.asserts.add("pv_count == 104255");
        test.asserts.add("pv_count != 104255-1");

        Assertor.assertAll(wrap(results), wrap(test));
        Assert.assertFalse(test.failed());
    }

    @Test
    public void testLogicalAssertion() {
        addToResult("pv_count", TypeSystem.Type.LONG, 104255L);
        addToResult("li_count", TypeSystem.Type.LONG, 155L);

        com.yahoo.validatar.common.Test test = new com.yahoo.validatar.common.Test();
        test.asserts = new ArrayList<>();
        test.asserts.add("pv_count == 104255 && pv_count > li_count || pv_count == 5");
        test.asserts.add("pv_count != 104255 && pv_count > li_count || li_count == 155");

        Assertor.assertAll(wrap(results), wrap(test));

        Assert.assertFalse(test.failed());
    }

    @Test
    public void testApproxAssertion() {
        addToResult("pv_count", TypeSystem.Type.LONG, 104255L);
        addToResult("foo.pv_count", TypeSystem.Type.LONG, 0L);

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
        addToResult("pv_count", TypeSystem.Type.LONG, 104255L);
        addToResult("li_count", TypeSystem.Type.LONG, 155L);

        com.yahoo.validatar.common.Test test = new com.yahoo.validatar.common.Test();
        test.asserts = new ArrayList<>();
        test.asserts.add("((pv_count == 104255 && pv_count < li_count) || (li_count*10000 > pv_count))");
        Assertor.assertAll(wrap(results), wrap(test));
        Assert.assertFalse(test.failed());
    }

    @Test
    public void testUnicodeStringAssertion() {
        addToResult("str", TypeSystem.Type.STRING, "\u0001");

        com.yahoo.validatar.common.Test test = new com.yahoo.validatar.common.Test();
        test.asserts = new ArrayList<>();
        test.asserts.add("str == \"\\u0001\"");
        Assertor.assertAll(wrap(results), wrap(test));
        Assert.assertFalse(test.failed());
    }

    @Test
    public void testSingleQuoteStringAssertion() {
        addToResult("str", TypeSystem.Type.STRING, "Foo's Bar");

        com.yahoo.validatar.common.Test test = new com.yahoo.validatar.common.Test();
        test.asserts = new ArrayList<>();
        test.asserts.add("str == \"Foo's Bar\"");
        Assertor.assertAll(wrap(results), wrap(test));
        Assert.assertFalse(test.failed());
    }

    @Test
    public void testBooleanAssertion() {
        addToResult("bool", TypeSystem.Type.BOOLEAN, true);

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
        addToResult("counts", TypeSystem.Type.LONG, 29L);

        com.yahoo.validatar.common.Test test = new com.yahoo.validatar.common.Test();
        test.asserts = new ArrayList<>();
        test.asserts.add("counts % 19 == 10");

        Assertor.assertAll(wrap(results), wrap(test));
        Assert.assertFalse(test.failed());
    }
}

