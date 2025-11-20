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

import kotlin.math.sqrt

/**
 * An immutable 4x4 matrix that represents translation, scale, and rotation. The matrix is column
 * major and right handed. The indexes of [dataToCopy] represent the following matrix layout:
 * ```
 *  [0, 4,  8, 12]
 *  [1, 5,  9, 13]
 *  [2, 6, 10, 14]
 *  [3, 7, 11, 15]
 * ```
 *
 * @param dataToCopy the array with 16 elements that will be copied over.
 */
public class Matrix4(dataToCopy: FloatArray) {
    init {
        // TODO: Consider using contracts to avoid the exception being inlined.
        require(dataToCopy.size == 16) {
            "Input array must have exactly 16 elements for a 4x4 matrix"
        }
    }

    /** Returns an array of the components of this matrix. */
    public val data: FloatArray = dataToCopy.copyOf()

    /** Returns a matrix that performs the opposite transformation. */
    public val inverse: Matrix4 by lazy(LazyThreadSafetyMode.NONE) { inverse() }

    /** Returns a matrix that is the transpose of this matrix. */
    public val transpose: Matrix4 by lazy(LazyThreadSafetyMode.NONE) { transpose() }

    /** Returns the translation component of this matrix. */
    public val translation: Vector3 by
        lazy(LazyThreadSafetyMode.NONE) { Vector3(data[12], data[13], data[14]) }

    /** Returns the scale component of this matrix. */
    public val scale: Vector3 by lazy(LazyThreadSafetyMode.NONE) { scale() }

    /** Returns the rotation component of this matrix. */
    public val rotation: Quaternion by lazy(LazyThreadSafetyMode.NONE) { rotation() }

    /** Returns the pose (i.e. rotation and translation) of this matrix. */
    public val pose: Pose by lazy(LazyThreadSafetyMode.NONE) { Pose(translation, rotation) }

    /**
     * Returns true if this matrix is a valid transformation matrix that can be decomposed into
     * translation, rotation and scale using determinant properties.
     */
    public val isTrs: Boolean by lazy(LazyThreadSafetyMode.NONE) { determinant() != 0.0f }

    /** Creates a new matrix with a deep copy of the data from the [other] [Matrix4]. */
    public constructor(other: Matrix4) : this(other.data.copyOf())

    /**
     * Returns a new matrix with the matrix multiplication product of this matrix and the [other]
     * matrix.
     */
    public operator fun times(other: Matrix4): Matrix4 {
        val resultData = FloatArray(16)
        android.opengl.Matrix.multiplyMM(
            /* result= */ resultData,
            /* resultOffset= */ 0,
            /* lhs= */ this.data,
            /* lhsOffset= */ 0,
            /* rhs= */ other.data,
            /* rhsOffset= */ 0,
        )

        return Matrix4(resultData)
    }

    private fun inverse(): Matrix4 {
        val resultData = FloatArray(16)
        android.opengl.Matrix.invertM(
            /* mInv= */ resultData,
            /* mInvOffset= */ 0,
            /* m= */ this.data,
            /* mOffset= */ 0,
        )

        return Matrix4(resultData)
    }

    private fun transpose(): Matrix4 {
        val resultData = FloatArray(16)
        android.opengl.Matrix.transposeM(
            /* mTrans= */ resultData,
            /* mTransOffset= */ 0,
            /* m= */ this.data,
            /* mOffset= */ 0,
        )

        return Matrix4(resultData)
    }

    private fun rotation(): Quaternion {
        val m00 = data[0] / this.scale.x
        val m01 = data[4] / this.scale.y
        val m02 = data[8] / this.scale.z
        val m10 = data[1] / this.scale.x
        val m11 = data[5] / this.scale.y
        val m12 = data[9] / this.scale.z
        val m20 = data[2] / this.scale.x
        val m21 = data[6] / this.scale.y
        val m22 = data[10] / this.scale.z

        val trace = m00 + m11 + m22 + 1.0f

        return if (trace > 0) {
            val s = 0.5f / sqrt(trace)
            Quaternion((m21 - m12) * s, (m02 - m20) * s, (m10 - m01) * s, 0.25f / s)
        } else if ((m00 > m11) && (m00 > m22)) {
            val s = 2.0f * sqrt(1.0f + m00 - m11 - m22)
            Quaternion(0.25f * s, (m01 + m10) / s, (m02 + m20) / s, (m21 - m12) / s)
        } else if (m11 > m22) {
            val s = 2.0f * sqrt(1.0f + m11 - m00 - m22)
            Quaternion((m01 + m10) / s, 0.25f * s, (m12 + m21) / s, (m02 - m20) / s)
        } else {
            val s = 2.0f * sqrt(1.0f + m22 - m00 - m11)
            Quaternion((m02 + m20) / s, (m12 + m21) / s, 0.25f * s, (m10 - m01) / s)
        }
    }

    private fun scale(): Vector3 {
        // use either a positive or negative scale based on the rotation matrix determinant
        val rotationDeterminant =
            data[0] * (data[5] * data[10] - data[9] * data[6]) -
                data[4] * (data[1] * data[10] - data[9] * data[2]) +
                data[8] * (data[1] * data[6] - data[5] * data[2])
        val sign = if (rotationDeterminant < 0) -1.0f else 1.0f
        return Vector3(
            sign * sqrt(data[0] * data[0] + data[1] * data[1] + data[2] * data[2]),
            sign * sqrt(data[4] * data[4] + data[5] * data[5] + data[6] * data[6]),
            sign * sqrt(data[8] * data[8] + data[9] * data[9] + data[10] * data[10]),
        )
    }

    /**
     * Returns a normalized transformation matrix.
     *
     * This method removes the scaling factors from this matrix, as determined by [scale], while
     * preserving its core rotational and translational components. The resulting matrix is
     * therefore ideal for accurate normal vector transformations and pure orientation extraction.
     */
    public fun unscaled(): Matrix4 {
        // Matrix4.scale returns either a positive or negative scale based on the
        // determinant of the rotation matrix.
        val positiveScale = Vector3.abs(scale())
        val scaleX = positiveScale.x
        val scaleY = positiveScale.y
        val scaleZ = positiveScale.z
        return Matrix4(
            floatArrayOf(
                data[0] / scaleX,
                data[1] / scaleX,
                data[2] / scaleX,
                data[3],
                data[4] / scaleY,
                data[5] / scaleY,
                data[6] / scaleY,
                data[7],
                data[8] / scaleZ,
                data[9] / scaleZ,
                data[10] / scaleZ,
                data[11],
                data[12],
                data[13],
                data[14],
                data[15],
            )
        )
    }

    /** Computes the determinant of a 4x4 matrix. */
    private fun determinant(): Float =
        data[0] *
            (data[5] * (data[10] * data[15] - data[14] * data[11]) -
                data[9] * (data[6] * data[15] - data[14] * data[7]) +
                data[13] * (data[6] * data[11] - data[10] * data[7])) -
            data[4] *
                (data[1] * (data[10] * data[15] - data[14] * data[11]) -
                    data[9] * (data[2] * data[15] - data[14] * data[3]) +
                    data[13] * (data[2] * data[11] - data[10] * data[3])) +
            data[8] *
                (data[1] * (data[6] * data[15] - data[14] * data[7]) -
                    data[5] * (data[2] * data[15] - data[14] * data[3]) +
                    data[13] * (data[2] * data[7] - data[6] * data[3])) -
            data[12] *
                (data[1] * (data[6] * data[11] - data[10] * data[7]) -
                    data[5] * (data[2] * data[11] - data[10] * data[3]) +
                    data[9] * (data[2] * data[7] - data[6] * data[3]))

    /** Returns true if this pose is equal to [other]. */
    public override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (other !is Matrix4) return false

        return this.data.contentEquals(other.data)
    }

    /** Standard hash code calculation using constructor values */
    public override fun hashCode(): Int = data.contentHashCode()

    /** Standard toString() implementation */
    public override fun toString(): String =
        "\n[ " +
            data[0] +
            "\t" +
            data[4] +
            "\t" +
            data[8] +
            "\t" +
            data[12] +
            "\n  " +
            data[1] +
            "\t" +
            data[5] +
            "\t" +
            data[9] +
            "\t" +
            data[13] +
            "\n  " +
            data[2] +
            "\t" +
            data[6] +
            "\t" +
            data[10] +
            "\t" +
            data[14] +
            "\n  " +
            data[3] +
            "\t" +
            data[7] +
            "\t" +
            data[11] +
            "\t" +
            data[15] +
            " ]"

    /** Returns a copy of the matrix. */
    public fun copy(data: FloatArray = this.data): Matrix4 = Matrix4(data)

    public companion object {
        /** Returns an identity matrix. */
        @JvmField
        public val Identity: Matrix4 =
            Matrix4(floatArrayOf(1f, 0f, 0f, 0f, 0f, 1f, 0f, 0f, 0f, 0f, 1f, 0f, 0f, 0f, 0f, 1f))

        /** Returns a zero matrix. */
        @JvmField
        public val Zero: Matrix4 =
            Matrix4(floatArrayOf(0f, 0f, 0f, 0f, 0f, 0f, 0f, 0f, 0f, 0f, 0f, 0f, 0f, 0f, 0f, 0f))

        /**
         * Returns a new transformation matrix. The returned matrix is such that it first scales
         * objects, then rotates them, and finally translates them.
         */
        @JvmStatic
        public fun fromTrs(translation: Vector3, rotation: Quaternion, scale: Vector3): Matrix4 {
            // implementationd details: https://www.songho.ca/opengl/gl_quaternion.html
            val q = rotation.toNormalized()

            // double var1 var2
            val dqyx = 2 * q.y * q.x
            val dqxz = 2 * q.x * q.z
            val dqxw = 2 * q.x * q.w
            val dqyw = 2 * q.y * q.w
            val dqzw = 2 * q.z * q.w
            val dqzy = 2 * q.z * q.y

            // double var squared
            val dsqz = 2 * q.z * q.z
            val dsqy = 2 * q.y * q.y

            val oneMinusDSQX = 1 - 2 * q.x * q.x

            return Matrix4(
                floatArrayOf(
                    (1 - dsqy - dsqz) * scale.x,
                    (dqyx + dqzw) * scale.x,
                    (dqxz - dqyw) * scale.x,
                    0.0f,
                    (dqyx - dqzw) * scale.y,
                    (oneMinusDSQX - dsqz) * scale.y,
                    (dqzy + dqxw) * scale.y,
                    0.0f,
                    (dqxz + dqyw) * scale.z,
                    (dqzy - dqxw) * scale.z,
                    (oneMinusDSQX - dsqy) * scale.z,
                    0.0f,
                    translation.x,
                    translation.y,
                    translation.z,
                    1.0f,
                )
            )
        }

        /** Returns a new translation matrix. */
        @JvmStatic
        public fun fromTranslation(translation: Vector3): Matrix4 =
            Matrix4(
                floatArrayOf(
                    1.0f,
                    0.0f,
                    0.0f,
                    0.0f,
                    0.0f,
                    1.0f,
                    0.0f,
                    0.0f,
                    0.0f,
                    0.0f,
                    1.0f,
                    0.0f,
                    translation.x,
                    translation.y,
                    translation.z,
                    1.0f,
                )
            )

        /** Returns a new uniform scale matrix. */
        @JvmStatic
        public fun fromScale(scale: Vector3): Matrix4 =
            Matrix4(
                floatArrayOf(
                    scale.x,
                    0.0f,
                    0.0f,
                    0.0f,
                    0.0f,
                    scale.y,
                    0.0f,
                    0.0f,
                    0.0f,
                    0.0f,
                    scale.z,
                    0.0f,
                    0.0f,
                    0.0f,
                    0.0f,
                    1.0f,
                )
            )

        /** Returns a new scale matrix. */
        @JvmStatic
        public fun fromScale(scale: Float): Matrix4 =
            Matrix4(
                floatArrayOf(
                    scale,
                    0.0f,
                    0.0f,
                    0.0f,
                    0.0f,
                    scale,
                    0.0f,
                    0.0f,
                    0.0f,
                    0.0f,
                    scale,
                    0.0f,
                    0.0f,
                    0.0f,
                    0.0f,
                    1.0f,
                )
            )

        /** Returns a new rotation matrix. */
        @JvmStatic
        public fun fromQuaternion(quaternion: Quaternion): Matrix4 =
            fromTrs(Vector3.Zero, quaternion, Vector3.One)

        /**
         * Returns a new rigid transformation matrix. The returned matrix is such that it first
         * rotates objects, and then translates them.
         */
        @JvmStatic
        public fun fromPose(pose: Pose): Matrix4 {
            return fromTrs(pose.translation, pose.rotation, Vector3.One)
        }
    }
}
