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

import java.util.Map;
import java.util.HashMap;
import java.util.List;
import java.util.ArrayList;

import org.testng.Assert;
import org.testng.annotations.Test;
import org.testng.annotations.BeforeMethod;
import org.testng.annotations.AfterMethod;

public class AssertorTest {
    private Map<String, List<String>> columns = null;

    private List<com.yahoo.validatar.common.Test> wrap(com.yahoo.validatar.common.Test... tests) {
        List<com.yahoo.validatar.common.Test> asList = new ArrayList<com.yahoo.validatar.common.Test>();
        for (com.yahoo.validatar.common.Test test : tests) {
            asList.add(test);
        }
        return asList;
    }

    @Test
    public void testNoLookupAssertion() {
        com.yahoo.validatar.common.Test test = new com.yahoo.validatar.common.Test();
        test.asserts = new ArrayList<String>();
        test.asserts.add("100 > 1000");

        Assertor.assertAll(columns, wrap(test));
        Assert.assertTrue(test.failed());
        Assert.assertEquals("100 > 1000 was false for these values {}", test.getMessages().get(0));
    }

    @Test
    public void testOneValidNumericAssertion() {

        columns = new HashMap<String, List<String>>();

        String columnA = "AV.pv_count";
        List<String> columnAValues = new ArrayList<String>();
        columnAValues.add("104255");
        columns.put(columnA, columnAValues);

        String columnB = "AV.li_count";
        List<String> columnBValues = new ArrayList<String>();
        columnBValues.add("155");
        columns.put(columnB, columnBValues);

        com.yahoo.validatar.common.Test test = new com.yahoo.validatar.common.Test();
        test.asserts = new ArrayList<String>();
        test.asserts.add("AV.pv_count > 1000");

        Assertor.assertAll(columns, wrap(test));
        Assert.assertFalse(test.failed());
    }

    @Test
    public void testOneValidTextAssertion() {
        columns = new HashMap<String, List<String>>();

        String columnA = "largest_spaceid";
        List<String> columnAValues = new ArrayList<String>();
        columnAValues.add("104255");
        columns.put(columnA, columnAValues);

        com.yahoo.validatar.common.Test test = new com.yahoo.validatar.common.Test();
        test.asserts = new ArrayList<String>();
        test.asserts.add("largest_spaceid == \"104255\"");
        Assertor.assertAll(columns, wrap(test));
        Assert.assertFalse(test.failed());
    }

    @Test
    public void testWhitespaceStringAssertion() {
        columns = new HashMap<String, List<String>>();

        String columnA = "largest_spaceid";
        List<String> columnAValues = new ArrayList<String>();
        columnAValues.add("  104255");
        columns.put(columnA, columnAValues);


        com.yahoo.validatar.common.Test test = new com.yahoo.validatar.common.Test();
        test.asserts = new ArrayList<String>();
        test.asserts.add("largest_spaceid == \"  104255\"");
        Assertor.assertAll(columns, wrap(test));
        Assert.assertFalse(test.failed());
    }

    @Test
    public void testWhitespaceAssertion() {
        columns = new HashMap<String, List<String>>();

        String columnA = "largest_spaceid";
        List<String> columnAValues = new ArrayList<String>();
        columnAValues.add("104255");
        columns.put(columnA, columnAValues);

        com.yahoo.validatar.common.Test test = new com.yahoo.validatar.common.Test();
        test.asserts = new ArrayList<String>();
        test.asserts.add("\tlargest_spaceid                == \"104255\"      ");

        Assertor.assertAll(columns, wrap(test));

        Assert.assertFalse(test.failed());
    }

    @Test
    public void testOneNonValidNumericAssertion() {
        columns = new HashMap<String, List<String>>();

        String columnA = "pv_count";
        List<String> columnAValues = new ArrayList<String>();
        columnAValues.add("104255");
        columns.put(columnA, columnAValues);

        String columnB = "li_count";
        List<String> columnBValues = new ArrayList<String>();
        columnBValues.add("155");
        columns.put(columnB, columnBValues);

        com.yahoo.validatar.common.Test test = new com.yahoo.validatar.common.Test();
        test.asserts = new ArrayList<String>();
        test.asserts.add("pv_count > 1000 && li_count < 100");
        Assertor.assertAll(columns, wrap(test));

        Assert.assertTrue(test.failed());
        Assert.assertEquals("pv_count > 1000 && li_count < 100 was false for these values {pv_count=104255, li_count=155}", test.getMessages().get(0));
    }

    @Test
    public void testOneNegationAssertion() {
        columns = new HashMap<String, List<String>>();

        String columnA = "pv_count";
        List<String> columnAValues = new ArrayList<String>();
        columnAValues.add("104255");
        columns.put(columnA, columnAValues);

        com.yahoo.validatar.common.Test test = new com.yahoo.validatar.common.Test();
        test.asserts = new ArrayList<String>();
        test.asserts.add("pv_count > -1000");
        Assertor.assertAll(columns, wrap(test));
        Assert.assertFalse(test.failed());
    }

    @Test
    public void testOneNegationLogicalAndParanthesizedAssertion() {
        columns = new HashMap<String, List<String>>();

        String columnA = "pv_count";
        List<String> columnAValues = new ArrayList<String>();
        columnAValues.add("104255");
        columns.put(columnA, columnAValues);

        com.yahoo.validatar.common.Test test = new com.yahoo.validatar.common.Test();
        test.asserts = new ArrayList<String>();
        test.asserts.add("!(pv_count < 1000)");

        Assertor.assertAll(columns, wrap(test));
        Assert.assertFalse(test.failed());
    }

    @Test
    public void testRelationalEqualityAssertion() {
        columns = new HashMap<String, List<String>>();

        String columnA = "pv_count";
        List<String> columnAValues = new ArrayList<String>();
        columnAValues.add("104255");
        columns.put(columnA, columnAValues);

        com.yahoo.validatar.common.Test test = new com.yahoo.validatar.common.Test();
        test.asserts = new ArrayList<String>();
        test.asserts.add("pv_count >= 104255");
        test.asserts.add("pv_count <= 104255");

        Assertor.assertAll(columns, wrap(test));
        Assert.assertFalse(test.failed());
    }

    @Test
    public void testMultiplicativeAssertion() {
        columns = new HashMap<String, List<String>>();

        String columnA = "pv_count";
        List<String> columnAValues = new ArrayList<String>();
        columnAValues.add("104255");
        columns.put(columnA, columnAValues);

        com.yahoo.validatar.common.Test test = new com.yahoo.validatar.common.Test();
        test.asserts = new ArrayList<String>();
        test.asserts.add("pv_count < 104255*2");
        test.asserts.add("pv_count > 104255/2");

        Assertor.assertAll(columns, wrap(test));
        Assert.assertFalse(test.failed());
    }

    @Test
    public void testAdditiveAssertion() {
        columns = new HashMap<String, List<String>>();

        String columnA = "pv_count";
        List<String> columnAValues = new ArrayList<String>();
        columnAValues.add("104255");
        columns.put(columnA, columnAValues);

        com.yahoo.validatar.common.Test test = new com.yahoo.validatar.common.Test();
        test.asserts = new ArrayList<String>();
        test.asserts.add("pv_count < 104255+1");
        test.asserts.add("pv_count > 104255-1");

        Assertor.assertAll(columns, wrap(test));
        Assert.assertFalse(test.failed());
    }

    @Test
    public void testEqualityAssertion() {
        columns = new HashMap<String, List<String>>();

        String columnA = "pv_count";
        List<String> columnAValues = new ArrayList<String>();
        columnAValues.add("104255");
        columns.put(columnA, columnAValues);

        com.yahoo.validatar.common.Test test = new com.yahoo.validatar.common.Test();
        test.asserts = new ArrayList<String>();
        test.asserts.add("pv_count == 104255");
        test.asserts.add("pv_count != 104255-1");

        Assertor.assertAll(columns, wrap(test));
        Assert.assertFalse(test.failed());
    }

    @Test
    public void testLogicalAssertion() {
        columns = new HashMap<String, List<String>>();

        String columnA = "pv_count";
        List<String> columnAValues = new ArrayList<String>();
        columnAValues.add("104255");
        columns.put(columnA, columnAValues);

        String columnB = "li_count";
        List<String> columnBValues = new ArrayList<String>();
        columnBValues.add("155");
        columns.put(columnB, columnBValues);


        com.yahoo.validatar.common.Test test = new com.yahoo.validatar.common.Test();
        test.asserts = new ArrayList<String>();
        test.asserts.add("pv_count == 104255 && pv_count > li_count || pv_count == 5");
        test.asserts.add("pv_count != 104255 && pv_count > li_count || li_count == 155");

        Assertor.assertAll(columns, wrap(test));

        Assert.assertFalse(test.failed());
    }

    @Test
    public void testApproxAssertion() {
        columns = new HashMap<String, List<String>>();

        String columnA = "pv_count";
        List<String> columnAValues = new ArrayList<String>();
        columnAValues.add("104255");
        columns.put(columnA, columnAValues);
        String columnB = "foo.pv_count";
        List<String> columnBValues = new ArrayList<String>();
        columnBValues.add("0");
        columns.put(columnB, columnBValues);

        com.yahoo.validatar.common.Test test = new com.yahoo.validatar.common.Test();
        test.asserts = new ArrayList<String>();
        test.asserts.add("approx(pv_count, 100000, 0.05)");
        Assertor.assertAll(columns, wrap(test));
        Assert.assertFalse(test.failed());

        test = new com.yahoo.validatar.common.Test();
        test.asserts = new ArrayList<String>();
        test.asserts.add("approx(pv_count, 100000, 0.01)");
        Assertor.assertAll(columns, wrap(test));
        Assert.assertTrue(test.failed());

        test = new com.yahoo.validatar.common.Test();
        test.asserts = new ArrayList<String>();
        test.asserts.add("approx(pv_count, 100000, 11241)");
        Assertor.assertAll(columns, wrap(test));
        Assert.assertTrue(test.failed());

        test = new com.yahoo.validatar.common.Test();
        test.asserts = new ArrayList<String>();
        test.asserts.add("approx(pv_count, pv_count, 0.01)");
        Assertor.assertAll(columns, wrap(test));
        Assert.assertFalse(test.failed());

        test = new com.yahoo.validatar.common.Test();
        test.asserts = new ArrayList<String>();
        test.asserts.add("approx(10000, 10010, 0.01)");
        Assertor.assertAll(columns, wrap(test));
        Assert.assertFalse(test.failed());

        test = new com.yahoo.validatar.common.Test();
        test.asserts = new ArrayList<String>();
        test.asserts.add("approx(pv_count, foo.pv_count, 0.10)");
        Assertor.assertAll(columns, wrap(test));
        Assert.assertTrue(test.failed());
    }

    @Test
    public void testComplexAssertion() {
        columns = new HashMap<String, List<String>>();

        String columnA = "pv_count";
        List<String> columnAValues = new ArrayList<String>();
        columnAValues.add("104255");
        columns.put(columnA, columnAValues);

        String columnB = "li_count";
        List<String> columnBValues = new ArrayList<String>();
        columnBValues.add("155");
        columns.put(columnB, columnBValues);

        com.yahoo.validatar.common.Test test = new com.yahoo.validatar.common.Test();
        test.asserts = new ArrayList<String>();
        test.asserts.add("((pv_count == 104255 && pv_count < li_count) || (li_count*10000 > pv_count))");

        Assertor.assertAll(columns, wrap(test));

        Assert.assertFalse(test.failed());
    }
}

