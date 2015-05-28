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

package com.yahoo.validatar.report;

import com.yahoo.validatar.common.TestSuite;

import org.apache.log4j.Logger;

import joptsimple.OptionParser;
import static java.util.Arrays.*;
import java.util.Set;
import java.util.List;
import java.util.Map;
import java.util.HashMap;
import java.io.IOException;

import org.reflections.Reflections;

/**
 * Manages the writing of test reports.
 */
public class FormatManager {
    /** Used for logging. */
    protected Logger log = Logger.getLogger(getClass().getName());

    private Map<String, Formatter> availableFormatters;
    private Formatter formatterToUse = null;

    // Leaving it here for now. If new formatters that require more complex options are needed,
    // it can be moved to inside the respective formatters.
    private OptionParser parser = new OptionParser() {
        {
            acceptsAll(asList("report-format"), "Which report format to use.")
                .withRequiredArg()
                .describedAs("Report format")
                .defaultsTo("junit");
            allowsUnrecognizedOptions();
        }
    };

    /**
     * Setups the format engine using the input parameters.
     * @param arguments An array of parameters of the form [--param1 value1 --param2 value2...]
     */
    public FormatManager(String[] arguments) {
        String name = (String) parser.parse(arguments).valueOf("report-format");
        formatterToUse = findFormatter(name, arguments);
        if (formatterToUse == null) {
            printHelp();
            throw new RuntimeException("Could not find a formatter to use");
        }
        if (!formatterToUse.setup(arguments)) {
            formatterToUse.printHelp();
            throw new RuntimeException("Could not initialize the requested formatter");
        }
    }

    /**
     * For testing purposes. Explicitly set the formatter to use.
     * @param formatter The formatter to use.
     */
    protected void setFormatterToUse(Formatter formatter) {
        formatterToUse = formatter;
    }

    private Formatter findFormatter(String name, String[] arguments) {
        Reflections reflections = new Reflections("com.yahoo.validatar.report");
        Set<Class<? extends Formatter>> subTypes = reflections.getSubTypesOf(Formatter.class);
        availableFormatters = new HashMap<String, Formatter>();
        Formatter searchingFor = null;
        for (Class<? extends Formatter> formatterClass : subTypes) {
            try {
                Formatter formatter = formatterClass.newInstance();
                if (name.equals(formatter.getName())) {
                    searchingFor = formatter;
                }
                availableFormatters.put(formatter.getName(), formatter);
                log.info("Setup formatter " + formatter.getName());
            } catch (InstantiationException e) {
                log.info("Error instantiating " + formatterClass + " " + e);
            } catch (IllegalAccessException e) {
                log.info("Illegal access of " + formatterClass + " " + e);
            }
        }
        return searchingFor;
    }

    /**
     * Write out a list of TestSuites.
     * @param testSuites List of test suites.
     */
    public void writeReport(List<TestSuite> testSuites) throws IOException {
        formatterToUse.writeReport(testSuites);
    }

    /**
     * Print help for all available formatters on System.out.
     */
    public void printHelp() {
        System.out.println();
        try {
            parser.printHelpOn(System.out);
            for (Map.Entry<String, Formatter> entry : availableFormatters.entrySet()) {
                System.out.println(entry.getKey());
                entry.getValue().printHelp();
            }
        } catch (IOException ioe) {
            log.error(ioe);
            throw new RuntimeException("Could not print help for formatter");
        }
        System.out.println();
    }
}