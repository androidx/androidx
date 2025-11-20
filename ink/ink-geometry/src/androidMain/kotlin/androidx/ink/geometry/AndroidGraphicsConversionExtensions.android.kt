/*
 * Copyright (C) 2024 The Android Open Source Project
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

@file:JvmName("AndroidGraphicsConverter")

package androidx.ink.geometry

import android.graphics.Matrix
import android.graphics.Path
import android.graphics.PointF
import android.graphics.RectF
import androidx.annotation.CheckResult
import androidx.annotation.IntRange
import androidx.ink.geometry.internal.threadLocal

/** Scratch space to be used as the argument to [Matrix.getValues] and [Matrix.setValues]. */
private val matrixValuesScratchArray by threadLocal { FloatArray(9) }

/** Scratch space to be used as the argument to [BoxAccumulator.add]. */
private val boxAccumulatorScratchMutableBox by threadLocal { MutableBox() }

/** Scratch space to be used as the argument to [PartitionedMesh.populateOutlinePosition]. */
private val populateOutlinePositionScratchMutableVec by threadLocal { MutableVec() }

/**
 * Constructs a [Matrix] with the values from the [AffineTransform].
 *
 * Performance-sensitive code should use the [populateMatrix] overload that takes a pre-allocated
 * [Matrix], so that the instance can be reused across multiple calls.
 */
public fun AffineTransform.toMatrix(): Matrix {
    getValues(matrixValuesScratchArray)
    matrixValuesScratchArray[Matrix.MPERSP_0] = 0f
    matrixValuesScratchArray[Matrix.MPERSP_1] = 0f
    matrixValuesScratchArray[Matrix.MPERSP_2] = 1f
    return Matrix().apply { setValues(matrixValuesScratchArray) }
}

/**
 * Writes the values from this [AffineTransform] to [out].
 *
 * Returns the modified [Matrix] to allow chaining calls.
 *
 * @return [out]
 */
public fun AffineTransform.populateMatrix(out: Matrix) {
    getValues(matrixValuesScratchArray)
    matrixValuesScratchArray[Matrix.MPERSP_0] = 0f
    matrixValuesScratchArray[Matrix.MPERSP_1] = 0f
    matrixValuesScratchArray[Matrix.MPERSP_2] = 1f
    out.setValues(matrixValuesScratchArray)
}

/**
 * Constructs an [ImmutableAffineTransform] with the values from [matrix].
 *
 * If [matrix] is not an affine transform, returns null instead.
 *
 * Performance-sensitive code should use the [populateFrom] overload that takes a pre-allocated
 * [MutableAffineTransform], so that the instance can be reused across multiple calls.
 *
 * Java callers should prefer `AndroidGraphicsConverter.createAffineTransform(Matrix)`
 * ([createAffineTransform]).
 */
public fun ImmutableAffineTransform.Companion.from(matrix: Matrix): ImmutableAffineTransform? {
    if (!matrix.isAffine) {
        return null
    }
    matrix.getValues(matrixValuesScratchArray)
    return ImmutableAffineTransform(matrixValuesScratchArray)
}

/**
 * Constructs an [ImmutableAffineTransform] with the values from [matrix].
 *
 * If [matrix] is not an affine transform, returns null instead.
 *
 * Performance-sensitive code should use the [populateFrom] overload that takes a pre-allocated
 * [MutableAffineTransform], so that the instance can be reused across multiple calls.
 *
 * Kotlin callers should prefer [ImmutableAffineTransform.Companion.from].
 */
public fun createAffineTransform(matrix: Matrix): ImmutableAffineTransform? =
    ImmutableAffineTransform.from(matrix)

/**
 * Fills this [MutableAffineTransform] with the values from [matrix].
 *
 * If [matrix] is not an affine transform, throws [IllegalArgumentException] instead.
 *
 * Leaves [matrix] unchanged. Returns this modified instance to allow chaining calls.
 *
 * @return `this`
 * @throws [IllegalArgumentException] if [matrix] is not an affine transform.
 */
public fun MutableAffineTransform.populateFrom(matrix: Matrix): MutableAffineTransform {
    if (!matrix.isAffine) {
        throw IllegalArgumentException("Matrix is not affine")
    }
    matrix.getValues(matrixValuesScratchArray)
    setValues(matrixValuesScratchArray)
    return this
}

/** Constructs a [PointF] with the same coordinates as the [Vec] */
public fun Vec.toPointF(): PointF = PointF(x, y)

/**
 * Writes the values from this [Vec] to [out].
 *
 * Returns the modified [PointF] instance to allow chaining calls.
 *
 * @return [out]
 */
public fun Vec.populatePointF(out: PointF): PointF {
    out.x = x
    out.y = y
    return out
}

/**
 * Constructs an [ImmutableVec] with the values from [point].
 *
 * Java callers should prefer `AndroidGraphicsConverter.createVec(PointF)`([createVec]).
 */
public fun ImmutableVec.Companion.from(point: PointF): ImmutableVec = ImmutableVec(point.x, point.y)

/**
 * Constructs an [ImmutableVec] with the values from [point].
 *
 * Kotlin callers should prefer [ImmutableVec.Companion.from].
 */
public fun createVec(point: PointF): ImmutableVec = ImmutableVec(point.x, point.y)

/**
 * Fills this [MutableVec] with the values from [point].
 *
 * Leaves [point] unchanged. Returns the modified instance to allow chaining calls.
 *
 * @return `this`
 */
public fun MutableVec.populateFrom(point: PointF): MutableVec {
    x = point.x
    y = point.y
    return this
}

/** Constructs a [Rect] with the same coordinates as the [Box] */
public fun Box.toRectF(): RectF = RectF(xMin, yMin, xMax, yMax)

/**
 * Writes the values from this [Box] to [out].
 *
 * Returns the modified [RectF] instance to allow chaining calls.
 *
 * @return [out]
 */
public fun Box.populateRectF(out: RectF): RectF {
    out.left = xMin
    out.top = yMin
    out.right = xMax
    out.bottom = yMax
    return out
}

/**
 * Constructs an [ImmutableBox] with the values from [rect].
 *
 * If [rect] is empty, returns null instead.
 *
 * Java callers should prefer `AndroidGraphicsConverter.createBox`([createBox]).
 */
public fun ImmutableBox.Companion.from(rect: RectF): ImmutableBox? =
    if (rect.isEmpty) {
        null
    } else {
        fromTwoPoints(ImmutableVec(rect.left, rect.bottom), ImmutableVec(rect.right, rect.top))
    }

/**
 * Constructs an [ImmutableBox] with the values from [rect].
 *
 * If [rect] is empty, returns null instead.
 *
 * Kotlin callers should prefer [ImmutableBox.Companion.from].
 */
public fun createBox(rect: RectF): ImmutableBox? = ImmutableBox.from(rect)

/**
 * Expands the accumulated bounding box (if necessary) such that it also contains [rect]. If [rect]
 * is null, this is a no-op.
 *
 * This is functionally equivalent to, but more efficient than: `add(ImmutableBox.from(rect))`
 *
 * @return `this`
 */
public fun BoxAccumulator.add(rect: RectF): BoxAccumulator {
    if (!rect.isEmpty) {
        boxAccumulatorScratchMutableBox.setXBounds(rect.left, rect.right)
        boxAccumulatorScratchMutableBox.setYBounds(rect.top, rect.bottom)
        add(boxAccumulatorScratchMutableBox)
    }
    return this
}

/** Returns a [Path] containing the outlines in the render group at [renderGroupIndex]. */
public fun PartitionedMesh.outlinesToPath(@IntRange(from = 0) renderGroupIndex: Int): Path =
    populateOutlines(renderGroupIndex, Path())

/**
 * Replaces the contents of [out] with the outline of the render group at [renderGroupIndex].
 *
 * Returns the modified [Path] to allow chaining calls.
 *
 * @return [out]
 */
public fun PartitionedMesh.populateOutlines(
    @IntRange(from = 0) renderGroupIndex: Int,
    out: Path,
): Path {
    out.rewind()
    for (outlineIndex in 0 until getOutlineCount(renderGroupIndex)) {
        val outlineVertexCount = getOutlineVertexCount(renderGroupIndex, outlineIndex)
        if (outlineVertexCount == 0) continue

        populateOutlinePosition(
            renderGroupIndex,
            outlineIndex,
            0,
            populateOutlinePositionScratchMutableVec,
        )
        out.moveTo(
            populateOutlinePositionScratchMutableVec.x,
            populateOutlinePositionScratchMutableVec.y,
        )

        for (outlineVertexIndex in 1 until outlineVertexCount) {
            populateOutlinePosition(
                renderGroupIndex,
                outlineIndex,
                outlineVertexIndex,
                populateOutlinePositionScratchMutableVec,
            )
            out.lineTo(
                populateOutlinePositionScratchMutableVec.x,
                populateOutlinePositionScratchMutableVec.y,
            )
        }

        out.close()
    }
    return out
}

/**
 * Fill the given [RectF] with the bounds of this [BoxAccumulator], returning whether or not the
 * object was actually overwritten.
 */
@CheckResult
public fun BoxAccumulator.getBounds(outRect: RectF): Boolean {
    box?.let {
        outRect.set(it.xMin, it.yMin, it.xMax, it.yMax)
        return true
    }
    return false
}
