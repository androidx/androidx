/*
 * Copyright 2021 The Android Open Source Project
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

import androidx.benchmark.junit4.BenchmarkRule
import androidx.benchmark.junit4.measureRepeated
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Assert.fail
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.junit.runners.Parameterized
import org.junit.runners.Parameterized.Parameters
import kotlin.random.Random

@RunWith(Parameterized::class)
class SimpleArrayMapBenchmarkTest(size: Int, sparse: Boolean) {

    private val sourceMap = mutableMapOf<Int, String>().apply {
        val keyFactory: () -> Int = if (sparse) {
            // Despite the fixed seed, the algorithm which produces random values may vary across
            // OS versions. Since we're not doing cross-device comparison this is acceptable.
            val random = Random(0);
            {
                val value: Int
                while (true) {
                    val candidate = random.nextInt()
                    if (candidate !in this) {
                        value = candidate
                        break
                    }
                }
                value
            }
        } else {
            var value = 0
            { value++ }
        }
        repeat(size) {
            val key = keyFactory()
            this.put(key, "value of $key")
        }
        check(size == this.size)
    }

    @get:Rule
    val benchmark = BenchmarkRule()

    @Test
    fun create() {
        benchmark.measureRepeated {
            val map = SimpleArrayMap<Int, String>()
            for ((key, value) in sourceMap) {
                map.put(key, value)
            }
            runWithTimingDisabled {
                assertEquals(sourceMap.size, map.size())
            }
        }
    }

    @Test
    fun containsKey() {
        // Split the source map into two lists, one with elements in the created map, one not.
        val src = sourceMap.toList()
        val inList = src.slice(0 until src.size / 2)
        val inListKeys = inList.map { it.first }
        val outListKeys = src.slice(src.size / 2 until src.size).map { it.first }

        val map = SimpleArrayMap<Int, String>()
        for ((key, value) in inList) {
            map.put(key, value)
        }
        benchmark.measureRepeated {
            for (key in inListKeys) {
                if (!map.containsKey(key)) {
                    fail()
                }
            }

            for (key in outListKeys) {
                if (map.containsKey(key)) {
                    fail()
                }
            }
        }
    }

    @Test
    fun addAllThenRemoveIndividually() {
        val sourceSimpleArrayMap = SimpleArrayMap<Int, String>(sourceMap.size)
        for ((key, value) in sourceMap) {
            sourceSimpleArrayMap.put(key, value)
        }

        var map = SimpleArrayMap<Int, String>(sourceSimpleArrayMap.size())
        benchmark.measureRepeated {
            map.putAll(sourceSimpleArrayMap)
            runWithTimingDisabled {
                assertEquals(sourceSimpleArrayMap.size(), map.size())
            }

            for (key in sourceMap.keys) {
                map.remove(key)
            }
            runWithTimingDisabled {
                assertTrue(map.isEmpty())
            }
        }
    }

    companion object {
        @JvmStatic
        @Parameters(name = "size={0},sparse={1}")
        fun parameters() = buildParameters(
            // Slow tests, so only run the suite up to 1000 elements.
            listOf(10, 100, 1_000),
            listOf(true, false)
        )
    }
}
