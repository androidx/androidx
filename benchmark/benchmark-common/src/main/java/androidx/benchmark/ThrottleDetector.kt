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

    /**
     * Copies 400K, 10 times.
     */
    private fun copySomeData() {
        val a = ByteArray(400000)
        val b = ByteArray(400000)
        for (i in 0..9) {
            System.arraycopy(a, 0, b, 0, a.size)
        }
    }

    private fun measureWorkNs(): Long {
        // Access a non-trivial amount of data to try and 'reset' any cache state.
        // Have observed this to give more consistent performance when clocks are unlocked.
        copySomeData()

        val state = BenchmarkState()
        state.simplifiedTimingOnlyMode = true
        val sourceMatrix = FloatArray(16) { System.nanoTime().toFloat() }
        val resultMatrix = FloatArray(16)

        while (state.keepRunningInline()) {
            // Benchmark a simple thermal
            Matrix.translateM(resultMatrix, 0, sourceMatrix, 0, 1F, 2F, 3F)
        }

        return state.getMinTimeNanos()
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
     * Called to reset throttle baseline, if throttle detection is firing too regularly, and
     * inaccurate initial measurement is suspected.
     */
    fun resetThrottleBaseline() {
        initNs = 0L
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
