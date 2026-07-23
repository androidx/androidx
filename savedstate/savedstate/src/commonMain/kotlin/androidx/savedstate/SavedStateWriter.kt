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
     * Stores a [Boolean] value associated with the specified key in the [SavedState].
     *
     * @param key The key to associate the value with.
     * @param value The [Boolean] value to store.
     */
    public fun putBoolean(key: String, value: Boolean)

    /**
     * Stores a [Char] value associated with the specified key in the [SavedState].
     *
     * @param key The key to associate the value with.
     * @param value The [Char] value to store.
     */
    public fun putChar(key: String, value: Char)

    /**
     * Stores a [CharSequence] value associated with the specified key in the [SavedState].
     *
     * @param key The key to associate the value with.
     * @param value The [CharSequence] value to store.
     */
    public fun putCharSequence(key: String, value: CharSequence)

    /**
     * Stores a [Double] value associated with the specified key in the [SavedState].
     *
     * @param key The key to associate the value with.
     * @param value The [Double] value to store.
     */
    public fun putDouble(key: String, value: Double)

    /**
     * Stores a [Float] value associated with the specified key in the [SavedState].
     *
     * @param key The key to associate the value with.
     * @param value The [Float] value to store.
     */
    public fun putFloat(key: String, value: Float)

    /**
     * Stores an [Int] value associated with the specified key in the [SavedState].
     *
     * @param key The key to associate the value with.
     * @param value The [Int] value to store.
     */
    public fun putInt(key: String, value: Int)

    /**
     * Stores a [Long] value associated with the specified key in the [SavedState].
     *
     * @param key The key to associate the value with.
     * @param value The [Long] value to store.
     */
    public fun putLong(key: String, value: Long)

    /**
     * Stores a `null` reference associated with the specified key in the [SavedState].
     *
     * @param key The key to associate the `null` reference.
     */
    public fun putNull(key: String)

    /**
     * Stores a [String] value associated with the specified key in the [SavedState].
     *
     * @param key The key to associate the value with.
     * @param value The [String] value to store.
     */
    public fun putString(key: String, value: String)

    /**
     * Stores a [List] of [Int] values associated with the specified key in the [SavedState].
     *
     * @param key The key to associate the value with.
     * @param value The list of elements to store.
     */
    public fun putIntList(key: String, value: List<Int>)

    /**
     * Stores a [List] of [CharSequence] values associated with the specified key in the
     * [SavedState].
     *
     * @param key The key to associate the value with.
     * @param value The list of elements to store.
     */
    public fun putCharSequenceList(key: String, value: List<CharSequence>)

    /**
     * Stores a [List] of [SavedState] values associated with the specified key in the [SavedState].
     *
     * @param key The key to associate the value with.
     * @param value The list of elements to store.
     */
    public fun putSavedStateList(key: String, value: List<SavedState>)

    /**
     * Stores a [List] of [String] values associated with the specified key in the [SavedState].
     *
     * @param key The key to associate the value with.
     * @param value The list of elements to store.
     */
    public fun putStringList(key: String, value: List<String>)

    /**
     * Stores a [BooleanArray] associated with the specified key in the [SavedState].
     *
     * @param key The key to associate the value with.
     * @param value The array of elements to store.
     */
    public fun putBooleanArray(key: String, value: BooleanArray)

    /**
     * Stores a [CharArray] associated with the specified key in the [SavedState].
     *
     * @param key The key to associate the value with.
     * @param value The array of elements to store.
     */
    public fun putCharArray(key: String, value: CharArray)

    /**
     * Stores an [Array] of [CharSequence] values associated with the specified key in the
     * [SavedState].
     *
     * @param key The key to associate the value with.
     * @param value The array of elements to store.
     */
    public fun putCharSequenceArray(key: String, value: Array<CharSequence>)

    /**
     * Stores a [DoubleArray] associated with the specified key in the [SavedState].
     *
     * @param key The key to associate the value with.
     * @param value The array of elements to store.
     */
    public fun putDoubleArray(key: String, value: DoubleArray)

    /**
     * Stores a [FloatArray] associated with the specified key in the [SavedState].
     *
     * @param key The key to associate the value with.
     * @param value The array of elements to store.
     */
    public fun putFloatArray(key: String, value: FloatArray)

    /**
     * Stores an [IntArray] associated with the specified key in the [SavedState].
     *
     * @param key The key to associate the value with.
     * @param value The array of elements to store.
     */
    public fun putIntArray(key: String, value: IntArray)

    /**
     * Stores a [LongArray] associated with the specified key in the [SavedState].
     *
     * @param key The key to associate the value with.
     * @param value The array of elements to store.
     */
    public fun putLongArray(key: String, value: LongArray)

    /**
     * Stores an [Array] of [SavedState] values associated with the specified key in the
     * [SavedState].
     *
     * @param key The key to associate the value with.
     * @param value The array of elements to store.
     */
    public fun putSavedStateArray(key: String, value: Array<SavedState>)

    /**
     * Stores an [Array] of [String] values associated with the specified key in the [SavedState].
     *
     * @param key The key to associate the value with.
     * @param value The array of elements to store.
     */
    public fun putStringArray(key: String, value: Array<String>)

    /**
     * Stores a [SavedState] object associated with the specified key in the [SavedState].
     *
     * @param key The key to associate the value with.
     * @param value The [SavedState] object to store.
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
