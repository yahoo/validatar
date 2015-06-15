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

package com.yahoo.validatar;

import com.yahoo.validatar.common.TestSuite;
import com.yahoo.validatar.common.Test;
import com.yahoo.validatar.common.Query;
import com.yahoo.validatar.common.Result;
import com.yahoo.validatar.execution.EngineManager;
import com.yahoo.validatar.parse.ParseManager;
import com.yahoo.validatar.assertion.Assertor;
import com.yahoo.validatar.report.FormatManager;
import static com.yahoo.validatar.common.Utilities.*;

import org.apache.log4j.Logger;

import java.util.Map;
import java.util.HashMap;
import java.util.List;
import java.util.ArrayList;
import static java.util.Arrays.*;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.IOException;

import joptsimple.OptionParser;
import joptsimple.OptionSet;

public class App {
    /** Logging class. */
    protected static final Logger LOG = Logger.getLogger(com.yahoo.validatar.App.class.getName());

    /** The CLI parser. */
    public static final OptionParser PARSER = new OptionParser() {
        {
            acceptsAll(asList("parameter"), "Parameter to replace all '${VAR}' in the query string. Ex: --parameter DATE=2014-07-24")
                .withRequiredArg()
                .describedAs("Parameter");
            acceptsAll(asList("test-suite"), "File or folder that contains the test suite file(s).")
                .withRequiredArg()
                .required()
                .ofType(File.class)
                .describedAs("Test suite file/folder");
            acceptsAll(asList("h", "help"), "Shows help message.");
            allowsUnrecognizedOptions();
        }
    };

    /**
     * Split parameters for string replacement.
     *
     * @param options Option set
     * @param parameterName Option parameter to split
     * @return Map of parameters and replacement strings
     */
    public static Map<String, String> splitParameters(OptionSet options, String parameterName) {
        Map<String, String> parameterMap = new HashMap<String, String>();
        for (String parameter : (List<String>) options.valuesOf(parameterName)) {
            String[] tokens = parameter.split("=");
            if (tokens.length != 2) {
                throw new RuntimeException("Invalid parameter. It should be KEY=VALUE. Found " + parameter);
            }
            parameterMap.put(tokens[0], tokens[1]);
        }
        return parameterMap;
    }

    /**
     * Parse arguements with parser.
     * @param args CLI args
     * @return Option set containing all settings
     */
    public static OptionSet parse(String[] args) throws IOException {
        try {
            return PARSER.parse(args);
        } catch (Exception e) {
            PARSER.printHelpOn(System.out);
            throw new RuntimeException("Unable to parse arguments", e);
        }
    }

    /**
     * Run the testSuite and parameters with the given Parse, Engine and Format Managers.
     */
    public static void run(File testSuite, Map<String, String> parameters, ParseManager parseManager,
                           EngineManager engineManager, FormatManager formatManager) throws FileNotFoundException, IOException {
        // Load the test suite file(s)
        LOG.info("Parsing test files...");
        List<TestSuite> suites = ParseManager.expandParameters(parseManager.load(testSuite), parameters);

        // Get the queries and tests
        List<Query> queries = new ArrayList<Query>();
        List<Test> tests = new ArrayList<Test>();
        for (TestSuite suite : suites) {
            addNonNull(suite.queries, queries);
            addNonNull(suite.tests, tests);
        }

        // Run the queries
        LOG.info("Running queries...");
        if (!engineManager.run(queries)) {
            LOG.error("Error running queries. Failing...");
            return;
        }

        // Get the data
        List<Result> data = new ArrayList<Result>();
        for (TestSuite suite : suites) {
            for (Query query : suite.queries) {
                data.add(query.getResult());
            }
        }

        // Run the tests
        LOG.info("Running tests...");
        Assertor.assertAll(data, tests);

        // Write reports
        LOG.info("Writing reports...");
        formatManager.writeReport(suites);

        LOG.info("Done!");
    }

    /**
     * Main.
     */
    public static void main(String[] args) throws IOException, FileNotFoundException {
        // Parse CLI args
        OptionSet options = parse(args);
        Map<String, String> parameterMap = splitParameters(options, "parameter");

        ParseManager parseManager = new ParseManager();
        EngineManager engineManager = new EngineManager(args);
        FormatManager formatManager = new FormatManager(args);

        // Check if user wants to see help
        if (options.has("h") || options.has("help")) {
            PARSER.printHelpOn(System.out);
            engineManager.printHelp();
            formatManager.printHelp();
            return;
        }
        run((File) options.valueOf("test-suite"), parameterMap, parseManager, engineManager, formatManager);
    }
}
