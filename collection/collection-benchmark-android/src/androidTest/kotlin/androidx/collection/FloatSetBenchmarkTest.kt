/*
 * Copyright 2024 The Android Open Source Project
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

@RunWith(Parameterized::class)
class FloatSetBenchmarkTest(size: Int) {
    private val sourceSet = createFloatSetDataSet(size)

    @get:Rule val benchmark = BenchmarkRule()

    @Test
    fun insert() {
        benchmark.runCollectionBenchmark(FloatSetInsertBenchmark(sourceSet))
    }

    @Test
    fun remove() {
        benchmark.runCollectionBenchmark(FloatSetRemoveBenchmark(sourceSet))
    }

    @Test
    fun contains() {
        benchmark.runCollectionBenchmark(FloatSetContainsBenchmark(sourceSet))
    }

    companion object {
        @JvmStatic
        @Parameterized.Parameters(name = "size={0}")
        fun parameters() = buildParameters(listOf(1_000))
    }
}
