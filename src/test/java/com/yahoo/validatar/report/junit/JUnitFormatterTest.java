/*
 * Copyright 2015 Yahoo Inc.
 * Licensed under the terms of the Apache 2 license. Please see LICENSE file in the project root for terms.
 */
package com.yahoo.validatar.report.junit;

import com.yahoo.validatar.common.Query;
import com.yahoo.validatar.common.TestSuite;
import com.yahoo.validatar.parse.ParseManager;
import org.dom4j.Document;
import org.dom4j.DocumentException;
import org.dom4j.DocumentHelper;
import org.dom4j.util.NodeComparator;
import org.testng.Assert;
import org.testng.annotations.Test;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class JUnitFormatterTest {
    @Test
    public void testWriteReport() throws IOException, DocumentException {
        Map<String, String> paramMap = new HashMap<>();
        paramMap.put("DATE", "20140807");

        ParseManager manager = new ParseManager(new String[0]);

        List<TestSuite> testSuites = manager.load((new File("src/test/resources/sample-tests/")));
        ParseManager.deParametrize(testSuites, paramMap);

        Assert.assertEquals(testSuites.size(), 3);

        TestSuite simpleExamples = testSuites.stream().filter(s -> "Simple examples".equals(s.name)).findFirst().get();
        Query failingQuery = simpleExamples.queries.get(2);
        failingQuery.setFailure("Query had a typo");
        com.yahoo.validatar.common.Test test = simpleExamples.tests.get(1);
        test.setFailed();
        test.addMessage("Sample fail message");

        TestSuite validatarExamples = testSuites.stream().filter(s -> "Validatar Example".equals(s.name)).findFirst().get();
        test = validatarExamples.tests.get(0);
        test.setFailed();
        test.addMessage("Another multiline \nfail \nmessage");

        // Generate the test report file
        String[] args = {"--report-file", "target/JUnitOutputTest.xml"};
        JUnitFormatter jUnitFormatter = new JUnitFormatter();
        jUnitFormatter.setup(args);
        jUnitFormatter.writeReport(testSuites);

        // Diff the test report file, and the expected output
        String output = new String(Files.readAllBytes(Paths.get("target/JUnitOutputTest.xml")));
        Document outputDOM = DocumentHelper.parseText(output);

        String expectedOutput = new String(Files.readAllBytes(Paths.get("src/test/resources/ExpectedJUnitOutput.xml")));
        Document expectedOutputDom = DocumentHelper.parseText(expectedOutput);

        NodeComparator comparator = new NodeComparator();
        if (comparator.compare(expectedOutputDom, outputDOM) != 0) {
            Assert.fail("The generated XML does not match expected XML!");
        }
    }
}
