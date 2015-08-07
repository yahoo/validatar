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
     * @return true iff setup was succesful.
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
