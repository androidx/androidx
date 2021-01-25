/*
 * Copyright 2020 The Android Open Source Project
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

package androidx.benchmark.macro

import android.os.Build
import android.util.Log
import androidx.benchmark.perfetto.PerfettoCapture
import androidx.benchmark.perfetto.PerfettoHelper
import androidx.benchmark.perfetto.destinationPath
import androidx.benchmark.perfetto.reportAdditionalFileToCopy

/**
 * Wrapper for PerfettoCapture, which does nothing on API < Q
 */
class PerfettoCaptureWrapper {
    private var capture: PerfettoCapture? = null
    init {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            capture = PerfettoCapture()
        }
    }

    fun <T> captureTrace(
        benchmarkName: String,
        iteration: Int,
        block: (String) -> T
    ): T {
        try {
            start()
            return block(PerfettoHelper.getPerfettoTmpOutputFilePath())
        } finally {
            stop(benchmarkName, iteration)
        }
    }

    private fun start(): Boolean {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            Log.d(TAG, "Recording perfetto trace")
            capture?.start()
        }
        return true
    }

    private fun stop(benchmarkName: String, iteration: Int): Boolean {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            val iterString = iteration.toString().padStart(3, '0')
            // NOTE: macrobench still using legacy .trace name until
            // Studio supports .perfetto-trace extension (b/171251272)
            val traceName = "${benchmarkName}_iter$iterString.trace"
            val destination = destinationPath(traceName).absolutePath
            capture?.stop(destination)
            reportAdditionalFileToCopy("perfetto_trace_$iterString", destination)
        }
        return true
    }
}
