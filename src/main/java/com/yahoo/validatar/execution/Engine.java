/*
 * Copyright 2015 Yahoo Inc.
 * Licensed under the terms of the Apache 2 license. Please see LICENSE file in the project root for terms.
 */
package com.yahoo.validatar.execution;

import com.yahoo.validatar.common.Helpable;
import com.yahoo.validatar.common.Query;

public interface Engine extends Helpable {
    /**
     * Setups the engine using the input parameters.
     *
     * @param arguments An array of parameters of the form [--param1 value1 --param2 value2...]
     * @return true iff setup was succesful.
     */
    boolean setup(String[] arguments);

    /**
     * Executes the given query on this Engine and places the result into it.
     *
     * @param query The query object representing the query.
     */
    void execute(Query query);

    /**
     * Returns the name of the engine. Ex: 'Hive', 'Pig', etc.
     *
     * @return Name of the engine.
     */
    String getName();
}
