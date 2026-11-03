/*
 * Copyright 2026 The Android Open Source Project
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

import kotlinx.benchmark.Benchmark
import kotlinx.benchmark.Param
import kotlinx.benchmark.Scope
import kotlinx.benchmark.Setup
import kotlinx.benchmark.State

/** Read operations through [IntObjectMap], including present keys mapped to null. */
@State(Scope.Benchmark)
open class IntObjectMapBenchmark {
    // Include small maps, full small tables (6, 14, 112), and larger maps.
    @Param("0", "1", "6", "14", "112", "256", "1024") var size: Int = 0

    private lateinit var map: IntObjectMap<IntObjectMapBenchmarkValue?>
    private lateinit var queryKeys: IntArray
    private lateinit var missingKeys: IntArray

    @Setup
    fun setup() {
        val keys = IntArray(size, ::intObjectMapBenchmarkKey)
        missingKeys = IntArray(QUERY_COUNT) { intObjectMapBenchmarkKey(size + it) }
        // Select keys outside measurement so small-map timings do not include integer division.
        queryKeys =
            IntArray(QUERY_COUNT) {
                if (size == 0) missingKeys[it] else keys[it % size]
            }
        val mutableMap = MutableIntObjectMap<IntObjectMapBenchmarkValue?>(size)
        for (index in keys.indices) {
            mutableMap[keys[index]] = intObjectMapBenchmarkValue(index)
        }
        map = mutableMap
    }

    @Benchmark
    fun getPresent(): Int {
        var result = 0
        // Keep the number of lookups fixed so map sizes can be compared, including empty maps.
        for (key in queryKeys) {
            result += map[key]?.value ?: 0
        }
        return result
    }

    @Benchmark
    fun getAbsent(): Int {
        var result = 0
        for (key in missingKeys) {
            result += map[key]?.value ?: 0
        }
        return result
    }

    @Benchmark
    fun containsPresent(): Int {
        var result = 0
        for (key in queryKeys) {
            if (map.containsKey(key)) result++
        }
        return result
    }

    @Benchmark
    fun containsAbsent(): Int {
        var result = 0
        for (key in missingKeys) {
            if (map.containsKey(key)) result++
        }
        return result
    }

    @Benchmark
    fun forEach(): Int {
        var result = 0
        map.forEach { key, value -> result += key + (value?.value ?: 0) }
        return result
    }

    @Benchmark
    fun getOrDefault(): Int {
        var result = 0
        for (key in missingKeys) {
            result += map.getOrDefault(key, DEFAULT_VALUE)?.value ?: 0
        }
        return result
    }

    private companion object {
        const val QUERY_COUNT = 256
        val DEFAULT_VALUE = IntObjectMapBenchmarkValue(-1)
    }
}

// Multiplication by an odd number gives distinct, mixed positive and negative Int keys.
internal fun intObjectMapBenchmarkKey(index: Int): Int = index * -1640531527

internal class IntObjectMapBenchmarkValue(val value: Int)

internal fun intObjectMapBenchmarkValue(index: Int): IntObjectMapBenchmarkValue? =
    if (index % 3 == 0) null else IntObjectMapBenchmarkValue(index + 1)
