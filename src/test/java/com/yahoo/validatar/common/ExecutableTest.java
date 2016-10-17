/*
 * Copyright 2014-2016 Yahoo! Inc.
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
