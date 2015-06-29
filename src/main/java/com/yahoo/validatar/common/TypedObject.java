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

/**
 * This is the custom annotated object that is used in our assertion language.
 */
public class TypedObject {
    /** We are now handling type safety. */
    @SuppressWarnings("unchecked")
    public Comparable data;
    public TypeSystem.Type type;

    /**
     * Constructor.
     *
     * @param data A {@link java.lang.Comparable} object that we are managing the type for.
     * @param type The {@link com.yahoo.validatar.common.TypeSystem.Type} of the object.
     */
    public TypedObject(Comparable data, TypeSystem.Type type) {
        this.data = data;
        this.type = type;
    }
}
