package androidx.benchmark

import android.annotation.SuppressLint
import android.util.Log
import androidx.benchmark.CpuEventCounter.Event
import java.util.concurrent.TimeUnit
import kotlinx.coroutines.delay

internal class MicrobenchmarkPhase(
    val label: String,
    val measurementCount: Int,
    val loopMode: LoopMode,
    val metrics: Array<MetricCapture> = arrayOf(TimeCapture()),
    val profiler: Profiler? = null,
    val gcBeforePhase: Boolean = false,
    val thermalThrottleSleepsMax: Int = 0,
) {
    val metricsContainer = MetricsContainer(metrics, measurementCount)
    private var thermalThrottleSleepsRemaining = thermalThrottleSleepsMax
    var thermalThrottleSleepSeconds = 0L

    init {
        if (loopMode.warmupManager != null) {
            check(metricsContainer.names.first() == "timeNs" && metricsContainer.names.size <= 2) {
                "If warmup is enabled, expect to only capture one or two metrics"
            }
        }
    }

    /** @return If true, finishing the phase was successful, otherwise must be retried */
    fun tryEnd(): Boolean {
        return if (thermalThrottleSleepsRemaining > 0 && sleepIfThermalThrottled()) {
            thermalThrottleSleepsRemaining--
            false // don't start next phase, do-over
        } else {
            if (thermalThrottleSleepsMax > 0 && thermalThrottleSleepsRemaining == 0) {
                // If we ran out of throttle detection retries, it's possible the throttle baseline
                // is incorrect. Force next benchmark to recompute it.
                ThrottleDetector.resetThrottleBaseline()
            }
            true // start next phase
        }
    }

    @SuppressLint("BanThreadSleep") // we all need sleep to cool off sometimes
    private fun sleepIfThermalThrottled(): Boolean =
        when {
            ThrottleDetector.isDeviceThermalThrottled() -> {
                Log.d(
                    BenchmarkState.TAG,
                    "THERMAL THROTTLE DETECTED, SLEEPING FOR $THROTTLE_BACKOFF_S SECONDS"
                )
                val startTimeNs = System.nanoTime()
                inMemoryTrace("Sleep due to Thermal Throttle") {
                    Thread.sleep(TimeUnit.SECONDS.toMillis(THROTTLE_BACKOFF_S))
                }
                val sleepTimeNs = System.nanoTime() - startTimeNs
                thermalThrottleSleepSeconds += TimeUnit.NANOSECONDS.toSeconds(sleepTimeNs)
                true
            }
            else -> false
        }

    internal suspend inline fun execute(
        traceUniqueName: String,
        scope: MicrobenchmarkScope,
        state: MicrobenchmarkRunningState,
        loopedMeasurementBlock: LoopedMeasurementBlock
    ) {
        var thermalThrottleSleepsRemaining = thermalThrottleSleepsMax
        val loopsPerMeasurement = loopMode.getIterations(state.warmupEstimatedIterationTimeNs)
        state.maxIterationsPerRepeat =
            state.maxIterationsPerRepeat.coerceAtLeast(loopsPerMeasurement)

        var phaseProfilerResult: Profiler.ResultFile?
        try {
            InMemoryTracing.beginSection(label)
            if (gcBeforePhase) {
                // Run GC to avoid memory pressure from previous run from affecting this one.
                // Note, we don't use System.gc() because it doesn't always have consistent behavior
                Runtime.getRuntime().gc()
            }
            var profilerStartBegin = 0L
            var profilerStartEnd = 0L
            while (true) { // keep running until phase successful
                try {
                    phaseProfilerResult =
                        profiler?.run {
                            profilerStartBegin = System.nanoTime()
                            startIfNotRiskingAnrDeadline(
                                    traceUniqueName = traceUniqueName,
                                    estimatedDurationNs = state.warmupEstimatedIterationTimeNs
                                )
                                .also { profilerStartEnd = System.nanoTime() }
                        }
                    state.metrics = metricsContainer // needed for pausing
                    metricsContainer.captureInit()

                    // warmup the container
                    metricsContainer.captureStart()
                    metricsContainer.captureStop()
                    metricsContainer.captureInit()

                    repeat(measurementCount) {
                        // perform measurement
                        metricsContainer.captureStart()
                        loopedMeasurementBlock.invoke(scope, loopsPerMeasurement)
                        metricsContainer.captureStop()
                        state.yieldThreadIfDeadlinePassed()
                    }
                    if (loopMode.warmupManager != null) {
                        // warmup, so retry until complete
                        metricsContainer.captureInit()
                        // Note that warmup is based on repeat time, *not* the timeNs metric, since
                        // we
                        // want to account for paused time during warmup (paused work should
                        // stabilize
                        // too)
                        val lastMeasuredWarmupValue = metricsContainer.peekSingleRepeatTime()
                        if (loopMode.warmupManager.onNextIteration(lastMeasuredWarmupValue)) {
                            state.warmupEstimatedIterationTimeNs = lastMeasuredWarmupValue
                            state.warmupIterations = loopMode.warmupManager.iteration
                            break
                        } else {
                            continue
                        }
                    }
                } finally {
                    profiler?.run {
                        val profilerStopBegin = System.nanoTime()
                        stop()
                        val profilerStopEnd = System.nanoTime()
                        // instead of actually using inMemoryTrace(){} directly to trace profiling,
                        // we record timestamps and defer to avoid profiling the tracing logic
                        // itself, since it's very intrusive to method traces
                        InMemoryTracing.beginSection("start profiling", profilerStartBegin)
                        InMemoryTracing.endSection(profilerStartEnd)
                        InMemoryTracing.beginSection("stop profiling", profilerStopBegin)
                        InMemoryTracing.endSection(profilerStopEnd)
                    }
                    state.yieldThreadIfDeadlinePassed()
                }
                if (!ThrottleDetector.isDeviceThermalThrottled()) {
                    // not thermal throttled, phase complete
                    break
                } else {
                    // thermal throttled! delay and retry!
                    Log.d(
                        BenchmarkState.TAG,
                        "THERMAL THROTTLE DETECTED, DELAYING FOR " +
                            "${Arguments.thermalThrottleSleepDurationSeconds} SECONDS"
                    )
                    val startTimeNs = System.nanoTime()
                    inMemoryTrace("Sleep due to Thermal Throttle") {
                        delay(
                            TimeUnit.SECONDS.toMillis(Arguments.thermalThrottleSleepDurationSeconds)
                        )
                    }
                    val sleepTimeNs = System.nanoTime() - startTimeNs
                    state.totalThermalThrottleSleepSeconds +=
                        TimeUnit.NANOSECONDS.toSeconds(sleepTimeNs)
                    thermalThrottleSleepsRemaining--
                    if (thermalThrottleSleepsRemaining <= 0) break
                }
            }
        } finally {
            InMemoryTracing.endSection()
        }
        if (loopMode.warmupManager == null) {
            // Save captured metrics except during warmup, where we intentionally discard
            state.metricResults.addAll(
                metricsContainer.captureFinished(maxIterations = loopsPerMeasurement)
            )
        }
        if (phaseProfilerResult != null) {
            state.profilerResults.add(phaseProfilerResult)
        }
    }

    internal sealed class LoopMode(val warmupManager: WarmupManager? = null) {
        /** Warmup looping mode - reports a single iteration, but there is specialized code in */
        class Warmup(warmupManager: WarmupManager) : LoopMode(warmupManager) {
            // always return one iter per measurement as we remeasure warmup after each loop
            override fun getIterations(warmupEstimatedIterationTimeNs: Long): Int = 1
        }

        /** Each repeat of the phase will run a predefined number of iterations */
        class FixedIterations(private val iterations: Int) : LoopMode() {
            override fun getIterations(warmupEstimatedIterationTimeNs: Long): Int = iterations
        }

        class Duration(private val targetRepeatDurationNs: Long) : LoopMode() {
            override fun getIterations(warmupEstimatedIterationTimeNs: Long): Int {
                check(warmupEstimatedIterationTimeNs >= 0) {
                    "Cannot dynamically determine repeat duration, warmup has not run!"
                }
                return (targetRepeatDurationNs / warmupEstimatedIterationTimeNs.coerceAtLeast(1))
                    .toInt()
                    .coerceIn(MIN_TEST_ITERATIONS, MAX_TEST_ITERATIONS)
            }
        }

        abstract fun getIterations(warmupEstimatedIterationTimeNs: Long): Int

        companion object {
            internal const val MAX_TEST_ITERATIONS = 1_000_000
            internal const val MIN_TEST_ITERATIONS = 1
        }
    }

    companion object {
        private val THROTTLE_BACKOFF_S = Arguments.thermalThrottleSleepDurationSeconds

        // static instance ensures there's only one, and we don't leak native memory
        internal val cpuEventCounter: CpuEventCounter by lazy {
            // As this is only ever enabled by experimental arguments, we force enable this
            // permanently once the first benchmark uses it, for local runs only.
            CpuEventCounter.forceEnable()?.let { errorMessage ->
                throw IllegalStateException(errorMessage)
            }
            CpuEventCounter()
        }

        fun dryRunModePhase() =
            MicrobenchmarkPhase(
                label = "Benchmark DryRun Timing",
                measurementCount = 1,
                loopMode = LoopMode.FixedIterations(1),
            )

        fun startupModePhase() =
            MicrobenchmarkPhase(
                label = "Benchmark Startup Timing (experimental)",
                measurementCount = 10,
                loopMode = LoopMode.FixedIterations(1),
            )

        fun warmupPhase(
            warmupManager: WarmupManager,
            collectCpuEventInstructions: Boolean,
        ) =
            MicrobenchmarkPhase(
                label = "Benchmark Warmup",
                measurementCount = 1,
                loopMode = LoopMode.Warmup(warmupManager),
                metrics =
                    if (collectCpuEventInstructions) {
                        arrayOf(
                            TimeCapture(),
                            CpuEventCounterCapture(cpuEventCounter, listOf(Event.Instructions))
                        )
                    } else {
                        arrayOf(TimeCapture())
                    },
                gcBeforePhase = true
            )

        fun timingMeasurementPhase(
            loopMode: LoopMode,
            measurementCount: Int,
            simplifiedTimingOnlyMode: Boolean,
            metrics: Array<MetricCapture>
        ) =
            MicrobenchmarkPhase(
                label = "Benchmark Time",
                measurementCount = measurementCount,
                loopMode = loopMode,
                metrics = metrics,
                thermalThrottleSleepsMax = if (simplifiedTimingOnlyMode) 0 else 2
            )

        fun profiledTimingPhase(
            profiler: Profiler,
            metrics: Array<MetricCapture>,
            loopModeOverride: LoopMode?,
            measurementCountOverride: Int?
        ): MicrobenchmarkPhase {
            val measurementCount =
                measurementCountOverride
                    ?: if (profiler.requiresSingleMeasurementIteration) 1 else 50
            return MicrobenchmarkPhase(
                label = "Benchmark Profiled Time",
                measurementCount = measurementCount,
                loopMode =
                    loopModeOverride
                        ?: if (profiler.requiresSingleMeasurementIteration) {
                            LoopMode.FixedIterations(1)
                        } else {
                            LoopMode.Duration(
                                if (profiler.requiresExtraRuntime) {
                                    BenchmarkState.SAMPLED_PROFILER_DURATION_NS / measurementCount
                                } else {
                                    BenchmarkState.DEFAULT_MEASUREMENT_DURATION_NS
                                }
                            )
                        },
                profiler = profiler,
                metrics = metrics
            )
        }

        fun allocationMeasurementPhase(loopMode: LoopMode) =
            MicrobenchmarkPhase(
                label = "Benchmark Allocations",
                measurementCount = 5,
                loopMode = loopMode,
                metrics = arrayOf(AllocationCountCapture())
            )
    }

    /**
     * Configuration for phase and looping behavior in a microbenchmark.
     *
     * Note that many arguments can override subsequent ones in the list (e.g. dryRunMode=true
     * ignores all subsequent args).
     */
    class Config(
        val dryRunMode: Boolean,
        val startupMode: Boolean,
        val simplifiedTimingOnlyMode: Boolean,
        val profiler: Profiler?,
        val profilerPerfCompareMode: Boolean,
        val warmupCount: Int?,
        val measurementCount: Int?,
        val metrics: Array<MetricCapture>,
    ) {
        constructor(
            microbenchmarkConfig: MicrobenchmarkConfig,
            simplifiedTimingOnlyMode: Boolean
        ) : this(
            dryRunMode = Arguments.dryRunMode,
            startupMode = Arguments.startupMode,
            profiler = microbenchmarkConfig.profiler?.profiler ?: Arguments.profiler,
            profilerPerfCompareMode = Arguments.profilerPerfCompareEnable,
            warmupCount = microbenchmarkConfig.warmupCount,
            measurementCount = Arguments.iterations ?: microbenchmarkConfig.measurementCount,
            simplifiedTimingOnlyMode = simplifiedTimingOnlyMode,
            metrics = microbenchmarkConfig.metrics.toTypedArray()
        )

        val warmupManager = WarmupManager(overrideCount = warmupCount)

        init {
            require(warmupCount == null || warmupCount > 0) {
                "warmupCount ($warmupCount) must null or positive"
            }
            require(measurementCount == null || measurementCount > 0) {
                "measurementCount ($measurementCount) must be null or positive"
            }
        }

        fun generatePhases(): List<MicrobenchmarkPhase> {
            return if (dryRunMode) {
                listOf(dryRunModePhase())
            } else
                if (startupMode) {
                        listOf(startupModePhase())
                    } else {
                        val timingMeasurementCount = measurementCount ?: 50

                        val profiler = if (simplifiedTimingOnlyMode) null else profiler
                        // note that it's currently important that allocation runs for the same
                        // target
                        // duration as timing, since we only report a single value for
                        // "repeatIterations" in the output JSON. If we ever want to avoid loopMode
                        // sharing between these phases, we should update that JSON representation.
                        val loopMode =
                            if (profilerPerfCompareMode) {
                                // single fixed iteration as a compromise choice that can be matched
                                // between
                                // measurement and profiler, and not produce overwhelming method
                                // tracing capture
                                // durations/file sizes
                                LoopMode.FixedIterations(1)
                            } else {
                                LoopMode.Duration(BenchmarkState.DEFAULT_MEASUREMENT_DURATION_NS)
                            }
                        listOfNotNull(
                            warmupPhase(
                                warmupManager = warmupManager,
                                // Collect the instructions metric to ensure that behavior and
                                // timing aren't significantly skewed between warmup and timing
                                // phases. For example, if only timing phase has a complex impl of
                                // pause/resume, then behavior changes drastically, and the
                                // warmupManager will estimate a far faster impl of
                                // `measureRepeated { runWithMeasurementDisabled }`
                                collectCpuEventInstructions =
                                    metrics.any {
                                        it is CpuEventCounterCapture && it.names.isNotEmpty()
                                    }
                            ),
                            // Regular timing phase
                            timingMeasurementPhase(
                                measurementCount = timingMeasurementCount,
                                loopMode = loopMode,
                                metrics = metrics,
                                simplifiedTimingOnlyMode = simplifiedTimingOnlyMode
                            ),
                            if (simplifiedTimingOnlyMode || profiler == null) {
                                null
                            } else {
                                if (profilerPerfCompareMode) {
                                    // benchmark the profiler, matching the timing phases for fair
                                    // compare
                                    profiledTimingPhase(
                                        profiler = profiler,
                                        metrics = arrayOf(TimeCapture("profilerTimeNs")),
                                        loopModeOverride = loopMode,
                                        measurementCountOverride = timingMeasurementCount
                                    )
                                } else {
                                    // standard profiling
                                    profiledTimingPhase(
                                        profiler,
                                        metrics = emptyArray(),
                                        loopModeOverride = null,
                                        measurementCountOverride = null
                                    )
                                }
                            },
                            if (simplifiedTimingOnlyMode) {
                                null // skip allocations
                            } else {
                                allocationMeasurementPhase(loopMode)
                            }
                        )
                    }
                    .also {
                        if (simplifiedTimingOnlyMode) {
                            // can't use thermal throttle checks with simplifiedTimingOnlyMode,
                            // since we're already checking for throttling
                            check(it.all { phase -> phase.thermalThrottleSleepsMax == 0 }) {
                                "Thermal throttle check banned within simplifiedTimingOnlyMode"
                            }
                        }
                    }
        }
    }
}
