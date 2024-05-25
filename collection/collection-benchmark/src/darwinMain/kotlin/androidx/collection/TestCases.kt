/*
 * Copyright 2022 The Android Open Source Project
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

import androidx.benchmark.darwin.TestCase
import androidx.benchmark.darwin.TestCaseContext
import platform.XCTest.XCTCPUMetric
import platform.XCTest.XCTClockMetric
import platform.XCTest.XCTMeasureOptions
import platform.XCTest.XCTMemoryMetric

object TestCases {
    fun benchmarkTests(): List<TestCase> {
        return listOf(10, 100, 1_000)
            .flatMap { size -> listOf(size to true, size to false) }
            .map { params -> createSourceSet(size = params.first, sparse = params.second) }
            .flatMap { sourceSet ->
                listOf(
                    CollectionTestCase(
                        benchmark = ArraySetCreateBenchmark(sourceSet),
                        testDescription = "ArraySet_ContainsElement",
                    ),
                    CollectionTestCase(
                        benchmark = ArraySetContainsElementBenchmark(sourceSet),
                        testDescription = "ArraySet_ContainsElement",
                    ),
                    CollectionTestCase(
                        benchmark = ArraySetIndexOfBenchmark(sourceSet),
                        testDescription = "ArraySet_IndexOf",
                    ),
                    CollectionTestCase(
                        benchmark = ArraySetAddAllThenRemoveIndividuallyBenchmark(sourceSet),
                        testDescription = "ArraySet_AddAllThenRemoveIndividually",
                    ),
                )
            } +
            listOf(10, 100, 1_000, 10_000)
                .map { size -> createSeed(size) }
                .map { seed ->
                    CollectionTestCase(
                        benchmark = CircularyArrayAddFromHeadAndPopFromTailBenchmark(seed),
                        testDescription = "CircularyArray_AddFromHeadAndPopFromTail",
                    )
                } +
            listOf(10, 100, 1_000)
                .map { size -> createKeyList(size) }
                .flatMap { keyList ->
                    listOf(
                        CollectionTestCase(
                            benchmark =
                                LruCacheCreateThenFetchWithAllHitsBenchmark(keyList, keyList.size),
                            testDescription = "LruCache_CreateThenFetchWithAllHits",
                        ),
                        CollectionTestCase(
                            benchmark = LruCacheAllMissesBenchmark(keyList, keyList.size),
                            testDescription = "LruCache_AllMisses",
                        ),
                    )
                } +
            listOf(10, 100, 1_000)
                .flatMap { size -> listOf(size to true, size to false) }
                .map { params -> createSourceMap(size = params.first, sparse = params.second) }
                .flatMap { sourceMap ->
                    listOf(
                        CollectionTestCase(
                            benchmark = SimpleArrayMapCreateBenchmark(sourceMap),
                            testDescription = "SimpleArrayMap_Create",
                        ),
                        CollectionTestCase(
                            benchmark = SimpleArrayMapContainsKeyBenchmark(sourceMap),
                            testDescription = "SimpleArrayMap_ContainsKey",
                        ),
                        CollectionTestCase(
                            benchmark =
                                SimpleArrayMapAddAllThenRemoveIndividuallyBenchmark(sourceMap),
                            testDescription = "SimpleArrayMap_AddAllThenRemoveIndividually",
                        ),
                    )
                } +
            listOf(10, 100, 1_000, 10_000)
                .flatMap { size -> listOf(size to true, size to false) }
                .map { params ->
                    createFilledSparseArray(size = params.first, sparse = params.second)
                }
                .flatMap { map ->
                    listOf(
                        CollectionTestCase(
                            benchmark = SparseArrayGetBenchmark(map),
                            testDescription = "SimpleArrayMap_Get",
                        ),
                        CollectionTestCase(
                            benchmark = SparseArrayContainsKeyBenchmark(map),
                            testDescription = "SparseArray_ContainsKey",
                        ),
                        CollectionTestCase(
                            benchmark = SparseArrayIndexOfKeyBenchmark(map),
                            testDescription = "SparseArray_IndexOfKey",
                        ),
                        CollectionTestCase(
                            benchmark = SparseArrayIndexOfValueBenchmark(map),
                            testDescription = "SparseArray_IndexOfValue",
                        ),
                    )
                }
    }
}

private class CollectionTestCase(
    private val benchmark: CollectionBenchmark,
    private val testDescription: String,
) : TestCase() {
    override fun benchmark(context: TestCaseContext) {
        val options = XCTMeasureOptions.defaultOptions()
        // A single iteration
        options.iterationCount = 5.toULong()
        context.measureWithMetrics(
            listOf(XCTCPUMetric(), XCTMemoryMetric(), XCTClockMetric()),
            options
        ) {
            benchmark.measuredBlock()
        }
    }

    override fun testDescription(): String {
        return testDescription
    }
}
