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
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.junit.runners.Parameterized
import org.junit.runners.Parameterized.Parameters

@RunWith(Parameterized::class)
class ArraySetBenchmarkTest(size: Int, sparse: Boolean) {
    private val sourceSet = createSourceSet(size, sparse)

    @get:Rule
    val benchmark = BenchmarkRule()

    @Test
    fun create() {
        benchmark.runCollectionBenchmark(ArraySetCreateBenchmark(sourceSet))
    }

    @Test
    fun containsElement() {
        benchmark.runCollectionBenchmark(ArraySetContainsElementBenchmark(sourceSet))
    }

    @Test
    fun indexOf() {
        benchmark.runCollectionBenchmark(ArraySetIndexOfBenchmark(sourceSet))
    }

    @Test
    fun addAllThenRemoveIndividually() {
        benchmark.runCollectionBenchmark(ArraySetAddAllThenRemoveIndividuallyBenchmark(sourceSet))
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
