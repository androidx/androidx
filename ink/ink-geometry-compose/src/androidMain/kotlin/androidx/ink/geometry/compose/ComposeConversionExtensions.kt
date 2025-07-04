/*
 * Copyright (C) 2025 The Android Open Source Project
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

package androidx.ink.geometry.compose

import androidx.compose.ui.geometry.MutableRect
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.graphics.Matrix
import androidx.ink.geometry.AffineTransform
import androidx.ink.geometry.Box
import androidx.ink.geometry.BoxAccumulator
import androidx.ink.geometry.ImmutableAffineTransform
import androidx.ink.geometry.ImmutableBox
import androidx.ink.geometry.ImmutableVec
import androidx.ink.geometry.MutableAffineTransform
import androidx.ink.geometry.MutableBox
import androidx.ink.geometry.MutableVec
import androidx.ink.geometry.Vec
import androidx.ink.geometry.compose.internal.threadLocal

/** Scratch space to be used as the argument to [BoxAccumulator.add]. */
private val boxAccumulatorScratchMutableBox by threadLocal { MutableBox() }

/** Returns an [Offset] with the values from this [Vec]. */
public fun Vec.toOffset(): Offset = Offset(x, y)

/**
 * Constructs an [ImmutableVec] with the values from [offset].
 *
 * Performance-sensitive code should use the [populateFrom] overload that takes a pre-allocated
 * [MutableVec], so that instance can be reused across multiple calls.
 */
public fun ImmutableVec.Companion.from(offset: Offset): ImmutableVec =
    ImmutableVec(offset.x, offset.y)

/**
 * Writes the values from [offset] to this [MutableVec].
 *
 * Returns the modified instance to allow chaining function calls.
 *
 * @return `this`
 */
public fun MutableVec.populateFrom(offset: Offset): MutableVec {
    x = offset.x
    y = offset.y
    return this
}

/** Constructs a [Rect] with the values from this [Box]. */
public fun Box.toRect(): Rect = Rect(xMin, yMin, xMax, yMax)

/**
 * Writes the values from this [Box] to [out].
 *
 * Returns the modified [MutableRect] instance to allow chaining calls.
 *
 * @return [out]
 */
public fun Box.populateMutableRect(out: MutableRect): MutableRect {
    out.left = xMin
    out.top = yMin
    out.right = xMax
    out.bottom = yMax
    return out
}

/**
 * If [rect] is not empty, returns an [ImmutableBox] constructed from the values of [rect].
 * Otherwise returns null.
 *
 * Performance-sensitive code should use the [BoxAccumulator.add] overload and obtain the bounding
 * box from there, so that instance can be reused across multiple calls.
 */
public fun ImmutableBox.Companion.from(rect: Rect): ImmutableBox? =
    if (rect.isEmpty) {
        null
    } else {
        fromTwoPoints(ImmutableVec(rect.left, rect.bottom), ImmutableVec(rect.right, rect.top))
    }

/**
 * If [rect] is not empty, returns an [ImmutableBox] constructed from the values of [rect].
 * Otherwise returns null.
 *
 * Performance-sensitive code should use the [BoxAccumulator.add] overload and obtain the bounding
 * box from there, so that instance can be reused across multiple calls.
 */
public fun ImmutableBox.Companion.from(rect: MutableRect): ImmutableBox? =
    if (rect.isEmpty) {
        null
    } else {
        fromTwoPoints(ImmutableVec(rect.left, rect.bottom), ImmutableVec(rect.right, rect.top))
    }

/**
 * Expands the accumulated bounding box (if necessary) such that it also contains [rect]. If [rect]
 * is empty, this is a no-op.
 *
 * Returns the modified instance to allow chaining calls.
 *
 * @return `this`
 */
public fun BoxAccumulator.add(rect: Rect): BoxAccumulator {
    if (!rect.isEmpty) {
        boxAccumulatorScratchMutableBox.setXBounds(rect.left, rect.right)
        boxAccumulatorScratchMutableBox.setYBounds(rect.top, rect.bottom)
        add(boxAccumulatorScratchMutableBox)
    }
    return this
}

/**
 * Expands the accumulated bounding box (if necessary) such that it also contains [rect]. If [rect]
 * is empty, this is a no-op.
 *
 * Returns the modified instance to allow chaining calls.
 *
 * @return `this`
 */
public fun BoxAccumulator.add(rect: MutableRect): BoxAccumulator {
    if (!rect.isEmpty) {
        boxAccumulatorScratchMutableBox.setXBounds(rect.left, rect.right)
        boxAccumulatorScratchMutableBox.setYBounds(rect.top, rect.bottom)
        add(boxAccumulatorScratchMutableBox)
    }
    return this
}

/** Helper function to determine if a [Matrix] is an affine transform. */
private fun isAffineMatrix(matrix: Matrix): Boolean {
    // NOMUTANTS -- this is a performance optimization to hint the size to the compiler.
    if (matrix.values.size != 16) return false
    return matrix.values[2] == 0f &&
        matrix.values[3] == 0f &&
        matrix.values[6] == 0f &&
        matrix.values[7] == 0f &&
        matrix.values[8] == 0f &&
        matrix.values[9] == 0f &&
        matrix.values[10] == 1f &&
        matrix.values[11] == 0f &&
        matrix.values[14] == 0f &&
        matrix.values[15] == 1f
}

/**
 * Constructs a [Matrix] with the values from the [AffineTransform].
 *
 * Performance-sensitive code should still use this because [Matrix] is an inline value class.
 */
public fun AffineTransform.toMatrix(): Matrix =
    Matrix(floatArrayOf(m00, m01, 0f, 0f, m10, m11, 0f, 0f, 0f, 0f, 1f, 0f, m20, m21, 0f, 1f))

/**
 * Writes the values from this [AffineTransform] to [out]. Note the conversion from a 3x3
 * [AffineTransform] to a 4x4 column-major [android.compose.ui.graphics.Matrix].
 *
 * Returns the modified [Matrix] instance to allow chaining calls.
 *
 * @return [out]
 */
public fun AffineTransform.populateMatrix(out: Matrix): Matrix {
    out.reset()
    out.values[Matrix.ScaleX] = m00
    out.values[Matrix.SkewY] = m01
    out.values[Matrix.SkewX] = m10
    out.values[Matrix.ScaleY] = m11
    out.values[Matrix.TranslateX] = m20
    out.values[Matrix.TranslateY] = m21
    return out
}

/**
 * Constructs an ImmutableAffineTransform with the values from [matrix], if and only if [matrix] is
 * a 2D affine transform; that is, that the Z row and column in [matrix] must both be `0, 0, 1, 0`.
 * Returns null if [matrix] is not a 2D affine transform.
 *
 * Performance-sensitive code should use the [populateFrom] overload that takes a pre-allocated
 * [AffineTransform], so that the instance can be reused across multiple calls.
 */
public fun ImmutableAffineTransform.Companion.from(matrix: Matrix): ImmutableAffineTransform? {
    if (!isAffineMatrix(matrix)) {
        return null
    }
    return ImmutableAffineTransform(
        matrix.values[Matrix.ScaleX],
        matrix.values[Matrix.SkewX],
        matrix.values[Matrix.TranslateX],
        matrix.values[Matrix.SkewY],
        matrix.values[Matrix.ScaleY],
        matrix.values[Matrix.TranslateY],
    )
}

/**
 * Fills this [MutableAffineTransform] with the values from [matrix].
 *
 * If [matrix] is not an affine transform, throws [IllegalArgumentException] instead.
 *
 * Leaves the input [matrix] unchanged. Returns the modified instance to allow chaining calls.
 *
 * @return `this`
 * @throws [IllegalArgumentException] if [matrix] is not a 2D affine transform.
 */
public fun MutableAffineTransform.populateFrom(matrix: Matrix): MutableAffineTransform {
    if (!isAffineMatrix(matrix)) {
        throw IllegalArgumentException("Matrix is not affine")
    }
    m00 = matrix.values[Matrix.ScaleX]
    m10 = matrix.values[Matrix.SkewX]
    m20 = matrix.values[Matrix.TranslateX]
    m01 = matrix.values[Matrix.SkewY]
    m11 = matrix.values[Matrix.ScaleY]
    m21 = matrix.values[Matrix.TranslateY]
    return this
}
