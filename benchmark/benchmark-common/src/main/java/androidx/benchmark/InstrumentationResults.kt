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
 * Provides a way to capture all the instrumentation results which needs to be reported.
 * @suppress
 */
@RestrictTo(RestrictTo.Scope.LIBRARY_GROUP)
public class InstrumentationResultScope(public val bundle: Bundle = Bundle()) {
    @Suppress("MissingJvmstatic")
    public fun ideSummaryRecord(
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
 * @suppress
 */
@RestrictTo(RestrictTo.Scope.LIBRARY_GROUP)
public object InstrumentationResults {
    private const val STUDIO_OUTPUT_KEY_ID = "benchmark"

    /**
     * Bundle containing values to be reported at end of run, instead of for each test.
     *
     * See androidx.benchmark.junit.InstrumentationResultsRunListener
     */
    public val runEndResultBundle: Bundle = Bundle()

    /**
     * Creates an Instrumentation Result.
     */
    public fun instrumentationReport(
        block: InstrumentationResultScope.() -> Unit
    ) {
        val scope = InstrumentationResultScope()
        block.invoke(scope)
        reportBundle(scope.bundle)
    }

    // NOTE: this summary line will use default locale to determine separators. As
    // this line is only meant for human eyes, we don't worry about consistency here.
    internal fun ideSummaryLine(
        key: String,
        nanos: Double,
        allocations: Double?,
        traceRelPath: String?,
        profilerResult: Profiler.ResultFile?
    ): String {
        return listOfNotNull(
            // for readability, report nanos with 10ths only if less than 100
            if (nanos >= 100.0) {
                // 13 alignment is enough for ~10 seconds
                "%,13d   ns".format(nanos.toLong())
            } else {
                // 13 + 2(.X) to match alignment above
                "%,15.1f ns".format(nanos)
            },
            // 9 alignment is enough for ~10 million allocations
            allocations?.run {
                "%8d allocs".format(allocations.toInt())
            },
            traceRelPath?.run {
                // always fixed length
                "[trace](file://$traceRelPath)"
            },
            profilerResult?.run {
                // should be fixed length within a run, as each benchmark will use same profiler
                "[$label](file://$outputRelativePath)"
            },
            key
        ).joinToString("    ")
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
        if (reportOnRunEndOnly) {
            InstrumentationResultScope(runEndResultBundle).fileRecord(key, absoluteFilePath)
        } else {
            instrumentationReport {
                fileRecord(key, absoluteFilePath)
            }
        }
    }

    internal fun ideSummaryLineWrapped(
        key: String,
        nanos: Double,
        allocations: Double?,
        traceRelPath: String?,
        profilerResult: Profiler.ResultFile?
    ): String {
        val warningLines =
            Errors.acquireWarningStringForLogging()?.split("\n") ?: listOf()
        return (warningLines + ideSummaryLine(
            key = key,
            nanos = nanos,
            allocations = allocations,
            traceRelPath = traceRelPath,
            profilerResult = profilerResult
        ))
            // remove first line if empty
            .filterIndexed { index, it -> index != 0 || it.isNotBlank() }
            // join, prepending key to everything but first string,
            // to make each line look the same
            .joinToString("\n$STUDIO_OUTPUT_KEY_ID: ")
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
