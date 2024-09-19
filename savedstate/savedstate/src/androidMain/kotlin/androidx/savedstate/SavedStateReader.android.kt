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

import android.os.Parcelable
import androidx.core.os.BundleCompat
import androidx.savedstate.internal.SavedStateUtils
import androidx.savedstate.internal.SavedStateUtils.getValueFromSavedState
import androidx.savedstate.internal.SavedStateUtils.keyNotFoundError

@Suppress("NOTHING_TO_INLINE")
@JvmInline
actual value class SavedStateReader actual constructor(actual val source: SavedState) {

    actual inline fun getBoolean(key: String): Boolean {
        return getSingleResultOrThrow(key) {
            source.getBoolean(key, SavedStateUtils.DEFAULT_BOOLEAN)
        }
    }

    actual inline fun getBooleanOrElse(key: String, defaultValue: () -> Boolean): Boolean {
        return getSingleResultOrElse(key, defaultValue) { source.getBoolean(key) }
    }

    actual inline fun getDouble(key: String): Double {
        return getSingleResultOrThrow(key) { source.getDouble(key, SavedStateUtils.DEFAULT_DOUBLE) }
    }

    actual inline fun getDoubleOrElse(key: String, defaultValue: () -> Double): Double {
        return getSingleResultOrElse(key, defaultValue) { source.getDouble(key) }
    }

    actual inline fun getFloat(key: String): Float {
        return getSingleResultOrThrow(key) { source.getFloat(key, SavedStateUtils.DEFAULT_FLOAT) }
    }

    actual inline fun getFloatOrElse(key: String, defaultValue: () -> Float): Float {
        return getSingleResultOrElse(key, defaultValue) { source.getFloat(key) }
    }

    actual inline fun getInt(key: String): Int {
        return getSingleResultOrThrow(key) { source.getInt(key, SavedStateUtils.DEFAULT_INT) }
    }

    actual inline fun getIntOrElse(key: String, defaultValue: () -> Int): Int {
        return getSingleResultOrElse(key, defaultValue) { source.getInt(key) }
    }

    /**
     * Retrieves a [Parcelable] object associated with the specified key. Throws an
     * [IllegalStateException] if the key doesn't exist.
     *
     * @param key The key to retrieve the value for.
     * @return The [Parcelable] object associated with the key.
     * @throws IllegalStateException If the key is not found.
     */
    inline fun <reified T : Parcelable> getParcelable(key: String): T {
        return getSingleResultOrThrow(key) {
            BundleCompat.getParcelable(source, key, T::class.java)
        }
    }

    /**
     * Retrieves a [Parcelable] object associated with the specified key, or a default value if the
     * key doesn't exist.
     *
     * @param key The key to retrieve the value for.
     * @param defaultValue A function providing the default [Parcelable] if the key is not found.
     * @return The [Parcelable] object associated with the key, or the default value if the key is
     *   not found.
     */
    inline fun <reified T : Parcelable> getParcelableOrElse(key: String, defaultValue: () -> T): T {
        return getSingleResultOrElse(key, defaultValue) {
            BundleCompat.getParcelable(source, key, T::class.java)
        }
    }

    actual inline fun getString(key: String): String {
        return getSingleResultOrThrow(key) { source.getString(key) }
    }

    actual inline fun getStringOrElse(key: String, defaultValue: () -> String): String {
        return getSingleResultOrElse(key, defaultValue) { source.getString(key) }
    }

    actual inline fun getIntList(key: String): List<Int> {
        return getListResultOrThrow(key) { source.getIntegerArrayList(key) }
    }

    actual inline fun getIntListOrElse(key: String, defaultValue: () -> List<Int>): List<Int> {
        return getListResultOrElse(key, defaultValue) { source.getIntegerArrayList(key) }
    }

    actual inline fun getStringList(key: String): List<String> {
        return getListResultOrThrow(key) { source.getStringArrayList(key) }
    }

    actual inline fun getStringListOrElse(
        key: String,
        defaultValue: () -> List<String>
    ): List<String> {
        return getListResultOrElse(key, defaultValue) { source.getStringArrayList(key) }
    }

    /**
     * Retrieves a [List] of elements of [Parcelable] associated with the specified [key]. Throws an
     * [IllegalStateException] if the [key] doesn't exist.
     *
     * @param key The [key] to retrieve the value for.
     * @return The [List] of elements of [Parcelable] associated with the [key].
     * @throws IllegalStateException If the [key] is not found.
     */
    inline fun <reified T : Parcelable> getParcelableList(key: String): List<T> {
        return getListResultOrThrow(key) {
            BundleCompat.getParcelableArrayList(source, key, T::class.java)
        }
    }

    /**
     * Retrieves a [List] of elements of [Parcelable] associated with the specified [key], or a
     * default value if the [key] doesn't exist.
     *
     * @param key The [key] to retrieve the value for.
     * @param defaultValue A function providing the default value if the [key] is not found or the
     *   retrieved value is not a list of [Parcelable].
     * @return The list of elements of [Parcelable] associated with the [key], or the default value
     *   if the [key] is not found.
     */
    inline fun <reified T : Parcelable> getParcelableListOrElse(
        key: String,
        defaultValue: () -> List<T>
    ): List<T> {
        return getListResultOrElse(key, defaultValue) {
            BundleCompat.getParcelableArrayList(source, key, T::class.java)
        }
    }

    actual inline fun getSavedState(key: String): SavedState {
        return getSingleResultOrThrow(key) { source.getBundle(key) }
    }

    actual inline fun getSavedStateOrElse(key: String, defaultValue: () -> SavedState): SavedState {
        return getSingleResultOrElse(key, defaultValue) { source.getBundle(key) }
    }

    actual inline fun size(): Int = source.size()

    actual inline fun isEmpty(): Boolean = source.isEmpty

    actual inline operator fun contains(key: String): Boolean = source.containsKey(key)

    @PublishedApi
    internal inline fun <reified T> getSingleResultOrThrow(
        key: String,
        currentValue: () -> T?,
    ): T =
        getValueFromSavedState(
            key = key,
            contains = { source.containsKey(key) },
            currentValue = { currentValue() },
            defaultValue = { keyNotFoundError(key) },
        )

    @PublishedApi
    internal inline fun <reified T> getSingleResultOrElse(
        key: String,
        defaultValue: () -> T,
        currentValue: () -> T?,
    ): T =
        getValueFromSavedState(
            key = key,
            contains = { source.containsKey(key) },
            currentValue = { currentValue() },
            defaultValue = { defaultValue() },
        )

    @PublishedApi
    internal inline fun <reified T> getListResultOrThrow(
        key: String,
        currentValue: () -> List<T>?,
    ): List<T> =
        getValueFromSavedState(
            key = key,
            contains = { source.containsKey(key) },
            currentValue = { currentValue() },
            defaultValue = { keyNotFoundError(key) },
        )

    @PublishedApi
    internal inline fun <reified T> getListResultOrElse(
        key: String,
        defaultValue: () -> List<T>,
        currentValue: () -> List<T>?,
    ): List<T> =
        getValueFromSavedState(
            key = key,
            contains = { source.containsKey(key) },
            currentValue = { currentValue() },
            defaultValue = { defaultValue() },
        )
}
