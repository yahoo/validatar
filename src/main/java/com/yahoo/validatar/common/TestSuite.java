/*
 * Copyright 2015 Yahoo Inc.
 * Licensed under the terms of the Apache 2 license. Please see LICENSE file in the project root for terms.
 */
package com.yahoo.validatar.common;

import java.util.List;

public class TestSuite {
    public String name;
    public String description;
    public List<Query> queries;
    public List<Test> tests;

    /**
     * Checks to see if this had any failures. For this method, tests are marked as failed even if they are warn only.
     *
     * @return A boolean denoting whether there were any errors in this suite.
     */
    public boolean hasFailures() {
        // Even if tests were set to warn only want to mark if
        return hasError(queries) || hasError(tests);
    }

    private static <T extends Executable> boolean hasError(List<T> executables) {
        return executables != null && executables.stream().anyMatch(Executable::failed);
    }
}
