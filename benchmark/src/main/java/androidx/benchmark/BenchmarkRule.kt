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

import android.app.Activity
import android.util.Log
import androidx.test.platform.app.InstrumentationRegistry
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.rules.TestRule
import org.junit.runner.Description
import org.junit.runners.model.Statement

/**
 * JUnit rule for benchmarking code on an Android device.
 *
 * In Kotlin, benchmark with [keepRunning]:
 *
 * ```
 * @get:Rule
 * val benchmarkRule = BenchmarkRule();
 *
 * @Test
 * fun myBenchmark() {
 *     ...
 *     benchmarkRule.keepRunning {
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
    private val internalState = BenchmarkState()

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
    val state: BenchmarkState
    get() {
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
    val context = Context()

    /**
     * Handle used for controlling timing during [keepRunning].
     */
    inner class Context internal constructor() {
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
         * fun bitmapProcessing() = benchmarkRule.keepRunning {
         *     val input: Bitmap = runWithTimingDisabled { constructTestBitmap() }
         *     processBitmap(input)
         * }
         * ```
         */
        inline fun <T> runWithTimingDisabled(block: () -> T): T {
            state.pauseTiming()
            val ret = block()
            state.resumeTiming()
            return ret
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
     *     benchmarkRule.keepRunning {
     *         doSomeWork()
     *     }
     *     ...
     * }
     * ```
     *
     * @param block The block of code to benchmark.
     */
    inline fun keepRunning(crossinline block: Context.() -> Unit) {
        // Extract members to locals, to ensure we check #applied, and we don't hit accessors
        val localState = state
        val localContext = context

        while (localState.keepRunningInline()) {
            block(localContext)
        }
    }

    override fun apply(base: Statement, description: Description): Statement {
        return object : Statement() {
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
                    state.getFullStatusReport(fullTestName)
                )

                ResultWriter.appendStats(invokeMethodName, description.className, state.getReport())
            }
        }
    }

    /**
     * @hide
     */
    companion object {
        private const val TAG = "BenchmarkRule"
    }
}
