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

package androidx.collection

import kotlin.DeprecationLevel.HIDDEN

/** Returns the number of key/value pairs in the collection. */
public inline val <T> LongSparseArray<T>.size: Int get() = size()

/** Returns true if the collection contains [key]. */
public inline operator fun <T> LongSparseArray<T>.contains(key: Long): Boolean = containsKey(key)

/** Allows the use of the index operator for storing values in the collection. */
public inline operator fun <T> LongSparseArray<T>.set(key: Long, value: T): Unit = put(key, value)

/** Creates a new collection by adding or replacing entries from [other]. */
public operator fun <T> LongSparseArray<T>.plus(other: LongSparseArray<T>): LongSparseArray<T> {
    val new = LongSparseArray<T>(size() + other.size())
    new.putAll(this)
    new.putAll(other)
    return new
}

/** Return the value corresponding to [key], or [defaultValue] when not present. */
public inline fun <T> LongSparseArray<T>.getOrDefault(key: Long, defaultValue: T): T =
    get(key, defaultValue)

/** Return the value corresponding to [key], or from [defaultValue] when not present. */
public inline fun <T> LongSparseArray<T>.getOrElse(key: Long, defaultValue: () -> T): T =
    get(key) ?: defaultValue()

/** Return true when the collection contains elements. */
public inline fun <T> LongSparseArray<T>.isNotEmpty(): Boolean = !isEmpty()

/** Removes the entry for [key] only if it is mapped to [value]. */
@Suppress("EXTENSION_SHADOWED_BY_MEMBER") // Binary API compatibility.
@Deprecated("Replaced with member function. Remove extension import!", level = HIDDEN)
public fun <T> LongSparseArray<T>.remove(key: Long, value: T): Boolean = remove(key, value)

/** Performs the given [action] for each key/value entry. */
public inline fun <T> LongSparseArray<T>.forEach(action: (key: Long, value: T) -> Unit) {
    for (index in 0 until size()) {
        action(keyAt(index), valueAt(index))
    }
}

/** Return an iterator over the collection's keys. */
public fun <T> LongSparseArray<T>.keyIterator(): LongIterator = object : LongIterator() {
    var index = 0
    override fun hasNext() = index < size()
    override fun nextLong() = keyAt(index++)
}

/** Return an iterator over the collection's values. */
public fun <T> LongSparseArray<T>.valueIterator(): Iterator<T> = object : Iterator<T> {
    var index = 0
    override fun hasNext() = index < size()
    override fun next() = valueAt(index++)
}
