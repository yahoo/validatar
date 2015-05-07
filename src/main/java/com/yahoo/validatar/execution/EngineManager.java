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

import org.apache.log4j.Logger;

import java.util.Map;
import java.util.HashMap;
import java.util.List;
import java.util.ArrayList;
import java.util.Set;
import java.util.HashSet;

import org.reflections.Reflections;

/**
 * Manages the creation and execution of execution engines.
 */
public class EngineManager {
    /** A simple wrapper to mark an engine as started. */
    protected class WorkingEngine {
        public boolean isStarted = false;
        private Engine engine = null;

        /** Constructor. */
        public WorkingEngine(Engine engine) {
            this.engine = engine;
        }

        /** Getter. */
        public Engine getEngine() {
            return this.engine;
        }
    }

    /** The delimiter between the query name and the column name in a query result. */
    public static final String NAMESPACE_SEPARATOR = ".";

    /** Manages logging. */
    protected final Logger log = Logger.getLogger(getClass());

    /** Stores the CLI arguments. */
    protected String[] arguments;

    /** Stores engine names to engine references. */
    protected Map<String, WorkingEngine> engines;

    /**
     * Default no argument constructor.
     */
    public EngineManager() {
    }

    /**
     * Store arguments and create the engine map.
     *
     * @param arguments CLI arguments.
     */
    public EngineManager(String[] arguments) {
        this.arguments = arguments;

        // Create the engines map, engine name -> engine
        engines = new HashMap<String, WorkingEngine>();
        Reflections reflections = new Reflections("com.yahoo.validatar.execution");
        Set<Class<? extends Engine>> subTypes = reflections.getSubTypesOf(Engine.class);
        for (java.lang.Class<? extends com.yahoo.validatar.execution.Engine> engineClass : subTypes) {
            Engine engine = null;
            try {
                engine = engineClass.newInstance();
                engines.put(engine.getName(), new WorkingEngine(engine));
                log.debug("Added engine " + engine.getName() + " to list of engines.");
            } catch (InstantiationException e) {
                log.error("Error instantiating " + ((engine == null) ? engineClass : engine.getName()) + " engine.", e);
            } catch (IllegalAccessException e) {
                log.error("Illegal access while loading " + ((engine == null) ? engineClass : engine.getName()) + " engine.", e);
            }
        }
    }

    /**
     * For testing purposes to inject engines. Will always override existing engines.
     * @param enginesToUse A list of engines to use as the engines to work with.
     */
    protected void setEngines(List<Engine> enginesToUse) {
        engines = new HashMap<String, WorkingEngine>();
        if (enginesToUse == null) {
            return;
        }
        for (Engine engine : enginesToUse) {
            engines.put(engine.getName(), new WorkingEngine(engine));
        }
    }

    /**
     * Returns a set of the distinct engines given a list of queries.
     *
     * @param queries List of queries.
     * @return Set of names of distinct required engines.
     */
    private Set<String> distinctEngines(List<Query> queries) {
        Set<String> requiredEngines = new HashSet<String>();
        if (queries != null) {
            for (Query query : queries) {
                requiredEngines.add(query.engine);
            }
        }
        return requiredEngines;
    }

    /**
     * For a list of queries, start corresponding engines.
     *
     * @param queries Queries to check for engine support.
     * @return true iff the required engines were loaded.
     */
    protected boolean startEngines(List<Query> queries) {
        // Stores list of errors when starting engine, if any.
        List<String> errors = new ArrayList<String>();

        // Get the set of all required engines.
        Set<String> requiredEngines = distinctEngines(queries);

        boolean allSetup = true;

        // Check that each required engine has been loaded.
        for (String engine : requiredEngines) {
            WorkingEngine workingEngine = engines.get(engine);
            if (workingEngine == null) {
                log.error("Engine " + engine + " not loaded but required by query.");
                allSetup = false;
                continue;
            } else if (!workingEngine.isStarted) {
                workingEngine.isStarted = workingEngine.getEngine().setup(arguments);
                if (!workingEngine.isStarted) {
                    log.error("Required engine " + engine + " could not be setup.");
                    workingEngine.getEngine().printHelp();
                }
                allSetup &= workingEngine.isStarted;
            }
        }
        return allSetup;
    }

    /**
     * For a query, namespace the query results so that column names are not ambiguous
     * across queries.
     *
     * @param query The Query object.
     */
    protected void nameSpaceResults(Query query) {
        if (query.getResults() == null) {
            return;
        }
        Map<String, List<String>> namespacedResults = new HashMap<String, List<String>>();
        for (Map.Entry<String, List<String>> column : query.getResults().entrySet()) {
            namespacedResults.put(query.name + NAMESPACE_SEPARATOR + column.getKey(), column.getValue());
        }
        query.setResults(namespacedResults);
    }

    /**
     * Prints the help message for each engine.
     */
    public void printHelp() {
        for (WorkingEngine entry : engines.values()) {
            entry.getEngine().printHelp();
        }
    }

    /**
     * Run a query and store the results in the query object.
     *
     * @param queries Queries to execute and store.
     * @return true iff the required engines were loaded and the queries were able to run.
     */
    public boolean run(List<Query> queries) {
        if (!startEngines(queries)) {
            return false;
        }

        // Run each query.
        for (Query query : queries) {
            engines.get(query.engine).getEngine().execute(query);
            nameSpaceResults(query);
        }

        return true;
    }
}
