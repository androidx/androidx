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

package androidx.compose.ui.graphics

import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.geometry.RoundRect
import org.jetbrains.skia.Matrix33
import org.jetbrains.skia.PathDirection
import org.jetbrains.skia.PathFillMode
import org.jetbrains.skia.PathOp

actual fun Path(): Path = SkiaBackedPath()

/**
 * Convert the [org.jetbrains.skia.Path] instance into a Compose-compatible Path
 */
fun org.jetbrains.skia.Path.asComposePath(): Path = SkiaBackedPath(this)

/**
 * Obtain a reference to the [org.jetbrains.skia.Path]
 *
 * @Throws UnsupportedOperationException if this Path is not backed by an org.jetbrains.skia.Path
 */
fun Path.asSkiaPath(): org.jetbrains.skia.Path =
    if (this is SkiaBackedPath) {
        internalPath
    } else {
        throw UnsupportedOperationException("Unable to obtain org.jetbrains.skia.Path")
    }

internal class SkiaBackedPath(
    internalPath: org.jetbrains.skia.Path = org.jetbrains.skia.Path()
) : Path {
    var internalPath = internalPath
        private set

    override var fillType: PathFillType
        get() {
            if (internalPath.fillMode == PathFillMode.EVEN_ODD) {
                return PathFillType.EvenOdd
            } else {
                return PathFillType.NonZero
            }
        }

        set(value) {
            internalPath.fillMode =
                if (value == PathFillType.EvenOdd) {
                    PathFillMode.EVEN_ODD
                } else {
                    PathFillMode.WINDING
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

    override fun relativeQuadraticBezierTo(dx1: Float, dy1: Float, dx2: Float, dy2: Float) {
        internalPath.rQuadTo(dx1, dy1, dx2, dy2)
    }

    override fun cubicTo(x1: Float, y1: Float, x2: Float, y2: Float, x3: Float, y3: Float) {
        internalPath.cubicTo(
            x1, y1,
            x2, y2,
            x3, y3
        )
    }

    override fun relativeCubicTo(
        dx1: Float,
        dy1: Float,
        dx2: Float,
        dy2: Float,
        dx3: Float,
        dy3: Float
    ) {
        internalPath.rCubicTo(
            dx1, dy1,
            dx2, dy2,
            dx3, dy3
        )
    }

    override fun arcTo(
        rect: Rect,
        startAngleDegrees: Float,
        sweepAngleDegrees: Float,
        forceMoveTo: Boolean
    ) {
        internalPath.arcTo(
            rect.toSkiaRect(),
            startAngleDegrees,
            sweepAngleDegrees,
            forceMoveTo
        )
    }

    override fun addRect(rect: Rect) {
        internalPath.addRect(rect.toSkiaRect(), PathDirection.COUNTER_CLOCKWISE)
    }

    override fun addOval(oval: Rect) {
        internalPath.addOval(oval.toSkiaRect(), PathDirection.COUNTER_CLOCKWISE)
    }

    override fun addArcRad(oval: Rect, startAngleRadians: Float, sweepAngleRadians: Float) {
        addArc(oval, degrees(startAngleRadians), degrees(sweepAngleRadians))
    }

    override fun addArc(oval: Rect, startAngleDegrees: Float, sweepAngleDegrees: Float) {
        internalPath.addArc(oval.toSkiaRect(), startAngleDegrees, sweepAngleDegrees)
    }

    override fun addRoundRect(roundRect: RoundRect) {
        internalPath.addRRect(roundRect.toSkiaRRect(), PathDirection.COUNTER_CLOCKWISE)
    }

    override fun addPath(path: Path, offset: Offset) {
        internalPath.addPath(path.asSkiaPath(), offset.x, offset.y)
    }

    override fun close() {
        internalPath.closePath()
    }

    override fun reset() {
        // preserve fillType to match the Android behavior
        // see https://cs.android.com/android/_/android/platform/frameworks/base/+/d0f379c1976c600313f1f4c39f2587a649e3a4fc
        val fillType = this.fillType
        internalPath.reset()
        this.fillType = fillType
    }

    override fun rewind() {
        internalPath.rewind()
    }

    override fun translate(offset: Offset) {
        internalPath.transform(Matrix33.makeTranslate(offset.x, offset.y))
    }

    override fun transform(matrix: Matrix) {
        internalPath.transform(Matrix33.makeTranslate(0f, 0f).apply { setFrom(matrix) })
    }

    override fun getBounds(): Rect {
        val bounds = internalPath.bounds
        return Rect(
            bounds.left,
            bounds.top,
            bounds.right,
            bounds.bottom
        )
    }

    override fun op(
        path1: Path,
        path2: Path,
        operation: PathOperation
    ): Boolean {
        val path = org.jetbrains.skia.Path.makeCombining(
            path1.asSkiaPath(),
            path2.asSkiaPath(),
            operation.toSkiaOperation()
        )

        internalPath = path ?: internalPath
        return path != null
    }

    private fun PathOperation.toSkiaOperation() = when (this) {
        PathOperation.Difference -> PathOp.DIFFERENCE
        PathOperation.Intersect -> PathOp.INTERSECT
        PathOperation.Union -> PathOp.UNION
        PathOperation.Xor -> PathOp.XOR
        PathOperation.ReverseDifference -> PathOp.REVERSE_DIFFERENCE
        else -> PathOp.XOR
    }

    override val isConvex: Boolean get() = internalPath.isConvex

    override val isEmpty: Boolean get() = internalPath.isEmpty

    fun Matrix33.setFrom(matrix: Matrix) {
        require(
            matrix[0, 2] == 0f &&
                matrix[1, 2] == 0f &&
                matrix[2, 2] == 1f &&
                matrix[3, 2] == 0f &&
                matrix[2, 0] == 0f &&
                matrix[2, 1] == 0f &&
                matrix[2, 3] == 0f
        ) {
            "Matrix33 does not support arbitrary transforms"
        }

        // We'll reuse the array used in Matrix to avoid allocation by temporarily
        // setting it to the 3x3 matrix used by android.graphics.Matrix
        // Store the values of the 4 x 4 matrix into temporary variables
        // to be reset after the 3 x 3 matrix is configured
        val scaleX = matrix.values[Matrix.ScaleX] // 0
        val skewY = matrix.values[Matrix.SkewY] // 1
        val v2 = matrix.values[2] // 2
        val persp0 = matrix.values[Matrix.Perspective0] // 3
        val skewX = matrix.values[Matrix.SkewX] // 4
        val scaleY = matrix.values[Matrix.ScaleY] // 5
        val v6 = matrix.values[6] // 6
        val persp1 = matrix.values[Matrix.Perspective1] // 7
        val v8 = matrix.values[8] // 8

        val translateX = matrix.values[Matrix.TranslateX]
        val translateY = matrix.values[Matrix.TranslateY]
        val persp2 = matrix.values[Matrix.Perspective2]

        val v = matrix.values

        v[0] = scaleX // MSCALE_X = 0
        v[1] = skewX // MSKEW_X = 1
        v[2] = translateX // MTRANS_X = 2
        v[3] = skewY // MSKEW_Y = 3
        v[4] = scaleY // MSCALE_Y = 4
        v[5] = translateY // MTRANS_Y
        v[6] = persp0 // MPERSP_0 = 6
        v[7] = persp1 // MPERSP_1 = 7
        v[8] = persp2 // MPERSP_2 = 8

        for (i in 0..8) {
            mat[i] = v[i]
        }

        // Reset the values back after the android.graphics.Matrix is configured
        v[Matrix.ScaleX] = scaleX // 0
        v[Matrix.SkewY] = skewY // 1
        v[2] = v2 // 2
        v[Matrix.Perspective0] = persp0 // 3
        v[Matrix.SkewX] = skewX // 4
        v[Matrix.ScaleY] = scaleY // 5
        v[6] = v6 // 6
        v[Matrix.Perspective1] = persp1 // 7
        v[8] = v8 // 8
    }
}