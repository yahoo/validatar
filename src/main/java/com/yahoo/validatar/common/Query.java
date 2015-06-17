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

public class Query extends Executable {
    public String name;
    public String engine;
    public String value;
    public List<Metadata> metadata;

    private Map<String, List<String>> results = null;

    /**
     * Add a failure message and mark as failed.
     */
    public void setFailure(String failedMessage) {
        setFailed();
        addMessage(failedMessage);
    }

    /**
     * Store the results of the query.
     */
    public void setResults(Map<String, List<String>> results) {
        this.results = results;
    }

    /**
     * Get the results of the query.
     */
    public Map<String, List<String>> getResults() {
        return results;
    }

    /**
     * Returns the metadata list flattened into a map. If there are metadata with
     * the same key, the last one is one that is kept.
     */
    public Map<String, String> getMetadata() {
        if (metadata == null) {
            return null;
        }
        Map<String, String> metas = new HashMap<>();
        for (Metadata meta : metadata) {
            metas.put(meta.key, meta.value);
        }
        return metas;
    }
}
