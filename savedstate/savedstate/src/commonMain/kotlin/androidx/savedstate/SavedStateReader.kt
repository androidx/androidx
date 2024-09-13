/*
 * Copyright 2024 The Android Open Source Project
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

package androidx.savedstate

import kotlin.jvm.JvmInline

/**
 * An inline class that encapsulates an opaque [SavedState], and provides an API for reading the
 * platform specific state.
 *
 * @see SavedState.read
 */
@JvmInline
public expect value class SavedStateReader
internal constructor(
    @PublishedApi internal val source: SavedState,
) {

    /**
     * Retrieves a [Boolean] value associated with the specified [key]. Throws an
     * [IllegalStateException] if the [key] doesn't exist.
     *
     * @param key The [key] to retrieve the value for.
     * @return The [Boolean] value associated with the [key].
     * @throws IllegalStateException If the [key] is not found.
     */
    public inline fun getBoolean(key: String): Boolean

    /**
     * Retrieves a [Boolean] value associated with the specified [key], or a default value if the
     * [key] doesn't exist.
     *
     * @param key The [key] to retrieve the value for.
     * @param defaultValue A function providing the default value if the [key] is not found.
     * @return The [Boolean] value associated with the [key], or the default value if the [key] is
     *   not found.
     */
    public inline fun getBooleanOrElse(key: String, defaultValue: () -> Boolean): Boolean

    /**
     * Retrieves a [Double] value associated with the specified [key]. Throws an
     * [IllegalStateException] if the [key] doesn't exist.
     *
     * @param key The [key] to retrieve the value for.
     * @return The [Double] value associated with the [key].
     * @throws IllegalStateException If the [key] is not found.
     */
    public inline fun getDouble(key: String): Double

    /**
     * Retrieves a [Double] value associated with the specified [key], or a default value if the
     * [key] doesn't exist.
     *
     * @param key The [key] to retrieve the value for.
     * @param defaultValue A function providing the default value if the [key] is not found.
     * @return The [Double] value associated with the [key], or the default value if the [key] is
     *   not found.
     */
    public inline fun getDoubleOrElse(key: String, defaultValue: () -> Double): Double

    /**
     * Retrieves a [Float] value associated with the specified [key]. Throws an
     * [IllegalStateException] if the [key] doesn't exist.
     *
     * @param key The [key] to retrieve the value for.
     * @return The [Float] value associated with the [key].
     * @throws IllegalStateException If the [key] is not found.
     */
    public inline fun getFloat(key: String): Float

    /**
     * Retrieves a [Float] value associated with the specified [key], or a default value if the
     * [key] doesn't exist.
     *
     * @param key The [key] to retrieve the value for.
     * @param defaultValue A function providing the default value if the [key] is not found.
     * @return The [Float] value associated with the [key], or the default value if the [key] is not
     *   found.
     */
    public inline fun getFloatOrElse(key: String, defaultValue: () -> Float): Float

    /**
     * Retrieves an [Int] value associated with the specified [key]. Throws an
     * [IllegalStateException] if the [key] doesn't exist.
     *
     * @param key The [key] to retrieve the value for.
     * @return The [Int] value associated with the [key].
     * @throws IllegalStateException If the [key] is not found.
     */
    public inline fun getInt(key: String): Int

    /**
     * Retrieves an [Int] value associated with the specified [key], or a default value if the [key]
     * doesn't exist.
     *
     * @param key The [key] to retrieve the value for.
     * @param defaultValue A function providing the default value if the [key] is not found.
     * @return The [Int] value associated with the [key], or the default value if the [key] is not
     *   found.
     */
    public inline fun getIntOrElse(key: String, defaultValue: () -> Int): Int

    /**
     * Retrieves a [String] value associated with the specified [key]. Throws an
     * [IllegalStateException] if the [key] doesn't exist.
     *
     * @param key The [key] to retrieve the value for.
     * @return The [String] value associated with the [key].
     * @throws IllegalStateException If the [key] is not found.
     */
    public inline fun getString(key: String): String

    /**
     * Retrieves a [String] value associated with the specified [key], or a default value if the
     * [key] doesn't exist.
     *
     * @param key The [key] to retrieve the value for.
     * @param defaultValue A function providing the default value if the [key] is not found.
     * @return The [String] value associated with the [key], or the default value if the [key] is
     *   not found.
     */
    public inline fun getStringOrElse(key: String, defaultValue: () -> String): String

    /**
     * Retrieves a [List] of elements of [Int] associated with the specified [key]. Throws an
     * [IllegalStateException] if the [key] doesn't exist.
     *
     * @param key The [key] to retrieve the value for.
     * @return The [List] of elements of [Int] associated with the [key].
     * @throws IllegalStateException If the [key] is not found.
     */
    public inline fun getIntList(key: String): List<Int>

    /**
     * Retrieves a [List] of elements of [Int] associated with the specified [key], or a default
     * value if the [key] doesn't exist.
     *
     * @param key The [key] to retrieve the value for.
     * @param defaultValue A function providing the default value if the [key] is not found or the
     *   retrieved value is not a list of [Int].
     * @return The list of elements of [Int] associated with the [key], or the default value if the
     *   [key] is not found.
     */
    public inline fun getIntListOrElse(key: String, defaultValue: () -> List<Int>): List<Int>

    /**
     * Retrieves a [List] of elements of [String] associated with the specified [key]. Throws an
     * [IllegalStateException] if the [key] doesn't exist.
     *
     * @param key The [key] to retrieve the value for.
     * @return The [List] of elements of [String] associated with the [key].
     * @throws IllegalStateException If the [key] is not found.
     */
    public inline fun getStringList(key: String): List<String>

    /**
     * Retrieves a [List] of elements of [String] associated with the specified [key], or a default
     * value if the [key] doesn't exist.
     *
     * @param key The [key] to retrieve the value for.
     * @param defaultValue A function providing the default value if the [key] is not found or the
     *   retrieved value is not a list of [String].
     * @return The list of elements of [String] associated with the [key], or the default value if
     *   the [key] is not found.
     */
    public inline fun getStringListOrElse(
        key: String,
        defaultValue: () -> List<String>
    ): List<String>

    /**
     * Retrieves a [SavedState] object associated with the specified [key]. Throws an
     * [IllegalStateException] if the [key] doesn't exist.
     *
     * @param key The [key] to retrieve the value for.
     * @return The [SavedState] object associated with the [key].
     * @throws IllegalStateException If the [key] is not found.
     */
    public inline fun getSavedState(key: String): SavedState

    /**
     * Retrieves a [SavedState] object associated with the specified [key], or a default value if
     * the [key] doesn't exist.
     *
     * @param key The [key] to retrieve the value for.
     * @param defaultValue A function providing the default [SavedState] if the [key] is not found.
     * @return The [SavedState] object associated with the [key], or the default value if the [key]
     *   is not found.
     */
    public inline fun getSavedStateOrElse(key: String, defaultValue: () -> SavedState): SavedState

    /**
     * Returns the number of key-value pairs in the [SavedState].
     *
     * @return The size of the [SavedState].
     */
    public inline fun size(): Int

    /**
     * Checks if the [SavedState] is empty (contains no key-value pairs).
     *
     * @return `true` if the [SavedState] is empty, `false` otherwise.
     */
    public inline fun isEmpty(): Boolean

    /**
     * Checks if the [SavedState] contains the specified [key].
     *
     * @param key The [key] to check for.
     * @return `true` if the [SavedState] contains the [key], `false` otherwise.
     */
    public inline operator fun contains(key: String): Boolean
}
