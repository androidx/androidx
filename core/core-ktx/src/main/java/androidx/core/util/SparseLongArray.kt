/*
 * Copyright (C) 2017 The Android Open Source Project
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *       http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

@file:Suppress("NOTHING_TO_INLINE") // Aliases to public API.

package androidx.core.util

import android.util.SparseLongArray

/** Returns the number of key/value entries in the collection. */
public inline val SparseLongArray.size: Int get() = size()

/** Returns true if the collection contains [key]. */
public inline operator fun SparseLongArray.contains(key: Int): Boolean = indexOfKey(key) >= 0

/** Allows the use of the index operator for storing values in the collection. */
public inline operator fun SparseLongArray.set(key: Int, value: Long): Unit = put(key, value)

/** Creates a new collection by adding or replacing entries from [other]. */
public operator fun SparseLongArray.plus(other: SparseLongArray): SparseLongArray {
    val new = SparseLongArray(size() + other.size())
    new.putAll(this)
    new.putAll(other)
    return new
}

/** Returns true if the collection contains [key]. */
public inline fun SparseLongArray.containsKey(key: Int): Boolean = indexOfKey(key) >= 0

/** Returns true if the collection contains [value]. */
public inline fun SparseLongArray.containsValue(value: Long): Boolean = indexOfValue(value) >= 0

/** Return the value corresponding to [key], or [defaultValue] when not present. */
public inline fun SparseLongArray.getOrDefault(key: Int, defaultValue: Long): Long =
    get(key, defaultValue)

/** Return the value corresponding to [key], or from [defaultValue] when not present. */
public inline fun SparseLongArray.getOrElse(key: Int, defaultValue: () -> Long): Long =
    indexOfKey(key).let { if (it >= 0) valueAt(it) else defaultValue() }

/** Return true when the collection contains no elements. */
public inline fun SparseLongArray.isEmpty(): Boolean = size() == 0

/** Return true when the collection contains elements. */
public inline fun SparseLongArray.isNotEmpty(): Boolean = size() != 0

/** Removes the entry for [key] only if it is set to [value]. */
public fun SparseLongArray.remove(key: Int, value: Long): Boolean {
    val index = indexOfKey(key)
    if (index >= 0 && value == valueAt(index)) {
        removeAt(index)
        return true
    }
    return false
}

/** Update this collection by adding or replacing entries from [other]. */
public fun SparseLongArray.putAll(other: SparseLongArray): Unit = other.forEach(::put)

/** Performs the given [action] for each key/value entry. */
public inline fun SparseLongArray.forEach(action: (key: Int, value: Long) -> Unit) {
    for (index in 0 until size()) {
        action(keyAt(index), valueAt(index))
    }
}

/** Return an iterator over the collection's keys. */
public fun SparseLongArray.keyIterator(): IntIterator = object : IntIterator() {
    var index = 0
    override fun hasNext() = index < size()
    override fun nextInt() = keyAt(index++)
}

/** Return an iterator over the collection's values. */
public fun SparseLongArray.valueIterator(): LongIterator = object : LongIterator() {
    var index = 0
    override fun hasNext() = index < size()
    override fun nextLong() = valueAt(index++)
}
