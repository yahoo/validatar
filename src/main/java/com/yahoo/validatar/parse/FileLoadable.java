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

import java.io.File;
import java.io.FileNotFoundException;
import java.util.List;

public interface FileLoadable {
    /**
     * Load test file(s) from a provided path. If a folder, load all test files. If a file, load it.
     *
     * @param path The folder with the test file(s) or the test file.j
     * @return A list of TestSuites representing the TestSuites in path. Empty if no TestSuite found.
     * @throws java.io.FileNotFoundException if any.
     */
    public List<TestSuite> load(File path) throws FileNotFoundException;
}
