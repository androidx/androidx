/*
 * Copyright 2018 The Android Open Source Project
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

package androidx.benchmark.junit4

import android.Manifest
import android.util.Log
import androidx.annotation.RestrictTo
import androidx.benchmark.Arguments
import androidx.benchmark.BenchmarkState
import androidx.benchmark.UserspaceTracing
import androidx.benchmark.perfetto.PerfettoCaptureWrapper
import androidx.benchmark.perfetto.PerfettoConfig
import androidx.benchmark.perfetto.UiState
import androidx.benchmark.perfetto.appendUiState
import androidx.test.platform.app.InstrumentationRegistry
import androidx.test.rule.GrantPermissionRule
import androidx.tracing.Trace
import androidx.tracing.trace
import java.io.File
import org.junit.Assert.assertTrue
import org.junit.Assume.assumeTrue
import org.junit.rules.RuleChain
import org.junit.rules.TestRule
import org.junit.runner.Description
import org.junit.runners.model.Statement

/**
 * JUnit rule for benchmarking code on an Android device.
 *
 * In Kotlin, benchmark with [measureRepeated]:
 *
 * ```
 * @get:Rule
 * val benchmarkRule = BenchmarkRule();
 *
 * @Test
 * fun myBenchmark() {
 *     benchmarkRule.measureRepeated {
 *         doSomeWork()
 *     }
 * }
 * ```
 *
 * In Java, use `getState()`:
 *
 * ```
 * @Rule
 * public BenchmarkRule benchmarkRule = new BenchmarkRule();
 *
 * @Test
 * public void myBenchmark() {
 *     BenchmarkState state = benchmarkRule.getState();
 *     while (state.keepRunning()) {
 *         doSomeWork();
 *     }
 * }
 * ```
 *
 * Benchmark results will be output:
 * - Summary in AndroidStudio in the test log
 * - In JSON format, on the host
 * - In simple form in Logcat with the tag "Benchmark"
 *
 * Every test in the Class using this @Rule must contain a single benchmark.
 *
 * See the [Benchmark Guide](https://developer.android.com/studio/profile/benchmark)
 * for more information on writing Benchmarks.
 */
public class BenchmarkRule internal constructor(
    /**
     * Used to disable reporting, for correctness tests that shouldn't report values
     * (and would trigger warnings if they did, e.g. debuggable=true)
     * Is always true when called non-internally.
     */
    private val enableReport: Boolean,
    private val packages: List<String> = emptyList() // TODO: revisit if needed
) : TestRule {
    public constructor() : this(true)
    @RestrictTo(RestrictTo.Scope.LIBRARY_GROUP)
    public constructor(packages: List<String>) : this(true, packages)

    internal // synthetic access
    val internalState = BenchmarkState()

    /**
     * Object used for benchmarking in Java.
     *
     * ```
     * @Rule
     * public BenchmarkRule benchmarkRule = new BenchmarkRule();
     *
     * @Test
     * public void myBenchmark() {
     *     ...
     *     BenchmarkState state = benchmarkRule.getBenchmarkState();
     *     while (state.keepRunning()) {
     *         doSomeWork();
     *     }
     *     ...
     * }
     * ```
     *
     * @throws [IllegalStateException] if the BenchmarkRule isn't correctly applied to a test.
     */
    public fun getState(): BenchmarkState {
        // Note: this is an explicit method instead of an accessor to help convey it's only for Java
        // Kotlin users should call the [measureRepeated] method.
        if (!applied) {
            throw IllegalStateException(
                "Cannot get state before BenchmarkRule is applied to a test. Check that your " +
                    "BenchmarkRule is annotated correctly (@Rule in Java, @get:Rule in Kotlin)."
            )
        }
        return internalState
    }

    internal // synthetic access
    var applied = false

    /** @suppress */
    @RestrictTo(RestrictTo.Scope.LIBRARY)
    public val scope: Scope = Scope()

    /**
     * Handle used for controlling timing during [measureRepeated].
     */
    public inner class Scope internal constructor() {
        /**
         * Disable timing for a block of code.
         *
         * Used for disabling timing for work that isn't part of the benchmark:
         * - When constructing per-loop randomized inputs for operations with caching,
         * - Controlling which parts of multi-stage work are measured (e.g. View measure/layout)
         * - Disabling timing during per-loop verification
         *
         * ```
         * @Test
         * fun bitmapProcessing() = benchmarkRule.measureRepeated {
         *     val input: Bitmap = runWithTimingDisabled { constructTestBitmap() }
         *     processBitmap(input)
         * }
         * ```
         */
        public inline fun <T> runWithTimingDisabled(block: () -> T): T {
            getOuterState().pauseTiming()
            // Note: we only bother with tracing for the runWithTimingDisabled function for
            // Kotlin callers, as it's more difficult to corrupt the trace with incorrectly
            // paired BenchmarkState pause/resume calls
            val ret: T = try {
                // TODO: use `trace() {}` instead of this manual try/finally,
                //  once the block parameter is marked crossinline.
                Trace.beginSection("runWithTimingDisabled")
                block()
            } finally {
                Trace.endSection()
            }
            getOuterState().resumeTiming()
            return ret
        }

        /**
         * Allows the inline function [runWithTimingDisabled] to be called outside of this scope.
         */
        @PublishedApi
        internal fun getOuterState(): BenchmarkState {
            return getState()
        }
    }

    override fun apply(base: Statement, description: Description): Statement {
        return RuleChain
            .outerRule(GrantPermissionRule.grant(Manifest.permission.WRITE_EXTERNAL_STORAGE))
            .around(::applyInternal)
            .apply(base, description)
    }

    private fun applyInternal(base: Statement, description: Description) =
        Statement {
            applied = true
            assumeTrue(Arguments.RuleType.Microbenchmark in Arguments.enabledRules)
            var invokeMethodName = description.methodName
            Log.d(TAG, "-- Running ${description.className}#$invokeMethodName --")

            // validate and simplify the function name.
            // First, remove the "test" prefix which normally comes from CTS test.
            // Then make sure the [subTestName] is valid, not just numbers like [0].
            if (invokeMethodName.startsWith("test")) {
                assertTrue(
                    "The test name $invokeMethodName is too short",
                    invokeMethodName.length > 5
                )
                invokeMethodName = invokeMethodName.substring(4, 5).lowercase() +
                    invokeMethodName.substring(5)
            }
            val uniqueName = description.testClass.simpleName + "_" + invokeMethodName
            internalState.traceUniqueName = uniqueName

            var userspaceTrace: perfetto.protos.Trace? = null

            val tracePath = PerfettoCaptureWrapper().record(
                fileLabel = uniqueName,
                config = PerfettoConfig.Benchmark(packages),
                userspaceTracingPackage = null
            ) {
                UserspaceTracing.commitToTrace() // clear buffer

                trace(description.displayName) { base.evaluate() }

                // To avoid b/174007010, userspace tracing is cleared and saved *during* trace, so
                // that events won't lie outside the bounds of the trace content.
                userspaceTrace = UserspaceTracing.commitToTrace()
            }?.apply {
                // trace completed, and copied into shell writeable dir
                val file = File(this)
                file.appendBytes(userspaceTrace!!.encode())
                file.appendUiState(
                    UiState(
                        timelineStart = null,
                        timelineEnd = null,
                        highlightPackage = InstrumentationRegistry.getInstrumentation()
                            .context.packageName
                    )
                )
            }

            if (enableReport) {
                internalState.report(
                    fullClassName = description.className,
                    simpleClassName = description.testClass.simpleName,
                    methodName = invokeMethodName,
                    tracePath = tracePath
                )
            }
        }

    internal companion object {
        private const val TAG = "Benchmark"
    }
}

/**
 * Benchmark a block of code.
 *
 * ```
 * @get:Rule
 * val benchmarkRule = BenchmarkRule();
 *
 * @Test
 * fun myBenchmark() {
 *     ...
 *     benchmarkRule.measureRepeated {
 *         doSomeWork()
 *     }
 *     ...
 * }
 * ```
 *
 * @param block The block of code to benchmark.
 */
public inline fun BenchmarkRule.measureRepeated(crossinline block: BenchmarkRule.Scope.() -> Unit) {
    // Note: this is an extension function to discourage calling from Java.

    // Extract members to locals, to ensure we check #applied, and we don't hit accessors
    val localState = getState()
    val localScope = scope

    while (localState.keepRunningInline()) {
        block(localScope)
    }
}

internal inline fun Statement(crossinline evaluate: () -> Unit) = object : Statement() {
    override fun evaluate() = evaluate()
}