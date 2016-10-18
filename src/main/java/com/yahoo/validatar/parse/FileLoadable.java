/*
 * Copyright 2015 Yahoo Inc.
 * Licensed under the terms of the Apache 2 license. Please see LICENSE file in the project root for terms.
 */
package com.yahoo.validatar.parse;

import com.yahoo.validatar.common.TestSuite;

import java.io.File;
import java.io.FileNotFoundException;
import java.util.List;

public interface FileLoadable {
    /**
     * Load test file(s) from a provided path. If a folder, load all test files. If a file, load it.
     *
     * @param path The folder with the test file(s) or the test file.j
     * @return A non null list of TestSuites representing the TestSuites in path. Empty if no TestSuite found.
     * @throws java.io.FileNotFoundException if any.
     */
    List<TestSuite> load(File path) throws FileNotFoundException;
}
