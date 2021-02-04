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
class ArraySetBenchmarkTest(size: Int, sparse: Boolean) {
    private val sourceSet = mutableSetOf<Int>().apply {
        val valueFactory: () -> Int = if (sparse) {
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
            this.add(valueFactory())
        }
        check(size == this.size)
    }

    @get:Rule
    val benchmark = BenchmarkRule()

    @Test
    fun create() {
        benchmark.measureRepeated {
            val set = ArraySet(sourceSet)
            runWithTimingDisabled {
                assertEquals(sourceSet.size, set.size)
            }
        }
    }

    @Test
    fun containsElement() {
        // Split the set into two lists, one with elements in the created set, one not.
        val src = sourceSet.toList()
        val inList = src.slice(0 until src.size / 2)
        val outList = src.slice(src.size / 2 until src.size)

        val set = ArraySet(inList)
        benchmark.measureRepeated {
            for (e in inList) {
                if (e !in set) {
                    fail()
                }
            }

            for (e in outList) {
                if (e in set) {
                    fail()
                }
            }
        }
    }

    @Test
    fun indexOf() {
        // Split the set into two lists, one with elements in the created set, one not.
        val src = sourceSet.toList()
        val inList = src.slice(0 until src.size / 2)
        val outList = src.slice(src.size / 2 until src.size)

        val set = ArraySet(inList)
        benchmark.measureRepeated {
            for (e in inList) {
                if (set.indexOf(e) < 0) {
                    fail()
                }
            }

            for (e in outList) {
                if (set.indexOf(e) >= 0) {
                    fail()
                }
            }
        }
    }

    @Test
    fun addAllThenRemoveIndividually() {
        val set = ArraySet<Int>(sourceSet.size)
        benchmark.measureRepeated {
            set.addAll(sourceSet)
            runWithTimingDisabled {
                assertEquals(sourceSet.size, set.size)
            }

            for (e in sourceSet) {
                set.remove(e)
            }
            runWithTimingDisabled {
                assertTrue(set.isEmpty())
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
