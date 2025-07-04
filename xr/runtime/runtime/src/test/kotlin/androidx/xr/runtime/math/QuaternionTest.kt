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
import kotlin.math.sqrt
import org.junit.Test
import org.junit.runner.RunWith
import org.junit.runners.JUnit4

@RunWith(JUnit4::class)
class QuaternionTest {

    @Test
    fun constructor_noArguments_returnsIdentityQuaternion() {
        val underTest = Quaternion()
        assertRotation(underTest, 0f, 0f, 0f, 1f)

        val underTest2 = Quaternion.Identity
        assertRotation(underTest2, 0f, 0f, 0f, 1f)
    }

    @Test
    fun equals_sameValues_returnsTrue() {
        val underTest = Quaternion(1f, 2f, 3f, 4f)
        val underTest2 = Quaternion(1f, 2f, 3f, 4f)

        assertThat(underTest).isEqualTo(underTest2)
    }

    @Test
    fun equals_differentValues_returnsFalse() {
        val underTest = Quaternion(1f, 2f, 3f, 4f)
        val underTest2 = Quaternion(9f, 10f, 11f, 12f)
        val underTest3 = Vector2()

        assertThat(underTest).isNotEqualTo(underTest2)
        assertThat(underTest).isNotEqualTo(underTest3)
    }

    @Test
    fun hashCodeEquals_sameValues_returnsTrue() {
        val underTest = Quaternion(1f, 2f, 3f, 4f)
        val underTest2 = Quaternion(1f, 2f, 3f, 4f)

        assertThat(underTest.hashCode()).isEqualTo(underTest2.hashCode())
    }

    @Test
    fun hashCodeEquals_differentValues_returnsFalse() {
        val underTest = Quaternion(1f, 2f, 3f, 4f)
        val underTest2 = Quaternion(9f, 10f, 11f, 12f)
        val underTest3 = Quaternion()

        assertThat(underTest.hashCode()).isNotEqualTo(underTest2.hashCode())
        assertThat(underTest.hashCode()).isNotEqualTo(underTest3.hashCode())
    }

    @Test
    fun constructorEquals_expectedToString_returnsTrue() {
        val underTest = Quaternion(0f, 1f, 2f, 0f)
        val underTest2 = Quaternion()

        assertThat(underTest.toString()).isEqualTo("[x=0.0, y=0.4472136, z=0.8944272, w=0.0]")
        assertThat(underTest2.toString()).isEqualTo("[x=0.0, y=0.0, z=0.0, w=1.0]")
    }

    @Test
    fun constructor_fromQuaternion_returnsSameValues() {
        val underTest = Quaternion(1f, 2f, 3f, 4f)
        val underTest2 = Quaternion(underTest)

        assertRotation(underTest, underTest2.x, underTest2.y, underTest2.z, underTest2.w)
    }

    @Test
    fun constructor_fromVector4_returnsSameValues() {
        val underTest = Vector4(1f, 2f, 3f, 4f)
        val underTest2 = Quaternion(underTest)
        val length =
            sqrt(
                underTest.x * underTest.x +
                    underTest.y * underTest.y +
                    underTest.z * underTest.z +
                    underTest.w * underTest.w
            )

        assertRotation(
            underTest2,
            underTest.x / length,
            underTest.y / length,
            underTest.z / length,
            underTest.w / length,
        )
    }

    @Test
    fun axisAngle_returnsQuaternionFromAxis() {
        val underTestAxis = Vector3(1f, 0f, 0f)
        val degrees = 180f

        // angle = 1/2 * 180 * pi / 180 = 1/2 * pi
        // sin(angle) = sin(1/2 pi) = 1
        // cos(angle) = cos(1/2 pi) = 0
        // returns Quaternion(1 * 1, 1 * 0, 1 * 0, 0)
        val underTestAxisAngle = Quaternion.fromAxisAngle(underTestAxis, degrees)

        assertRotation(underTestAxisAngle, 1f, 0f, 0f, 0f)
    }

    @Test
    fun quaternion_fromEulerAngles1() {
        val eulerAngles = Vector3(180f, 0f, 0f)
        val underTestYawPitchRoll = Quaternion.fromEulerAngles(eulerAngles)

        assertRotation(underTestYawPitchRoll, 1f, 0f, 0f, 0f)
    }

    @Test
    fun quaternion_fromEulerAngles2() {
        val eulerAngles = Vector3(0f, 0f, 0f)
        val underTestYawPitchRoll = Quaternion.fromEulerAngles(eulerAngles)

        assertRotation(underTestYawPitchRoll, 0f, 0f, 0f, 1f)
    }

    @Test
    fun quaternion_fromEulerAngles3() {
        val eulerAngles = Vector3(90f, 0f, 0f)
        val underTestYawPitchRoll = Quaternion.fromEulerAngles(eulerAngles)

        assertRotation(underTestYawPitchRoll, 0.7071f, 0f, 0f, 0.7071f)
    }

    @Test
    fun quaternion_fromEulerAngles4() {
        val eulerAngles = Vector3(0f, 180f, 0f)
        val underTestYawPitchRoll = Quaternion.fromEulerAngles(eulerAngles)

        assertRotation(underTestYawPitchRoll, 0f, 1f, 0f, 0f)
    }

    @Test
    fun quaternion_fromPitchYawRoll1() {
        val underTestYawPitchRoll = Quaternion.fromEulerAngles(180f, 0f, 0f)

        assertRotation(underTestYawPitchRoll, 1f, 0f, 0f, 0f)
    }

    @Test
    fun quaternion_fromPitchYawRoll2() {
        val underTestYawPitchRoll = Quaternion.fromEulerAngles(0f, 0f, 0f)

        assertRotation(underTestYawPitchRoll, 0f, 0f, 0f, 1f)
    }

    @Test
    fun quaternion_fromPitchYawRoll3() {
        val underTestYawPitchRoll = Quaternion.fromEulerAngles(90f, 0f, 0f)

        assertRotation(underTestYawPitchRoll, 0.7071f, 0f, 0f, 0.7071f)
    }

    @Test
    fun quaternion_fromPitchYawRoll4() {
        val underTestYawPitchRoll = Quaternion.fromEulerAngles(0f, 180f, 0f)

        assertRotation(underTestYawPitchRoll, 0f, 1f, 0f, 0f)
    }

    @Test
    fun normalized_returnsQuaternionNormalized() {
        val underTest = Quaternion(1f, 2f, 3f, 4f)
        val underTestNormalized = underTest.toNormalized()

        assertRotation(
            underTest,
            underTestNormalized.x,
            underTestNormalized.y,
            underTestNormalized.z,
            underTestNormalized.w,
        )
    }

    @Test
    fun inverted_returnsQuaternionInverted() {
        val underTest = Quaternion(1f, 2f, 3f, 4f)
        val underTestInverted = underTest.inverse

        assertRotation(
            underTestInverted,
            -1 * underTest.x,
            -1 * underTest.y,
            -1 * underTest.z,
            1 * underTest.w,
        )
    }

    @Test
    fun unaryMinus_returnsQuaternionWithSignsFlipped() {
        val underTest = Quaternion(1f, -2f, 3f, -4f)
        val underTestUnary = -underTest

        assertRotation(
            underTestUnary,
            -1 * underTest.x,
            -1 * underTest.y,
            -1 * underTest.z,
            -1 * underTest.w,
        )
    }

    @Test
    fun rotateVector_returnsVector3RotatedByQuaternion() {
        val underTest = Quaternion(1f, 2f, 3f, 4f)
        val vector = Vector3(1f, 2f, 3f)
        val underTestRotated = underTest * vector

        assertThat(underTestRotated.x).isWithin(1e-5f).of(1f)
        assertThat(underTestRotated.y).isWithin(1e-5f).of(2f)
        assertThat(underTestRotated.z).isWithin(1e-5f).of(3f)
    }

    @Test
    fun times_returnsTwoQuaternionsMultiplied() {
        val underTest = Quaternion(1f, 2f, 3f, 4f)
        val underTest2 = Quaternion(5f, -6f, 7f, -8f)
        val underTestTimes = underTest * underTest2

        assertRotation(underTestTimes, 0.6090002f, -0.442909f, -0.16609f, -0.63668f)
    }

    @Test
    fun div_returnsQuaternionDividedByScalar() {
        val underTest = Quaternion(0f, 0f, 0f, 1f)
        val underTestDiv = underTest / 2f

        assertRotation(underTestDiv, 0f, 0f, 0f, 1f)
    }

    @Test
    fun interpolate_returnsInterpolatedQuaternion() {
        val underTest = Quaternion(1f, 2f, 3f, 4f)
        val underTest2 = Quaternion(2f, 4f, 6f, 8f)
        val underTestInterpolate = Quaternion.lerp(underTest, underTest2, 0.5f)

        assertRotation(underTestInterpolate, 0.18257418f, 0.36514837f, 0.5477226f, 0.73029673f)
    }

    @Test
    fun sphericalInterpolate_returnsQuaternionInterpolatedBetweenQuaternions() {
        val axis = Vector3(1f, 2f, 3f)
        val ratio = 0.5f
        val sourceAngle = 90f
        val destinationAngle = 180f
        val expectedAngle = sourceAngle + (destinationAngle - sourceAngle) * ratio
        val sourceQuaternion = Quaternion.fromAxisAngle(axis, sourceAngle)
        val destinationQuaternion = Quaternion.fromAxisAngle(axis, destinationAngle)
        val expected = Quaternion.fromAxisAngle(axis, expectedAngle)
        val slerpResult = Quaternion.slerp(sourceQuaternion, destinationQuaternion, ratio)

        assertRotation(slerpResult, expected.x, expected.y, expected.z, expected.w)
    }

    @Test
    fun sphericalInterpolate_smallAngle_returnsQuaternionInterpolatedBetweenQuaternions() {
        val axis = Vector3(1f, 2f, 3f)
        val ratio = 0.5f
        val sourceAngle = 90f
        val destinationAngle = 90.01f
        val expectedAngle = sourceAngle + (destinationAngle - sourceAngle) * ratio
        val sourceQuaternion = Quaternion.fromAxisAngle(axis, sourceAngle)
        val destinationQuaternion = Quaternion.fromAxisAngle(axis, destinationAngle)
        val expected = Quaternion.fromAxisAngle(axis, expectedAngle)
        val slerpResult = Quaternion.slerp(sourceQuaternion, destinationQuaternion, ratio)

        assertRotation(
            slerpResult,
            expected.x,
            expected.y,
            expected.z,
            expected.w,
            tolerance = 1.0e-5f,
        )
    }

    @Test
    fun sphericalInterpolate_ratioAboveOne_returnsQuaternionExtrapolatedOutsideQuaternions() {
        val axis = Vector3(1f, 2f, 3f)
        val ratio = 2.0f
        val sourceAngle = 90f
        val destinationAngle = 100f
        val expectedAngle = sourceAngle + (destinationAngle - sourceAngle) * ratio
        val sourceQuaternion = Quaternion.fromAxisAngle(axis, sourceAngle)
        val destinationQuaternion = Quaternion.fromAxisAngle(axis, destinationAngle)
        val expected = Quaternion.fromAxisAngle(axis, expectedAngle)

        val slerpResult = Quaternion.slerp(sourceQuaternion, destinationQuaternion, ratio)

        assertRotation(slerpResult, expected.x, expected.y, expected.z, expected.w)
    }

    @Test
    fun sphericalInterpolate_ratioBelowZero_returnsQuaternionExtrapolatedOutsideQuaternions() {
        val axis = Vector3(1f, 2f, 3f)
        val ratio = -2.0f
        val sourceAngle = 90f
        val destinationAngle = 100f
        val expectedAngle = sourceAngle + (destinationAngle - sourceAngle) * ratio
        val sourceQuaternion = Quaternion.fromAxisAngle(axis, sourceAngle)
        val destinationQuaternion = Quaternion.fromAxisAngle(axis, destinationAngle)
        val expected = Quaternion.fromAxisAngle(axis, expectedAngle)
        val slerpResult = Quaternion.slerp(sourceQuaternion, destinationQuaternion, ratio)

        assertRotation(slerpResult, expected.x, expected.y, expected.z, expected.w)
    }

    @Test
    fun sphericalInterpolate_RatioZero_returnsSourceQuaternion() {
        val sourceQuaternion = Quaternion(1f, 2f, 3f, 4f)
        val destinationQuaternion = Quaternion(2f, 5f, 8f, 23f)
        val slerpResult = Quaternion.slerp(sourceQuaternion, destinationQuaternion, 0.0f)
        val expected = sourceQuaternion // The source quaternion should be returned at ratio 0.

        assertRotation(slerpResult, expected.x, expected.y, expected.z, expected.w)
    }

    @Test
    fun sphericalInterpolate_RatioOne_returnsDestinationQuaternion() {
        val axis = Vector3(1f, 2f, 3f)
        val sourceQuaternion = Quaternion.fromAxisAngle(axis, 90f)
        val destinationQuaternion = Quaternion.fromAxisAngle(axis, 180f)
        val slerpResult = Quaternion.slerp(sourceQuaternion, destinationQuaternion, 1.0f)
        val expected = destinationQuaternion // The destination quat should be returned at ratio 1.

        assertRotation(slerpResult, expected.x, expected.y, expected.z, expected.w)
    }

    @Test
    fun dot_returnsDotProductOfTwoQuaternions() {
        val underTest = Quaternion(1f, 2f, 3f, 4f)
        val underTest2 = Quaternion(1f, -2f, 3f, 2f)
        val underTestDot = underTest.dot(underTest2)
        val underTestDot2 = Quaternion.dot(underTest, underTest2)

        assertThat(underTestDot).isEqualTo(0.6024641f)
        assertThat(underTestDot2).isEqualTo(0.6024641f)
    }

    @Test
    fun eulerAngles_returnedFromQuaternion() {
        val underTest = Quaternion(1f, 2f, 3f, 4f)
        val yawPitchRollVector = underTest.eulerAngles

        assertThat(yawPitchRollVector.x).isWithin(1.0e-5f).of(-7.66226f)
        assertThat(yawPitchRollVector.y).isWithin(1.0e-5f).of(47.726307f)
        assertThat(yawPitchRollVector.z).isWithin(1.0e-5f).of(70.34616f)
    }

    @Test
    fun rotationBetweenQuaternions_returnsAngleBetweenTwoQuaternions1() {
        assertThat(Quaternion.angle(Quaternion(1f, 2f, 3f, 0f), Quaternion(1f, 2f, 3f, 0f)))
            .isEqualTo(0.055952907f)
    }

    @Test
    fun rotationBetweenQuaternions_returnsAngleBetweenTwoQuaternions2() {
        assertThat(Quaternion.angle(Quaternion(1f, 2f, 3f, 5f), Quaternion(-1f, -2f, -3f, 5f)))
            .isEqualTo(147.23466f)
    }

    @Test
    fun fromRotation_returnsQuaternionFromRotationBetweenTwoVectors1() {
        val underTest = Quaternion.fromRotation(Vector3(1f, 0f, 0f), Vector3(1f, 0f, 0f))

        assertRotation(underTest, 0f, 0f, 0f, 1f)
    }

    @Test
    fun fromRotation_returnsQuaternionFromRotationBetweenTwoVectors2() {
        val underTest = Quaternion.fromRotation(Vector3(1f, 0f, 0f), Vector3(-1f, 0f, 0f))

        assertRotation(underTest, 0f, 1f, 0f, 0f)
    }

    @Test
    fun fromRotation_returnsQuaternionFromRotationBetweenTwoVectors3() {
        val underTest = Quaternion.fromRotation(Vector3(1f, 0f, 0f), Vector3(0f, 1f, 0f))

        assertRotation(underTest, 0f, 0f, 0.7071f, 0.7071f)
    }

    @Test
    fun axisAngle_returnsAxisAngleOfQuaternion1() {
        assertAxisAngle(Quaternion(0f, 1f, 0f, 0f), Vector3(0f, 1f, 0f), 180f)
    }

    @Test
    fun axisAngle_returnsAxisAngleOfQuaternion2() {
        assertAxisAngle(Quaternion(0f, 0f, 0.7071f, 0.7071f), Vector3(0f, 0f, 1f), 90f)
    }

    @Test
    fun axisAngle_returnsAxisAngleOfQuaternion3() {
        assertAxisAngle(Quaternion(0f, 0f, 0f, 1f), Vector3(1f, 0f, 0f), 0f)
    }

    @Test
    fun axisAngle_returnsAxisAngleOfQuaternion4() {
        assertAxisAngle(Quaternion(0f, 0.7071f, 0f, 0.7071f), Vector3(0f, 1f, 0f), 90f)
    }

    private fun assertAxisAngle(rotation: Quaternion, axis: Vector3, angle: Float) {
        assertThat(rotation.axisAngle.first.x).isWithin(1.0e-5f).of(axis.x)
        assertThat(rotation.axisAngle.first.y).isWithin(1.0e-5f).of(axis.y)
        assertThat(rotation.axisAngle.first.z).isWithin(1.0e-5f).of(axis.z)
        assertThat(rotation.axisAngle.second).isWithin(1.0e-5f).of(angle)
    }

    @Test
    fun copy_returnsCopyOfQuaternion() {
        val underTest = Quaternion(1f, 2f, 3f, 4f)
        val underTestCopy = underTest.copy()

        assertRotation(underTestCopy, underTest.x, underTest.y, underTest.z, underTest.w)
    }

    @Test
    fun fromRotation_returnsQuaternionFromRotationBetweenTwoQuaternions1() {
        val resultantQuaternion =
            Quaternion.fromRotation(Quaternion(1f, 2f, 3f, 0f), Quaternion(1f, 2f, 3f, 0f))

        assertRotation(resultantQuaternion, 0f, 0f, 0f, 1f)
    }

    @Test
    fun fromRotation_returnsQuaternionFromRotationBetweenTwoQuaternions2() {
        val resultantQuaternion =
            Quaternion.fromRotation(
                Quaternion(0f, 0f, 0.7071f, 0.7071f),
                Quaternion(0f, 0f, 0.7071f, -0.7071f),
            )

        assertRotation(resultantQuaternion, 0f, 0f, 1f, 0f)
    }

    @Test
    fun fromRotation_returnsQuaternionFromRotationBetweenTwoQuaternions3() {
        val resultantQuaternion =
            Quaternion.fromRotation(
                Quaternion(0.7071f, 0f, 0f, 0.7071f),
                Quaternion(0f, 0.7071f, 0f, 0.7071f),
            )

        assertRotation(resultantQuaternion, -0.5f, 0.5f, 0.5f, 0.5f)
    }

    private fun assertRotation(
        rotation: Quaternion,
        expectedX: Float,
        expectedY: Float,
        expectedZ: Float,
        expectedW: Float,
        tolerance: Float = 1.0e-4f,
    ) {
        assertThat(rotation.x).isWithin(tolerance).of(expectedX)
        assertThat(rotation.y).isWithin(tolerance).of(expectedY)
        assertThat(rotation.z).isWithin(tolerance).of(expectedZ)
        assertThat(rotation.w).isWithin(tolerance).of(expectedW)
    }

    @Test
    fun fromLookTowards_returnsQuaternionFromForwardAndUpVectors() {
        val resultantQuaternion =
            Quaternion.fromLookTowards(Vector3(0f, 0f, 1f), Vector3(0f, 1f, 0f))

        assertRotation(resultantQuaternion, 0f, 0f, 0f, 1f)

        val resultantQuaternion2 =
            Quaternion.fromLookTowards(Vector3(1f, 0f, 0f), Vector3(0f, 1f, 0f))

        assertRotation(resultantQuaternion2, 0f, 0.707107f, 0f, 0.707107f)
    }
}
