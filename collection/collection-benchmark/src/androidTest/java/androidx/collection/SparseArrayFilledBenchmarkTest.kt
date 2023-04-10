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
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.junit.runners.Parameterized
import org.junit.runners.Parameterized.Parameters

@RunWith(Parameterized::class)
class SparseArrayFilledBenchmarkTest(size: Int, sparse: Boolean) {
    private val map = createFilledSparseArray(size, sparse)

    @get:Rule
    val benchmark = BenchmarkRule()

    @Test
    fun get() {
        benchmark.runCollectionBenchmark(SparseArrayGetBenchmark(map))
    }

    @Test
    fun containsKey() {
        benchmark.runCollectionBenchmark(SparseArrayContainsKeyBenchmark(map))
    }

    @Test
    fun indexOfKey() {
        benchmark.runCollectionBenchmark(SparseArrayIndexOfKeyBenchmark(map))
    }

    @Test
    fun indexOfValue() {
        benchmark.runCollectionBenchmark(SparseArrayIndexOfValueBenchmark(map))
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
