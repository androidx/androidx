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

package androidx.benchmark.macro

import android.annotation.SuppressLint
import android.os.Build
import androidx.annotation.RequiresApi
import androidx.annotation.RestrictTo
import androidx.benchmark.Shell
import androidx.benchmark.macro.perfetto.FrameTimingQuery
import androidx.benchmark.macro.perfetto.FrameTimingQuery.SubMetric
import androidx.benchmark.macro.perfetto.PerfettoResultsParser.parseStartupResult
import androidx.benchmark.macro.perfetto.PerfettoTraceProcessor
import androidx.benchmark.macro.perfetto.StartupTimingQuery
import androidx.test.platform.app.InstrumentationRegistry

/**
 * Metric interface.
 */
public sealed class Metric {
    internal abstract fun configure(packageName: String)

    internal abstract fun start()

    internal abstract fun stop()
    /**
     * After stopping, collect metrics
     *
     * TODO: takes package for package level filtering, but probably want a
     *  general config object coming into [start].
     */
    internal abstract fun getMetrics(captureInfo: CaptureInfo, tracePath: String): IterationResult

    internal data class CaptureInfo(
        val apiLevel: Int,
        val targetPackageName: String,
        val testPackageName: String,
        val startupMode: StartupMode?
    )
}

private fun Long.nsToDoubleMs(): Double = this / 1_000_000.0

/**
 * Legacy version of FrameTimingMetric, based on 'dumpsys gfxinfo' instead of trace data.
 *
 * Temporary - to be removed after transition to FrameTimingMetric
 */
@RestrictTo(RestrictTo.Scope.LIBRARY_GROUP_PREFIX)
public class FrameTimingGfxInfoMetric : Metric() {
    private lateinit var packageName: String
    private val helper = JankCollectionHelper()

    internal override fun configure(packageName: String) {
        this.packageName = packageName
        helper.addTrackedPackages(packageName)
    }

    internal override fun start() {
        try {
            helper.startCollecting()
        } catch (exception: RuntimeException) {
            // Ignore the exception that might result from trying to clear GfxInfo
            // The current implementation of JankCollectionHelper throws a RuntimeException
            // when that happens. This is safe to ignore because the app being benchmarked
            // is not showing any UI when this happens typically.

            // Once the MacroBenchmarkRule has the ability to setup the app in the right state via
            // a designated setup block, we can get rid of this.
            val instrumentation = InstrumentationRegistry.getInstrumentation()
            if (instrumentation != null) {
                if (!Shell.isPackageAlive(packageName)) {
                    error(exception.message ?: "Assertion error, $packageName not running")
                }
            }
        }
    }

    internal override fun stop() {
        helper.stopCollecting()
    }

    /**
     * Used to convert keys from platform to JSON format.
     *
     * This both converts `snake_case_format` to `camelCaseFormat`, and renames for clarity.
     *
     * Note that these will still output to inst results in snake_case, with `MetricNameUtils`
     * via [androidx.benchmark.MetricResult.putInBundle].
     */
    private val keyRenameMap = mapOf(
        "frame_render_time_percentile_50" to "frameTime50thPercentileMs",
        "frame_render_time_percentile_90" to "frameTime90thPercentileMs",
        "frame_render_time_percentile_95" to "frameTime95thPercentileMs",
        "frame_render_time_percentile_99" to "frameTime99thPercentileMs",
        "gpu_frame_render_time_percentile_50" to "gpuFrameTime50thPercentileMs",
        "gpu_frame_render_time_percentile_90" to "gpuFrameTime90thPercentileMs",
        "gpu_frame_render_time_percentile_95" to "gpuFrameTime95thPercentileMs",
        "gpu_frame_render_time_percentile_99" to "gpuFrameTime99thPercentileMs",
        "missed_vsync" to "vsyncMissedFrameCount",
        "deadline_missed" to "deadlineMissedFrameCount",
        "deadline_missed_legacy" to "deadlineMissedFrameCountLegacy",
        "janky_frames_count" to "jankyFrameCount",
        "janky_frames_legacy_count" to "jankyFrameCountLegacy",
        "high_input_latency" to "highInputLatencyFrameCount",
        "slow_ui_thread" to "slowUiThreadFrameCount",
        "slow_bmp_upload" to "slowBitmapUploadFrameCount",
        "slow_issue_draw_cmds" to "slowIssueDrawCommandsFrameCount",
        "total_frames" to "totalFrameCount",
        "janky_frames_percent" to "jankyFramePercent",
        "janky_frames_legacy_percent" to "jankyFramePercentLegacy"
    )

    /**
     * Filters output to only frameTimeXXthPercentileMs and totalFrameCount
     */
    private val keyAllowList = setOf(
        "frameTime50thPercentileMs",
        "frameTime90thPercentileMs",
        "frameTime95thPercentileMs",
        "frameTime99thPercentileMs",
        "totalFrameCount"
    )

    internal override fun getMetrics(captureInfo: CaptureInfo, tracePath: String) = IterationResult(
        singleMetrics = helper.metrics
            .map {
                val prefix = "gfxinfo_${packageName}_"
                val keyWithoutPrefix = it.key.removePrefix(prefix)

                if (keyWithoutPrefix != it.key && keyRenameMap.containsKey(keyWithoutPrefix)) {
                    keyRenameMap[keyWithoutPrefix]!! to it.value
                } else {
                    throw IllegalStateException("Unexpected key ${it.key}")
                }
            }
            .toMap()
            .filterKeys { keyAllowList.contains(it) },
        sampledMetrics = emptyMap(),
        timelineRangeNs = null
    )
}

/**
 * Metric which captures timing information from frames produced by a benchmark, such as
 * a scrolling or animation benchmark.
 */
@Suppress("CanSealedSubClassBeObject")
public class FrameTimingMetric : Metric() {
    internal override fun configure(packageName: String) {}
    internal override fun start() {}
    internal override fun stop() {}

    @SuppressLint("SyntheticAccessor")
    internal override fun getMetrics(captureInfo: CaptureInfo, tracePath: String): IterationResult {
        val subMetricsMsMap = FrameTimingQuery.getFrameSubMetrics(
            absoluteTracePath = tracePath,
            captureApiLevel = Build.VERSION.SDK_INT,
            packageName = captureInfo.targetPackageName
        )
            .filterKeys { it == SubMetric.FrameCpuTime || it == SubMetric.FrameNegativeSlackTime }
            .mapKeys {
                if (it.key == SubMetric.FrameCpuTime) "frameCpuTimeMs" else "frameNegativeSlackMs"
            }
            .mapValues { entry ->
                entry.value.map { timeNs -> timeNs.nsToDoubleMs() }
            }
        return IterationResult(
            singleMetrics = emptyMap(),
            sampledMetrics = subMetricsMsMap,
            timelineRangeNs = null
        )
    }
}

/**
 * Captures app startup timing metrics.
 */
@Suppress("CanSealedSubClassBeObject")
@RequiresApi(23)
public class StartupTimingMetric : Metric() {
    internal override fun configure(packageName: String) {
    }

    internal override fun start() {
    }

    internal override fun stop() {
    }

    @SuppressLint("SyntheticAccessor")
    internal override fun getMetrics(captureInfo: CaptureInfo, tracePath: String): IterationResult {
        return StartupTimingQuery.getFrameSubMetrics(
            absoluteTracePath = tracePath,
            captureApiLevel = captureInfo.apiLevel,
            targetPackageName = captureInfo.targetPackageName,
            testPackageName = captureInfo.testPackageName,

            // Pick an arbitrary startup mode if unspecified. In the future, consider throwing an
            // error if startup mode not defined
            startupMode = captureInfo.startupMode ?: StartupMode.COLD
        )?.run {
            @Suppress("UNCHECKED_CAST")
            IterationResult(
                singleMetrics = mapOf(
                    "timeToInitialDisplayMs" to timeToInitialDisplayNs.nsToDoubleMs(),
                    "timeToFullDisplayMs" to timeToFullDisplayNs?.nsToDoubleMs()
                ).filterValues { it != null } as Map<String, Double>,
                sampledMetrics = emptyMap(),
                timelineRangeNs = timelineRangeNs
            )
        } ?: IterationResult.EMPTY
    }
}

/**
 * Captures app startup timing metrics.
 */
@RestrictTo(RestrictTo.Scope.LIBRARY_GROUP_PREFIX)
@Suppress("CanSealedSubClassBeObject")
@RequiresApi(29)
public class StartupTimingLegacyMetric : Metric() {
    internal override fun configure(packageName: String) {
    }

    internal override fun start() {
    }

    internal override fun stop() {
    }

    internal override fun getMetrics(captureInfo: CaptureInfo, tracePath: String): IterationResult {
        val json = PerfettoTraceProcessor.getJsonMetrics(tracePath, "android_startup")
        return parseStartupResult(json, captureInfo.targetPackageName)
    }
}
