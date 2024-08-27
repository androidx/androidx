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

package androidx.compose.animation.core

import androidx.compose.animation.core.ArcMode.Companion.ArcAbove
import androidx.compose.animation.core.ArcMode.Companion.ArcBelow
import androidx.compose.animation.core.ArcMode.Companion.ArcLinear
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith
import org.junit.runners.JUnit4

/** Mostly tests some mathematical assumptions about arcs. */
@Suppress("JoinDeclarationAndAssignment") // Looks kinda messy
@OptIn(ExperimentalAnimationSpecApi::class)
@RunWith(JUnit4::class)
class ArcAnimationTest {
    // Animation parameters used in all tests
    private val timeMillis = 1000
    private val initialValue = 0f
    private val targetValue = 300f

    private val error = 0.01f

    @Test
    fun test2DInterpolation_withArcAbove() {
        val animation = createArcAnimation<AnimationVector2D>(ArcAbove)
        var arcValue: AnimationVector2D
        var linearValue: AnimationVector2D

        // Test values at 25%, 50%, 75%
        // For arc above Y will always be lower but X will be higher
        arcValue = animation.valueAt(0.25f)
        linearValue = linearValueAt(0.25f)
        assertTrue(arcValue[0] > linearValue[0])
        assertTrue(arcValue[1] < linearValue[1])

        arcValue = animation.valueAt(0.5f)
        linearValue = linearValueAt(0.5f)
        assertTrue(arcValue[0] > linearValue[0])
        assertTrue(arcValue[1] < linearValue[1])

        arcValue = animation.valueAt(0.75f)
        linearValue = linearValueAt(0.75f)
        assertTrue(arcValue[0] > linearValue[0])
        assertTrue(arcValue[1] < linearValue[1])

        // Test that x at 25% is the complement of y at 75%
        assertEquals(
            targetValue - animation.valueAt(0.25f)[0],
            animation.valueAt(0.75f)[1],
            error // Bound to have some minor differences :)
        )

        var arcVelocity: AnimationVector2D
        // Test that velocity at 50% is equal on both components
        arcVelocity = animation.velocityAt(0.5f)
        assertEquals(arcVelocity[0], arcVelocity[1], error)

        // Test that for velocity at 0% only the X component is non-zero
        arcVelocity = animation.velocityAt(0.0f)
        assertEquals(0f, arcVelocity[1], error)
        assertTrue(arcVelocity[0] > error)

        // Test that for velocity at 100% only the X component in non-zero
        arcVelocity = animation.velocityAt(1f)
        assertEquals(0f, arcVelocity[0], error)
        assertTrue(arcVelocity[1] > error)
    }

    @Test
    fun test2DInterpolation_withArcBelow() {
        val animation = createArcAnimation<AnimationVector2D>(ArcBelow)
        var arcValue: AnimationVector2D
        var linearValue: AnimationVector2D

        // Test values at 25%, 50%, 75%
        // For arc below Y will always be higher but X will be lower
        arcValue = animation.valueAt(0.25f)
        linearValue = linearValueAt(0.25f)
        assertTrue(arcValue[0] < linearValue[0])
        assertTrue(arcValue[1] > linearValue[1])

        arcValue = animation.valueAt(0.5f)
        linearValue = linearValueAt(0.5f)
        assertTrue(arcValue[0] < linearValue[0])
        assertTrue(arcValue[1] > linearValue[1])

        arcValue = animation.valueAt(0.75f)
        linearValue = linearValueAt(0.75f)
        assertTrue(arcValue[0] < linearValue[0])
        assertTrue(arcValue[1] > linearValue[1])

        // Test that Y at 25% is the complement of X at 75%
        assertEquals(
            targetValue - animation.valueAt(0.25f)[1],
            animation.valueAt(0.75f)[0],
            error // Bound to have some minor differences :)
        )

        var arcVelocity: AnimationVector2D
        // Test that velocity at 50% is equal on both components
        arcVelocity = animation.velocityAt(0.5f)
        assertEquals(arcVelocity[0], arcVelocity[1], error)

        // Test that for velocity at 0% only the Y component is non-zero
        arcVelocity = animation.velocityAt(0.0f)
        assertEquals(0f, arcVelocity[0], error)
        assertTrue(arcVelocity[1] > error)

        // Test that for velocity at 100% only the Y component in non-zero
        arcVelocity = animation.velocityAt(1f)
        assertEquals(0f, arcVelocity[1], error)
        assertTrue(arcVelocity[0] > error)
    }

    @Test
    fun test2DInterpolation_withLinearArc() {
        val animation = createArcAnimation<AnimationVector2D>(ArcLinear)
        var arcValue: AnimationVector2D
        var linearValue: AnimationVector2D

        // Test values at 25%, 50%, 75% should be exactly the same as a linear interpolation
        arcValue = animation.valueAt(0.25f)
        linearValue = linearValueAt(0.25f)
        assertEquals(linearValue, arcValue)

        arcValue = animation.valueAt(0.5f)
        linearValue = linearValueAt(0.5f)
        assertEquals(linearValue, arcValue)

        arcValue = animation.valueAt(0.75f)
        linearValue = linearValueAt(0.75f)
        assertEquals(linearValue, arcValue)

        var arcVelocity: AnimationVector2D
        arcVelocity = animation.velocityAt(0.25f)
        assertEquals(0f, arcVelocity[0] - arcVelocity[1], error)

        arcVelocity = animation.velocityAt(0.5f)
        assertEquals(0f, arcVelocity[0] - arcVelocity[1], error)

        arcVelocity = animation.velocityAt(0.75f)
        assertEquals(0f, arcVelocity[0] - arcVelocity[1], error)
    }

    @Test
    fun test2DInterpolation_withEasing() {
        val animation = createArcAnimation<AnimationVector2D>(ArcLinear)
        val easedAnimation = createArcAnimation<AnimationVector2D>(ArcLinear, FastOutSlowInEasing)

        var arcValue: AnimationVector2D
        var easedArcValue: AnimationVector2D

        // At 15% of time, the eased animation will lag behind
        arcValue = animation.valueAt(0.15f)
        easedArcValue = easedAnimation.valueAt(0.15f)
        assertTrue(arcValue[0] > easedArcValue[0])
        assertTrue(arcValue[1] > easedArcValue[1])

        // At 26% of time, both animations will be around the same value
        arcValue = animation.valueAt(0.26f)
        easedArcValue = easedAnimation.valueAt(0.26f)
        // Bigger error here, but still within 1% of the target value
        assertEquals(arcValue[0], easedArcValue[0], 1f)
        assertEquals(arcValue[1], easedArcValue[1], 1f)

        // At 50% of time, the eased animation should lead ahead
        arcValue = animation.valueAt(0.5f)
        easedArcValue = easedAnimation.valueAt(0.5f)
        assertTrue(arcValue[0] < easedArcValue[0])
        assertTrue(arcValue[1] < easedArcValue[1])
    }

    @Test
    fun test1DInterpolation_isAlwaysLinear() {
        // TODO: This behavior might change, to be a forced Arc by repeating the same value on a
        //  fake second dimension
        fun testArcMode(arcMode: ArcMode) {
            val animation = createArcAnimation<AnimationVector1D>(arcMode)
            var arcValue: AnimationVector1D

            arcValue = animation.valueAt(0.25f)
            assertEquals(arcValue, linearValueAt<AnimationVector1D>(0.25f))

            arcValue = animation.valueAt(0.5f)
            assertEquals(arcValue, linearValueAt<AnimationVector1D>(0.5f))

            arcValue = animation.valueAt(0.75f)
            assertEquals(arcValue, linearValueAt<AnimationVector1D>(0.75f))
        }

        testArcMode(ArcAbove)
        testArcMode(ArcBelow)
        testArcMode(ArcLinear)
    }

    @Test
    fun test3DInterpolation_firstPairAsArc() {
        val animation = createArcAnimation<AnimationVector3D>(ArcAbove)
        var arcValue: AnimationVector3D
        var linearValue: AnimationVector3D

        // TODO: Test the 3rd dimension, not as important since we don't have any 3-dimensional
        //  values out of the box. Currently, this is the same as `test2DInterpolation_withArcAbove`

        // Test values at 25%, 50%, 75%
        // For arc above Y will always be lower but X will be higher
        arcValue = animation.valueAt(0.25f)
        linearValue = linearValueAt(0.25f)
        assertTrue(arcValue[0] > linearValue[0])
        assertTrue(arcValue[1] < linearValue[1])

        arcValue = animation.valueAt(0.5f)
        linearValue = linearValueAt(0.5f)
        assertTrue(arcValue[0] > linearValue[0])
        assertTrue(arcValue[1] < linearValue[1])

        arcValue = animation.valueAt(0.75f)
        linearValue = linearValueAt(0.75f)
        assertTrue(arcValue[0] > linearValue[0])
        assertTrue(arcValue[1] < linearValue[1])

        // Test that x at 25% is the complement of y at 75%
        assertEquals(
            targetValue - animation.valueAt(0.25f)[0],
            animation.valueAt(0.75f)[1],
            error // Bound to have some minor differences :)
        )

        var arcVelocity: AnimationVector3D
        // Test that velocity at 50% is equal on both components
        arcVelocity = animation.velocityAt(0.5f)
        assertEquals(arcVelocity[0], arcVelocity[1], error)

        // Test that for velocity at 0% only the X component is non-zero
        arcVelocity = animation.velocityAt(0.0f)
        assertEquals(0f, arcVelocity[1], error)
        assertTrue(arcVelocity[0] > error)

        // Test that for velocity at 100% only the Y component in non-zero
        arcVelocity = animation.velocityAt(1f)
        assertEquals(0f, arcVelocity[0], error)
        assertTrue(arcVelocity[1] > error)
    }

    @Test
    fun test4DInterpolation_twoPairsAsArcs() {
        val animation = createArcAnimation<AnimationVector4D>(ArcAbove)
        var arcValue: AnimationVector4D
        var linearValue: AnimationVector4D

        // Test values at 25%, 50%, 75%
        // For arc below Y will always be higher but X will be lower
        // Similarly for [3] and [2], the second pair
        arcValue = animation.valueAt(0.25f)
        linearValue = linearValueAt(0.25f)
        assertTrue(arcValue[0] > linearValue[0])
        assertTrue(arcValue[1] < linearValue[1])

        // Second pair
        assertTrue(arcValue[2] > linearValue[2])
        assertTrue(arcValue[3] < linearValue[3])

        arcValue = animation.valueAt(0.5f)
        linearValue = linearValueAt(0.5f)
        assertTrue(arcValue[0] > linearValue[0])
        assertTrue(arcValue[1] < linearValue[1])

        // Second pair
        assertTrue(arcValue[2] > linearValue[2])
        assertTrue(arcValue[3] < linearValue[3])

        arcValue = animation.valueAt(0.75f)
        linearValue = linearValueAt(0.75f)
        assertTrue(arcValue[0] > linearValue[0])
        assertTrue(arcValue[1] < linearValue[1])

        // Second pair
        assertTrue(arcValue[2] > linearValue[2])
        assertTrue(arcValue[3] < linearValue[3])
    }

    @Test
    fun testEquals() {
        // Equal mode with defaults
        var animationA = ArcAnimationSpec<Float>(ArcAbove)
        var animationB = ArcAnimationSpec<Float>(ArcAbove)
        assertEquals(animationA, animationB)

        // Equals with custom values
        animationA =
            ArcAnimationSpec(
                mode = ArcBelow,
                durationMillis = 13,
                delayMillis = 17,
                easing = EaseInOut
            )
        animationB =
            ArcAnimationSpec(
                mode = ArcBelow,
                durationMillis = 13,
                delayMillis = 17,
                easing = CubicBezierEasing(0.42f, 0.0f, 0.58f, 1.0f) // Re-declared EasInOut
            )
        assertEquals(animationA, animationB)
    }

    @Test
    fun testNotEquals() {
        // Different modes
        var animationA = ArcAnimationSpec<Float>(ArcAbove)
        var animationB = ArcAnimationSpec<Float>(ArcBelow)
        assertNotEquals(animationA, animationB)

        // Different duration
        animationA = ArcAnimationSpec(mode = ArcLinear, durationMillis = 5)
        animationB = ArcAnimationSpec(mode = ArcLinear, durationMillis = 7)
        assertNotEquals(animationA, animationB)

        // Different delay
        animationA = ArcAnimationSpec(mode = ArcLinear, delayMillis = 9)
        animationB = ArcAnimationSpec(mode = ArcLinear, delayMillis = 11)
        assertNotEquals(animationA, animationB)

        // Different Easing
        animationA = ArcAnimationSpec(mode = ArcLinear, easing = EaseInOut)
        animationB = ArcAnimationSpec(mode = ArcLinear, easing = FastOutSlowInEasing)
        assertNotEquals(animationA, animationB)
    }

    @Test
    fun testArcSplineGraph_overallCurve() {
        val expectX =
            """
                |*******************                                         | 0.0
                |                   ******                                   |
                |                          *****                             |
                |                               *****                        |
                |                                    ***                     |
                |                                       ***                  | 71.429
                |                                          ***               |
                |                                             ***            |
                |                                                **          |
                |                                                 * *        |
                |                                                    **      | 142.857
                |                                                      **    |
                |                                                        **  |
                |                                                          * |
                |                                                           *| 200.0
                0.0                                                        5.0
            """
                .trimIndent()
        val expectY =
            """
                |****                                                        | 0.0
                |    ***                                                     |
                |       ****                                                 |
                |           ***                                              |
                |              ****                                          |
                |                  ****                                      | 142.857
                |                      ***                                   |
                |                        * ***                               |
                |                             ****                           |
                |                                 ***                        |
                |                                    ****                    | 285.714
                |                                        *****               |
                |                                             *****          |
                |                                                 * ******** |
                |                                                           *| 400.0
                0.0                                                        5.0
            """
                .trimIndent()
        assertArcSplineCurve(
            segment = CurveSegment.All,
            expectGraphX = expectX,
            expectGraphY = expectY
        )
    }

    @Test
    fun testArcSplineGraph_startOfCurve() {
        val expectX =
            """
                |****************                                            | 0.0
                |                *******                                     |
                |                       *****                                |
                |                            ***                             |
                |                                ****                        |
                |                                    ***                     | 2.116
                |                                       ***                  |
                |                                          ***               |
                |                                             ***            |
                |                                                **          |
                |                                                  ***       | 4.232
                |                                                     **     |
                |                                                       **   |
                |                                                         ** |
                |                                                           *| 5.925
                0.0                                                        1.0
            """
                .trimIndent()
        val expectY =
            """
                |*****                                                       | 0.0
                |     ****                                                   |
                |         ****                                               |
                |             ****                                           |
                |                 *****                                      |
                |                      ****                                  | 34.515
                |                          ****                              |
                |                              * **                          |
                |                                  ****                      |
                |                                      *****                 |
                |                                           ****             | 69.029
                |                                               ****         |
                |                                                   ****     |
                |                                                       **** |
                |                                                           *| 96.641
                0.0                                                        1.0
            """
                .trimIndent()
        assertArcSplineCurve(
            segment = CurveSegment.Start,
            expectGraphX = expectX,
            expectGraphY = expectY
        )
    }

    @Test
    fun testArcSplineGraph_endOfCurve() {
        val expectX =
            """
                |******                                                      | 113.9
                |      ****                                                  |
                |          ****                                              |
                |              **** *                                        |
                |                    ****                                    |
                |                        ****                                | 144.65
                |                            ***                             |
                |                               *****                        |
                |                                    * **                    |
                |                                        ****                |
                |                                            ****            | 175.4
                |                                                * *         |
                |                                                   ****     |
                |                                                       ** **|
                |                                                            | 200.0
                4.0                                                        5.0
            """
                .trimIndent()
        val expectY =
            """
                |***                                                         | 361.036
                |   ***                                                      |
                |      **                                                    |
                |        ***                                                 |
                |          ***                                               |
                |             ***                                            | 374.952
                |                ** *                                        |
                |                    ***                                     |
                |                       ***                                  |
                |                          ****                              |
                |                             ****                           | 388.868
                |                                 **** *                     |
                |                                       ******               |
                |                                             **** ******* * |
                |                                                           *| 400.0
                4.0                                                        5.0
            """
                .trimIndent()
        assertArcSplineCurve(
            segment = CurveSegment.End,
            expectGraphX = expectX,
            expectGraphY = expectY
        )
    }

    private fun assertArcSplineCurve(
        segment: CurveSegment,
        expectGraphX: String,
        expectGraphY: String
    ) {
        val startTime = 0f
        val endTime = 5f
        val arcSpline =
            ArcSpline(
                arcModes = intArrayOf(ArcSplineArcBelow),
                timePoints = floatArrayOf(startTime, endTime),
                y = arrayOf(floatArrayOf(0f, 0f), floatArrayOf(200f, 400f))
            )
        val arcSplineX =
            plot2DArcSpline(
                spline = arcSpline,
                dimensionToPlot = 0,
                start = endTime * segment.startPercent,
                end = endTime * segment.endPercent
            )
        assertEquals("Graph on X dimension not equals", expectGraphX, arcSplineX)

        val arcSplineY =
            plot2DArcSpline(
                spline = arcSpline,
                dimensionToPlot = 1,
                start = endTime * segment.startPercent,
                end = endTime * segment.endPercent
            )
        assertEquals("Graph on Y dimension not equals", expectGraphY, arcSplineY)
    }

    private inline fun <reified V : AnimationVector> VectorizedDurationBasedAnimationSpec<V>
        .valueAt(timePercent: Float): V =
        this.getValueFromNanos(
            playTimeNanos = (durationMillis * timePercent).toLong() * 1_000_000,
            initialValue = createFilledVector(initialValue),
            targetValue = createFilledVector(targetValue),
            initialVelocity = createFilledVector(0f)
        )

    private inline fun <reified V : AnimationVector> VectorizedDurationBasedAnimationSpec<V>
        .velocityAt(timePercent: Float): V =
        this.getVelocityFromNanos(
            playTimeNanos = (durationMillis * timePercent).toLong() * 1_000_000,
            initialValue = createFilledVector(initialValue),
            targetValue = createFilledVector(targetValue),
            initialVelocity = createFilledVector(0f)
        )

    private inline fun <reified V : AnimationVector> linearValueAt(timePercent: Float): V {
        val value = timePercent * targetValue
        return createFilledVector<V>(value)
    }

    /** Creates an [ArcAnimationSpec] for the given [AnimationVector] type. */
    private inline fun <reified V : AnimationVector> createArcAnimation(
        mode: ArcMode,
        easing: Easing = LinearEasing
    ): VectorizedDurationBasedAnimationSpec<V> {
        val spec =
            ArcAnimationSpec<FloatArray>(mode = mode, durationMillis = timeMillis, easing = easing)
        return spec.vectorize(createFloatArrayConverter())
    }
}

private enum class CurveSegment(val startPercent: Float, val endPercent: Float) {
    All(0f, 1f),
    Start(0f, 1f / 5f),
    End(4f / 5f, 1f)
}

/** Plot an [ArcSpline] under the assumption that it has 2 dimensions in values. */
private fun plot2DArcSpline(
    spline: ArcSpline,
    dimensionToPlot: Int,
    start: Float,
    end: Float
): String {
    val count = 60
    val x = FloatArray(count)
    val y = FloatArray(count)
    var c = 0
    val output = FloatArray(2)
    for (i in 0 until count) {
        val t = start + (end - start) * i / (count - 1)
        x[c] = t
        spline.getPos(t, output)
        y[c] = output[dimensionToPlot]
        c++
    }
    return drawTextGraph(count, count / 4, x, y, false)
}
