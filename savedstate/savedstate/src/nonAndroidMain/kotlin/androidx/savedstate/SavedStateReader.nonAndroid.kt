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

import androidx.savedstate.internal.SavedStateUtils
import androidx.savedstate.internal.SavedStateUtils.getValueFromSavedState
import androidx.savedstate.internal.SavedStateUtils.keyNotFoundError
import kotlin.jvm.JvmInline

@Suppress("NOTHING_TO_INLINE")
@JvmInline
actual value class SavedStateReader actual constructor(actual val source: SavedState) {

    actual inline fun getBoolean(key: String): Boolean =
        getSingleResultOrThrow(key) {
            source.map[key] as? Boolean ?: SavedStateUtils.DEFAULT_BOOLEAN
        }

    actual inline fun getBooleanOrElse(key: String, defaultValue: () -> Boolean): Boolean =
        getSingleResultOrElse(key, defaultValue) { source.map[key] as? Boolean }

    actual inline fun getChar(key: String): Char =
        getSingleResultOrThrow(key) { source.map[key] as? Char ?: SavedStateUtils.DEFAULT_CHAR }

    actual inline fun getCharOrElse(key: String, defaultValue: () -> Char): Char =
        getSingleResultOrElse(key, defaultValue) { source.map[key] as? Char }

    actual inline fun getDouble(key: String): Double =
        getSingleResultOrThrow(key) { source.map[key] as? Double ?: SavedStateUtils.DEFAULT_DOUBLE }

    actual inline fun getDoubleOrElse(key: String, defaultValue: () -> Double): Double =
        getSingleResultOrElse(key, defaultValue) { source.map[key] as? Double }

    actual inline fun getFloat(key: String): Float =
        getSingleResultOrThrow(key) { source.map[key] as? Float ?: SavedStateUtils.DEFAULT_FLOAT }

    actual inline fun getFloatOrElse(key: String, defaultValue: () -> Float): Float =
        getSingleResultOrElse(key, defaultValue) { source.map[key] as? Float }

    actual inline fun getInt(key: String): Int =
        getSingleResultOrThrow(key) { source.map[key] as? Int ?: SavedStateUtils.DEFAULT_INT }

    actual inline fun getIntOrElse(key: String, defaultValue: () -> Int): Int =
        getSingleResultOrElse(key, defaultValue) { source.map[key] as? Int }

    actual inline fun getLong(key: String): Long =
        getSingleResultOrThrow(key) { source.map[key] as? Long ?: SavedStateUtils.DEFAULT_LONG }

    actual inline fun getLongOrElse(key: String, defaultValue: () -> Long): Long =
        getSingleResultOrElse(key, defaultValue) { source.map[key] as? Long }

    actual inline fun getString(key: String): String =
        getSingleResultOrThrow(key) { source.map[key] as? String }

    actual inline fun getStringOrElse(key: String, defaultValue: () -> String): String =
        getSingleResultOrElse(key, defaultValue) { source.map[key] as? String }

    @Suppress("UNCHECKED_CAST")
    actual inline fun getIntList(key: String): List<Int> {
        return getListResultOrThrow(key) { source.map[key] as? List<Int> }
    }

    @Suppress("UNCHECKED_CAST")
    actual inline fun getIntListOrElse(key: String, defaultValue: () -> List<Int>): List<Int> {
        return getListResultOrElse(key, defaultValue) { source.map[key] as? List<Int> }
    }

    @Suppress("UNCHECKED_CAST")
    actual inline fun getStringList(key: String): List<String> {
        return getListResultOrThrow(key) { source.map[key] as? List<String> }
    }

    @Suppress("UNCHECKED_CAST")
    actual inline fun getStringListOrElse(
        key: String,
        defaultValue: () -> List<String>
    ): List<String> {
        return getListResultOrElse(key, defaultValue) { source.map[key] as? List<String> }
    }

    actual inline fun getSavedState(key: String): SavedState =
        getSingleResultOrThrow(key) { source.map[key] as? SavedState }

    actual inline fun getSavedStateOrElse(key: String, defaultValue: () -> SavedState): SavedState =
        getSingleResultOrElse(key, defaultValue) { source.map[key] as? SavedState }

    actual inline fun size(): Int {
        return source.map.size
    }

    actual inline fun isEmpty(): Boolean {
        return source.map.isEmpty()
    }

    actual inline operator fun contains(key: String): Boolean {
        return source.map.containsKey(key)
    }

    @PublishedApi
    internal inline fun <reified T> getSingleResultOrThrow(
        key: String,
        currentValue: () -> T?,
    ): T =
        getValueFromSavedState(
            key = key,
            contains = { source.map.containsKey(key) },
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
            contains = { source.map.containsKey(key) },
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
            contains = { source.map.containsKey(key) },
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
            contains = { source.map.containsKey(key) },
            currentValue = { currentValue() },
            defaultValue = { defaultValue() },
        )

    actual fun contentDeepEquals(other: SavedState): Boolean {
        // Map implements `equals` as a content deep, there is no need to do anything else.
        return source.map == other.map
    }
}
