/*
 * Copyright 2015 Yahoo Inc.
 * Licensed under the terms of the Apache 2 license. Please see LICENSE file in the project root for terms.
 */
package com.yahoo.validatar.parse.yaml;

import com.yahoo.validatar.common.TestSuite;
import com.yahoo.validatar.parse.Parser;
import org.yaml.snakeyaml.Yaml;
import org.yaml.snakeyaml.constructor.Constructor;

import java.io.InputStream;

public class YAML implements Parser {
    public static final String NAME = "yaml";

    @Override
    public TestSuite parse(InputStream data) {
        Yaml yaml = new Yaml(new Constructor(TestSuite.class));
        return (TestSuite) yaml.load(data);
    }

    @Override
    public String getName() {
        return NAME;
    }
}
