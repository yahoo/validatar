/*
 * Copyright 2015 Yahoo Inc.
 * Licensed under the terms of the Apache 2 license. Please see LICENSE file in the project root for terms.
 */
package com.yahoo.validatar.common;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class Query extends Executable {
    public String name;
    public String engine;
    public String value;
    public List<Metadata> metadata;

    public static final String NAMESPACE_SEPARATOR = ".";

    private Result result = null;

    /**
     * Add a failure message and mark as failed.
     *
     * @param failedMessage A {@link java.lang.String} message to set.
     */
    public void setFailure(String failedMessage) {
        setFailed();
        addMessage(failedMessage);
    }

    /**
     * Initialize the results.
     *
     * @return The created {@link com.yahoo.validatar.common.Result} object.
     */
    public Result createResults() {
        result = new Result(name + NAMESPACE_SEPARATOR);
        return result;
    }

    /**
     * Get the results of the query.
     *
     * @return The {@link com.yahoo.validatar.common.Result} result object.
     */
    public Result getResult() {
        return result;
    }

    /**
     * Returns the metadata list flattened into a map. If there are metadata with
     * the same key, the last one is one that is kept.
     *
     * @return A {@link java.util.Map} view of the metadata.
     */
    public Map<String, String> getMetadata() {
        if (metadata == null) {
            return null;
        }
        Map<String, String> map = new HashMap<>();
        // default Collectors.toMap doesn't handle null values
        metadata.stream().forEach(m -> map.put(m.key, m.value));
        return map;
    }
}
