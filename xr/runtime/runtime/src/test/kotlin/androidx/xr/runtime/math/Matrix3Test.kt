/*
 * Copyright 2025 The Android Open Source Project
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package androidx.xr.runtime.math

import androidx.test.ext.junit.runners.AndroidJUnit4
import com.google.common.truth.Truth.assertThat
import kotlin.math.sqrt
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class Matrix3Test {
    @Test
    fun constructorEquals_expectedToString_returnsTrue() {
        val underTest = Matrix3(floatArrayOf(1f, 2f, 3f, 4f, 5f, 6f, 7f, 8f, 9f))

        assertThat(underTest.toString())
            .isEqualTo(
                "\n[ " +
                    1.0f +
                    "\t" +
                    4.0f +
                    "\t" +
                    7.0f +
                    "\n  " +
                    2.0f +
                    "\t" +
                    5.0f +
                    "\t" +
                    8.0f +
                    "\n  " +
                    3.0f +
                    "\t" +
                    6.0f +
                    "\t" +
                    9.0f +
                    " ]"
            )
    }

    @Test
    fun constructor_fromMatrix3_returnsSameValues() {
        val original = Matrix3(floatArrayOf(1f, 2f, 3f, 4f, 5f, 6f, 7f, 8f, 9f))
        val copy = Matrix3(original)

        assertThat(copy.data.size).isEqualTo(9)
        assertThat(original.data.size).isEqualTo(9)
        for (i in copy.data.indices) {
            assertThat(copy.data[i]).isEqualTo(original.data[i])
        }
        assertThat(copy.data).isNotSameInstanceAs(original.data)
    }

    @Test
    fun equals_sameValues_returnsTrue() {
        val underTest1 = Matrix3(floatArrayOf(1f, 2f, 3f, 4f, 5f, 6f, 7f, 8f, 9f))
        val underTest2 = Matrix3(floatArrayOf(1f, 2f, 3f, 4f, 5f, 6f, 7f, 8f, 9f))

        assertThat(underTest1).isEqualTo(underTest2)
    }

    @Test
    fun equals_differentValues_returnsFalse() {
        val underTest1 = Matrix3(floatArrayOf(1f, 2f, 3f, 4f, 5f, 6f, 7f, 8f, 9f))
        val underTest2 = Matrix3(floatArrayOf(9f, 8f, 7f, 6f, 5f, 4f, 3f, 2f, 1f))

        assertThat(underTest1).isNotEqualTo(underTest2)
    }

    @Test
    fun equals_differentObjects_returnsFalse() {
        val underTest = Matrix3(floatArrayOf(1f, 2f, 3f, 4f, 5f, 6f, 7f, 8f, 9f))
        val differentObject = Vector3()

        assertThat(underTest.equals(differentObject)).isFalse()
    }

    @Test
    fun hashCode_sameValues_returnsTrue() {
        val underTest1 = Matrix3(floatArrayOf(1f, 2f, 3f, 4f, 5f, 6f, 7f, 8f, 9f))
        val underTest2 = Matrix3(floatArrayOf(1f, 2f, 3f, 4f, 5f, 6f, 7f, 8f, 9f))

        assertThat(underTest1.hashCode()).isEqualTo(underTest2.hashCode())
    }

    @Test
    fun hashCode_differentValues_returnsFalse() {
        val underTest1 = Matrix3(floatArrayOf(1f, 2f, 3f, 4f, 5f, 6f, 7f, 8f, 9f))
        val underTest2 = Matrix3(floatArrayOf(9f, 8f, 7f, 6f, 5f, 4f, 3f, 2f, 1f))

        assertThat(underTest1.hashCode()).isNotEqualTo(underTest2.hashCode())
    }

    @Test
    fun data_returnsFloatArrayOfMatrixComponents() {
        val inputArray = floatArrayOf(1f, 2f, 3f, 4f, 5f, 6f, 7f, 8f, 9f)
        val matrix = Matrix3(inputArray)
        val outputData = matrix.data

        assertThat(outputData).isEqualTo(inputArray)
        assertThat(outputData).isNotSameInstanceAs(inputArray)
    }

    @Test
    fun copy_returnsCopyOfMatrix() {
        val original = Matrix3(floatArrayOf(1f, 2f, 3f, 4f, 5f, 6f, 7f, 8f, 9f))
        val copy = original.copy()

        assertThat(copy).isEqualTo(original)
        assertThat(copy).isNotSameInstanceAs(original)
        assertThat(copy.data).isNotSameInstanceAs(original.data)
    }

    @Test
    fun identity_is() {
        val expected = Matrix3(floatArrayOf(1f, 0f, 0f, 0f, 1f, 0f, 0f, 0f, 1f))

        assertThat(Matrix3.IDENTITY).isEqualTo(expected)
    }

    @Test
    fun zero_is() {
        val expected = Matrix3(floatArrayOf(0f, 0f, 0f, 0f, 0f, 0f, 0f, 0f, 0f))

        assertThat(Matrix3.ZERO).isEqualTo(expected)
    }

    @Test
    fun inverse_returnsInverseMatrix1() {
        val identity = Matrix3.IDENTITY

        assertThat(identity.inverse).isEqualTo(identity)
    }

    @Test
    fun inverse_returnsInverseMatrix2() {
        val underTest = Matrix3(floatArrayOf(2f, 0f, 0f, 0f, 0.5f, 0f, 0f, 0f, 4f))
        val expected = Matrix3(floatArrayOf(0.5f, 0f, 0f, 0f, 2f, 0f, 0f, 0f, 0.25f))
        val actualInverse = underTest.inverse

        assertThat(actualInverse.data.size).isEqualTo(expected.data.size)
        for (i in actualInverse.data.indices) {
            assertThat(actualInverse.data[i]).isWithin(1e-5f).of(expected.data[i])
        }
    }

    @Test
    fun inverse_returnsInverseMatrix3() {
        val nonInvertible = Matrix3(floatArrayOf(1f, 2f, 3f, 1f, 2f, 3f, 1f, 2f, 3f))
        assertThat(nonInvertible.inverse).isEqualTo(Matrix3.ZERO)

        val nonInvertible2 = Matrix3(floatArrayOf(1f, 1f, 1f, 2f, 2f, 2f, 3f, 3f, 3f))
        assertThat(nonInvertible2.inverse).isEqualTo(Matrix3.ZERO)
    }

    @Test
    fun transpose_returnsTransposeMatrix1() {
        val identity = Matrix3.IDENTITY

        assertThat(identity.transpose).isEqualTo(identity)
    }

    @Test
    fun transpose_returnsTransposeMatrix2() {
        val underTest = Matrix3(floatArrayOf(1f, 2f, 3f, 4f, 5f, 6f, 7f, 8f, 9f))
        val expected = Matrix3(floatArrayOf(1f, 4f, 7f, 2f, 5f, 8f, 3f, 6f, 9f))

        assertThat(underTest.transpose).isEqualTo(expected)
    }

    @Test
    fun scale_returnsScaleVector1() {
        val identity = Matrix3.IDENTITY
        val scale = identity.scale

        assertThat(scale).isEqualTo(Vector3(1f, 1f, 1f))
    }

    @Test
    fun scale_returnsScaleVector2() {
        val scaleVec = Vector3(2f, -3f, 4f)
        val underTest = Matrix3.fromScale(scaleVec)
        val extractedScale = underTest.scale

        assertThat(extractedScale).isEqualTo(Vector3(-2f, -3f, -4f))
    }

    @Test
    fun rotation_returnsRotationQuaternion1() {
        val identity = Matrix3.IDENTITY
        val extractedRotation = identity.rotation
        val expectedQuaternion = Quaternion(0f, 0f, 0f, 1f)

        assertThat(extractedRotation.x).isWithin(1e-5f).of(expectedQuaternion.x)
        assertThat(extractedRotation.y).isWithin(1e-5f).of(expectedQuaternion.y)
        assertThat(extractedRotation.z).isWithin(1e-5f).of(expectedQuaternion.z)
        assertThat(extractedRotation.w).isWithin(1e-5f).of(expectedQuaternion.w)
    }

    @Test
    fun rotation_returnsRotationQuaternion2() {
        val expectedQuaternion = Quaternion(0f, 0f, sqrt(0.5f), sqrt(0.5f))
        val rotationMatrix = Matrix3.fromQuaternion(expectedQuaternion)
        val extractedRotation = rotationMatrix.rotation

        assertThat(extractedRotation.x).isWithin(1e-5f).of(expectedQuaternion.x)
        assertThat(extractedRotation.y).isWithin(1e-5f).of(expectedQuaternion.y)
        assertThat(extractedRotation.z).isWithin(1e-5f).of(expectedQuaternion.z)
        assertThat(extractedRotation.w).isWithin(1e-5f).of(expectedQuaternion.w)
    }

    @Test
    fun rotation_matrixWithNonUniformScale_returnsUnscaledRotation() {
        val expectedQuaternion = Quaternion.fromAxisAngle(Vector3(0f, 1f, 0f), 45f)
        // Build M = R * S so each column is scaled by the corresponding factor; rotation extraction
        // must divide the scale back out. Uses positive non-uniform scale because the per-axis sign
        // heuristic in scale() only recovers positive scales reliably (See [Matrix3#scale]).
        val scaledMatrix =
            Matrix3.fromQuaternion(expectedQuaternion) * Matrix3.fromScale(Vector3(2f, 3f, 4f))
        val extractedRotation = scaledMatrix.rotation

        assertThat(extractedRotation.x).isWithin(1e-5f).of(expectedQuaternion.x)
        assertThat(extractedRotation.y).isWithin(1e-5f).of(expectedQuaternion.y)
        assertThat(extractedRotation.z).isWithin(1e-5f).of(expectedQuaternion.z)
        assertThat(extractedRotation.w).isWithin(1e-5f).of(expectedQuaternion.w)
    }

    @Test
    fun rotation_halfTurnAboutEachAxis_returnsUnreflectedRotation() {
        // 180-degree rotations produce a rotation matrix with two negative diagonal entries, so the
        // per-axis sign(diagonal) heuristic in scale() reports a negative scale on those axes.
        // Dividing the basis columns by that signed scale (rather than its magnitude) would flip
        // the columns and collapse the rotation toward identity. These cases lock in that the
        // extraction divides by magnitude and recovers the true half-turn.
        for (axis in listOf(Vector3(1f, 0f, 0f), Vector3(0f, 1f, 0f), Vector3(0f, 0f, 1f))) {
            val expectedQuaternion = Quaternion.fromAxisAngle(axis, 180f)
            val extractedRotation = Matrix3.fromQuaternion(expectedQuaternion).rotation

            // A quaternion and its negation represent the same rotation; compare so that either
            // sign passes by aligning to the expected quaternion's hemisphere via the dot product.
            val sign = if ((extractedRotation dot expectedQuaternion) < 0f) -1f else 1f

            assertThat(sign * extractedRotation.x).isWithin(1e-5f).of(expectedQuaternion.x)
            assertThat(sign * extractedRotation.y).isWithin(1e-5f).of(expectedQuaternion.y)
            assertThat(sign * extractedRotation.z).isWithin(1e-5f).of(expectedQuaternion.z)
            assertThat(sign * extractedRotation.w).isWithin(1e-5f).of(expectedQuaternion.w)
        }
    }

    @Test
    fun rotation_withZeroScale_doesNotReturnNaN() {
        val zeroScaleMatrix = Matrix3.fromScale(0f)
        val extractedRotation = zeroScaleMatrix.rotation

        assertThat(extractedRotation.x.isNaN()).isFalse()
        assertThat(extractedRotation.y.isNaN()).isFalse()
        assertThat(extractedRotation.z.isNaN()).isFalse()
        assertThat(extractedRotation.w.isNaN()).isFalse()
    }

    @Test
    fun isTrs_identityMatrix_returnsTrue() {
        assertThat(Matrix3.IDENTITY.isTrs).isTrue()
    }

    @Test
    fun isTrs_zeroMatrix_returnsFalse() {
        assertThat(Matrix3.ZERO.isTrs).isFalse()
    }

    @Test
    fun isTrs_nonInvertibleMatrix_returnsFalse() {
        val nonInvertible = Matrix3(floatArrayOf(1f, 1f, 1f, 2f, 2f, 2f, 3f, 3f, 3f))

        assertThat(nonInvertible.isTrs).isFalse()
    }

    @Test
    fun isTrs_invertibleMatrix_returnsTrue() {
        val invertible = Matrix3.fromScale(Vector3(1f, 2f, 3f))

        assertThat(invertible.isTrs).isTrue()
    }

    @Test
    fun multiply_returnsMultipliedMatrix1() {
        val matrix = Matrix3(floatArrayOf(1f, 2f, 3f, 4f, 5f, 6f, 7f, 8f, 9f))

        assertThat(matrix * Matrix3.IDENTITY).isEqualTo(matrix)
    }

    @Test
    fun multiply_returnsMultipliedMatrix2() {
        val matrix = Matrix3(floatArrayOf(1f, 2f, 3f, 4f, 5f, 6f, 7f, 8f, 9f))

        assertThat(Matrix3.IDENTITY * matrix).isEqualTo(matrix)
    }

    @Test
    fun fromScale_returnsScaleMatrix1() {
        val scale = Vector3(2f, -3f, 0.5f)
        val expected = Matrix3(floatArrayOf(2f, 0f, 0f, 0f, -3f, 0f, 0f, 0f, 0.5f))

        assertThat(Matrix3.fromScale(scale)).isEqualTo(expected)
    }

    @Test
    fun fromScale_returnsScaleMatrix2() {
        val scale = 2.5f
        val expected = Matrix3(floatArrayOf(2.5f, 0f, 0f, 0f, 2.5f, 0f, 0f, 0f, 2.5f))

        assertThat(Matrix3.fromScale(scale)).isEqualTo(expected)
    }
}
