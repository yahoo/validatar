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
    public Map<String, List<String>> data = new HashMap<>();
    public Map<String, TypeSystem.Type> types = new HashMap<>();
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
     * Add a new column to the result.
     * @param name The name of the column.
     * @param type The type of the column.
     */
    public void addColumn(String name, TypeSystem.Type type) {
        data.put(prefix + name, new ArrayList<String>());
        types.put(prefix + name, type);
    }

    /**
     * Merge another result into this. Collision in names is not handled.
     * @param result. The result to merge with.
     */
    public void merge(Result result) {
        data.putAll(result.data);
        types.putAll(result.types);
    }

    /**
     * Add a new row to an existing column.
     * @param name The name of the column.
     * @param value The value to add to it.
     */
    public void addColumnRow(String name, String value) {
        data.get(prefix + name).add(value);
    }
}
