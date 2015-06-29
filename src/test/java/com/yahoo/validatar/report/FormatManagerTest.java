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

import com.yahoo.validatar.parse.yaml.YAML;
import com.yahoo.validatar.parse.ParseManager;
import com.yahoo.validatar.common.TestSuite;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileWriter;
import java.io.IOException;
import java.io.FileDescriptor;
import java.io.FileOutputStream;
import java.io.PrintStream;

import java.nio.file.Paths;
import java.nio.file.Files;

import java.util.Map;
import java.util.HashMap;
import java.util.List;
import java.util.ArrayList;

import org.testng.Assert;
import org.testng.annotations.Test;

import org.dom4j.Document;
import org.dom4j.DocumentHelper;
import org.dom4j.util.NodeComparator;

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
            return;
        }

        @Override
        public void writeReport(List<TestSuite> testSuites) throws IOException {
            wroteReport = true;
        }

        @Override
        public String getName() {
            return "MockFormatter";
        }
    }

    @Test
    public void testConstructorAndFindFormatterExceptionNoFormatterFound() throws FileNotFoundException {
        System.setOut(new PrintStream(new FileOutputStream("target/out")));
        System.setErr(new PrintStream(new FileOutputStream("target/err")));
        String[] args = {"--report-format", "INVALID"};
        try {
            FormatManager manager = new FormatManager(args);
            Assert.fail("Should have thrown an Exception");
        } catch (RuntimeException re) {
        }
        System.setOut(new PrintStream(new FileOutputStream(FileDescriptor.out)));
        System.setErr(new PrintStream(new FileOutputStream(FileDescriptor.err)));
    }

    @Test
    public void testWriteReport() throws IOException {
        String[] args = {};
        FormatManager manager = new FormatManager(args);
        MockFormatter formatter = new MockFormatter();
        manager.setFormatterToUse(formatter);
        manager.writeReport(null);
        Assert.assertTrue(formatter.wroteReport);
    }
}
