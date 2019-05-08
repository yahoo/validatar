/*
 * Copyright 2017 Yahoo Inc.
 * Licensed under the terms of the Apache 2 license. Please see LICENSE file in the project root for terms.
 */
package com.yahoo.validatar.common;

import lombok.Getter;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.Objects;
import java.util.stream.Stream;

/**
 * Wraps a sequence of {@link TypedObject} as a Column.
 */
public class Column implements Iterable<TypedObject> {
    @Getter
    private final List<TypedObject> values;

    /**
     * Creates a new empty Column.
     */
    public Column() {
        values = new ArrayList<>();
    }

    /**
     * Wraps a {@link TypedObject} as a Column of size 1.
     *
     * @param object The single TypedObject to wrap.
     */
    public Column(TypedObject object) {
        Objects.requireNonNull(object);
        values = new ArrayList<>();
        values.add(object);
    }

    /**
     * Wraps a {@link List} of {@link TypedObject} as a Column.
     *
     * @param list The TypedObjects to wrap.
     */
    public Column(List<TypedObject> list) {
        Objects.requireNonNull(list);
        values = new ArrayList<>(list);
    }

    /**
     * If this contains no data, then it is empty.
     *
     * @return A boolean denoting whether this is empty.
     */
    public boolean isEmpty() {
        return size() == 0;
    }

    /**
     * If this contains only one {@link TypedObject}, then it is a scalar.
     *
     * @return A boolean denoting whether this is a scalar.
     */
    public boolean isScalar() {
        return size() == 1;
    }

    /**
     * If this contains more than one {@link TypedObject}, then it is a vector.
     *
     * @return A boolean denoting whether this is a vector.
     */
    public boolean isVector() {
        return size() > 1;
    }

    /**
     * The number of elements in the vector.
     *
     * @return The size of the vector.
     */
    public int size() {
        return values.size();
    }

    /**
     * Returns the first element of this vector.
     *
     * @return The {@link TypedObject} at the first position.
     */
    public TypedObject first() {
        return get(0);
    }

    /**
     * Returns the {@link TypedObject} at the given position.
     *
     * @param position The integer representing the position to get.
     * @return The TypedObject at that position if one exists.
     * @throws IndexOutOfBoundsException if the position is invalid.
     */
    public TypedObject get(int position) {
        if (position < 0 || position >= values.size()) {
            throw new IndexOutOfBoundsException("There is no object at position " + position + " in " + this);
        }
        return values.get(position);
    }

    /**
     * Add a {@link TypedObject} to this vector.
     *
     * @param object The non-null TypedObject to add.
     */
    public void add(TypedObject object) {
        values.add(object);
    }

    /**
     * Add another {@link Column} to this vector.
     *
     * @param column The non-null Column to add.
     */
    public void add(Column column) {
        column.stream().forEach(this::add);
    }

    /**
     * Creates a full copy of this Column.
     *
     * @return The copied column.
     */
    public Column copy() {
        return this.stream().collect(Column::new, (c, t) -> c.add(t == null ? null : new TypedObject(t.data, t.type)), Column::add);
    }

    /**
     * Creates a {@link Stream} of the values in this Column.
     *
     * @return A Stream of the {@link TypedObject} in this object.
     */
    public Stream<TypedObject> stream() {
        return values.stream();
    }

    @Override
    public Iterator<TypedObject> iterator() {
        return values.iterator();
    }

    @Override
    public String toString() {
        return Objects.toString(values);
    }
}
