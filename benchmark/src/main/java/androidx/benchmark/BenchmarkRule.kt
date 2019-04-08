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

package androidx.benchmark

import android.Manifest
import android.app.Activity
import android.util.Log
import androidx.test.platform.app.InstrumentationRegistry
import androidx.test.rule.GrantPermissionRule
import org.junit.Assert.assertFalse
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
 * - Summary in AndroidStudio in the test log,
 * - In simple form in Logcat with the tag "Benchmark"
 * - In csv form in Logcat with the tag "BenchmarkCsv"
 * - To the instrumentation status result Bundle on the gradle command line
 *
 * Every test in the Class using this @Rule must contain a single benchmark.
 */
class BenchmarkRule : TestRule {
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
     */
    fun getState(): BenchmarkState {
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

    /** @hide */
    val scope = Scope()

    /**
     * Handle used for controlling timing during [measureRepeated].
     */
    inner class Scope internal constructor() {
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
        inline fun <T> runWithTimingDisabled(block: () -> T): T {
            getOuterState().pauseTiming()
            val ret = block()
            getOuterState().resumeTiming()
            return ret
        }

        /**
         * Allows the inline function [runWithTimingDisabled] to be called outside of this scope.
         *
         * @hide
         */
        fun getOuterState(): BenchmarkState {
            return getState()
        }
    }

    override fun apply(base: Statement, description: Description): Statement {
        return RuleChain
            .outerRule(GrantPermissionRule.grant(Manifest.permission.WRITE_EXTERNAL_STORAGE))
            .around { base, description ->
                object : Statement() {
                    @Throws(Throwable::class)
                    override fun evaluate() {
                        applied = true
                        var invokeMethodName = description.methodName
                        Log.i(TAG, "Running ${description.className}#$invokeMethodName")

                        // validate and simplify the function name.
                        // First, remove the "test" prefix which normally comes from CTS test.
                        // Then make sure the [subTestName] is valid, not just numbers like [0].
                        if (invokeMethodName.startsWith("test")) {
                            assertTrue(
                                "The test name $invokeMethodName is too short",
                                invokeMethodName.length > 5
                            )
                            invokeMethodName = invokeMethodName.substring(4, 5).toLowerCase() +
                                    invokeMethodName.substring(5)
                        }

                        val index = invokeMethodName.lastIndexOf('[')
                        if (index > 0) {
                            val allDigits =
                                invokeMethodName.substring(index + 1, invokeMethodName.length - 1)
                                    .all { Character.isDigit(it) }
                            assertFalse(
                                "The name in [] can't contain only digits for $invokeMethodName",
                                allDigits
                            )
                        }

                        base.evaluate()

                        val fullTestName = WarningState.WARNING_PREFIX +
                                description.testClass.simpleName + "." + invokeMethodName
                        InstrumentationRegistry.getInstrumentation().sendStatus(
                            Activity.RESULT_OK,
                            internalState.getFullStatusReport(fullTestName)
                        )

                        ResultWriter.appendStats(
                            invokeMethodName, description.className, internalState.getReport()
                        )
                    }
                }
            }
            .apply(base, description)
    }

    /**
     * @hide
     */
    companion object {
        private const val TAG = "BenchmarkRule"
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
inline fun BenchmarkRule.measureRepeated(crossinline block: BenchmarkRule.Scope.() -> Unit) {
    // Note: this is an extension function to discourage calling from Java.

    // Extract members to locals, to ensure we check #applied, and we don't hit accessors
    val localState = getState()
    val localScope = scope

    while (localState.keepRunningInline()) {
        block(localScope)
    }
}
