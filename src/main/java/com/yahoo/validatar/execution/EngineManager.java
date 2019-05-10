/*
 * Copyright 2015 Yahoo Inc.
 * Licensed under the terms of the Apache 2 license. Please see LICENSE file in the project root for terms.
 */
package com.yahoo.validatar.execution;

import com.yahoo.validatar.common.Helpable;
import com.yahoo.validatar.common.Pluggable;
import com.yahoo.validatar.common.Query;
import com.yahoo.validatar.execution.fixed.DSV;
import com.yahoo.validatar.execution.hive.Apiary;
import com.yahoo.validatar.execution.pig.Sty;
import com.yahoo.validatar.execution.rest.JSON;
import lombok.extern.slf4j.Slf4j;

import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

/**
 * Manages the creation and execution of execution engines.
 */
@Slf4j
public class EngineManager extends Pluggable<Engine> implements Helpable {
    public static final String CUSTOM_ENGINE = "custom-engine";
    public static final String CUSTOM_ENGINE_DESCRIPTION = "Additional custom engine to load.";

    /**
     * The Engine classes to manage.
     */
    public static final List<Class<? extends Engine>> MANAGED_ENGINES = Arrays.asList(Apiary.class, Sty.class, JSON.class, DSV.class);

    /**
     * Stores the CLI arguments.
     */
    protected String[] arguments;

    /**
     * Stores engine names to engine references.
     */
    protected Map<String, WorkingEngine> engines;

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
     * Store arguments and create the engine map.
     *
     * @param arguments CLI arguments.
     */
    public EngineManager(String[] arguments) {
        super(MANAGED_ENGINES, CUSTOM_ENGINE, CUSTOM_ENGINE_DESCRIPTION);

        this.arguments = arguments;

        // Create the engines map, engine name -> engine
        engines = new HashMap<>();
        for (Engine engine : getPlugins(arguments)) {
            engines.put(engine.getName(), new WorkingEngine(engine));
            log.info("Added engine {} to list of engines.", engine.getName());
        }
    }

    /**
     * For testing purposes to inject engines. Will always override existing engines.
     *
     * @param engines A list of engines to use as the engines to work with.
     */
    void setEngines(List<Engine> engines) {
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
            log.error("Engine {} not loaded but required by query.", engine);
            return false;
        }
        // Already started?
        if (working.isStarted) {
            return true;
        }
        working.isStarted = working.getEngine().setup(arguments);
        if (!working.isStarted) {
            log.error("Required engine {} could not be setup.", engine);
            working.getEngine().printHelp();
            return false;
        }
        return true;
    }

    @Override
    public void printHelp() {
        engines.values().stream().map(WorkingEngine::getEngine).forEach(Engine::printHelp);
        Helpable.printHelp("Advanced Engine Options", getPluginOptionsParser());
    }

    private void run(Query query) {
        try {
            engines.get(query.engine).getEngine().execute(query);
        } catch (Exception e) {
            query.setFailure(e.toString());
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
        queries.stream().forEach(this::run);
        return true;
    }
}
