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
 * An inline class that encapsulates an opaque [SavedState], and provides an API for writing the
 * platform specific state.
 *
 * @see SavedState.write
 */
@JvmInline
public expect value class SavedStateWriter
internal constructor(
    @PublishedApi internal val source: SavedState,
) {

    /**
     * Stores a boolean value associated with the specified key in the [SavedState].
     *
     * @param key The key to associate the value with.
     * @param value The boolean value to store.
     */
    public inline fun putBoolean(key: String, value: Boolean)

    /**
     * Stores a double value associated with the specified key in the [SavedState].
     *
     * @param key The key to associate the value with.
     * @param value The double value to store.
     */
    public inline fun putDouble(key: String, value: Double)

    /**
     * Stores a float value associated with the specified key in the [SavedState].
     *
     * @param key The key to associate the value with.
     * @param value The float value to store.
     */
    public inline fun putFloat(key: String, value: Float)

    /**
     * Stores an int value associated with the specified key in the [SavedState].
     *
     * @param key The key to associate the value with.
     * @param value The int value to store.
     */
    public inline fun putInt(key: String, value: Int)

    /**
     * Stores a string value associated with the specified key in the [SavedState].
     *
     * @param key The key to associate the value with.
     * @param value The string value to store.
     */
    public inline fun putString(key: String, value: String)

    /**
     * Stores a list of elements of [Int] associated with the specified key in the [SavedState].
     *
     * @param key The key to associate the value with.
     * @param values The list of elements to store.
     */
    public inline fun putIntList(key: String, values: List<Int>)

    /**
     * Stores a list of elements of [String] associated with the specified key in the [SavedState].
     *
     * @param key The key to associate the value with.
     * @param values The list of elements to store.
     */
    public inline fun putStringList(key: String, values: List<String>)

    /**
     * Stores a [SavedState] object associated with the specified key in the [SavedState].
     *
     * @param key The key to associate the value with.
     * @param value The [SavedState] object to store
     */
    public inline fun putSavedState(key: String, value: SavedState)

    /**
     * Stores all key-value pairs from the provided [SavedState] into this [SavedState].
     *
     * @param values The [SavedState] containing the key-value pairs to add.
     */
    public inline fun putAll(values: SavedState)

    /**
     * Removes the value associated with the specified key from the [SavedState].
     *
     * @param key The key to remove.
     */
    public inline fun remove(key: String)

    /** Removes all key-value pairs from the [SavedState]. */
    public inline fun clear()
}
