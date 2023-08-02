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

import android.content.Context
import android.opengl.Matrix
import android.os.Build
import android.os.PowerManager
import androidx.annotation.RequiresApi
import androidx.test.platform.app.InstrumentationRegistry

internal object ThrottleDetector {
    private var initNs = 0.0

    /**
     * Copies 400K, 10 times.
     */
    private fun copySomeData() {
        val a = ByteArray(400_000)
        val b = ByteArray(400_000)
        repeat(10) {
            System.arraycopy(a, 0, b, 0, a.size)
        }
    }

    internal fun measureWorkNs(): Double {
        // Access a non-trivial amount of data to try and 'reset' any cache state.
        // Have observed this to give more consistent performance when clocks are unlocked.
        copySomeData()

        val state = BenchmarkState(simplifiedTimingOnlyMode = true)
        val sourceMatrix = FloatArray(16) { System.nanoTime().toFloat() }
        val resultMatrix = FloatArray(16)

        while (state.keepRunningInline()) {
            // Benchmark a simple thermal
            Matrix.translateM(resultMatrix, 0, sourceMatrix, 0, 1F, 2F, 3F)
        }

        return state.getMinTimeNanos()
    }

    /**
     * Called to calculate throttling baseline, will be ignored after first call until reset.
     *
     * Does nothing if baseline isn't needed
     */
    fun computeThrottleBaselineIfNeeded() {
        if (Build.VERSION.SDK_INT < 29 && // can't use PowerManager API yet
            initNs == 0.0 && // first time
            !CpuInfo.locked && // CPU locked (presumably to stable values), should be no throttling
            !IsolationActivity.sustainedPerformanceModeInUse && // trust sustained perf
            !Errors.isEmulator // don't bother with emulators, will always be unpredicatable
        ) {
            initNs = measureWorkNs()
        }
    }

    /**
     * Called to reset throttle baseline, if throttle detection is firing too regularly, and
     * inaccurate initial measurement is suspected.
     */
    fun resetThrottleBaseline() {
        initNs = 0.0
    }

    /**
     * Makes a guess as to whether the device is currently thermal throttled based on performance
     * of single-threaded CPU work.
     */
    fun isDeviceThermalThrottled(): Boolean {
        if (Build.VERSION.SDK_INT >= 29) {
            return Api29Helper.isDeviceThermalThrottled()
        }

        if (initNs == 0.0) {
            // baseline not set, so assume not throttled.
            return false
        }

        val workNs = measureWorkNs()
        return workNs > initNs * 1.10
    }

    @RequiresApi(29)
    internal object Api29Helper {
        /**
         * We don't accept any thermal throttling status over NONE, since we have observed
         * significant CPU variation even with `THERMAL_STATUS_LIGHT` (see ThrottleDetectorTest).
         *
         * On a Pixel 2 running API 30, we've seen variation from 55 - 72ms in the measureWorkNs()
         * matrix workload.
         */
        fun isDeviceThermalThrottled(): Boolean {
            return (InstrumentationRegistry
                .getInstrumentation()
                .context
                .getSystemService(Context.POWER_SERVICE) as PowerManager)
                .currentThermalStatus > PowerManager.THERMAL_STATUS_NONE
        }
    }
}
