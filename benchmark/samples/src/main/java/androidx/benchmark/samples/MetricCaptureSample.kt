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

@file:Suppress("JUnitMalformedDeclaration", "unused")

package androidx.benchmark.samples

import androidx.annotation.Sampled
import androidx.benchmark.ExperimentalBenchmarkConfigApi
import androidx.benchmark.MetricCapture

private fun getCurrentFoo(): Long = 0

private fun getCurrentBar(): Long = 0

@OptIn(ExperimentalBenchmarkConfigApi::class)
@Sampled
fun metricCaptureMultiMetricSample() {
    /**
     * Sample shows how to collect multiple continuously running counters as metrics which measure
     * difference.
     *
     * This is similar to how [androidx.benchmark.TimeCapture] produces `timeNs`.
     */
    class MyMetricCapture : MetricCapture(listOf("foo", "bar")) {
        private var currentStartedFoo = 0L
        private var currentPausedStartedFoo = 0L
        private var currentTotalPausedFoo = 0L

        private var currentStartedBar = 0L
        private var currentPausedStartedBar = 0L
        private var currentTotalPausedBar = 0L

        override fun captureStart(timeNs: Long) {
            // reset paused state, capture current
            currentTotalPausedBar = 0
            currentTotalPausedFoo = 0

            currentStartedFoo = getCurrentFoo()
            currentStartedBar = getCurrentBar()
        }

        override fun captureStop(timeNs: Long, output: LongArray, offset: Int) {
            output[offset + 0] = getCurrentFoo() - currentStartedFoo - currentTotalPausedFoo
            output[offset + 1] = getCurrentBar() - currentStartedBar - currentTotalPausedBar
        }

        override fun capturePaused() {
            currentPausedStartedFoo = getCurrentFoo()
            currentPausedStartedBar = getCurrentBar()
        }

        override fun captureResumed() {
            currentTotalPausedFoo += getCurrentFoo() - currentPausedStartedFoo
            currentTotalPausedBar += getCurrentBar() - currentPausedStartedBar
        }
    }
}
