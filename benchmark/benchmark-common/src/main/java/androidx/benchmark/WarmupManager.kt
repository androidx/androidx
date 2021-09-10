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

import android.util.Log
import java.util.concurrent.TimeUnit

/**
 * Used to detect when a benchmark has warmed up, given time taken for each iteration.
 *
 * Uses emperically determined constants, primarily looking for the convergence of two
 * exponential moving averages.
 *
 * Tuned to do minimal amount of intrusive work in onNextIteration to avoid polluting the benchmark.
 */
internal class WarmupManager {
    private var fastMovingAvg: Float = 0f
    private var slowMovingAvg: Float = 0f
    private var similarIterationCount: Int = 0

    val estimatedIterationTimeNs: Float get() = fastMovingAvg

    var iteration = 0
        private set

    var totalDurationNs: Long = 0
        private set

    /**
     * Pass the just-run iteration timing, and return whether the warmup has completed.
     *
     * NOTE: it is critical to do a minimum amount of work and memory access in this method, to
     * avoid polluting the benchmark's memory access patterns. This is why we chose exponential
     * moving averages, and why we only log once at the end.
     *
     * @param durationNs Duration of the next iteration.
     * @return True if the warmup has completed, false otherwise.
     */
    fun onNextIteration(durationNs: Long): Boolean {
        iteration++
        totalDurationNs += durationNs

        if (iteration == 1) {
            fastMovingAvg = durationNs.toFloat()
            slowMovingAvg = durationNs.toFloat()
            return false
        }

        fastMovingAvg = FAST_RATIO * durationNs + (1 - FAST_RATIO) * fastMovingAvg
        slowMovingAvg = SLOW_RATIO * durationNs + (1 - SLOW_RATIO) * slowMovingAvg

        // If fast moving avg is close to slow, the benchmark is stabilizing
        val ratio = fastMovingAvg / slowMovingAvg
        if (ratio < 1 + THRESHOLD && ratio > 1 - THRESHOLD) {
            similarIterationCount++
        } else {
            similarIterationCount = 0
        }

        if (iteration >= MIN_ITERATIONS && totalDurationNs >= MIN_DURATION_NS) {
            if (similarIterationCount > MIN_SIMILAR_ITERATIONS ||
                totalDurationNs >= MAX_DURATION_NS
            ) {
                // benchmark has stabilized, or we're out of time
                return true
            }
        }
        return false
    }

    fun logInfo() {
        if (iteration > 0) {
            Log.d(
                BenchmarkState.TAG,
                "Warmup: t=%.3f, iter=%d, fastAvg=%3.0f, slowAvg=%3.0f"
                    .format(
                        totalDurationNs / 1e9,
                        iteration,
                        fastMovingAvg,
                        slowMovingAvg
                    )
            )
        }
    }

    companion object {
        val MIN_DURATION_NS = TimeUnit.MILLISECONDS.toNanos(250)
        val MAX_DURATION_NS = TimeUnit.SECONDS.toNanos(8)
        const val MIN_ITERATIONS = 30
        private const val MIN_SIMILAR_ITERATIONS = 40

        private const val FAST_RATIO = 0.1f
        private const val SLOW_RATIO = 0.005f
        private const val THRESHOLD = 0.04f
    }
}
