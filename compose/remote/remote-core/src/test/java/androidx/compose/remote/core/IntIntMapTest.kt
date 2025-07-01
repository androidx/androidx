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

import androidx.compose.remote.core.operations.utilities.IntIntMap
import kotlin.random.Random
import org.junit.Assert.assertEquals
import org.junit.Test

/** This a a hashmap that maps id to int */
class IntIntMapTest {
    @Test
    fun testIntMap1() {
        assertEquals(2, 1 + 1)
        var map = IntIntMap()
        val count = 10000
        println("===== random insert test $count ===")
        for (k in 1 until 12) {
            if (k > 6) {
                println("------")
                map = IntIntMap()
            }
            val innerCount = 10000
            var r = Random(45678)
            var sum = 0
            var start = System.nanoTime()
            for (i in 1 until innerCount) {
                val key = r.nextInt(Int.MAX_VALUE)
                val value = r.nextInt(Int.MAX_VALUE)
                sum += key
                sum += value
            }
            println(sum)
            val delta = System.nanoTime() - start
            println("$innerCount random  " + ((delta) * 1E-6f) + "ms")

            r = Random(45678)
            start = System.nanoTime()
            for (i in 1 until innerCount) {
                val key = r.nextInt(Int.MAX_VALUE)
                val value = r.nextInt(Int.MAX_VALUE)
                map.put(key, value)
            }
            val insert = System.nanoTime() - start
            println("$innerCount inserts " + ((insert) * 1E-6f) + "ms")

            r = Random(45678)
            start = System.nanoTime()
            for (i in 1 until innerCount) {
                val key = r.nextInt(Int.MAX_VALUE)
                val value = r.nextInt(Int.MAX_VALUE)
                assertEquals(value, map.get(key))
            }
            println("$innerCount gets in " + ((System.nanoTime() - start) * 1E-6f) + "ms\n")
        }
    }

    @Test
    fun testIntMap_sequential() {
        assertEquals(2, 1 + 1)
        var map = IntIntMap()
        val count = 1000_000
        println("===== sequential access test $count ===")
        for (k in 1 until 12) {
            if (k > 6) {
                println("------")
                map = IntIntMap()
            }

            var r = Random(45678)
            var sum = 0
            var start = System.nanoTime()
            for (i in 1 until count) {
                val key = i
                val value = r.nextInt(Int.MAX_VALUE)
                sum += key
                sum += value
            }
            println(sum)
            val delta = System.nanoTime() - start
            println("$count random  " + ((delta) * 1E-6f) + "ms")

            r = Random(45678)
            start = System.nanoTime()
            for (i in 1 until count) {
                val key = i
                val value = r.nextInt(Int.MAX_VALUE)
                map.put(key, value)
            }
            val insert = System.nanoTime() - start
            println("$count inserts " + ((insert) * 1E-6f) + "ms")

            r = Random(45678)
            start = System.nanoTime()
            for (i in 1 until count) {
                val key = i
                val value = r.nextInt(Int.MAX_VALUE)
                assertEquals(value, map.get(key))
            }
            println("$count gets in " + ((System.nanoTime() - start) * 1E-6f) + "ms\n")
        }
    }
}
