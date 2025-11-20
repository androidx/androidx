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

package androidx.xr.runtime.math

import com.google.common.truth.Truth.assertThat
import java.lang.IllegalArgumentException
import org.junit.Assert.assertThrows
import org.junit.Test
import org.junit.runner.RunWith
import org.junit.runners.JUnit4

@RunWith(JUnit4::class)
class Vector4Test {
    @Test
    fun constructor_noArguments_returnsZeroVector() {
        val underTest = Vector4()

        assertThat(underTest).isEqualTo(Vector4(0f, 0f, 0f, 0f))
    }

    @Test
    fun equals_sameValues_returnsTrue() {
        val underTest = Vector4(1f, 2f, 3f, 4f)
        val underTest2 = Vector4(1f, 2f, 3f, 4f)

        assertThat(underTest).isEqualTo(underTest2)
    }

    @Test
    fun equals_differentValues_returnsFalse() {
        val underTest = Vector4(1f, 2f, 3f, 4f)
        val underTest2 = Vector4(9f, 10f, 11f, 12f)
        val underTest3 = Vector3()

        assertThat(underTest).isNotEqualTo(underTest2)
        assertThat(underTest).isNotEqualTo(underTest3)
    }

    @Test
    fun hashCodeEquals_sameValues_returnsTrue() {
        val underTest = Vector4(1f, 2f, 3f, 4f)
        val underTest2 = Vector4(1f, 2f, 3f, 4f)

        assertThat(underTest.hashCode()).isEqualTo(underTest2.hashCode())
    }

    @Test
    fun hashCodeEquals_differentValues_returnsFalse() {
        val underTest = Vector4(1f, 2f, 3f, 4f)
        val underTest2 = Vector4(9f, 10f, 11f, 12f)
        val underTest3 = Vector4()

        assertThat(underTest.hashCode()).isNotEqualTo(underTest2.hashCode())
        assertThat(underTest.hashCode()).isNotEqualTo(underTest3.hashCode())
    }

    @Test
    fun constructorEquals_expectedToString_returnsTrue() {
        val underTest = Vector4(1f, 2f, 3f, 4f)
        val underTest2 = Vector4()

        assertThat(underTest.toString()).isEqualTo("[x=1.0, y=2.0, z=3.0, w=4.0]")
        assertThat(underTest2.toString()).isEqualTo("[x=0.0, y=0.0, z=0.0, w=0.0]")
    }

    @Test
    fun constructor_fromVector4_returnsSameValues() {
        val underTest = Vector4(1f, 2f, 3f, 4f)
        val underTest2 = underTest

        assertThat(underTest).isEqualTo(underTest2)
    }

    @Test
    fun fromFloat_returnsSameValues() {
        val underTest = Vector4.fromValue(1f)

        assertThat(underTest).isEqualTo(Vector4.One)
    }

    @Test
    fun normalized_returnsVectorWithUnitLength1() {
        val underTest = Vector4(1f, 1f, 2f, 2f).toNormalized()

        assertVector4(underTest, 0.3162f, 0.3162f, 0.6325f, 0.6325f)
    }

    @Test
    fun normalized_returnsVectorWithUnitLength2() {
        val underTest = Vector4(1f, 1f, 1f, 1f).toNormalized()

        assertVector4(underTest, 0.5f, 0.5f, 0.5f, 0.5f)
    }

    @Test
    fun multiply_returnsVectorScaledByScalar1() {
        assertThat(Vector4(3f, 4f, 5f, 6f) * 2f).isEqualTo(Vector4(6f, 8f, 10f, 12f))
    }

    @Test
    fun multiply_returnsVectorScaledByScalar2() {
        assertThat(Vector4(1f, 1f, 0.5f, 10f) * 0.5f).isEqualTo(Vector4(0.5f, 0.5f, 0.25f, 5f))
    }

    @Test
    fun plus_returnsVectorWithAddedValues() {
        val underTest = Vector4(1F, 2F, 3F, 4F) + Vector4(4F, 5F, 6F, 7F)

        assertVector4(underTest, 5F, 7F, 9F, 11F)
    }

    @Test
    fun minus_returnsVectorWithSubtractedValues() {
        val underTest = Vector4(4F, 5F, 6F, 7F) - Vector4(1F, 2F, 3F, 4F)

        assertVector4(underTest, 3F, 3F, 3F, 3F)
    }

    @Test
    fun scale_returnsTwoVectorsMultiplied() {
        val underTest = Vector4(1f, 2f, 3f, 4f)
        val underTest2 = Vector4(3f, 4f, 5f, 6f)
        val underTestMultiply = underTest.scale(underTest2)

        assertThat(underTestMultiply).isEqualTo(Vector4(3f, 8f, 15f, 24f))
    }

    @Test
    fun dot_returnsDotProductOfTwoVectors() {
        val underTest = Vector4(1f, 2f, 3f, 4f)
        val underTest2 = Vector4(3f, -4f, 5f, 2f)
        val underTestDot = underTest dot underTest2

        assertThat(underTestDot).isEqualTo(18f)
    }

    @Test
    fun length_returnsSqrtOfEachComponentSquared() {
        assertThat(Vector4(0F, 0F, 3F, 4F).length)
            .isEqualTo(5F) // sqrt(0^2 + 0^2 + 3^2 + 4^2) = sqrt(25)
    }

    @Test
    fun clamp_returnsVectorClampedBetweenTwoVectors1() {
        val underTest =
            Vector4(1f, 2f, 3f, 4f).clamp(Vector4(4f, 5f, 6f, 7f), Vector4(7f, 8f, 9f, 10f))

        assertVector4(underTest, 4f, 5f, 6f, 7f)
    }

    @Test
    fun clamp_returnsVectorClampedBetweenTwoVectors2() {
        val underTest =
            Vector4(1f, 2f, 3f, 4f).clamp(Vector4(1f, 2f, 3f, 4f), Vector4(5f, 6f, 7f, 8f))

        assertVector4(underTest, 1f, 2f, 3f, 4f)
    }

    @Test
    fun clamp_returnsVectorClampedBetweenTwoVectors3() {
        val underTest =
            Vector4(6f, 7f, 8f, 9f).clamp(Vector4(1f, 2f, 3f, 4f), Vector4(5f, 6f, 7f, 8f))

        assertVector4(underTest, 5f, 6f, 7f, 8f)
    }

    @Test
    fun unaryMinus_returnsVectorWithNegativeValues() {
        val underTest = Vector4(1f, 2f, 3f, 4f)
        val underTestNegative = -underTest

        assertThat(underTestNegative).isEqualTo(Vector4(-1f, -2f, -3f, -4f))
    }

    @Test
    fun div_returnsVectorDividedByScalar() {
        val underTest = Vector4(1f, -2f, 3f, -4f)
        val underTestDiv = underTest / -2f

        assertThat(underTestDiv).isEqualTo(Vector4(-0.5f, 1f, -1.5f, 2f))
    }

    @Test
    fun componentwiseDivision_returnsVectorDividedByVector() {
        val underTest = Vector4(1f, 2f, 6f, 12f)
        val underTest2 = Vector4(-2f, 4f, -3f, 2f)
        val underTestDiv = underTest.scale(underTest2.inverse())

        assertThat(underTestDiv).isEqualTo(Vector4(-0.5f, 0.5f, -2f, 6f))
    }

    @Test
    fun inverse_nonZeroComponentVector_returnsInverseVector() {
        val underTest = Vector4(2f, 3f, 4f, 5f)
        val underTestInverse = underTest.inverse()

        assertThat(underTestInverse).isEqualTo(Vector4(1 / 2f, 1 / 3f, 1 / 4f, 1 / 5f))
    }

    @Test
    fun inverse_zeroComponent_returnsIllegalArgumentException() {
        val underTest = Vector4(0f, 1f, 2f, 3f)

        assertThrows(IllegalArgumentException::class.java) { underTest.inverse() }
    }

    @Test
    fun abs_returnsVectorWithAbsoluteValues() {
        val underTest = Vector4.abs(Vector4(-1f, 2f, -3f, 4f))

        assertThat(underTest).isEqualTo(Vector4(1f, 2f, 3f, 4f))
    }

    @Test
    fun copy_returnsCopyOfVector() {
        val underTest = Vector4(1f, 2f, 3f, 4f)
        val underTestCopy = underTest.copy()

        assertThat(underTestCopy).isEqualTo(underTest)
    }

    @Test
    fun angleBetween_returnsAngleBetweenTwoVectors1() {
        assertThat(
                toDegrees(Vector4.angleBetween(Vector4(1f, 0f, 0f, 0f), Vector4(0f, 1f, 0f, 0f)))
            )
            .isWithin(1e-5f)
            .of(90f)
    }

    @Test
    fun angleBetween_returnsAngleBetweenTwoVectors2() {
        assertThat(
                toDegrees(Vector4.angleBetween(Vector4(0f, 0f, 1f, 0f), Vector4(0f, 0f, -1f, 0f)))
            )
            .isWithin(1e-5f)
            .of(180f)
    }

    @Test
    fun angleBetween_returnsAngleBetweenTwoVectors3() {
        assertThat(
                toDegrees(Vector4.angleBetween(Vector4(2f, 4f, 0f, 0f), Vector4(4f, 8f, 0f, 0f)))
            )
            .isWithin(1e-5f)
            .of(0f)
    }

    @Test
    fun angleBetween_returnsAngleBetweenTwoVectors4() {
        assertThat(
                toDegrees(Vector4.angleBetween(Vector4(0f, 0f, 0f, 1f), Vector4(0f, 0f, 0f, 0f)))
            )
            .isWithin(1e-5f)
            .of(0f)
    }

    @Test
    fun distance_returnsDistanceBetweenTwoVectors() {
        val underTest = Vector4(1f, 0f, 1f, 1f)
        val underTest2 = Vector4(2f, 2f, 5f, 3f)
        val underTestDistance = Vector4.distance(underTest, underTest2)

        assertThat(underTestDistance).isWithin(1.0e-4f).of(5f) // sqrt(1^2 + 2^2 + 4^2 + 2^2)
    }

    @Test
    fun lerp_returnsInterpolatedVector1() {
        val underTest = Vector4.lerp(Vector4(1f, 2f, 3f, 4f), Vector4(4f, 5f, 6f, 7f), 0.5f)

        assertVector4(underTest, 2.5f, 3.5f, 4.5f, 5.5f)
    }

    @Test
    fun lerp_returnsInterpolatedVector2() {
        val underTest = Vector4.lerp(Vector4(4f, 5f, 6f, 7f), Vector4(12f, 15f, 18f, 21f), 0.25f)

        assertVector4(underTest, 6f, 7.5f, 9f, 10.5f)
    }

    @Test
    fun lerp_returnsInterpolatedVector3() {
        val underTest = Vector4.lerp(Vector4(2f, 6f, 10f, 14f), Vector4(12f, 26f, 30f, 34f), 0.4f)

        assertVector4(underTest, 6f, 14f, 18f, 22f)
    }

    @Test
    fun maxVector_returnsMaxVectorFromTwoVectors() {
        val underTest = Vector4(8f, 2f, -3f, 5f)
        val underTest2 = Vector4(4f, 5f, 6f, 2f)
        val underTestMax = Vector4.max(underTest, underTest2)

        assertThat(underTestMax).isEqualTo(Vector4(8f, 5f, 6f, 5f))
    }

    @Test
    fun minVector_returnsMinVectorFromTwoVectors() {
        val underTest = Vector4(8f, 2f, -3f, 0f)
        val underTest2 = Vector4(4f, 5f, 6f, -1f)
        val underTestMin = Vector4.min(underTest, underTest2)

        assertThat(underTestMin).isEqualTo(Vector4(4f, 2f, -3f, -1f))
    }

    private fun assertVector4(
        vector: Vector4,
        expectedX: Float,
        expectedY: Float,
        expectedZ: Float,
        expectedW: Float,
    ) {
        assertThat(vector.x).isWithin(1.0e-4f).of(expectedX)
        assertThat(vector.y).isWithin(1.0e-4f).of(expectedY)
        assertThat(vector.z).isWithin(1.0e-4f).of(expectedZ)
        assertThat(vector.w).isWithin(1.0e-4f).of(expectedW)
    }
}
