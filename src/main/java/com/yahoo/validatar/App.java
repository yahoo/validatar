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

import com.yahoo.validatar.assertion.Assertor;
import com.yahoo.validatar.common.Helpable;
import com.yahoo.validatar.common.Query;
import com.yahoo.validatar.common.Result;
import com.yahoo.validatar.common.Test;
import com.yahoo.validatar.common.TestSuite;
import com.yahoo.validatar.execution.EngineManager;
import com.yahoo.validatar.parse.ParseManager;
import com.yahoo.validatar.report.FormatManager;
import joptsimple.OptionParser;
import joptsimple.OptionSet;
import lombok.extern.slf4j.Slf4j;

import java.io.File;
import java.io.IOException;
import java.util.Collection;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.stream.Collectors;

import static java.util.Arrays.asList;
import static java.util.Collections.singletonList;

@Slf4j
public class App {
    public static final String PARAMETER = "parameter";
    public static final String PARAMETER_DELIMITER = "=";
    public static final String TEST_SUITE = "test-suite";
    public static final String HELP = "help";
    public static final String HELP_ABBREVIATED = "h";

    /**
     * The CLI parser.
     */
    public static final OptionParser PARSER = new OptionParser() {
        {
            acceptsAll(singletonList(PARAMETER), "Parameter to replace all '${VAR}' in the query string. Ex: --parameter DATE=2014-07-24")
                .withRequiredArg()
                .describedAs("Parameter");
            acceptsAll(singletonList(TEST_SUITE), "File or folder that contains the test suite file(s).")
                .withRequiredArg()
                .required()
                .ofType(File.class)
                .describedAs("Test suite file/folder");
            acceptsAll(asList(HELP_ABBREVIATED, HELP), "Shows help message.");
            allowsUnrecognizedOptions();
        }
    };

    /**
     * Split parameters for string replacement.
     *
     * @param options       Option set
     * @param parameterName Option parameter to split
     * @return Map of parameters and replacement strings
     */
    public static Map<String, String> splitParameters(OptionSet options, String parameterName) {
        Map<String, String> parameterMap = new HashMap<>();
        for (String parameter : (List<String>) options.valuesOf(parameterName)) {
            String[] tokens = parameter.split(PARAMETER_DELIMITER);
            if (tokens.length != 2) {
                throw new RuntimeException("Invalid parameter. It should be KEY=VALUE. Found " + parameter);
            }
            parameterMap.put(tokens[0], tokens[1]);
        }
        return parameterMap;
    }

    /**
     * Parse arguements with parser.
     *
     * @param args CLI args
     * @return Option set containing all settings
     */
    public static OptionSet parse(String[] args) {
        try {
            return PARSER.parse(args);
        } catch (Exception e) {
            return null;
        }
    }

    /**
     * Run the testSuite and parameters with the given Parse, Engine and Format Managers.
     *
     * @param testSuite     The {@link java.io.File} where the TestSuite(s) are.
     * @param parameters    An optional {@link java.util.Map} of parameters to their values to expand.
     * @param parseManager  A {@link com.yahoo.validatar.parse.ParseManager} to use.
     * @param engineManager A {@link com.yahoo.validatar.execution.EngineManager} to use.
     * @param formatManager A {@link com.yahoo.validatar.report.FormatManager} to use.
     * @throws java.io.IOException if any.
     */
    public static void run(File testSuite, Map<String, String> parameters, ParseManager parseManager,
                           EngineManager engineManager, FormatManager formatManager) throws IOException {
        // Load the test suite file(s)
        log.info("Parsing test files...");
        List<TestSuite> suites = parseManager.load(testSuite);
        log.info("Expanding parameters...");
        ParseManager.expandParameters(suites, parameters);

        // Get the non-null queries
        List<Query> queries = suites.stream().map(s -> s.queries).filter(Objects::nonNull)
                              .flatMap(Collection::stream).filter(Objects::nonNull).collect(Collectors.toList());

        // Run the queries
        log.info("Running queries...");
        if (!engineManager.run(queries)) {
            log.error("Error running queries. Failing...");
            return;
        }

        // Get the query results and merge them into one
        Result data = queries.stream().map(Query::getResult).reduce(new Result(), Result::merge);

        // Get the non-null tests
        List<Test> tests = suites.stream().map(s -> s.tests).filter(Objects::nonNull)
                           .flatMap(Collection::stream).filter(Objects::nonNull).collect(Collectors.toList());

        // Run the tests
        log.info("Running tests...");
        Assertor.assertAll(data, tests);

        // Write reports
        log.info("Writing reports...");
        formatManager.writeReport(suites);

        log.info("Done!");
    }

    /**
     * Main.
     *
     * @param args The input arguments.
     * @throws java.io.IOException if any.
     */
    public static void main(String[] args) throws IOException  {
        // Parse CLI args
        OptionSet options = parse(args);

        ParseManager parseManager = new ParseManager();
        EngineManager engineManager = new EngineManager(args);
        FormatManager formatManager = new FormatManager(args);

        // Check if user needs help
        if (options == null || options.has(HELP_ABBREVIATED) || options.has(HELP)) {
            Helpable.printHelp("Application options", PARSER);
            engineManager.printHelp();
            formatManager.printHelp();
            return;
        }
        Map<String, String> parameterMap = splitParameters(options, PARAMETER);
        run((File) options.valueOf(TEST_SUITE), parameterMap, parseManager, engineManager, formatManager);
    }
}
