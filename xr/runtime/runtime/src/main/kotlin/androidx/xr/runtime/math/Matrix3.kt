/*
 * Copyright 2025 The Android Open Source Project
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

import kotlin.math.sign
import kotlin.math.sqrt

/**
 * An immutable 3x3 matrix that represents rotation and scale. The matrix is column major and right
 * handed. The indexes of [dataToCopy] represent the following matrix layout:
 * ```
 * [0, 3, 6]
 * [1, 4, 7]
 * [2, 5, 8]
 * ```
 *
 * @param dataToCopy the array with 9 elements that will be copied over.
 */
public class Matrix3(dataToCopy: FloatArray) {
    init {
        // TODO: Consider using contracts to avoid the exception being inlined.
        require(dataToCopy.size == 9) {
            "Input array must have exactly 9 elements for a 3x3 matrix"
        }
    }

    /** Returns an array of the components of this matrix. */
    public val data: FloatArray = dataToCopy.copyOf()

    /** Returns a matrix that performs the opposite transformation. */
    public val inverse: Matrix3 by lazy(LazyThreadSafetyMode.NONE) { inverse() }

    /** Returns a matrix that is the transpose of this matrix. */
    public val transpose: Matrix3 by lazy(LazyThreadSafetyMode.NONE) { transpose() }

    /** Returns the scale component of this matrix. */
    public val scale: Vector3 by lazy(LazyThreadSafetyMode.NONE) { scale() }

    /** Returns the rotation component of this matrix. */
    public val rotation: Quaternion by lazy(LazyThreadSafetyMode.NONE) { rotation() }

    /**
     * Returns true if this matrix is a valid transformation matrix that can be decomposed into
     * rotation and scale using determinant properties.
     */
    public val isTrs: Boolean by lazy(LazyThreadSafetyMode.NONE) { determinant() != 0.0f }

    /** Creates a new matrix with a deep copy of the data from the [other] [Matrix3]. */
    public constructor(other: Matrix3) : this(other.data.copyOf())

    /**
     * Returns a new matrix with the matrix multiplication product of this matrix and the [other]
     * matrix.
     */
    public operator fun times(other: Matrix3): Matrix3 {
        // multiplyMM is not supported for 3x3 matrices so we manually do the multiplication.
        val resultData = FloatArray(9)
        val a = this.data
        val b = other.data

        resultData[0] = a[0] * b[0] + a[3] * b[1] + a[6] * b[2]
        resultData[1] = a[1] * b[0] + a[4] * b[1] + a[7] * b[2]
        resultData[2] = a[2] * b[0] + a[5] * b[1] + a[8] * b[2]

        resultData[3] = a[0] * b[3] + a[3] * b[4] + a[6] * b[5]
        resultData[4] = a[1] * b[3] + a[4] * b[4] + a[7] * b[5]
        resultData[5] = a[2] * b[3] + a[5] * b[4] + a[8] * b[5]

        resultData[6] = a[0] * b[6] + a[3] * b[7] + a[6] * b[8]
        resultData[7] = a[1] * b[6] + a[4] * b[7] + a[7] * b[8]
        resultData[8] = a[2] * b[6] + a[5] * b[7] + a[8] * b[8]

        return Matrix3(resultData)
    }

    private fun inverse(): Matrix3 {
        // invertM is not supported for 3x3 matrices so we manually do the inversion.
        val det = determinant()
        if (det == 0.0f) {
            // Matrix4 uses invertM which might return a zeroed matrix on failure. For consistency
            // with Matrix4, we return a zero matrix on failure.
            return ZERO
        }

        val invDet = 1.0f / det
        val resultData = FloatArray(9)
        val d = this.data

        resultData[0] = (d[4] * d[8] - d[7] * d[5]) * invDet
        resultData[1] = (d[7] * d[2] - d[1] * d[8]) * invDet
        resultData[2] = (d[1] * d[5] - d[4] * d[2]) * invDet

        resultData[3] = (d[6] * d[5] - d[3] * d[8]) * invDet
        resultData[4] = (d[0] * d[8] - d[6] * d[2]) * invDet
        resultData[5] = (d[3] * d[2] - d[0] * d[5]) * invDet

        resultData[6] = (d[3] * d[7] - d[6] * d[4]) * invDet
        resultData[7] = (d[6] * d[1] - d[0] * d[7]) * invDet
        resultData[8] = (d[0] * d[4] - d[3] * d[1]) * invDet

        return Matrix3(resultData)
    }

    private fun transpose(): Matrix3 {
        // transposeM is not supported for 3x3 matrices so we manually do the transpose.
        val resultData = FloatArray(9)
        val d = this.data

        resultData[0] = d[0]
        resultData[1] = d[3]
        resultData[2] = d[6]

        resultData[3] = d[1]
        resultData[4] = d[4]
        resultData[5] = d[7]

        resultData[6] = d[2]
        resultData[7] = d[5]
        resultData[8] = d[8]

        return Matrix3(resultData)
    }

    private fun rotation(): Quaternion {
        val m00 = data[0]
        val m01 = data[3]
        val m02 = data[6]
        val m10 = data[1]
        val m11 = data[4]
        val m12 = data[7]
        val m20 = data[2]
        val m21 = data[5]
        val m22 = data[8]

        val trace = m00 + m11 + m22

        // We check if s is zero to avoid division by zero when calculating the inverse.
        return if (trace > 0.0f) {
            var s = sqrt(trace + 1.0f) * 2.0f
            if (s == 0.0f) s = 1.0f
            val invS = 1.0f / s
            Quaternion((m21 - m12) * invS, (m02 - m20) * invS, (m10 - m01) * invS, 0.25f * s)
        } else if ((m00 > m11) && (m00 > m22)) {
            var s = sqrt(1.0f + m00 - m11 - m22) * 2.0f
            if (s == 0.0f) s = 1.0f
            val invS = 1.0f / s
            Quaternion(0.25f * s, (m01 + m10) * invS, (m02 + m20) * invS, (m21 - m12) * invS)
        } else if (m11 > m22) {
            var s = sqrt(1.0f + m11 - m00 - m22) * 2.0f
            if (s == 0.0f) s = 1.0f
            val invS = 1.0f / s
            Quaternion((m01 + m10) * invS, 0.25f * s, (m12 + m21) * invS, (m02 - m20) * invS)
        } else {
            var s = sqrt(1.0f + m22 - m00 - m11) * 2.0f
            if (s == 0.0f) s = 1.0f
            val invS = 1.0f / s
            Quaternion((m02 + m20) * invS, (m12 + m21) * invS, 0.25f * s, (m10 - m01) * invS)
        }
    }

    private fun scale(): Vector3 {
        // TODO: b/367780918 - Investigate why scale can have negative values when inputs were
        // positive.
        // We don't want it to ever return 0.
        val signX = if (data[0] == 0.0f) 1.0f else sign(data[0])
        val signY = if (data[4] == 0.0f) 1.0f else sign(data[4])
        val signZ = if (data[8] == 0.0f) 1.0f else sign(data[8])

        return Vector3(
            signX * sqrt(data[0] * data[0] + data[1] * data[1] + data[2] * data[2]),
            signY * sqrt(data[3] * data[3] + data[4] * data[4] + data[5] * data[5]),
            signZ * sqrt(data[6] * data[6] + data[7] * data[7] + data[8] * data[8]),
        )
    }

    /** Computes the determinant of a 3x3 matrix. */
    private fun determinant(): Float =
        data[0] * (data[4] * data[8] - data[7] * data[5]) -
            data[3] * (data[1] * data[8] - data[7] * data[2]) +
            data[6] * (data[1] * data[5] - data[4] * data[2])

    /** Returns true if this matrix is equal to [other]. */
    public override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (other !is Matrix3) return false

        return this.data.contentEquals(other.data)
    }

    /** Standard hash code calculation using constructor values. */
    public override fun hashCode(): Int = data.contentHashCode()

    /** Standard toString() implementation. */
    public override fun toString(): String =
        "\n[ " +
            data[0] +
            "\t" +
            data[3] +
            "\t" +
            data[6] +
            "\n  " +
            data[1] +
            "\t" +
            data[4] +
            "\t" +
            data[7] +
            "\n  " +
            data[2] +
            "\t" +
            data[5] +
            "\t" +
            data[8] +
            " ]"

    /** Returns a copy of the matrix. */
    public fun copy(data: FloatArray = this.data): Matrix3 = Matrix3(data)

    public companion object {
        /** Returns an identity matrix. */
        @JvmField
        public val IDENTITY: Matrix3 = Matrix3(floatArrayOf(1f, 0f, 0f, 0f, 1f, 0f, 0f, 0f, 1f))

        /** Returns a zero matrix. */
        @JvmField
        public val ZERO: Matrix3 = Matrix3(floatArrayOf(0f, 0f, 0f, 0f, 0f, 0f, 0f, 0f, 0f))

        /** Returns a new scale matrix. */
        @JvmStatic
        public fun fromScale(scale: Vector3): Matrix3 =
            Matrix3(floatArrayOf(scale.x, 0.0f, 0.0f, 0.0f, scale.y, 0.0f, 0.0f, 0.0f, scale.z))

        /** Returns a new uniform scale matrix. */
        @JvmStatic
        public fun fromScale(scale: Float): Matrix3 =
            Matrix3(floatArrayOf(scale, 0.0f, 0.0f, 0.0f, scale, 0.0f, 0.0f, 0.0f, scale))

        /**
         * Returns a new 3x3 rotation matrix from the given [quaternion], which is first normalized.
         * This function uses a standard formula for the conversion, though alternative algebraic
         * expressions exist due to differing conventions. The resulting matrix typically transforms
         * 3D column vectors by pre-multiplication (e.g., $v'_{new} = M \cdot v_{old}$).
         */
        @JvmStatic
        public fun fromQuaternion(quaternion: Quaternion): Matrix3 {
            val q = quaternion.toNormalized()

            val qx = q.x
            val qy = q.y
            val qz = q.z
            val qw = q.w

            val qx2 = qx * qx
            val qy2 = qy * qy
            val qz2 = qz * qz

            val qxy = qx * qy
            val qxz = qx * qz
            val qxw = qx * qw
            val qyz = qy * qz
            val qyw = qy * qw
            val qzw = qz * qw

            return Matrix3(
                floatArrayOf(
                    1.0f - 2.0f * qy2 - 2.0f * qz2,
                    2.0f * qxy + 2.0f * qzw,
                    2.0f * qxz - 2.0f * qyw,
                    2.0f * qxy - 2.0f * qzw,
                    1.0f - 2.0f * qx2 - 2.0f * qz2,
                    2.0f * qyz + 2.0f * qxw,
                    2.0f * qxz + 2.0f * qyw,
                    2.0f * qyz - 2.0f * qxw,
                    1.0f - 2.0f * qx2 - 2.0f * qy2,
                )
            )
        }
    }
}
