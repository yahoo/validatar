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

import com.yahoo.validatar.common.Helpable;
import com.yahoo.validatar.common.TestSuite;
import joptsimple.OptionParser;
import lombok.extern.slf4j.Slf4j;
import org.reflections.Reflections;

import java.io.IOException;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

import static java.util.Collections.singletonList;

/**
 * Manages the writing of test reports.
 */
@Slf4j
public class FormatManager implements Helpable {
    public static final String REPORT_FORMAT = "report-format";

    private Map<String, Formatter> availableFormatters;
    private Formatter formatterToUse = null;

    // Leaving it here for now. If new formatters that require more complex options are needed,
    // it can be moved to inside the respective formatters.
    private final OptionParser parser = new OptionParser() {
        {
            acceptsAll(singletonList(REPORT_FORMAT), "Which report format to use.")
                .withRequiredArg()
                .describedAs("Report format")
                .defaultsTo("junit");
            allowsUnrecognizedOptions();
        }
    };

    /**
     * Setups the format engine using the input parameters.
     *
     * @param arguments An array of parameters of the form [--param1 value1 --param2 value2...]
     */
    public FormatManager(String[] arguments) {
        loadFormatters();
        String name = (String) parser.parse(arguments).valueOf(REPORT_FORMAT);
        formatterToUse = availableFormatters.get(name);

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
     *
     * @param formatter The formatter to use.
     */
    protected void setFormatterToUse(Formatter formatter) {
        formatterToUse = formatter;
    }

    private void loadFormatters() {
        Reflections reflections = new Reflections("com.yahoo.validatar.report");
        Set<Class<? extends Formatter>> subTypes = reflections.getSubTypesOf(Formatter.class);
        availableFormatters = new HashMap<>();
        for (Class<? extends Formatter> formatterClass : subTypes) {
            try {
                Formatter formatter = formatterClass.newInstance();
                availableFormatters.put(formatter.getName(), formatter);
                log.info("Setup formatter {}", formatter.getName());
            } catch (InstantiationException e) {
                log.info("Error instantiating {}\n{}", formatterClass, e);
            } catch (IllegalAccessException e) {
                log.info("Illegal access of {}\n{}", formatterClass, e);
            }
        }
    }

    /**
     * Write out a list of TestSuites.
     *
     * @param testSuites List of test suites.
     * @throws java.io.IOException if any.
     */
    public void writeReport(List<TestSuite> testSuites) throws IOException {
        formatterToUse.writeReport(testSuites);
    }

    @Override
    public void printHelp() {
        Helpable.printHelp("Reporting options", parser);
        availableFormatters.values().stream().forEach(Formatter::printHelp);
    }
}
