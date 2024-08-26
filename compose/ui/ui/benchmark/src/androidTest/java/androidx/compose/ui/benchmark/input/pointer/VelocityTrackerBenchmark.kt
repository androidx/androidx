/*
 * Copyright 2023 The Android Open Source Project
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
package androidx.compose.ui.benchmark.input.pointer

import androidx.compose.testutils.benchmark.ComposeBenchmarkRule
import androidx.compose.ui.input.pointer.util.VelocityTracker1D
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.filters.LargeTest
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

/** Helper class to create [MotionDataPoint]s deterministically for velocity benchmark testing. */
private class DataPointCreator(private val dataPointDelta: Float = DefaultDataPointDelta) {
    private var currentTimeMillis = StartTimeMillis
    private var dataPointValue = DataPointStartValue

    fun createDataPoint(): MotionDataPoint {
        val dataPoint = MotionDataPoint(currentTimeMillis, dataPointValue)
        dataPointValue += dataPointDelta
        currentTimeMillis += TimeDeltaMillis
        return dataPoint
    }

    companion object {
        /** Arbitrarily chosen. */
        private const val StartTimeMillis = 0L
        /** Small enough that it won't reset velocity tracking. */
        private const val TimeDeltaMillis = 2L
        /** Arbitrarily chosen. Not 0, so it won't remain 0 if delta is 0. */
        private const val DataPointStartValue = 100f
        /** Arbitrarily chosen. */
        private const val DefaultDataPointDelta = 3f
    }
}

private data class MotionDataPoint(val timeMillis: Long, val motionValue: Float)

@LargeTest
@RunWith(AndroidJUnit4::class)
class VelocityTrackerBenchmarkTest {
    @get:Rule val benchmarkRule = ComposeBenchmarkRule()

    @Test
    fun addMovement_differential() {
        testAddDataPoint(differential = true)
    }

    @Test
    fun addMovement_nonDifferential() {
        testAddDataPoint(differential = false)
    }

    @Test
    fun calculateVelocity_differential() {
        testCalculateVelocity(differential = true)
    }

    @Test
    fun calculateVelocity_nonDifferential() {
        testCalculateVelocity(differential = false)
    }

    private fun testAddDataPoint(differential: Boolean) {
        benchmarkRule.measureRepeated {
            val velocityTracker = VelocityTracker1D(differential)
            val dataPointCreator = DataPointCreator()

            for (i in 0 until TestNumDataPoints) {
                val dataPoint = dataPointCreator.createDataPoint()
                velocityTracker.addDataPoint(dataPoint.timeMillis, dataPoint.motionValue)
            }
        }
    }

    private fun testCalculateVelocity(differential: Boolean) {
        val velocityTracker = VelocityTracker1D(differential)
        val dataPointCreator = DataPointCreator()

        for (i in 0 until TestNumDataPoints) {
            val dataPoint = dataPointCreator.createDataPoint()
            velocityTracker.addDataPoint(dataPoint.timeMillis, dataPoint.motionValue)
        }

        benchmarkRule.measureRepeated { assertTrue(velocityTracker.calculateVelocity() != 0f) }
    }

    companion object {
        private const val TestNumDataPoints = 100
    }
}
