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
import java.util.List;
import java.util.stream.Collectors;

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
        return metadata.stream().collect(Collectors.toMap(meta -> meta.key, meta -> meta.value));
    }
}
