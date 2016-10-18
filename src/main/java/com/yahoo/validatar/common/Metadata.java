/*
 * Copyright 2015 Yahoo Inc.
 * Licensed under the terms of the Apache 2 license. Please see LICENSE file in the project root for terms.
 */
package com.yahoo.validatar.common;

import lombok.AllArgsConstructor;
import lombok.NoArgsConstructor;

/**
 * A wrapper type that represents a single piece of Metadata, i.e. a key value pair.
 */
@NoArgsConstructor @AllArgsConstructor
public class Metadata {
    public String key;
    public String value;
}
