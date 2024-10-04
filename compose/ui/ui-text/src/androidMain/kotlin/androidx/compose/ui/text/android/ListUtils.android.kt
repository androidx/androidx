/*
 * Copyright 2021 The Android Open Source Project
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

package androidx.compose.ui.text.android

import kotlin.contracts.ExperimentalContracts
import kotlin.contracts.contract

/**
 * Iterates through a [List] using the index and calls [action] for each item. This does not
 * allocate an iterator like [Iterable.forEach].
 *
 * **Do not use for collections that come from public APIs**, since they may not support random
 * access in an efficient way, and this method may actually be a lot slower. Only use for
 * collections that are created by code we control and are known to support random access.
 */
@Suppress("BanInlineOptIn")
@OptIn(ExperimentalContracts::class)
internal inline fun <T> List<T>.fastForEach(action: (T) -> Unit) {
    contract { callsInPlace(action) }
    for (index in indices) {
        val item = get(index)
        action(item)
    }
}

/**
 * Applies the given [transform] function to each element of the original collection and appends the
 * results to the given [destination].
 *
 * **Do not use for collections that come from public APIs**, since they may not support random
 * access in an efficient way, and this method may actually be a lot slower. Only use for
 * collections that are created by code we control and are known to support random access.
 */
@Suppress("BanInlineOptIn")
@OptIn(ExperimentalContracts::class)
internal inline fun <T, R, C : MutableCollection<in R>> List<T>.fastMapTo(
    destination: C,
    transform: (T) -> R
): C {
    contract { callsInPlace(transform) }
    fastForEach { item -> destination.add(transform(item)) }
    return destination
}

/**
 * Returns a list containing the results of applying the given [transform] function to each pair of
 * two adjacent elements in this collection.
 *
 * The returned list is empty if this collection contains less than two elements.
 *
 * **Do not use for collections that come from public APIs**, since they may not support random
 * access in an efficient way, and this method may actually be a lot slower. Only use for
 * collections that are created by code we control and are known to support random access.
 */
@Suppress("BanInlineOptIn")
@OptIn(ExperimentalContracts::class)
internal inline fun <T, R> List<T>.fastZipWithNext(transform: (T, T) -> R): List<R> {
    contract { callsInPlace(transform) }
    if (size <= 1) return emptyList()
    val result = mutableListOf<R>()
    var current = get(0)
    // `until` as we don't want to invoke this for the last element, since that won't have a `next`
    for (i in 0 until lastIndex) {
        val next = get(i + 1)
        result.add(transform(current, next))
        current = next
    }
    return result
}
