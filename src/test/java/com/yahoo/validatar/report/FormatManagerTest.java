/*
 * Copyright 2015 Yahoo Inc.
 * Licensed under the terms of the Apache 2 license. Please see LICENSE file in the project root for terms.
 */
package com.yahoo.validatar.report;

import com.yahoo.validatar.common.Query;
import com.yahoo.validatar.common.TestSuite;
import org.testng.Assert;
import org.testng.annotations.Test;

import java.io.FileNotFoundException;
import java.io.IOException;
import java.util.Collections;
import java.util.List;

import static com.yahoo.validatar.OutputCaptor.runWithoutOutput;

public class FormatManagerTest {
    // Used for tests
    private class MockFormatter implements Formatter {
        public boolean wroteReport = false;

        @Override
        public boolean setup(String[] arguments) {
            return true;
        }

        @Override
        public void printHelp() {
        }

        @Override
        public void writeReport(List<TestSuite> testSuites) throws IOException {
            wroteReport = true;
        }

        @Override
        public String getName() {
            return "MockFormat";
        }
    }

    public static class FailingFormatter implements Formatter {
        public FailingFormatter() {
        }

        @Override
        public boolean setup(String[] arguments) {
            return false;
        }

        @Override
        public void printHelp() {
        }

        @Override
        public void writeReport(List<TestSuite> testSuites) throws IOException {
        }

        @Override
        public String getName() {
            return "FailingFormat";
        }
    }

    @Test(expectedExceptions = {NullPointerException.class})
    public void testConstructorAndFindFormatterExceptionNoFormatterFound() throws FileNotFoundException {
        String[] args = {"--report-format", "INVALID"};
        runWithoutOutput(() -> new FormatManager(args));
    }

    @Test(expectedExceptions = {RuntimeException.class})
    public void testNullFormatter() {
        String[] args = {};
        FormatManager manager = new FormatManager(args);
        FailingFormatter formatter = new FailingFormatter();
        manager.setAvailableFormatters(Collections.singletonMap("fail", formatter));
        runWithoutOutput(() -> manager.setupFormatter("fail", args));
    }

    @Test
    public void testWriteReport() throws IOException {
        String[] args = {};
        FormatManager manager = new FormatManager(args);
        MockFormatter formatter = new MockFormatter();
        manager.setAvailableFormatters(Collections.singletonMap("MockFormat", formatter));
        manager.setupFormatter("MockFormat", args);
        manager.writeReport(null);
        Assert.assertTrue(formatter.wroteReport);
    }

    @Test
    public void testWriteReportOnlyOnFailureForFailingQueriesAndTests() throws IOException {
        String[] args = {"--report-on-failure-only", "true"};
        MockFormatter formatter = new MockFormatter();
        FormatManager manager = new FormatManager(args);
        manager.setAvailableFormatters(Collections.singletonMap("MockFormat", formatter));
        manager.setupFormatter("MockFormat", args);
        TestSuite suite = new TestSuite();

        Assert.assertFalse(formatter.wroteReport);

        com.yahoo.validatar.common.Test failingTest = new com.yahoo.validatar.common.Test();
        failingTest.setFailed();
        Query failingQuery = new Query();
        failingQuery.setFailed();
        suite.queries = Collections.singletonList(failingQuery);
        suite.tests = Collections.singletonList(failingTest);
        manager.writeReport(Collections.singletonList(suite));
        Assert.assertTrue(formatter.wroteReport);
    }

    @Test
    public void testWriteReportOnlyOnFailureForWarnOnlyTests() throws IOException {
        String[] args = {"--report-on-failure-only", "true"};
        MockFormatter formatter = new MockFormatter();
        FormatManager manager = new FormatManager(args);
        manager.setAvailableFormatters(Collections.singletonMap("MockFormat", formatter));
        manager.setupFormatter("MockFormat", args);
        TestSuite suite = new TestSuite();

        Assert.assertFalse(formatter.wroteReport);

        com.yahoo.validatar.common.Test warnOnlyTest = new com.yahoo.validatar.common.Test();
        warnOnlyTest.warnOnly = true;
        warnOnlyTest.setFailed();
        suite.queries = null;
        suite.tests = Collections.singletonList(warnOnlyTest);
        manager.writeReport(Collections.singletonList(suite));
        Assert.assertTrue(formatter.wroteReport);
    }

    @Test
    public void testWriteReportOnlyOnFailureForPassingQueriesAndTests() throws IOException {
        String[] args = {"--report-on-failure-only", "true"};
        MockFormatter formatter = new MockFormatter();
        FormatManager manager = new FormatManager(args);
        manager.setAvailableFormatters(Collections.singletonMap("MockFormat", formatter));
        manager.setupFormatter("MockFormat", args);
        TestSuite suite = new TestSuite();

        Assert.assertFalse(formatter.wroteReport);

        Query passingQuery = new Query();
        suite.queries = Collections.singletonList(passingQuery);
        suite.tests = null;
        manager.writeReport(Collections.singletonList(suite));
        Assert.assertFalse(formatter.wroteReport);

        com.yahoo.validatar.common.Test passingTest = new com.yahoo.validatar.common.Test();
        suite.tests = Collections.singletonList(passingTest);
        manager.writeReport(Collections.singletonList(suite));
        Assert.assertFalse(formatter.wroteReport);

    }
}
