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

import java.util.List;

public class Test {
    public String name;
    public String description;
    public List<String> asserts;

    private boolean failed;
    private String failedAssertMessage;

    /**
     * Get the message for the failure.
     */
    public String getFailedAssertMessage() {
        return failedAssertMessage;
    }

    /**
     * Set the failure message.
     */
    public void setFailedAssertMessage(String failedAssertMessage) {
        this.failedAssertMessage = failedAssertMessage;
    }

    /**
     * Set the test to be failed.
     */
    public void setTestFailed() {
        this.failed = true;
    }

    /**
     * Set the test to be success.
     */
    public void setTestSuccess() {
        this.failed = false;
    }

    /**
     * True if the test failed.
     */
    public boolean failed() {
        return failed == true;
    }
}
