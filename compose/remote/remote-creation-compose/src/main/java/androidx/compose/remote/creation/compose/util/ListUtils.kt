/*
 * Copyright 2026 The Android Open Source Project
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

package androidx.compose.remote.creation.compose.util

/** Iterates through a [List] using index-based access to avoid [Iterator] heap allocations. */
internal inline fun <T> List<T>.fastForEach(action: (T) -> Unit) {
    for (index in indices) {
        action(get(index))
    }
}

/**
 * Iterates through a [List] in reverse order using index-based access to avoid [Iterator] heap
 * allocations.
 */
internal inline fun <T> List<T>.fastForEachReversed(action: (T) -> Unit) {
    for (index in indices.reversed()) {
        action(get(index))
    }
}

/**
 * Returns a list containing the results of applying the given [transform] function to each element
 * in the original [List].
 */
internal inline fun <T, R> List<T>.fastMap(transform: (T) -> R): List<R> {
    val target = ArrayList<R>(size)
    for (index in indices) {
        target.add(transform(get(index)))
    }
    return target
}

/** Returns a list containing only elements matching the given [predicate]. */
internal inline fun <T> List<T>.fastFilter(predicate: (T) -> Boolean): List<T> {
    val target = ArrayList<T>(size)
    for (index in indices) {
        val item = get(index)
        if (predicate(item)) target.add(item)
    }
    return target
}

/**
 * Accumulates value starting with [initial] value and applying [operation] from left to right to
 * current accumulator value and each element.
 */
internal inline fun <T, R> List<T>.fastFold(initial: R, operation: (acc: R, T) -> R): R {
    var accumulator = initial
    for (index in indices) {
        accumulator = operation(accumulator, get(index))
    }
    return accumulator
}

/** Returns `true` if all elements match the given [predicate]. */
internal inline fun <T> List<T>.fastAll(predicate: (T) -> Boolean): Boolean {
    for (index in indices) {
        if (!predicate(get(index))) return false
    }
    return true
}

/**
 * Returns the first element matching the given [predicate], or `null` if no such element was found.
 */
internal inline fun <T> List<T>.fastFirstOrNull(predicate: (T) -> Boolean): T? {
    for (index in indices) {
        val item = get(index)
        if (predicate(item)) return item
    }
    return null
}
