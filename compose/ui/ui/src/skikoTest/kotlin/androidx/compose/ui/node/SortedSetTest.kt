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

    @Test
    fun CMP_8935_Test() {
        // Test object that can have a custom hash code
        class TestNode(private val hashCodeValue: Int) : Comparable<TestNode> {
            override fun hashCode(): Int = hashCodeValue
            override fun equals(other: Any?): Boolean = other is TestNode && other.hashCodeValue == hashCodeValue
            override fun compareTo(other: TestNode): Int = hashCodeValue.compareTo(other.hashCodeValue)
            override fun toString(): String = "TestNode@$hashCodeValue"
        }

        // Create test objects with hash codes from the log
        val node156265344 = TestNode(156265344)
        val node148961824 = TestNode(148961824)
        val node148962400 = TestNode(148962400)
        val node148963552 = TestNode(148963552)
        val node148964704 = TestNode(148964704)
        val node148965856 = TestNode(148965856)
        val node148967008 = TestNode(148967008)
        val node148968160 = TestNode(148968160)
        val node148969312 = TestNode(148969312)
        val node148970464 = TestNode(148970464)
        val node148971616 = TestNode(148971616)
        val node148972768 = TestNode(148972768)
        val node148973920 = TestNode(148973920)
        val node148975072 = TestNode(148975072)
        val node148976224 = TestNode(148976224)
        val node148977376 = TestNode(148977376)
        val node148978528 = TestNode(148978528)
        val node148961248 = TestNode(148961248)
        val node148962976 = TestNode(148962976)
        val node148964128 = TestNode(148964128)
        val node148965280 = TestNode(148965280)
        val node148966432 = TestNode(148966432)
        val node148967584 = TestNode(148967584)
        val node148968736 = TestNode(148968736)
        val node148969888 = TestNode(148969888)
        val node148971040 = TestNode(148971040)
        val node148972192 = TestNode(148972192)
        val node148973344 = TestNode(148973344)
        val node148974496 = TestNode(148974496)
        val node148975648 = TestNode(148975648)
        val node148976800 = TestNode(148976800)
        val node148977952 = TestNode(148977952)
        val node148979104 = TestNode(148979104)

        val set = SortedSet<TestNode>(naturalOrder())

        // Reproduce the exact sequence from the log
        set.add(node156265344)
        set.remove(node156265344)
        set.add(node156265344)
        set.remove(node156265344)
        set.add(node148961824)
        set.add(node148962400)
        set.add(node148963552)
        set.add(node148964704)
        set.add(node148965856)
        set.add(node148967008)
        set.add(node148968160)
        set.add(node148969312)
        set.add(node148970464)
        set.add(node148971616)
        set.add(node148972768)
        set.add(node148973920)
        set.add(node148975072)
        set.add(node148976224)
        set.add(node148977376)
        set.add(node148978528)
        set.remove(node148961824)
        set.remove(node148962400)
        set.remove(node148963552)
        set.remove(node148964704)
        set.remove(node148965856)
        set.remove(node148967008)
        set.remove(node148968160)
        set.remove(node148969312)
        set.remove(node148970464)
        set.remove(node148971616)
        set.remove(node148972768)
        set.remove(node148973920)
        set.remove(node148975072)
        set.remove(node148976224)
        set.remove(node148977376)
        set.remove(node148978528)
        set.add(node148961248)
        set.remove(node148962976)
        set.remove(node148962400)
        set.remove(node148964128)
        set.remove(node148963552)
        set.remove(node148965280)
        set.remove(node148964704)
        set.remove(node148966432)
        set.remove(node148965856)
        set.remove(node148967584)
        set.remove(node148967008)
        set.remove(node148968736)
        set.remove(node148968160)
        set.remove(node148969888)
        set.remove(node148969312)
        set.remove(node148971040)
        set.remove(node148970464)
        set.remove(node148972192)
        set.remove(node148971616)
        set.remove(node148973344)
        set.remove(node148972768)
        set.remove(node148974496)
        set.remove(node148973920)
        set.remove(node148975648)
        set.remove(node148975072)
        set.remove(node148976800)
        set.remove(node148976224)
        set.remove(node148977952)
        set.remove(node148977376)
        set.remove(node148979104)
        set.remove(node148978528)
        set.remove(node148961824)
        set.remove(node148961248)

        // If we reach here without crashing, the test passes
        assertTrue(set.isEmpty())
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
