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

import com.yahoo.validatar.common.Helpable;
import com.yahoo.validatar.common.Query;
import org.apache.log4j.Logger;
import org.reflections.Reflections;

import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

/**
 * Manages the creation and execution of execution engines.
 */
public class EngineManager implements Helpable {
    /**
     * A simple wrapper to mark an engine as started.
     */
    protected class WorkingEngine {
        public boolean isStarted = false;
        private Engine engine = null;

        /**
         * Constructor.
         *
         * @param engine The engine to wrap.
         */
        public WorkingEngine(Engine engine) {
            this.engine = engine;
        }

        /**
         * Getter.
         *
         * @return The wrapped Engine.
         */
        public Engine getEngine() {
            return this.engine;
        }
    }

    /**
     * Manages logging.
     */
    protected final Logger log = Logger.getLogger(getClass());

    /**
     * Stores the CLI arguments.
     */
    protected String[] arguments;

    /**
     * Stores engine names to engine references.
     */
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
        engines = new HashMap<>();
        Reflections reflections = new Reflections("com.yahoo.validatar.execution");
        Set<Class<? extends Engine>> subTypes = reflections.getSubTypesOf(Engine.class);
        for (java.lang.Class<? extends com.yahoo.validatar.execution.Engine> engineClass : subTypes) {
            Engine engine = null;
            try {
                engine = engineClass.newInstance();
                engines.put(engine.getName(), new WorkingEngine(engine));
                log.debug("Added engine " + engine.getName() + " to list of engines.");
            } catch (InstantiationException e) {
                log.error("Error instantiating " + engineClass + " engine.", e);
            } catch (IllegalAccessException e) {
                log.error("Illegal access while loading " + engineClass + " engine.", e);
            }
        }
    }

    /**
     * For testing purposes to inject engines. Will always override existing engines.
     *
     * @param engines A list of engines to use as the engines to work with.
     */
    protected void setEngines(List<Engine> engines) {
        List<Engine> all = engines == null ? Collections.emptyList() : engines;
        this.engines = all.stream().collect(Collectors.toMap(Engine::getName, WorkingEngine::new));
    }

    /**
     * For a list of queries, start corresponding engines.
     *
     * @param queries Queries to check for engine support.
     * @return true iff the required engines were loaded.
     */
    protected boolean startEngines(List<Query> queries) {
        List<Query> all = queries == null ? Collections.emptyList() : queries;
        // Queries -> engine name Set -> start engine -> verify all started
        return all.stream().map(q -> q.engine).collect(Collectors.toSet())
               .stream().map(this::startEngine)
               .allMatch(b -> b);
    }

    private boolean startEngine(String engine) {
        WorkingEngine working = engines.get(engine);
        if (working == null) {
            log.error("Engine " + engine + " not loaded but required by query.");
            return false;
        }
        // Already started?
        if (working.isStarted) {
            return true;
        }
        working.isStarted = working.getEngine().setup(arguments);
        if (!working.isStarted) {
            log.error("Required engine " + engine + " could not be setup.");
            working.getEngine().printHelp();
            return false;
        }
        return true;
    }

    /**
     * {@inheritDoc}
     */
    public void printHelp() {
        engines.values().stream().map(WorkingEngine::getEngine).forEach(Engine::printHelp);
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
        queries.stream().forEach(query -> engines.get(query.engine).getEngine().execute(query));
        return true;
    }
}
