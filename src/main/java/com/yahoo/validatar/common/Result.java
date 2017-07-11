/*
 * Copyright 2015 Yahoo Inc.
 * Licensed under the terms of the Apache 2 license. Please see LICENSE file in the project root for terms.
 */
package com.yahoo.validatar.common;

import lombok.Getter;
import lombok.NoArgsConstructor;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

@NoArgsConstructor
public class Result {
    private final Map<String, Column> data = new HashMap<>();
    @Getter
    private String namespace = "";

    public static final String SEPARATOR = ".";

    /**
     * Constructor that initializes a result with a namespace to add for each column name.
     *
     * @param namespace The namespace to add.
     */
    public Result(String namespace) {
        this.namespace = namespace;
    }

    /**
     * Adds an entire set of data to the results. The namespace will be added.
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
        data.put(namespace(name), new Column());
    }

    /**
     * Create and add a new column to the result with the given rows.
     *
     * @param name   The name of the column.
     * @param values The column rows.
     */
    public void addColumn(String name, List<TypedObject> values) {
        data.put(namespace(name), new Column(values));
    }

    /**
     * Add a new row to a column.
     *
     * @param name  The name of the column.
     * @param value The value to add t it.
     */
    public void addColumnRow(String name, TypedObject value) {
        if (getColumn(name) == null) {
            addColumn(name);
        }
        getColumn(name).add(value);
    }

    /**
     * Merge another result into this. Collision in names is not handled. Once results are merged in, column and
     * row operations for this result will still affect only the existing columns and rows pre-merge unless there
     * were collisions.
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
     * @return The column viewed as a {@link List}.
     */
    public Column getColumn(String columnName) {
        return data.get(namespace + columnName);
    }

    /**
     * Gets the columns representing as a {@link Map} of column names to their values as a {@link Column}.
     *
     * @return The column viewed as a Map.
     */
    public Map<String, Column> getColumns() {
        return data;
    }

    private String namespace(String name) {
        return namespace + SEPARATOR + name;
    }
}
