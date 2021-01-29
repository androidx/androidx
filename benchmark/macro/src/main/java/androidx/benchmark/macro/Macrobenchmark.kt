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

package androidx.benchmark.macro/*
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

import android.util.Log
import androidx.benchmark.BenchmarkResult
import androidx.benchmark.InstrumentationResults
import androidx.benchmark.MetricResult
import androidx.benchmark.ResultWriter
import androidx.benchmark.macro.perfetto.PerfettoCaptureWrapper

/**
 * macrobenchmark test entrypoint, which doesn't depend on JUnit.
 *
 * This function is a building block for public testing APIs
 */
fun macrobenchmark(
    uniqueName: String,
    className: String,
    testName: String,
    packageName: String,
    metrics: List<Metric>,
    compilationMode: CompilationMode = CompilationMode.SpeedProfile(),
    iterations: Int,
    launchWithClearTask: Boolean,
    setupBlock: MacrobenchmarkScope.(Boolean) -> Unit,
    measureBlock: MacrobenchmarkScope.() -> Unit
) {
    val startTime = System.nanoTime()
    val scope = MacrobenchmarkScope(packageName, launchWithClearTask)

    // always kill the process at beginning of test
    scope.killProcess()

    compilationMode.compile(packageName) {
        setupBlock(scope, false)
        measureBlock(scope)
    }

    // Perfetto collector is separate from metrics, so we can control file
    // output, and give it different (test-wide) lifecycle
    val perfettoCollector = PerfettoCaptureWrapper()
    try {
        metrics.forEach {
            it.configure(packageName)
        }
        var isFirstRun = true
        val metricResults = List(iterations) { iteration ->
            setupBlock(scope, isFirstRun)
            isFirstRun = false
            perfettoCollector.start()

            try {
                metrics.forEach {
                    it.start()
                }
                measureBlock(scope)
            } finally {
                metrics.forEach {
                    it.stop()
                }
            }
            val tracePath = perfettoCollector.stop(uniqueName, iteration)
            metrics
                // capture list of Map<String,Long> per metric
                .map { it.getMetrics(packageName, tracePath!!) }
                // merge into one map
                .reduce { sum, element -> sum + element }
        }.mergeToMetricResults()

        InstrumentationResults.instrumentationReport {
            val statsList = metricResults.map { it.stats }
            ideSummaryRecord(ideSummaryString(uniqueName, statsList))
            statsList.forEach { it.putInBundle(bundle, "") }
        }

        val warmupIterations = if (compilationMode is CompilationMode.SpeedProfile) {
            compilationMode.warmupIterations
        } else {
            0
        }

        ResultWriter.appendReport(
            BenchmarkResult(
                className = className,
                testName = testName,
                totalRunTimeNs = System.nanoTime() - startTime,
                metrics = metricResults,
                repeatIterations = iterations,
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

fun macrobenchmarkWithStartupMode(
    uniqueName: String,
    className: String,
    testName: String,
    packageName: String,
    metrics: List<Metric>,
    compilationMode: CompilationMode = CompilationMode.SpeedProfile(),
    iterations: Int,
    startupMode: StartupMode?,
    setupBlock: MacrobenchmarkScope.() -> Unit,
    measureBlock: MacrobenchmarkScope.() -> Unit
) {
    macrobenchmark(
        uniqueName = uniqueName,
        className = className,
        testName = testName,
        packageName = packageName,
        metrics = metrics,
        compilationMode = compilationMode,
        iterations = iterations,
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
