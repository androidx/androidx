/*
 * Copyright 2018 The Android Open Source Project
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

package androidx.compose.ui.graphics

import android.graphics.Matrix as PlatformMatrix
import android.graphics.Path as PlatformPath
import android.graphics.RectF as PlatformRectF
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.geometry.RoundRect

actual fun Path(): Path = AndroidPath()

/** Convert the [android.graphics.Path] instance into a Compose-compatible Path */
fun PlatformPath.asComposePath(): Path = AndroidPath(this)

/**
 * @Throws UnsupportedOperationException if this Path is not backed by an [android.graphics.Path].
 */
@Suppress("NOTHING_TO_INLINE")
inline fun Path.asAndroidPath(): PlatformPath =
    if (this is AndroidPath) {
        internalPath
    } else {
        throw UnsupportedOperationException("Unable to obtain android.graphics.Path")
    }

@Suppress("OVERRIDE_DEPRECATION") // b/407491706
/* actual */ class AndroidPath(val internalPath: PlatformPath = PlatformPath()) : Path {

    // Temporary value holders to reuse an object (not part of a state):
    private var rectF: PlatformRectF? = null
    private var radii: FloatArray? = null
    private var mMatrix: PlatformMatrix? = null

    override var fillType: PathFillType
        get() {
            return if (internalPath.fillType == PlatformPath.FillType.EVEN_ODD) {
                PathFillType.EvenOdd
            } else {
                PathFillType.NonZero
            }
        }
        set(value) {
            internalPath.fillType =
                if (value == PathFillType.EvenOdd) {
                    PlatformPath.FillType.EVEN_ODD
                } else {
                    PlatformPath.FillType.WINDING
                }
        }

    override fun moveTo(x: Float, y: Float) {
        internalPath.moveTo(x, y)
    }

    override fun relativeMoveTo(dx: Float, dy: Float) {
        internalPath.rMoveTo(dx, dy)
    }

    override fun lineTo(x: Float, y: Float) {
        internalPath.lineTo(x, y)
    }

    override fun relativeLineTo(dx: Float, dy: Float) {
        internalPath.rLineTo(dx, dy)
    }

    override fun quadraticBezierTo(x1: Float, y1: Float, x2: Float, y2: Float) {
        internalPath.quadTo(x1, y1, x2, y2)
    }

    override fun quadraticTo(x1: Float, y1: Float, x2: Float, y2: Float) {
        internalPath.quadTo(x1, y1, x2, y2)
    }

    override fun relativeQuadraticBezierTo(dx1: Float, dy1: Float, dx2: Float, dy2: Float) {
        internalPath.rQuadTo(dx1, dy1, dx2, dy2)
    }

    override fun relativeQuadraticTo(dx1: Float, dy1: Float, dx2: Float, dy2: Float) {
        internalPath.rQuadTo(dx1, dy1, dx2, dy2)
    }

    override fun cubicTo(x1: Float, y1: Float, x2: Float, y2: Float, x3: Float, y3: Float) {
        internalPath.cubicTo(x1, y1, x2, y2, x3, y3)
    }

    override fun relativeCubicTo(
        dx1: Float,
        dy1: Float,
        dx2: Float,
        dy2: Float,
        dx3: Float,
        dy3: Float,
    ) {
        internalPath.rCubicTo(dx1, dy1, dx2, dy2, dx3, dy3)
    }

    override fun arcTo(
        rect: Rect,
        startAngleDegrees: Float,
        sweepAngleDegrees: Float,
        forceMoveTo: Boolean,
    ) {
        val left = rect.left
        val top = rect.top
        val right = rect.right
        val bottom = rect.bottom
        if (rectF == null) rectF = PlatformRectF()
        rectF!!.set(left, top, right, bottom)
        internalPath.arcTo(rectF!!, startAngleDegrees, sweepAngleDegrees, forceMoveTo)
    }

    override fun addRect(@Suppress("InvalidNullabilityOverride") rect: Rect) {
        addRect(rect, Path.Direction.CounterClockwise)
    }

    override fun addRect(rect: Rect, direction: Path.Direction) {
        validateRectangle(rect)
        if (rectF == null) rectF = PlatformRectF()
        rectF!!.set(rect.left, rect.top, rect.right, rect.bottom)
        internalPath.addRect(rectF!!, direction.toPlatformPathDirection())
    }

    override fun addOval(@Suppress("InvalidNullabilityOverride") oval: Rect) {
        addOval(oval, Path.Direction.CounterClockwise)
    }

    override fun addOval(oval: Rect, direction: Path.Direction) {
        if (rectF == null) rectF = PlatformRectF()
        rectF!!.set(oval.left, oval.top, oval.right, oval.bottom)
        internalPath.addOval(rectF!!, direction.toPlatformPathDirection())
    }

    override fun addRoundRect(@Suppress("InvalidNullabilityOverride") roundRect: RoundRect) {
        addRoundRect(roundRect, Path.Direction.CounterClockwise)
    }

    override fun addRoundRect(roundRect: RoundRect, direction: Path.Direction) {
        if (rectF == null) rectF = PlatformRectF()
        rectF!!.set(roundRect.left, roundRect.top, roundRect.right, roundRect.bottom)

        if (radii == null) radii = FloatArray(8)
        with(radii!!) {
            this[0] = roundRect.topLeftCornerRadius.x
            this[1] = roundRect.topLeftCornerRadius.y

            this[2] = roundRect.topRightCornerRadius.x
            this[3] = roundRect.topRightCornerRadius.y

            this[4] = roundRect.bottomRightCornerRadius.x
            this[5] = roundRect.bottomRightCornerRadius.y

            this[6] = roundRect.bottomLeftCornerRadius.x
            this[7] = roundRect.bottomLeftCornerRadius.y
        }
        internalPath.addRoundRect(rectF!!, radii!!, direction.toPlatformPathDirection())
    }

    override fun addArcRad(oval: Rect, startAngleRadians: Float, sweepAngleRadians: Float) {
        addArc(oval, degrees(startAngleRadians), degrees(sweepAngleRadians))
    }

    override fun addArc(oval: Rect, startAngleDegrees: Float, sweepAngleDegrees: Float) {
        validateRectangle(oval)
        if (rectF == null) rectF = PlatformRectF()
        rectF!!.set(oval.left, oval.top, oval.right, oval.bottom)
        internalPath.addArc(rectF!!, startAngleDegrees, sweepAngleDegrees)
    }

    override fun addPath(path: Path, offset: Offset) {
        internalPath.addPath(path.asAndroidPath(), offset.x, offset.y)
    }

    override fun close() {
        internalPath.close()
    }

    override fun reset() {
        internalPath.reset()
    }

    override fun rewind() {
        internalPath.rewind()
    }

    override fun translate(offset: Offset) {
        if (mMatrix == null) mMatrix = PlatformMatrix() else mMatrix!!.reset()
        mMatrix!!.setTranslate(offset.x, offset.y)
        internalPath.transform(mMatrix!!)
    }

    override fun transform(matrix: Matrix) {
        if (mMatrix == null) mMatrix = PlatformMatrix()
        mMatrix!!.setFrom(matrix)
        internalPath.transform(mMatrix!!)
    }

    override fun getBounds(): Rect {
        if (rectF == null) rectF = PlatformRectF()
        with(rectF!!) {
            @Suppress("DEPRECATION") internalPath.computeBounds(this, true)
            return Rect(this.left, this.top, this.right, this.bottom)
        }
    }

    override fun op(path1: Path, path2: Path, operation: PathOperation): Boolean {
        val op =
            when (operation) {
                PathOperation.Difference -> PlatformPath.Op.DIFFERENCE
                PathOperation.Intersect -> PlatformPath.Op.INTERSECT
                PathOperation.ReverseDifference -> PlatformPath.Op.REVERSE_DIFFERENCE
                PathOperation.Union -> PlatformPath.Op.UNION
                else -> PlatformPath.Op.XOR
            }
        return internalPath.op(path1.asAndroidPath(), path2.asAndroidPath(), op)
    }

    @Suppress("DEPRECATION") // Path.isConvex
    override val isConvex: Boolean
        get() = internalPath.isConvex

    override val isEmpty: Boolean
        get() = internalPath.isEmpty

    private fun validateRectangle(rect: Rect) {
        if (rect.left.isNaN() || rect.top.isNaN() || rect.right.isNaN() || rect.bottom.isNaN()) {
            throwIllegalStateException("Invalid rectangle, make sure no value is NaN")
        }
    }
}

internal fun throwIllegalStateException(message: String) {
    throw IllegalStateException(message)
}

private fun Path.Direction.toPlatformPathDirection() =
    when (this) {
        Path.Direction.CounterClockwise -> PlatformPath.Direction.CCW
        Path.Direction.Clockwise -> PlatformPath.Direction.CW
    }
