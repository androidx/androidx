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
class Vector3Test {
    @Test
    fun constructor_noArguments_returnsZeroVector() {
        val underTest = Vector3()

        assertThat(underTest).isEqualTo(Vector3(0f, 0f, 0f))
    }

    @Test
    fun equals_sameValues_returnsTrue() {
        val underTest = Vector3(1f, 2f, 3f)
        val underTest2 = Vector3(1f, 2f, 3f)

        assertThat(underTest).isEqualTo(underTest2)
    }

    @Test
    fun equals_differentValues_returnsFalse() {
        val underTest = Vector3(1f, 2f, 3f)
        val underTest2 = Vector3(9f, 10f, 11f)
        val underTest3 = Vector2()

        assertThat(underTest).isNotEqualTo(underTest2)
        assertThat(underTest).isNotEqualTo(underTest3)
    }

    @Test
    fun hashCodeEquals_sameValues_returnsTrue() {
        val underTest = Vector3(1f, 2f, 3f)
        val underTest2 = Vector3(1f, 2f, 3f)

        assertThat(underTest.hashCode()).isEqualTo(underTest2.hashCode())
    }

    @Test
    fun hashCodeEquals_differentValues_returnsFalse() {
        val underTest = Vector3(1f, 2f, 3f)
        val underTest2 = Vector3(9f, 10f, 11f)
        val underTest3 = Vector3()

        assertThat(underTest.hashCode()).isNotEqualTo(underTest2.hashCode())
        assertThat(underTest.hashCode()).isNotEqualTo(underTest3.hashCode())
    }

    @Test
    fun constructorEquals_expectedToString_returnsTrue() {
        val underTest = Vector3(1f, 2f, 3f)
        val underTest2 = Vector3()

        assertThat(underTest.toString()).isEqualTo("[x=1.0, y=2.0, z=3.0]")
        assertThat(underTest2.toString()).isEqualTo("[x=0.0, y=0.0, z=0.0]")
    }

    @Test
    fun constructor_fromVector3_returnsSameValues() {
        val underTest = Vector3(1f, 2f, 3f)
        val underTest2 = underTest

        assertThat(underTest).isEqualTo(underTest2)
    }

    @Test
    fun fromFloat_returnsSameValues() {
        val underTest = Vector3.fromValue(1f)

        assertThat(underTest).isEqualTo(Vector3.One)
    }

    @Test
    fun normalized_returnsVectorWithUnitLength() {
        assertThat(Vector3(3f, 4f, 5f).toNormalized())
            .isEqualTo(Vector3(0.42426407f, 0.56568545f, 0.7071068f))

        assertThat(Vector3(1f, 1f, 0.5f).toNormalized())
            .isEqualTo(Vector3(0.6666667f, 0.6666667f, 0.33333334f))
    }

    @Test
    fun multiply_returnsVectorScaledByScalar() {
        assertThat(Vector3(3f, 4f, 5f) * 2f).isEqualTo(Vector3(6f, 8f, 10f))

        assertThat(Vector3(1f, 1f, 0.5f) * 0.5f).isEqualTo(Vector3(0.5f, 0.5f, 0.25f))
    }

    @Test
    fun plus_returnsVectorWithAddedValues() {
        val underTest = Vector3(1F, 2F, 3F) + Vector3(4F, 5F, 6F)

        assertThat(underTest.x).isEqualTo(5F) // 1 + 4
        assertThat(underTest.y).isEqualTo(7F) // 2 + 5
        assertThat(underTest.z).isEqualTo(9F) // 3 + 6
    }

    @Test
    fun minus_returnsVectorWithSubtractedValues() {
        val underTest = Vector3(4F, 5F, 6F) - Vector3(1F, 2F, 3F)

        assertThat(underTest.x).isEqualTo(3F) // 4 - 1
        assertThat(underTest.y).isEqualTo(3F) // 5 - 2
        assertThat(underTest.z).isEqualTo(3F) // 6 - 3
    }

    @Test
    fun scale_returnsTwoVectorsMultiplied() {
        val underTest = Vector3(1f, 2f, 3f)
        val underTest2 = Vector3(3f, 4f, 5f)
        val underTestMultiply = underTest.scale(underTest2)

        assertThat(underTestMultiply).isEqualTo(Vector3(3f, 8f, 15f))
    }

    @Test
    fun dot_returnsDotProductOfTwoVectors() {
        val underTest = Vector3(1f, 2f, 3f)
        val underTest2 = Vector3(3f, -4f, 5f)
        val underTestDot = underTest dot underTest2

        assertThat(underTestDot).isEqualTo(10f)
    }

    @Test
    fun cross_returnsCrossProductOfTwoVectors() {
        val underTest = Vector3(1f, 2f, 3f)
        val underTest2 = Vector3(3f, -4f, 5f)
        val underTestCross = underTest cross underTest2

        assertThat(underTestCross).isEqualTo(Vector3(22f, 4f, -10f))
    }

    @Test
    fun length_returnsSqrtOfEachComponentSquared() {
        assertThat(Vector3(0F, 3F, 4F).length).isEqualTo(5F) // sqrt(0^2 + 3^2 + 4^2) = sqrt(25)
    }

    @Test
    fun clamp_returnsVectorClampedBetweenTwoVectors1() {
        val underTest = Vector3(1f, 2f, 3f).clamp(Vector3(4f, 5f, 6f), Vector3(7f, 8f, 9f))

        assertThat(underTest).isEqualTo(Vector3(4f, 5f, 6f))
    }

    @Test
    fun clamp_returnsVectorClampedBetweenTwoVectors2() {
        val underTest = Vector3(1f, 2f, 3f).clamp(Vector3(1f, 2f, 3f), Vector3(5f, 6f, 7f))

        assertThat(underTest).isEqualTo(Vector3(1f, 2f, 3f))
    }

    @Test
    fun clamp_returnsVectorClampedBetweenTwoVectors3() {
        val underTest = Vector3(5f, 6f, 7f).clamp(Vector3(1f, 2f, 3f), Vector3(5f, 6f, 7f))

        assertThat(underTest).isEqualTo(Vector3(5f, 6f, 7f))
    }

    @Test
    fun angleBetweenVectors_returnsAngleBetweenTwoVectors1() {
        assertThat(toDegrees(Vector3.angleBetween(Vector3(1f, 0f, 0f), Vector3(0f, 1f, 0f))))
            .isWithin(1e-5f)
            .of(90f)
    }

    @Test
    fun angleBetweenVectors_returnsAngleBetweenTwoVectors2() {
        assertThat(toDegrees(Vector3.angleBetween(Vector3(0f, 0f, 1f), Vector3(0f, 0f, -1f))))
            .isWithin(1e-5f)
            .of(180f)
    }

    @Test
    fun angleBetweenVectors_returnsAngleBetweenTwoVectors3() {
        assertThat(toDegrees(Vector3.angleBetween(Vector3(2f, 4f, 0f), Vector3(4f, 8f, 0f))))
            .isWithin(1e-5f)
            .of(0f)
    }

    @Test
    fun angleBetweenVectors_returnsAngleBetweenTwoVectors4() {
        assertThat(toDegrees(Vector3.angleBetween(Vector3(2f, 2f, 0f), Vector3(0f, 3f, 0f))))
            .isWithin(1e-5f)
            .of(45f)
    }

    @Test
    fun lerp_returnsInterpolatedVector1() {
        val underTest = Vector3.lerp(Vector3(1f, 2f, 3f), Vector3(4f, 5f, 6f), 0.5f)

        assertThat(underTest.x).isWithin(1e-5f).of(2.5f)
        assertThat(underTest.y).isWithin(1e-5f).of(3.5f)
        assertThat(underTest.z).isWithin(1e-5f).of(4.5f)
    }

    @Test
    fun lerp_returnsInterpolatedVector2() {
        val underTest = Vector3.lerp(Vector3(4f, 5f, 6f), Vector3(12f, 15f, 18f), 0.25f)

        assertThat(underTest.x).isWithin(1e-5f).of(6f)
        assertThat(underTest.y).isWithin(1e-5f).of(7.5f)
        assertThat(underTest.z).isWithin(1e-5f).of(9f)
    }

    @Test
    fun lerp_returnsInterpolatedVector3() {
        val underTest = Vector3.lerp(Vector3(2f, 6f, 10f), Vector3(12f, 26f, 30f), 0.4f)

        assertThat(underTest.x).isWithin(1e-5f).of(6f)
        assertThat(underTest.y).isWithin(1e-5f).of(14f)
        assertThat(underTest.z).isWithin(1e-5f).of(18f)
    }

    @Test
    fun unaryMinus_returnsVectorWithNegativeValues() {
        val underTest = Vector3(1f, 2f, 3f)
        val underTestNegative = -underTest

        assertThat(underTestNegative).isEqualTo(Vector3(-1f, -2f, -3f))
    }

    @Test
    fun div_returnsVectorDividedByScalar() {
        val underTest = Vector3(1f, -2f, 3f)
        val underTestDiv = underTest / -2f

        assertThat(underTestDiv).isEqualTo(Vector3(-0.5f, 1f, -1.5f))
    }

    @Test
    fun componentwiseDivision_returnsVectorDividedByVector() {
        val underTest = Vector3(1f, 2f, 6f)
        val underTest2 = Vector3(-2f, 4f, -3f)
        val underTestDiv = underTest.scale(underTest2.inverse())

        assertThat(underTestDiv).isEqualTo(Vector3(-0.5f, 0.5f, -2f))
    }

    @Test
    fun inverse_nonZeroComponentVector_returnsInverseVector() {
        val underTest = Vector3(2f, 3f, 4f)
        val underTestInverse = underTest.inverse()

        assertThat(underTestInverse).isEqualTo(Vector3(1 / 2f, 1 / 3f, 1 / 4f))
    }

    @Test
    fun inverse_zeroComponent_returnsIllegalArgumentException() {
        val underTest = Vector3(0f, 1f, 2f)

        assertThrows(IllegalArgumentException::class.java) { underTest.inverse() }
    }

    @Test
    fun projectOnPlane_returnsVectorProjectedOnPlane() {
        val underTest = Vector3(1f, 2f, 3f)
        val underTest2 = Vector3(1f, 2f, 0f)
        val underTestProject = Vector3.projectOnPlane(underTest, underTest2)

        assertThat(underTestProject).isEqualTo(Vector3(0f, 0f, 3f))
    }

    @Test
    fun distance_returnsDistanceBetweenTwoVectors() {
        val underTest = Vector3(1f, 0f, 1f)
        val underTest2 = Vector3(2f, 2f, 5f)
        val underTestDistance = Vector3.distance(underTest, underTest2)

        assertThat(underTestDistance).isWithin(1.0e-4f).of(4.58257569f) // sqrt(1^2 + 2^2 + 4^2)
    }

    @Test
    fun abs_returnsVectorWithAbsoluteValues() {
        val underTest = Vector3.abs(Vector3(-1f, 2f, -3f))

        assertThat(underTest).isEqualTo(Vector3(1f, 2f, 3f))
    }

    @Test
    fun maxVector_returnsMaxVectorFromTwoVectors() {
        val underTest = Vector3(8f, 2f, -3f)
        val underTest2 = Vector3(4f, 5f, 6f)
        val underTestMax = Vector3.max(underTest, underTest2)

        assertThat(underTestMax).isEqualTo(Vector3(8f, 5f, 6f))
    }

    @Test
    fun minVector_returnsMinVectorFromTwoVectors() {
        val underTest = Vector3(8f, 2f, -3f)
        val underTest2 = Vector3(4f, 5f, 6f)
        val underTestMin = Vector3.min(underTest, underTest2)

        assertThat(underTestMin).isEqualTo(Vector3(4f, 2f, -3f))
    }

    @Test
    fun copy_returnsCopyOfVector() {
        val underTest = Vector3(1f, 2f, 3f)
        val underTestCopy = underTest.copy()

        assertThat(underTestCopy).isEqualTo(underTest)
    }
}
