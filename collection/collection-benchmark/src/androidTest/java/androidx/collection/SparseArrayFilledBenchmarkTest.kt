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

import androidx.benchmark.junit4.BenchmarkRule
import androidx.benchmark.junit4.measureRepeated
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.junit.runners.Parameterized
import org.junit.runners.Parameterized.Parameters
import kotlin.random.Random

@RunWith(Parameterized::class)
class SparseArrayFilledBenchmarkTest(size: Int, sparse: Boolean) {
    private val map = SparseArrayCompat<String>().apply {
        val keyFactory: () -> Int = if (sparse) {
            // Despite the fixed seed, the algorithm which produces random values may vary across
            // OS versions. Since we're not doing cross-device comparison this is acceptable.
            val random = Random(0);
            {
                val key: Int
                while (true) {
                    val candidate = random.nextInt()
                    if (candidate !in this) {
                        key = candidate
                        break
                    }
                }
                key
            }
        } else {
            var key = 0
            { key++ }
        }
        repeat(size) {
            val key = keyFactory()
            put(key, "value$key")
        }
        check(size == size())
    }

    @get:Rule
    val benchmark = BenchmarkRule()

    @Test fun get() {
        val lastKey = map.keyAt(map.size() - 1)
        benchmark.measureRepeated {
            map.get(lastKey)
        }
    }

    @Test fun containsKey() {
        val lastKey = map.keyAt(map.size() - 1)
        benchmark.measureRepeated {
            map.containsKey(lastKey)
        }
    }

    @Test fun indexOfKey() {
        val lastKey = map.keyAt(map.size() - 1)
        benchmark.measureRepeated {
            map.indexOfKey(lastKey)
        }
    }

    @Test fun indexOfValue() {
        val lastValue = map.valueAt(map.size() - 1)
        benchmark.measureRepeated {
            map.indexOfValue(lastValue)
        }
    }

    companion object {
        @JvmStatic
        @Parameters(name = "size={0},sparse={1}")
        fun parameters() = buildParameters(
            listOf(10, 100, 1_000, 10_000),
            listOf(true, false)
        )
    }
}
