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
class Vector2Test {
    @Test
    fun constructor_noArguments_returnsZeroVector() {
        val underTest = Vector2()

        assertThat(underTest.x).isEqualTo(0)
        assertThat(underTest.y).isEqualTo(0)
    }

    @Test
    fun equals_sameValues_returnsTrue() {
        val underTest = Vector2(1f, 2f)
        val underTest2 = Vector2(1f, 2f)

        assertThat(underTest).isEqualTo(underTest2)
    }

    @Test
    fun equals_differentValues_returnsFalse() {
        val underTest = Vector2(1f, 2f)
        val underTest2 = Vector2(9f, 10f)
        val underTest3 = Vector3()

        assertThat(underTest).isNotEqualTo(underTest2)
        assertThat(underTest).isNotEqualTo(underTest3)
    }

    @Test
    fun hashCodeEquals_sameValues_returnsTrue() {
        val underTest = Vector2(1f, 2f)
        val underTest2 = Vector2(1f, 2f)

        assertThat(underTest.hashCode()).isEqualTo(underTest2.hashCode())
    }

    @Test
    fun hashCodeEquals_differentValues_returnsFalse() {
        val underTest = Vector2(1f, 2f)
        val underTest2 = Vector2(9f, 10f)
        val underTest3 = Vector2()

        assertThat(underTest.hashCode()).isNotEqualTo(underTest2.hashCode())
        assertThat(underTest.hashCode()).isNotEqualTo(underTest3.hashCode())
    }

    @Test
    fun constructorEquals_expectedToString_returnsTrue() {
        val underTest = Vector2(1f, 2f)
        val underTest2 = Vector2()

        assertThat(underTest.toString()).isEqualTo("[x=1.0, y=2.0]")
        assertThat(underTest2.toString()).isEqualTo("[x=0.0, y=0.0]")
    }

    @Test
    fun constructor_fromVector2_returnsSameValues() {
        val underTest = Vector2(1f, 2f)
        val underTest2 = underTest

        assertThat(underTest).isEqualTo(underTest2)
    }

    @Test
    fun normalized_returnsVectorWithUnitLength() {
        assertThat(Vector2(3f, 4f).toNormalized()).isEqualTo(Vector2(0.6f, 0.8f))
        assertThat(Vector2(1f, 1f).toNormalized()).isEqualTo(Vector2(0.70710677f, 0.70710677f))
    }

    @Test
    fun multiply_returnsVectorScaledByScalar() {
        assertThat(Vector2(3f, 4f) * 2f).isEqualTo(Vector2(6f, 8f))
        assertThat(Vector2(1f, 1f) * 0.5f).isEqualTo(Vector2(0.5f, 0.5f))
    }

    @Test
    fun add_returnsTwoVectorsAddedTogether() {
        val underTest = Vector2(1f, 2f)
        val underTest2 = Vector2(3f, 4f)
        val underTestAdd = underTest + underTest2

        assertThat(underTestAdd).isEqualTo(Vector2(4f, 6f))
    }

    @Test
    fun subtract_returnsTwoVectorsSubtracted() {
        val underTest = Vector2(1f, 5f)
        val underTest2 = Vector2(3f, 4f)
        val underTestSubtract = underTest - underTest2

        assertThat(underTestSubtract).isEqualTo(Vector2(-2f, 1f))
    }

    @Test
    fun scale_returnsTwoVectorsMultiplied() {
        val underTest = Vector2(1f, 2f)
        val underTest2 = Vector2(3f, 4f)
        val underTestMultiply = underTest.scale(underTest2)

        assertThat(underTestMultiply).isEqualTo(Vector2(3f, 8f))
    }

    @Test
    fun cross_returnsCrossProductOfTwoVectors() {
        val underTest = Vector2(1f, 2f)
        val underTest2 = Vector2(3f, 4f)
        val underTestCross = underTest cross underTest2

        assertThat(underTestCross).isEqualTo(-2f)
    }

    @Test
    fun dot_returnsDotProductOfTwoVectors() {
        assertThat(Vector2(1f, 2f) dot Vector2(3f, 4f)).isEqualTo(11f)
        assertThat(Vector2(-1f, 2f) dot Vector2(1f, 10f)).isEqualTo(19f)
    }

    @Test
    fun clamp_returnsVectorClampedBetweenTwoVectors1() {
        val underTest = Vector2(3f, 4f).clamp(Vector2(5f, 6f), Vector2(7f, 8f))

        assertThat(underTest).isEqualTo(Vector2(5f, 6f))
    }

    @Test
    fun clamp_returnsVectorClampedBetweenTwoVectors2() {
        val underTest = Vector2(3f, 4f).clamp(Vector2(1f, 2f), Vector2(5f, 6f))

        assertThat(underTest).isEqualTo(Vector2(3f, 4f))
    }

    @Test
    fun clamp_returnsVectorClampedBetweenTwoVectors3() {
        val underTest = Vector2(5f, 6f).clamp(Vector2(1f, 2f), Vector2(3f, 4f))

        assertThat(underTest).isEqualTo(Vector2(3f, 4f))
    }

    @Test
    fun unaryMinus_returnsVectorWithNegativeValues() {
        val underTest = Vector2(1f, 2f)
        val underTestNegative = -underTest

        assertThat(underTestNegative).isEqualTo(Vector2(-1f, -2f))
    }

    @Test
    fun div_returnsVectorDividedByScalar() {
        val underTest = Vector2(1f, -2f)
        val underTestDiv = underTest / -2f

        assertThat(underTestDiv).isEqualTo(Vector2(-0.5f, 1f))
    }

    @Test
    fun componentwiseDivision_returnsVectorDividedByVector() {
        val underTest = Vector2(1f, 2f)
        val underTest2 = Vector2(-2f, 4f)
        val underTestDiv = underTest.scale(underTest2.inverse())

        assertThat(underTestDiv).isEqualTo(Vector2(-0.5f, 0.5f))
    }

    @Test
    fun inverse_nonZeroComponentVector_returnsInverseVector() {
        val underTest = Vector2(2f, 3f)
        val underTestInverse = underTest.inverse()

        assertThat(underTestInverse).isEqualTo(Vector2(1 / 2f, 1 / 3f))
    }

    @Test
    fun inverse_zeroComponent_returnsIllegalArgumentException() {
        val underTest = Vector2(0f, 1f)

        assertThrows(IllegalArgumentException::class.java) { underTest.inverse() }
    }

    @Test
    fun distance_returnsDistanceBetweenTwoVectors() {
        val underTest = Vector2(1f, 0f)
        val underTest2 = Vector2(2f, 2f)
        val underTestDistance = Vector2.distance(underTest, underTest2)

        assertThat(underTestDistance).isEqualTo(2.2360679775f) // sqrt(1^2 + 2^2)
    }

    @Test
    fun copy_returnsCopyOfVector() {
        val underTest = Vector2(1f, 2f)
        val underTestCopy = underTest.copy()

        assertThat(underTestCopy).isEqualTo(underTest)
    }

    @Test
    fun angularDistance_returnsAngleBetweenTwoVectors1() {
        assertThat(Vector2.angularDistance(Vector2(1f, 0f), Vector2(0f, 1f)))
            .isWithin(1e-5f)
            .of(90f)
    }

    @Test
    fun angularDistance_returnsAngleBetweenTwoVectors2() {
        assertThat(Vector2.angularDistance(Vector2(1f, 0f), Vector2(-1f, 0f)))
            .isWithin(1e-5f)
            .of(180f)
    }

    @Test
    fun angularDistance_returnsAngleBetweenTwoVectors3() {
        assertThat(Vector2.angularDistance(Vector2(2f, 4f), Vector2(4f, 8f))).isWithin(1e-5f).of(0f)
    }

    @Test
    fun angularDistance_returnsAngleBetweenTwoVectors4() {
        assertThat(Vector2.angularDistance(Vector2(2f, 2f), Vector2(0f, 3f)))
            .isWithin(1e-5f)
            .of(45f)
    }

    @Test
    fun lerp_returnsInterpolatedVector1() {
        val underTest = Vector2.lerp(Vector2(1f, 2f), Vector2(2f, 4f), 0.5f)

        assertThat(underTest.x).isWithin(1e-5f).of(1.5f)
        assertThat(underTest.y).isWithin(1e-5f).of(3f)
    }

    @Test
    fun lerp_returnsInterpolatedVector2() {
        val underTest = Vector2.lerp(Vector2(4f, 5f), Vector2(12f, 15f), 0.25f)

        assertThat(underTest.x).isWithin(1e-5f).of(6f)
        assertThat(underTest.y).isWithin(1e-5f).of(7.5f)
    }

    @Test
    fun lerp_returnsInterpolatedVector3() {
        val underTest = Vector2.lerp(Vector2(2f, 6f), Vector2(12f, 26f), 0.4f)

        assertThat(underTest.x).isWithin(1e-5f).of(6f)
        assertThat(underTest.y).isWithin(1e-5f).of(14f)
    }

    @Test
    fun abs_returnsAbsoluteValueofVector2() {
        val underTest = Vector2.abs(Vector2(-3f, 4f))

        assertThat(underTest.x).isEqualTo(3f)
        assertThat(underTest.y).isEqualTo(4f)
    }
}
