/*
 * Copyright (C) 2023 The Android Open Source Project
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
package androidx.compose.remote.core

import androidx.compose.remote.core.operations.utilities.IntMap
import kotlin.random.Random
import org.junit.Assert.assertEquals
import org.junit.Test

class IntMapTest {
    @Test
    fun testIntMap() {
        assertEquals(2, 1 + 1)
        val map = IntMap<String>()
        map.put(1, "One")
        map.put(2, "Two")

        assertEquals("One", map[1])
        assertEquals("Two", map[2])

        // test that we can insert and retrieve 1,000,000 values
        val insert = 1000_000
        var r = Random(45678)
        for (i in 1 until insert) {
            val v = r.nextInt(32324)
            map.put(v, "($v)")
        }
        r = Random(45678)
        var start = System.nanoTime()
        for (i in 1 until insert) {
            val v = r.nextInt(32324)
            assertEquals("($v)", map.get(v))
        }
        println(" " + ((System.nanoTime() - start) * 1E-6f) + "ms")
    }
}
