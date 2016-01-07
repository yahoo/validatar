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
