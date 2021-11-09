/*
 * Copyright 2015 Yahoo Inc.
 * Licensed under the terms of the Apache 2 license. Please see LICENSE file in the project root for terms.
 */
package com.yahoo.validatar.parse.yaml;

import com.yahoo.validatar.common.Query;
import com.yahoo.validatar.common.TestSuite;
import org.testng.Assert;
import org.testng.annotations.Test;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.util.Map;

public class YAMLTest {
    private final YAML yaml = new YAML();

    @Test
    public void testLoadOfValidTestFile() throws FileNotFoundException {
        TestSuite testSuite = yaml.parse(new FileInputStream(new File("src/test/resources/sample-tests/tests.yaml")));
        Assert.assertEquals(testSuite.name, "Validatar Example");
    }

    @Test
    public void testParamNotReplaced() throws FileNotFoundException {
        TestSuite testSuite = yaml.parse(new FileInputStream(new File("src/test/resources/sample-tests/tests.yaml")));
        Query query = testSuite.queries.get(2);
        Assert.assertEquals(query.value, "TEST ${DATE}");
    }

    @Test
    public void testQueryMetadata() throws FileNotFoundException {
        TestSuite testSuite = yaml.parse(new FileInputStream(new File("src/test/resources/metadata-tests/tests.yaml")));
        Assert.assertEquals(testSuite.queries.size(), 3);

        Query noMeta = testSuite.queries.get(0);
        Query windowMeta = testSuite.queries.get(1);
        Query threadMeta = testSuite.queries.get(2);

        Assert.assertNull(noMeta.metadata);
        Assert.assertEquals(windowMeta.metadata.size(), 2);
        Assert.assertEquals(threadMeta.metadata.size(), 1);

        Map<String, String> metaMap = noMeta.getMetadata();
        Assert.assertNull(metaMap);

        metaMap = windowMeta.getMetadata();
        Assert.assertEquals(metaMap.size(), 2);
        Assert.assertEquals(metaMap.get("windowSize"), "600");
        Assert.assertEquals(metaMap.get("records"), "10000");

        metaMap = threadMeta.getMetadata();
        Assert.assertEquals(metaMap.size(), 1);
        Assert.assertEquals(metaMap.get("threads"), "4");
    }

    @Test
    public void testQueryPriority() throws FileNotFoundException {
        TestSuite testSuite = yaml.parse(new FileInputStream(new File("src/test/resources/priority-tests/tests.yaml")));
        Assert.assertEquals(testSuite.queries.size(), 5);
        Assert.assertEquals(testSuite.queries.get(0).priority, Integer.MAX_VALUE);
        Assert.assertEquals(testSuite.queries.get(1).priority, -1);
        Assert.assertEquals(testSuite.queries.get(2).priority, 0);
        Assert.assertEquals(testSuite.queries.get(3).priority, 1);
        Assert.assertEquals(testSuite.queries.get(4).priority, 2);
    }
}
