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

/** Returns the number of key/value pairs in the collection. */
inline val <T> SparseArrayCompat<T>.size get() = size()

/** Returns true if the collection contains [key]. */
inline operator fun <T> SparseArrayCompat<T>.contains(key: Int) = indexOfKey(key) >= 0

/** Allows the use of the index operator for storing values in the collection. */
inline operator fun <T> SparseArrayCompat<T>.set(key: Int, value: T) = put(key, value)

/** Creates a new collection by adding or replacing entries from [other]. */
operator fun <T> SparseArrayCompat<T>.plus(other: SparseArrayCompat<T>): SparseArrayCompat<T> {
    val new = SparseArrayCompat<T>(size() + other.size())
    new.putAll(this)
    new.putAll(other)
    return new
}

/** Returns true if the collection contains [key]. */
inline fun <T> SparseArrayCompat<T>.containsKey(key: Int) = indexOfKey(key) >= 0

/** Returns true if the collection contains [value]. */
inline fun <T> SparseArrayCompat<T>.containsValue(value: T) = indexOfValue(value) != -1

/** Return the value corresponding to [key], or [defaultValue] when not present. */
inline fun <T> SparseArrayCompat<T>.getOrDefault(key: Int, defaultValue: T) =
    get(key) ?: defaultValue

/** Return the value corresponding to [key], or from [defaultValue] when not present. */
inline fun <T> SparseArrayCompat<T>.getOrElse(key: Int, defaultValue: () -> T) =
    get(key) ?: defaultValue()

/** Return true when the collection contains elements. */
inline fun <T> SparseArrayCompat<T>.isNotEmpty() = size() != 0

/** Removes the entry for [key] only if it is mapped to [value]. */
fun <T> SparseArrayCompat<T>.remove(key: Int, value: T): Boolean {
    val index = indexOfKey(key)
    if (index != -1 && value == valueAt(index)) {
        removeAt(index)
        return true
    }
    return false
}

/** Update this collection by adding or replacing entries from [other]. */
fun <T> SparseArrayCompat<T>.putAll(other: SparseArrayCompat<T>) = other.forEach(::put)

/** Performs the given [action] for each key/value entry. */
inline fun <T> SparseArrayCompat<T>.forEach(action: (key: Int, value: T) -> Unit) {
    for (index in 0 until size()) {
        action(keyAt(index), valueAt(index))
    }
}

/** Return an iterator over the collection's keys. */
fun <T> SparseArrayCompat<T>.keyIterator(): IntIterator = object : IntIterator() {
    var index = 0
    override fun hasNext() = index < size()
    override fun nextInt() = keyAt(index++)
}

/** Return an iterator over the collection's values. */
fun <T> SparseArrayCompat<T>.valueIterator(): Iterator<T> = object : Iterator<T> {
    var index = 0
    override fun hasNext() = index < size()
    override fun next() = valueAt(index++)
}
