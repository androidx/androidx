/*
 * Copyright 2020 The Android Open Source Project
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

package androidx.ui.util

/**
 * Iterates through a [List] using the index and calls [action] for each item.
 * This does not allocate an iterator like [Iterable.forEach].
 */
inline fun <T> List<T>.fastForEach(action: (T) -> Unit) {
    for (index in indices) {
        val item = get(index)
        action(item)
    }
}

/**
 * Iterates through a [List] using the index and calls [action] for each item.
 * This does not allocate an iterator like [Iterable.forEachIndexed].
 */
inline fun <T> List<T>.fastForEachIndexed(action: (Int, T) -> Unit) {
    for (index in indices) {
        val item = get(index)
        action(index, item)
    }
}

/**
 * Returns `true` if at least one element matches the given [predicate].
 */
inline fun <T> List<T>.fastAny(predicate: (T) -> Boolean): Boolean {
    fastForEach { if (predicate(it)) return true }
    return false
}

/**
 * Returns the first value that [predicate] returns `true` for or `null` if nothing matches.
 */
inline fun <T> List<T>.fastFirstOrNull(predicate: (T) -> Boolean): T? {
    fastForEach { if (predicate(it)) return it }
    return null
}