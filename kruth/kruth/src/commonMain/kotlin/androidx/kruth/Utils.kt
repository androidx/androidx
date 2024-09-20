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

internal const val HUMAN_UNDERSTANDABLE_EMPTY_STRING = "\"\" (empty String)"

/**
 * Same as [requireNotNull] but throws [NullPointerException] instead of [IllegalArgumentException].
 *
 * Used for better behaviour compatibility with Truth, which uses Guava's checkNotNull.
 */
@OptIn(ExperimentalContracts::class)
@Suppress("NOTHING_TO_INLINE")
internal inline fun <T : Any> requireNonNull(value: T?): T {
    contract { returns() implies (value != null) }

    return value ?: throw NullPointerException()
}

/**
 * Same as [requireNotNull] but throws [NullPointerException] instead of [IllegalArgumentException].
 *
 * Used for better behaviour compatibility with Truth, which uses Guava's checkNotNull.
 */
@OptIn(ExperimentalContracts::class)
internal inline fun <T : Any> requireNonNull(
    value: T?,
    lazyMessage: () -> Any,
): T {
    contract { returns() implies (value != null) }

    return value ?: throw NullPointerException(lazyMessage().toString())
}

internal fun Iterable<*>.isEmpty(): Boolean =
    (this as? Collection<*>)?.isEmpty() ?: !iterator().hasNext()

/**
 * Returns a new collection containing all elements in [this] for which there exists at least one
 * element in [itemsToCheck] that has the same [toString][Any.toString] value without being equal.
 *
 * Example:
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

internal fun Iterable<*>.hasMatchingToStringPair(items: Iterable<*>): Boolean =
    if (isEmpty() || items.isEmpty()) {
        false // Bail early to avoid calling hashCode() on the elements unnecessarily.
    } else {
        retainMatchingToString(items).isNotEmpty()
    }

// TODO(b/317811086): Truth does some extra String processing here for nested classes and Subjects
//  for j2cl that we do not yet have implemented. It is possible we don't need anything, but we need
//  to double check and add a test.
internal fun Any?.typeName(): String =
    when (this) {
        null -> {
            // The name "null type" comes from the interface javax.lang.model.type.NullType
            "null type"
        }
        is Map.Entry<*, *> -> {
            // Fix for interesting bug when entry.getValue() returns itself b/170390717
            val valueTypeName = if (value === this) "Map.Entry" else value.typeName()
            "Map.Entry<${key.typeName()}, $valueTypeName>"
        }
        else -> this::class.simpleName ?: "unknown type"
    }

internal fun Iterable<*>.countDuplicatesAndAddTypeInfo(): String {
    val homogeneousTypeName = homogeneousTypeName()

    return if (homogeneousTypeName != null) {
        "${countDuplicates()} ($homogeneousTypeName)"
    } else {
        addTypeInfoToEveryItem().countDuplicates()
    }
}

internal fun Iterable<*>.countDuplicates(): String =
    groupingBy { it }
        .eachCount()
        .entries
        .joinToString(
            prefix = "[",
            postfix = "]",
            transform = { (item, count) ->
                if (count > 1) "$item [$count copies]" else item.toString()
            },
        )

/** Returns the name of the single type of all given items or `null` if no such type exists. */
private fun Iterable<*>.homogeneousTypeName(): String? {
    var homogeneousTypeName: String? = null

    for (item in this) {
        when {
            item == null -> return null
            homogeneousTypeName == null -> homogeneousTypeName = item.typeName() // First item
            item.typeName() != homogeneousTypeName -> return null // Heterogeneous collection
        }
    }

    return homogeneousTypeName
}

private fun Iterable<*>.addTypeInfoToEveryItem(): List<String> = map { "$it (${it.typeName()})" }
