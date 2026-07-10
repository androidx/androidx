/*
 * Copyright 2025 The Android Open Source Project
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

package androidx.lifecycle

import androidx.kruth.assertThat
import kotlin.test.Test
import kotlin.test.assertFailsWith

class FastSafeIterableMapTest {

    @Test
    fun putAndRemove() {
        val map = FastSafeIterableMap<String, Int>()
        assertThat(map.putIfAbsent("a", 1)).isNull()
        assertThat(map.putIfAbsent("a", 2)).isEqualTo(1)
        assertThat(map.size()).isEqualTo(1)

        assertThat(map.remove("a")).isEqualTo(1)
        assertThat(map.remove("a")).isNull()
        assertThat(map.size()).isEqualTo(0)
    }

    @Test
    fun iterationFollowsInsertionOrder() {
        val map = FastSafeIterableMap<String, Int>()
        map.putIfAbsent("a", 1)
        map.putIfAbsent("b", 2)
        map.putIfAbsent("c", 3)

        val keys = mutableListOf<String>()
        map.forEachWithAdditions { keys.add(it.key) }

        assertThat(keys).containsExactly("a", "b", "c").inOrder()
    }

    @Test
    fun removeCurrentDuringIteration() {
        val map = FastSafeIterableMap<String, Int>()
        map.putIfAbsent("a", 1)
        map.putIfAbsent("b", 2)
        map.putIfAbsent("c", 3)

        val visited = mutableListOf<String>()
        map.forEachWithAdditions { entry ->
            visited.add(entry.key)
            if (entry.key == "b") {
                map.remove("b")
            }
        }

        assertThat(visited).containsExactly("a", "b", "c").inOrder()
        assertThat(map.contains("b")).isFalse()
    }

    @Test
    fun addDuringIteration() {
        val map = FastSafeIterableMap<String, Int>()
        map.putIfAbsent("a", 1)

        val visited = mutableListOf<String>()
        map.forEachWithAdditions { entry ->
            visited.add(entry.key)
            if (entry.key == "a") {
                map.putIfAbsent("b", 2)
            }
        }

        assertThat(visited).containsExactly("a", "b").inOrder()
    }

    @Test
    fun removeAllRemainingDuringIteration() {
        val map = FastSafeIterableMap<String, Int>()
        map.putIfAbsent("a", 1)
        map.putIfAbsent("b", 2)
        map.putIfAbsent("c", 3)

        val visited = mutableListOf<String>()
        map.forEachWithAdditions { entry ->
            visited.add(entry.key)
            map.remove("a")
            map.remove("b")
            map.remove("c")
        }

        assertThat(visited).containsExactly("a")
        assertThat(map.size()).isEqualTo(0)
    }

    @Test
    fun reAddRemovedElementDuringIteration() {
        val map = FastSafeIterableMap<String, Int>()
        map.putIfAbsent("a", 1)
        map.putIfAbsent("b", 2)

        val visited = mutableListOf<String>()
        map.forEachWithAdditions { entry ->
            visited.add(entry.key)
            if (entry.key == "a") {
                map.remove("a")
                map.putIfAbsent("a", 100)
            }
        }

        assertThat(visited).containsExactly("a", "b", "a").inOrder()
    }

    @Test
    fun nestedIterationSafety() {
        val map = FastSafeIterableMap<String, Int>()
        map.putIfAbsent("a", 1)
        map.putIfAbsent("b", 2)

        val results = mutableListOf<String>()
        map.forEachWithAdditions { outer ->
            results.add("outer:${outer.key}")
            map.forEachWithAdditions { inner ->
                results.add("inner:${inner.key}")
                if (inner.key == "a" && outer.key == "a") {
                    map.remove("a")
                }
            }
        }

        assertThat(results)
            .containsExactly("outer:a", "inner:a", "inner:b", "outer:b", "inner:b")
            .inOrder()
    }

    @Test
    fun emptyMapThrowsWithCustomMessage() {
        val map = FastSafeIterableMap<String, Int>()

        val e1 = assertFailsWith<NoSuchElementException> { map.first() }
        assertThat(e1.message).contains("Collection is empty.")

        val e2 = assertFailsWith<NoSuchElementException> { map.last() }
        assertThat(e2.message).contains("Collection is empty.")
    }
}
