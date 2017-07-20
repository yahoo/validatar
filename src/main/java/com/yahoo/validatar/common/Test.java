/*
 * Copyright 2015 Yahoo Inc.
 * Licensed under the terms of the Apache 2 license. Please see LICENSE file in the project root for terms.
 */
package com.yahoo.validatar.common;

import lombok.Getter;

import java.util.List;

public class Test extends Executable {
    public String name;
    public String description;
    public List<String> asserts;
    @Getter
    public Boolean warnOnly = false;

    /**
     * Did this test pass. A test passes if it only warns.
     *
     * @return A boolean denoting if the test passed or not.
     */
    public boolean passed() {
        return warnOnly || !failed;
    }
}
