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

package androidx.benchmark.macro.perfetto

import android.os.Build
import android.util.Log
import androidx.annotation.RequiresApi
import androidx.benchmark.Outputs
import androidx.benchmark.Outputs.dateToFileName
import androidx.benchmark.macro.device
import androidx.test.platform.app.InstrumentationRegistry

/**
 * Wrapper for [PerfettoCapture] which does nothing below L.
 */
internal class PerfettoCaptureWrapper {
    private var capture: PerfettoCapture? = null
    private val TRACE_ENABLE_PROP = "persist.traced.enable"

    init {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
            capture = PerfettoCapture()
        }
    }

    @RequiresApi(Build.VERSION_CODES.LOLLIPOP)
    private fun start(): Boolean {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
            Log.d(PerfettoHelper.LOG_TAG, "Recording perfetto trace")
            capture?.start()
        }
        return true
    }

    @RequiresApi(Build.VERSION_CODES.LOLLIPOP)
    private fun stop(benchmarkName: String, iteration: Int): String {
        val iterString = iteration.toString().padStart(3, '0')
        // NOTE: Macrobenchmarks still use legacy .trace name until
        // Studio supports .perfetto-trace extension (b/171251272)
        val traceName = "${benchmarkName}_iter${iterString}_${dateToFileName()}.trace"
        return Outputs.writeFile(fileName = traceName, reportKey = "perfetto_trace_$iterString") {
            capture!!.stop(it.absolutePath)
        }
    }

    fun record(
        benchmarkName: String,
        iteration: Int,
        block: () -> Unit
    ): String? {
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.Q) {
            return null // tracing currently not supported on this version
        }

        val device = InstrumentationRegistry.getInstrumentation().device()

        var resetTracedEnabledString: String? = null
        try {
            // Prior to Android 11 (R), a shell property must be set to enable perfetto tracing, see
            // https://perfetto.dev/docs/quickstart/android-tracing#starting-the-tracing-services
            if (Build.VERSION.SDK_INT <= Build.VERSION_CODES.Q) {
                val currentPropVal = device.executeShellCommand("getprop $TRACE_ENABLE_PROP")
                if (currentPropVal != "1") {
                    Log.d(
                        PerfettoHelper.LOG_TAG,
                        "set $TRACE_ENABLE_PROP to 1 (was $currentPropVal"
                    )
                    device.executeShellCommand("setprop $TRACE_ENABLE_PROP 1")
                    resetTracedEnabledString = currentPropVal
                }
            }

            start()
            block()
            return stop(benchmarkName, iteration)
        } finally {
            if (resetTracedEnabledString != null) {
                Log.d(
                    PerfettoHelper.LOG_TAG,
                    "resetting $TRACE_ENABLE_PROP to $resetTracedEnabledString"
                )
                device.executeShellCommand(
                    "setprop persist.traced.enable $resetTracedEnabledString"
                )
            }
        }
    }
}
