/*
 * Copyright 2015 Yahoo Inc.
 * Licensed under the terms of the Apache 2 license. Please see LICENSE file in the project root for terms.
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
