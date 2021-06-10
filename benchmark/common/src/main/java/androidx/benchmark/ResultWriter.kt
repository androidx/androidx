/*
 * Copyright 2019 The Android Open Source Project
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

import android.os.Build
import android.util.JsonWriter
import android.util.Log
import androidx.annotation.RestrictTo
import androidx.annotation.VisibleForTesting
import androidx.test.platform.app.InstrumentationRegistry
import java.io.File
import java.io.IOException

/** @suppress */
@RestrictTo(RestrictTo.Scope.LIBRARY_GROUP)
public object ResultWriter {

    @VisibleForTesting
    internal val reports = ArrayList<BenchmarkResult>()

    public fun appendReport(benchmarkResult: BenchmarkResult) {
        reports.add(benchmarkResult)

        if (Arguments.outputEnable) {
            // Currently, we just overwrite the whole file
            // Ideally, append for efficiency
            val packageName = InstrumentationRegistry.getInstrumentation()
                .targetContext!!
                .packageName

            Outputs.writeFile(
                fileName = "$packageName-benchmarkData.json",
                reportKey = "results_json",
                reportOnRunEndOnly = true
            ) {
                Log.d(
                    BenchmarkState.TAG,
                    "writing results to ${it.absolutePath}"
                )
                writeReport(it, reports)
            }
        } else {
            Log.d(
                BenchmarkState.TAG,
                "androidx.benchmark.output.enable not set, not writing results json"
            )
        }
    }

    @VisibleForTesting
    internal fun writeReport(file: File, benchmarkResults: List<BenchmarkResult>) {
        file.run {
            if (!exists()) {
                parentFile?.mkdirs()
                try {
                    createNewFile()
                } catch (exception: IOException) {
                    throw IOException(
                        """
                            Failed to create file for benchmark report in:
                            $parent
                            Make sure the instrumentation argument additionalTestOutputDir is set
                            to a writable directory on device. If using a version of Android Gradle
                            Plugin that doesn't support additionalTestOutputDir, ensure your app's
                            manifest file enables legacy storage behavior by adding the
                            application attribute: android:requestLegacyExternalStorage="true"
                        """.trimIndent(),
                        exception
                    )
                }
            }

            val writer = JsonWriter(bufferedWriter())
            writer.setIndent("    ")

            writer.beginObject()

            writer.name("context").beginObject()
                .name("build").buildInfoObject()
                .name("cpuCoreCount").value(CpuInfo.coreDirs.size)
                .name("cpuLocked").value(CpuInfo.locked)
                .name("cpuMaxFreqHz").value(CpuInfo.maxFreqHz)
                .name("memTotalBytes").value(MemInfo.memTotalBytes)
                .name("sustainedPerformanceModeEnabled")
                .value(IsolationActivity.sustainedPerformanceModeInUse)
            writer.endObject()

            writer.name("benchmarks").beginArray()
            benchmarkResults.forEach { writer.reportObject(it) }
            writer.endArray()

            writer.endObject()

            writer.flush()
            writer.close()
        }
    }

    private fun JsonWriter.buildInfoObject(): JsonWriter {
        beginObject()
            .name("brand").value(Build.BRAND)
            .name("device").value(Build.DEVICE)
            .name("fingerprint").value(Build.FINGERPRINT)
            .name("model").value(Build.MODEL)
            .name("version").beginObject().name("sdk").value(Build.VERSION.SDK_INT).endObject()
        return endObject()
    }

    private fun JsonWriter.reportObject(benchmarkResult: BenchmarkResult): JsonWriter {
        beginObject()
            .name("name").value(benchmarkResult.testName)
            .name("params").paramsObject(benchmarkResult)
            .name("className").value(benchmarkResult.className)
            .name("totalRunTimeNs").value(benchmarkResult.totalRunTimeNs)
            .name("metrics").metricsContainerObject(benchmarkResult.metrics)
            .name("warmupIterations").value(benchmarkResult.warmupIterations)
            .name("repeatIterations").value(benchmarkResult.repeatIterations)
            .name("thermalThrottleSleepSeconds").value(benchmarkResult.thermalThrottleSleepSeconds)
        return endObject()
    }

    private fun JsonWriter.statsObject(
        stats: Stats
    ): JsonWriter {
        name("minimum").value(stats.min)
        name("maximum").value(stats.max)
        name("median").value(stats.median)
        return this
    }

    private fun JsonWriter.metricsContainerObject(
        metricResults: List<MetricResult>
    ): JsonWriter {
        beginObject()
        metricResults.forEach {
            name(it.stats.name).beginObject()
            statsObject(it.stats)
            name("runs").beginArray()
            it.data.forEach { value(it) }
            endArray()
            endObject()
        }
        return endObject()
    }

    private fun JsonWriter.paramsObject(benchmarkResult: BenchmarkResult): JsonWriter {
        beginObject()
        getParams(benchmarkResult.testName).forEach { name(it.key).value(it.value) }
        return endObject()
    }

    private fun getParams(testName: String): Map<String, String> {
        val parameterStrStart = testName.indexOf('[')
        val parameterStrEnd = testName.lastIndexOf(']')

        val params = HashMap<String, String>()
        if (parameterStrStart >= 0 && parameterStrEnd >= 0) {
            val paramListString = testName.substring(parameterStrStart + 1, parameterStrEnd)
            paramListString.split(",").forEach { paramString ->
                val separatorIndex = paramString.indexOfFirst { it == ':' || it == '=' }
                if (separatorIndex in 1 until paramString.length - 1) {
                    val key = paramString.substring(0, separatorIndex)
                    val value = paramString.substring(separatorIndex + 1)
                    params[key] = value
                }
            }
        }
        return params
    }
}
