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

package com.yahoo.validatar.execution;

import com.yahoo.validatar.common.Query;
import com.yahoo.validatar.common.Result;
import com.yahoo.validatar.LogCaptor;

import java.util.Map;
import java.util.HashMap;
import java.util.List;
import java.util.ArrayList;

import org.testng.Assert;
import org.testng.annotations.Test;
import org.testng.annotations.BeforeMethod;
import org.testng.annotations.AfterMethod;

public class EngineManagerTest extends LogCaptor {

    private class MockFailingEngine implements Engine {
        public static final String ENGINE_NAME = "FAILER";

        @Override
        public boolean setup(String[] arguments) {
            return false;
        }

        @Override
        public void printHelp() {
        }

        @Override
        public void execute(Query query) {
        }

        @Override
        public String getName() {
            return ENGINE_NAME;
        }
    }

    private class MockPassingEngine implements Engine {
        public static final String ENGINE_NAME = "PASSER";
        public boolean helpPrinted = false;

        @Override
        public boolean setup(String[] arguments) {
            return true;
        }

        @Override
        public void printHelp() {
            helpPrinted = true;
        }

        @Override
        public void execute(Query query) {
            query.createResults();
        }

        @Override
        public String getName() {
            return ENGINE_NAME;
        }
    }

    private class MockRunningEngine implements Engine {
        public static final String ENGINE_NAME = "RUNNER";

        @Override
        public boolean setup(String[] arguments) {
            return true;
        }

        @Override
        public void printHelp() {
        }

        @Override
        public void execute(Query query) {
            Result results = query.createResults();
            results.addColumn("a", null);
            results.addColumnRow("a", "42");
        }

        @Override
        public String getName() {
            return ENGINE_NAME;
        }
    }

    List<Query> queries;
    List<Engine> engines;
    EngineManager manager;
    Query query;

    @BeforeMethod
    public void setup() {
        query = new Query();
        query.engine = MockPassingEngine.ENGINE_NAME;
        queries = new ArrayList<Query>();
        queries.add(query);
        engines = new ArrayList<Engine>();
        engines.add(new MockFailingEngine());
        engines.add(new MockPassingEngine());
        engines.add(new MockRunningEngine());
        String[] args = {};
        manager = new EngineManager(args);
        setupMockedAppender();
    }

    @AfterMethod
    public void teardown() {
        teardownMockedAppender();
    }

    @Test
    public void testEngineFailToStart() {
        query.engine = MockFailingEngine.ENGINE_NAME;
        manager.setEngines(engines);
        Assert.assertFalse(manager.startEngines(queries));
        Assert.assertTrue(isStringInLog("Required engine " + MockFailingEngine.ENGINE_NAME + " could not be setup"));
    }

    @Test
    public void testEngineNotPresent() {
        query.engine = "FAKE_ENGINE";
        manager.setEngines(engines);
        Assert.assertFalse(manager.startEngines(queries));
        Assert.assertTrue(isStringInLog("Engine FAKE_ENGINE not loaded but required by query"));
    }

    @Test
    public void testEngineNullQueriesNotNull() {
        query.engine = null;
        manager.setEngines(engines);
        Assert.assertFalse(manager.startEngines(queries));
        Assert.assertTrue(isStringInLog("Engine null not loaded but required by query"));
    }

    @Test(expectedExceptions = {NullPointerException.class})
    public void testEngineNullQueryNull() {
        manager.setEngines(null);
        Assert.assertTrue(manager.run(null));
    }

    @Test
    public void testPrintHelp() {
        manager.setEngines(engines);
        manager.printHelp();
        MockPassingEngine engine = (MockPassingEngine) engines.get(1);
        Assert.assertTrue(engine.helpPrinted);
    }

    @Test
    public void testNormalNoResults() {
        query.engine = MockPassingEngine.ENGINE_NAME;
        manager.setEngines(engines);
        Assert.assertTrue(manager.run(queries));
        Assert.assertEquals(query.getResult().data.size(), 0);
    }

    @Test
    public void testNormalRun() {
        query.engine = MockRunningEngine.ENGINE_NAME;
        query.name = "Foo";
        manager.setEngines(engines);

        Assert.assertTrue(manager.run(queries));

        Map<String, List<String>> results = new HashMap<String, List<String>>();
        List<String> columns = new ArrayList<String>();
        columns.add("42");
        results.put("Foo.a", columns);
        Assert.assertEquals(query.getResult().data, results);
    }
}
