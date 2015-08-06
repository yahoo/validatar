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

import org.testng.Assert;
import org.testng.annotations.Test;

import java.util.Arrays;
import java.util.List;
import java.util.Map;

public class ResultTest {
    @Test
    public void testPrefix() {
        Result result = new Result("foo.");
        result.addColumn("a", Arrays.asList(new TypedObject(1L, TypeSystem.Type.LONG)));

        Assert.assertEquals(result.getColumn("a").size(), 1);
        Assert.assertEquals((Long) result.getColumn("a").get(0).data, Long.valueOf(1L));
        Assert.assertEquals((Long) result.getColumns().get("foo.a").get(0).data, Long.valueOf(1L));
    }

    @Test
    public void testAddRow() {
        Result result = new Result();
        result.addColumnRow("a", new TypedObject(2L, TypeSystem.Type.LONG));
        Assert.assertEquals((Long) result.getColumn("a").get(0).data, Long.valueOf(2L));
        result.addColumnRow("a",new TypedObject(3L, TypeSystem.Type.LONG));
        Assert.assertEquals((Long) result.getColumn("a").get(1).data, Long.valueOf(3L));
    }

    @Test
    public void testAddColumn() {
        Result result = new Result();

        result.addColumn("a");
        Assert.assertTrue(result.getColumn("a").isEmpty());

        result.addColumnRow("a", new TypedObject(2L, TypeSystem.Type.LONG));
        Assert.assertEquals((Long) result.getColumn("a").get(0).data, Long.valueOf(2L));
    }

    @Test
    public void testMergeNull() {
        Result result = new Result("Bar.");
        result.merge(null);
        Assert.assertTrue(result.getColumns().isEmpty());
    }

    @Test
    public void testMerge() {
        Result result = new Result("Bar.");
        result.addColumnRow("a", new TypedObject(2L, TypeSystem.Type.LONG));
        Assert.assertEquals((Long) result.getColumn("a").get(0).data, Long.valueOf(2L));

        Result anotherResult = new Result("Foo.");
        anotherResult.addColumnRow("a", new TypedObject(3L, TypeSystem.Type.LONG));
        Assert.assertEquals((Long) anotherResult.getColumn("a").get(0).data, Long.valueOf(3L));

        result.merge(anotherResult);
        // You can't get anotherResult.a anymore
        Assert.assertEquals((Long) result.getColumn("a").get(0).data, Long.valueOf(2L));
        // You have to get the map
        Map<String, List<TypedObject>> results = result.getColumns();
        Assert.assertEquals(results.size(), 2);
        Assert.assertEquals(results.get("Foo.a").size(), 1);
        Assert.assertEquals((Long) results.get("Foo.a").get(0).data, Long.valueOf(3L));
        Assert.assertEquals(results.get("Bar.a").size(), 1);
        Assert.assertEquals((Long) results.get("Bar.a").get(0).data, Long.valueOf(2L));

    }
}
