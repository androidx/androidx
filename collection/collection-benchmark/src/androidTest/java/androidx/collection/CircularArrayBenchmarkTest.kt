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
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.junit.runners.Parameterized
import org.junit.runners.Parameterized.Parameters

@RunWith(Parameterized::class)
class CircularArrayBenchmarkTest(size: Int) {

    val seed = mutableListOf<String>().apply {
        for (i in 0 until size) {
            add("element $i")
        }
    }

    @get:Rule
    val benchmark = BenchmarkRule()

    @Test
    fun addFromHeadAndPopFromTail() {
        benchmark.measureRepeated {
            val array = CircularArray<String>()
            for (e in seed) {
                array.addFirst(e)
            }
            runWithTimingDisabled {
                assertEquals(seed.size, array.size())
            }
            repeat(seed.size) {
                array.popLast()
            }
            runWithTimingDisabled {
                assertTrue(array.isEmpty())
            }
        }
    }

    companion object {
        @JvmStatic
        @Parameters(name = "size={0}")
        fun parameters() = buildParameters(
            listOf(10, 100, 1_000, 10_000),
        )
    }
}
