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

package com.yahoo.validatar.assertion;

import com.yahoo.validatar.common.Result;
import com.yahoo.validatar.common.TypeSystem;
import com.yahoo.validatar.common.TypedObject;

import java.util.Map;
import java.util.HashMap;
import java.util.List;
import java.util.ArrayList;
import java.math.BigDecimal;
import java.sql.Timestamp;

import org.testng.Assert;
import org.testng.annotations.Test;
import org.testng.annotations.BeforeMethod;
import org.testng.annotations.AfterMethod;

public class AssertorTest {
    private Result results;

    private List<com.yahoo.validatar.common.Test> wrap(com.yahoo.validatar.common.Test... tests) {
        List<com.yahoo.validatar.common.Test> asList = new ArrayList<com.yahoo.validatar.common.Test>();
        for (com.yahoo.validatar.common.Test test : tests) {
            asList.add(test);
        }
        return asList;
    }

    @BeforeMethod
    public void setup() {
        results = new Result();
    }

    private void addToResult(String name, TypedObject value) {
        results.addColumnRow(name, value);
    }

    private void addToResult(String name, TypeSystem.Type type, Object value) {
        switch(type) {
            case STRING:
                addToResult(name, new TypedObject((String) value, TypeSystem.Type.STRING));
                break;
            case CHARACTER:
                addToResult(name, new TypedObject((Character) value, TypeSystem.Type.CHARACTER));
                break;
            case LONG:
                addToResult(name, new TypedObject((Long) value, TypeSystem.Type.LONG));
                break;
            case DOUBLE:
                addToResult(name, new TypedObject((Double) value, TypeSystem.Type.DOUBLE));
                break;
            case DECIMAL:
                addToResult(name, new TypedObject((BigDecimal) value, TypeSystem.Type.DECIMAL));
                break;
            case TIMESTAMP:
                addToResult(name, new TypedObject((Timestamp) value, TypeSystem.Type.TIMESTAMP));
                break;
            case BOOLEAN:
                addToResult(name, new TypedObject((Boolean) value, TypeSystem.Type.BOOLEAN));
                break;
        }
    }

    private void addToResult(String name, TypeSystem.Type type, Object... values) {
        for (Object value : values) {
            addToResult(name, type, value);
        }
    }

    @Test
    public void testNoLookupAssertion() {
        com.yahoo.validatar.common.Test test = new com.yahoo.validatar.common.Test();
        test.asserts = new ArrayList<String>();
        test.asserts.add("100 > 1000");

        Assertor.assertAll(results, wrap(test));
        Assert.assertTrue(test.failed());
        Assert.assertEquals("100 > 1000 was false for these values {}", test.getMessages().get(0));
    }

    @Test
    public void testOneValidNumericAssertion() {
        addToResult("AV.pv_count", TypeSystem.Type.LONG, 104255L);
        addToResult("AV.li_count", TypeSystem.Type.LONG, 155L);

        com.yahoo.validatar.common.Test test = new com.yahoo.validatar.common.Test();
        test.asserts = new ArrayList<String>();
        test.asserts.add("AV.pv_count > 1000");

        Assertor.assertAll(results, wrap(test));
        Assert.assertFalse(test.failed());
    }

    @Test
    public void testOneValidTextAssertion() {
        addToResult("largest_spaceid", TypeSystem.Type.STRING, "104255");

        com.yahoo.validatar.common.Test test = new com.yahoo.validatar.common.Test();
        test.asserts = new ArrayList<String>();
        test.asserts.add("largest_spaceid == \"104255\"");
        Assertor.assertAll(results, wrap(test));
        Assert.assertFalse(test.failed());
    }

    @Test
    public void testWhitespaceStringAssertion() {
        addToResult("largest_spaceid", TypeSystem.Type.STRING, "  104255");

        com.yahoo.validatar.common.Test test = new com.yahoo.validatar.common.Test();
        test.asserts = new ArrayList<String>();
        test.asserts.add("largest_spaceid == \"  104255\"");
        Assertor.assertAll(results, wrap(test));
        Assert.assertFalse(test.failed());
    }

    @Test
    public void testWhitespaceAssertion() {
        addToResult("largest_spaceid", TypeSystem.Type.STRING, "104255");

        com.yahoo.validatar.common.Test test = new com.yahoo.validatar.common.Test();
        test.asserts = new ArrayList<String>();
        test.asserts.add("\tlargest_spaceid                == \"104255\"      ");
        Assertor.assertAll(results, wrap(test));
        Assert.assertFalse(test.failed());
    }

    @Test
    public void testOneNonValidNumericAssertion() {
        addToResult("pv_count", TypeSystem.Type.LONG, 104255L);
        addToResult("li_count", TypeSystem.Type.LONG, 155L);

        com.yahoo.validatar.common.Test test = new com.yahoo.validatar.common.Test();
        test.asserts = new ArrayList<String>();
        test.asserts.add("pv_count > 1000 && li_count < 100");
        Assertor.assertAll(results, wrap(test));

        Assert.assertTrue(test.failed());
        Assert.assertEquals("pv_count > 1000 && li_count < 100 was false for these values {pv_count=104255, li_count=155}", test.getMessages().get(0));
    }

    @Test
    public void testOneNegationAssertion() {
        addToResult("pv_count", TypeSystem.Type.LONG, 104255L);

        com.yahoo.validatar.common.Test test = new com.yahoo.validatar.common.Test();
        test.asserts = new ArrayList<String>();
        test.asserts.add("pv_count > -1000");
        Assertor.assertAll(results, wrap(test));
        Assert.assertFalse(test.failed());
    }

    @Test
    public void testOneNegationLogicalAndParanthesizedAssertion() {
        addToResult("pv_count", TypeSystem.Type.LONG, 104255L);

        com.yahoo.validatar.common.Test test = new com.yahoo.validatar.common.Test();
        test.asserts = new ArrayList<String>();
        test.asserts.add("!(pv_count < 1000)");

        Assertor.assertAll(results, wrap(test));
        Assert.assertFalse(test.failed());
    }

    @Test
    public void testRelationalEqualityAssertion() {
        addToResult("pv_count", TypeSystem.Type.LONG, 104255L);

        com.yahoo.validatar.common.Test test = new com.yahoo.validatar.common.Test();
        test.asserts = new ArrayList<String>();
        test.asserts.add("pv_count >= 104255");
        test.asserts.add("pv_count <= 104255");

        Assertor.assertAll(results, wrap(test));
        Assert.assertFalse(test.failed());
    }

    @Test
    public void testMultiplicativeAssertion() {
        addToResult("pv_count", TypeSystem.Type.LONG, 104255L);

        com.yahoo.validatar.common.Test test = new com.yahoo.validatar.common.Test();
        test.asserts = new ArrayList<String>();
        test.asserts.add("pv_count < 104255*2");
        test.asserts.add("pv_count > 104255/2");

        Assertor.assertAll(results, wrap(test));
        Assert.assertFalse(test.failed());
    }

    @Test
    public void testAdditiveAssertion() {
        addToResult("pv_count", TypeSystem.Type.LONG, 104255L);

        com.yahoo.validatar.common.Test test = new com.yahoo.validatar.common.Test();
        test.asserts = new ArrayList<String>();
        test.asserts.add("pv_count < 104255+1");
        test.asserts.add("pv_count > 104255-1");

        Assertor.assertAll(results, wrap(test));
        Assert.assertFalse(test.failed());
    }

    @Test
    public void testEqualityAssertion() {
        addToResult("pv_count", TypeSystem.Type.LONG, 104255L);

        com.yahoo.validatar.common.Test test = new com.yahoo.validatar.common.Test();
        test.asserts = new ArrayList<String>();
        test.asserts.add("pv_count == 104255");
        test.asserts.add("pv_count != 104255-1");

        Assertor.assertAll(results, wrap(test));
        Assert.assertFalse(test.failed());
    }

    @Test
    public void testLogicalAssertion() {
        addToResult("pv_count", TypeSystem.Type.LONG, 104255L);
        addToResult("li_count", TypeSystem.Type.LONG, 155L);

        com.yahoo.validatar.common.Test test = new com.yahoo.validatar.common.Test();
        test.asserts = new ArrayList<String>();
        test.asserts.add("pv_count == 104255 && pv_count > li_count || pv_count == 5");
        test.asserts.add("pv_count != 104255 && pv_count > li_count || li_count == 155");

        Assertor.assertAll(results, wrap(test));

        Assert.assertFalse(test.failed());
    }

    @Test
    public void testApproxAssertion() {
        addToResult("pv_count", TypeSystem.Type.LONG, 104255L);
        addToResult("foo.pv_count", TypeSystem.Type.LONG, 0L);

        com.yahoo.validatar.common.Test test = new com.yahoo.validatar.common.Test();
        test.asserts = new ArrayList<String>();
        test.asserts.add("approx(pv_count, 100000, 0.05)");
        Assertor.assertAll(results, wrap(test));
        Assert.assertFalse(test.failed());

        test = new com.yahoo.validatar.common.Test();
        test.asserts = new ArrayList<String>();
        test.asserts.add("approx(pv_count, 100000, 0.01)");
        Assertor.assertAll(results, wrap(test));
        Assert.assertTrue(test.failed());

        test = new com.yahoo.validatar.common.Test();
        test.asserts = new ArrayList<String>();
        test.asserts.add("approx(pv_count, 100000, 11241)");
        Assertor.assertAll(results, wrap(test));
        Assert.assertTrue(test.failed());

        test = new com.yahoo.validatar.common.Test();
        test.asserts = new ArrayList<String>();
        test.asserts.add("approx(pv_count, pv_count, 0.01)");
        Assertor.assertAll(results, wrap(test));
        Assert.assertFalse(test.failed());

        test = new com.yahoo.validatar.common.Test();
        test.asserts = new ArrayList<String>();
        test.asserts.add("approx(10000, 10010, 0.01)");
        Assertor.assertAll(results, wrap(test));
        Assert.assertFalse(test.failed());

        test = new com.yahoo.validatar.common.Test();
        test.asserts = new ArrayList<String>();
        test.asserts.add("approx(pv_count, foo.pv_count, 0.10)");
        Assertor.assertAll(results, wrap(test));
        Assert.assertTrue(test.failed());
    }

    @Test
    public void testComplexAssertion() {
        addToResult("pv_count", TypeSystem.Type.LONG, 104255L);
        addToResult("li_count", TypeSystem.Type.LONG, 155L);

        com.yahoo.validatar.common.Test test = new com.yahoo.validatar.common.Test();
        test.asserts = new ArrayList<String>();
        test.asserts.add("((pv_count == 104255 && pv_count < li_count) || (li_count*10000 > pv_count))");

        Assertor.assertAll(results, wrap(test));

        Assert.assertFalse(test.failed());
    }
}

