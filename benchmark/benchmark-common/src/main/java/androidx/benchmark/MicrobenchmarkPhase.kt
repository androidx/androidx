package androidx.benchmark

import android.annotation.SuppressLint
import android.util.Log
import java.util.concurrent.TimeUnit

internal class MicrobenchmarkPhase(
    val label: String,
    val measurementCount: Int,
    val loopMode: LoopMode,

    val metrics: Array<MetricCapture> = arrayOf(TimeCapture()),

    val profiler: Profiler? = null,
    val gcBeforePhase: Boolean = false,
    val ignorePauseEvent: Boolean = false,
    val thermalThrottleSleepsMax: Int = 0,
) {
    val metricsContainer = MetricsContainer(metrics, measurementCount)
    private var thermalThrottleSleepsRemaining = thermalThrottleSleepsMax
    var thermalThrottleSleepSeconds = 0L

    init {
        check(loopMode.warmupManager == null || metricsContainer.names.size == 1) {
            "If warmup is enabled, must only capture one metric," +
                " as WarmupManager only one value per repeat"
        }
    }

    /**
     * @return If true, finishing the phase was successful, otherwise must be retried
     */
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
    private fun sleepIfThermalThrottled(): Boolean = when {
        ThrottleDetector.isDeviceThermalThrottled() -> {
            Log.d(
                BenchmarkState.TAG,
                "THERMAL THROTTLE DETECTED, SLEEPING FOR $THROTTLE_BACKOFF_S SECONDS"
            )
            val startTimeNs = System.nanoTime()
            userspaceTrace("Sleep due to Thermal Throttle") {
                Thread.sleep(TimeUnit.SECONDS.toMillis(THROTTLE_BACKOFF_S))
            }
            val sleepTimeNs = System.nanoTime() - startTimeNs
            thermalThrottleSleepSeconds += TimeUnit.NANOSECONDS.toSeconds(sleepTimeNs)
            true
        }
        else -> false
    }

    internal sealed class LoopMode(
        val warmupManager: WarmupManager? = null
    ) {
        /**
         * Warmup looping mode - reports a single iteration, but there is specialized code in
         */
        class Warmup(warmupManager: WarmupManager) : LoopMode(warmupManager) {
            // always return one iter per measurement as we remeasure warmup after each loop
            override fun getIterations(warmupEstimatedIterationTimeNs: Long): Int = 1
        }

        /**
         * Each repeat of the phase will run a predefined number of iterations
         */
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

        fun dryRunModePhase() = MicrobenchmarkPhase(
            label = "Benchmark DryRun Timing",
            measurementCount = 1,
            loopMode = LoopMode.FixedIterations(1),
        )

        fun startupModePhase() = MicrobenchmarkPhase(
            label = "Benchmark Startup Timing (experimental)",
            measurementCount = 10,
            loopMode = LoopMode.FixedIterations(1),
        )

        fun warmupPhase(
            warmupManager: WarmupManager
        ) = MicrobenchmarkPhase(
            label = "Benchmark Warmup",
            measurementCount = 1,
            loopMode = LoopMode.Warmup(warmupManager),
            gcBeforePhase = true,
            ignorePauseEvent = true
        )

        fun timingMeasurementPhase(
            loopMode: LoopMode,
            measurementCount: Int,
            simplifiedTimingOnlyMode: Boolean
        ) = MicrobenchmarkPhase(
            label = "Benchmark Time",
            measurementCount = measurementCount,
            loopMode = loopMode,
            thermalThrottleSleepsMax = if (simplifiedTimingOnlyMode) 0 else 2
        )

        fun profiledTimingPhase(
            profiler: Profiler
        ): MicrobenchmarkPhase {
            val measurementCount = if (profiler.requiresSingleMeasurementIteration) 1 else 50
            return MicrobenchmarkPhase(
                label = "Benchmark Profiled Time",
                measurementCount = measurementCount,
                loopMode = if (profiler.requiresSingleMeasurementIteration) {
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
                profiler = profiler
            )
        }

        fun allocationMeasurementPhase(loopMode: LoopMode) = MicrobenchmarkPhase(
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
        val warmupCount: Int?,
        val measurementCount: Int?,
    ) {
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
            } else if (startupMode) {
                listOf(startupModePhase())
            } else {
                val profiler = if (simplifiedTimingOnlyMode) null else profiler
                // note that it's currently important that allocation runs for the same target
                // duration as timing, since we only report a single value for
                // "repeatIterations" in the output JSON. If we ever want to avoid loopMode
                // sharing between these phases, we should update that JSON representation.
                val loopMode = LoopMode.Duration(BenchmarkState.DEFAULT_MEASUREMENT_DURATION_NS)
                listOfNotNull(
                    warmupPhase(warmupManager),
                    // Regular timing phase
                    timingMeasurementPhase(
                        measurementCount = measurementCount ?: 50,
                        loopMode = loopMode,
                        simplifiedTimingOnlyMode = simplifiedTimingOnlyMode
                    ),
                    if (simplifiedTimingOnlyMode || profiler == null) {
                        null
                    } else {
                        profiledTimingPhase(profiler)
                    },
                    if (simplifiedTimingOnlyMode) {
                        null // skip allocations
                    } else {
                        allocationMeasurementPhase(loopMode)
                    }
                )
            }.also {
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
