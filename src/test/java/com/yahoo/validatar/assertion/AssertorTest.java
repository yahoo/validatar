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

import java.util.Map;
import java.util.HashMap;
import java.util.List;
import java.util.ArrayList;

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

    private void addToResult(String name, List<String> values) {
        addToResult(name, null, values);
    }

    private void addToResult(String name, TypeSystem.Type type, List<String> values) {
        results.addColumn(name, type, values);
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
        String columnA = "AV.pv_count";
        List<String> columnAValues = new ArrayList<String>();
        columnAValues.add("104255");
        addToResult(columnA, columnAValues);

        String columnB = "AV.li_count";
        List<String> columnBValues = new ArrayList<String>();
        columnBValues.add("155");
        addToResult(columnB, columnBValues);

        com.yahoo.validatar.common.Test test = new com.yahoo.validatar.common.Test();
        test.asserts = new ArrayList<String>();
        test.asserts.add("AV.pv_count > 1000");

        Assertor.assertAll(results, wrap(test));
        Assert.assertFalse(test.failed());
    }

    @Test
    public void testOneValidTextAssertion() {
        String columnA = "largest_spaceid";
        List<String> columnAValues = new ArrayList<String>();
        columnAValues.add("104255");
        addToResult(columnA, columnAValues);

        com.yahoo.validatar.common.Test test = new com.yahoo.validatar.common.Test();
        test.asserts = new ArrayList<String>();
        test.asserts.add("largest_spaceid == \"104255\"");
        Assertor.assertAll(results, wrap(test));
        Assert.assertFalse(test.failed());
    }

    @Test
    public void testWhitespaceStringAssertion() {
        String columnA = "largest_spaceid";
        List<String> columnAValues = new ArrayList<String>();
        columnAValues.add("  104255");
        addToResult(columnA, columnAValues);


        com.yahoo.validatar.common.Test test = new com.yahoo.validatar.common.Test();
        test.asserts = new ArrayList<String>();
        test.asserts.add("largest_spaceid == \"  104255\"");
        Assertor.assertAll(results, wrap(test));
        Assert.assertFalse(test.failed());
    }

    @Test
    public void testWhitespaceAssertion() {
        String columnA = "largest_spaceid";
        List<String> columnAValues = new ArrayList<String>();
        columnAValues.add("104255");
        addToResult(columnA, columnAValues);

        com.yahoo.validatar.common.Test test = new com.yahoo.validatar.common.Test();
        test.asserts = new ArrayList<String>();
        test.asserts.add("\tlargest_spaceid                == \"104255\"      ");

        Assertor.assertAll(results, wrap(test));

        Assert.assertFalse(test.failed());
    }

    @Test
    public void testOneNonValidNumericAssertion() {
        String columnA = "pv_count";
        List<String> columnAValues = new ArrayList<String>();
        columnAValues.add("104255");
        addToResult(columnA, columnAValues);

        String columnB = "li_count";
        List<String> columnBValues = new ArrayList<String>();
        columnBValues.add("155");
        addToResult(columnB, columnBValues);

        com.yahoo.validatar.common.Test test = new com.yahoo.validatar.common.Test();
        test.asserts = new ArrayList<String>();
        test.asserts.add("pv_count > 1000 && li_count < 100");
        Assertor.assertAll(results, wrap(test));

        Assert.assertTrue(test.failed());
        Assert.assertEquals("pv_count > 1000 && li_count < 100 was false for these values {pv_count=104255, li_count=155}", test.getMessages().get(0));
    }

    @Test
    public void testOneNegationAssertion() {
        String columnA = "pv_count";
        List<String> columnAValues = new ArrayList<String>();
        columnAValues.add("104255");
        addToResult(columnA, columnAValues);

        com.yahoo.validatar.common.Test test = new com.yahoo.validatar.common.Test();
        test.asserts = new ArrayList<String>();
        test.asserts.add("pv_count > -1000");
        Assertor.assertAll(results, wrap(test));
        Assert.assertFalse(test.failed());
    }

    @Test
    public void testOneNegationLogicalAndParanthesizedAssertion() {
        String columnA = "pv_count";
        List<String> columnAValues = new ArrayList<String>();
        columnAValues.add("104255");
        addToResult(columnA, columnAValues);

        com.yahoo.validatar.common.Test test = new com.yahoo.validatar.common.Test();
        test.asserts = new ArrayList<String>();
        test.asserts.add("!(pv_count < 1000)");

        Assertor.assertAll(results, wrap(test));
        Assert.assertFalse(test.failed());
    }

    @Test
    public void testRelationalEqualityAssertion() {
        String columnA = "pv_count";
        List<String> columnAValues = new ArrayList<String>();
        columnAValues.add("104255");
        addToResult(columnA, columnAValues);

        com.yahoo.validatar.common.Test test = new com.yahoo.validatar.common.Test();
        test.asserts = new ArrayList<String>();
        test.asserts.add("pv_count >= 104255");
        test.asserts.add("pv_count <= 104255");

        Assertor.assertAll(results, wrap(test));
        Assert.assertFalse(test.failed());
    }

    @Test
    public void testMultiplicativeAssertion() {
        String columnA = "pv_count";
        List<String> columnAValues = new ArrayList<String>();
        columnAValues.add("104255");
        addToResult(columnA, columnAValues);

        com.yahoo.validatar.common.Test test = new com.yahoo.validatar.common.Test();
        test.asserts = new ArrayList<String>();
        test.asserts.add("pv_count < 104255*2");
        test.asserts.add("pv_count > 104255/2");

        Assertor.assertAll(results, wrap(test));
        Assert.assertFalse(test.failed());
    }

    @Test
    public void testAdditiveAssertion() {
        String columnA = "pv_count";
        List<String> columnAValues = new ArrayList<String>();
        columnAValues.add("104255");
        addToResult(columnA, columnAValues);

        com.yahoo.validatar.common.Test test = new com.yahoo.validatar.common.Test();
        test.asserts = new ArrayList<String>();
        test.asserts.add("pv_count < 104255+1");
        test.asserts.add("pv_count > 104255-1");

        Assertor.assertAll(results, wrap(test));
        Assert.assertFalse(test.failed());
    }

    @Test
    public void testEqualityAssertion() {
        String columnA = "pv_count";
        List<String> columnAValues = new ArrayList<String>();
        columnAValues.add("104255");
        addToResult(columnA, columnAValues);

        com.yahoo.validatar.common.Test test = new com.yahoo.validatar.common.Test();
        test.asserts = new ArrayList<String>();
        test.asserts.add("pv_count == 104255");
        test.asserts.add("pv_count != 104255-1");

        Assertor.assertAll(results, wrap(test));
        Assert.assertFalse(test.failed());
    }

    @Test
    public void testLogicalAssertion() {
        String columnA = "pv_count";
        List<String> columnAValues = new ArrayList<String>();
        columnAValues.add("104255");
        addToResult(columnA, columnAValues);

        String columnB = "li_count";
        List<String> columnBValues = new ArrayList<String>();
        columnBValues.add("155");
        addToResult(columnB, columnBValues);


        com.yahoo.validatar.common.Test test = new com.yahoo.validatar.common.Test();
        test.asserts = new ArrayList<String>();
        test.asserts.add("pv_count == 104255 && pv_count > li_count || pv_count == 5");
        test.asserts.add("pv_count != 104255 && pv_count > li_count || li_count == 155");

        Assertor.assertAll(results, wrap(test));

        Assert.assertFalse(test.failed());
    }

    @Test
    public void testApproxAssertion() {
        String columnA = "pv_count";
        List<String> columnAValues = new ArrayList<String>();
        columnAValues.add("104255");
        addToResult(columnA, columnAValues);
        String columnB = "foo.pv_count";
        List<String> columnBValues = new ArrayList<String>();
        columnBValues.add("0");
        addToResult(columnB, columnBValues);

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
        String columnA = "pv_count";
        List<String> columnAValues = new ArrayList<String>();
        columnAValues.add("104255");
        addToResult(columnA, columnAValues);

        String columnB = "li_count";
        List<String> columnBValues = new ArrayList<String>();
        columnBValues.add("155");
        addToResult(columnB, columnBValues);

        com.yahoo.validatar.common.Test test = new com.yahoo.validatar.common.Test();
        test.asserts = new ArrayList<String>();
        test.asserts.add("((pv_count == 104255 && pv_count < li_count) || (li_count*10000 > pv_count))");

        Assertor.assertAll(results, wrap(test));

        Assert.assertFalse(test.failed());
    }
}

