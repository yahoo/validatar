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
import org.testng.Assert;
import org.testng.annotations.Test;

import java.io.FileNotFoundException;
import java.io.IOException;
import java.util.Collections;
import java.util.List;

import static com.yahoo.validatar.OutputCaptor.runWithoutOutput;

public class FormatManagerTest {
    // Used for tests
    private class MockFormatter implements Formatter {
        public boolean wroteReport = false;

        @Override
        public boolean setup(String[] arguments) {
            return true;
        }

        @Override
        public void printHelp() {
        }

        @Override
        public void writeReport(List<TestSuite> testSuites) throws IOException {
            wroteReport = true;
        }

        @Override
        public String getName() {
            return "MockFormat";
        }
    }

    public static class FailingFormatter implements Formatter {
        public FailingFormatter() {
        }

        @Override
        public boolean setup(String[] arguments) {
            return false;
        }

        @Override
        public void printHelp() {
        }

        @Override
        public void writeReport(List<TestSuite> testSuites) throws IOException {
        }

        @Override
        public String getName() {
            return "FailingFormat";
        }
    }

    @Test(expectedExceptions = {NullPointerException.class})
    public void testConstructorAndFindFormatterExceptionNoFormatterFound() throws FileNotFoundException {
        String[] args = {"--report-format", "INVALID"};
        runWithoutOutput(() -> new FormatManager(args));
    }

    @Test(expectedExceptions = {RuntimeException.class})
    public void testNullFormatter() {
        String[] args = {};
        FormatManager manager = new FormatManager(args);
        FailingFormatter formatter = new FailingFormatter();
        manager.setAvailableFormatters(Collections.singletonMap("fail", formatter));
        runWithoutOutput(() -> manager.setupFormatter("fail", args));
    }

    @Test
    public void testWriteReport() throws IOException {
        String[] args = {};
        FormatManager manager = new FormatManager(args);
        MockFormatter formatter = new MockFormatter();
        manager.setAvailableFormatters(Collections.singletonMap("MockFormat", formatter));
        manager.setupFormatter("MockFormat", args);
        manager.writeReport(null);
        Assert.assertTrue(formatter.wroteReport);
    }
}
