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

package androidx.benchmark.darwin.samples

import androidx.benchmark.darwin.TestCase
import androidx.benchmark.darwin.TestCaseContext
import platform.XCTest.XCTMeasureOptions

class ArrayListAllocationBenchmark : TestCase() {
    override fun setUp() {
        // does nothing
    }

    override fun benchmark(context: TestCaseContext) {
        val options = XCTMeasureOptions.defaultOptions()
        // 5 Iterations
        options.iterationCount = 5.toULong()
        context.measureWithMetrics(
            listOf(
                platform.XCTest.XCTCPUMetric(),
                platform.XCTest.XCTMemoryMetric(),
                platform.XCTest.XCTClockMetric()
            ),
            options
        ) {
            // Do something a bit expensive
            repeat(1000) { ArrayList<Float>(SIZE) }
        }
    }

    override fun testDescription(): String {
        return "Allocate an ArrayList of size $SIZE"
    }

    companion object {
        // The initial capacity of the allocation
        private const val SIZE = 1000
    }
}
