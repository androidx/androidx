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

@file:JvmName("SavedStateWriterKt")
@file:JvmMultifileClass
@file:Suppress("NOTHING_TO_INLINE")

package androidx.savedstate

import kotlin.jvm.JvmInline
import kotlin.jvm.JvmMultifileClass
import kotlin.jvm.JvmName

/**
 * An inline class that encapsulates an opaque [SavedState], and provides an API for writing the
 * platform specific state.
 *
 * @see SavedState.write
 */
@JvmInline
public expect value class SavedStateWriter
@PublishedApi
internal constructor(private val source: SavedState) {

    /**
     * Stores a boolean value associated with the specified key in the [SavedState].
     *
     * @param key The key to associate the value with.
     * @param value The boolean value to store.
     */
    public fun putBoolean(key: String, value: Boolean)

    /**
     * Stores a char value associated with the specified key in the [SavedState].
     *
     * @param key The key to associate the value with.
     * @param value The char value to store.
     */
    public fun putChar(key: String, value: Char)

    /**
     * Stores a char sequence value associated with the specified key in the [SavedState].
     *
     * @param key The key to associate the value with.
     * @param value The char sequence value to store.
     */
    public fun putCharSequence(key: String, value: CharSequence)

    /**
     * Stores a double value associated with the specified key in the [SavedState].
     *
     * @param key The key to associate the value with.
     * @param value The double value to store.
     */
    public fun putDouble(key: String, value: Double)

    /**
     * Stores a float value associated with the specified key in the [SavedState].
     *
     * @param key The key to associate the value with.
     * @param value The float value to store.
     */
    public fun putFloat(key: String, value: Float)

    /**
     * Stores an int value associated with the specified key in the [SavedState].
     *
     * @param key The key to associate the value with.
     * @param value The int value to store.
     */
    public fun putInt(key: String, value: Int)

    /**
     * Stores an int value associated with the specified key in the [SavedState].
     *
     * @param key The key to associate the value with.
     * @param value The [Long] value to store.
     */
    public fun putLong(key: String, value: Long)

    /**
     * Stores a null reference associated with the specified key in the [SavedState].
     *
     * @param key The key to associate the null reference.
     */
    public fun putNull(key: String)

    /**
     * Stores a string value associated with the specified key in the [SavedState].
     *
     * @param key The key to associate the value with.
     * @param value The string value to store.
     */
    public fun putString(key: String, value: String)

    /**
     * Stores a list of elements of [Int] associated with the specified key in the [SavedState].
     *
     * @param key The key to associate the value with.
     * @param value The list of elements to store.
     */
    public fun putIntList(key: String, value: List<Int>)

    /**
     * Stores a list of elements of [CharSequence] associated with the specified key in the
     * [SavedState].
     *
     * @param key The key to associate the value with.
     * @param value The list of elements to store.
     */
    public fun putCharSequenceList(key: String, value: List<CharSequence>)

    /**
     * Stores a list of elements of [SavedState] associated with the specified key in the
     * [SavedState].
     *
     * @param key The key to associate the value with.
     * @param value The list of elements to store.
     */
    public fun putSavedStateList(key: String, value: List<SavedState>)

    /**
     * Stores a list of elements of [String] associated with the specified key in the [SavedState].
     *
     * @param key The key to associate the value with.
     * @param value The list of elements to store.
     */
    public fun putStringList(key: String, value: List<String>)

    /**
     * Stores an [Array] of elements of [Boolean] associated with the specified key in the
     * [SavedState].
     *
     * @param key The key to associate the value with.
     * @param value The array of elements to store.
     */
    public fun putBooleanArray(key: String, value: BooleanArray)

    /**
     * Stores an [Array] of elements of [Boolean] associated with the specified key in the
     * [SavedState].
     *
     * @param key The key to associate the value with.
     * @param value The array of elements to store.
     */
    public fun putCharArray(key: String, value: CharArray)

    /**
     * Stores an [Array] of elements of [CharSequence] associated with the specified key in the
     * [SavedState].
     *
     * @param key The key to associate the value with.
     * @param value The array of elements to store.
     */
    public fun putCharSequenceArray(key: String, value: Array<CharSequence>)

    /**
     * Stores an [Array] of elements of [Double] associated with the specified key in the
     * [SavedState].
     *
     * @param key The key to associate the value with.
     * @param value The array of elements to store.
     */
    public fun putDoubleArray(key: String, value: DoubleArray)

    /**
     * Stores an [Array] of elements of [Float] associated with the specified key in the
     * [SavedState].
     *
     * @param key The key to associate the value with.
     * @param value The array of elements to store.
     */
    public fun putFloatArray(key: String, value: FloatArray)

    /**
     * Stores an [Array] of elements of [Int] associated with the specified key in the [SavedState].
     *
     * @param key The key to associate the value with.
     * @param value The array of elements to store.
     */
    public fun putIntArray(key: String, value: IntArray)

    /**
     * Stores an [Array] of elements of [Long] associated with the specified key in the
     * [SavedState].
     *
     * @param key The key to associate the value with.
     * @param value The array of elements to store.
     */
    public fun putLongArray(key: String, value: LongArray)

    /**
     * Stores an [Array] of elements of [SavedState] associated with the specified key in the
     * [SavedState].
     *
     * @param key The key to associate the value with.
     * @param value The array of elements to store.
     */
    public fun putSavedStateArray(key: String, value: Array<SavedState>)

    /**
     * Stores an [Array] of elements of [String] associated with the specified key in the
     * [SavedState].
     *
     * @param key The key to associate the value with.
     * @param value The array of elements to store.
     */
    public fun putStringArray(key: String, value: Array<String>)

    /**
     * Stores a [SavedState] object associated with the specified key in the [SavedState].
     *
     * @param key The key to associate the value with.
     * @param value The [SavedState] object to store
     */
    public fun putSavedState(key: String, value: SavedState)

    /**
     * Stores all key-value pairs from the provided [SavedState] into this [SavedState].
     *
     * @param from The [SavedState] containing the key-value pairs to add.
     */
    public fun putAll(from: SavedState)

    /**
     * Removes the value associated with the specified key from the [SavedState].
     *
     * @param key The key to remove.
     */
    public fun remove(key: String)

    /** Removes all key-value pairs from the [SavedState]. */
    public fun clear()
}
