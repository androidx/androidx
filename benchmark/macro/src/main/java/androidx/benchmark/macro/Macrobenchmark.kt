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

import android.content.Intent
import android.util.Log
import androidx.benchmark.BenchmarkResult
import androidx.benchmark.InstrumentationResults
import androidx.benchmark.MetricResult
import androidx.benchmark.ResultWriter
import androidx.benchmark.perfetto.executeShellScript
import androidx.test.platform.app.InstrumentationRegistry
import androidx.test.uiautomator.By
import androidx.test.uiautomator.UiDevice
import androidx.test.uiautomator.Until

/**
 * Provides access to common operations in app automation, such as killing the app,
 * or navigating home.
 */
public class MacrobenchmarkScope(
    private val packageName: String,
    /**
     * Controls whether launches will automatically set [Intent.FLAG_ACTIVITY_CLEAR_TASK].
     *
     * Default to true, so Activity launches go through full creation lifecycle stages, instead of
     * just resume.
     */
    private val launchWithClearTask: Boolean
) {
    private val instrumentation = InstrumentationRegistry.getInstrumentation()
    private val context = instrumentation.context
    private val device = UiDevice.getInstance(instrumentation)

    /**
     * Launch the package, with a customizable intent.
     */
    fun launchPackageAndWait(
        block: (Intent) -> Unit = {}
    ) {
        val intent = context.packageManager.getLaunchIntentForPackage(packageName)
            ?: throw IllegalStateException("Unable to acquire intent for package $packageName")

        block(intent)
        launchIntentAndWait(intent)
    }

    fun launchIntentAndWait(intent: Intent) {
        // Must launch with new task, as we're not launching from an existing task
        intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
        if (launchWithClearTask) {
            intent.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TASK)
        }
        context.startActivity(intent)
        device.wait(
            Until.hasObject(By.pkg(packageName).depth(0)),
            5000 /* ms */
        )
    }

    fun pressHome(delayDurationMs: Long = 300) {
        device.pressHome()
        Thread.sleep(delayDurationMs)
    }

    fun killProcess() {
        Log.d(TAG, "Killing process $packageName")
        device.executeShellCommand("am force-stop $packageName")
    }

    /**
     * Drop Kernel's in-memory cache of disk pages.
     *
     * Enables measuring disk-based startup cost, without simply accessing cache of disk data
     * held in memory, such as during [cold startup](StartupMode.COLD).
     */
    fun dropKernelPageCache() {
        val result = device.executeShellScript(
            "echo 3 > /proc/sys/vm/drop_caches && echo Success || echo Failure"
        ).trim()
        check(result == "Success") {
            "Failed to drop kernel page cache, result: '$result'"
        }
    }
}

data class MacrobenchmarkConfig(
    val packageName: String,
    var metrics: List<Metric>,
    val compilationMode: CompilationMode = CompilationMode.SpeedProfile(),
    val iterations: Int
) {
    init {
        val metricSet = metrics.toSet()
        val hasStartupMetric = metricSet.any { it is StartupTimingMetric }
        if (hasStartupMetric) {
            val metrics = metrics.toMutableList()
            metrics += PerfettoMetric()
            this.metrics = metrics.toList()
        }
    }
}

/**
 * macrobenchmark test entrypoint, which doesn't depend on JUnit.
 *
 * This function is a building block for public testing APIs
 */
fun macrobenchmark(
    uniqueName: String,
    className: String,
    testName: String,
    config: MacrobenchmarkConfig,
    launchWithClearTask: Boolean,
    setupBlock: MacrobenchmarkScope.(Boolean) -> Unit,
    measureBlock: MacrobenchmarkScope.() -> Unit
) = withPermissiveSeLinuxPolicy {
    val startTime = System.nanoTime()
    val scope = MacrobenchmarkScope(config.packageName, launchWithClearTask)

    // always kill the process at beginning of test
    scope.killProcess()

    config.compilationMode.compile(config.packageName) {
        setupBlock(scope, false)
        measureBlock(scope)
    }

    // Perfetto collector is separate from metrics, so we can control file
    // output, and give it different (test-wide) lifecycle
    val perfettoCollector = PerfettoCaptureWrapper()
    try {
        config.metrics.forEach {
            it.configure(config)
        }
        var isFirstRun = true
        val metricResults = List(config.iterations) { iteration ->
            setupBlock(scope, isFirstRun)
            isFirstRun = false
            perfettoCollector.captureTrace(uniqueName, iteration) { tracePath ->
                try {
                    config.metrics.forEach {
                        it.start()
                    }
                    measureBlock(scope)
                } finally {
                    config.metrics.forEach {
                        it.stop()
                    }
                }
                config.metrics
                    // capture list of Map<String,Long> per metric
                    .map { it.getMetrics(config.packageName, tracePath) }
                    // merge into one map
                    .reduce { sum, element -> sum + element }
            }
        }.mergeToMetricResults()

        InstrumentationResults.instrumentationReport {
            val statsList = metricResults.map { it.stats }
            ideSummaryRecord(ideSummaryString(uniqueName, statsList))
            statsList.forEach { it.putInBundle(bundle, "") }
        }

        val warmupIterations = if (config.compilationMode is CompilationMode.SpeedProfile) {
            config.compilationMode.warmupIterations
        } else {
            0
        }

        ResultWriter.appendReport(
            BenchmarkResult(
                className = className,
                testName = testName,
                totalRunTimeNs = System.nanoTime() - startTime,
                metrics = metricResults,
                repeatIterations = config.iterations,
                thermalThrottleSleepSeconds = 0,
                warmupIterations = warmupIterations
            )
        )
    } finally {
        scope.killProcess()
    }
}

/**
 * Merge the Map<String, Long> results from each iteration into one Map<MetricResult>
 */
private fun List<Map<String, Long>>.mergeToMetricResults(): List<MetricResult> {
    val setOfAllKeys = flatMap { it.keys }.toSet()
    val listResults = setOfAllKeys.map { key ->
        // b/174175947
        key to mapNotNull {
            if (key !in it) {
                Log.w(TAG, "Value $key missing from one iteration {$it}")
            }
            it[key]
        }
    }.toMap()
    return listResults.map { (metricName, values) ->
        MetricResult(metricName, values.toLongArray())
    }.sortedBy { it.stats.name }
}

enum class StartupMode {
    /**
     * Startup from scratch - app's process is not alive, and must be started in addition to
     * Activity creation.
     *
     * See
     * [Cold startup documentation](https://developer.android.com/topic/performance/vitals/launch-time#cold)
     */
    COLD,

    /**
     * Create and display a new Activity in a currently running app process.
     *
     * See
     * [Warm startup documentation](https://developer.android.com/topic/performance/vitals/launch-time#warm)
     */
    WARM,

    /**
     * Bring existing activity to the foreground, process and Activity still exist from previous
     * launch.
     *
     * See
     * [Hot startup documentation](https://developer.android.com/topic/performance/vitals/launch-time#hot)
     */
    HOT
}

fun macrobenchmarkWithStartupMode(
    uniqueName: String,
    className: String,
    testName: String,
    config: MacrobenchmarkConfig,
    startupMode: StartupMode?,
    setupBlock: MacrobenchmarkScope.() -> Unit,
    measureBlock: MacrobenchmarkScope.() -> Unit
) {
    macrobenchmark(
        uniqueName = uniqueName,
        className = className,
        testName = testName,
        config = config,
        setupBlock = { firstIterationAfterCompile ->
            if (startupMode == StartupMode.COLD) {
                killProcess()
                // drop app pages from page cache to ensure it is loaded from disk, from scratch
                dropKernelPageCache()
            } else if (startupMode != null && firstIterationAfterCompile) {
                // warmup process by running the measure block once unmeasured
                measureBlock()
            }
            setupBlock(this)
        },
        // Don't reuse activities by default in COLD / WARM
        launchWithClearTask = startupMode == StartupMode.COLD || startupMode == StartupMode.WARM,
        measureBlock = measureBlock
    )
}
