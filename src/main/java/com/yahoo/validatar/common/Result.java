/*
 * Copyright 2015 Yahoo Inc.
 * Licensed under the terms of the Apache 2 license. Please see LICENSE file in the project root for terms.
 */
package com.yahoo.validatar.common;

import lombok.Getter;
import lombok.extern.slf4j.Slf4j;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.Formatter;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.stream.IntStream;

/**
 * Wrapper for a columnar dataset. Stores a {@link Map} of String column names to {@link Column} column values.
 * The columns must have the same length or the behavior is undefined. The dataset is a Table or Matrix, in other words.
 *
 * If you provide a namespace for the dataset using {@link #Result(String)}, you can insert and get columns without
 * having to specify the namespace. If you do not provide a namespace, then you will have to ensure that columns you
 * operate on within the Result are unique yourself.
 *
 * Provides methods to do Cartesian Products and Joins on datasets. See {@link #cartesianProduct(List)} and
 * {@link #join(Result, Column)}.
 */
@Slf4j @Getter
public class Result {
    public static final String COMMA = ",";
    public static final String FORMAT_NEWLINE = "%n";
    public static final String EMPTY_RESULT = "";
    public static final String NULL = "null";
    private final Map<String, Column> columns;
    private String namespace = "";

    public static final String SEPARATOR = ".";
    public static final String FORMAT_VALUE = "%16s";

    /**
     * Creates an empty result containing the provided column names.
     *
     * @param names A {@link Collection} of column names.
     */
    public Result(Collection<String> names) {
        Objects.requireNonNull(names);
        columns = new HashMap<>();
        names.stream().forEach(n -> columns.put(n, new Column()));
    }

    /**
     * Creates an empty Result object.
     */
    public Result() {
        this(Collections.emptyList());
    }

    /**
     * Constructor that initializes a result with a namespace to add for each column name.
     *
     * @param namespace The non-null namespace to add.
     */
    public Result(String namespace) {
        this();
        this.namespace = namespace;
    }

    /**
     * Returns the number of rows in this result.
     *
     * @return The number of entries in each {@link Column} stored within the result.
     */
    public int numberOfRows() {
        if (columns.isEmpty()) {
            return 0;
        }
        Column someColumn = columns.values().iterator().next();
        return someColumn.size();
    }

    /**
     * Create or replace a column in the result with the given rows.
     *
     * @param name The fully qualified name of the column.
     * @param value The column rows.
     */
    public void addQualifiedColumn(String name, Column value) {
        columns.put(name, value);
    }

    /**
     * Create and add a new column to the result with the given rows.
     *
     * @param name  The name of the column.
     * @param value The column rows.
     */
    public void addColumn(String name, Column value) {
        addQualifiedColumn(namespace(name), value);
    }

    /**
     * Create and add a new column to the result with the given values.
     *
     * @param name The name of the column.
     * @param values The column rows.
     */
    public void addColumn(String name, List<TypedObject> values) {
        addColumn(name, new Column(values));
    }

    /**
     * Create and add a new column to the result.
     *
     * @param name The name of the column.
     */
    public void addColumn(String name) {
        addColumn(name, new Column());
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
     * Adds a row to the data. The names of the columns in the row will be treated as fully qualified.
     *
     * @param row A {@link Map} of fully qualified column names to their values.
     */
    public void addQualifiedRow(Map<String, TypedObject> row) {
        // Will add nulls if row does not contain all the column names.
        for (Map.Entry<String, Column> column : columns.entrySet()) {
            TypedObject value = row.get(column.getKey());
            // Create a copy of the TypedObject
            column.getValue().add(new TypedObject(value.data, value.type));
        }
    }

    /**
     * Add a new row to a column.
     *
     * @param name The name of the column.
     * @param value The value to add t it.
     */
    public void addColumnRow(String name, TypedObject value) {
        if (getColumn(name) == null) {
            addColumn(name);
        }
        getColumn(name).add(value);
    }

    /**
     * Gets the column for a given fully qualified column name.
     *
     * @param columnName The fully qualified name of the column.
     * @return The column as a {@link Column}.
     */
    public Column getQualifiedColumn(String columnName) {
        return columns.get(columnName);
    }
    /**
     * Gets the column for a given column name.
     *
     * @param columnName The name of the column.
     * @return The column as a {@link Column}.
     */
    public Column getColumn(String columnName) {
        return getQualifiedColumn(namespace(columnName));
    }

    /**
     * Returns true if the namespaced name is present in this result.
     *
     * @param name The fully qualified name for the column.
     * @return A boolean denoting whether this is a column in this result.
     */
    public boolean hasQualifiedColumn(String name) {
        return columns.containsKey(name);
    }

    /**
     * Returns true if the name of a column is present in this result.
     *
     * @param name The name for the column.
     * @return A boolean denoting whether this is a column in this result.
     */
    public boolean hasColumn(String name) {
        return hasQualifiedColumn(namespace(name));
    }

    /**
     * Returns the row values at the given zero-based index.
     *
     * @param row The index of the row to fetch.
     * @return A {@link Map} of fully qualifed column names to {@link TypedObject} which are its values at that row.
     */
    public Map<String, TypedObject> getRow(int row) {
        Map<String, TypedObject> value = new HashMap<>();
        columns.entrySet().forEach(e -> value.put(e.getKey(), e.getValue().get(row)));
        return value;
    }

    /**
     * Gets a row safely from this result even if the result is no longer a matrix.
     *
     * @param row The index of the row to fetch. Missing entries are replaced with a {@link TypedObject}
     *            with String type and the empty string value.
     * @return {@link Map} of column names to values.
     */
    public Map<String, TypedObject> getRowSafe(int row) {
        // Only for prettyprint (to diagnose the problem)
        Map<String, TypedObject> value = new HashMap<>();
        for (Map.Entry<String, Column> column : columns.entrySet()) {
            Column columnEntry = column.getValue();
            TypedObject entry;
            if (row < columnEntry.size()) {
                entry = columnEntry.get(row);
                entry = entry == null ? TypeSystem.asTypedObject(NULL) : entry;
            } else {
                entry = TypeSystem.asTypedObject(EMPTY_RESULT);
            }
            value.put(column.getKey(), entry);
        }
        return value;
    }

    private String namespace(String name) {
        return namespace == null || namespace.isEmpty() ? name : namespace + SEPARATOR + name;
    }

    /**
     * Merge another result as is into this result with its namespace. The current namespace is unaltered.
     *
     * @param result The result to merge with.
     * @return The merged result, i.e. this.
     */
    public Result merge(Result result) {
        if (result != null) {
            columns.putAll(result.columns);
        }
        return this;
    }

    @Override
    public String toString() {
        return "{ Name: " + namespace + ", Columns: " + columns.keySet() + " }";
    }

    /**
     * Pretty prints the entire result.
     *
     * @return A String representing the result.
     */
    public String prettyPrint() {
        Set<String> columnSet = columns.keySet();
        int numberOfColumns = columnSet.size();

        if (numberOfColumns == 0) {
            return EMPTY_RESULT;
        }

        StringBuilder builder = new StringBuilder();
        Formatter formatter = new Formatter(builder);
        // Pad each column as 16 width and strings
        String format = columnSet.stream().map(c -> FORMAT_VALUE).reduce((a, b) -> a + COMMA + b).get();
        // Add a platform specific newline
        format += FORMAT_NEWLINE;

        String[] columnNames = columnSet.stream().sorted().toArray(String[]::new);
        Object[] values = new Comparable[numberOfColumns];

        // Add header
        formatter.format(format, (Object[]) columnNames);
        for (int i = 0; i < numberOfRows(); ++i) {
            Map<String, TypedObject> row = getRowSafe(i);
            for (int j = 0; j < numberOfColumns; ++j) {
                values[j] = row.get(columnNames[j]).data;
            }
            formatter.format(format, values);
        }
        return builder.toString();
    }

    /**
     * Creates a full copy of the result.
     *
     * @param result The {@link Result} to copy.
     * @return A copy of the original.
     */
    public static Result copy(Result result) {
        // Already namespaced columns. So don't create a Result with a namespace.
        Result copy = new Result();
        for (Map.Entry<String, Column> column : result.getColumns().entrySet()) {
            copy.addColumn(column.getKey(), column.getValue().copy());
        }
        return copy;
    }

    /**
     * Creates a copy of the result with only the columns specified.
     *
     * @param result The {@link Result} to copy.
     * @param columnsToCopy A {@link Set} of column names (qualified) to copy.
     * @return A copy of the original with only the columns provided.
     */
    public static Result copy(Result result, Set<String> columnsToCopy) {
        Result copy = new Result();
        for (String column : columnsToCopy) {
            copy.addColumn(column, result.getQualifiedColumn(column).copy());
        }
        return copy;
    }

    /**
     * Performs a Cartesian Product of all the results.
     *
     * @param results A {@link List} of {@link Result} datasets.
     * @return A joined Result.
     */
    public static Result cartesianProduct(List<Result> results) {
        // Product is not associative if you treat the results as actual sets of ordered tuples but generally
        // A  X (B X C) = (A X B) X C = A X B X C since there is a bijection from a, (b, c) -> (a, b), c.
        // For us, it is also commutative since A X B and B X A is the same result for the purposes of our
        // columnar operations.

        if (results == null || results.isEmpty()) {
            return new Result();
        }
        // Identity: {} X B  = B
        if (results.size() == 1) {
            return copy(results.get(0));
        }

        return results.stream().reduce(Result::cartesianProduct).get();
    }

    private static Result cartesianProduct(Result currentProduct, Result target) {
        log.info("Performing a cartesian product on {} and {}", currentProduct, target);

        // Create a new Result with all the new columns
        List<String> names = new ArrayList<>();
        names.addAll(currentProduct.getColumns().keySet());
        names.addAll(target.getColumns().keySet());
        Result product = new Result(names);

        // The number of rows in currentProduct are the same for all columns in currentProduct.
        // This can be slow if there are lot of column or rows. This is the most generic and simplest to maintain
        for (int i = 0; i < currentProduct.numberOfRows(); ++i) {
            Map<String, TypedObject> row = currentProduct.getRow(i);
            // Extend the row with the new values. They will be overridden each iteration.
            for (int j = 0; j < target.numberOfRows(); ++j) {
                row.putAll(target.getRow(j));
                product.addQualifiedRow(row);
            }
        }
        return product;
    }

    /**
     * Joins a {@link Result} and a {@link Column} containing boolean TypedObjects. The result must be a matrix with
     * equal sizes for all columns and this must be the same seize as the column containing the booleans. Picks all
     * the rows for which the corresponding boolean TypedObject in the column is true.
     *
     * @param result The result to join with the column.
     * @param row The row containing booleans that has the same length as all the columns in the result.
     * @return The joined result.
     */
    public static Result join(Result result, Column row) {
        Objects.requireNonNull(result);
        Objects.requireNonNull(row);
        // Empty copy
        Result joined = new Result(result.getColumns().keySet());
        // For all row numbers that have true in the row, copy them into the result
        IntStream.range(0, row.size())
                 .filter(r -> (Boolean) row.get(r).data)
                 .forEach(i -> copyRow(result, joined, i));
        return joined;
    }

    private static void copyRow(Result source, Result target, int rowNumber) {
        Map<String, Column> sourceColumns = source.getColumns();
        Map<String, Column> targetColumns = target.getColumns();
        for (Map.Entry<String, Column> column : sourceColumns.entrySet()) {
            Column targetColumn = targetColumns.get(column.getKey());
            targetColumn.add(column.getValue().get(rowNumber));
        }
    }
}
