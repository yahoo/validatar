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

/**
 * This class can be used by any that wish to store metadata, maintain messages
 * and mark failure.
 */
public abstract class Metadata {
    protected boolean failed = false;
    protected List<String> messages = null;
    protected Map<String, String> metadata = null;

    /**
     * Get messages.
     */
    public List<String> getMessages() {
        return messages;
    }

    /**
     * Add a message.
     */
    public void addMessage(String message) {
        if (this.messages == null) {
            this.messages = new ArrayList<String>();
        }
        this.messages.add(message);
    }

    /**
     * Set failure status.
     */
    public void setFailed() {
        this.failed = true;
    }

    /**
     * Set success status.
     */
    public void setSuccess() {
        this.failed = false;
    }

    /**
     * True iff in the failure status.
     */
    public boolean failed() {
        return failed == true;
    }

    /**
     * Set any key, value metadata. This will be added to the Query's metadata.
     */
    public void addMetadata(Map<String, String> metadata) {
        if (this.metadata == null) {
            this.metadata = new HashMap<String, String>();
        }
        this.metadata.putAll(metadata);
    }

    /**
     * Get metadata.
     */
    public Map<String, String> getMetadata() {
        return this.metadata;
    }
}
