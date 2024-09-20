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
import androidx.benchmark.json.BenchmarkData
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.filters.SmallTest
import kotlin.test.assertContains
import kotlin.test.assertTrue
import org.junit.Assert.assertEquals
import org.junit.Rule
import org.junit.Test
import org.junit.rules.TemporaryFolder
import org.junit.runner.RunWith

@SmallTest
@RunWith(AndroidJUnit4::class)
class ResultWriterTest {
    @get:Rule val tempFolder: TemporaryFolder = TemporaryFolder()

    private val metricResults =
        listOf(MetricResult(name = "timeNs", data = listOf(100.0, 101.0, 102.0)))
    private val sampledMetricIterationData = listOf(listOf(0.0), listOf(50.0), listOf(100.0))
    private val sampledMetricResults =
        listOf(
            MetricResult(
                name = "frameTimeMs",
                iterationData = sampledMetricIterationData,
                data = sampledMetricIterationData.flatten()
            )
        )

    private val reportA =
        BenchmarkData.TestResult(
            name = "MethodA",
            className = "package.Class1",
            totalRunTimeNs = 900000000,
            metrics = metricResults,
            repeatIterations = 100000,
            thermalThrottleSleepSeconds = 90000000,
            warmupIterations = 8000,
            profilerOutputs = null
        )
    private val reportB =
        BenchmarkData.TestResult(
            name = "MethodB",
            className = "package.Class2",
            totalRunTimeNs = 900000000,
            metrics = metricResults + sampledMetricResults,
            repeatIterations = 100000,
            thermalThrottleSleepSeconds = 90000000,
            warmupIterations = 8000,
            profilerOutputs = null
        )

    @Test
    fun shouldClearExistingContent() {
        val tempFile = tempFolder.newFile()

        val fakeText = "This text should not be in the final output"
        tempFile.writeText(fakeText)

        ResultWriter.writeReport(tempFile, listOf(reportA, reportB))
        assertTrue(!tempFile.readText().startsWith(fakeText))
    }

    @Test
    fun validateJson() {
        val tempFile = tempFolder.newFile()

        val sustainedPerformanceModeInUse = IsolationActivity.sustainedPerformanceModeInUse

        ResultWriter.writeReport(tempFile, listOf(reportA, reportB))
        val context = BenchmarkData.Context()
        assertEquals(
            """
            {
                "context": {
                    "build": {
                        "brand": "${Build.BRAND}",
                        "device": "${Build.DEVICE}",
                        "fingerprint": "${Build.FINGERPRINT}",
                        "id": "${Build.ID}",
                        "model": "${Build.MODEL}",
                        "type": "${Build.TYPE}",
                        "version": {
                            "codename": "${Build.VERSION.CODENAME}",
                            "sdk": ${Build.VERSION.SDK_INT}
                        }
                    },
                    "cpuCoreCount": ${CpuInfo.coreDirs.size},
                    "cpuLocked": ${CpuInfo.locked},
                    "cpuMaxFreqHz": ${CpuInfo.maxFreqHz},
                    "memTotalBytes": ${MemInfo.memTotalBytes},
                    "sustainedPerformanceModeEnabled": $sustainedPerformanceModeInUse,
                    "artMainlineVersion": ${context.artMainlineVersion},
                    "osCodenameAbbreviated": "${context.osCodenameAbbreviated}",
                    "compilationMode": "${PackageInfo.compilationMode}",
                    "payload": {
                        "customKey1": "custom value 1",
                        "customKey2": "custom value 2"
                    }
                },
                "benchmarks": [
                    {
                        "name": "MethodA",
                        "params": {},
                        "className": "package.Class1",
                        "totalRunTimeNs": 900000000,
                        "metrics": {
                            "timeNs": {
                                "minimum": 100.0,
                                "maximum": 102.0,
                                "median": 101.0,
                                "runs": [
                                    100.0,
                                    101.0,
                                    102.0
                                ]
                            }
                        },
                        "sampledMetrics": {},
                        "warmupIterations": 8000,
                        "repeatIterations": 100000,
                        "thermalThrottleSleepSeconds": 90000000
                    },
                    {
                        "name": "MethodB",
                        "params": {},
                        "className": "package.Class2",
                        "totalRunTimeNs": 900000000,
                        "metrics": {
                            "timeNs": {
                                "minimum": 100.0,
                                "maximum": 102.0,
                                "median": 101.0,
                                "runs": [
                                    100.0,
                                    101.0,
                                    102.0
                                ]
                            }
                        },
                        "sampledMetrics": {
                            "frameTimeMs": {
                                "P50": 50.0,
                                "P90": 90.0,
                                "P95": 94.99999999999999,
                                "P99": 99.0,
                                "runs": [
                                    [
                                        0.0
                                    ],
                                    [
                                        50.0
                                    ],
                                    [
                                        100.0
                                    ]
                                ]
                            }
                        },
                        "warmupIterations": 8000,
                        "repeatIterations": 100000,
                        "thermalThrottleSleepSeconds": 90000000
                    }
                ]
            }
            """
                .trimIndent(),
            tempFile.readText()
        )
    }

    @Test
    fun validateJsonWithProfilingResults() {
        val reportWithParams =
            BenchmarkData.TestResult(
                name = "MethodWithProfilingResults",
                className = "package.Class",
                totalRunTimeNs = 900000000,
                metrics = metricResults,
                repeatIterations = 100000,
                thermalThrottleSleepSeconds = 90000000,
                warmupIterations = 8000,
                profilerOutputs =
                    listOf(
                            Profiler.ResultFile.ofPerfettoTrace(
                                label = "Trace",
                                absolutePath =
                                    Outputs.outputDirectory.absolutePath + "/trace.perfetto-trace"
                            ),
                            Profiler.ResultFile.of(
                                label = "Method Trace",
                                type = BenchmarkData.TestResult.ProfilerOutput.Type.MethodTrace,
                                outputRelativePath = "trace.trace",
                                source = MethodTracing
                            )
                        )
                        .map { BenchmarkData.TestResult.ProfilerOutput(it) }
            )

        val tempFile = tempFolder.newFile()
        ResultWriter.writeReport(tempFile, listOf(reportWithParams))
        val reportText = tempFile.readText()

        assertContains(
            reportText,
            """
                |            "profilerOutputs": [
                |                {
                |                    "type": "PerfettoTrace",
                |                    "label": "Trace",
                |                    "filename": "trace.perfetto-trace"
                |                },
                |                {
                |                    "type": "MethodTrace",
                |                    "label": "Method Trace",
                |                    "filename": "trace.trace"
                |                }
                |            ]
                """
                .trimMargin()
        )
    }

    @Test
    fun validateJsonWithParams() {
        val reportWithParams =
            BenchmarkData.TestResult(
                name = "MethodWithParams[number=2,primeNumber=true]",
                className = "package.Class",
                totalRunTimeNs = 900000000,
                metrics = metricResults,
                repeatIterations = 100000,
                thermalThrottleSleepSeconds = 90000000,
                warmupIterations = 8000,
                profilerOutputs = null
            )

        val tempFile = tempFolder.newFile()
        ResultWriter.writeReport(tempFile, listOf(reportWithParams))
        val reportText = tempFile.readText()

        assertContains(
            reportText,
            """
                |            "name": "MethodWithParams[number=2,primeNumber=true]",
                |            "params": {
                |                "number": "2",
                |                "primeNumber": "true"
                |            },
                """
                .trimMargin()
        )
    }

    @Test
    fun validateJsonWithInvalidParams() {
        val reportWithInvalidParams =
            BenchmarkData.TestResult(
                name = "MethodWithParams[number=2,=true,]",
                className = "package.Class",
                totalRunTimeNs = 900000000,
                metrics = metricResults,
                repeatIterations = 100000,
                thermalThrottleSleepSeconds = 90000000,
                warmupIterations = 8000,
                profilerOutputs = null
            )

        val tempFile = tempFolder.newFile()
        ResultWriter.writeReport(tempFile, listOf(reportWithInvalidParams))
        val reportText = tempFile.readText()

        assertTrue {
            reportText.contains(
                """
                |            "name": "MethodWithParams[number=2,=true,]",
                |            "params": {
                |                "number": "2"
                |            },
                """
                    .trimMargin()
            )
        }
    }
}
