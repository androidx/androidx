/*
 * Copyright 2019 The Android Open Source Project
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

package androidx.benchmark

import android.opengl.Matrix

internal object ThrottleDetector {
    private var initNs = 0L

    private fun measureWorkNs(): Long {
        val state = BenchmarkState()
        state.performThrottleChecks = false
        val input = FloatArray(16) { it.toFloat() }
        val output = FloatArray(16)

        while (state.keepRunningInline()) {
            // Benchmark a simple thermal
            Matrix.translateM(output, 0, input, 0, 1F, 2F, 3F)
        }

        return state.stats.min
    }

    /**
     * Called to calculate throttling baseline, will be ignored after first call.
     */
    fun computeThrottleBaseline() {
        if (initNs == 0L) {
            initNs = measureWorkNs()
        }
    }

    /**
     * Makes a guess as to whether the device is currently thermal throttled based on performance
     * of single-threaded CPU work.
     */
    fun isDeviceThermalThrottled(): Boolean {
        if (initNs == 0L) {
            // not initialized, so assume not throttled.
            return false
        }

        val workNs = measureWorkNs()
        return workNs > initNs * 1.10
    }
}
