/*
 * Copyright 2015 Yahoo Inc.
 * Licensed under the terms of the Apache 2 license. Please see LICENSE file in the project root for terms.
 */
package com.yahoo.validatar.common;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class Result {
    private final Map<String, List<TypedObject>> data = new HashMap<>();
    private String prefix = "";

    /**
     * Constructor that initializes a result with a prefix to add for each column name.
     *
     * @param prefix The prefix to add.
     */
    public Result(String prefix) {
        this.prefix = prefix;
    }

    /**
     * Default constructor with no prefix.
     */
    public Result() {
    }

    /**
     * Adds an entire set of data to the results.
     *
     * @param data The data to add to the result.
     */
    public void addColumns(Map<String, List<TypedObject>> data) {
        if (data != null) {
            data.entrySet().stream().forEach(e -> addColumn(e.getKey(), e.getValue()));
        }
    }

    /**
     * Create and add a new column to the result.
     *
     * @param name The name of the column.
     */
    public void addColumn(String name) {
        data.put(prefix + name, new ArrayList<>());
    }

    /**
     * Create and add a new column to the result with the given rows.
     *
     * @param name   The name of the column.
     * @param values The column rows.
     */
    public void addColumn(String name, List<TypedObject> values) {
        data.put(prefix + name, values);
    }

    /**
     * Add a new row to a column.
     *
     * @param name  The name of the column.
     * @param value The value to add to it.
     */
    public void addColumnRow(String name, TypedObject value) {
        if (getColumn(name) == null) {
            addColumn(name);
        }
        getColumn(name).add(value);
    }

    /**
     * Merge another result into this. Collision in names is not handled.
     * Once results are merged in, column and row operations for this
     * result will still affect only the existing columns and rows pre-merge
     * unless there were collisions.
     *
     * @param result The result to merge with.
     * @return The merged result, i.e. this.
     */
    public Result merge(Result result) {
        if (result != null) {
            data.putAll(result.data);
        }
        return this;
    }

    /**
     * Gets the column for a given column name.
     *
     * @param columnName The name of the column.
     * @return The TypeSystem.Type of the column.
     */
    public List<TypedObject> getColumn(String columnName) {
        return data.get(prefix + columnName);
    }

    /**
     * Gets the columns representing as a Map of column names to their List of values.
     *
     * @return The column viewed as a Map.
     */
    public Map<String, List<TypedObject>> getColumns() {
        return data;
    }
}
