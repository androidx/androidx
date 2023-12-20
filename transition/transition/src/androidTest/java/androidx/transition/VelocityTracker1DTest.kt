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
package androidx.transition

import androidx.test.filters.SmallTest
import com.google.common.truth.Truth
import com.google.common.truth.Truth.assertThat
import kotlin.math.abs
import org.junit.Test

// Velocities between (1-Tolerance)*RV and (1+Tolerance)*RV are accepted
// where RV is the "Real Velocity"
private const val Tolerance: Float = 0.2f

@SmallTest
class VelocityTracker1DTest : BaseTest() {
    @Test
    fun twoPoints_nonDifferentialValues() {
        checkTestCase(
            VelocityTrackingTestCase(
                dataPoints = listOf(1 to 5f, 2 to 15f),
                expectedVelocity = 10000f
            )
        )
    }

    @Test
    fun threePoints_pointerStoppedMoving_nonDifferentialValues() {
        checkTestCase(
            VelocityTrackingTestCase(
                dataPoints = listOf(
                    25 to 25f,
                    50 to 50f,
                    100 to 100f,
                ),
                // Expect 0 velocities, as the pointer will be considered to have stopped moving,
                // due to the (100-50)=40ms gap from the last data point (i.e. it's effectively
                // a data set with only 1 data point).
                expectedVelocity = 0f,
            )
        )
    }

    /** Impulse strategy specific test cases. */
    @Test
    fun threePoints_zeroVelocity_nonDifferentialValues() {
        checkTestCase(
            VelocityTrackingTestCase(
                dataPoints = listOf(
                    0 to 273f,
                    1 to 273f,
                    2 to 273f,
                ),
                expectedVelocity = 0f
            ),
        )
    }

    @Test
    fun resetTracking_defaultConstructor() {
        // Fixed velocity at 5 points per 10 milliseconds
        val tracker = VelocityTracker1D()
        tracker.addDataPoint(0, 0f)
        tracker.addDataPoint(10, 5f)
        tracker.addDataPoint(20, 10f)
        tracker.addDataPoint(30, 15f)
        tracker.addDataPoint(40, 30f)

        tracker.resetTracking()

        assertThat(tracker.calculateVelocity()).isZero()
    }

    @Test
    fun resetTracking_nonDifferentialValues_impulse() {
        // Fixed velocity at 5 points per 10 milliseconds
        val tracker = VelocityTracker1D()
        tracker.addDataPoint(0, 0f)
        tracker.addDataPoint(10, 5f)
        tracker.addDataPoint(20, 10f)
        tracker.addDataPoint(30, 15f)
        tracker.addDataPoint(40, 30f)

        tracker.resetTracking()

        assertThat(tracker.calculateVelocity()).isZero()
    }

    @Test
    fun linearMotion_positiveVelocity_positiveDataPoints_nonDifferentialValues() {
        // Fixed velocity at 5 points per 10 milliseconds
        checkTestCase(
            VelocityTrackingTestCase(
                dataPoints = listOf(
                    0 to 0f,
                    10 to 5f,
                    20 to 10f,
                    30 to 15f,
                ),
                expectedVelocity = 500f,
            )
        )
    }

    @Test
    fun linearMotion_positiveVelocity_negativeDataPoints_nonDifferentialValues() {
        // Fixed velocity at 5 points per 10 milliseconds
        checkTestCase(
            VelocityTrackingTestCase(
                dataPoints = listOf(
                    0 to -20f,
                    10 to -15f,
                    20 to -10f,
                ),
                expectedVelocity = 500f,
            )
        )
    }

    @Test
    fun linearMotion_positiveVelocity_mixedSignDataPoints_nonDifferentialValues() {
        // Fixed velocity at 5 points per 10 milliseconds
        checkTestCase(
            VelocityTrackingTestCase(
                dataPoints = listOf(
                    0 to -5f,
                    10 to 0f,
                    20 to 5f,
                ),
                expectedVelocity = 500f,
            )
        )
    }

    @Test
    fun linearMotion_negativeVelocity_negativeDataPoints_nonDifferentialValues() {
        // Fixed velocity at 5 points per 10 milliseconds
        checkTestCase(
            VelocityTrackingTestCase(
                dataPoints = listOf(
                    0 to 0f,
                    10 to -5f,
                    20 to -10f,
                ),
                expectedVelocity = -500f,
            )
        )
    }

    @Test
    fun linearMotion_negativeVelocity_postiveDataPoints_nonDifferentialValues() {
        // Fixed velocity at 5 points per 10 milliseconds
        checkTestCase(
            VelocityTrackingTestCase(
                dataPoints = listOf(
                    0 to 20f,
                    10 to 15f,
                    20 to 10f,
                ),
                expectedVelocity = -500f,
            )
        )
    }

    @Test
    fun linearMotion_negativeVelocity_mixedSignDataPoints_nonDifferentialValues() {
        // Fixed velocity at 5 points per 10 milliseconds
        checkTestCase(
            VelocityTrackingTestCase(
                dataPoints = listOf(
                    0 to 5f,
                    10 to 0f,
                    20 to -5f,
                ),
                expectedVelocity = -500f,
            )
        )
    }

    @Test
    fun linearHalfMotion() {
        // Stay still for 50 ms, and then move 100 points in the final 50 ms.
        // The final line is sloped at 2 units/ms.
        // This can be visualized as 2 lines: flat line (50ms), and line with slope of 2 units/ms.
        checkTestCase(
            VelocityTrackingTestCase(
                dataPoints = listOf(
                    0 to 0f,
                    10 to 0f,
                    20 to 0f,
                    30 to 0f,
                    40 to 0f,
                    50 to 0f,
                    60 to 20f,
                    70 to 40f,
                    80 to 60f,
                    90 to 80f,
                    100 to 100f,
                ),
                expectedVelocity = 2000f
            ),
        )
    }

    @Test
    fun linearHalfMotionSampled() {
        // Linear half motion, but sampled much less frequently. The resulting velocity is higher
        // than the previous test, because the path looks significantly different now if you
        // were to just plot these points.
        checkTestCase(
            VelocityTrackingTestCase(
                dataPoints = listOf(
                    0 to 0f,
                    30 to 0f,
                    40 to 0f,
                    70 to 40f,
                    100 to 100f,
                ),
                expectedVelocity = 2018.2f
            )
        )
    }

    @Test
    fun linearMotionFollowedByFlatLine() {
        // Fixed velocity at first, but flat line afterwards.
        checkTestCase(
            VelocityTrackingTestCase(
                dataPoints = listOf(
                    0 to 0f,
                    10 to 10f,
                    20 to 20f,
                    30 to 30f,
                    40 to 40f,
                    50 to 50f,
                    60 to 50f,
                    70 to 50f,
                    80 to 50f,
                    90 to 50f,
                    100 to 50f,
                ),
                expectedVelocity = 1000f
            )
        )
    }

    @Test
    fun linearMotionFollowedByFlatLineWithoutIntermediatePoints() {
        // Fixed velocity at first, but flat line afterwards
        checkTestCase(
            VelocityTrackingTestCase(
                dataPoints = listOf(
                    0 to 0f,
                    50 to 50f,
                    100 to 50f,
                ),
                expectedVelocity = 0f
            ),
        )
    }

    @Test
    fun swordfishFlingDown_xValues() {
        checkTestCase(
            VelocityTrackingTestCase(
                dataPoints = listOf(
                    0 to 271f,
                    16 to 269.786346f,
                    35 to 267.983063f,
                    52 to 262.638397f,
                    68 to 266.138824f,
                    85 to 274.79245f,
                    96 to 274.79245f,
                ),
                expectedVelocity = 623.57f
            )
        )
    }

    @Test
    fun swordfishFlingDown_yValues() {
        checkTestCase(
            VelocityTrackingTestCase(
                dataPoints = listOf(
                    0 to 96f,
                    16 to 106.922775f,
                    35 to 156.660034f,
                    52 to 220.339081f,
                    68 to 331.581116f,
                    85 to 428.113159f,
                    96 to 428.113159f,
                ),
                expectedVelocity = 5970.73f
            )
        )
    }

    @Test
    fun sailfishFlingUpSlow_xValues() {
        checkTestCase(
            VelocityTrackingTestCase(
                dataPoints = listOf(
                    235089067 to 528.0f,
                    235089084 to 527.0f,
                    235089093 to 527.0f,
                    235089095 to 527.0f,
                    235089101 to 527.0f,
                    235089110 to 528.0f,
                    235089112 to 528.25f,
                    235089118 to 531.0f,
                    235089126 to 535.0f,
                    235089129 to 536.33f,
                    235089135 to 540.0f,
                    235089144 to 546.0f,
                    235089146 to 547.21f,
                    235089152 to 553.0f,
                    235089160 to 559.0f,
                    235089162 to 560.66f,
                ),
                expectedVelocity = 764.34f,
            )
        )
    }

    @Test
    fun sailfishFlingUpSlow_yValues() {
        checkTestCase(
            VelocityTrackingTestCase(
                dataPoints = listOf(
                    235089067 to 983.0f,
                    235089084 to 981.0f,
                    235089093 to 977.0f,
                    235089095 to 975.93f,
                    235089101 to 970.0f,
                    235089110 to 960.0f,
                    235089112 to 957.51f,
                    235089118 to 946.0f,
                    235089126 to 931.0f,
                    235089129 to 926.02f,
                    235089135 to 914.0f,
                    235089144 to 896.0f,
                    235089146 to 892.36f,
                    235089152 to 877.0f,
                    235089160 to 851.0f,
                    235089162 to 843.82f,
                ),
                expectedVelocity = -3604.82f,
            )
        )
    }

    @Test
    fun sailfishFlingUpFast_xValues() {
        // Some "repeated" data points are removed, since the conversion from ns to ms made some
        // data ponits "repeated"
        checkTestCase(
            VelocityTrackingTestCase(
                dataPoints = listOf(
                    920922 to 561.0f,
                    920930 to 559.0f,
                    920938 to 559.0f,
                    920947 to 562.91f,
                    920955 to 577.0f,
                    920963 to 596.87f,
                    920972 to 631.0f,
                    920980 to 671.31f,
                    920989 to 715.0f,
                ),
                expectedVelocity = 5670.32f,
            )
        )
    }

    @Test
    fun sailfishFlingUpFast_yValues() {
        // Some "repeated" data points are removed, since the conversion from ns to ms made some
        // data ponits "repeated"
        checkTestCase(
            VelocityTrackingTestCase(
                dataPoints = listOf(
                    920922 to 1412.0f,
                    920930 to 1377.0f,
                    920938 to 1371.0f,
                    920947 to 1342.68f,
                    920955 to 1272.0f,
                    920963 to 1190.54f,
                    920972 to 1093.0f,
                    920980 to 994.68f,
                    920989 to 903.0f,
                ),
                expectedVelocity = -13021.10f,
            )
        )
    }

    private fun checkTestCase(testCase: VelocityTrackingTestCase) {
        val expectedVelocity = testCase.expectedVelocity
        val tracker = VelocityTracker1D()
        testCase.dataPoints.forEach {
            tracker.addDataPoint(it.first.toLong(), it.second)
        }

        Truth.assertWithMessage(
            "Wrong velocity for data points: ${testCase.dataPoints}" +
                "\nExpected velocity: {$expectedVelocity}"
        )
            .that(tracker.calculateVelocity())
            .isWithin(abs(expectedVelocity) * Tolerance)
            .of(expectedVelocity)
    }
}

/** Holds configs for a velocity tracking test case, for convenience. */
private data class VelocityTrackingTestCase(
    val dataPoints: List<Pair<Int, Float>>,
    val expectedVelocity: Float
)
