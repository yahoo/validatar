/*
 * Copyright 2015 Yahoo Inc.
 * Licensed under the terms of the Apache 2 license. Please see LICENSE file in the project root for terms.
 */
package com.yahoo.validatar.common;

import org.testng.Assert;
import org.testng.annotations.Test;

public class TestTest {
    @Test
    public void testGetSet() {
        com.yahoo.validatar.common.Test test = new com.yahoo.validatar.common.Test();

        test.addMessage("sample message");
        Assert.assertEquals(test.getMessages().size(), 1);
        Assert.assertEquals(test.getMessages().get(0), "sample message");

        test.setFailed();
        Assert.assertTrue(test.failed());

        test.setSuccess();
        Assert.assertFalse(test.failed());
    }
}
