/*
 * Copyright 2023 The Android Open Source Project
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

/**
 * Propositions for [Map] subjects.
 *
 * @constructor Constructor for use by subclasses. If you want to create an instance of this class
 *   itself, call [check(...)][Subject.check].[that(actual)][StandardSubjectBuilder.that].
 */
open class MapSubject<K, V>
protected constructor(
    metadata: FailureMetadata,
    actual: Map<K, V>?,
) : Subject<Map<K, V>>(actual, metadata = metadata, typeDescriptionOverride = null) {

    internal constructor(actual: Map<K, V>?, metadata: FailureMetadata) : this(metadata, actual)

    /** Fails if the map is not empty. */
    fun isEmpty() {
        requireNonNull(actual)
        if (actual.isNotEmpty()) {
            failWithActual(simpleFact("expected to be empty"))
        }
    }

    /** Fails if the map is empty. */
    fun isNotEmpty() {
        requireNonNull(actual)
        if (actual.isEmpty()) {
            failWithoutActual(simpleFact("expected not to be empty"))
        }
    }

    /** Fails if expected size of map is not equal to actual. */
    fun hasSize(expectedSize: Int) {
        require(expectedSize >= 0) { "expectedSize ($expectedSize) must be >= 0" }
        check("size").that(requireNonNull(actual).size).isEqualTo(expectedSize)
    }

    /** Fails if the map does not contain the given key. */
    fun containsKey(key: Any?) {
        check("keys").that(requireNonNull(actual).keys).contains(key)
    }

    /** Fails if the map contains the given key. */
    fun doesNotContainKey(key: Any?) {
        check("keys").that(requireNonNull(actual).keys).doesNotContain(key)
    }

    /** Fails if the map does not contain the given entry. */
    fun containsEntry(key: K, value: V) {
        val entry = key to value

        requireNonNull(actual)

        if (actual.entries.any { (k, v) -> (k == key) && (v == value) }) {
            return
        }

        val keyList = listOf(key)
        val valueList = listOf(value)

        if (key in actual) {
            val actualValue = actual[key]
            /*
             * In the case of a null expected or actual value, clarify that the key *is* present
             * and *is* expected to be present. That is, get() isn't returning null to indicate
             * that the key is missing, and the user isn't making an assertion that the key is
             * missing.
             */
            if ((value == null) || (actualValue == null)) {
                failWithActual(
                    fact("Expected to contain entry", entry),
                    simpleFact("key is present but with a different value"),
                )
            } else {
                failWithActual(fact("Expected to contain entry", entry))
            }
        } else if (actual.keys.hasMatchingToStringPair(keyList)) {
            failWithActual(
                fact("Expected to contain entry", entry),
                fact("an instance of", entry.typeName()),
                simpleFact("but did not"),
                fact(
                    "though it did contain keys",
                    actual.keys.retainMatchingToString(keyList).countDuplicatesAndAddTypeInfo(),
                ),
            )
        } else if (actual.containsValue(value)) {
            val keys = actual.filterValues { it == value }.keys
            failWithActual(
                fact("Expected to contain entry", entry),
                simpleFact("but did not"),
                fact("though it did contain keys with that value", keys),
            )
        } else if (actual.values.hasMatchingToStringPair(valueList)) {
            failWithActual(
                fact("Expected to contain entry", entry),
                fact("an instance of", entry.typeName()),
                simpleFact("but did not"),
                fact(
                    "though it did contain values",
                    actual.values.retainMatchingToString(valueList).countDuplicatesAndAddTypeInfo(),
                ),
            )
        } else {
            failWithActual(fact("Expected to contain entry", entry))
        }
    }

    /** Fails if the map does not contain the given entry. */
    fun containsEntry(entry: Pair<K, V>) {
        containsEntry(key = entry.first, value = entry.second)
    }

    /** Fails if the map contains the given entry. */
    fun doesNotContainEntry(key: K, value: V) {
        val entry = key to value

        requireNonNull(actual)

        if (actual.entries.any { (k, v) -> (k == key) && (v == value) }) {
            failWithActual(fact("Expected not to contain", entry))
        }
    }

    /** Fails if the map contains the given entry. */
    fun doesNotContainEntry(entry: Pair<K, V>) {
        doesNotContainEntry(key = entry.first, value = entry.second)
    }

    /**
     * Fails if the map does not contain exactly the given set of key/value pairs. The arguments
     * must not contain duplicate keys.
     */
    fun containsExactly(vararg entries: Pair<K, V>): Ordered =
        containsExactlyEntriesIn(
            accumulateMap(
                functionName = "containsExactly",
                entries = entries.toList(),
            )
        )

    /**
     * Fails if the map does not contain at least the given set of key/value pairs. The arguments
     * must not contain duplicate keys.
     */
    fun containsAtLeast(vararg entries: Pair<K, V>): Ordered =
        containsAtLeastEntriesIn(
            accumulateMap(
                functionName = "containsAtLeast",
                entries = entries.toList(),
            )
        )

    /** Fails if the map does not contain exactly the given set of entries in the given map. */
    fun containsExactlyEntriesIn(expectedMap: Map<K, V>): Ordered {
        if (expectedMap.isEmpty()) {
            requireNonNull(actual)
            if (actual.isNotEmpty()) {
                isEmpty()
            }

            return NoopOrdered
        }

        containsEntriesInAnyOrder(expectedMap = expectedMap, allowUnexpected = false)

        return MapInOrder(expectedMap = expectedMap, allowUnexpected = false)
    }

    /** Fails if the map does not contain at least the given set of entries in the given map. */
    fun containsAtLeastEntriesIn(expectedMap: Map<K, V>): Ordered {
        if (expectedMap.isEmpty()) {
            return NoopOrdered
        }

        containsEntriesInAnyOrder(expectedMap = expectedMap, allowUnexpected = true)

        return MapInOrder(expectedMap = expectedMap, allowUnexpected = true)
    }

    private fun containsEntriesInAnyOrder(
        expectedMap: Map<K, V>,
        allowUnexpected: Boolean,
    ) {
        val actual = requireNonNull(actual)
        val expectedSet = expectedMap.mapTo(HashSet()) { (key, value) -> key to value }
        val actualSet = actual.mapTo(HashSet()) { (key, value) -> key to value }

        if (allowUnexpected) {
            if (!actualSet.containsAll(expectedSet)) {
                failWithActual("Expected to contain at least", expectedSet)
            }
        } else {
            if (expectedSet != actualSet) {
                failWithActual("Expected", expectedMap)
            }
        }
    }

    private fun <K, V> accumulateMap(functionName: String, entries: List<Pair<K, V>>): Map<K, V> {
        val keyToValuesMap = entries.groupBy { it.first }

        require(keyToValuesMap.size == entries.size) {
            val keysStr = keyToValuesMap.map { (k, v) -> "$k x ${v.size}" }
            "Duplicate keys ($keysStr) cannot be passed to $functionName()."
        }

        return entries.toMap()
    }

    private inner class MapInOrder(
        private val expectedMap: Map<*, *>,
        private val allowUnexpected: Boolean,
    ) : Ordered {

        /**
         * Checks whether the common elements between actual and expected are in the same order.
         *
         * This doesn't check whether the keys have the same values or whether all the required keys
         * are actually present. That was supposed to be done before the "in order" part.
         */
        override fun inOrder() {
            requireNonNull(actual)

            val commonFromExpected = expectedMap.keys.intersect(actual.keys).toList()
            val commonFromActual = actual.keys.intersect(expectedMap.keys).toList()

            metadata.assertEquals(commonFromExpected, commonFromActual) {
                buildString {
                    appendLine(
                        if (allowUnexpected) {
                            "Required entries were all found, but order was wrong."
                        } else {
                            "Entries match, but order was wrong."
                        }
                    )

                    appendLine(
                        if (allowUnexpected) {
                            "Expected to contain at least: $expectedMap."
                        } else {
                            "Expected: $expectedMap."
                        }
                    )

                    appendLine("Actual: $actual.")
                }
            }
        }
    }
}
