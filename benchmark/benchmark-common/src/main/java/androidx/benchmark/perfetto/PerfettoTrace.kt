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

package androidx.benchmark.perfetto

import androidx.annotation.RequiresApi
import androidx.test.platform.app.InstrumentationRegistry
import java.io.File

@RequiresApi(23)
@ExperimentalPerfettoCaptureApi
class PerfettoTrace(
    /**
     * Absolute file path of the trace.
     *
     * Note that the trace is not guaranteed to be placed into an app-accessible directory, and
     * may require shell commands to access.
     */
    val path: String
) {
    companion object {
        /**
         * Record a Perfetto System Trace for the specified [block].
         *
         * ```
         * PerfettoTrace.record("myTrace") {
         *     // content in here is traced to myTrace_<timestamp>.perfetto_trace
         * }
         * ```
         *
         * Reentrant Perfetto trace capture is not supported, so this API may not be combined with
         * `BenchmarkRule`, `MacrobenchmarkRule`, or `PerfettoTraceRule`.
         *
         * If the block throws, the trace is still captured and passed to [traceCallback].
         */
        @JvmStatic
        @JvmOverloads
        fun record(
            /**
             * Output trace file names are labelled `<fileLabel>_<timestamp>.perfetto_trace`
             *
             * This timestamp is used for uniqueness when trace files are pulled automatically to
             * Studio.
             */
            fileLabel: String,
            /**
             * Target process to trace with app tag (enables android.os.Trace / androidx.Trace).
             *
             * By default, traces this process.
             */
            appTagPackages: List<String> = listOf(
                InstrumentationRegistry.getInstrumentation().targetContext.packageName
            ),
            /**
             * Process to trace with userspace tracing, i.e. `androidx.tracing:tracing-perfetto`,
             * ignored below API 30.
             *
             * This tracing is lower overhead than standard `android.os.Trace` tracepoints, but is
             * currently experimental.
             */
            userspaceTracingPackage: String? = null,
            /**
             * Callback for trace capture.
             *
             * This callback allows you to process the trace even if the block throws, e.g. during
             * a test failure.
             */
            traceCallback: ((PerfettoTrace) -> Unit)? = null,
            /**
             * Block to be traced.
             */
            block: () -> Unit
        ) = record(
            fileLabel = fileLabel,
            config = PerfettoConfig.Benchmark(appTagPackages),
            userspaceTracingPackage = userspaceTracingPackage,
            traceCallback = traceCallback,
            block = block
        )

        /**
         * Record a Perfetto System Trace for the specified [block], with a fully custom Perfetto
         * config, either text or binary.
         *
         * ```
         * PerfettoTrace.record("myTrace", config = """...""") {
         *     // content in here is traced to myTrace_<timestamp>.perfetto_trace
         * }
         * ```
         *
         * Reentrant Perfetto trace capture is not supported, so this API may not be combined with
         * `BenchmarkRule`, `MacrobenchmarkRule`, or `PerfettoTraceRule`.
         *
         * If the block throws, the trace is still captured and passed to [traceCallback].
         */
        @JvmStatic
        @JvmOverloads
        fun record(
            /**
             * Output trace file names are labelled `<fileLabel>_<timestamp>.perfetto_trace`
             *
             * This timestamp is used for uniqueness when trace files are pulled automatically to
             * Studio.
             */
            fileLabel: String,
            /**
             * Trace recording configuration.
             */
            config: PerfettoConfig,
            /**
             * Process to emphasize in the tracing UI.
             *
             * Used to emphasize the target process, e.g. by pre-populating Studio trace viewer
             * process selection.
             *
             * Defaults to the test's target process. Note that for self-instrumenting tests that
             * measure another app, you must pass that target app package.
             */
            highlightPackage: String =
                InstrumentationRegistry.getInstrumentation().targetContext.packageName,
            /**
             * Process to trace with userspace tracing, i.e. `androidx.tracing:tracing-perfetto`,
             * ignored below API 30.
             *
             * This tracing is lower overhead than standard `android.os.Trace` tracepoints, but is
             * currently experimental.
             */
            userspaceTracingPackage: String? = null,
            /**
             * Callback for trace capture.
             *
             * This callback allows you to process the trace even if the block throws, e.g. during
             * a test failure.
             */
            traceCallback: ((PerfettoTrace) -> Unit)? = null,
            /**
             * Block to be traced.
             */
            block: () -> Unit
        ) {
            PerfettoCaptureWrapper().record(
                fileLabel = fileLabel,
                config,
                userspaceTracingPackage,
                traceCallback = { path ->
                    File(path).appendUiState(
                        UiState(
                            timelineStart = null,
                            timelineEnd = null,
                            highlightPackage = highlightPackage
                        )
                    )
                    traceCallback?.invoke(PerfettoTrace(path))
                },
                block = block
            )
        }
    }
}