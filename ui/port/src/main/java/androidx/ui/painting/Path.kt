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

package androidx.ui.painting

import android.os.Build
import androidx.annotation.RequiresApi
import androidx.ui.engine.geometry.Offset
import androidx.ui.engine.geometry.RRect
import androidx.ui.engine.geometry.Radius
import androidx.ui.engine.geometry.Rect
import androidx.ui.vectormath64.Matrix4

class Path {

    private val internalPath = android.graphics.Path();
    private val rectF by lazy {
        android.graphics.RectF();
    }

    private val radii by lazy {
        FloatArray(8)
    }

    private val mMatrix by lazy {
        android.graphics.Matrix();
    }

    fun getFrameworkPath(): android.graphics.Path = internalPath;

    /// Determines how the interior of this path is calculated.
    ///
    /// Defaults to the non-zero winding rule, [PathFillType.nonZero].
    fun getFillType() : PathFillType {
        if (internalPath.fillType == android.graphics.Path.FillType.EVEN_ODD) {
            return PathFillType.evenOdd;
        } else {
            return PathFillType.nonZero;
        }
    }

    fun setFillType(value : PathFillType) {
        internalPath.fillType =
            if (value == PathFillType.evenOdd) {
                android.graphics.Path.FillType.EVEN_ODD
            } else {
                android.graphics.Path.FillType.WINDING;
            }
    }

    /// Starts a new subpath at the given coordinate.
    fun moveTo(dx: Double, dy: Double) {
        internalPath.moveTo(dx.toFloat(), dy.toFloat());
    }

    /// Starts a new subpath at the given offset from the current point.
    fun relativeMoveTo(dx : Double, dy: Double) {
        internalPath.rMoveTo(dx.toFloat(), dy.toFloat());
    }

    /// Adds a straight line segment from the current point to the given
    /// point.
    fun lineTo(dx: Double, dy: Double) {
        internalPath.lineTo(dx.toFloat(), dy.toFloat());
    }

    /// Adds a straight line segment from the current point to the point
    /// at the given offset from the current point.
    fun relativeLineTo(dx: Double, dy: Double) {
        internalPath.rLineTo(dx.toFloat(), dy.toFloat());
    }

    /// Adds a quadratic bezier segment that curves from the current
    /// point to the given point (x2,y2), using the control point
    /// (x1,y1).
    fun quadraticBezierTo(x1 : Double, y1: Double, x2: Double, y2: Double) {
        internalPath.quadTo(x1.toFloat(), y1.toFloat(), x2.toFloat(), y2.toFloat());
    }

    /// Adds a quadratic bezier segment that curves from the current
    /// point to the point at the offset (x2,y2) from the current point,
    /// using the control point at the offset (x1,y1) from the current
    /// point.
    fun relativeQuadraticBezierTo(x1: Double, y1: Double, x2: Double, y2: Double) {
        internalPath.rQuadTo(x1.toFloat(), y1.toFloat(), x2.toFloat(), y2.toFloat());
    }

    /// Adds a cubic bezier segment that curves from the current point
    /// to the given point (x3,y3), using the control points (x1,y1) and
    /// (x2,y2).
    fun cubicTo(x1: Double, y1: Double, x2: Double, y2: Double, x3: Double, y3: Double) {
        internalPath.cubicTo(
                x1.toFloat(), y1.toFloat(),
                x2.toFloat(), y2.toFloat(),
                x3.toFloat(), y3.toFloat());
    }

    /// Adds a cubic bezier segment that curves from the current point
    /// to the point at the offset (x3,y3) from the current point, using
    /// the control points at the offsets (x1,y1) and (x2,y2) from the
    /// current point.
    fun relativeCubicTo(x1: Double, y1: Double, x2: Double, y2: Double, x3: Double, y3: Double) {
        internalPath.rCubicTo(
                x1.toFloat(), y1.toFloat(),
                x2.toFloat(), y2.toFloat(),
                x3.toFloat(), y3.toFloat());
    }

    /// Adds a bezier segment that curves from the current point to the
    /// given point (x2,y2), using the control points (x1,y1) and the
    /// weight w. If the weight is greater than 1, then the curve is a
    /// hyperbola; if the weight equals 1, it's a parabola; and if it is
    /// less than 1, it is an ellipse.
    fun conicTo(x1: Double, y1: Double, x2: Double, y2: Double, w: Double) {
        // TODO(Migration/njawad) figure out how to handle unsupported framework Path operations
        throw UnsupportedOperationException("conicTo not supported in framework Path");
    }

    /// Adds a bezier segment that curves from the current point to the
    /// point at the offset (x2,y2) from the current point, using the
    /// control point at the offset (x1,y1) from the current point and
    /// the weight w. If the weight is greater than 1, then the curve is
    /// a hyperbola; if the weight equals 1, it's a parabola; and if it
    /// is less than 1, it is an ellipse.
    fun relativeConicTo(x1: Double, y1: Double, x2: Double, y2: Double, w: Double) {
        // TODO(Migration/njawad) figure out how to handle unsupported framework Path operations
        throw UnsupportedOperationException("relativeConicTo not supported in framework Path");
    }

    /// If the `forceMoveTo` argument is false, adds a straight line
    /// segment and an arc segment.
    ///
    /// If the `forceMoveTo` argument is true, starts a new subpath
    /// consisting of an arc segment.
    ///
    /// In either case, the arc segment consists of the arc that follows
    /// the edge of the oval bounded by the given rectangle, from
    /// startAngle radians around the oval up to startAngle + sweepAngle
    /// radians around the oval, with zero radians being the point on
    /// the right hand side of the oval that crosses the horizontal line
    /// that intersects the center of the rectangle and with positive
    /// angles going clockwise around the oval.
    ///
    /// The line segment added if `forceMoveTo` is false starts at the
    /// current point and ends at the start of the arc.
    fun arcTo(rect: Rect, startAngle: Double, sweepAngle: Double, forceMoveTo: Boolean) {
        val left = rect.left.toFloat();
        val top = rect.top.toFloat();
        val right = rect.right.toFloat();
        val bottom = rect.bottom.toFloat();
        rectF.set(left, top, right, bottom);
        internalPath.arcTo(
                rectF,
                startAngle.toFloat(),
                sweepAngle.toFloat(),
                forceMoveTo
        )
    }

    /// Appends up to four conic curves weighted to describe an oval of `radius`
    /// and rotated by `rotation`.
    ///
    /// The first curve begins from the last point in the path and the last ends
    /// at `arcEnd`. The curves follow a path in a direction determined by
    /// `clockwise` and `largeArc` in such a way that the sweep angle
    /// is always less than 360 degrees.
    ///
    /// A simple line is appended if either either radii are zero or the last
    /// point in the path is `arcEnd`. The radii are scaled to fit the last path
    /// point if both are greater than zero but too small to describe an arc.
    ///
    fun arcToPoint(arcEnd: Offset, radius: Radius = Radius.zero, rotation: Double = 0.0,
            largeArc: Boolean = false, clockwise: Boolean = true) {
        // TODO(Migration/njawad) figure out how to handle unsupported framework Path operations
        throw UnsupportedOperationException("arcToPoint not supported in framework Path");
    }


    private fun _arcToPoint(arcEndX: Double, arcEndY: Double, radius: Double, radiusY: Double,
            rotation: Double, largeArc: Boolean, clockwise: Boolean) {
        // TODO(Migration/njawad: figure out how to handle unsupported framework Path operations)
        TODO()
        // Flutter calls into native Path logic here
        // native 'Path_arcToPoint'
    }

    /// Appends up to four conic curves weighted to describe an oval of `radius`
    /// and rotated by `rotation`.
    ///
    /// The last path point is described by (px, py).
    ///
    /// The first curve begins from the last point in the path and the last ends
    /// at `arcEndDelta.dx + px` and `arcEndDelta.dy + py`. The curves follow a
    /// path in a direction determined by `clockwise` and `largeArc`
    /// in such a way that the sweep angle is always less than 360 degrees.
    ///
    /// A simple line is appended if either either radii are zero, or, both
    /// `arcEndDelta.dx` and `arcEndDelta.dy` are zero. The radii are scaled to
    /// fit the last path point if both are greater than zero but too small to
    /// describe an arc.
    fun relativeArcToPoint(arcEndDelta: Offset, radius: Radius = Radius.zero,
        rotation: Double = 0.0,
        largeArc: Boolean = false,
        clockwise: Boolean = true) {
        _relativeArcToPoint(arcEndDelta.dx, arcEndDelta.dy, radius.x, radius.y,
                rotation, largeArc, clockwise)
    }

    private fun _relativeArcToPoint(arcEndX: Double, arcEndY: Double, radius: Double, radiusY: Double,
            rotation: Double, largeArc: Boolean, clockwise: Boolean) {
        // TODO(Migration/njawad: figure out how to handle unsupported framework Path operations)
        TODO()
        // Flutter calls into native Path logic here
        // native 'Path_relativeArcToPoint';
    }

    /// Adds a new subpath that consists of four lines that outline the
    /// given rectangle.
    fun addRect(rect: Rect) {
        assert(_rectIsValid(rect));
        rectF.set(rect.toFrameworkRect())
        // TODO(Migration/njawad) figure out what to do with Path Direction, Flutter does not use it, Platform does
        internalPath.addRect(rectF, android.graphics.Path.Direction.CCW);
    }

    // Not necessary as wrapping platform Path
    fun _addRect(left: Double, top: Double, right: Double, bottom: Double) {
        TODO()
        // Flutter calls into native Path logic here
        // native 'Path_addRect';
    }

    /// Adds a new subpath that consists of a curve that forms the
    /// ellipse that fills the given rectangle.
    ///
    /// To add a circle, pass an appropriate rectangle as `oval`. [Rect.fromCircle]
    /// can be used to easily describe the circle's center [Offset] and radius.
    fun addOval(oval: Rect) {
        rectF.set(oval.toFrameworkRect());
        // TODO(Migration/njawad: figure out what to do with Path Direction, Flutter does not use it, Platform does)
        internalPath.addOval(rectF, android.graphics.Path.Direction.CCW);
        // _addOval(oval.left, oval.top, oval.right, oval.bottom);
    }

    // Not necessary as wrapping platform Path
    private fun _addOval(left: Double, top: Double, right: Double, bottom: Double) {
        TODO()
        // Flutter calls into native Path logic here
        // native 'Path_addOval';
    }

    /// Adds a new subpath with one arc segment that consists of the arc
    /// that follows the edge of the oval bounded by the given
    /// rectangle, from startAngle radians around the oval up to
    /// startAngle + sweepAngle radians around the oval, with zero
    /// radians being the point on the right hand side of the oval that
    /// crosses the horizontal line that intersects the center of the
    /// rectangle and with positive angles going clockwise around the
    /// oval.
    fun addArc(oval: Rect, startAngle: Double, sweepAngle: Double) {
        assert(_rectIsValid(oval));
        rectF.set(oval.toFrameworkRect());
        internalPath.addArc(rectF, startAngle.toFloat(), sweepAngle.toFloat());
        // _addArc(oval.left, oval.top, oval.right, oval.bottom, startAngle, sweepAngle);
    }

    // Not necessary as wrapping platform Path
    private fun _addArc(left: Double, top: Double, right: Double, bottom: Double) {
        TODO()
        // Flutter calls into native Path logic here
        // native 'Path_addArc'
    }

    /// Adds a new subpath with a sequence of line segments that connect the given
    /// points.
    ///
    /// If `close` is true, a final line segment will be added that connects the
    /// last point to the first point.
    ///
    /// The `points` argument is interpreted as offsets from the origin.
    fun addPolygon(points: List<Offset>, close: Boolean) {
        assert(points != null);
        // TODO(Migration/njawad) implement with sequence of "lineTo" calls
        TODO()
    }

    private fun _addPolygon(points: FloatArray, close: Boolean) {
        // TODO(Migration/njawad: implement with sequence of "lineTo" calls)
        TODO()
        // Flutter calls into native code here
        // native 'Path_addPolygon'
    }

    fun addRRect(rrect: RRect) {
        rectF.set(rrect.left.toFloat(), rrect.top.toFloat(), rrect.right.toFloat(),
                rrect.bottom.toFloat());
        radii[0] = rrect.tlRadiusX.toFloat();
        radii[1] = rrect.tlRadiusY.toFloat();

        radii[2] = rrect.trRadiusX.toFloat();
        radii[3] = rrect.trRadiusY.toFloat();

        radii[4] = rrect.brRadiusX.toFloat();
        radii[5] = rrect.brRadiusY.toFloat();

        radii[6] = rrect.blRadiusX.toFloat();
        radii[7] = rrect.blRadiusY.toFloat();
        internalPath.addRoundRect(rectF, radii, android.graphics.Path.Direction.CCW);
    }

    // Not necessary as wrapping platform Path
    private fun _addRRect(rrect: FloatArray) {
        TODO()
        // Flutter calls into native Path logic here
        // native 'Path_addRRect';
    }

    /// Adds a new subpath that consists of the given `path` offset by the given
    /// `offset`.
    ///
    /// If `matrix4` is specified, the path will be transformed by this matrix
    /// after the matrix is translated by the given offset. The matrix is a 4x4
    /// matrix stored in column major order.
    fun addPath(path: Path, offset: Offset, matrix: Matrix4? = null) {
        if (matrix != null) {
            //TODO(Migration/njawad: update logic to convert Matrix4 -> framework Matrix when ported)
            TODO("Refactor to convert Matrix4 to framework Matrix when Matrix4 is ported")
            //internalPath.addPath(path.getFrameworkPath(), matrix);
        } else {
            internalPath.addPath(path.getFrameworkPath(), offset.dx.toFloat(), offset.dy.toFloat());
        }
    }

    // Not necessary as wrapping platform Path
    private fun _addPath(path: Path, dx: Double, dy: Double) {
        TODO()
        // Flutter calls into native Path logic here
        // native 'Path_addPath';
    }

    // Not necessary as wrapping platform Path
    private fun _addPathWithMatrix(path: Path, dx: Double, dy: Double, matrix4: Matrix4) {
        TODO()
        // Flutter calls into native Path logic here
        // native 'Path_addPathWithMatrix';
    }

    fun extendWithPath(path: Path, offset: Offset, matrix: Matrix4) {
        assert(path != null); // path is checked on the engine side
        assert(_offsetIsValid(offset));
        if (matrix != null) {
            assert(_matrixIsValid(matrix));
            _extendWithPathAndMatrix(path, offset.dx, offset.dy, matrix);
        } else {
            _extendWithPath(path, offset.dx, offset.dy);
        }
    }

    private fun _extendWithPath(path: Path, dx: Double, dy: Double) {
        // TODO(Migration/njawad: figure out how to handle unsupported framework Path operations)
        TODO()
        // Flutter calls into native Path logic here
        // native 'Path_extendWithPath';
    }

    private fun _extendWithPathAndMatrix(path: Path, dx: Double, dy: Double, matrix: Matrix4) {
        // TODO(Migration/njawad: figure out how to handle unsupported framework Path operations)
        TODO()
        // Flutter calls into native Path logic here
        // native 'Path_extendWithPathAndMatrix';
    }

    /// Closes the last subpath, as if a straight line had been drawn
    /// from the current point to the first point of the subpath.
    fun close() {
        internalPath.close();
    }

    /// Clears the [Path] object of all subpaths, returning it to the
    /// same state it had when it was created. The _current point_ is
    /// reset to the origin.*/
    fun reset() {
        internalPath.reset();
    }

    /// Tests to see if the given point is within the path. (That is, whether the
    /// point would be in the visible portion of the path if the path was used
    /// with [Canvas.clipPath].)
    ///
    /// The `point` argument is interpreted as an offset from the origin.
    ///
    /// Returns true if the point is in the path, and false otherwise.
    fun contains(offset: Offset) {
        assert(_offsetIsValid(offset));
        _contains(offset);
        // TODO(Migration/njawad) framework Path implementation does not have a contains method
    }

    private fun _contains(offset: Offset) {
        // TODO(Migration/njawad: figure out how to handle unsupported framework Path operations)
        TODO()
        // Flutter calls into native code here
        // native 'Path_contains';
    }

    /// Returns a copy of the path with all the segments of every
    /// subpath translated by the given offset.
    fun shift(offset: Offset): androidx.ui.painting.Path {
        mMatrix.reset();
        mMatrix.setTranslate(offset.dx.toFloat(), offset.dy.toFloat());
        internalPath.transform(mMatrix);
        return this;
    }

    // Not necessary as ported implementation in public shift method above
    private fun _shift(dx: Double, dy: Double): androidx.ui.painting.Path {
        TODO()
        // Flutter calls into native code here
        // native 'Path_shift';
        return this;
    }

    /// Returns a copy of the path with all the segments of every
    /// subpath transformed by the given matrix.
    fun transform(matrix: Matrix4): androidx.ui.painting.Path {
        //TODO(Migration/njawad: Update implementation with Matrix4 -> android.graphics.Matrix)
        TODO("Update implementation with Matrix4 -> android.graphics.Matrix conversion")
        //internalPath.transform(matrix);
        return this;
    }

    // Not necessary as ported implementation with public transform method
    private fun _transform(matrix: Matrix4) {
        TODO()
        // Flutter calls into native code here
        // native 'Path_transform';
    }

    /// Computes the bounding rectangle for this path.
    ///
    /// A path containing only axis-aligned points on the same straight line will
    /// have no area, and therefore `Rect.isEmpty` will return true for such a
    /// path. Consider checking `rect.width + rect.height > 0.0` instead, or
    /// using the [computeMetrics] API to check the path length.
    ///
    /// For many more elaborate paths, the bounds may be inaccurate.  For example,
    /// when a path contains a circle, the points used to compute the bounds are
    /// the circle's implied control points, which form a square around the circle;
    /// if the circle has a transformation applied using [transform] then that
    /// square is rotated, and the (axis-aligned, non-rotated) bounding box
    /// therefore ends up grossly overestimating the actual area covered by the
    /// circle.
    // see https://skia.org/user/api/SkPath_Reference#SkPath_getBounds
    fun getBounds(): Rect {
        internalPath.computeBounds(rectF, true);
        return Rect(rectF.left.toDouble(),
                rectF.top.toDouble(),
                rectF.right.toDouble(),
                rectF.bottom.toDouble());
    }

    // Not necessary as implemented with framework Path#computeBounds method
    private fun _getBounds(): Rect {
        TODO()
        // Flutter calls into native code here
        // native 'Path_getBounds';
        return Rect(0.0, 0.0, 0.0, 0.0);
    }

    companion object {
        /// Combines the two paths according to the manner specified by the given
        /// `operation`.
        ///
        /// The resulting path will be constructed from non-overlapping contours. The
        /// curve order is reduced where possible so that cubics may be turned into
        /// quadratics, and quadratics maybe turned into lines.
        @RequiresApi(Build.VERSION_CODES.KITKAT)
        fun combine(operation: PathOperation, path1: androidx.ui.painting.Path,
                path2: androidx.ui.painting.Path): androidx.ui.painting.Path {
            val path = androidx.ui.painting.Path();

            if (path.op(path1, path2, operation)) {
                return path;
            }
            throw IllegalArgumentException("Path.combine() failed.  This may be due an invalid path; in particular, check for NaN values.")
            // TODO(Migration/njawad) where do we put Dart's StateError or equivalent?
            //throw StateError('Path.combine() failed.  This may be due an invalid path; in particular, check for NaN values.');
        }
    }

    // Wrapper around _op method to avoid synthetic accessor in companion combine method
    @RequiresApi(Build.VERSION_CODES.KITKAT)
    fun op(path1: androidx.ui.painting.Path, path2: androidx.ui.painting.Path,
            operation: PathOperation): Boolean {
        return _op(path1, path2, operation);
    }

    @RequiresApi(Build.VERSION_CODES.KITKAT)
    private fun _op(path1: androidx.ui.painting.Path,
            path2: androidx.ui.painting.Path, operation: PathOperation) : Boolean {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.KITKAT) {
            val op : android.graphics.Path.Op;
            when(operation) {
                PathOperation.difference -> op = android.graphics.Path.Op.DIFFERENCE;
                PathOperation.intersect -> op = android.graphics.Path.Op.INTERSECT;
                PathOperation.reverseDifference -> op = android.graphics.Path.Op.REVERSE_DIFFERENCE;
                PathOperation.union -> op = android.graphics.Path.Op.UNION;
                else -> op = android.graphics.Path.Op.XOR
            }
            return path1.internalPath.op(path1.getFrameworkPath(), path2.getFrameworkPath(), op);
        } else {
            return false;
        }
    }


    // TODO(Migration/njawad) figure out equivalent for PathMetrics for the framework based in Path
//
//    /// Creates a [PathMetrics] object for this path.
//    ///
//    /// If `forceClosed` is set to true, the contours of the path will be measured
//    /// as if they had been closed, even if they were not explicitly closed.
//    PathMetrics computeMetrics({bool forceClosed: false}) {
//        return new PathMetrics._(this, forceClosed);
//    }

    private fun _offsetIsValid(offset: Offset): Boolean {
        assert(offset != null) {
            "Offset argument was null."
        }
        assert(Double.NaN != offset.dx && Double.NaN != offset.dy) {
            "Offset argument contained a NaN value."
        };
        return true;
    }

    private fun _rectIsValid(rect: Rect): Boolean {
        assert(rect != null) {
            "Rect argument was null.";
        }
        assert(Double.NaN != rect.left) {
            "Rect.left is NaN"
        }
        assert(Double.NaN != rect.top) {
            "Rect.top is NaN"
        }
        assert(Double.NaN != rect.right) {
            "Rect.right is NaN"
        }
        assert(Double.NaN != rect.bottom) {
            "Rect.bottom is NaN"
        }
        return true;
    }

    private fun _matrixIsValid(matrix: Matrix4): Boolean {
        assert(matrix != null) {
            "Matrix argument was null."
        }
        return true;
    }
}