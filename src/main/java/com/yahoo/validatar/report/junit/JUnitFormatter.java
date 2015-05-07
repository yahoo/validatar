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

package com.yahoo.validatar.report.junit;

import com.yahoo.validatar.common.TestSuite;
import com.yahoo.validatar.common.Test;
import com.yahoo.validatar.common.Query;
import com.yahoo.validatar.report.Formatter;

import java.io.FileWriter;
import java.io.IOException;

import org.dom4j.Document;
import org.dom4j.DocumentHelper;
import org.dom4j.Element;
import org.dom4j.io.XMLWriter;
import org.dom4j.io.OutputFormat;

import joptsimple.OptionParser;

import org.apache.commons.lang3.StringUtils;

import java.util.List;
import static java.util.Arrays.*;

import org.apache.log4j.Logger;

public class JUnitFormatter implements Formatter {
    protected final Logger log = Logger.getLogger(getClass());

    public static final String JUNIT = "junit";

    private OptionParser parser = new OptionParser() {
        {
            acceptsAll(asList("report-file"), "File to store the test reports.")
                .withRequiredArg()
                .describedAs("Report file")
                .defaultsTo("report.xml");
            allowsUnrecognizedOptions();
        }
    };
    private String outputFile;

    @Override
    public boolean setup(String[] arguments) {
        outputFile = (String) parser.parse(arguments).valueOf("report-file");
        return true;
    }

    /**
     * TODO document.
     */
    @Override
    public void writeReport(List<TestSuite> testSuites) throws IOException {
        Document document = DocumentHelper.createDocument();

        Element testSuitesRoot = document.addElement("testsuites");

        // Output for each test suite
        for (TestSuite testSuite : testSuites) {
            Element testSuiteRoot = testSuitesRoot.addElement("testsuite")
                                                  .addAttribute("tests", Integer.toString(testSuite.queries.size() + testSuite.tests.size()))
                                                  .addAttribute("name", testSuite.name);

            for (Query query: testSuite.queries) {
                Element queryNode = testSuiteRoot.addElement("testcase")
                                                 .addAttribute("name", query.name);
                if (query.failed()) {
                    String failureMessage = query.getFailedMessage();
                    queryNode.addElement("failed")
                             .addText(failureMessage);
                }
            }
            for (Test test: testSuite.tests) {
                Element testNode = testSuiteRoot.addElement("testcase")
                                                .addAttribute("name", test.name);
                if (test.failed()) {
                    String failedAsserts = StringUtils.join(test.getFailedAssertMessage(), ", ");
                    String failureMessage = "Description: " + test.description + ";\n" +
                                            "Failed asserts: " + failedAsserts + "\n";
                    testNode.addElement("failed")
                            .addText(failureMessage);
                }
            }
        }

        OutputFormat format = OutputFormat.createPrettyPrint();
        XMLWriter writer = new XMLWriter(new FileWriter(outputFile), format);
        writer.write(document);
        writer.close();
    }

    @Override
    public void printHelp() {
        try {
            parser.printHelpOn(System.out);
        } catch (IOException e) {
            log.error(e);
        }
    }

    @Override
    public String getName() {
        return JUNIT;
    }

}
