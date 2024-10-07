/*
 * Copyright 2020 The Android Open Source Project
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

@file:Suppress("NOTHING_TO_INLINE", "KotlinRedundantDiagnosticSuppress")

package androidx.compose.ui.graphics

import androidx.compose.ui.geometry.MutableRect
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.util.fastIsFinite
import androidx.compose.ui.util.fastMaxOf
import androidx.compose.ui.util.fastMinOf
import kotlin.math.PI
import kotlin.math.cos
import kotlin.math.sin

// NOTE: This class contains a number of tests like this:
//
//     `if (values.size < 16) return`
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
@kotlin.jvm.JvmInline
value class Matrix(
    val values: FloatArray =
        floatArrayOf(1f, 0f, 0f, 0f, 0f, 1f, 0f, 0f, 0f, 0f, 1f, 0f, 0f, 0f, 0f, 1f)
) {
    inline operator fun get(row: Int, column: Int) = values[(row * 4) + column]

    inline operator fun set(row: Int, column: Int, v: Float) {
        values[(row * 4) + column] = v
    }

    /** Does the 3D transform on [point] and returns the `x` and `y` values in an [Offset]. */
    fun map(point: Offset): Offset {
        // See top-level comment
        if (values.size < 16) return point

        val v00 = this[0, 0]
        val v01 = this[0, 1]
        val v03 = this[0, 3]
        val v10 = this[1, 0]
        val v11 = this[1, 1]
        val v13 = this[1, 3]
        val v30 = this[3, 0]
        val v31 = this[3, 1]
        val v33 = this[3, 3]

        val x = point.x
        val y = point.y
        val z = v03 * x + v13 * y + v33
        val inverseZ = 1 / z
        val pZ = if (inverseZ.fastIsFinite()) inverseZ else 0f

        return Offset(x = pZ * (v00 * x + v10 * y + v30), y = pZ * (v01 * x + v11 * y + v31))
    }

    /** Does a 3D transform on [rect] and returns its bounds after the transform. */
    fun map(rect: Rect): Rect {
        // See top-level comment
        if (values.size < 16) return rect

        val v00 = this[0, 0]
        val v01 = this[0, 1]
        val v03 = this[0, 3]
        val v10 = this[1, 0]
        val v11 = this[1, 1]
        val v13 = this[1, 3]
        val v30 = this[3, 0]
        val v31 = this[3, 1]
        val v33 = this[3, 3]

        val l = rect.left
        val t = rect.top
        val r = rect.right
        val b = rect.bottom

        var x = l
        var y = t
        var inverseZ = 1.0f / (v03 * x + v13 * y + v33)
        var pZ = if (inverseZ.fastIsFinite()) inverseZ else 0f
        val x0 = pZ * (v00 * x + v10 * y + v30)
        val y0 = pZ * (v01 * x + v11 * y + v31)

        x = l
        y = b
        inverseZ = 1.0f / (v03 * x + v13 * y + v33)
        pZ = if (inverseZ.fastIsFinite()) inverseZ else 0f
        val x1 = pZ * (v00 * x + v10 * y + v30)
        val y1 = pZ * (v01 * x + v11 * y + v31)

        x = r
        y = t
        inverseZ = 1.0f / (v03 * x + v13 * y + v33)
        pZ = if (inverseZ.fastIsFinite()) inverseZ else 0f
        val x2 = pZ * (v00 * x + v10 * y + v30)
        val y2 = pZ * (v01 * x + v11 * y + v31)

        x = r
        y = b
        inverseZ = 1.0f / (v03 * x + v13 * y + v33)
        pZ = if (inverseZ.fastIsFinite()) inverseZ else 0f
        val x3 = pZ * (v00 * x + v10 * y + v30)
        val y3 = pZ * (v01 * x + v11 * y + v31)

        return Rect(
            fastMinOf(x0, x1, x2, x3),
            fastMinOf(y0, y1, y2, y3),
            fastMaxOf(x0, x1, x2, x3),
            fastMaxOf(y0, y1, y2, y3)
        )
    }

    /** Does a 3D transform on [rect], transforming [rect] with the results. */
    fun map(rect: MutableRect) {
        // See top-level comment
        if (values.size < 16) return

        val v00 = this[0, 0]
        val v01 = this[0, 1]
        val v03 = this[0, 3]
        val v10 = this[1, 0]
        val v11 = this[1, 1]
        val v13 = this[1, 3]
        val v30 = this[3, 0]
        val v31 = this[3, 1]
        val v33 = this[3, 3]

        val l = rect.left
        val t = rect.top
        val r = rect.right
        val b = rect.bottom

        var x = l
        var y = t
        var inverseZ = 1.0f / (v03 * x + v13 * y + v33)
        var pZ = if (inverseZ.fastIsFinite()) inverseZ else 0f
        val x0 = pZ * (v00 * x + v10 * y + v30)
        val y0 = pZ * (v01 * x + v11 * y + v31)

        x = l
        y = b
        inverseZ = 1.0f / (v03 * x + v13 * y + v33)
        pZ = if (inverseZ.fastIsFinite()) inverseZ else 0f
        val x1 = pZ * (v00 * x + v10 * y + v30)
        val y1 = pZ * (v01 * x + v11 * y + v31)

        x = r
        y = t
        inverseZ = 1.0f / (v03 * x + v13 * y + v33)
        pZ = if (inverseZ.fastIsFinite()) inverseZ else 0f
        val x2 = pZ * (v00 * x + v10 * y + v30)
        val y2 = pZ * (v01 * x + v11 * y + v31)

        x = r
        y = b
        inverseZ = 1.0f / (v03 * x + v13 * y + v33)
        pZ = if (inverseZ.fastIsFinite()) inverseZ else 0f
        val x3 = pZ * (v00 * x + v10 * y + v30)
        val y3 = pZ * (v01 * x + v11 * y + v31)

        rect.left = fastMinOf(x0, x1, x2, x3)
        rect.top = fastMinOf(y0, y1, y2, y3)
        rect.right = fastMaxOf(x0, x1, x2, x3)
        rect.bottom = fastMaxOf(y0, y1, y2, y3)
    }

    /** Multiply this matrix by [m] and assign the result to this matrix. */
    operator fun timesAssign(m: Matrix) {
        // See top-level comment
        val v = values
        if (v.size < 16) return
        if (m.values.size < 16) return

        val v00 = dot(this, 0, m, 0)
        val v01 = dot(this, 0, m, 1)
        val v02 = dot(this, 0, m, 2)
        val v03 = dot(this, 0, m, 3)
        val v10 = dot(this, 1, m, 0)
        val v11 = dot(this, 1, m, 1)
        val v12 = dot(this, 1, m, 2)
        val v13 = dot(this, 1, m, 3)
        val v20 = dot(this, 2, m, 0)
        val v21 = dot(this, 2, m, 1)
        val v22 = dot(this, 2, m, 2)
        val v23 = dot(this, 2, m, 3)
        val v30 = dot(this, 3, m, 0)
        val v31 = dot(this, 3, m, 1)
        val v32 = dot(this, 3, m, 2)
        val v33 = dot(this, 3, m, 3)

        v[0] = v00
        v[1] = v01
        v[2] = v02
        v[3] = v03
        v[4] = v10
        v[5] = v11
        v[6] = v12
        v[7] = v13
        v[8] = v20
        v[9] = v21
        v[10] = v22
        v[11] = v23
        v[12] = v30
        v[13] = v31
        v[14] = v32
        v[15] = v33
    }

    override fun toString(): String {
        return """
            |${this[0, 0]} ${this[0, 1]} ${this[0, 2]} ${this[0, 3]}|
            |${this[1, 0]} ${this[1, 1]} ${this[1, 2]} ${this[1, 3]}|
            |${this[2, 0]} ${this[2, 1]} ${this[2, 2]} ${this[2, 3]}|
            |${this[3, 0]} ${this[3, 1]} ${this[3, 2]} ${this[3, 3]}|
        """
            .trimIndent()
    }

    /** Invert `this` Matrix. */
    fun invert() {
        // See top-level comment
        if (values.size < 16) return

        val a00 = this[0, 0]
        val a01 = this[0, 1]
        val a02 = this[0, 2]
        val a03 = this[0, 3]
        val a10 = this[1, 0]
        val a11 = this[1, 1]
        val a12 = this[1, 2]
        val a13 = this[1, 3]
        val a20 = this[2, 0]
        val a21 = this[2, 1]
        val a22 = this[2, 2]
        val a23 = this[2, 3]
        val a30 = this[3, 0]
        val a31 = this[3, 1]
        val a32 = this[3, 2]
        val a33 = this[3, 3]

        val b00 = a00 * a11 - a01 * a10
        val b01 = a00 * a12 - a02 * a10
        val b02 = a00 * a13 - a03 * a10
        val b03 = a01 * a12 - a02 * a11
        val b04 = a01 * a13 - a03 * a11
        val b05 = a02 * a13 - a03 * a12
        val b06 = a20 * a31 - a21 * a30
        val b07 = a20 * a32 - a22 * a30
        val b08 = a20 * a33 - a23 * a30
        val b09 = a21 * a32 - a22 * a31
        val b10 = a21 * a33 - a23 * a31
        val b11 = a22 * a33 - a23 * a32

        val det = (b00 * b11 - b01 * b10 + b02 * b09 + b03 * b08 - b04 * b07 + b05 * b06)
        if (det == 0.0f) {
            return
        }

        val invDet = 1.0f / det
        this[0, 0] = ((a11 * b11 - a12 * b10 + a13 * b09) * invDet)
        this[0, 1] = ((-a01 * b11 + a02 * b10 - a03 * b09) * invDet)
        this[0, 2] = ((a31 * b05 - a32 * b04 + a33 * b03) * invDet)
        this[0, 3] = ((-a21 * b05 + a22 * b04 - a23 * b03) * invDet)
        this[1, 0] = ((-a10 * b11 + a12 * b08 - a13 * b07) * invDet)
        this[1, 1] = ((a00 * b11 - a02 * b08 + a03 * b07) * invDet)
        this[1, 2] = ((-a30 * b05 + a32 * b02 - a33 * b01) * invDet)
        this[1, 3] = ((a20 * b05 - a22 * b02 + a23 * b01) * invDet)
        this[2, 0] = ((a10 * b10 - a11 * b08 + a13 * b06) * invDet)
        this[2, 1] = ((-a00 * b10 + a01 * b08 - a03 * b06) * invDet)
        this[2, 2] = ((a30 * b04 - a31 * b02 + a33 * b00) * invDet)
        this[2, 3] = ((-a20 * b04 + a21 * b02 - a23 * b00) * invDet)
        this[3, 0] = ((-a10 * b09 + a11 * b07 - a12 * b06) * invDet)
        this[3, 1] = ((a00 * b09 - a01 * b07 + a02 * b06) * invDet)
        this[3, 2] = ((-a30 * b03 + a31 * b01 - a32 * b00) * invDet)
        this[3, 3] = ((a20 * b03 - a21 * b01 + a22 * b00) * invDet)
    }

    /** Resets the `this` to the identity matrix. */
    fun reset() {
        // See top-level comment
        val v = values
        if (v.size < 16) return
        v[0] = 1f
        v[1] = 0f
        v[2] = 0f
        v[3] = 0f
        v[4] = 0f
        v[5] = 1f
        v[6] = 0f
        v[7] = 0f
        v[8] = 0f
        v[9] = 0f
        v[10] = 1f
        v[11] = 0f
        v[12] = 0f
        v[13] = 0f
        v[14] = 0f
        v[15] = 1f
    }

    /** Sets the entire matrix to the matrix in [matrix]. */
    fun setFrom(matrix: Matrix) {
        val src = values
        val dst = matrix.values

        // See top-level comment
        if (src.size < 16) return
        if (dst.size < 16) return

        src[0] = dst[0]
        src[1] = dst[1]
        src[2] = dst[2]
        src[3] = dst[3]
        src[4] = dst[4]
        src[5] = dst[5]
        src[6] = dst[6]
        src[7] = dst[7]
        src[8] = dst[8]
        src[9] = dst[9]
        src[10] = dst[10]
        src[11] = dst[11]
        src[12] = dst[12]
        src[13] = dst[13]
        src[14] = dst[14]
        src[15] = dst[15]
    }

    /** Applies a [degrees] rotation around X to `this`. */
    fun rotateX(degrees: Float) {
        // See top-level comment
        if (values.size < 16) return

        val r = degrees * (PI / 180.0)
        val s = sin(r).toFloat()
        val c = cos(r).toFloat()

        val a01 = this[0, 1]
        val a02 = this[0, 2]
        val v01 = a01 * c - a02 * s
        val v02 = a01 * s + a02 * c

        val a11 = this[1, 1]
        val a12 = this[1, 2]
        val v11 = a11 * c - a12 * s
        val v12 = a11 * s + a12 * c

        val a21 = this[2, 1]
        val a22 = this[2, 2]
        val v21 = a21 * c - a22 * s
        val v22 = a21 * s + a22 * c

        val a31 = this[3, 1]
        val a32 = this[3, 2]
        val v31 = a31 * c - a32 * s
        val v32 = a31 * s + a32 * c

        this[0, 1] = v01
        this[0, 2] = v02
        this[1, 1] = v11
        this[1, 2] = v12
        this[2, 1] = v21
        this[2, 2] = v22
        this[3, 1] = v31
        this[3, 2] = v32
    }

    /** Applies a [degrees] rotation around Y to `this`. */
    fun rotateY(degrees: Float) {
        // See top-level comment
        if (values.size < 16) return

        val r = degrees * (PI / 180.0)
        val s = sin(r).toFloat()
        val c = cos(r).toFloat()

        val a00 = this[0, 0]
        val a02 = this[0, 2]
        val v00 = a00 * c + a02 * s
        val v02 = -a00 * s + a02 * c

        val a10 = this[1, 0]
        val a12 = this[1, 2]
        val v10 = a10 * c + a12 * s
        val v12 = -a10 * s + a12 * c

        val a20 = this[2, 0]
        val a22 = this[2, 2]
        val v20 = a20 * c + a22 * s
        val v22 = -a20 * s + a22 * c

        val a30 = this[3, 0]
        val a32 = this[3, 2]
        val v30 = a30 * c + a32 * s
        val v32 = -a30 * s + a32 * c

        this[0, 0] = v00
        this[0, 2] = v02
        this[1, 0] = v10
        this[1, 2] = v12
        this[2, 0] = v20
        this[2, 2] = v22
        this[3, 0] = v30
        this[3, 2] = v32
    }

    /** Applies a [degrees] rotation around Z to `this`. */
    fun rotateZ(degrees: Float) {
        // See top-level comment
        if (values.size < 16) return

        val r = degrees * (PI / 180.0)
        val s = sin(r).toFloat()
        val c = cos(r).toFloat()

        val a00 = this[0, 0]
        val a10 = this[1, 0]
        val v00 = c * a00 + s * a10
        val v10 = -s * a00 + c * a10

        val a01 = this[0, 1]
        val a11 = this[1, 1]
        val v01 = c * a01 + s * a11
        val v11 = -s * a01 + c * a11

        val a02 = this[0, 2]
        val a12 = this[1, 2]
        val v02 = c * a02 + s * a12
        val v12 = -s * a02 + c * a12

        val a03 = this[0, 3]
        val a13 = this[1, 3]
        val v03 = c * a03 + s * a13
        val v13 = -s * a03 + c * a13

        this[0, 0] = v00
        this[0, 1] = v01
        this[0, 2] = v02
        this[0, 3] = v03
        this[1, 0] = v10
        this[1, 1] = v11
        this[1, 2] = v12
        this[1, 3] = v13
    }

    /** Scale this matrix by [x], [y], [z] */
    fun scale(x: Float = 1f, y: Float = 1f, z: Float = 1f) {
        // See top-level comment
        if (values.size < 16) return
        this[0, 0] *= x
        this[0, 1] *= x
        this[0, 2] *= x
        this[0, 3] *= x
        this[1, 0] *= y
        this[1, 1] *= y
        this[1, 2] *= y
        this[1, 3] *= y
        this[2, 0] *= z
        this[2, 1] *= z
        this[2, 2] *= z
        this[2, 3] *= z
    }

    /** Translate this matrix by [x], [y], [z] */
    fun translate(x: Float = 0f, y: Float = 0f, z: Float = 0f) {
        // See top-level comment
        if (values.size < 16) return
        val t1 = this[0, 0] * x + this[1, 0] * y + this[2, 0] * z + this[3, 0]
        val t2 = this[0, 1] * x + this[1, 1] * y + this[2, 1] * z + this[3, 1]
        val t3 = this[0, 2] * x + this[1, 2] * y + this[2, 2] * z + this[3, 2]
        val t4 = this[0, 3] * x + this[1, 3] * y + this[2, 3] * z + this[3, 3]
        this[3, 0] = t1
        this[3, 1] = t2
        this[3, 2] = t3
        this[3, 3] = t4
    }

    /**
     * Resets this matrix to a "TRS" (translation, rotation, scale) transform around a pivot point.
     * The transform operations encoded in the matrix are the following, in this specific order:
     * - A translation by -[pivotX], -[pivotY]
     * - A translation by [translationX], [translationY], and [translationZ]
     * - An X rotation by [rotationX]
     * - A Y rotation by [rotationY]
     * - A Z rotation by [rotationZ]
     * - A scale by [scaleX] and [scaleY]
     * - A translation by [pivotX], [pivotY]
     *
     * Calling this method is equivalent to the following code:
     * ```
     * val m: Matrix ...
     * m.reset()
     * m.translate(-pivotX, -pivotY)
     * m *= Matrix().apply {
     *     translate(translationX, translationY)
     *     rotateX(rotationX)
     *     rotateY(rotationY)
     *     rotateZ(rotationZ)
     *     scale(scaleX, scaleY)
     * }
     * m *= Matrix().apply { translate(pivotX, pivotY) }
     * ```
     */
    fun resetToPivotedTransform(
        pivotX: Float = 0f,
        pivotY: Float = 0f,
        translationX: Float = 0f,
        translationY: Float = 0f,
        translationZ: Float = 0f,
        rotationX: Float = 0f,
        rotationY: Float = 0f,
        rotationZ: Float = 0f,
        scaleX: Float = 1f,
        scaleY: Float = 1f,
        scaleZ: Float = 1f
    ) {
        // X
        val rx = rotationX * (PI / 180.0)
        val rsx = sin(rx).toFloat()
        val rcx = cos(rx).toFloat()

        var v11 = rcx
        var v12 = rsx

        var v21 = -rsx
        var v22 = rcx

        val v31 = translationY * rcx - translationZ * rsx
        var v32 = translationY * rsx + translationZ * rcx

        // Y
        val ry = rotationY * (PI / 180.0)
        val rsy = sin(ry).toFloat()
        val rcy = cos(ry).toFloat()

        var v00 = rcy
        var v02 = -rsy

        var v10 = v12 * rsy
        v12 *= rcy

        var v20 = v22 * rsy
        v22 *= rcy

        val v30 = translationX * rcy + v32 * rsy
        v32 = -translationX * rsy + v32 * rcy

        // Z
        val rz = rotationZ * (PI / 180.0)
        val rsz = sin(rz).toFloat()
        val rcz = cos(rz).toFloat()

        val a10 = v10
        v10 = -rsz * v00 + rcz * v10
        v00 = rcz * v00 + rsz * a10

        var v01 = rsz * v11
        v11 *= rcz

        val a12 = v12
        v12 = -rsz * v02 + rcz * a12
        v02 = rcz * v02 + rsz * a12

        v00 *= scaleX
        v01 *= scaleX
        v02 *= scaleX
        v10 *= scaleY
        v11 *= scaleY
        v12 *= scaleY
        v20 *= scaleZ
        v21 *= scaleZ
        v22 *= scaleZ

        // See top-level comment
        if (values.size < 16) return

        this[0, 0] = v00
        this[0, 1] = v01
        this[0, 2] = v02
        this[0, 3] = 0f
        this[1, 0] = v10
        this[1, 1] = v11
        this[1, 2] = v12
        this[1, 3] = 0f
        this[2, 0] = v20
        this[2, 1] = v21
        this[2, 2] = v22
        this[2, 3] = 0f
        this[3, 0] = -pivotX * v00 - pivotY * v10 + v30 + pivotX
        this[3, 1] = -pivotX * v01 - pivotY * v11 + v31 + pivotY
        this[3, 2] = -pivotX * v02 - pivotY * v12 + v32
        this[3, 3] = 1f
    }

    companion object {
        /** Index of the flattened array that represents the scale factor along the X axis */
        const val ScaleX = 0

        /** Index of the flattened array that represents the skew factor along the Y axis */
        const val SkewY = 1

        /** Index of the flattened array that represents the perspective factor along the X axis */
        const val Perspective0 = 3

        /** Index of the flattened array that represents the skew factor along the X axis */
        const val SkewX = 4

        /** Index of the flattened array that represents the scale factor along the Y axis */
        const val ScaleY = 5

        /** Index of the flattened array that represents the perspective factor along the Y axis */
        const val Perspective1 = 7

        /** Index of the flattened array that represents the scale factor along the Z axis */
        const val ScaleZ = 10

        /** Index of the flattened array that represents the translation along the X axis */
        const val TranslateX = 12

        /** Index of the flattened array that represents the translation along the Y axis */
        const val TranslateY = 13

        /** Index of the flattened array that represents the translation along the Z axis */
        const val TranslateZ = 14

        /** Index of the flattened array that represents the perspective factor along the Z axis */
        const val Perspective2 = 15
    }
}

private inline fun dot(m1: Matrix, row: Int, m2: Matrix, column: Int): Float {
    return m1[row, 0] * m2[0, column] +
        m1[row, 1] * m2[1, column] +
        m1[row, 2] * m2[2, column] +
        m1[row, 3] * m2[3, column]
}

/** Whether the given matrix is the identity matrix. */
fun Matrix.isIdentity(): Boolean {
    // See top-level comment
    val v = values
    if (v.size < 16) return false
    return v[0] == 1f &&
        v[1] == 0f &&
        v[2] == 0f &&
        v[3] == 0f &&
        v[4] == 0f &&
        v[5] == 1f &&
        v[6] == 0f &&
        v[7] == 0f &&
        v[8] == 0f &&
        v[9] == 0f &&
        v[10] == 1f &&
        v[11] == 0f &&
        v[12] == 0f &&
        v[13] == 0f &&
        v[14] == 0f &&
        v[15] == 1f
}
