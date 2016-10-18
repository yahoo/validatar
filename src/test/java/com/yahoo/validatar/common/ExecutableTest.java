/*
 * Copyright 2015 Yahoo Inc.
 * Licensed under the terms of the Apache 2 license. Please see LICENSE file in the project root for terms.
 */
package com.yahoo.validatar.common;

import org.testng.Assert;
import org.testng.annotations.Test;

public class ExecutableTest {
    private class Annotated extends Executable {
    }

    @Test
    public void testInitials() {
        Annotated sample = new Annotated();

        Assert.assertFalse(sample.failed());
        Assert.assertNull(sample.getMessages());
    }

    @Test
    public void testRanState() {
        Annotated sample = new Annotated();

        Assert.assertFalse(sample.failed());
        sample.setFailed();
        Assert.assertTrue(sample.failed());
        sample.setSuccess();
        Assert.assertFalse(sample.failed());
    }

    @Test
    public void testMessages() {
        Annotated sample = new Annotated();

        sample.addMessage("Test 1");
        Assert.assertEquals(sample.getMessages().size(), 1);
        Assert.assertEquals(sample.getMessages().get(0), "Test 1");

        sample.addMessage("Test 2");
        Assert.assertEquals(sample.getMessages().size(), 2);
        Assert.assertEquals(sample.getMessages().get(0), "Test 1");
        Assert.assertEquals(sample.getMessages().get(1), "Test 2");
    }
}
