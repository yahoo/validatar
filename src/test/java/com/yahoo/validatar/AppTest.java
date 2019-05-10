/*
 * Copyright 2015 Yahoo Inc.
 * Licensed under the terms of the Apache 2 license. Please see LICENSE file in the project root for terms.
 */
package com.yahoo.validatar;

import com.yahoo.validatar.execution.EngineManager;
import com.yahoo.validatar.execution.hive.Apiary;
import com.yahoo.validatar.parse.ParseManager;
import com.yahoo.validatar.report.FormatManager;
import joptsimple.OptionParser;
import joptsimple.OptionSet;
import org.testng.Assert;
import org.testng.annotations.Test;

import java.io.File;
import java.io.IOException;
import java.sql.Connection;
import java.sql.DriverManager;
import java.util.HashMap;
import java.util.Map;

import static com.yahoo.validatar.OutputCaptor.redirectToDevNull;
import static com.yahoo.validatar.OutputCaptor.redirectToStandard;

public class AppTest {
    private class MemoryDB extends Apiary {
        private final OptionParser parser = new OptionParser() {
            {
                accepts("hive-driver", "Fully qualified package name to the hive driver.")
                    .withRequiredArg()
                    .describedAs("Hive driver");
                accepts("hive-jdbc", "JDBC string to the HiveServer. Ex: 'jdbc:hive2://HIVE_SERVER:PORT/DATABASENAME' ")
                    .withRequiredArg()
                    .describedAs("Hive JDBC connector.");
                accepts("hive-username", "Hive server username.")
                    .withRequiredArg()
                    .describedAs("Hive server username.")
                    .defaultsTo("anon");
                accepts("hive-password", "Hive server password.")
                    .withRequiredArg()
                    .describedAs("Hive server password.")
                    .defaultsTo("anon");
                allowsUnrecognizedOptions();
            }
        };

        @Override
        public boolean setup(String[] arguments) {
            try {
                OptionSet options = parser.parse(arguments);
                String driver = (String) options.valueOf("hive-driver");
                Class.forName(driver);
                String jdbcConnector = (String) options.valueOf("hive-jdbc");
                Connection connection = DriverManager.getConnection(jdbcConnector, "", "");
                statement = connection.createStatement();
            } catch (Exception e) {
                throw new RuntimeException(e);
            }
            return true;
        }
    }

    private class CustomEngineManager extends EngineManager {
        public CustomEngineManager(String[] arguments) {
            super(new String[0]);
            this.arguments = arguments;
            this.engines = new HashMap<>();
            MemoryDB db = new MemoryDB();
            db.setup(arguments);
            engines.put(Apiary.ENGINE_NAME, new WorkingEngine(db));
        }
    }

    @Test
    public void testFailRunTests() throws Exception {
        String[] args = {"--report-file", "target/AppTest-testFailRunTests.xml",
                         "--hive-driver", "org.h2.Driver",
                         "--hive-jdbc", "jdbc:h2:mem:"};
        Map<String, String> parameterMap = new HashMap<>();
        File emptyTest = new File("src/test/resources/pig-tests/sample.yaml");
        ParseManager parseManager = new ParseManager(args);
        EngineManager engineManager = new CustomEngineManager(args);
        FormatManager formatManager = new FormatManager(args);

        App.run(emptyTest, parameterMap, parseManager, engineManager, formatManager);
        Assert.assertFalse(new File("target/AppTest-testFailRunTests.xml").exists());
    }

    @Test(expectedExceptions = {RuntimeException.class})
    public void testParameterMissingToken() throws IOException {
        String[] args = {"--test-suite", "tests.yaml", "--parameter", "DATE:20140807"};
        OptionSet options = App.parse(args);
        App.splitParameters(options, "parameter");
    }

    @Test
    public void testParameterParsingFailure() throws IOException {
        String[] args = {"--parameter", "DATE:20140807"};
        Assert.assertNull(new App().parse(args));
    }

    @Test
    public void testSimpleParameterParse() throws IOException {
        // Fake CLI args
        String[] args = {"--test-suite", "tests.yaml",
                         "--parameter", "DATE=2014071800",
                         "--parameter", "NAME=ALPHA"};

        // Parse CLI args
        Map<String, String> paramMap;
        OptionSet options = App.parse(args);
        paramMap = App.splitParameters(options, "parameter");

        // Check parse
        File testFile = (File) options.valueOf("test-suite");
        Assert.assertEquals(testFile.getName(), "tests.yaml");

        Assert.assertEquals(paramMap.get("DATE"), "2014071800");
        Assert.assertEquals(paramMap.get("NAME"), "ALPHA");
    }

    @Test
    public void testRunTests() throws Exception {
        String[] args = {"--report-file", "target/AppTest-testRunTests.xml",
                         "--hive-driver", "org.h2.Driver",
                         "--hive-jdbc", "jdbc:h2:mem:"};
        Map<String, String> parameterMap = new HashMap<>();
        File emptyTest = new File("src/test/resources/sample-tests/empty-test.yaml");
        ParseManager parseManager = new ParseManager(args);
        EngineManager engineManager = new CustomEngineManager(args);
        FormatManager formatManager = new FormatManager(args);

        App.run(emptyTest, parameterMap, parseManager, engineManager, formatManager);
        Assert.assertTrue(new File("target/AppTest-testRunTests.xml").exists());
    }

    @Test
    public void testRun() throws IOException {
        redirectToDevNull();

        App.run(new String[0]);

        String[] abbreviated = {"--h", "--test-suite", "src/test/resources/sample-tests"};
        App.run(abbreviated);
        Assert.assertFalse(new File("target/AppTest-testMainHelpPrinting.xml").exists());

        String[] nonAbbreviated = {"--help", "--test-suite", "src/test/resources/sample-tests"};
        App.run(nonAbbreviated);
        Assert.assertFalse(new File("target/AppTest-testMainHelpPrinting.xml").exists());

        String[] args = {"--report-file", "target/AppTest-testMainHelpPrinting.xml",
                         "--test-suite", "src/test/resources/sample-tests",
                         "--hive-driver", "org.h2.Driver",
                         "--hive-jdbc", "jdbc:h2:mem:"};
        App.run(args);
        Assert.assertFalse(new File("target/AppTest-testMainHelpPrinting.xml").exists());

        redirectToStandard();
    }
}
