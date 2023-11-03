/*
 * Copyright 2021 The Android Open Source Project
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

package androidx.room.compiler.processing.profiling

import kotlin.time.Duration
import kotlin.time.Duration.Companion.nanoseconds
import kotlin.time.DurationUnit
import kotlin.time.ExperimentalTime
import org.junit.AssumptionViolatedException
import org.junit.rules.TestRule
import org.junit.runner.Description
import org.junit.runners.model.Statement

/**
 * Helper rule to run profiling tests.
 *
 * These tests are run along with `scripts/profile.sh` to build an async profile
 * output based on a test scenario.
 *
 * If this rule is applied outside a profiling session, it will ignore the test.
 */
@OptIn(ExperimentalTime::class)
class ProfileRule : TestRule {
    /**
     * Runs the given block, repeatedly :).
     *
     * It will first run it [warmUps] times with a fake tracer. Then it will run
     * the block [repeat] times with a real profiling scope that will be captured by
     * profile.sh.
     */
    fun runRepeated(
        warmUps: Int,
        repeat: Int,
        block: (ProfileScope) -> Unit
    ) {
        val warmUpScope = WarmUpProfileScope()
        repeat(warmUps) {
            block(warmUpScope)
        }
        val realProfileScope = RealProfileScope()
        repeat(repeat) {
            block(realProfileScope)
        }
        println(buildReport(realProfileScope.measurements).toString())
    }

    private fun buildReport(measurements: List<Duration>): Stats {
        check(measurements.isNotEmpty())
        val min = measurements.minByOrNull { it.toLong(DurationUnit.NANOSECONDS) }!!
        val max = measurements.maxByOrNull { it.toLong(DurationUnit.NANOSECONDS) }!!
        val avg = measurements.fold(Duration.ZERO) { acc, next -> acc + next } / measurements.size
        val mean = if (measurements.size % 2 == 0) {
            (measurements[measurements.size / 2] + measurements[measurements.size / 2 - 1]) / 2
        } else {
            measurements[measurements.size / 2]
        }
        return Stats(
            allMeasurements = measurements,
            min = min,
            max = max,
            avg = avg,
            mean = mean
        )
    }

    override fun apply(base: Statement, description: Description): Statement {
        return object : Statement() {
            override fun evaluate() {
                assumeProfiling()
                base.evaluate()
            }
        }
    }

    private fun assumeProfiling() {
        if (!isProfilingEnabled) {
            throw AssumptionViolatedException("No reason to run while not profiling")
        }
    }

    interface ProfileScope {
        /**
         * Utility function for tests to mark certain areas of their code for tracking.
         *
         * This method is explicitly not marked as inline to ensure it shows up in the
         * profiling output.
         */
        fun trace(block: () -> Unit)
    }

    private class RealProfileScope : ProfileScope {
        private val _measurements = mutableListOf<Duration>()
        val measurements: List<Duration>
            get() = _measurements
        @OptIn(ExperimentalTime::class)
        override fun trace(block: () -> Unit) {
            // this doesn't do anything but profile.sh trace profiler checks
            // this class while filtering stacktraces
            val start = now()
            try {
                block()
            } finally {
                val end = now()
                _measurements.add(end - start)
            }
        }

        private fun now() = System.nanoTime().nanoseconds
    }

    private class WarmUpProfileScope : ProfileScope {
        override fun trace(block: () -> Unit) {
            block()
        }
    }

    data class Stats(
        val min: Duration,
        val max: Duration,
        val avg: Duration,
        val mean: Duration,
        val allMeasurements: List<Duration>,
    )

    companion object {
        val isProfilingEnabled by lazy {
            // set by profile.sh
            System.getenv("ANDROIDX_ROOM_ENABLE_PROFILE_TESTS") != null
        }
    }
}
