/*
 * Copyright 2015 Yahoo Inc.
 * Licensed under the terms of the Apache 2 license. Please see LICENSE file in the project root for terms.
 */
package com.yahoo.validatar.parse;

import com.yahoo.validatar.common.TestSuite;

import java.io.InputStream;

public interface Parser {
    /**
     * Parse the TestSuite from an InputStream.
     *
     * @param data The InputStream containing the tests.
     * @return A TestSuite object representing the parsed testfile.
     */
    TestSuite parse(InputStream data);

    /**
     * Returns the name of this Parser.
     *
     * @return The {@link java.lang.String} name.
     */
    String getName();
}

