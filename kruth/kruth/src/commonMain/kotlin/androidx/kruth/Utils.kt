/*
 * Copyright 2022 The Android Open Source Project
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

package androidx.kruth

import kotlin.contracts.ExperimentalContracts
import kotlin.contracts.contract

/**
 * Same as [requireNotNull] but throws [NullPointerException] instead of [IllegalArgumentException].
 * Used for better behaviour compatibility with Truth.
 */
@OptIn(ExperimentalContracts::class)
internal inline fun <T : Any> requireNonNull(
    value: T?,
    lazyMessage: () -> Any = { "Required value was null." },
): T {
    contract {
        returns() implies (value != null)
    }

    return value ?: throw NullPointerException(lazyMessage().toString())
}

internal fun Iterable<*>.isEmpty(): Boolean =
    (this as? Collection<*>)?.isEmpty() ?: !iterator().hasNext()

/**
 * Returns a new collection containing all elements in [this] for which there exists at
 * least one element in [itemsToCheck] that has the same [toString][Any.toString] value without
 * being equal.
 *
 * Example:
 *
 * ```
 * listOf(1L, 2L, 2L).retainMatchingToString(listOf(2, 3)) == listOf(2L, 2L)
 * ```
 */
internal fun <T> Iterable<T>.retainMatchingToString(itemsToCheck: Iterable<T>): List<T> {
    val stringValueToItemsToCheck by lazy { itemsToCheck.groupBy(Any?::toString) }

    return filter { item ->
        val list = stringValueToItemsToCheck[item.toString()]
        (list != null) && (item !in list)
    }
}
