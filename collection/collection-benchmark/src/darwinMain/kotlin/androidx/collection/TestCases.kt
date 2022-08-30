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

val all: List<TestCase> = listOf(10, 100, 1000)
    .flatMap { size ->
        listOf(size to true, size to false)
    }
    .map { params ->
        createSourceSet(
            size = params.first,
            sparse = params.second
        )
    }
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
                testDescription = "ArraySet_IndexOfBenchmark",
            ),
            CollectionTestCase(
                benchmark = ArraySetAddAllThenRemoveIndividuallyBenchmark(sourceSet),
                testDescription = "ArraySet_AddAllThenRemoveIndividuallyBenchmark",
            ),
        )
    }

private class CollectionTestCase(
    private val benchmark: CollectionBenchmark,
    private val testDescription: String,
) : TestCase() {
    override fun benchmark(context: TestCaseContext) {
        val options = XCTMeasureOptions.defaultOptions()
        // A single iteration
        options.iterationCount = 1.toULong()
        context.measureWithMetrics(
            listOf(
                XCTCPUMetric(),
                XCTMemoryMetric(),
                XCTClockMetric()
            ),
            options
        ) {
            benchmark.measuredBlock()
        }
    }

    override fun testDescription(): String {
        return testDescription
    }
}
