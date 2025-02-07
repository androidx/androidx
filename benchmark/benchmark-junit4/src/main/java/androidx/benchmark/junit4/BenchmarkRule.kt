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
import android.os.Looper
import androidx.annotation.RestrictTo
import androidx.benchmark.Arguments
import androidx.benchmark.BenchmarkState
import androidx.benchmark.DeviceInfo
import androidx.benchmark.ExperimentalBenchmarkConfigApi
import androidx.benchmark.MicrobenchmarkConfig
import androidx.benchmark.MicrobenchmarkRunningState
import androidx.benchmark.MicrobenchmarkScope
import androidx.benchmark.TestDefinition
import androidx.benchmark.measureRepeatedImplWithTracing
import androidx.test.rule.GrantPermissionRule
import org.junit.Assume.assumeFalse
import org.junit.Assume.assumeTrue
import org.junit.rules.RuleChain
import org.junit.rules.TestRule
import org.junit.runner.Description
import org.junit.runners.model.Statement

/**
 * JUnit rule for benchmarking code on an Android device.
 *
 * In Kotlin, benchmark with [measureRepeated]. In Java, use [getState].
 *
 * Benchmark results will be output:
 * - Summary in Android Studio in the test log
 * - In JSON format, on the host
 * - In simple form in Logcat with the tag "Benchmark"
 *
 * Every test in the Class using this @Rule must contain a single benchmark.
 *
 * See the [Benchmark Guide](https://developer.android.com/studio/profile/benchmark) for more
 * information on writing Benchmarks.
 *
 * @sample androidx.benchmark.samples.benchmarkRuleSample
 */
class BenchmarkRule
@ExperimentalBenchmarkConfigApi
constructor(
    val config: MicrobenchmarkConfig,
) : TestRule {
    constructor() : this(config = MicrobenchmarkConfig())

    @PublishedApi
    internal // synthetic access
    var testDefinition: TestDefinition? = null
        get() {
            throwIfNotApplied()
            return field
        }

    internal // synthetic access
    var internalState: BenchmarkState? = null

    internal fun throwIfNotApplied() {
        if (!applied) {
            throw IllegalStateException(
                "Cannot get state before BenchmarkRule is applied to a test. Check that your " +
                    "BenchmarkRule is annotated correctly (@Rule in Java, @get:Rule in Kotlin)."
            )
        }
    }

    /**
     * Object used for benchmarking in Java.
     *
     * ```java
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
    fun getState(): BenchmarkState {
        // Note: this is an explicit method instead of an accessor to help convey it's only for Java
        // Kotlin users should call the [measureRepeated] method.
        throwIfNotApplied()
        return internalState!!
    }

    internal // synthetic access
    var applied = false

    // can we avoid published API here?
    @RestrictTo(RestrictTo.Scope.LIBRARY_GROUP)
    @get:RestrictTo(RestrictTo.Scope.LIBRARY_GROUP)
    val scopeFactory: (MicrobenchmarkRunningState) -> MicrobenchmarkScope = { runningState ->
        Scope(runningState)
    }

    /** Handle used for controlling measurement during [measureRepeated]. */
    inner class Scope internal constructor(internal val state: MicrobenchmarkRunningState) :

        /*
         * Ideally, the microbenchmark scope concept would live entirely in benchmark-common so that
         * we can define it in common code, without a dependence on benchmark-junit / JUnit.
         *
         * To preserve compatibility though, we have to preserve this copy, which causes the
         * following layering compromises:
         *
         * 1. The top level `measureRepeated` function accepts a `scopeFactory` function to let it
         * construct a BenchmarkRule.Scope object, even though it's a trivial wrapper around
         * MicrobenchmarkScope.
         *
         * 2. To let scope-ish calls go from BenchmarkState -> MicrobenchmarkScope ->
         * MicrobenchmarkRunningState, BenchmarkState has a LIBRARY_GROUP mutable var, so both
         * legacy BenchmarkState.keepRunning() and modern BenchmarkRule.measureRepeated have to
         * separately set BenchmarkState.scope after scope is constructed. This stinks, but it's the
         * price of compat. Both are needed only because it was valid to pause timing in a pure
         * Kotlin benchmark with rule.getState().pauseTiming()
         */
        MicrobenchmarkScope(state) {

        /**
         * Disable measurement for a block of code.
         *
         * Used for disabling timing/measurement for work that isn't part of the benchmark:
         * - When constructing per-loop randomized inputs for operations with caching,
         * - Controlling which parts of multi-stage work are measured (e.g. View measure/layout)
         * - Per-loop verification
         *
         * @sample androidx.benchmark.samples.runWithMeasurementDisabledSample
         */
        @Deprecated(
            "Renamed to runWithMeasurementDisabled to clarify all measurements are paused",
            replaceWith = ReplaceWith("runWithMeasurementDisabled")
        )
        inline fun <T> runWithTimingDisabled(block: () -> T): T {
            return runWithMeasurementDisabled(block)
        }

        /**
         * Allows the inline function [runWithTimingDisabled] to be called outside of this scope for
         * compat with compiled code using old versions of the library.
         */
        @Suppress("unused")
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

        testDefinition =
            TestDefinition(
                fullClassName = description.className,
                simpleClassName = description.testClass.simpleName,
                methodName = description.methodName
            )

        // only used with legacy getState() API, which is intended to be deprecated in the future,
        // to be replaced by Java variant of measureRepeated
        internalState = BenchmarkState(testDefinition!!, config)

        base.evaluate()
    }
}

/**
 * Benchmark a block of code.
 *
 * @param block The block of code to benchmark.
 * @sample androidx.benchmark.samples.benchmarkRuleSample
 */
public inline fun BenchmarkRule.measureRepeated(crossinline block: BenchmarkRule.Scope.() -> Unit) {
    // Note: this is an extension function to discourage calling from Java.
    if (Arguments.throwOnMainThreadMeasureRepeated) {
        check(Looper.myLooper() != Looper.getMainLooper()) {
            "Cannot invoke measureRepeated from the main thread. Instead use" +
                " measureRepeatedOnMainThread()"
        }
    }
    measureRepeatedImplWithTracing(
        postToMainThread = false,
        definition = testDefinition!!,
        config = config,
        scopeFactory = scopeFactory, // inflate custom Scope object to respect/maintain public API
        loopedMeasurementBlock = { scope, iterations ->
            val ruleScope = scope as BenchmarkRule.Scope // cast back to outer scope type
            getState().scope = scope
            var remainingIterations = iterations
            do {
                block.invoke(ruleScope)
                remainingIterations--
            } while (remainingIterations > 0)
        }
    )
}

/**
 * Benchmark a block of code, which runs on the main thread, and can safely interact with UI.
 *
 * While `@UiThreadRule` works for a standard test, it doesn't work for benchmarks of arbitrary
 * duration, as they may run for much more than 5 seconds and suffer ANRs, especially in continuous
 * runs.
 *
 * @param block The block of code to benchmark.
 * @throws java.lang.Throwable when an exception is thrown on the main thread.
 * @throws IllegalStateException if a hard deadline is exceeded while the block is running on the
 *   main thread.
 * @sample androidx.benchmark.samples.measureRepeatedOnMainThreadSample
 */
inline fun BenchmarkRule.measureRepeatedOnMainThread(
    crossinline block: BenchmarkRule.Scope.() -> Unit
) {
    check(Looper.myLooper() != Looper.getMainLooper()) {
        "Cannot invoke measureRepeatedOnMainThread from the main thread"
    }

    measureRepeatedImplWithTracing(
        postToMainThread = true,
        definition = testDefinition!!,
        config = config,
        scopeFactory = scopeFactory, // inflate custom Scope object to respect/maintain public API
        loopedMeasurementBlock = { scope, iterations ->
            val ruleScope = scope as BenchmarkRule.Scope // cast back to outer scope type
            getState().scope = scope
            var remainingIterations = iterations
            do {
                block.invoke(ruleScope)
                remainingIterations--
            } while (remainingIterations > 0)
        }
    )
}

internal inline fun Statement(crossinline evaluate: () -> Unit) =
    object : Statement() {
        override fun evaluate() = evaluate()
    }
