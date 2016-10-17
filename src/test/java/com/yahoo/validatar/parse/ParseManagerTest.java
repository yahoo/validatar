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

package com.yahoo.validatar.parse;

import com.yahoo.validatar.common.Metadata;
import com.yahoo.validatar.common.Query;
import com.yahoo.validatar.common.TestSuite;
import org.mockito.Mockito;
import org.testng.Assert;
import org.testng.annotations.Test;

import java.io.File;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class ParseManagerTest {
    @Test
    public void testFailLoadOfNonFile() {
        ParseManager manager = new ParseManager(new String[0]);
        TestSuite testSuite = manager.getTestSuite(new File("src/test/resources"));
        Assert.assertNull(testSuite);
    }

    @Test
    public void testFailLoadOfUnknownFile() {
        ParseManager manager = new ParseManager(new String[0]);
        TestSuite testSuite = manager.getTestSuite(new File("src/test/resources/log4j.properties"));
        Assert.assertNull(testSuite);
    }

    @Test
    public void testFailLoadOfBadExtensionFile() {
        ParseManager manager = new ParseManager(new String[0]);
        TestSuite testSuite = manager.getTestSuite(new File("src/test/resources/log4j.properties"));
        Assert.assertNull(testSuite);
    }

    @Test
    public void testFailLoadOfNoExtensionFile() {
        ParseManager manager = new ParseManager(new String[0]);
        TestSuite testSuite = manager.getTestSuite(new File("LICENSE"));
        Assert.assertNull(testSuite);
    }

    @Test
    public void testFailLoadOfDisappearingFile() {
        ParseManager manager = new ParseManager(new String[0]);
        File mocked = Mockito.mock(File.class);
        Mockito.when(mocked.isFile()).thenReturn(true);
        Mockito.when(mocked.getName()).thenReturn("foo.yaml");
        Mockito.when(mocked.getPath()).thenReturn(null);
        TestSuite testSuite = manager.getTestSuite(mocked);
        Assert.assertNull(testSuite);
    }

    @Test
    public void testFailLoadOfNullPath() {
        ParseManager manager = new ParseManager(new String[0]);
        List<TestSuite> loaded = manager.load(null);
        Assert.assertTrue(loaded.isEmpty());
    }

    @Test
    public void testFailLoadOfEmptyDirectory() {
        ParseManager manager = new ParseManager(new String[0]);
        File mocked = Mockito.mock(File.class);
        Mockito.when(mocked.isFile()).thenReturn(false);
        Mockito.when(mocked.listFiles()).thenReturn(null);
        List<TestSuite> loaded = manager.load(mocked);
        Assert.assertTrue(loaded.isEmpty());
    }

    @Test
    public void testLoadOfDirectory() {
        ParseManager manager = new ParseManager(new String[0]);
        List<TestSuite> loaded = manager.load(new File("src/test/resources/sample-tests"));
        Assert.assertEquals(loaded.size(), 3);
    }

    @Test
    public void testFailExpansion() {
        Query query = new Query();
        query.value = "${var}";
        ParseManager.deParametrize(query, Collections.emptyMap());
        Assert.assertEquals(query.value, "${var}");
    }

    @Test
    public void testExpansion() {
        Query query = new Query();
        query.value = "${var}";
        ParseManager.deParametrize(query, Collections.singletonMap("var", "foo"));
        Assert.assertEquals(query.value, "foo");
    }

    @Test
    public void testMetadataExpansion() {
        Query query = new Query();
        Metadata metadataOne = new Metadata("${foo}", "${bar}");
        Metadata metadataTwo = new Metadata("x", "${baz}");
        Map<String, String> parameters = new HashMap<>();
        parameters.put("foo", "1");
        parameters.put("bar", "2");
        parameters.put("baz", "3");
        parameters.put("var", "4");
        query.value = "${var}";
        query.metadata = Arrays.asList(metadataOne, metadataTwo);
        ParseManager.deParametrize(query, parameters);
        Assert.assertEquals(query.metadata.get(0).key, "1");
        Assert.assertEquals(query.metadata.get(0).value, "2");
        Assert.assertEquals(query.metadata.get(1).key, "x");
        Assert.assertEquals(query.metadata.get(1).value, "3");
        Assert.assertEquals(query.value, "4");
    }

    @Test
    public void testTestAssertExpansion() {
        com.yahoo.validatar.common.Test test = new com.yahoo.validatar.common.Test();
        Map<String, String> parameters = new HashMap<>();
        parameters.put("foo", "1");
        parameters.put("bar", "2");
        parameters.put("baz", "3");
        parameters.put("var", "4");
        test.asserts = Arrays.asList("${foo} != QUERY.column", "${bar} > 1 && approx(${foo}, QUERY.column, ${var})");
        ParseManager.deParametrize(test, parameters);
        Assert.assertEquals(test.asserts.get(0), "1 != QUERY.column");
        Assert.assertEquals(test.asserts.get(1), "2 > 1 && approx(1, QUERY.column, 4)");
    }
}
