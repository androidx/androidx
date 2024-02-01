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

package androidx.collection

import androidx.benchmark.junit4.BenchmarkRule
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.junit.runners.Parameterized
import org.junit.runners.Parameterized.Parameters

@RunWith(Parameterized::class)
class ScatterMapBenchmarkTest(private val size: Int) {
    private val sourceSet = createDataSet(size)
    private val badHashSourceSet = createBadHashDataSet(size)

    @get:Rule
    val benchmark = BenchmarkRule()

    @Test
    fun insert() {
        benchmark.runCollectionBenchmark(ScatterMapInsertBenchmark(sourceSet))
    }

    @Test
    fun insert_bad_hash() {
        benchmark.runCollectionBenchmark(ScatterMapInsertBenchmarkBadHash(badHashSourceSet))
    }

    @Test
    fun remove() {
        benchmark.runCollectionBenchmark(ScatterMapRemoveBenchmark(sourceSet))
    }

    @Test
    fun read() {
        benchmark.runCollectionBenchmark(ScatterHashMapReadBenchmark(sourceSet))
    }

    @Test
    fun read_bad_hash() {
        benchmark.runCollectionBenchmark(ScatterHashMapReadBadHashBenchmark(badHashSourceSet))
    }

    @Test
    fun forEach() {
        benchmark.runCollectionBenchmark(ScatterMapForEachBenchmark(sourceSet))
    }

    @Test
    fun compute() {
        benchmark.runCollectionBenchmark(ScatterMapComputeBenchmark(sourceSet))
    }

    companion object {
        @JvmStatic
        @Parameters(name = "size={0}")
        fun parameters() = buildParameters(
            listOf(10, 100, 1_000, 16_000)
        )
    }
}
