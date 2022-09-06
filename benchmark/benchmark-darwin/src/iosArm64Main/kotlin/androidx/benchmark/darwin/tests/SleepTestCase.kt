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

package androidx.benchmark.darwin.tests

import androidx.benchmark.darwin.TestCase
import androidx.benchmark.darwin.TestCaseContext
import androidx.benchmark.darwin.TestCases
import platform.Foundation.NSLog
import platform.XCTest.XCTMeasureOptions
import platform.posix.sleep

class SleepTestCase : TestCase() {
    override fun setUp() {
        NSLog("%s", "Hello Benchmarks !")
    }

    override fun benchmark(context: TestCaseContext) {
        val options = XCTMeasureOptions.defaultOptions()
        // A single iteration
        options.iterationCount = 1.toULong()
        context.measureWithMetrics(
            listOf(
                platform.XCTest.XCTCPUMetric(),
                platform.XCTest.XCTMemoryMetric(),
                platform.XCTest.XCTClockMetric()
            ),
            options
        ) {
            repeat(3) {
                NSLog("%s", "Sleeping for 1 second")
                sleep(1)
            }
        }
    }

    override fun testDescription(): String {
        return "A test that sleeps for 3 seconds"
    }

    companion object {
        fun addBenchmarkTest() {
            TestCases.addBenchmarkTest(SleepTestCase())
        }
    }
}
