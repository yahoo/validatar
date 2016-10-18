/*
 * Copyright 2015 Yahoo Inc.
 * Licensed under the terms of the Apache 2 license. Please see LICENSE file in the project root for terms.
 */
package com.yahoo.validatar.common;

import org.testng.Assert;
import org.testng.annotations.Test;

public class QueryTest {
    @Test
    public void testGetSet() {
        Query query = new Query();

        query.setFailure("sample message");
        Assert.assertEquals(query.getMessages().size(), 1);
        Assert.assertEquals(query.getMessages().get(0), "sample message");

        Assert.assertTrue(query.failed());
    }
}
