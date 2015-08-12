/*
 * Copyright 2014-2015 Yahoo! Inc.
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

