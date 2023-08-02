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

package androidx.compose.runtime.collection

import kotlin.random.Random
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertTrue
import kotlinx.test.IgnoreAndroidUnitTestTarget

// Run on all platforms except Android unit tests. IntMap on Android is backed by the platform
// SparseArray class, which is not implemented and will no-op on outside of an instrumented
// environment.
@IgnoreAndroidUnitTestTarget
class IntMapTest {

    @Test
    fun addingValue_increasesSize() {
        val subject = IntMap<Int>(10)
        subject[1] = 2
        assertEquals(subject.size, 1)

        subject[2] = 3
        assertEquals(subject.size, 2)

        subject[1] = 5
        assertEquals(subject.size, 2)
    }

    @Test
    fun setValue_canBeRetrieved() {
        val subject = IntMap<Int>(10)
        val added = mutableSetOf<Int>()
        for (i in 1..1000) {
            val next = Random.nextInt(i)
            added.add(next)
            subject[next] = next
        }
        for (item in added) {
            assertEquals(subject[item], item)
        }
    }

    @Test
    fun removingValue_decreasesSize() {
        val (subject, added) = makeRandom100()
        val item = added.first()
        subject.remove(item)
        assertEquals(subject.size, added.size - 1)
        assertEquals(subject[item], null)
    }

    @Test
    fun removedValue_canBeSet() {
        val (subject, added) = makeRandom100()
        val item = added.first()
        subject.remove(item)
        subject[item] = -1
        assertEquals(subject[item], -1)
    }

    @Test
    fun clear_clears() {
        val (subject, added) = makeRandom100()
        subject.clear()
        for (item in added) {
            assertEquals(subject[item], null)
        }

        val item = added.first()
        subject[item] = -1
        assertEquals(subject[item], -1)
    }

    @Test(timeout = 5_000)
    fun set() {
        val map = IntMap<String>()
        map.set(900, "9")
        assertTrue(900 in map)
        map.set(800, "8")
        assertTrue(800 in map)
        map.set(700, "7")
        assertTrue(700 in map)
        map.set(600, "6")
        assertTrue(600 in map)
        map.set(500, "5")
        assertTrue(500 in map)
        map.set(100, "1")
        assertTrue(100 in map)
        map.set(200, "2")
        assertTrue(200 in map)
        map.set(300, "3")
        assertTrue(300 in map)
        map.set(400, "4")
        assertTrue(400 in map)
    }

    @Test(timeout = 5_000)
    fun get() {
        val map = IntMap<String>()
        map.set(900, "9")
        assertEquals("9", map[900])
        map.set(800, "8")
        assertEquals("8", map[800])
        map.set(700, "7")
        assertEquals("7", map[700])
        map.set(600, "6")
        assertEquals("6", map[600])
        map.set(500, "5")
        assertEquals("5", map[500])
        map.set(100, "1")
        assertEquals("1", map[100])
        map.set(200, "2")
        assertEquals("2", map[200])
        map.set(300, "3")
        assertEquals("3", map[300])
        map.set(400, "4")
        assertEquals("4", map[400])
    }

    @Test(timeout = 5_000)
    fun remove() {
        val map = IntMap<String>()
        map.set(900, "9")
        map.set(800, "8")
        map.set(700, "7")
        map.set(600, "6")
        map.set(500, "5")
        map.set(100, "1")
        map.set(200, "2")
        map.set(300, "3")
        map.set(400, "4")

        map.remove(700)
        assertFalse(700 in map)
        map.remove(800)
        assertFalse(800 in map)
        map.remove(600)
        assertFalse(600 in map)
        map.remove(300)
        assertFalse(300 in map)
        map.remove(100)
        assertFalse(100 in map)
        map.remove(900)
        assertFalse(900 in map)
        map.remove(500)
        assertFalse(500 in map)
        map.remove(200)
        assertFalse(200 in map)
        map.remove(400)
        assertFalse(400 in map)
    }

    private fun makeRandom100(): Pair<IntMap<Int>, MutableSet<Int>> {
        val subject = IntMap<Int>(10)
        val added = mutableSetOf<Int>()
        for (i in 1..1000) {
            val next = Random.nextInt(i)
            added.add(next)
            subject[next] = next
        }
        for (item in added) {
            assertEquals(subject[item], item)
        }
        return subject to added
    }
}
