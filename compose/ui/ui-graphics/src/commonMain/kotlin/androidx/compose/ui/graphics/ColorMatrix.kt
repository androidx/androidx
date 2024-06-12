/*
 * Copyright 2021 The Android Open Source Project
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
@file:Suppress("NOTHING_TO_INLINE")

package androidx.compose.ui.graphics

import androidx.compose.ui.util.normalizedAngleCos
import androidx.compose.ui.util.normalizedAngleSin

/**
 * 4x5 matrix for transforming the color and alpha components of a source. The matrix can be passed
 * as single array, and is treated as follows:
 * ```
 *  [ a, b, c, d, e,
 *    f, g, h, i, j,
 *    k, l, m, n, o,
 *    p, q, r, s, t ]
 * ```
 *
 * When applied to a color <code>[[R, G, B, A]]</code>, the resulting color is computed as:
 * ```
 *   R' = a*R + b*G + c*B + d*A + e;
 *   G' = f*R + g*G + h*B + i*A + j;
 *   B' = k*R + l*G + m*B + n*A + o;
 *   A' = p*R + q*G + r*B + s*A + t;</pre>
 *
 * ```
 *
 * That resulting color <code>[[R', G', B', A']]</code> then has each channel clamped to the
 * <code>0</code> to <code>255</code> range.
 *
 * The sample ColorMatrix below inverts incoming colors by scaling each channel by <code>-1</code>,
 * and then shifting the result up by `255` to remain in the standard color space.
 *
 * ```
 *   [ -1, 0, 0, 0, 255,
 *     0, -1, 0, 0, 255,
 *     0, 0, -1, 0, 255,
 *     0, 0, 0, 1, 0 ]
 * ```
 *
 * This is often used as input for [ColorFilter.colorMatrix] and applied at draw time through
 * [Paint.colorFilter]
 */
@kotlin.jvm.JvmInline
value class ColorMatrix(
    val values: FloatArray =
        floatArrayOf(1f, 0f, 0f, 0f, 0f, 0f, 1f, 0f, 0f, 0f, 0f, 0f, 1f, 0f, 0f, 0f, 0f, 0f, 1f, 0f)
) {
    // NOTE: This class contains a number of tests like this:
    //
    //     `if (values.size < 20) return`
    //
    // These tests exist to give the AOT compiler a hint about the size of the array.
    // Their presence eliminates a large number of array bound checks. For instance,
    // in the isIdentity method, this eliminates half of the instructions and half
    // of the branches.
    //
    // These tests are not there to generate early returns (the test will always
    // fail), but only to influence code generation.
    //
    // DO NOT REMOVE THOSE TESTS.

    /**
     * Obtain an instance of the matrix value at the given [row] and [column]. [ColorMatrix] follows
     * row major order in regards to the positions of matrix values within the flattened array. That
     * is, content order goes from left to right then top to bottom as opposed to column major
     * order.
     *
     * @param row Row index to query the ColorMatrix value. Range is from 0 to 3 as [ColorMatrix] is
     *   represented as a 4 x 5 matrix
     * @param column Column index to query the ColorMatrix value. Range is from 0 to 4 as
     *   [ColorMatrix] is represented as a 4 x 5 matrix
     */
    inline operator fun get(row: Int, column: Int) = values[(row * 5) + column]

    /**
     * Set the matrix value at the given [row] and [column]. [ColorMatrix] follows row major order
     * in regards to the positions of matrix values within the flattened array. That is, content
     * order goes from left to right then top to bottom as opposed to column major order.
     *
     * @param row Row index to query the ColorMatrix value. Range is from 0 to 3 as [ColorMatrix] is
     *   represented as a 4 x 5 matrix
     * @param column Column index to query the ColorMatrix value. Range is from 0 to 4 as
     *   [ColorMatrix] is represented as a 4 x 5 matrix
     * @param v value to update at the given [row] and [column]
     */
    inline operator fun set(row: Int, column: Int, v: Float) {
        values[(row * 5) + column] = v
    }

    /**
     * Set this colormatrix to identity:
     * ```
     * [ 1 0 0 0 0   - red vector
     * 0 1 0 0 0   - green vector
     * 0 0 1 0 0   - blue vector
     * 0 0 0 1 0 ] - alpha vector
     * ```
     */
    inline fun reset() {
        this[0, 0] = 1f
        this[0, 1] = 0f
        this[0, 2] = 0f
        this[0, 3] = 0f
        this[0, 4] = 0f

        this[1, 0] = 0f
        this[1, 1] = 1f
        this[1, 2] = 0f
        this[1, 3] = 0f
        this[1, 4] = 0f

        this[2, 0] = 0f
        this[2, 1] = 0f
        this[2, 2] = 1f
        this[2, 3] = 0f
        this[2, 4] = 0f

        this[3, 0] = 0f
        this[3, 1] = 0f
        this[3, 2] = 0f
        this[3, 3] = 1f
        this[3, 4] = 0f
    }

    /** Assign the [src] colormatrix into this matrix, copying all of its values. */
    fun set(src: ColorMatrix) {
        val v1 = values
        if (v1.size < 20) return

        val v2 = src.values
        if (v2.size < 20) return

        v1[0] = v2[0]
        v1[1] = v2[1]
        v1[2] = v2[2]
        v1[3] = v2[3]
        v1[4] = v2[4]
        v1[5] = v2[5]
        v1[6] = v2[6]
        v1[7] = v2[7]
        v1[8] = v2[8]
        v1[9] = v2[9]
        v1[10] = v2[10]
        v1[11] = v2[11]
        v1[12] = v2[12]
        v1[13] = v2[13]
        v1[14] = v2[14]
        v1[15] = v2[15]
        v1[16] = v2[16]
        v1[17] = v2[17]
        v1[18] = v2[18]
        v1[19] = v2[19]
    }

    /**
     * Internal helper method to handle rotation computation and provides a callback used to apply
     * the result to different color rotation axes
     */
    private inline fun rotateInternal(degrees: Float, block: (cosine: Float, sine: Float) -> Unit) {
        reset()
        val normalizedAngle = degrees * (1.0f / 360.0f)
        val cosine = normalizedAngleCos(normalizedAngle)
        val sine = normalizedAngleSin(normalizedAngle)
        block(cosine, sine)
    }

    /** Multiply this matrix by [colorMatrix] and assign the result to this matrix. */
    operator fun timesAssign(colorMatrix: ColorMatrix) {
        if (values.size < 20) return

        val v00 = dot(this, 0, colorMatrix, 0)
        val v01 = dot(this, 0, colorMatrix, 1)
        val v02 = dot(this, 0, colorMatrix, 2)
        val v03 = dot(this, 0, colorMatrix, 3)
        val v04 =
            this[0, 0] * colorMatrix[0, 4] +
                this[0, 1] * colorMatrix[1, 4] +
                this[0, 2] * colorMatrix[2, 4] +
                this[0, 3] * colorMatrix[3, 4] +
                this[0, 4]

        val v10 = dot(this, 1, colorMatrix, 0)
        val v11 = dot(this, 1, colorMatrix, 1)
        val v12 = dot(this, 1, colorMatrix, 2)
        val v13 = dot(this, 1, colorMatrix, 3)
        val v14 =
            this[1, 0] * colorMatrix[0, 4] +
                this[1, 1] * colorMatrix[1, 4] +
                this[1, 2] * colorMatrix[2, 4] +
                this[1, 3] * colorMatrix[3, 4] +
                this[1, 4]

        val v20 = dot(this, 2, colorMatrix, 0)
        val v21 = dot(this, 2, colorMatrix, 1)
        val v22 = dot(this, 2, colorMatrix, 2)
        val v23 = dot(this, 2, colorMatrix, 3)
        val v24 =
            this[2, 0] * colorMatrix[0, 4] +
                this[2, 1] * colorMatrix[1, 4] +
                this[2, 2] * colorMatrix[2, 4] +
                this[2, 3] * colorMatrix[3, 4] +
                this[2, 4]

        val v30 = dot(this, 3, colorMatrix, 0)
        val v31 = dot(this, 3, colorMatrix, 1)
        val v32 = dot(this, 3, colorMatrix, 2)
        val v33 = dot(this, 3, colorMatrix, 3)
        val v34 =
            this[3, 0] * colorMatrix[0, 4] +
                this[3, 1] * colorMatrix[1, 4] +
                this[3, 2] * colorMatrix[2, 4] +
                this[3, 3] * colorMatrix[3, 4] +
                this[3, 4]

        this[0, 0] = v00
        this[0, 1] = v01
        this[0, 2] = v02
        this[0, 3] = v03
        this[0, 4] = v04
        this[1, 0] = v10
        this[1, 1] = v11
        this[1, 2] = v12
        this[1, 3] = v13
        this[1, 4] = v14
        this[2, 0] = v20
        this[2, 1] = v21
        this[2, 2] = v22
        this[2, 3] = v23
        this[2, 4] = v24
        this[3, 0] = v30
        this[3, 1] = v31
        this[3, 2] = v32
        this[3, 3] = v33
        this[3, 4] = v34
    }

    /**
     * Set the matrix to affect the saturation of colors.
     *
     * @param sat A value of 0 maps the color to gray-scale. 1 is identity.
     */
    fun setToSaturation(sat: Float) {
        if (values.size < 20) return
        reset()

        val invSat = 1 - sat
        val r = 0.213f * invSat
        val g = 0.715f * invSat
        val b = 0.072f * invSat

        this[0, 0] = r + sat
        this[0, 1] = g
        this[0, 2] = b
        this[1, 0] = r
        this[1, 1] = g + sat
        this[1, 2] = b
        this[2, 0] = r
        this[2, 1] = g
        this[2, 2] = b + sat
    }

    /**
     * Create a [ColorMatrix] with the corresponding scale parameters for the red, green, blue and
     * alpha axes
     *
     * @param redScale Desired scale parameter for the red channel
     * @param greenScale Desired scale parameter for the green channel
     * @param blueScale Desired scale parameter for the blue channel
     * @param alphaScale Desired scale parameter for the alpha channel
     */
    fun setToScale(redScale: Float, greenScale: Float, blueScale: Float, alphaScale: Float) {
        if (values.size < 20) return
        reset()

        this[0, 0] = redScale
        this[1, 1] = greenScale
        this[2, 2] = blueScale
        this[3, 3] = alphaScale
    }

    /** Rotate by [degrees] along the red color axis */
    fun setToRotateRed(degrees: Float) {
        if (values.size < 20) return

        rotateInternal(degrees) { cosine, sine ->
            this[1, 1] = cosine
            this[1, 2] = sine
            this[2, 1] = -sine
            this[2, 2] = cosine
        }
    }

    /** Rotate by [degrees] along the green color axis */
    fun setToRotateGreen(degrees: Float) {
        if (values.size < 20) return

        rotateInternal(degrees) { cosine, sine ->
            this[0, 0] = cosine
            this[0, 2] = -sine
            this[2, 0] = sine
            this[2, 2] = cosine
        }
    }

    /** Rotate by [degrees] along the blue color axis */
    fun setToRotateBlue(degrees: Float) {
        if (values.size < 20) return

        rotateInternal(degrees) { cosine, sine ->
            this[0, 0] = cosine
            this[0, 1] = sine
            this[1, 0] = -sine
            this[1, 1] = cosine
        }
    }

    /** Set the matrix to convert RGB to YUV */
    fun convertRgbToYuv() {
        if (values.size < 20) return
        reset()

        // these coefficients match those in libjpeg
        this[0, 0] = 0.299f
        this[0, 1] = 0.587f
        this[0, 2] = 0.114f
        this[1, 0] = -0.16874f
        this[1, 1] = -0.33126f
        this[1, 2] = 0.5f
        this[2, 0] = 0.5f
        this[2, 1] = -0.41869f
        this[2, 2] = -0.08131f
    }

    /** Set the matrix to convert from YUV to RGB */
    fun convertYuvToRgb() {
        if (values.size < 20) return
        reset()

        // these coefficients match those in libjpeg
        this[0, 2] = 1.402f
        this[1, 0] = 1f
        this[1, 1] = -0.34414f
        this[1, 2] = -0.71414f
        this[2, 0] = 1f
        this[2, 1] = 1.772f
        this[2, 2] = 0f
    }
}

/**
 * Helper method that returns the dot product of the top left 4 x 4 matrix of [ColorMatrix] used in
 * [ColorMatrix.timesAssign]
 */
private inline fun dot(m1: ColorMatrix, row: Int, m2: ColorMatrix, column: Int): Float {
    return m1[row, 0] * m2[0, column] +
        m1[row, 1] * m2[1, column] +
        m1[row, 2] * m2[2, column] +
        m1[row, 3] * m2[3, column]
}
