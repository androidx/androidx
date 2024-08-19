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
import android.os.Build
import android.os.Looper
import android.util.Log
import androidx.annotation.RestrictTo
import androidx.benchmark.Arguments
import androidx.benchmark.BenchmarkState
import androidx.benchmark.DeviceInfo
import androidx.benchmark.ExperimentalBenchmarkConfigApi
import androidx.benchmark.MicrobenchmarkConfig
import androidx.benchmark.perfetto.PerfettoCapture
import androidx.benchmark.perfetto.PerfettoCaptureWrapper
import androidx.benchmark.perfetto.PerfettoConfig
import androidx.benchmark.perfetto.UiState
import androidx.benchmark.perfetto.appendUiState
import androidx.test.platform.app.InstrumentationRegistry
import androidx.test.platform.app.InstrumentationRegistry.getInstrumentation
import androidx.test.rule.GrantPermissionRule
import androidx.tracing.Trace
import androidx.tracing.trace
import java.io.File
import java.util.concurrent.ExecutionException
import java.util.concurrent.FutureTask
import java.util.concurrent.TimeUnit
import org.junit.Assert.assertTrue
import org.junit.Assume.assumeFalse
import org.junit.Assume.assumeTrue
import org.junit.rules.RuleChain
import org.junit.rules.TestRule
import org.junit.runner.Description
import org.junit.runners.model.Statement

/**
 * JUnit rule for benchmarking code on an Android device.
 *
 * In Kotlin, benchmark with [measureRepeated]:
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
 * See the [Benchmark Guide](https://developer.android.com/studio/profile/benchmark) for more
 * information on writing Benchmarks.
 */
public class BenchmarkRule
private constructor(
    private val config: MicrobenchmarkConfig?,
    /**
     * This param is ignored, and just present to disambiguate the internal (nullable) vs external
     * (non-null) variants of the constructor, since a lint failure occurs if they have the same
     * signature, even if the external variant uses `this(config as MicrobenchmarkConfig?)`.
     *
     * In the future, we should just always pass a "default" config object, which can reference
     * default values from Arguments, but that's a deeper change.
     */
    @Suppress("UNUSED_PARAMETER") ignored: Boolean = true
) : TestRule {
    constructor() : this(config = null, ignored = true)

    @ExperimentalBenchmarkConfigApi
    constructor(config: MicrobenchmarkConfig) : this(config, ignored = true)

    internal // synthetic access
    var internalState = BenchmarkState(config)

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

    @get:RestrictTo(RestrictTo.Scope.LIBRARY) public val scope: Scope = Scope()

    /** Handle used for controlling timing during [measureRepeated]. */
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
            val ret: T =
                try {
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
        return RuleChain.outerRule(
                GrantPermissionRule.grant(Manifest.permission.WRITE_EXTERNAL_STORAGE)
            )
            .around(::applyInternal)
            .apply(base, description)
    }

    private fun applyInternal(base: Statement, description: Description) = Statement {
        applied = true

        assumeTrue(Arguments.RuleType.Microbenchmark in Arguments.enabledRules)

        // When running on emulator and argument `skipOnEmulator` is passed,
        // the test is skipped.
        if (Arguments.skipBenchmarksOnEmulator) {
            assumeFalse(
                "Skipping test because it's running on emulator and `skipOnEmulator` is enabled",
                DeviceInfo.isEmulator
            )
        }

        var invokeMethodName = description.methodName
        Log.d(TAG, "-- Running ${description.className}#$invokeMethodName --")

        // validate and simplify the function name.
        // First, remove the "test" prefix which normally comes from CTS test.
        // Then make sure the [subTestName] is valid, not just numbers like [0].
        if (invokeMethodName.startsWith("test")) {
            assertTrue("The test name $invokeMethodName is too short", invokeMethodName.length > 5)
            invokeMethodName =
                invokeMethodName.substring(4, 5).lowercase() + invokeMethodName.substring(5)
        }
        val uniqueName = description.testClass.simpleName + "_" + invokeMethodName
        internalState.traceUniqueName = uniqueName

        val tracePath =
            PerfettoCaptureWrapper()
                .record(
                    fileLabel = uniqueName,
                    config =
                        PerfettoConfig.Benchmark(
                            appTagPackages =
                                if (config?.traceAppTagEnabled == true) {
                                    listOf(
                                        InstrumentationRegistry.getInstrumentation()
                                            .context
                                            .packageName
                                    )
                                } else {
                                    emptyList()
                                },
                            useStackSamplingConfig = false
                        ),
                    // TODO(290918736): add support for Perfetto SDK Tracing in
                    //  Microbenchmark in other cases, outside of MicrobenchmarkConfig
                    perfettoSdkConfig =
                        if (
                            config?.perfettoSdkTracingEnabled == true &&
                                Build.VERSION.SDK_INT >= Build.VERSION_CODES.M
                        ) {
                            PerfettoCapture.PerfettoSdkConfig(
                                InstrumentationRegistry.getInstrumentation().context.packageName,
                                PerfettoCapture.PerfettoSdkConfig.InitialProcessState.Alive
                            )
                        } else {
                            null
                        },

                    // Optimize throughput in dryRunMode, since trace isn't useful, and extremely
                    //   expensive on some emulators. Could alternately use UserspaceTracing if
                    // desired
                    // Additionally, skip on misconfigured devices to still enable benchmarking.
                    enableTracing = !Arguments.dryRunMode && !DeviceInfo.misconfiguredForTracing,
                    inMemoryTracingLabel = "Microbenchmark"
                ) {
                    trace(description.displayName) { base.evaluate() }
                }
                ?.apply {
                    // trace completed, and copied into shell writeable dir
                    val file = File(this)
                    file.appendUiState(
                        UiState(
                            timelineStart = null,
                            timelineEnd = null,
                            highlightPackage =
                                InstrumentationRegistry.getInstrumentation().context.packageName
                        )
                    )
                }

        internalState.report(
            fullClassName = description.className,
            simpleClassName = description.testClass.simpleName,
            methodName = invokeMethodName,
            perfettoTracePath = tracePath
        )
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

    try {
        while (localState.keepRunningInline()) {
            block(localScope)
        }
    } catch (t: Throwable) {
        localState.cleanupBeforeThrow()
        throw t
    }
}

/**
 * Benchmark a block of code, which runs on the main thread, and can safely interact with UI.
 *
 * While `@UiThreadRule` works for a standard test, it doesn't work for benchmarks of arbitrary
 * duration, as they may run for much more than 5 seconds and suffer ANRs, especially in continuous
 * runs.
 *
 * ```
 * @get:Rule
 * val benchmarkRule = BenchmarkRule();
 *
 * @Test
 * fun myBenchmark() {
 *     ...
 *     benchmarkRule.measureRepeatedOnMainThread {
 *         doSomeWorkOnMainThread()
 *     }
 *     ...
 * }
 * ```
 *
 * @param block The block of code to benchmark.
 * @throws java.lang.Throwable when an exception is thrown on the main thread.
 * @throws IllegalStateException if a hard deadline is exceeded while the block is running on the
 *   main thread.
 */
@Suppress("DocumentExceptions") // `@throws Throwable` not recognized (b/305050883)
inline fun BenchmarkRule.measureRepeatedOnMainThread(
    crossinline block: BenchmarkRule.Scope.() -> Unit
) {
    check(Looper.myLooper() != Looper.getMainLooper()) {
        "Cannot invoke measureRepeatedOnMainThread from the main thread"
    }

    var resumeScheduled = false
    while (true) {
        val task = FutureTask {
            // Extract members to locals, to ensure we check #applied, and we don't hit accessors
            val localState = getState()
            val localScope = scope

            val initialTimeNs = System.nanoTime()
            val softDeadlineNs = initialTimeNs + TimeUnit.SECONDS.toNanos(2)
            val hardDeadlineNs = initialTimeNs + TimeUnit.SECONDS.toNanos(10)
            var timeNs: Long = 0

            try {
                Trace.beginSection("measureRepeatedOnMainThread task")

                if (resumeScheduled) {
                    localState.resumeTiming()
                }

                do {
                    // note that this function can still block for considerable time, e.g. when
                    // setting up / tearing down profiling, or sleeping to let the device cool off.
                    if (!localState.keepRunningInline()) {
                        return@FutureTask false
                    }

                    block(localScope)

                    // Avoid checking for deadline on all but last iteration per measurement,
                    // to amortize cost of System.nanoTime(). Without this optimization, minimum
                    // measured time can be 10x higher.
                    if (localState.getIterationsRemaining() != 1) {
                        continue
                    }
                    timeNs = System.nanoTime()
                } while (timeNs <= softDeadlineNs)

                resumeScheduled = true
                localState.pauseTiming()

                if (timeNs > hardDeadlineNs) {
                    localState.cleanupBeforeThrow()
                    val overrunInSec = (timeNs - hardDeadlineNs) / 1_000_000_000.0
                    throw IllegalStateException(
                        "Benchmark loop overran hard time limit by $overrunInSec seconds"
                    )
                }

                return@FutureTask true // continue
            } finally {
                Trace.endSection()
            }
        }
        getInstrumentation().runOnMainSync(task)
        val shouldContinue: Boolean =
            try {
                // Ideally we'd implement the delay here, as a timeout, but we can't do this until
                // have a way to move thermal throttle sleeping off the UI thread.
                task.get()
            } catch (e: ExecutionException) {
                // Expose the original exception
                throw e.cause!!
            }
        if (!shouldContinue) {
            // all done
            break
        }
    }
}

internal inline fun Statement(crossinline evaluate: () -> Unit) =
    object : Statement() {
        override fun evaluate() = evaluate()
    }
