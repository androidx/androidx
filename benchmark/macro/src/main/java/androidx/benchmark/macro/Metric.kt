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

import androidx.annotation.RequiresApi
import androidx.benchmark.macro.perfetto.PerfettoResultsParser.parseStartupResult
import androidx.benchmark.macro.perfetto.PerfettoTraceProcessor
import androidx.test.platform.app.InstrumentationRegistry
import androidx.test.uiautomator.UiDevice

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
    internal abstract fun getMetrics(packageName: String, tracePath: String): MetricsWithUiState
}

public class FrameTimingMetric : Metric() {
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
                val device = UiDevice.getInstance(instrumentation)
                val result = device.executeShellCommand("ps -A | grep $packageName")
                if (!result.isNullOrEmpty()) {
                    error(exception.message ?: "Assertion error (Found $packageName)")
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
     * via [androidx.benchmark.Stats.putInBundle].
     */
    private val keyRenameMap = mapOf(
        "jank_percentile_50" to "frameTime50thPercentileMs",
        "jank_percentile_90" to "frameTime90thPercentileMs",
        "jank_percentile_95" to "frameTime95thPercentileMs",
        "jank_percentile_99" to "frameTime99thPercentileMs",
        "gpu_jank_percentile_50" to "gpuFrameTime50thPercentileMs",
        "gpu_jank_percentile_90" to "gpuFrameTime90thPercentileMs",
        "gpu_jank_percentile_95" to "gpuFrameTime95thPercentileMs",
        "gpu_jank_percentile_99" to "gpuFrameTime99thPercentileMs",
        "missed_vsync" to "vsyncMissedFrameCount",
        "deadline_missed" to "deadlineMissedFrameCount",
        "janky_frames_count" to "jankyFrameCount",
        "high_input_latency" to "highInputLatencyFrameCount",
        "slow_ui_thread" to "slowUiThreadFrameCount",
        "slow_bmp_upload" to "slowBitmapUploadFrameCount",
        "slow_issue_draw_cmds" to "slowIssueDrawCommandsFrameCount",
        "total_frames" to "totalFrameCount",
        "janky_frames_percent" to "jankyFramePercent"
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

    internal override fun getMetrics(packageName: String, tracePath: String) = MetricsWithUiState(
        metrics = helper.metrics
            .map {
                val prefix = "gfxinfo_${packageName}_"
                val keyWithoutPrefix = it.key.removePrefix(prefix)

                if (keyWithoutPrefix != it.key && keyRenameMap.containsKey(keyWithoutPrefix)) {
                    // note - this conversion truncates
                    val newValue = it.value.toLong()
                    @Suppress("MapGetWithNotNullAssertionOperator")
                    keyRenameMap[keyWithoutPrefix]!! to newValue
                } else {
                    throw IllegalStateException("Unexpected key ${it.key}")
                }
            }
            .toMap()
            .filterKeys { keyAllowList.contains(it) }
    )
}

/**
 * Captures app startup timing metrics.
 */
@Suppress("CanSealedSubClassBeObject")
@RequiresApi(29)
public class StartupTimingMetric : Metric() {
    internal override fun configure(packageName: String) {
    }

    internal override fun start() {
    }

    internal override fun stop() {
    }

    internal override fun getMetrics(packageName: String, tracePath: String): MetricsWithUiState {
        val json = PerfettoTraceProcessor.getJsonMetrics(tracePath, "android_startup")
        return parseStartupResult(json, packageName)
    }
}

internal data class MetricsWithUiState(
    val metrics: Map<String, Long>,
    val timelineStart: Long? = null,
    val timelineEnd: Long? = null
) {
    operator fun plus(element: MetricsWithUiState) = MetricsWithUiState(
        metrics = metrics + element.metrics,
        timelineStart = minOfNullable(timelineStart, element.timelineStart),
        timelineEnd = maxOfNullable(timelineEnd, element.timelineEnd)
    )

    companion object {
        val EMPTY = MetricsWithUiState(mapOf())
    }
}

internal fun minOfNullable(a: Long?, b: Long?): Long? {
    if (a == null) return b
    if (b == null) return a
    return minOf(a, b)
}

internal fun maxOfNullable(a: Long?, b: Long?): Long? {
    if (a == null) return b
    if (b == null) return a
    return maxOf(a, b)
}