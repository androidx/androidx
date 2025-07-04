/*
 * Copyright 2024 The Android Open Source Project
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

import androidx.annotation.RestrictTo
import java.util.concurrent.TimeUnit
import kotlin.coroutines.Continuation
import kotlin.coroutines.CoroutineContext
import kotlin.coroutines.EmptyCoroutineContext
import kotlin.coroutines.intrinsics.COROUTINE_SUSPENDED
import kotlin.coroutines.intrinsics.createCoroutineUnintercepted
import kotlin.coroutines.intrinsics.suspendCoroutineUninterceptedOrReturn
import kotlin.coroutines.resume

/**
 * This function is used to allow BenchmarkState to provide its non-suspending keepRunning(), but
 * underneath incrementally progress the underlying coroutine microbenchmark API as needed.
 *
 * It is optimized to allow the underlying coroutine API to be incrementally upgraded without any
 * changes to BenchmarkState.
 *
 * This is modeled after the suspending iterator {} sequence builder in kotlin, but optimized for:
 * - minimal allocation
 * - keeping yields:resumes at 1:1 (for simplicity)
 * - minimal virtual functions
 *
 * @see kotlin.sequences.iterator
 */
private fun createSuspendedLoop(
    block: suspend SuspendedLoopTrigger.() -> Unit
): SuspendedLoopTrigger {
    val suspendedLoopTrigger = SuspendedLoopTrigger()
    suspendedLoopTrigger.nextStep =
        block.createCoroutineUnintercepted(
            receiver = suspendedLoopTrigger,
            completion = suspendedLoopTrigger,
        )
    return suspendedLoopTrigger
}

/**
 * SuspendedLoopTrigger functions as the bridge between the new coroutine measureRepeated
 * implementation and the (soon to be) legacy Java API.
 *
 * It allows the vast majority of the benchmark library to be written in coroutines (with very
 * deliberate suspend calls, generally just for yielding the main thread) and still function within
 * a runBlocking block inside of `benchmarkState.keepRunning()`
 *
 * Eventually, the BenchmarkState api will be deprecated in favor of a Java-friendly variant of
 * measureRepeated, but this code will remain (ideally without significant change) to support the
 * BenchmarkState API in the long term.
 */
private class SuspendedLoopTrigger : Continuation<Unit> {
    @JvmField var nextStep: Continuation<Unit>? = null
    private var next: Int = -1
    private var done: Boolean = false

    /**
     * Schedule the loop manager Yields a value of loops to be run by the user of the
     * SuspendedLoopTrigger.
     */
    suspend fun awaitLoops(loopCount: Int) {
        next = loopCount
        suspendCoroutineUninterceptedOrReturn { c ->
            nextStep = c
            COROUTINE_SUSPENDED
        }
    }

    /** Gets the number of loops to run before calling [getNextLoopCount] again */
    fun getNextLoopCount(): Int {
        if (done) return 0
        nextStep!!.resume(Unit)
        return next
    }

    override val context: CoroutineContext
        get() = EmptyCoroutineContext

    override fun resumeWith(result: Result<Unit>) {
        result.getOrThrow() // just rethrow exception if it is there
        done = true
    }
}

/**
 * Control object for microbenchmarking in Java.
 *
 * Query a state object with [androidx.benchmark.junit4.BenchmarkRule.getState], and use it to
 * measure a block of Java with [BenchmarkStateLegacy.keepRunning]:
 * ```
 * @Rule
 * public BenchmarkRule benchmarkRule = new BenchmarkRule();
 *
 * @Test
 * public void sampleMethod() {
 *     BenchmarkState state = benchmarkRule.getState();
 *
 *     int[] src = new int[] { 1, 2, 3, 4, 5, 6, 7, 8, 9, 10 };
 *     while (state.keepRunning()) {
 *         int[] dest = new int[src.length];
 *         System.arraycopy(src, 0, dest, 0, src.length);
 *     }
 * }
 * ```
 *
 * Note that BenchmarkState does not give access to Perfetto traces.
 */
class BenchmarkState
@RestrictTo(RestrictTo.Scope.LIBRARY_GROUP)
constructor(testDefinition: TestDefinition, private val config: MicrobenchmarkConfig) {
    // Secondary explicit constructor allows for internal usage without experimental config opt in
    @RestrictTo(RestrictTo.Scope.LIBRARY_GROUP)
    constructor(testDefinition: TestDefinition) : this(testDefinition, MicrobenchmarkConfig())

    @JvmField
    @PublishedApi // Previously used by [BenchmarkState.keepRunningInline()]
    internal var iterationsRemaining = 0

    /** Ideally we'd call into the top level function, but it's non-suspending */
    private var internalIter = createSuspendedLoop {
        // Theoretically we'd ideally call into the top level measureRepeated function, but
        // that function isn't suspending. Making it suspend would allow this call to perform
        // tracing, but would significantly complicate the thread management of outer layers (e.g.
        // carefully scheduling where trace capture start/end happens). As this is compat code, we
        // don't bother.
        measureRepeatedImplNoTracing(
            testDefinition,
            config = config,
            loopedMeasurementBlock = { microbenchScope, loops ->
                scope = microbenchScope
                awaitLoops(loops)
            },
        )
    }

    @RestrictTo(RestrictTo.Scope.LIBRARY_GROUP) @JvmField var scope: MicrobenchmarkScope? = null

    fun pauseTiming() {
        scope!!.pauseMeasurement()
    }

    fun resumeTiming() {
        scope!!.resumeMeasurement()
    }

    @PublishedApi
    internal fun keepRunningInternal(): Boolean {
        iterationsRemaining = internalIter.getNextLoopCount()
        if (iterationsRemaining > 0) {
            iterationsRemaining--
            return true
        }
        return false
    }

    fun keepRunning(): Boolean {
        if (iterationsRemaining > 0) {
            iterationsRemaining--
            return true
        }
        return keepRunningInternal()
    }

    // Note: Constants left here to avoid churn, but should eventually be moved out to more
    // appropriate locations
    companion object {
        internal const val TAG = "Benchmark"

        /**
         * Conservative estimate for how much method tracing slows down runtime - how much longer
         * will `methodTrace {x()}` be than `x()` for nontrivial workloads.
         *
         * This is a conservative estimate, better version of this would account for OS/Art version
         *
         * Value derived from observed numbers on bramble API 31 (600-800x slowdown)
         */
        internal const val METHOD_TRACING_ESTIMATED_SLOWDOWN_FACTOR = 1000

        /**
         * Maximum duration to trace on main thread to avoid ANRs
         *
         * In practice, other types of tracing can be equally dangerous for ANRs, but method tracing
         * is the default tracing mode.
         */
        internal const val METHOD_TRACING_MAX_DURATION_NS = 4_000_000_000

        internal val DEFAULT_MEASUREMENT_DURATION_NS = TimeUnit.MILLISECONDS.toNanos(100)

        internal val SAMPLED_PROFILER_DURATION_NS =
            TimeUnit.SECONDS.toNanos(Arguments.profilerSampleDurationSeconds)

        /**
         * Used to disable error to enable internal correctness tests, which need to use method
         * tracing and can safely ignore measurement accuracy
         *
         * Ideally this would function as a true suppressible error like in Errors.kt, but existing
         * error functionality doesn't handle changing error states dynamically
         */
        internal var enableMethodTracingAffectsMeasurementError = true
    }
}
