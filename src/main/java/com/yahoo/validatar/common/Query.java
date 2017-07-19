/*
 * Copyright 2015 Yahoo Inc.
 * Licensed under the terms of the Apache 2 license. Please see LICENSE file in the project root for terms.
 */
package com.yahoo.validatar.common;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

public class Query extends Executable {
    public String name;
    public String engine;
    public String value;
    public List<Metadata> metadata;

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
        result = new Result(name);
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

    /**
     * Gets a key from the metadata or returns an {@link Optional#empty()} otherwise.
     *
     * @param metadata The {@link Metadata} of a {@link Query} viewed as a {@link Map}.
     * @param key The key to get from the metadata.
     * @return The {@link Optional} value of the key.
     */
    public static Optional<String> getKey(Map<String, String> metadata, String key) {
        if (metadata == null) {
            return Optional.empty();
        }
        String value = metadata.get(key);
        return value == null || value.isEmpty() ? Optional.empty() : Optional.of(value);
    }
}
