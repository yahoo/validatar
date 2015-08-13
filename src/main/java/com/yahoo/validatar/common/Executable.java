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

import java.util.ArrayList;
import java.util.List;

/**
 * This is an abstraction of something that can be "run" to produce something. The
 * running of it can cause failure and generate messages. This abstracts that out.
 */
public abstract class Executable {
    protected boolean failed = false;
    protected List<String> messages = null;

    /**
     * Get messages.
     *
     * @return A {@link java.util.List} of messages.
     */
    public List<String> getMessages() {
        return messages;
    }

    /**
     * Add a message.
     *
     * @param message The message to add.
     */
    public void addMessage(String message) {
        if (this.messages == null) {
            this.messages = new ArrayList<>();
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
     * Is the executable in the failed status?
     *
     * @return True iff in the failure status.
     */
    public boolean failed() {
        return failed;
    }
}
