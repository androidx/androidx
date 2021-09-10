/*
 * Copyright 2020 The Android Open Source Project
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

package androidx.collection

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertTrue
import kotlin.test.fail

class ArraySetKotlinTest {
    @Test
    fun testCanNotIteratePastEnd() {
        val set = ArraySet<String>()
        set.add("value")
        val iterator: Iterator<String> = set.iterator()
        assertTrue(iterator.hasNext())
        assertEquals("value", iterator.next())
        assertFalse(iterator.hasNext())
        assertTrue(runCatching { iterator.next() }.exceptionOrNull() is NoSuchElementException)
    }

    /**
     * Check to make sure the same operations behave as expected in a single thread.
     */
    @Test
    fun testNonConcurrentAccesses() {
        val set = ArraySet<String>()
        var i = 0
        while (i < 100000) {
            try {
                set.add("key $i")
                i++
                if (i % 200 == 0) {
                    print(".")
                }
                if (i % 500 == 0) {
                    set.clear()
                }
            } catch (e: ConcurrentModificationException) {
                fail(cause = e)
            }
            i++
        }
    }

    @Test fun addAllTypeProjection() = testBody {
        val set1 = ArraySet<Any>()
        val set2 = ArraySet<String>()
        set2.add("Foo")
        set2.add("Bar")
        set1.addAll(set2)
        assertEquals(set1.asIterable().toSet(), setOf("Bar", "Foo"))
    }

    /**
     * Test for implementation correction. This makes sure that all branches in ArraySet's
     * backstore shrinking code gets exercised.
     */
    @Test
    fun addAllThenRemoveOneByOne() {
        val sourceList = (0 until 10).toList()
        val set = ArraySet(sourceList)
        assertEquals(sourceList.size, set.size)

        for (e in sourceList) {
            assertTrue(set.contains(e))
            set.remove(e)
        }
        assertTrue(set.isEmpty())
    }

    /**
     * Test for implementation correction of indexOf.
     */
    @Test
    fun addObjectsWithSameHashCode() {
        class Value {
            override fun hashCode(): Int {
                return 42
            }
        }

        val sourceList = (0 until 10).map { Value() }
        val set = ArraySet(sourceList)
        assertEquals(sourceList.size, set.size)

        for (e in sourceList) {
            assertEquals(e, set.valueAt(set.indexOf(e)))
        }
    }
}
