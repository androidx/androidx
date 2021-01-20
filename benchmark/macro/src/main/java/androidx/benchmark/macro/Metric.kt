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

import android.util.Log
import androidx.benchmark.perfetto.PerfettoResultsParser.parseResult
import androidx.benchmark.perfetto.PerfettoTraceParser
import androidx.test.platform.app.InstrumentationRegistry
import androidx.test.uiautomator.UiDevice
import com.android.helpers.CpuUsageHelper
import com.android.helpers.TotalPssHelper

/**
 * Metric interface.
 */
sealed class Metric {
    abstract fun configure(config: MacrobenchmarkConfig)

    abstract fun start()

    abstract fun stop()
    /**
     * After stopping, collect metrics
     *
     * TODO: takes package for package level filtering, but probably want a
     *  general config object coming into [start].
     */
    abstract fun getMetrics(packageName: String, tracePath: String): Map<String, Long>
}

class StartupTimingMetric : Metric() {
    private val helper = AppStartupHelper()

    override fun configure(config: MacrobenchmarkConfig) {
        // does nothing
    }

    override fun start() {
        helper.startCollecting()
    }

    override fun stop() {
        helper.stopCollecting()
    }

    override fun getMetrics(packageName: String, tracePath: String): Map<String, Long> {
        return helper.getMetrics(packageName)
    }
}

/**
 * Not public, as this needs clarified metric names, and fix zeros (b/173056421)
 */
internal class CpuUsageMetric : Metric() {
    private val helper = CpuUsageHelper().also {
        it.setEnableCpuUtilization()
    }

    override fun configure(config: MacrobenchmarkConfig) {
        // does nothing
    }

    override fun start() {
        helper.startCollecting()
    }

    override fun stop() {
        helper.stopCollecting()
    }

    override fun getMetrics(packageName: String, tracePath: String): Map<String, Long> {
        return helper.metrics
    }
}

class FrameTimingMetric : Metric() {
    private lateinit var packageName: String
    private val helper = JankCollectionHelper()

    override fun configure(config: MacrobenchmarkConfig) {
        packageName = config.packageName
        helper.addTrackedPackages(packageName)
    }

    override fun start() {
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

    override fun stop() {
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

    override fun getMetrics(packageName: String, tracePath: String): Map<String, Long> {
        return helper.metrics
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
    }
}

/**
 * Only does startup metrics now. Will need to expand scope.
 */
internal class PerfettoMetric : Metric() {
    private lateinit var packageName: String
    private lateinit var device: UiDevice
    private lateinit var parser: PerfettoTraceParser

    override fun configure(config: MacrobenchmarkConfig) {
        packageName = config.packageName
        val instrumentation = InstrumentationRegistry.getInstrumentation()
        device = instrumentation.device()
        parser = PerfettoTraceParser()
    }

    override fun start() {
        parser.copyTraceProcessorShell()
    }

    override fun stop() {
    }

    override fun getMetrics(packageName: String, tracePath: String): Map<String, Long> {
        val path = parser.shellFile?.absolutePath
        return if (path != null) {
            // TODO: Construct `METRICS` based on the config.
            val command = "$path --run-metric $METRICS $tracePath --metrics-output=json"
            Log.d(TAG, "Executing command $command")
            val json = device.executeShellCommand(command)
            Log.d(TAG, "Trace Processor result \n\n $json")
            parseResult(json, packageName)
        } else {
            emptyMap()
        }
    }

    companion object {
        private const val TAG = "PerfettoMetric"
        private const val METRICS = "android_startup"
    }
}

/**
 * Not public, as this needs clarified metric names
 */
internal class TotalPssMetric : Metric() {
    private val helper = TotalPssHelper()

    override fun configure(config: MacrobenchmarkConfig) {
        helper.setUp(config.packageName)
    }

    override fun start() {
        helper.startCollecting()
    }

    override fun stop() {
        helper.stopCollecting()
    }

    override fun getMetrics(packageName: String, tracePath: String): Map<String, Long> {
        return helper.metrics
    }
}
