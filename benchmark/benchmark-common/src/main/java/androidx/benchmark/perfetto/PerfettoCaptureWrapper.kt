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
import androidx.annotation.RestrictTo
import androidx.benchmark.InMemoryTracing
import androidx.benchmark.InProcessTracingMode
import androidx.benchmark.Outputs
import androidx.benchmark.Outputs.dateToFileName
import androidx.benchmark.PropOverride
import androidx.benchmark.ShellFile
import androidx.benchmark.UserFile
import androidx.benchmark.UserInfo
import androidx.benchmark.VirtualFile
import androidx.benchmark.inMemoryTrace
import androidx.benchmark.perfetto.PerfettoCapture.TracingLibraryConfig
import androidx.benchmark.perfetto.PerfettoHelper.Companion.LOG_TAG
import androidx.benchmark.perfetto.PerfettoHelper.Companion.isAbiSupported
import androidx.tracing.perfetto.handshake.protocol.ResponseResultCodes.RESULT_CODE_ALREADY_ENABLED
import androidx.tracing.perfetto.handshake.protocol.ResponseResultCodes.RESULT_CODE_SUCCESS
import kotlin.contracts.ExperimentalContracts
import kotlin.contracts.InvocationKind
import kotlin.contracts.contract

@RestrictTo(RestrictTo.Scope.LIBRARY_GROUP)
data class PerfettoStartResult(
    /** `true` iff in-process tracing is enabled during capture start */
    val inProcessTracingEnabled: Boolean = false,
    /** `true` iff perfetto sdk is enabled during capture start */
    val isPerfettoSdkEnabled: Boolean = false,
    /** `true` iff system tracing is enabled */
    val started: Boolean = false,
)

@RestrictTo(RestrictTo.Scope.LIBRARY_GROUP)
class PerfettoCaptureWrapper {
    private var capture: PerfettoCapture? = null
    private val traceEnabledProp = "persist.traced.enable"

    init {
        capture = PerfettoCapture()
    }

    companion object {
        val inUseLock = Any()

        /**
         * Prevents re-entrance of perfetto trace capture, as it doesn't handle this correctly
         *
         * (Single file output location, process cleanup, etc.)
         */
        var inUse = false
    }

    private fun start(
        config: PerfettoConfig,
        tracingLibraryConfig: TracingLibraryConfig?,
    ): PerfettoStartResult {
        var inProcessTracingEnabled = false
        var isPerfettoSdkEnabled = false
        capture?.apply {
            Log.d(LOG_TAG, "Recording perfetto trace")
            if (tracingLibraryConfig != null) {
                val inProcessTracingMode = tracingLibraryConfig.inProcessTracingMode
                // In-process tracing
                if (inProcessTracingMode != InProcessTracingMode.Disable) {
                    val response = startInProcessTracing(tracingLibraryConfig)
                    if (
                        response.isFailure() && inProcessTracingMode == InProcessTracingMode.Require
                    ) {
                        throw RuntimeException(response.data)
                    }
                    inProcessTracingEnabled = response.isSuccess()
                }
                // Perfetto SDK
                if (Build.VERSION.SDK_INT >= 30 && tracingLibraryConfig.enablePerfettoSdk) {
                    val (resultCode, message) = enableAndroidxTracingPerfetto(tracingLibraryConfig)
                    Log.d(LOG_TAG, "Enable full tracing result=$message")
                    // We only want to fail when we cannot enable the Perfetto SDK.
                    if (resultCode !in arrayOf(RESULT_CODE_SUCCESS, RESULT_CODE_ALREADY_ENABLED)) {
                        throw RuntimeException(
                            "Issue while enabling Perfetto SDK tracing / In-process tracing in" +
                                " ${tracingLibraryConfig.targetPackage}: $message"
                        )
                    }
                    isPerfettoSdkEnabled = true
                }
            }
            start(config)
        }
        return PerfettoStartResult(
            started = true,
            isPerfettoSdkEnabled = isPerfettoSdkEnabled,
            inProcessTracingEnabled = inProcessTracingEnabled,
        )
    }

    private fun stop(
        traceLabel: String,
        inMemoryTracingLabel: String?,
        additionalPaths: List<String>,
    ): String {
        return Outputs.writeFile(fileName = "${traceLabel}_${dateToFileName()}.perfetto-trace") {
            // The output of this method expects the final to be written in a user writeable folder.
            // If the default user is selected, perfetto can stop and write the file directly there.
            // Otherwise, we first need to write it in a shell storage and the use the VirtualFile
            // to cross between shell and user storage.

            if (UserInfo.isAdditionalUser) {
                ShellFile.inTempDir(it.name).apply {
                    capture!!.stop(
                        destinationPath = absolutePath,
                        inMemoryTracingLabel = inMemoryTracingLabel,
                        additionalPaths = additionalPaths,
                    )
                    copyTo(UserFile(it.absolutePath))
                    delete()
                }
            } else {
                capture!!.stop(
                    destinationPath = it.absolutePath,
                    inMemoryTracingLabel = inMemoryTracingLabel,
                    additionalPaths = additionalPaths,
                )
            }
        }
    }

    /** Starts in-process tracing in the [TracingLibraryConfig.targetPackage] */
    fun startInProcessTracing(config: TracingLibraryConfig): Response {
        return inMemoryTrace("start in-process tracing") {
            val connectedProfiler = ConnectedProfilerTracing(targetPackage = config.targetPackage)
            connectedProfiler.enable()
        }
    }

    /** Stops in-process tracing in the [TracingLibraryConfig.targetPackage] */
    fun stopInProcessTracing(config: TracingLibraryConfig): List<String> {
        return inMemoryTrace("stop in-process tracing") {
            val connectedProfiler = ConnectedProfilerTracing(targetPackage = config.targetPackage)
            with(connectedProfiler) {
                val response = flush()
                disable()
                check(response.isSuccess()) {
                    "Unable to flush profiles for ${config.targetPackage}"
                }
                val tracesPath = response.data
                check(tracesPath != null) {
                    "Unexpected trace output path for ${config.targetPackage}"
                }
                val relativeTracePaths = VirtualFile.fromPath(tracesPath).listFiles()
                relativeTracePaths.map { "$tracesPath/$it" }
            }
        }
    }

    @OptIn(ExperimentalContracts::class)
    fun record(
        fileLabel: String,
        config: PerfettoConfig,
        tracingLibraryConfig: TracingLibraryConfig?,
        traceCallback: ((String) -> Unit)? = null,
        enableTracing: Boolean = true,
        inMemoryTracingLabel: String? = null,
        block: () -> Unit,
    ): String? {
        contract { callsInPlace(block, InvocationKind.EXACTLY_ONCE) }
        // skip if Perfetto not supported, or if caller opts out
        if (!isAbiSupported() || !enableTracing) {
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
        val propOverride =
            if (Build.VERSION.SDK_INT == Build.VERSION_CODES.Q) {
                PropOverride(traceEnabledProp, "1")
            } else null

        val path: String
        try {
            propOverride?.forceValue()
            val result = start(config, tracingLibraryConfig)

            // To avoid b/174007010, userspace tracing is cleared and saved *during* trace, so
            // that events won't lie outside the bounds of the trace content.
            InMemoryTracing.clearEvents()
            try {
                block()
            } finally {
                val additionalPaths =
                    if (tracingLibraryConfig != null && result.inProcessTracingEnabled)
                        stopInProcessTracing(tracingLibraryConfig)
                    else emptyList()
                Log.d(
                    LOG_TAG,
                    "Additional perfetto outputs: ${additionalPaths.joinToString(separator = ",")}",
                )
                // finally here to ensure trace is fully recorded if block throws
                path = stop(fileLabel, inMemoryTracingLabel, additionalPaths)
                traceCallback?.invoke(path)
            }
        } finally {
            propOverride?.resetIfOverridden()
            synchronized(inUseLock) { inUse = false }
        }
        return path
    }
}
