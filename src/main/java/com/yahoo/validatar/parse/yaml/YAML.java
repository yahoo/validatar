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

package com.yahoo.validatar.parse.yaml;

import com.yahoo.validatar.common.TestSuite;
import com.yahoo.validatar.parse.Parser;
import org.yaml.snakeyaml.Yaml;
import org.yaml.snakeyaml.constructor.Constructor;

import java.io.InputStream;

public class YAML implements Parser {
    public static final String NAME = "yaml";

    /**
     * {@inheritDoc}
     */
    @Override
    public TestSuite parse(InputStream data) {
        Yaml yaml = new Yaml(new Constructor(TestSuite.class));
        return (TestSuite) yaml.load(data);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public String getName() {
        return NAME;
    }
}
