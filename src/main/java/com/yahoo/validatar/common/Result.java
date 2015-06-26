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

package com.yahoo.validatar.common;

import java.util.Map;
import java.util.HashMap;
import java.util.List;
import java.util.ArrayList;

public class Result {
    private Map<String, List<TypedObject>> data = new HashMap<>();
    private String prefix = "";

    /**
     * Constructor that initializes a result with a prefix to add for each column name.
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
     * Create and add a new column to the result.
     * @param name The name of the column.
     */
    public void addColumn(String name) {
        data.put(prefix + name, new ArrayList<TypedObject>());
    }

    /**
     * Create and add a new column to the result with the given rows.
     * @param name The name of the column.
     * @param values The column rows.
     */
    public void addColumn(String name, List<TypedObject> values) {
        data.put(prefix + name, values);
    }

    /**
     * Add a new row to a column.
     * @param name The name of the column.
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
     * @param result The result to merge with.
     */
    public void merge(Result result) {
        data.putAll(result.data);
    }

    /**
     * Gets the column for a given column name.
     * @param columnName The name of the column.
     * @return The TypeSystem.Type of the column.
     */
    public List<TypedObject> getColumn(String columnName) {
        return data.get(prefix + columnName);
    }

    /**
     * Gets the columns representing as a Map of column names to their List of values.
     * @return The column viewed as a Map.
     */
    public Map<String, List<TypedObject>> getColumns() {
        return data;
    }
}
