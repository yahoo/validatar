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

import java.util.Collection;

public class Utilities {

    /**
     * Takes an item and adds it to a collection if it's not null.
     * @param <T> The type of the item.
     * @param item The item to add.
     * @param collection The collection to add the item to. Not null.
     * @return The resulting collection.
     */
    public static <T> Collection<T> addNonNull(T item, Collection<T> collection) {
        if (item != null) {
            collection.add(item);
        }
        return collection;
    }

    /**
     * Takes a collection of item and adds them to a collection if they are not null.
     * @param <T> The type of the item.
     * @param items The collection of items to add.
     * @param collection The collection to add the items to. Not null.
     * @return The resulting collection.
     */
    public static <T> Collection<T> addNonNull(Collection<T> items, Collection<T> collection) {
        if (items != null) {
            for (T item : items) {
                addNonNull(item, collection);
            }
        }
        return collection;
    }
}
