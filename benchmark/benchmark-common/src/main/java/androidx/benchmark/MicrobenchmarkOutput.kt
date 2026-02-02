/*
 * Copyright 2024 The Android Open Source Project
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

package androidx.benchmark

import android.os.Bundle
import androidx.benchmark.Errors.PREFIX
import androidx.benchmark.json.BenchmarkData

internal class MicrobenchmarkOutput(
    val definition: TestDefinition,
    val metricResults: List<MetricResult>,
    val profilerResults: List<Profiler.ResultFile>,
    val totalRunTimeNs: Long,
    val warmupIterations: Int,
    val repeatIterations: Int,
    val thermalThrottleSleepSeconds: Long,
    val reportMetricsInBundle: Boolean,
) {
    fun createBundle() =
        Bundle().apply {
            if (reportMetricsInBundle) {
                // these 'legacy' CI output metrics are considered output
                metricResults.forEach { it.putInBundle(this, PREFIX) }
            }
            InstrumentationResultScope(this)
                .reportSummaryToIde(
                    testName = definition.outputTestName,
                    measurements =
                        Measurements(singleMetrics = metricResults, sampledMetrics = emptyList()),
                    profilerResults = profilerResults,
                )
        }

    fun createJsonTestResult() =
        BenchmarkData.TestResult(
            name = definition.outputMethodName,
            className = definition.fullClassName,
            totalRunTimeNs = totalRunTimeNs,
            metrics = metricResults,
            warmupIterations = warmupIterations,
            repeatIterations = repeatIterations,
            thermalThrottleSleepSeconds = thermalThrottleSleepSeconds,
            profilerOutputs = profilerResults.map { BenchmarkData.TestResult.ProfilerOutput(it) },
        )
}
