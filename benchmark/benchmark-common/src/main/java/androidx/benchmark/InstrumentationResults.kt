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

package androidx.benchmark

import android.os.Bundle
import androidx.annotation.RestrictTo
import androidx.test.platform.app.InstrumentationRegistry

/**
 * Wrapper for multi studio version link format
 *
 * TODO: drop support for very old versions of Studio in Benchmark 1.3,
 *  and remove v1 protocol support for simplicity (just post v2 twice)
 */
@RestrictTo(RestrictTo.Scope.LIBRARY_GROUP)
data class IdeSummaryPair(
    val summaryV1: String,
    val summaryV2: String
) {
    constructor(
        v1lines: List<String>,
        v2lines: List<String>
    ) : this(
        summaryV1 = v1lines.joinToString("\n"),
        summaryV2 = v2lines.joinToString("\n")
    )
}

/**
 * Provides a way to capture all the instrumentation results which needs to be reported.
 */
@RestrictTo(RestrictTo.Scope.LIBRARY_GROUP)
class InstrumentationResultScope(val bundle: Bundle = Bundle()) {

    private fun reportIdeSummary(
        /**
         * Simple text-only result summary string to output to IDE.
         */
        summaryV1: String,
        /**
         * V2 output string, supports linking to files in the output dir via links of the format
         * `[link](file://<relative-path-to-trace>`).
         */
        summaryV2: String = summaryV1
    ) {
        bundle.putString(IDE_V1_SUMMARY_KEY, summaryV1)
        // Outputs.outputDirectory is safe to use in the context of Studio currently.
        // This is because AGP does not populate the `additionalTestOutputDir` argument.
        bundle.putString(IDE_V2_OUTPUT_DIR_PATH_KEY, Outputs.outputDirectory.absolutePath)
        bundle.putString(IDE_V2_SUMMARY_KEY, summaryV2)
    }

    fun reportSummaryToIde(
        warningMessage: String? = null,
        testName: String? = null,
        message: String? = null,
        measurements: BenchmarkResult.Measurements? = null,
        iterationTracePaths: List<String>? = null,
        profilerResults: List<Profiler.ResultFile> = emptyList()
    ) {
        val summaryPair = InstrumentationResults.ideSummary(
            warningMessage = warningMessage,
            testName = testName,
            message = message,
            measurements = measurements,
            iterationTracePaths = iterationTracePaths,
            profilerResults = profilerResults
        )
        reportIdeSummary(
            summaryV1 = summaryPair.summaryV1,
            summaryV2 = summaryPair.summaryV2
        )
    }

    public fun fileRecord(key: String, path: String) {
        bundle.putString("additionalTestOutputFile_$key", path)
    }

    internal companion object {
        private const val IDE_V1_SUMMARY_KEY = "android.studio.display.benchmark"

        private const val IDE_V2_OUTPUT_DIR_PATH_KEY =
            "android.studio.v2display.benchmark.outputDirPath"
        private const val IDE_V2_SUMMARY_KEY = "android.studio.v2display.benchmark"
    }
}

/**
 * Provides way to report additional results via `Instrumentation.sendStatus()` / `addResult()`.
 */
@RestrictTo(RestrictTo.Scope.LIBRARY_GROUP)
object InstrumentationResults {
    /**
     * Bundle containing values to be reported at end of run, instead of for each test.
     *
     * See androidx.benchmark.junit.InstrumentationResultsRunListener
     */
    val runEndResultBundle: Bundle = Bundle()

    /**
     * Creates an Instrumentation Result.
     */
    fun instrumentationReport(
        block: InstrumentationResultScope.() -> Unit
    ) {
        val scope = InstrumentationResultScope()
        block.invoke(scope)
        reportBundle(scope.bundle)
    }

    /**
     * Simple single line benchmark output
     */
    internal fun ideSummaryBasicMicro(
        benchmarkName: String,
        nanos: Double,
        allocations: Double?,
        profilerResults: List<Profiler.ResultFile>,
    ): String {
        // for readability, report nanos with 10ths only if less than 100
        var output = if (nanos >= 100.0) {
            // 13 alignment is enough for ~10 seconds
            "%,13d   ns".format(nanos.toLong())
        } else {
            // 13 + 2(.X) to match alignment above
            "%,15.1f ns".format(nanos)
        }
        if (allocations != null) {
            // 9 alignment is enough for ~10 million allocations
            output += "    %8d allocs".format(allocations.toInt())
        }
        profilerResults.forEach {
            output += "    [${it.label}](file://${it.sanitizedOutputRelativePath})"
        }
        output += "    $benchmarkName"
        return output
    }

    internal fun ideSummary(
        warningMessage: String? = null,
        testName: String? = null,
        message: String? = null,
        measurements: BenchmarkResult.Measurements? = null,
        iterationTracePaths: List<String>? = null,
        profilerResults: List<Profiler.ResultFile> = emptyList()
    ): IdeSummaryPair {
        val v1metricLines: List<String>
        val v2metricLines: List<String>
        val linkableIterTraces = iterationTracePaths?.map { absolutePath ->
            Outputs.relativePathFor(absolutePath)
                .replace("(", "\\(")
                .replace(")", "\\)")
        }

        if (measurements != null) {
            require(measurements.isNotEmpty()) { "Require non-empty list of metric results." }
            val setOfMetrics = measurements.singleMetrics.map { it.name }.toSet()
            // specialized single line codepath for microbenchmarks with only 2 default metrics
            if (iterationTracePaths == null &&
                testName != null &&
                message == null &&
                measurements.sampledMetrics.isEmpty() &&
                (setOfMetrics == setOf(
                    "timeNs",
                    "allocationCount"
                ) || setOfMetrics == setOf("timeNs"))
            ) {
                val nanos = measurements.singleMetrics.single { it.name == "timeNs" }.min
                val allocs =
                    measurements.singleMetrics.singleOrNull { it.name == "allocationCount" }?.min
                return IdeSummaryPair(
                    summaryV1 = (warningMessage ?: "") + ideSummaryBasicMicro(
                        testName,
                        nanos,
                        allocs,
                        emptyList()
                    ),
                    summaryV2 = (warningMessage ?: "") + ideSummaryBasicMicro(
                        testName,
                        nanos,
                        allocs,
                        profilerResults
                    )
                )
            }

            val allMetrics = measurements.singleMetrics + measurements.sampledMetrics
            val maxLabelLength = allMetrics.maxOf { it.name.length }
            fun Double.toDisplayString() = "%,.1f".format(this)

            // max string length of any printed min/med/max is the largest max value seen. used to pad.
            val maxValueLength = allMetrics
                .maxOf { it.max }
                .toDisplayString().length

            fun metricLines(
                singleTransform: (
                    name: String,
                    min: String,
                    median: String,
                    max: String,
                    metricResult: MetricResult
                ) -> String
            ) = measurements.singleMetrics.map {
                singleTransform(
                    it.name.padEnd(maxLabelLength),
                    it.min.toDisplayString().padStart(maxValueLength),
                    it.median.toDisplayString().padStart(maxValueLength),
                    it.max.toDisplayString().padStart(maxValueLength),
                    it
                )
            } + measurements.sampledMetrics.map {
                val name = it.name.padEnd(maxLabelLength)
                val p50 = it.p50.toDisplayString().padStart(maxValueLength)
                val p90 = it.p90.toDisplayString().padStart(maxValueLength)
                val p95 = it.p95.toDisplayString().padStart(maxValueLength)
                val p99 = it.p99.toDisplayString().padStart(maxValueLength)
                // we don't try and link percentiles, since they're grouped across multiple iters
                "  $name   P50  $p50,   P90  $p90,   P95  $p95,   P99  $p99"
            }

            v1metricLines = metricLines { name, min, median, max, _ ->
                "  $name   min $min,   median $median,   max $max"
            }
            v2metricLines = if (linkableIterTraces != null) {
                // Per iteration trace paths present, so link min/med/max to respective iteration traces
                metricLines { name, min, median, max, result ->
                    "  $name" +
                        "   [min $min](file://${linkableIterTraces[result.minIndex]})," +
                        "   [median $median](file://${linkableIterTraces[result.medianIndex]})," +
                        "   [max $max](file://${linkableIterTraces[result.maxIndex]})"
                }
            } else {
                // No iteration traces, so just basic list
                v1metricLines
            }
        } else {
            // no metrics to report
            v1metricLines = emptyList()
            v2metricLines = emptyList()
        }

        val v2traceLinks = if (linkableIterTraces != null) {
            listOf(
                "    Traces: Iteration " + linkableIterTraces.mapIndexed { index, path ->
                    "[$index](file://$path)"
                }.joinToString(" ")
            )
        } else {
            emptyList()
        } + profilerResults.map {
            "    [${it.label}](file://${it.sanitizedOutputRelativePath})"
        }
        return IdeSummaryPair(
            v1lines = listOfNotNull(
                warningMessage,
                testName,
                message,
            ) + v1metricLines + /* adds \n */ "",
            v2lines = listOfNotNull(
                warningMessage,
                testName,
                message,
            ) + v2metricLines + v2traceLinks + /* adds \n */ ""
        )
    }

    /**
     * Report an output file for test infra to copy.
     *
     * [reportOnRunEndOnly] `=true` should only be used for files that aggregate data across many
     * tests, such as the final report json. All other files should be unique, per test.
     *
     * In internal terms, per-test results are called "test metrics", and per-run results are
     * called "run metrics". A profiling trace of a particular method would be a test metric, the
     * full output json would be a run metric.
     *
     * In am instrument terms, per-test results are printed with `INSTRUMENTATION_STATUS:`, and
     * per-run results are reported with `INSTRUMENTATION_RESULT:`.
     */
    @Suppress("MissingJvmstatic")
    public fun reportAdditionalFileToCopy(
        key: String,
        absoluteFilePath: String,
        reportOnRunEndOnly: Boolean = false
    ) {
        require(!key.contains('=')) {
            "Key must not contain '=', which breaks instrumentation result string parsing"
        }
        if (reportOnRunEndOnly) {
            InstrumentationResultScope(runEndResultBundle).fileRecord(key, absoluteFilePath)
        } else {
            instrumentationReport {
                fileRecord(key, absoluteFilePath)
            }
        }
    }

    /**
     * Report results bundle to instrumentation
     *
     * Before addResults() was added in the platform, we use sendStatus(). The constant '2'
     * comes from IInstrumentationResultParser.StatusCodes.IN_PROGRESS, and signals the
     * test infra that this is an "additional result" bundle, equivalent to addResults()
     * NOTE: we should a version check to call addResults(), but don't yet due to b/155103514
     *
     * @param bundle The [Bundle] to be reported to [android.app.Instrumentation]
     */
    internal fun reportBundle(bundle: Bundle) {
        InstrumentationRegistry
            .getInstrumentation()
            .sendStatus(2, bundle)
    }
}
