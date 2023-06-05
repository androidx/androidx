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

class MapSubject<K, V> internal constructor(actual: Map<K, V>?) : Subject<Map<K, V>>(actual) {

    /** Fails if the map is not empty. */
    fun isEmpty() {
        requireNonNull(actual) { "Expected to be empty, but was null" }

        if (actual.isNotEmpty()) {
            asserter.fail("Expected to be empty, but was $actual")
        }
    }

    /** Fails if the map is empty. */
    fun isNotEmpty() {
        requireNonNull(actual) { "Expected to be not empty, but was null" }

        if (actual.isEmpty()) {
            asserter.fail("Expected to be not empty, but was $actual")
        }
    }

    /** Fails if expected size of map is not equal to actual. */
    fun hasSize(expectedSize: Int) {
        require(expectedSize >= 0) { "expectedSize must be >= 0, but was $expectedSize" }
        requireNonNull(actual) { "Expected to be empty, but was null" }
        asserter.assertEquals(expectedSize, actual.size)
    }

    /** Fails if the map does not contain the given key. */
    fun containsKey(key: Any?) {
        requireNonNull(actual) { "Expected to contain $key, but was null" }

        if (!actual.containsKey(key)) {
            asserter.fail("Expected to contain $key, but was ${actual.keys}")
        }
    }

    /** Fails if the map does not contain exactly the given set of entries in the given map. */
    fun containsExactlyEntriesIn(expectedMap: Map<K, V>): Ordered {
        requireNonNull(actual) { "Expected $expectedMap, but was null" }

        if (expectedMap.isEmpty()) {
            isEmpty()
        } else if ((expectedMap.entries - actual.entries).isNotEmpty()) {
            asserter.fail("Expected $expectedMap, but was $actual")
        }

        return MapInOrder(expectedMap)
    }

    private inner class MapInOrder(
        private val expectedMap: Map<*, *>,
    ) : Ordered {

        /**
         * Checks whether the common elements between actual and expected are in the same order.
         *
         * This doesn't check whether the keys have the same values or whether all the required keys
         * are actually present. That was supposed to be done before the "in order" part.
         */
        override fun inOrder() {
            // We're using the fact that Iterable#intersect keeps the order of the first set.
            checkNotNull(actual)
            val expectedKeyOrder = (expectedMap.keys intersect actual.keys).toList()
            val actualKeyOrder = (actual.keys intersect expectedMap.keys).toList()
            if (actualKeyOrder != expectedKeyOrder) {
                asserter.fail(
                    "Entries match, but order was wrong. Expected $expectedMap, but was $actual",
                )
            }
        }
    }
}
