/*
 * Copyright (C) 2025 The Android Open Source Project
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package androidx.compose.remote.serialization;

import androidx.compose.remote.core.serialize.Serializable;

import org.jspecify.annotations.Nullable;

import java.util.List;
import java.util.Map;

/** Represents a serializer for an array */
public interface ArraySerializer {

    /**
     * Add a list to this array. The List values can be any primitive, List, Map, or Serializable
     *
     * @param value The list
     */
    <T> void add(@Nullable List<T> value);

    /**
     * Add a map to this array. The map values can be any primitive, List, Map, or Serializable
     *
     * @param value The map
     */
    <T> void add(@Nullable Map<String, T> value);

    /**
     * Adds any Serializable type to this array
     *
     * @param value The Serializable
     */
    void add(Serializable value);

    /**
     * Adds a String to this array
     *
     * @param value The String
     */
    void add(@Nullable String value);

    /**
     * Adds a Byte to this array
     *
     * @param value The Byte
     */
    void add(@Nullable Byte value);

    /**
     * Adds a Short to this array
     *
     * @param value The Short
     */
    void add(@Nullable Short value);

    /**
     * Adds an Integer to this array
     *
     * @param value The Integer
     */
    void add(@Nullable Integer value);

    /**
     * Adds a Long to this array
     *
     * @param value The Long
     */
    void add(@Nullable Long value);

    /**
     * Adds a Float to this array
     *
     * @param value The Float
     */
    void add(@Nullable Float value);

    /**
     * Adds a Double to this array
     *
     * @param value The Double
     */
    void add(@Nullable Double value);

    /**
     * Adds a Boolean to this array
     *
     * @param value The Boolean
     */
    void add(@Nullable Boolean value);

    /**
     * Adds am Enum to this array
     *
     * @param value The Enum
     */
    <T extends Enum<T>> void add(@Nullable Enum<T> value);
}
