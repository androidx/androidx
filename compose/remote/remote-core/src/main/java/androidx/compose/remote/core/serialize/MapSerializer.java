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
package androidx.compose.remote.core.serialize;

import androidx.annotation.RestrictTo;

import org.jspecify.annotations.NonNull;
import org.jspecify.annotations.Nullable;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/** Represents a serializer for a map */
@RestrictTo(RestrictTo.Scope.LIBRARY_GROUP)
public interface MapSerializer {

    /**
     * Adds a "type" field with this value
     *
     * @param type The name of the type
     */
    @NonNull MapSerializer addType(@NonNull String type);

    /**
     * Add a float expression
     *
     * @param key The key
     * @param value The float src
     */
    @NonNull MapSerializer addFloatExpressionSrc(@NonNull String key, float @NonNull [] value);

    /**
     * Add an int expression
     *
     * @param key The key
     * @param value The int src
     * @param mask For determining ID from int
     */
    @NonNull MapSerializer addIntExpressionSrc(
            @NonNull String key, int @NonNull [] value, int mask);

    /**
     * Add a path
     *
     * @param key The key
     * @param path The path
     */
    @NonNull MapSerializer addPath(@NonNull String key, float @NonNull [] path);

    /**
     * Add metadata to this map for filtering by the data format generator.
     *
     * @param value A set of tags to add
     */
    @NonNull MapSerializer addTags(SerializeTags @NonNull ... value);

    /**
     * Add a list entry to this map. The List values can be any primitive, List, Map, or
     * Serializable
     *
     * @param key The key
     * @param value The list
     */
    <T> @NonNull MapSerializer add(@NonNull String key, @Nullable List<T> value);

    /**
     * Add a map entry to this map. The map values can be any primitive, List, Map, or Serializable
     *
     * @param key The key
     * @param value The list
     */
    <T> @NonNull MapSerializer add(@NonNull String key, @Nullable Map<String, T> value);

    /**
     * Adds any Serializable type to this map
     *
     * @param key The key
     * @param value The Serializable
     */
    @NonNull MapSerializer add(@NonNull String key, @Nullable Serializable value);

    /**
     * Adds a String entry
     *
     * @param key The key
     * @param value The String
     */
    @NonNull MapSerializer add(@NonNull String key, @Nullable String value);

    /**
     * Adds a color entry
     *
     * @param key The key
     * @param a Alpha value [0, 1]
     * @param r Red value [0, 1]
     * @param g Green value [0, 1]
     * @param b Blue value [0, 1]
     */
    @NonNull MapSerializer add(@NonNull String key, float a, float r, float g, float b);

    /**
     * Adds an ID and Value pair. This can be either a value or variable.
     *
     * @param key The key
     * @param id Maybe float NaN ID
     * @param value Maybe value
     */
    @NonNull MapSerializer add(@NonNull String key, float id, float value);

    /**
     * Adds a Byte entry
     *
     * @param key The key
     * @param value The Byte
     */
    @NonNull MapSerializer add(@NonNull String key, @Nullable Byte value);

    /**
     * Adds a Short entry
     *
     * @param key The key
     * @param value The Short
     */
    @NonNull MapSerializer add(@NonNull String key, @Nullable Short value);

    /**
     * Adds an Integer entry
     *
     * @param key The key
     * @param value The Integer
     */
    @NonNull MapSerializer add(@NonNull String key, @Nullable Integer value);

    /**
     * Adds a Long entry
     *
     * @param key The key
     * @param value The Long
     */
    @NonNull MapSerializer add(@NonNull String key, @Nullable Long value);

    /**
     * Adds a Float entry
     *
     * @param key The key
     * @param value The Float
     */
    @NonNull MapSerializer add(@NonNull String key, @Nullable Float value);

    /**
     * Adds a Double entry
     *
     * @param key The key
     * @param value The Double
     */
    @NonNull MapSerializer add(@NonNull String key, @Nullable Double value);

    /**
     * Adds a Boolean entry
     *
     * @param key The key
     * @param value The Boolean
     */
    @NonNull MapSerializer add(@NonNull String key, @Nullable Boolean value);

    /**
     * Adds a Enum entry
     *
     * @param key The key
     * @param value The Enum
     */
    <T extends Enum<T>> @NonNull MapSerializer add(@NonNull String key, @Nullable Enum<T> value);

    /**
     * Similar to Map.of, but create a LinkedHashMap preserving insertion order for predictable
     * serialization.
     *
     * @param keysAndValues a even number of items, repeating String key and Object value.
     * @return A LinkedHashMap.
     */
    static @NonNull LinkedHashMap<String, Object> orderedOf(Object @NonNull ... keysAndValues) {
        final LinkedHashMap<String, Object> map = new LinkedHashMap<>();
        for (int i = 0; i < keysAndValues.length; i += 2) {
            map.put((String) keysAndValues[i], keysAndValues[i + 1]);
        }
        return map;
    }
}
