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
import android.os.Environment.DIRECTORY_DOWNLOADS
import android.os.Environment.getExternalStoragePublicDirectory
import android.util.JsonWriter
import androidx.annotation.VisibleForTesting
import androidx.test.platform.app.InstrumentationRegistry
import java.io.File

internal object ResultWriter {
    @VisibleForTesting
    internal val reports = ArrayList<BenchmarkState.Report>()

    fun appendReport(report: BenchmarkState.Report) {
        reports.add(report)

        val arguments = InstrumentationRegistry.getArguments()
        if (arguments.getString("androidx.benchmark.output.enable")?.toLowerCase() == "true") {
            // Currently, we just overwrite the whole file
            // Ideally, append for efficiency
            val packageName =
                InstrumentationRegistry.getInstrumentation().targetContext!!.packageName
            @Suppress("DEPRECATION") // b/134925431
            val filePath = getExternalStoragePublicDirectory(DIRECTORY_DOWNLOADS)
            val file = File(filePath, "$packageName-benchmarkData.json")
            writeReport(file, reports)
        }
    }

    @VisibleForTesting
    internal fun writeReport(file: File, reports: List<BenchmarkState.Report>) {
        file.run {
            if (!exists()) {
                parentFile?.mkdirs()
                createNewFile()
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
                .value(AndroidBenchmarkRunner.sustainedPerformanceModeInUse)
            writer.endObject()

            writer.name("benchmarks").beginArray()
            reports.forEach { writer.reportObject(it) }
            writer.endArray()

            writer.endObject()

            writer.flush()
            writer.close()
        }
    }

    private fun JsonWriter.buildInfoObject(): JsonWriter {
        beginObject()
            .name("device").value(Build.DEVICE)
            .name("fingerprint").value(Build.FINGERPRINT)
            .name("model").value(Build.MODEL)
            .name("version").beginObject().name("sdk").value(Build.VERSION.SDK_INT).endObject()
        return endObject()
    }

    private fun JsonWriter.reportObject(report: BenchmarkState.Report): JsonWriter {
        beginObject()
            .name("name").value(report.testName)
            .name("className").value(report.className)
            .name("totalRunTimeNs").value(report.totalRunTimeNs)
            .name("metrics").metricsObject(report)
            .name("warmupIterations").value(report.warmupIterations)
            .name("repeatIterations").value(report.repeatIterations)
            .name("thermalThrottleSleepSeconds").value(report.thermalThrottleSleepSeconds)

        return endObject()
    }

    private fun JsonWriter.metricsObject(report: BenchmarkState.Report): JsonWriter {
        beginObject()

        name("timeNs").beginObject()
            .name("minimum").value(report.stats.min)
            .name("maximum").value(report.stats.max)
            .name("median").value(report.stats.median)

        name("runs").beginArray()
        report.data.forEach { value(it) }
        endArray()

        endObject() // timeNs

        return endObject()
    }
}
