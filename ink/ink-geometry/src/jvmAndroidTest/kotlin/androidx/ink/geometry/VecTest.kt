/*
 * Copyright (C) 2024 The Android Open Source Project
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

package androidx.ink.geometry

import com.google.common.truth.Truth.assertThat
import kotlin.math.sqrt
import kotlin.test.assertFailsWith
import org.junit.Test
import org.junit.runner.RunWith
import org.junit.runners.JUnit4

@RunWith(JUnit4::class)
class VecTest {

    @Test
    fun isAlmostEqual_whenNoToleranceGiven_returnsCorrectValue() {
        val vec = ImmutableVec(1f, 2f)

        assertThat(vec.isAlmostEqual(vec)).isTrue()
        assertThat(vec.isAlmostEqual(ImmutableVec(1f, 2f))).isTrue()
        assertThat(vec.isAlmostEqual(ImmutableVec(1.00001f, 1.99999f))).isTrue()
        assertThat(vec.isAlmostEqual(ImmutableVec(1f, 1.99f))).isFalse()
        assertThat(vec.isAlmostEqual(ImmutableVec(1.01f, 2f))).isFalse()
        assertThat(vec.isAlmostEqual(ImmutableVec(1.01f, 1.99f))).isFalse()
    }

    @Test
    fun isAlmostEqual_withToleranceGiven_returnsCorrectValue() {
        val vec = ImmutableVec(1f, 2f)

        assertThat(vec.isAlmostEqual(vec, tolerance = 0.00000001f)).isTrue()
        assertThat(vec.isAlmostEqual(ImmutableVec(1f, 2f), tolerance = 0.00000001f)).isTrue()
        assertThat(vec.isAlmostEqual(ImmutableVec(1.00001f, 1.99999f), tolerance = 0.000001f))
            .isFalse()
        assertThat(vec.isAlmostEqual(ImmutableVec(1f, 1.99f), tolerance = 0.02f)).isTrue()
        assertThat(vec.isAlmostEqual(ImmutableVec(1.01f, 2f), tolerance = 0.02f)).isTrue()
        assertThat(vec.isAlmostEqual(ImmutableVec(1.01f, 1.99f), tolerance = 0.02f)).isTrue()
        assertThat(vec.isAlmostEqual(ImmutableVec(2.5f, 0.5f), tolerance = 2f)).isTrue()
    }

    @Test
    fun isAlmostEqual_whenSameInterface_returnsTrue() {
        val vec = MutableVec(1f, 2f)
        val other = ImmutableVec(0.99999f, 2.00001f)
        assertThat(vec.isAlmostEqual(other)).isTrue()
    }

    @Test
    fun direction_returnsCorrectValue() {
        assertThat(ImmutableVec(5f, 0f).direction).isEqualTo(Angle.degreesToRadians(0f))
        assertThat(ImmutableVec(0f, 5f).direction).isEqualTo(Angle.degreesToRadians(90f))
        assertThat(ImmutableVec(-5f, 0f).direction).isEqualTo(Angle.degreesToRadians(180f))
        assertThat(ImmutableVec(0f, -5f).direction).isEqualTo(Angle.degreesToRadians(-90f))
        assertThat(ImmutableVec(5f, 5f).direction).isEqualTo(Angle.degreesToRadians(45f))
        assertThat(ImmutableVec(-5f, 5f).direction).isEqualTo(Angle.degreesToRadians(135f))
        assertThat(ImmutableVec(-5f, -5f).direction).isEqualTo(Angle.degreesToRadians(-135f))
        assertThat(ImmutableVec(5f, -5f).direction).isEqualTo(Angle.degreesToRadians(-45f))
    }

    @Test
    fun direction_whenVecContainsZero_returnsCorrectValue() {
        assertThat(ImmutableVec(+0f, +0f).direction).isEqualTo(Angle.degreesToRadians(0f))
        assertThat(ImmutableVec(+0f, -0f).direction).isEqualTo(Angle.degreesToRadians(-0f))
        assertThat(ImmutableVec(-0f, +0f).direction).isEqualTo(Angle.degreesToRadians(180f))
        assertThat(ImmutableVec(-0f, -0f).direction).isEqualTo(Angle.degreesToRadians(-180f))
    }

    @Test
    fun unitVec_returnsCorrectValue() {
        assertThat(ImmutableVec(4f, 0f).unitVec).isEqualTo(ImmutableVec(1f, 0f))
        assertThat(MutableVec(0f, -25f).unitVec).isEqualTo(ImmutableVec(0f, -1f))
        assertThat(
                ImmutableVec(30f, 30f)
                    .unitVec
                    .isAlmostEqual(ImmutableVec(sqrt(.5f), sqrt(.5f)), tolerance = 0.000001f)
            )
            .isTrue()
        assertThat(
                MutableVec(-.05f, -.05f)
                    .unitVec
                    .isAlmostEqual(ImmutableVec(-sqrt(.5f), -sqrt(.5f)), tolerance = 0.000001f)
            )
            .isTrue()
    }

    @Test
    fun unitVec_whenVecContainsZeroes_returnsCorrectValue() {
        assertThat(ImmutableVec(+0f, 0f).unitVec).isEqualTo(ImmutableVec(1f, 0f))
        assertThat(MutableVec(-0f, 0f).unitVec).isEqualTo(ImmutableVec(-1f, 0f))
    }

    @Test
    fun populateUnitVec_populatesCorrectValue() {
        val mutableVec = MutableVec(0f, 0f)
        MutableVec(4f, 0f).populateUnitVec(mutableVec)
        assertThat(mutableVec).isEqualTo(ImmutableVec(1f, 0f))

        ImmutableVec(0f, -25f).populateUnitVec(mutableVec)
        assertThat(mutableVec).isEqualTo(ImmutableVec(0f, -1f))

        MutableVec(30f, 30f).populateUnitVec(mutableVec)
        assertThat(mutableVec.isAlmostEqual(ImmutableVec(sqrt(.5f), sqrt(.5f)))).isTrue()

        ImmutableVec(-.05f, -.05f).populateUnitVec(mutableVec)
        assertThat(mutableVec.isAlmostEqual(ImmutableVec(-sqrt(.5f), -sqrt(.5f)))).isTrue()
    }

    @Test
    fun populateUnitVec_whenVecContainsZeroes_populatesCorrectValue() {
        val mutableVec = MutableVec(0f, 0f)
        MutableVec(+0f, 0f).populateUnitVec(mutableVec)
        assertThat(mutableVec).isEqualTo(ImmutableVec(1f, 0f))

        ImmutableVec(-0f, -0f).populateUnitVec(mutableVec)
        assertThat(mutableVec).isEqualTo(ImmutableVec(-1f, 0f))
    }

    @Test
    fun absoluteAngleBetween_returnsCorrectValue() {
        assertThat(Vec.absoluteAngleBetween(ImmutableVec(10f, 0f), ImmutableVec(40f, 0f)))
            .isEqualTo(Angle.degreesToRadians(0f))
        assertThat(Vec.absoluteAngleBetween(MutableVec(7f, 0f), MutableVec(0f, 12f)))
            .isEqualTo(Angle.degreesToRadians(90f))
        assertThat(Vec.absoluteAngleBetween(ImmutableVec(-5f, 0f), MutableVec(.1f, 0f)))
            .isEqualTo(Angle.degreesToRadians(180f))
        assertThat(Vec.absoluteAngleBetween(MutableVec(20f, 20f), ImmutableVec(0f, 10f)))
            .isEqualTo(Angle.degreesToRadians(45f))
        assertThat(Vec.absoluteAngleBetween(ImmutableVec(-2f, 2f), ImmutableVec(0f, -3f)))
            .isEqualTo(Angle.degreesToRadians(135f))
        assertThat(
                Vec.absoluteAngleBetween(MutableVec(-1f, -sqrt(3.0f)), MutableVec(1f, -sqrt(3.0f)))
            )
            .isEqualTo(Angle.degreesToRadians(60f))
    }

    @Test
    fun signedAngleBetween_returnsCorrectValue() {
        assertThat(Vec.signedAngleBetween(MutableVec(2f, 0f), MutableVec(2f, 0f)))
            .isEqualTo(Angle.degreesToRadians(0f))
        assertThat(Vec.signedAngleBetween(ImmutableVec(20f, 0f), ImmutableVec(0f, .1f)))
            .isEqualTo(Angle.degreesToRadians(90f))
        assertThat(Vec.signedAngleBetween(MutableVec(0f, 10f), ImmutableVec(17f, 0f)))
            .isEqualTo(Angle.degreesToRadians(-90f))
        assertThat(Vec.signedAngleBetween(ImmutableVec(-1f, 0f), MutableVec(.11f, 0f)))
            .isEqualTo(Angle.degreesToRadians(180f))
        assertThat(Vec.signedAngleBetween(MutableVec(12f, 12f), MutableVec(-3f, 3f)))
            .isEqualTo(Angle.degreesToRadians(90f))
        assertThat(Vec.signedAngleBetween(ImmutableVec(-1f, -1f), ImmutableVec(-987f, 0f)))
            .isEqualTo(Angle.degreesToRadians(-45f))
        assertThat(Vec.signedAngleBetween(ImmutableVec(-62f, -62f), ImmutableVec(sqrt(3.0f), 1f)))
            .isEqualTo(Angle.degreesToRadians(165f))
        assertThat(Vec.signedAngleBetween(MutableVec(-11f, 11f), ImmutableVec(.01f, 0f)))
            .isEqualTo(Angle.degreesToRadians(-135f))
        assertThat(
                Vec.signedAngleBetween(MutableVec(1f, -sqrt(3.0f)), MutableVec(-1f, -sqrt(3.0f)))
            )
            .isEqualTo(Angle.degreesToRadians(-60f))
    }

    @Test
    fun isParallelTo_withEquivalentVecs_returnsTrue() {
        assertThat(MutableVec(1f, 0f).isParallelTo(MutableVec(1f, 0f), .001f)).isTrue()
        assertThat(MutableVec(0f, 100f).isParallelTo(MutableVec(0f, 100f), .001f)).isTrue()
        assertThat(MutableVec(359.38f, -7.84f).isParallelTo(MutableVec(359.38f, -7.84f), .001f))
            .isTrue()
    }

    @Test
    fun isParallelTo_whenVecsHaveSameDirection_returnsTrue() {
        assertThat(MutableVec(10f, 0f).isParallelTo(MutableVec(99f, 0f), .001f)).isTrue()
        assertThat(MutableVec(0f, 40f).isParallelTo(MutableVec(0f, 99f), .001f)).isTrue()
        assertThat(MutableVec(3f, -6f).isParallelTo(MutableVec(32f, -64f), .001f)).isTrue()
        assertThat(MutableVec(.0001f, .0009f).isParallelTo(MutableVec(.0005f, .0045f), .001f))
            .isTrue()
    }

    @Test
    fun isParallelTo_whenVecsHaveOppositeDirections_returnsTrue() {
        assertThat(MutableVec(8f, 0f).isParallelTo(MutableVec(-7f, 0f), .001f)).isTrue()
        assertThat(MutableVec(0f, 30f).isParallelTo(MutableVec(0f, -.99f), .001f)).isTrue()
        assertThat(MutableVec(.2f, .2f).isParallelTo(MutableVec(-99f, -99f), .001f)).isTrue()
        assertThat(MutableVec(-32f, 64f).isParallelTo(MutableVec(5f, -10f), .001f)).isTrue()
    }

    @Test
    fun isParallelTo_whenVecsHaveDifferentDirections_returnsFalse() {
        assertThat(MutableVec(5f, 5f).isParallelTo(MutableVec(1f, -1f), .001f)).isFalse()
        assertThat(MutableVec(-3f, -10f).isParallelTo(MutableVec(-88f, 17.5f), .001f)).isFalse()

        // These Vecs have different but close directions. These would pass with sufficiently high
        // tolerance, but fail with low tolerance.
        assertThat(MutableVec(100f, 100f).isParallelTo(MutableVec(99f, 100f), .001f)).isFalse()
        assertThat(MutableVec(100f, 100f).isParallelTo(MutableVec(100f, 99f), .001f)).isFalse()
        assertThat(MutableVec(-100f, 100f).isParallelTo(MutableVec(-99f, 100f), .001f)).isFalse()
        assertThat(MutableVec(100f, -100f).isParallelTo(MutableVec(100f, -99f), .001f)).isFalse()
    }

    @Test
    fun isPerpendicularTo_returnsCorrectValue() {
        assertThat(MutableVec(1f, 0f).isPerpendicularTo(MutableVec(0f, 5f), .001f)).isTrue()
        assertThat(MutableVec(5f, 0f).isPerpendicularTo(MutableVec(0f, -10f), .001f)).isTrue()
        assertThat(MutableVec(0f, 100f).isPerpendicularTo(MutableVec(-.01f, 0f), .001f)).isTrue()
        assertThat(MutableVec(77f, -77f).isPerpendicularTo(MutableVec(200f, 200f), .001f)).isTrue()
        assertThat(MutableVec(-32f, 64f).isPerpendicularTo(MutableVec(86f, 43f), .001f)).isTrue()
        assertThat(
                MutableVec(.0001f, -.0009f).isPerpendicularTo(MutableVec(-.0045f, -.0005f), .001f)
            )
            .isTrue()

        assertThat(MutableVec(1f, -2f).isPerpendicularTo(MutableVec(1f, -2f), .001f)).isFalse()
        assertThat(MutableVec(1f, -2f).isPerpendicularTo(MutableVec(-1f, 2f), .001f)).isFalse()
        assertThat(MutableVec(10f, 10f).isPerpendicularTo(MutableVec(0f, 10f), .001f)).isFalse()
        assertThat(MutableVec(-30f, 25f).isPerpendicularTo(MutableVec(50f, 30f), .001f)).isFalse()

        // These Vecs are close but not quite perpendicular. These would pass with sufficiently high
        // tolerance, but fail with low tolerance.
        assertThat(MutableVec(100f, 100f).isPerpendicularTo(MutableVec(-99f, 100f), .001f))
            .isFalse()
        assertThat(MutableVec(100f, 100f).isPerpendicularTo(MutableVec(-100f, 99f), .001f))
            .isFalse()
        assertThat(MutableVec(-100f, 100f).isPerpendicularTo(MutableVec(-99f, -100f), .001f))
            .isFalse()
        assertThat(MutableVec(100f, -100f).isPerpendicularTo(MutableVec(100f, 99f), .001f))
            .isFalse()
    }

    @Test
    fun determinant_returnsCorrectValue() {
        val a = ImmutableVec(3f, 0f)
        val b = ImmutableVec(-1f, 4f)
        val c = ImmutableVec(2f, .5f)

        assertThat(Vec.determinant(a, b)).isEqualTo(12f)
        assertThat(Vec.determinant(a, c)).isEqualTo(1.5f)
        assertThat(Vec.determinant(b, a)).isEqualTo(-12f)
        assertThat(Vec.determinant(b, c)).isEqualTo(-8.5f)
        assertThat(Vec.determinant(c, a)).isEqualTo(-1.5f)
        assertThat(Vec.determinant(c, b)).isEqualTo(8.5f)
    }

    @Test
    fun add_populatesCorrectValue() {
        val a = ImmutableVec(3f, 0f)
        val b = MutableVec(-1f, .3f)
        val c = ImmutableVec(2.7f, 4f)

        val aPlusbOut = MutableVec()
        val aPluscOut = MutableVec()
        val bPluscOut = MutableVec()

        Vec.add(a, b, aPlusbOut)
        Vec.add(a, c, aPluscOut)
        Vec.add(b, c, bPluscOut)

        assertThat(aPlusbOut.isAlmostEqual(ImmutableVec(2f, .3f))).isTrue()
        assertThat(aPluscOut.isAlmostEqual(ImmutableVec(5.7f, 4f))).isTrue()
        assertThat(bPluscOut.isAlmostEqual(ImmutableVec(1.7f, 4.3f))).isTrue()
    }

    @Test
    fun multiply_populatesCorrectValue() {
        val a = ImmutableVec(.7f, -3f)
        val b = MutableVec(3f, 5f)

        val aMultipliedBy2Out = MutableVec()
        val aMultipliedBy1TenthOut = MutableVec()
        val bMultipliedBy4Out = MutableVec()
        val bMultipliedByNegative3TenthsOut = MutableVec()

        Vec.multiply(a, 2f, aMultipliedBy2Out)
        Vec.multiply(.1f, a, aMultipliedBy1TenthOut)
        Vec.multiply(b, 4f, bMultipliedBy4Out)
        Vec.multiply(-.3f, b, bMultipliedByNegative3TenthsOut)

        assertThat(aMultipliedBy2Out.isAlmostEqual(ImmutableVec(1.4f, -6f))).isTrue()
        assertThat(aMultipliedBy1TenthOut.isAlmostEqual(ImmutableVec(.07f, -0.3f))).isTrue()
        assertThat(bMultipliedBy4Out.isAlmostEqual(ImmutableVec(12f, 20f))).isTrue()
        assertThat(bMultipliedByNegative3TenthsOut.isAlmostEqual(ImmutableVec(-0.9f, -1.5f)))
            .isTrue()
    }

    @Test
    fun divide_populatesCorrectValue() {
        val a = ImmutableVec(7f, .9f)
        val b = MutableVec(-4.5f, -2f)

        val aDividedBy2Out = MutableVec()
        val aDividedByNegative1TenthOut = MutableVec()
        val bDividedBy5Out = MutableVec()
        val bDividedBy2TenthsOut = MutableVec()

        Vec.divide(a, 2f, aDividedBy2Out)
        Vec.divide(a, -.1f, aDividedByNegative1TenthOut)
        Vec.divide(b, 5f, bDividedBy5Out)
        Vec.divide(b, .2f, bDividedBy2TenthsOut)

        assertThat(aDividedBy2Out.isAlmostEqual(ImmutableVec(3.5f, .45f))).isTrue()
        assertThat(aDividedByNegative1TenthOut.isAlmostEqual(ImmutableVec(-70f, -9f))).isTrue()
        assertThat(bDividedBy5Out.isAlmostEqual(ImmutableVec(-.9f, -.4f))).isTrue()
        assertThat(bDividedBy2TenthsOut.isAlmostEqual(ImmutableVec(-22.5f, -10f))).isTrue()
    }

    @Test
    fun divide_whenDividingByZero_throwsException() {
        val testOutput = MutableVec()

        assertFailsWith<IllegalArgumentException> {
            Vec.divide(ImmutableVec(2f, 3f), 0f, testOutput)
        }
        assertFailsWith<IllegalArgumentException> { Vec.divide(MutableVec(0f, 0f), 0f, testOutput) }
    }

    @Test
    fun subtract_returnsCorrectValue() {
        val a = ImmutableVec(0f, -2f)
        val b = MutableVec(.5f, 19f)
        val c = ImmutableVec(1.1f, -3.4f)
        val aMinusbOut = MutableVec()
        val aMinuscOut = MutableVec()
        val bMinuscOut = MutableVec()

        Vec.subtract(a, b, aMinusbOut)
        Vec.subtract(a, c, aMinuscOut)
        Vec.subtract(b, c, bMinuscOut)

        assertThat(aMinusbOut.isAlmostEqual(ImmutableVec(-.5f, -21f), tolerance = 0.001f)).isTrue()
        assertThat(aMinuscOut.isAlmostEqual(ImmutableVec(-1.1f, 1.4f), tolerance = 0.001f)).isTrue()
        assertThat(bMinuscOut.isAlmostEqual(ImmutableVec(-.6f, 22.4f), tolerance = 0.001f)).isTrue()
    }

    @Test
    fun dotProduct_returnsCorrectValue() {
        val a = ImmutableVec(3f, 0f)
        val b = MutableVec(-1f, 4f)
        val c = MutableVec(2f, .5f)
        val d = ImmutableVec(6f, 6f)

        assertThat(Vec.dotProduct(a, b)).isEqualTo(-3f)
        assertThat(Vec.dotProduct(a, c)).isEqualTo(6f)
        assertThat(Vec.dotProduct(a, d)).isEqualTo(18f)
        assertThat(Vec.dotProduct(b, c)).isEqualTo(0f)
        assertThat(Vec.dotProduct(b, d)).isEqualTo(18f)
        assertThat(Vec.dotProduct(c, d)).isEqualTo(15f)
    }

    @Test
    fun origin_isCorrectValueAndReturnsSameInstance() {
        assertThat(Vec.ORIGIN).isEqualTo(ImmutableVec(0f, 0f))
        assertThat(Vec.ORIGIN).isSameInstanceAs(Vec.ORIGIN)
    }
}
