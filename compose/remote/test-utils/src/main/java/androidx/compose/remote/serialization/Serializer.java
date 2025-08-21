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

import androidx.compose.remote.core.serialize.MapSerializer;

import org.jspecify.annotations.Nullable;

import java.util.List;
import java.util.Map;

public interface Serializer {

    enum ValueType {
        ARRAY,
        MAP,
        STRING,
        BYTE,
        SHORT,
        INT,
        LONG,
        FLOAT,
        DOUBLE,
        BOOLEAN,
        NULL
    }

    /**
     * Returns the value type
     *
     * @return
     */
    ValueType getValueType();

    /**
     * Declare this serializer to be an Array
     *
     * @return An ArraySerializer where elements can be added
     */
    ArraySerializer serializeArray();

    /**
     * Declares this serializer to be a list and initializes it with the passed in list
     *
     * @param value The list to initialize this serializer with
     * @return An ArraySerializer where additional elements can be added
     */
    <T> ArraySerializer serializeArray(@Nullable List<T> value);

    /**
     * Declare this serializer to be a Map
     *
     * @return An MapSerializer where entries can be added
     */
    MapSerializer serializeMap();

    /**
     * Serializes a float expression array
     *
     * @param value The float expression
     */
    void serializeFloatExpressionSrc(float[] value);

    /**
     * Serializes an int expression array
     *
     * @param value The int expression
     */
    void serializeIntExpressionSrc(int[] value, int mask);

    /**
     * Serializes a path
     *
     * @param path The path data
     */
    void serializePath(float[] path);

    /**
     * Declares this serializer to be a list and initializes it with the passed in list
     *
     * @param value The list to initialize this serializer with
     * @return A MapSerializer where additional entries can be added
     */
    <T> MapSerializer serializeMap(@Nullable Map<String, T> value);

    /**
     * Declare this serializer a String
     *
     * @param value The value of the String
     */
    void serialize(@Nullable String value);

    /**
     * Declare this serializer a Byte
     *
     * @param value The value of the Byte
     */
    void serialize(@Nullable Byte value);

    /**
     * Declare this serializer a Short
     *
     * @param value The value of the Short
     */
    void serialize(@Nullable Short value);

    /**
     * Declare this serializer a Integer
     *
     * @param value The value of the Integer
     */
    void serialize(@Nullable Integer value);

    /**
     * Declare this serializer a Long
     *
     * @param value The value of the Long
     */
    void serialize(@Nullable Long value);

    /**
     * Declare this serializer a Float
     *
     * @param value The value of the Float
     */
    void serialize(@Nullable Float value);

    /**
     * Declare this serializer a Double
     *
     * @param value The value of the Double
     */
    void serialize(@Nullable Double value);

    /**
     * Declare this serializer a Boolean
     *
     * @param value The value of the Boolean
     */
    void serialize(@Nullable Boolean value);

    /**
     * Declare this serializer an Enum
     *
     * @param value The value of the Enum
     */
    <T extends Enum<T>> void serialize(@Nullable Enum<T> value);

    /**
     * Declare this serializer a color
     *
     * @param a Alpha value [0, 1]
     * @param r Red value [0, 1]
     * @param g Green value [0, 1]
     * @param b Blue value [0, 1]
     */
    void serialize(float a, float r, float g, float b);
}
