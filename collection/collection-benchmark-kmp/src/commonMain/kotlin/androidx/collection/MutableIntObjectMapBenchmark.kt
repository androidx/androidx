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

/** Mutations of [MutableIntObjectMap] with reusable object values allocated outside measurement. */
@State(Scope.Benchmark)
open class MutableIntObjectMapBenchmark {
    @Param("0", "1", "6", "14", "112", "256", "1024") var size: Int = 0

    private lateinit var map: MutableIntObjectMap<IntObjectMapBenchmarkValue?>
    private lateinit var keys: IntArray
    private lateinit var values: Array<IntObjectMapBenchmarkValue?>
    private lateinit var extraValue: IntObjectMapBenchmarkValue
    private var extraKey: Int = 0

    @Setup
    fun setup() {
        keys = IntArray(size, ::intObjectMapBenchmarkKey)
        values = Array(size, ::intObjectMapBenchmarkValue)
        extraKey = intObjectMapBenchmarkKey(size)
        extraValue = IntObjectMapBenchmarkValue(-1)
        // Leave room for insertAndRemove/getOrPutAbsent so repeated calls do not grow the table.
        map = MutableIntObjectMap(size + 1)
        fill(map)
    }

    @Benchmark
    fun createAndFill(): MutableIntObjectMap<*> {
        val newMap = MutableIntObjectMap<IntObjectMapBenchmarkValue?>(size)
        fill(newMap)
        return newMap
    }

    @Benchmark
    fun growAndFill(): MutableIntObjectMap<*> {
        val newMap = MutableIntObjectMap<IntObjectMapBenchmarkValue?>(0)
        fill(newMap)
        return newMap
    }

    @Benchmark
    fun replaceExisting(): Int {
        for (index in keys.indices) {
            map[keys[index]] = values[index]
        }
        return map.size
    }

    @Benchmark
    fun insertAndRemove(): Int {
        map[extraKey] = extraValue
        return map.remove(extraKey)?.value ?: 0
    }

    @Benchmark
    fun getOrPutAbsent(): Int {
        val result = map.getOrPut(extraKey) { extraValue }
        map.remove(extraKey)
        return result?.value ?: 0
    }

    @Benchmark
    fun removeAndReinsert(): Int {
        var result = 0
        for (index in keys.indices) {
            val key = keys[index]
            result += map.remove(key)?.value ?: 0
            map[key] = values[index]
        }
        return result
    }

    @Benchmark
    fun clearAndRefill(): Int {
        map.clear()
        fill(map)
        return map.size
    }

    private fun fill(destination: MutableIntObjectMap<IntObjectMapBenchmarkValue?>) {
        for (index in keys.indices) {
            destination[keys[index]] = values[index]
        }
    }
}
