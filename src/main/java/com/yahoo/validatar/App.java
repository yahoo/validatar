/*
 * Copyright 2015 Yahoo Inc.
 * Licensed under the terms of the Apache 2 license. Please see LICENSE file in the project root for terms.
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
     * @param options Option set
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
     * @param testSuite The {@link File} where the TestSuite(s) are.
     * @param parameters An optional {@link Map} of parameters to their values to expand.
     * @param parseManager A {@link ParseManager} to use.
     * @param engineManager A {@link EngineManager} to use.
     * @param formatManager A {@link FormatManager} to use.
     * @return A boolean denoting whether all {@link Query} or {@link Test} passed.
     * @throws IOException if any.
     */
    public static boolean run(File testSuite, Map<String, String> parameters, ParseManager parseManager,
                              EngineManager engineManager, FormatManager formatManager) throws IOException {
        // Load the test suite file(s)
        log.info("Parsing test files...");
        List<TestSuite> suites = parseManager.load(testSuite);
        log.info("Expanding parameters...");
        ParseManager.deParametrize(suites, parameters);

        // Get the non-null queries
        List<Query> queries = suites.stream().map(s -> s.queries).filter(Objects::nonNull)
                                    .flatMap(Collection::stream).filter(Objects::nonNull).collect(Collectors.toList());

        // Run the queries
        log.info("Running queries...");
        if (!engineManager.run(queries)) {
            log.error("Error running queries. Failing...");
            return false;
        }

        // Get the non-null query results
        List<Result> data = queries.stream().map(Query::getResult).filter(Objects::nonNull).collect(Collectors.toList());

        // Get the non-null tests
        List<Test> tests = suites.stream().map(s -> s.tests).filter(Objects::nonNull)
                                          .flatMap(Collection::stream).filter(Objects::nonNull)
                                          .collect(Collectors.toList());

        // Run the tests
        log.info("Running tests...");
        Assertor.assertAll(data, tests);

        // Write reports
        log.info("Writing reports...");
        formatManager.writeReport(suites);
        log.info("Done!");

        return tests.stream().allMatch(Test::passed) && queries.stream().noneMatch(Query::failed);
    }

    /**
     * Runs Validatar with the given args.
     *
     * @param args The String arguments to Validatar.
     * @return A boolean denoting whether all tests or queries passed.
     * @throws IOException if any.
     */
    public static boolean run(String[] args) throws IOException {
        // Parse CLI args
        OptionSet options = parse(args);

        ParseManager parseManager = new ParseManager(args);
        EngineManager engineManager = new EngineManager(args);
        FormatManager formatManager = new FormatManager(args);

        // Check if user needs help
        if (options == null || options.has(HELP_ABBREVIATED) || options.has(HELP)) {
            Helpable.printHelp("Application options", PARSER);
            parseManager.printHelp();
            engineManager.printHelp();
            formatManager.printHelp();
            return true;
        }
        Map<String, String> parameterMap = splitParameters(options, PARAMETER);

        return run((File) options.valueOf(TEST_SUITE), parameterMap, parseManager, engineManager, formatManager);
    }

    /**
     * Main.
     *
     * @param args The input arguments.
     * @throws IOException if any.
     */
    public static void main(String[] args) throws IOException  {
        System.exit(run(args) ? 0 : 1);
    }
}
