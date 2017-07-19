/*
 * Copyright 2015 Yahoo Inc.
 * Licensed under the terms of the Apache 2 license. Please see LICENSE file in the project root for terms.
 */
package com.yahoo.validatar.execution;

import com.yahoo.validatar.OutputCaptor;
import com.yahoo.validatar.common.Column;
import com.yahoo.validatar.common.Query;
import com.yahoo.validatar.common.Result;
import com.yahoo.validatar.common.TypeSystem;
import com.yahoo.validatar.common.TypedObject;
import org.testng.Assert;
import org.testng.annotations.AfterMethod;
import org.testng.annotations.BeforeMethod;
import org.testng.annotations.Test;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;


public class EngineManagerTest extends OutputCaptor {

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
        public int timesStarted = 0;

        @Override
        public boolean setup(String[] arguments) {
            timesStarted++;
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
            results.addColumn("a");
            results.addColumnRow("a", new TypedObject("42", TypeSystem.Type.STRING));
            results.addColumn("b", Arrays.asList(new TypedObject("42", TypeSystem.Type.STRING),
                                                 new TypedObject("52", TypeSystem.Type.STRING)));
        }

        @Override
        public String getName() {
            return ENGINE_NAME;
        }
    }

    private class MockExplodingEngine implements Engine {
        public static final String ENGINE_NAME = "EXPLODER";

        @Override
        public boolean setup(String[] arguments) {
            return true;
        }

        @Override
        public void printHelp() {
        }

        @Override
        public void execute(Query query) {
            throw new RuntimeException("Boom");
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
        setupMockedAppender();
        query = new Query();
        query.engine = MockPassingEngine.ENGINE_NAME;
        queries = new ArrayList<>();
        queries.add(query);
        engines = new ArrayList<>();
        engines.add(new MockFailingEngine());
        engines.add(new MockPassingEngine());
        engines.add(new MockRunningEngine());
        String[] args = {};
        manager = new EngineManager(args);
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
    public void testUnhandledBadQueryExecution() {
        engines.add(new MockExplodingEngine());
        manager.setEngines(engines);
        Query bad = new Query();
        bad.engine = MockExplodingEngine.ENGINE_NAME;
        queries.add(bad);
        Assert.assertTrue(manager.run(queries));
        Assert.assertFalse(queries.get(0).failed());
        Assert.assertTrue(queries.get(1).failed());
        Assert.assertTrue(queries.get(1).getMessages().get(0).contains("Boom"));
    }

    @Test
    public void testPrintHelp() {
        manager.setEngines(engines);
        runWithoutOutput(() -> manager.printHelp());
        MockPassingEngine engine = (MockPassingEngine) engines.get(1);
        Assert.assertTrue(engine.helpPrinted);
    }

    @Test
    public void testNotStartingEngineIfStarted() {
        query.engine = MockPassingEngine.ENGINE_NAME;
        manager.setEngines(engines);
        manager.startEngines(queries);
        manager.startEngines(queries);
        MockPassingEngine startedEngine = (MockPassingEngine) engines.stream()
                                          .filter(e -> MockPassingEngine.ENGINE_NAME.equals(e.getName()))
                                          .findFirst().get();
        Assert.assertEquals(startedEngine.timesStarted, 1);
    }

    @Test
    public void testFailRun() {
        query.engine = MockFailingEngine.ENGINE_NAME;
        manager.setEngines(engines);
        Assert.assertFalse(manager.run(queries));
    }

    @Test
    public void testNormalNoResults() {
        query.engine = MockPassingEngine.ENGINE_NAME;
        manager.setEngines(engines);
        Assert.assertTrue(manager.run(queries));
        Assert.assertEquals(query.getResult().getColumns().size(), 0);
    }

    @Test
    public void testNormalRun() {
        query.engine = MockRunningEngine.ENGINE_NAME;
        query.name = "Foo";
        manager.setEngines(engines);

        Assert.assertTrue(manager.run(queries));

        Map<String, List<TypedObject>> expected = new HashMap<>();
        List<TypedObject> columns = new ArrayList<>();
        columns.add(new TypedObject("42", TypeSystem.Type.STRING));
        expected.put("Foo.a", columns);
        columns = new ArrayList<>();
        columns.add(new TypedObject("42", TypeSystem.Type.STRING));
        columns.add(new TypedObject("52", TypeSystem.Type.STRING));
        expected.put("Foo.b", columns);

        Map<String, Column> actual = query.getResult().getColumns();

        Assert.assertEquals(actual.size(), 2);
        Assert.assertEquals(expected.size(), 2);
        Assert.assertEquals(actual.get("Foo.a").size(), 1);
        Assert.assertEquals(expected.get("Foo.a").size(), 1);
        Assert.assertEquals((String) actual.get("Foo.a").get(0).data, (String) expected.get("Foo.a").get(0).data);
        Assert.assertEquals(actual.get("Foo.b").size(), 2);
        Assert.assertEquals(expected.get("Foo.b").size(), 2);
        Assert.assertEquals((String) actual.get("Foo.b").get(0).data, (String) expected.get("Foo.b").get(0).data);
        Assert.assertEquals((String) actual.get("Foo.b").get(1).data, (String) expected.get("Foo.b").get(1).data);
    }
}
