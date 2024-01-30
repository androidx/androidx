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
import kotlin.random.Random
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.junit.runners.Parameterized

@RunWith(Parameterized::class)
class ScatterSetBenchmarkTest(size: Int) {
    private val sourceSet = createDataSet(size)

    @get:Rule
    val benchmark = BenchmarkRule()

    @Test
    fun insert() {
        benchmark.runCollectionBenchmark(
            object : CollectionBenchmark {
                override fun measuredBlock() {
                    val set = MutableScatterSet<String>(sourceSet.size)
                    for (testValue in sourceSet) {
                        set += testValue
                    }
                }
            }
        )
    }

    @Test
    fun remove() {
        benchmark.runCollectionBenchmark(
            object : CollectionBenchmark {
                private val set = MutableScatterSet<String>()

                init {
                    for (testValue in sourceSet) {
                        set += testValue
                    }
                }

                override fun measuredBlock() {
                    for (testValue in sourceSet) {
                        set.remove(testValue)
                    }
                }
            }
        )
    }

    @Test
    fun forEach() {
        benchmark.runCollectionBenchmark(
            object : CollectionBenchmark {
                private val set = MutableScatterSet<String>()

                init {
                    for (testValue in sourceSet) {
                        set += testValue
                    }
                }

                override fun measuredBlock() {
                    set.forEach { element ->
                        @Suppress("UnusedEquals", "RedundantSuppression")
                        element != ""
                    }
                }
            }
        )
    }

    companion object {
        @JvmStatic
        @Parameterized.Parameters(name = "size={0}")
        fun parameters() = buildParameters(
            listOf(/*10, 100, */ 1_000 /*, 16_000*/)
        )

        internal fun createDataSet(
            size: Int
        ): Array<String> = Array(size) { index ->
            (index * Random.Default.nextFloat()).toString()
        }
    }
}
