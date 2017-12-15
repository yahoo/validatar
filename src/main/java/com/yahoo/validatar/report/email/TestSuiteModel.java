package com.yahoo.validatar.report.email;

import com.yahoo.validatar.common.Query;
import com.yahoo.validatar.common.Test;
import com.yahoo.validatar.common.TestSuite;

import java.util.LinkedList;
import java.util.List;

/**
 * This class holds the necessary information for rendering
 * the Validatar report using Jtwig.
 */
public class TestSuiteModel {
    /**
     * Test suite name.
     */
    public final String name;
    /**
     * Number of passed queries.
     */
    public final int queryPassed;
    /**
     * Total number of queries.
     */
    public final int queryTotal;
    /**
     * Number of passed tests.
     */
    public final int testPassed;
    /**
     * Total number of tests.
     */
    public final int testTotal;

    /**
     * List of failed queries.
     */
    public final List<Query> failedQueries;
    /**
     * List of failed tests.
     */
    public final List<Test> failedTests;

    /**
     * Create a {@code TestSuiteModel} from a {@code TestSuite}.
     * The constructor will pull the required information from
     * the given test suite.
     *
     * @param testSuite The test suite object to model.
     */
    protected TestSuiteModel(TestSuite testSuite) {
        failedQueries = new LinkedList<>();
        failedTests = new LinkedList<>();
        this.name = testSuite.name;
        int passCount = 0;
        for (Query query : testSuite.queries) {
            if (query.failed()) {
                failedQueries.add(query);
            } else {
                passCount++;
            }
        }
        this.queryPassed = passCount;
        this.queryTotal = testSuite.queries.size();
        passCount = 0;
        for (Test test : testSuite.tests) {
            if (test.failed()) {
                failedTests.add(test);
            } else {
                passCount++;
            }
        }
        this.testPassed = passCount;
        this.testTotal = testSuite.tests.size();
    }

    /**
     * @return True if all queries and tests passed.
     */
    protected boolean allPassed() {
        return queryPassed == queryTotal && testPassed == testTotal;
    }
}
