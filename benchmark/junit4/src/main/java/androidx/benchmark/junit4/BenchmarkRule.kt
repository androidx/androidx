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
import androidx.benchmark.BenchmarkState
import androidx.test.rule.GrantPermissionRule
import androidx.tracing.Trace
import androidx.tracing.trace
import org.junit.Assert.assertTrue
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
 *     ...
 *     benchmarkRule.measureRepeated {
 *         doSomeWork()
 *     }
 *     ...
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
 *     ...
 *     BenchmarkState state = benchmarkRule.getState();
 *     while (state.keepRunning()) {
 *         doSomeWork();
 *     }
 *     ...
 * }
 * ```
 *
 * Benchmark results will be output:
 * - Summary in AndroidStudio in the test log
 * - In JSON format, on the host
 * - In simple form in Logcat with the tag "Benchmark"
 * - To the instrumentation status result Bundle on the gradle command line
 *
 * Every test in the Class using this @Rule must contain a single benchmark.
 */
public class BenchmarkRule internal constructor(
    /**
     * Used to disable reporting, for correctness tests that shouldn't report values
     * (and would trigger warnings if they did, e.g. debuggable=true)
     * Is always true when called non-internally.
     */
    private val enableReport: Boolean
) : TestRule {
    public constructor() : this(true)

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
            internalState.traceUniqueName = description.testClass.simpleName + "_" +
                invokeMethodName

            trace(description.displayName) {
                base.evaluate()
            }

            if (enableReport) {
                internalState.report(
                    fullClassName = description.className,
                    simpleClassName = description.testClass.simpleName,
                    methodName = invokeMethodName
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