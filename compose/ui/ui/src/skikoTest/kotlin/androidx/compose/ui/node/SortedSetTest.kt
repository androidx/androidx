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

package androidx.compose.ui.node

import kotlin.random.Random
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertTrue

class SortedSetTest {
    private fun <E: Comparable<E>> sortedSetOf(vararg elements: E): SortedSet<E> =
        sortedSetOf(naturalOrder(), *elements)

    private fun <E> sortedSetOf(comparator: Comparator<in E>, vararg elements: E): SortedSet<E> {
        val set = SortedSet(comparator)
        for (element in elements) {
            set.add(element)
            assertTrue(set.contains(element))
        }
        return set
    }

    private fun <E> assertOrderEquals(
        expect: Iterable<E>,
        actual: SortedSet<E>,
        message: String? = null
    ) {
        for (e in expect) {
            assertEquals(e, actual.first())
            assertTrue(actual.contains(e))
            assertTrue(actual.remove(e))
            assertFalse(actual.contains(e))
        }
        assertTrue(actual.isEmpty(), message)
    }

    @Test
    fun correctOrder() {
        assertOrderEquals(listOf(1), sortedSetOf(1))
        assertOrderEquals(listOf(1, 2, 5, 6), sortedSetOf(1, 2, 5, 6))
        assertOrderEquals(listOf(1, 2, 5, 6), sortedSetOf(2, 6, 1, 5))
        val (seed, _, numbers) = generateRandomInts(amount = 1000)
        logSeedOnFailure(seed) {
            val set = sortedSetOf(*numbers.toTypedArray())
            assertOrderEquals(numbers.sorted(), set, "Wrong order with seed $seed")
        }
    }

    @Test
    fun hashmapTest() {
        val map = mutableMapOf<Int, Int>()
        map[1] = 0
        map.remove(1)
        assertFalse(map.keys.contains(1))
    }

    @Test
    fun checkExistingWhenAdded() {
        val set = sortedSetOf(1, 2, 3, 4, 5)
        assertFalse(set.add(3))
        assertFalse(set.add(4))
        assertFalse(set.add(5))
        assertTrue(set.add(6))
        assertTrue(set.add(7))
    }

    @Test
    fun customComparator() {
        val set = sortedSetOf(compareBy { it.length }, "B", "AAA", "DD")
        assertOrderEquals(listOf("B", "DD", "AAA"), set)
    }

    @Test
    fun removeNonMember() {
        val set = sortedSetOf(1, 2, 3, 4, 5)
        assertFalse(set.remove(0))
    }

    @Test
    fun removeRandom() {
        val (seed, random, numbers) = generateRandomInts(amount = 1000, seed = -1290005190)
        logSeedOnFailure(seed) {
            val set = sortedSetOf(*numbers.toTypedArray())
            numbers.sort()
            val countToRemove = random.nextInt(numbers.size)
            repeat(countToRemove) {
                val index = random.nextInt(until = numbers.size)
                val number = numbers.removeAt(index)
                set.remove(number)
            }
            assertOrderEquals(numbers, set, "Wrong order after removing with seed $seed")
        }
    }

    @Test
    fun removeLastAdded() {
        val set = sortedSetOf(1, 2, 3)
        set.remove(3)
        assertOrderEquals(listOf(1, 2), set)
    }

    private fun generateRandomInts(
        amount: Int,
        seed: Int? = null
    ): Triple<Int, Random, MutableList<Int>> {
        val actualSeed = seed ?: Random.nextInt()
        val random = Random(actualSeed)
        val numbers = (1..amount)
            .mapTo(mutableSetOf()) { random.nextInt(10_000_000) }
            .toMutableList()
            .apply { shuffle(random) }
        return Triple(actualSeed, random, numbers)
    }
}

private inline fun logSeedOnFailure(seed: Int, block: () -> Unit) {
    try {
        block()
    } catch (e: Throwable) {
        println("Test failed with seed $seed")
        throw e
    }
}
