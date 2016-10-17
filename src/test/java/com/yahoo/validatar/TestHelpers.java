/*
 * Copyright 2014-2016 Yahoo! Inc.
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

package com.yahoo.validatar;

import com.yahoo.validatar.common.Query;
import com.yahoo.validatar.common.TestSuite;
import com.yahoo.validatar.parse.yaml.YAML;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;

public class TestHelpers {
    public static Query getQueryFrom(String file, String name) throws FileNotFoundException {
        return getTestSuiteFrom(file).queries.stream().filter(q -> name.equals(q.name)).findAny().get();
    }

    public static TestSuite getTestSuiteFrom(String file) throws FileNotFoundException {
        File testFile = new File(TestHelpers.class.getClassLoader().getResource(file).getFile());
        return new YAML().parse(new FileInputStream(testFile));
    }
}
