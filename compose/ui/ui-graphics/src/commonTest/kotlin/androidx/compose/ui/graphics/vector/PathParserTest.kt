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

package androidx.compose.ui.graphics.vector

import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.geometry.RoundRect
import androidx.compose.ui.graphics.Matrix
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.PathFillType
import androidx.compose.ui.graphics.PathOperation
import kotlin.test.Test
import kotlin.test.assertEquals

class PathParserTest {
    @Test
    fun negativeExponent() {
        val linePath = object : TestPath() {
            var lineToPoints = ArrayList<Offset>()

            override fun lineTo(x: Float, y: Float) {
                lineToPoints.add(Offset(x, y))
            }
        }

        val parser = PathParser()
        parser.parsePathString("H1e-5").toPath(linePath)

        assertEquals(1, linePath.lineToPoints.size)
        assertEquals(1e-5f, linePath.lineToPoints[0].x)
    }

    @Test
    fun dotDot() {
        val linePath = object : TestPath() {
            var lineToPoints = ArrayList<Offset>()

            override fun relativeLineTo(dx: Float, dy: Float) {
                lineToPoints.add(Offset(dx, dy))
            }
        }

        val parser = PathParser()
        parser.parsePathString("m0 0l2..5").toPath(linePath)

        assertEquals(1, linePath.lineToPoints.size)
        assertEquals(2.0f, linePath.lineToPoints[0].x)
        assertEquals(0.5f, linePath.lineToPoints[0].y)
    }

    @Test
    fun relativeMoveToBecomesRelativeLineTo() {
        val linePath = object : TestPath() {
            var lineToPoints = ArrayList<Offset>()

            override fun relativeLineTo(dx: Float, dy: Float) {
                lineToPoints.add(Offset(dx, dy))
            }
        }

        val parser = PathParser()
        parser.parsePathString("m0 0 2 5").toPath(linePath)

        assertEquals(1, linePath.lineToPoints.size)
        assertEquals(2.0f, linePath.lineToPoints[0].x)
        assertEquals(5.0f, linePath.lineToPoints[0].y)
    }

    @Test
    fun moveToBecomesLineTo() {
        val linePath = object : TestPath() {
            var lineToPoints = ArrayList<Offset>()

            override fun lineTo(x: Float, y: Float) {
                lineToPoints.add(Offset(x, y))
            }
        }

        val parser = PathParser()
        parser.parsePathString("M0 0 2 5 6 7").toPath(linePath)

        assertEquals(2, linePath.lineToPoints.size)
        assertEquals(2.0f, linePath.lineToPoints[0].x)
        assertEquals(5.0f, linePath.lineToPoints[0].y)
        assertEquals(6.0f, linePath.lineToPoints[1].x)
        assertEquals(7.0f, linePath.lineToPoints[1].y)
    }

    @Test
    fun relativeQuadToTest() {
        val quadPath = object : TestPath() {
            var lineToPoints = ArrayList<Offset>()

            override fun lineTo(x: Float, y: Float) {
                lineToPoints.add(Offset(x, y))
            }
        }

        // After a relative quad operation, ensure that the currentPoint is updated
        // properly. In order to do so, verify that the y coordindate of an
        // absolute horizontal line matches the 4th parameter of the relative
        // quadratic and that the x coordinate of an absolute line matches the 3rd
        // parameter of the relative quadratic
        val parser = PathParser()
        parser.parsePathString("q5, 5 7, 10H5").toPath(quadPath)

        assertEquals(1, quadPath.lineToPoints.size)
        assertEquals(10f, quadPath.lineToPoints[0].y)

        quadPath.lineToPoints.clear()

        parser.parsePathString("q5, 5, 8, 8 V5").toPath(quadPath)
        assertEquals(1, quadPath.lineToPoints.size)
        assertEquals(8f, quadPath.lineToPoints[0].x)
    }

    /**
     * Path that implements the Path interface with stubs to allow for simple implementations
     * to override individual methods for testing
     */
    @Suppress("OVERRIDE_DEPRECATION")
    open class TestPath : Path {
        override var fillType: PathFillType = PathFillType.EvenOdd
        override val isConvex: Boolean = false

        override val isEmpty: Boolean = false

        override fun moveTo(x: Float, y: Float) {
            // NO-OP
        }

        override fun relativeMoveTo(dx: Float, dy: Float) {
            // NO-OP
        }

        override fun lineTo(x: Float, y: Float) {
            // NO-OP
        }

        override fun relativeLineTo(dx: Float, dy: Float) {
            // NO-OP
        }

        override fun quadraticBezierTo(x1: Float, y1: Float, x2: Float, y2: Float) {
            // NO-OP
        }

        override fun quadraticTo(x1: Float, y1: Float, x2: Float, y2: Float) {
            // NO-OP
        }

        override fun relativeQuadraticBezierTo(dx1: Float, dy1: Float, dx2: Float, dy2: Float) {
            // NO-OP
        }

        override fun relativeQuadraticTo(dx1: Float, dy1: Float, dx2: Float, dy2: Float) {
            // NO-OP
        }

        override fun cubicTo(x1: Float, y1: Float, x2: Float, y2: Float, x3: Float, y3: Float) {
            // NO-OP
        }

        override fun relativeCubicTo(
            dx1: Float,
            dy1: Float,
            dx2: Float,
            dy2: Float,
            dx3: Float,
            dy3: Float
        ) {
            // NO-OP
        }

        override fun arcTo(
            rect: Rect,
            startAngleDegrees: Float,
            sweepAngleDegrees: Float,
            forceMoveTo: Boolean
        ) {
            // NO-OP
        }

        override fun addRect(rect: Rect) {
            // NO-OP
        }

        override fun addRect(rect: Rect, direction: Path.Direction) {
            // NO-OP
        }

        override fun addOval(oval: Rect) {
            // NO-OP
        }

        override fun addOval(oval: Rect, direction: Path.Direction) {
            // NO-OP
        }

        override fun addRoundRect(roundRect: RoundRect) {
            // NO-OP
        }

        override fun addRoundRect(roundRect: RoundRect, direction: Path.Direction) {
            // NO-OP
        }

        override fun addArcRad(oval: Rect, startAngleRadians: Float, sweepAngleRadians: Float) {
            // NO-OP
        }

        override fun addArc(oval: Rect, startAngleDegrees: Float, sweepAngleDegrees: Float) {
            // NO-OP
        }

        override fun addPath(path: Path, offset: Offset) {
            // NO-OP
        }

        override fun close() {
            // NO-OP
        }

        override fun reset() {
            // NO-OP
        }

        override fun rewind() {
            // NO-OP
        }

        override fun translate(offset: Offset) {
            // NO-OP
        }

        override fun transform(matrix: Matrix) {
            // NO-OP
        }

        override fun getBounds(): Rect = Rect.Zero

        override fun op(path1: Path, path2: Path, operation: PathOperation): Boolean = false
    }
}
