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

package com.yahoo.validatar.common;

import java.util.List;
import java.util.ArrayList;
import java.util.Arrays;

import org.testng.Assert;
import org.testng.annotations.Test;
import org.testng.annotations.BeforeMethod;
import org.testng.annotations.AfterMethod;

public class UtilitiesTest {
    private Utilities utilities = new Utilities();

    @Test
    public void testAdditionToCollection() {
        List<String> list = new ArrayList<>();
        Utilities.addNonNull((String) null, list);
        Utilities.addNonNull("52", list);
        Utilities.addNonNull((String) null, list);
        Assert.assertEquals(list.size(), 1);
        Assert.assertEquals(list.get(0), "52");
    }

    @Test
    public void testAdditionOfCollectionToCollection() {
        List<String> items = Arrays.asList("1", "2", null, "3");
        List<String> list = new ArrayList<>();
        Utilities.addNonNull((List<String>) null, list);
        Utilities.addNonNull(items, list);
        Utilities.addNonNull((List<String>) null, list);
        Assert.assertEquals(list.size(), 3);
        Assert.assertEquals(list.get(2), "3");
    }
}
