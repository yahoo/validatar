/*
 * Copyright 2015 Yahoo Inc.
 * Licensed under the terms of the Apache 2 license. Please see LICENSE file in the project root for terms.
 */
package com.yahoo.validatar.common;

import java.util.Objects;

/**
 * This is the custom annotated object that is used in our assertion language.
 */
public class TypedObject {
    /**
     * We are now handling type safety.
     */
    @SuppressWarnings("unchecked")
    // TODO: Change these to final or use getters/setters.
    public Comparable data;
    public TypeSystem.Type type;

    /**
     * Constructor.
     *
     * @param data A non-null {@link java.lang.Comparable} object that we are managing the type for.
     * @param type The non-null {@link com.yahoo.validatar.common.TypeSystem.Type} of the object.
     */
    public TypedObject(Comparable data, TypeSystem.Type type) {
        Objects.requireNonNull(data);
        Objects.requireNonNull(type);
        this.data = data;
        this.type = type;
    }

    @Override
    public String toString() {
        return "<Type: " + type + ", Value: " + data.toString() + ">";
    }
}
