/*
 * Copyright 2015 Yahoo Inc.
 * Licensed under the terms of the Apache 2 license. Please see LICENSE file in the project root for terms.
 */
package com.yahoo.validatar.report;

import com.yahoo.validatar.common.Helpable;
import com.yahoo.validatar.common.TestSuite;

import java.io.IOException;
import java.util.List;

/**
 * Interface for writing test report files.
 */
public interface Formatter extends Helpable {
    /**
     * Setups the engine using the input parameters.
     *
     * @param arguments An array of parameters of the form [--param1 value1 --param2 value2...]
     * @return true if setup was successful.
     */
    boolean setup(String[] arguments);

    /**
     * Write the results of the test suites out.
     *
     * @param testSuites List of TestSuites to generate the report with
     * @throws java.io.IOException if any.
     */
    void writeReport(List<TestSuite> testSuites) throws IOException;

    /**
     * Returns the name of the Formatter. Ex: 'JUnit' etc.
     *
     * @return Name of the formatter.
     */
    String getName();
}
