/*
 * Copyright 2015 Yahoo Inc.
 * Licensed under the terms of the Apache 2 license. Please see LICENSE file in the project root for terms.
 */
package com.yahoo.validatar.report;

import com.yahoo.validatar.common.Helpable;
import com.yahoo.validatar.common.Pluggable;
import com.yahoo.validatar.common.TestSuite;
import com.yahoo.validatar.report.email.EmailFormatter;
import com.yahoo.validatar.report.junit.JUnitFormatter;
import joptsimple.OptionParser;
import joptsimple.OptionSet;
import lombok.extern.slf4j.Slf4j;

import java.io.IOException;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import static java.util.Collections.singletonList;

/**
 * Manages the writing of test reports.
 */
@Slf4j
public class FormatManager extends Pluggable<Formatter> implements Helpable {
    public static final String CUSTOM_FORMATTER = "custom-formatter";
    public static final String CUSTOM_FORMATTER_DESCRIPTION = "Additional custom formatter to load.";

    /**
     * The Parser classes to manage.
     */
    public static final List<Class<? extends Formatter>> MANAGED_FORMATTERS = Arrays.asList(
        JUnitFormatter.class, EmailFormatter.class
    );
    public static final String REPORT_FORMAT = "report-format";
    public static final String REPORT_ONLY_ON_FAILURE = "report-on-failure-only";

    private Map<String, Formatter> availableFormatters;
    private Formatter formatterToUse = null;
    private boolean onlyOnFailure = false;

    public static final String JUNIT = "junit";
    // Leaving it here for now. If new formatters that require more complex options are needed,
    // it can be moved to inside the respective formatters.
    private static final OptionParser PARSER = new OptionParser() {
        {
            acceptsAll(singletonList(REPORT_FORMAT), "Which report format to use.")
                .withRequiredArg()
                .describedAs("Report format")
                .defaultsTo(JUNIT);
            acceptsAll(singletonList(REPORT_ONLY_ON_FAILURE), "Should the reporter be only run on failure.")
                .withRequiredArg()
                .describedAs("Report on failure")
                .ofType(Boolean.class)
                .defaultsTo(false);

            allowsUnrecognizedOptions();
        }
    };

    /**
     * Setups the format engine using the input parameters.
     *
     * @param arguments An array of parameters of the form [--param1 value1 --param2 value2...]
     */
    public FormatManager(String[] arguments) {
        super(MANAGED_FORMATTERS, CUSTOM_FORMATTER, CUSTOM_FORMATTER_DESCRIPTION);

        availableFormatters = new HashMap<>();
        for (Formatter formatter : getPlugins(arguments)) {
            availableFormatters.put(formatter.getName(), formatter);
            log.info("Setup formatter {}", formatter.getName());
        }

        OptionSet parser = PARSER.parse(arguments);
        onlyOnFailure = (Boolean) parser.valueOf(REPORT_ONLY_ON_FAILURE);
        String name = (String) parser.valueOf(REPORT_FORMAT);
        setupFormatter(name, arguments);
    }

    /**
     * For testing purposes only. Pick the formatter to use.
     *
     * @param name The name of the formatter to setup
     */
    void setupFormatter(String name, String[] arguments) {
        formatterToUse = availableFormatters.get(name);

        if (formatterToUse == null) {
            printHelp();
            throw new NullPointerException("Could not find a formatter to use");
        }

        if (!formatterToUse.setup(arguments)) {
            formatterToUse.printHelp();
            throw new RuntimeException("Could not initialize the requested formatter");
        }
    }

    /**
     * For testing purposes only. Explicitly set the formatters.
     *
     * @param formatters The map of names to formatters to use.
     */
    protected void setAvailableFormatters(Map<String, Formatter> formatters) {
        availableFormatters = formatters;
    }

    /**
     * Write out a list of TestSuites unless this was configured to write out only on failures.
     *
     * @param testSuites List of test suites.
     * @throws java.io.IOException if any.
     */
    public void writeReport(List<TestSuite> testSuites) throws IOException {
        // Do nothing if we wanted to write out only on failures and we had no failures.
        if (onlyOnFailure && testSuites.stream().noneMatch(TestSuite::hasFailures)) {
            log.warn("Reports should be generated only on failure. Skipping report since there were no failures.");
            return;
        }
        formatterToUse.writeReport(testSuites);
    }

    @Override
    public void printHelp() {
        Helpable.printHelp("Reporting options", PARSER);
        availableFormatters.values().stream().forEach(Formatter::printHelp);
        Helpable.printHelp("Advanced Reporting Options", getPluginOptionsParser());
    }
}
