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
import android.util.Log
import androidx.annotation.RestrictTo
import androidx.benchmark.Markdown.createFileLink
import androidx.test.platform.app.InstrumentationRegistry
import java.lang.StringBuilder
import java.util.Locale
import org.jetbrains.annotations.TestOnly

/** Wrapper for multi studio version link format */
@RestrictTo(RestrictTo.Scope.LIBRARY_GROUP)
data class IdeSummaryPair(val summaryV2: String) {
    constructor(v2lines: List<String>) : this(summaryV2 = v2lines.joinToString("\n"))

    /** Fallback for very old versions of Studio */
    val summaryV1: String
        get() = summaryV2
}

/** Provides a way to capture all the instrumentation results which needs to be reported. */
@RestrictTo(RestrictTo.Scope.LIBRARY_GROUP)
class InstrumentationResultScope(val bundle: Bundle = Bundle()) {

    private fun reportIdeSummary(
        /**
         * V2 output string, supports linking to files in the output dir via links of the format
         * `[link](file://<relative-path-to-trace>`).
         */
        summaryV2: String
    ) {
        bundle.putString(IDE_V1_SUMMARY_KEY, summaryV2) // deprecating v1 with a "graceful" fallback
        // Outputs.outputDirectory is safe to use in the context of Studio currently.
        // This is because AGP does not populate the `additionalTestOutputDir` argument.
        bundle.putString(IDE_V2_OUTPUT_DIR_PATH_KEY, Outputs.outputDirectory.absolutePath)
        bundle.putString(IDE_V2_SUMMARY_KEY, summaryV2)
    }

    fun reportSummaryToIde(
        warningMessage: String? = null,
        testName: String? = null,
        message: String? = null,
        measurements: Measurements? = null,
        iterationTracePaths: List<String>? = null,
        profilerResults: List<Profiler.ResultFile> = emptyList(),
        insights: List<Insight> = emptyList(),
        useTreeDisplayFormat: Boolean = false
    ) {
        if (warningMessage != null) {
            InstrumentationResults.scheduleIdeWarningOnNextReport(warningMessage)
        }
        val summaryPair =
            InstrumentationResults.ideSummary(
                testName = testName,
                message = message,
                measurements = measurements,
                iterationTracePaths = iterationTracePaths,
                profilerResults = profilerResults,
                insights = insights,
                useTreeDisplayFormat = useTreeDisplayFormat
            )
        reportIdeSummary(summaryV2 = summaryPair.summaryV2)
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

/** Provides way to report additional results via `Instrumentation.sendStatus()` / `addResult()`. */
@RestrictTo(RestrictTo.Scope.LIBRARY_GROUP)
object InstrumentationResults {

    /**
     * Bundle containing values to be reported at end of run, instead of for each test.
     *
     * See androidx.benchmark.junit.InstrumentationResultsRunListener
     */
    val runEndResultBundle: Bundle = Bundle()

    /** Creates an Instrumentation Result. */
    fun instrumentationReport(block: InstrumentationResultScope.() -> Unit) {
        val scope = InstrumentationResultScope()
        block.invoke(scope)
        reportBundle(scope.bundle)
    }

    /** Simple single line benchmark output */
    internal fun ideSummaryBasicMicro(
        benchmarkName: String,
        nanos: Double,
        allocations: Double?,
        profilerResults: List<Profiler.ResultFile>,
    ): String {
        // for readability, report nanos with 10ths only if less than 100
        var output =
            if (nanos >= 100.0) {
                // 13 alignment is enough for ~10 seconds
                "%,13d   ns".format(Locale.US, nanos.toLong())
            } else {
                // 13 + 2(.X) to match alignment above
                "%,15.1f ns".format(Locale.US, nanos)
            }
        if (allocations != null) {
            // 9 alignment is enough for ~10 million allocations
            output += "    %8d allocs".format(Locale.US, allocations.toInt())
        }
        profilerResults.forEach {
            output += "    ${createFileLink(it.label, it.outputRelativePath)}"
        }
        output += "    $benchmarkName"
        return output
    }

    private var ideWarningPrefix = ""

    @TestOnly
    fun clearIdeWarningPrefix() {
        println("clear ide warning")
        ideWarningPrefix = ""
    }

    /**
     * Schedule a string to be reported to the IDE on next benchmark report.
     *
     * Requires ideSummary to be called afterward, since we only post one instrumentation result per
     * test.
     *
     * Note that this also prints to logcat.
     */
    fun scheduleIdeWarningOnNextReport(string: String) {
        ideWarningPrefix =
            if (ideWarningPrefix.isEmpty()) {
                string
            } else {
                ideWarningPrefix + "\n" + string
            }
        string.split("\n").map { Log.w(BenchmarkState.TAG, it) }
    }

    internal fun ideSummary(
        testName: String? = null,
        message: String? = null,
        measurements: Measurements? = null,
        iterationTracePaths: List<String>? = null,
        profilerResults: List<Profiler.ResultFile> = emptyList(),
        insights: List<Insight> = emptyList(),
        useTreeDisplayFormat: Boolean = false,
    ): IdeSummaryPair {
        val warningMessage = ideWarningPrefix.ifEmpty { null }
        ideWarningPrefix = ""

        val v2metricLines: List<String>
        val linkableIterTraces =
            iterationTracePaths?.map { absolutePath -> Outputs.relativePathFor(absolutePath) }
                ?: emptyList()

        if (measurements != null) {
            require(measurements.isNotEmpty()) { "Require non-empty list of metric results." }
            val setOfMetrics = measurements.singleMetrics.map { it.name }.toSet()
            // specialized single line codepath for microbenchmarks with only 2 default metrics
            if (
                iterationTracePaths == null &&
                    testName != null &&
                    message == null &&
                    measurements.sampledMetrics.isEmpty() &&
                    (setOfMetrics == setOf("timeNs", "allocationCount") ||
                        setOfMetrics == setOf("timeNs"))
            ) {
                val nanos = measurements.singleMetrics.single { it.name == "timeNs" }.min
                val allocs =
                    measurements.singleMetrics.singleOrNull { it.name == "allocationCount" }?.min
                // add newline (note that multi-line codepath below handles newline separately)
                val warningPrefix = if (warningMessage == null) "" else warningMessage + "\n"
                return IdeSummaryPair(
                    summaryV2 =
                        warningPrefix +
                            ideSummaryBasicMicro(testName, nanos, allocs, profilerResults)
                )
            }

            val allMetrics = measurements.singleMetrics + measurements.sampledMetrics
            val maxLabelLength = allMetrics.maxOf { it.name.length }
            fun Double.toDisplayString() = "%,.1f".format(Locale.US, this)

            // max string length of any printed min/med/max is the largest max value seen. used to
            // pad.
            val maxValueLength = allMetrics.maxOf { it.max }.toDisplayString().length

            fun metricLines(
                singleTransform:
                    (
                        name: String,
                        min: String,
                        median: String,
                        max: String,
                        metricResult: MetricResult
                    ) -> String
            ) =
                measurements.singleMetrics.map {
                    singleTransform(
                        it.name.padEnd(maxLabelLength),
                        it.min.toDisplayString().padStart(maxValueLength),
                        it.median.toDisplayString().padStart(maxValueLength),
                        it.max.toDisplayString().padStart(maxValueLength),
                        it
                    )
                } +
                    measurements.sampledMetrics.map {
                        val name = it.name.padEnd(maxLabelLength)
                        val p50 = it.p50.toDisplayString().padStart(maxValueLength)
                        val p90 = it.p90.toDisplayString().padStart(maxValueLength)
                        val p95 = it.p95.toDisplayString().padStart(maxValueLength)
                        val p99 = it.p99.toDisplayString().padStart(maxValueLength)
                        // we don't try and link percentiles, since they're grouped across multiple
                        // iters
                        "  $name   P50  $p50,   P90  $p90,   P95  $p95,   P99  $p99"
                    }

            v2metricLines =
                if (linkableIterTraces.isNotEmpty()) {
                    // Per iteration trace paths present, so link min/med/max to respective
                    // iteration traces
                    metricLines { name, min, median, max, result ->
                        "  $name" +
                            "   ${createFileLink("min $min", linkableIterTraces[result.minIndex])}," +
                            "   ${createFileLink("median $median", linkableIterTraces[result.medianIndex])}," +
                            "   ${createFileLink("max $max", linkableIterTraces[result.maxIndex])}"
                    }
                } else {
                    // No iteration traces, so just basic list
                    metricLines { name, min, median, max, _ ->
                        "  $name   min $min,   median $median,   max $max"
                    }
                }
        } else {
            // no metrics to report
            v2metricLines = emptyList()
        }

        val v2lines =
            if (!useTreeDisplayFormat) { // use the regular output format
                val v2traceLinks =
                    if (linkableIterTraces.isNotEmpty()) {
                        listOf(
                            "    Traces: Iteration " +
                                linkableIterTraces
                                    .mapIndexed { index, path -> createFileLink("$index", path) }
                                    .joinToString(" ")
                        )
                    } else {
                        emptyList()
                    } +
                        profilerResults.map {
                            "    ${createFileLink(it.label, it.outputRelativePath)}"
                        }
                listOfNotNull(warningMessage, testName, message) +
                    v2metricLines +
                    v2traceLinks +
                    "" /* adds \n */
            } else { // use the experimental tree-like output format
                buildList {
                    if (warningMessage != null) add(warningMessage)
                    if (testName != null) add(testName)
                    if (message != null) add(message)
                    val tree = TreeBuilder()
                    if (v2metricLines.isNotEmpty()) {
                        tree.append("Metrics", 0)
                        for (metric in v2metricLines) tree.append(metric, 1)
                    }
                    if (insights.isNotEmpty()) {
                        tree.append("App Startup Insights", 0)
                        for ((criterion, observed) in insights) {
                            tree.append(criterion, 1)
                            tree.append(observed, 2)
                        }
                    }
                    if (linkableIterTraces.isNotEmpty() || profilerResults.isNotEmpty()) {
                        tree.append("Traces", 0)
                        if (linkableIterTraces.isNotEmpty())
                            tree.append(
                                linkableIterTraces
                                    .mapIndexed { ix, trace -> createFileLink("$ix", trace) }
                                    .joinToString(prefix = "Iteration ", separator = " "),
                                1
                            )
                        for (line in profilerResults) tree.append(
                            createFileLink(line.label, line.outputRelativePath),
                            1
                        )
                    }
                    addAll(tree.build())
                    add("")
                }
            }

        return IdeSummaryPair(v2lines = v2lines)
    }

    /**
     * Report an output file for test infra to copy.
     *
     * [reportOnRunEndOnly] `=true` should only be used for files that aggregate data across many
     * tests, such as the final report json. All other files should be unique, per test.
     *
     * In internal terms, per-test results are called "test metrics", and per-run results are called
     * "run metrics". A profiling trace of a particular method would be a test metric, the full
     * output json would be a run metric.
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
            instrumentationReport { fileRecord(key, absoluteFilePath) }
        }
    }

    /**
     * Report results bundle to instrumentation
     *
     * Before addResults() was added in the platform, we use sendStatus(). The constant '2' comes
     * from IInstrumentationResultParser.StatusCodes.IN_PROGRESS, and signals the test infra that
     * this is an "additional result" bundle, equivalent to addResults() NOTE: we should a version
     * check to call addResults(), but don't yet due to b/155103514
     *
     * @param bundle The [Bundle] to be reported to [android.app.Instrumentation]
     */
    internal fun reportBundle(bundle: Bundle) {
        InstrumentationRegistry.getInstrumentation().sendStatus(2, bundle)
    }
}

/**
 * Constructs a hierarchical, tree-like representation of data, similar to the output of the 'tree'
 * command.
 */
private class TreeBuilder {
    private val lines = mutableListOf<StringBuilder>()
    private val nbsp = '\u00A0'

    fun append(message: String, depth: Int): TreeBuilder {
        require(depth >= 0)

        // Create a new line for the tree node, with appropriate indentation using spaces.
        val line = StringBuilder()
        repeat(depth * 4) { line.append(nbsp) }
        line.append("└── ")
        line.append(message)
        lines.add(line)

        // Update vertical lines (pipes) to visually connect the new node to its parent/sibling.
        // TODO: Optimize this for deep trees to avoid potential quadratic time complexity.
        val anchorColumn = depth * 4
        var i = lines.lastIndex - 1 // start climbing with the first line above the newly added one
        while (i >= 0 && lines[i].getOrNull(anchorColumn) == nbsp) lines[i--][anchorColumn] = '│'
        if (i >= 0 && lines[i].getOrNull(anchorColumn) == '└') lines[i][anchorColumn] = '├'

        return this
    }

    fun build(): List<String> = lines.map { it.toString() }
}
