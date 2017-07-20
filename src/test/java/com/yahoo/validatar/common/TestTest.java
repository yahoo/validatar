/*
 * Copyright 2015 Yahoo Inc.
 * Licensed under the terms of the Apache 2 license. Please see LICENSE file in the project root for terms.
 */
package com.yahoo.validatar.common;

import org.testng.Assert;
import org.testng.annotations.Test;

import java.io.FileNotFoundException;

import static com.yahoo.validatar.TestHelpers.getTestSuiteFrom;

public class TestTest {
    @Test
    public void testGetSet() {
        com.yahoo.validatar.common.Test test = new com.yahoo.validatar.common.Test();

        test.addMessage("sample message");
        Assert.assertEquals(test.getMessages().size(), 1);
        Assert.assertEquals(test.getMessages().get(0), "sample message");

        test.setFailed();
        Assert.assertTrue(test.failed());
        Assert.assertFalse(test.passed());

        test.setSuccess();
        Assert.assertFalse(test.failed());
        Assert.assertTrue(test.passed());

        test.warnOnly = true;
        Assert.assertTrue(test.passed());
        test.setFailed();
        Assert.assertTrue(test.passed());
    }

    @Test
    public void testLoadingWithWarnOnly() throws FileNotFoundException {
        TestSuite suite = getTestSuiteFrom("sample-tests/tests.yaml");

        com.yahoo.validatar.common.Test test = suite.tests.get(0);
        Assert.assertTrue(test.warnOnly);

        test = suite.tests.get(1);
        Assert.assertFalse(test.warnOnly);
    }
}
