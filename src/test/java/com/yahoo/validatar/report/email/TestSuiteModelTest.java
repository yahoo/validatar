package com.yahoo.validatar.report.email;

import com.yahoo.validatar.common.Query;
import com.yahoo.validatar.common.TestSuite;
import org.testng.annotations.Test;

import java.util.Arrays;

import static junit.framework.Assert.assertFalse;
import static junit.framework.Assert.assertTrue;
import static org.junit.Assert.assertEquals;

public class TestSuiteModelTest {

    @Test
    public void testConstructorCountsPassedQueriesAndTests() {
        TestSuite ts = new TestSuite();
        com.yahoo.validatar.common.Test passedTest = new com.yahoo.validatar.common.Test();
        assertTrue(passedTest.passed());
        com.yahoo.validatar.common.Test failedTest = new com.yahoo.validatar.common.Test();
        failedTest.setFailed();
        assertFalse(failedTest.passed());
        Query passedQuery = new Query();
        assertFalse(passedQuery.failed());
        Query failedQuery = new Query();
        failedQuery.setFailed();
        assertTrue(failedQuery.failed());
        ts.queries = Arrays.asList(failedQuery, passedQuery, failedQuery, passedQuery, passedQuery);
        ts.tests = Arrays.asList(passedTest, failedTest, passedTest, failedTest, failedTest);
        TestSuiteModel model = new TestSuiteModel(ts);
        assertEquals(5, model.queryTotal);
        assertEquals(5, model.testTotal);
        assertEquals(3, model.queryPassed);
        assertEquals(2, model.testPassed);
    }

}
