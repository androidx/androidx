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

package androidx.benchmark.perfetto

import android.os.Build
import android.util.Log
import androidx.annotation.RequiresApi
import androidx.annotation.RestrictTo
import androidx.benchmark.Outputs
import androidx.benchmark.Outputs.dateToFileName
import androidx.benchmark.PropOverride
import androidx.benchmark.Shell
import androidx.benchmark.perfetto.PerfettoHelper.Companion.LOG_TAG
import androidx.benchmark.perfetto.PerfettoHelper.Companion.isAbiSupported

/**
 * Wrapper for [PerfettoCapture] which does nothing below API 23.
 */
@RestrictTo(RestrictTo.Scope.LIBRARY_GROUP)
class PerfettoCaptureWrapper {
    private var capture: PerfettoCapture? = null
    private val TRACE_ENABLE_PROP = "persist.traced.enable"

    init {
        if (Build.VERSION.SDK_INT >= 23) {
            capture = PerfettoCapture()
        }
    }

    companion object {
        val inUseLock = Object()

        /**
         * Prevents re-entrance of perfetto trace capture, as it doesn't handle this correctly
         *
         * (Single file output location, process cleanup, etc.)
         */
        var inUse = false
    }

    @RequiresApi(23)
    private fun start(
        config: PerfettoConfig,
        userspaceTracingPackage: String?
    ): Boolean {
        capture?.apply {
            Log.d(LOG_TAG, "Recording perfetto trace")
            if (userspaceTracingPackage != null &&
                Build.VERSION.SDK_INT >= 30
            ) {
                val result = enableAndroidxTracingPerfetto(
                    targetPackage = userspaceTracingPackage,
                    provideBinariesIfMissing = true
                ) ?: "Success"
                Log.d(LOG_TAG, "Enable full tracing result=$result")
            }
            start(config)
        }

        return true
    }

    @RequiresApi(23)
    private fun stop(traceLabel: String): String {
        return Outputs.writeFile(
            fileName = "${traceLabel}_${dateToFileName()}.perfetto-trace",
            reportKey = "perfetto_trace_$traceLabel"
        ) {
            capture!!.stop(it.absolutePath)
            if (Outputs.forceFilesForShellAccessible) {
                // This shell written file must be made readable to be later accessed by this
                // process (e.g. for appending UiState). Unlike in other places, shell
                // must increase access, since it's giving the app access
                Shell.executeScriptSilent("chmod 777 ${it.absolutePath}")
            }
        }
    }

    fun record(
        fileLabel: String,
        config: PerfettoConfig,
        userspaceTracingPackage: String?,
        traceCallback: ((String) -> Unit)? = null,
        block: () -> Unit
    ): String? {
        // skip if Perfetto not supported, or on Cuttlefish (where tracing doesn't work)
        if (Build.VERSION.SDK_INT < 23 || !isAbiSupported()) {
            block()
            return null
        }

        synchronized(inUseLock) {
            if (inUse) {
                throw IllegalStateException(
                    "Reentrant Perfetto Tracing is not supported." +
                        " This means you cannot use more than one of" +
                        " BenchmarkRule/MacrobenchmarkRule/PerfettoTraceRule/PerfettoTrace.record" +
                        " together."
                )
            }
            inUse = true
        }
        // Prior to Android 11 (R), a shell property must be set to enable perfetto tracing, see
        // https://perfetto.dev/docs/quickstart/android-tracing#starting-the-tracing-services
        val propOverride = if (Build.VERSION.SDK_INT == Build.VERSION_CODES.Q) {
            PropOverride(TRACE_ENABLE_PROP, "1")
        } else null

        val path: String
        try {
            propOverride?.forceValue()
            start(config, userspaceTracingPackage)
            try {
                block()
            } finally {
                // finally here to ensure trace is fully recorded if block throws
                path = stop(fileLabel)
                traceCallback?.invoke(path)
            }
            return path
        } finally {
            propOverride?.resetIfOverridden()
            synchronized(inUseLock) {
                inUse = false
            }
        }
    }
}
