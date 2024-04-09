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

package androidx.kruth

import androidx.kruth.Fact.Companion.fact
import androidx.kruth.Fact.Companion.simpleFact
import androidx.kruth.Subject.Factory
import com.google.common.collect.ImmutableList
import com.google.common.collect.LinkedHashMultiset
import com.google.common.collect.LinkedListMultimap
import com.google.common.collect.ListMultimap
import com.google.common.collect.Maps.immutableEntry
import com.google.common.collect.Multimap
import com.google.common.collect.SetMultimap

/** Propositions for [Multimap] subjects. */
open class MultimapSubject<K, V> internal constructor(
    actual: Multimap<K, V>?,
    metadata: FailureMetadata = FailureMetadata(),
) : Subject<Multimap<K, V>>(actual, metadata, typeDescriptionOverride = "multimap") {

    /** Fails if the multimap is not empty. */
    fun isEmpty() {
        if (!requireNonNull(actual).isEmpty) {
            failWithActual(simpleFact("Expected to be empty"))
        }
    }

    /** Fails if the multimap is empty. */
    fun isNotEmpty() {
        if (requireNonNull(actual).isEmpty) {
            failWithoutActual(simpleFact("Expected not to be empty"))
        }
    }

    /** Fails if the multimap does not have the given size. */
    fun hasSize(expectedSize: Int) {
        require(expectedSize >= 0) { "expectedSize($expectedSize) must be >= 0" }
        // TODO: Use check("size()") once the API is there
        check().that(requireNonNull(actual).size()).isEqualTo(expectedSize)
    }

    /** Fails if the multimap does not contain the given key. */
    fun containsKey(key: K) {
        // TODO: Use check("keySet()") once the API is there
        check().that(requireNonNull(actual).keySet()).contains(key)
    }

    /** Fails if the multimap contains the given key. */
    fun doesNotContainKey(key: K) {
        // TODO: Use check("keySet()") once the API is there
        check().that(requireNonNull(actual).keySet()).doesNotContain(key)
    }

    /** Fails if the multimap does not contain the given entry. */
    fun containsEntry(key: K, value: Any?) {
        requireNonNull(actual)

        if (!actual.containsEntry(key, value)) {
            val entry = immutableEntry(key, value)
            val entryList = ImmutableList.of(entry)
            if (actual.entries().hasMatchingToStringPair(entryList)) {
                failWithActual(
                    fact("expected to contain entry", entry),
                    fact("an instance of", entry.typeName()),
                    simpleFact("but did not"),
                    fact(
                        "though it did contain",
                        actual.entries()
                            .retainMatchingToString(entryList)
                            .countDuplicatesAndAddTypeInfo(),
                    ),
                )
            } else if (actual.containsKey(key)) {
                failWithActual(
                    fact("expected to contain entry", entry),
                    simpleFact("but did not"),
                    fact("though it did contain values with that key", actual.asMap()[key]),
                )
            } else if (actual.containsValue(value)) {
                val keys =
                    actual.entries()
                        .asSequence()
                        .filter { it.value == value }
                        .map { it.key }
                        .toList()

                failWithActual(
                    fact("expected to contain entry", entry),
                    simpleFact("but did not"),
                    fact("though it did contain keys with that value", keys),
                )
            } else {
                failWithActual("expected to contain entry", immutableEntry(key, value))
            }
        }
    }

    /** Fails if the multimap contains the given entry.  */
    fun doesNotContainEntry(key: K, value: Any?) {
        // TODO: Use checkNoNeedToDisplayBothValues("entries()") when the API is there?
        check()
            .that(requireNonNull(actual).entries())
            .doesNotContain(immutableEntry(key, value))
    }

    /**
     * Returns a context-aware [Subject] for making assertions about the values for the given
     * key within the [Multimap].
     *
     * This method performs no checks on its own and cannot cause test failures. Subsequent
     * assertions must be chained onto this method call to test properties of the [Multimap].
     */
    open fun valuesForKey(key: K): IterableSubject<V> {
        // TODO: Use check("valuesForKey($key)") once the API is the there
        return check().that(requireNonNull(actual).get(key))
    }

    override fun isEqualTo(expected: Any?) {
        if (actual == expected) {
            return
        }

        // Fail but with a more descriptive message:
        if (((actual is ListMultimap) && (expected is SetMultimap<*, *>)) ||
            ((actual is SetMultimap) && (expected is ListMultimap<*, *>))
        ) {
            val actualType = if (actual is ListMultimap) "ListMultimap" else "SetMultimap"
            val otherType = if (expected is ListMultimap<*, *>) "ListMultimap" else "SetMultimap"
            failWithoutActual(
                fact("expected", expected),
                fact("an instance of", otherType),
                fact("but was", actual),
                fact("an instance of", actualType),
                simpleFact("a $actualType cannot equal a $otherType if either is non-empty"),
            )
        } else if (actual is ListMultimap) {
            @Suppress("UNCHECKED_CAST")
            containsExactlyEntriesIn(requireNonNull(expected) as Multimap<K, *>).inOrder()
        } else if (actual is SetMultimap<*, *>) {
            @Suppress("UNCHECKED_CAST")
            containsExactlyEntriesIn(requireNonNull(expected) as Multimap<K, *>)
        } else {
            super.isEqualTo(expected)
        }
    }

    /**
     * Fails if the [Multimap] does not contain precisely the same entries as the argument
     * [Multimap].
     *
     *
     * A subsequent call to [Ordered.inOrder] may be made if the caller wishes to verify that
     * the two multimaps iterate fully in the same order. That is, their key sets iterate in the same
     * order, and the value collections for each key iterate in the same order.
     */
    fun containsExactlyEntriesIn(expectedMultimap: Multimap<K, *>?): Ordered {
        requireNonNull(actual)
        requireNonNull(expectedMultimap)

        val missing = difference(expectedMultimap, actual)
        val extra = difference(actual, expectedMultimap)

        // TODO(kak): Possible enhancement: Include "[1 copy]" if the element does appear in
        // the subject but not enough times. Similarly for unexpected extra items.
        if (!missing.isEmpty) {
            if (!extra.isEmpty) {
                val addTypeInfo = missing.entries().hasMatchingToStringPair(extra.entries())
                // Note: The usage of countDuplicatesAndAddTypeInfo() below causes entries no longer to be
                // grouped by key in the 'missing' and 'unexpected items' parts of the message (we still
                // show the actual and expected multimaps in the standard format).

                val missingDisplay =
                    if (addTypeInfo) {
                        missing.annotateEmptyStrings().entries().countDuplicatesAndAddTypeInfo()
                    } else {
                        missing.annotateEmptyStrings().countDuplicates()
                    }

                val extraDisplay =
                    if (addTypeInfo) {
                        extra.annotateEmptyStrings().entries().countDuplicatesAndAddTypeInfo()
                    } else {
                        extra.annotateEmptyStrings().countDuplicates()
                    }

                failWithActual(
                    fact("missing", missingDisplay),
                    fact("unexpected", extraDisplay),
                    simpleFact("---"),
                    fact("expected", expectedMultimap.annotateEmptyStrings()),
                )
            } else {
                failWithActual(
                    fact("missing", missing.annotateEmptyStrings().countDuplicates()),
                    simpleFact("---"),
                    fact("expected", expectedMultimap.annotateEmptyStrings()),
                )
            }
        } else if (!extra.isEmpty) {
            failWithActual(
                fact("unexpected", extra.annotateEmptyStrings().countDuplicates()),
                simpleFact("---"),
                fact("expected", expectedMultimap.annotateEmptyStrings()),
            )
        }

        return MultimapInOrder(allowUnexpected = false, expectedMultimap = expectedMultimap)
    }

    /**
     * Fails if the [Multimap] does not contain at least the entries in the argument [ ].
     *
     *
     * A subsequent call to [Ordered.inOrder] may be made if the caller wishes to verify that
     * the entries are present in the same order as given. That is, the keys are present in the given
     * order in the key set, and the values for each key are present in the given order order in the
     * value collections.
     */
    fun containsAtLeastEntriesIn(expectedMultimap: Multimap<K, *>?): Ordered {
        requireNonNull(actual)
        requireNonNull(expectedMultimap)

        val missing = difference(expectedMultimap, actual)

        if (!missing.isEmpty) {
            failWithActual(
                fact("missing", missing.annotateEmptyStrings().countDuplicates()),
                simpleFact("---"),
                fact("expected to contain at least", expectedMultimap.annotateEmptyStrings()),
            )
        }

        return MultimapInOrder(allowUnexpected = true, expectedMultimap = expectedMultimap)
    }

    /** Fails if the multimap is not empty.  */
    fun containsExactly(): Ordered =
        check()
            .about(iterableEntries())
            .that(checkNotNull(actual).entries())
            .containsExactly()

    /** Fails if the multimap does not contain exactly the given set of key/value pairs. */
    fun containsExactly(vararg entries: Pair<K, *>): Ordered {
        return containsExactlyEntriesIn(accumulateMultimap(entries))
    }

    /** Fails if the multimap does not contain at least the given key/value pairs. */
    fun containsAtLeast(vararg entries: Pair<K, *>): Ordered {
        return containsAtLeastEntriesIn(accumulateMultimap(entries))
    }

    private fun iterableEntries(): Factory<IterableSubject<*>, Iterable<*>> =
        Factory<IterableSubject<*>, Iterable<*>> { metadata, actual ->
            IterableSubject(actual, metadata)
        }

    private inner class MultimapInOrder(
        private val allowUnexpected: Boolean,
        private val expectedMultimap: Multimap<K, *>,
    ) : Ordered {
        /**
         * Checks whether entries in expected appear in the same order in actual.
         *
         *
         * We allow for actual to have more items than the expected to support both [ ][.containsExactly] and [.containsAtLeast].
         */
        override fun inOrder() {
            requireNonNull(actual)

            // We use the fact that Set#intersect's result has the same order as the first parameter
            val keysInOrder =
                actual.keySet().intersect(expectedMultimap.keySet()) == expectedMultimap.keySet()

            val keysWithValuesOutOfOrder = LinkedHashSet<K>()
            for (key in expectedMultimap.keySet()) {
                val actualVals = actual[key].toList()
                val expectedVals = expectedMultimap[key].toList()
                val actualIterator = actualVals.iterator()
                for (value in expectedVals) {
                    if (!actualIterator.advanceToFind(value)) {
                        keysWithValuesOutOfOrder.add(key)
                        break
                    }
                }
            }

            if (!keysInOrder) {
                if (keysWithValuesOutOfOrder.isNotEmpty()) {
                    failWithActual(
                        simpleFact("contents match, but order was wrong"),
                        simpleFact("keys are not in order"),
                        fact("keys with out-of-order values", keysWithValuesOutOfOrder),
                        simpleFact("---"),
                        fact(
                            if (allowUnexpected) "expected to contain at least" else "expected",
                            expectedMultimap,
                        ),
                    )
                } else {
                    failWithActual(
                        simpleFact("contents match, but order was wrong"),
                        simpleFact("keys are not in order"),
                        simpleFact("---"),
                        fact(
                            if (allowUnexpected) "expected to contain at least" else "expected",
                            expectedMultimap,
                        ),
                    )
                }
            } else if (keysWithValuesOutOfOrder.isNotEmpty()) {
                failWithActual(
                    simpleFact("contents match, but order was wrong"),
                    fact("keys with out-of-order values", keysWithValuesOutOfOrder),
                    simpleFact("---"),
                    fact(
                        if (allowUnexpected) "expected to contain at least" else "expected",
                        expectedMultimap,
                    ),
                )
            }
        }
    }
}

private fun <K, V> difference(
    minuend: Multimap<K, out V>,
    subtrahend: Multimap<in K, out V>,
): ListMultimap<K, V> {
    val difference = LinkedListMultimap.create<K, V>()
    for (key: K in minuend.keySet()) {
        val a = minuend.get(key).toMutableList()
        val b = subtrahend.get(key).toMutableList()
        difference.putAll(key, difference(a, b))
    }

    return difference
}

private fun <T> difference(minuend: Iterable<T>, subtrahend: Iterable<T>): List<T> {
    val remaining = LinkedHashMultiset.create(subtrahend)
    return minuend.filterNot(remaining::remove)
}

private fun <K> Multimap<K, *>.countDuplicates(): String =
    keySet().joinToString(
        prefix = "{",
        postfix = "}",
        transform = { key -> "$key=${get(key).countDuplicates()}" },
    )

/**
 * Returns a multimap with all empty strings (as keys or values) replaced by a non-empty human
 * understandable indicator for an empty string.
 *
 *
 * Returns the given multimap if it contains no empty strings.
 */
private fun <K, V> Multimap<K, V>.annotateEmptyStrings(): Multimap<K, V> =
    if (containsKey("") || containsValue("")) {
        val annotatedMultimap = LinkedListMultimap.create<K, V>()
        for ((k, v) in entries()) {
            @Suppress("UNCHECKED_CAST")
            val key: K = (if (k == "") HUMAN_UNDERSTANDABLE_EMPTY_STRING as K else k)
            @Suppress("UNCHECKED_CAST")
            val value: V = (if (v == "") HUMAN_UNDERSTANDABLE_EMPTY_STRING as V else v)
            annotatedMultimap.put(key, value)
        }
        annotatedMultimap
    } else {
        this
    }

/**
 * Advances the iterator until it either returns value, or has no more elements.
 *
 * Returns true if the value was found, false if the end was reached before finding it.
 *
 * This is basically the same as [com.google.common.collect.Iterables.contains], but where the
 * contract explicitly states that the iterator isn't advanced beyond the value if the value is
 * found.
 */
private fun <T> Iterator<T>.advanceToFind(value: T): Boolean {
    while (hasNext()) {
        if (next() == value) {
            return true
        }
    }

    return false
}

private fun <K, V> accumulateMultimap(entries: Array<out Pair<K, V>>): ListMultimap<K, V> =
    LinkedListMultimap.create<K, V>().also { map ->
        entries.forEach { (k, v) -> map.put(k, v) }
    }
