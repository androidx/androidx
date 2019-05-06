/*
 * Copyright 2019 The Android Open Source Project
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

package androidx.ui.core.vectorgraphics

import android.util.Log
import androidx.ui.painting.Path

private const val LOGTAG = "PathParser"

private const val NUM_MOVE_TO_ARGS = 2
private const val NUM_LINE_TO_ARGS = 2
private const val NUM_HORIZONTAL_TO_ARGS = 1
private const val NUM_VERTICAL_TO_ARGS = 1
private const val NUM_CURVE_TO_ARGS = 6
private const val NUM_REFLECTIVE_CURVE_TO_ARGS = 4
private const val NUM_QUAD_TO_ARGS = 4
private const val NUM_REFLECTIVE_QUAD_TO_ARGS = 2
private const val NUM_ARC_TO_ARGS = 7

class PathParser {

    private data class PathPoint(var x: Float = 0.0f, var y: Float = 0.0f) {
        fun reset() {
            x = 0.0f
            y = 0.0f
        }
    }

    private val nodes = mutableListOf<PathNode>()

    fun clear() {
        nodes.clear()
    }

    private val currentPoint = PathPoint()
    private val ctrlPoint = PathPoint()
    private val segmentPoint = PathPoint()
    private val reflectiveCtrlPoint = PathPoint()

    @Throws(java.lang.IllegalArgumentException::class, NumberFormatException::class)
    fun parsePathString(pathData: String): PathParser {
        nodes.clear()

        var start = 0
        var end = 1
        while (end < pathData.length) {
            end = nextStart(pathData, end)
            val s = pathData.substring(start, end).trim({ it <= ' ' })
            if (s.length > 0) {
                val args = getFloats(s)
                addNode(s.get(0), args)
            }

            start = end
            end++
        }
        if (end - start == 1 && start < pathData.length) {
            addNode(pathData.get(start), FloatArray(0))
        }

        return this
    }

    fun addPathNodes(nodes: Array<PathNode>): PathParser {
        this.nodes.addAll(nodes)
        return this
    }

    fun toNodes(): Array<PathNode> = nodes.toTypedArray()

    fun toPath(target: Path = Path()): Path {
        target.reset()
        currentPoint.reset()
        ctrlPoint.reset()
        segmentPoint.reset()
        reflectiveCtrlPoint.reset()

        var previousCmd = PathCommand.RelativeMoveTo
        for (node in nodes) {
            val currentCmd = node.command
            val args = node.args
            when (currentCmd) {
                // Both absolute and relative close operations invoke the same close method
                PathCommand.RelativeClose -> close(target)
                PathCommand.Close -> close(target)
                PathCommand.RelativeMoveTo -> relativeMoveTo(target, args)
                PathCommand.MoveTo -> moveTo(target, args)
                PathCommand.RelativeLineTo -> relativeLineTo(target, args)
                PathCommand.LineTo -> lineTo(target, args)
                PathCommand.RelativeHorizontalTo -> relativeHorizontalTo(target, args)
                PathCommand.HorizontalLineTo -> horizontalTo(target, args)
                PathCommand.RelativeVerticalTo -> relativeVerticalTo(target, args)
                PathCommand.VerticalLineTo -> verticalTo(target, args)
                PathCommand.RelativeCurveTo -> relativeCurveTo(target, args)
                PathCommand.CurveTo -> curveTo(target, args)
                PathCommand.RelativeReflectiveCurveTo ->
                    relativeReflectiveCurveTo(previousCmd, target, args)
                PathCommand.ReflectiveCurveTo -> reflectiveCurveTo(previousCmd, target, args)
                PathCommand.RelativeQuadTo -> relativeQuadTo(target, args)
                PathCommand.QuadTo -> quadTo(target, args)
                PathCommand.RelativeReflectiveQuadTo ->
                    relativeReflectiveQuadTo(previousCmd, target, args)
                PathCommand.ReflectiveQuadTo -> reflectiveQuadTo(previousCmd, target, args)
                PathCommand.RelativeArcTo -> relativeArcTo(target, args)
                PathCommand.ArcTo -> arcTo(target, args)
            }
            previousCmd = currentCmd
        }
        return target
    }

    private fun arcTo(target: Path, args: FloatArray) {
        forEachPathArg(args, NUM_ARC_TO_ARGS) { index ->
            val horizontalEllipseRadius = args[index]
            val verticalEllipseRadius = args[index + 1]
            val theta = args[index + 2]
            val isMoreThanHalf = args[index + 3].compareTo(0.0f) != 0
            val isPositiveArc = args[index + 4].compareTo(0.0f) != 0
            val arcStartX = args[index + 5]
            val arcStartY = args[index + 6]

            drawArc(target,
                currentPoint.x.toDouble(),
                currentPoint.y.toDouble(),
                arcStartX.toDouble(),
                arcStartY.toDouble(),
                horizontalEllipseRadius.toDouble(),
                verticalEllipseRadius.toDouble(),
                theta.toDouble(),
                isMoreThanHalf,
                isPositiveArc)

            currentPoint.x = arcStartX
            currentPoint.y = arcStartY

            ctrlPoint.x = currentPoint.x
            ctrlPoint.y = currentPoint.y
        }
    }

    private fun relativeArcTo(target: Path, args: FloatArray) {
        forEachPathArg(args, NUM_ARC_TO_ARGS) { index ->
            val horizontalEllipseRadius = args[index]
            val verticalEllipseRadius = args[index + 1]
            val theta = args[index + 2]
            val isMoreThanHalf = args[index + 3].compareTo(0.0f) != 0
            val isPositiveArc = args[index + 4].compareTo(0.0f) != 0
            val arcStartX = args[index + 5] + currentPoint.x
            val arcStartY = args[index + 6] + currentPoint.y

            drawArc(
                target,
                currentPoint.x.toDouble(),
                currentPoint.y.toDouble(),
                arcStartX.toDouble(),
                arcStartY.toDouble(),
                horizontalEllipseRadius.toDouble(),
                verticalEllipseRadius.toDouble(),
                theta.toDouble(),
                isMoreThanHalf,
                isPositiveArc
            )
            currentPoint.x = arcStartX
            currentPoint.y = arcStartY

            ctrlPoint.x = currentPoint.x
            ctrlPoint.y = currentPoint.y
        }
    }

    private fun drawArc(
        p: Path,
        x0: Double,
        y0: Double,
        x1: Double,
        y1: Double,
        a: Double,
        b: Double,
        theta: Double,
        isMoreThanHalf: Boolean,
        isPositiveArc: Boolean
    ) {

        /* Convert rotation angle from degrees to radians */
        val thetaD = Math.toRadians(theta)
        /* Pre-compute rotation matrix entries */
        val cosTheta = Math.cos(thetaD)
        val sinTheta = Math.sin(thetaD)
        /* Transform (x0, y0) and (x1, y1) into unit space */
        /* using (inverse) rotation, followed by (inverse) scale */
        val x0p = (x0 * cosTheta + y0 * sinTheta) / a
        val y0p = (-x0 * sinTheta + y0 * cosTheta) / b
        val x1p = (x1 * cosTheta + y1 * sinTheta) / a
        val y1p = (-x1 * sinTheta + y1 * cosTheta) / b

        /* Compute differences and averages */
        val dx = x0p - x1p
        val dy = y0p - y1p
        val xm = (x0p + x1p) / 2
        val ym = (y0p + y1p) / 2
        /* Solve for intersecting unit circles */
        val dsq = dx * dx + dy * dy
        if (dsq == 0.0) {
            Log.w(LOGTAG, " Points are coincident")
            return /* Points are coincident */
        }
        val disc = 1.0 / dsq - 1.0 / 4.0
        if (disc < 0.0) {
            Log.w(LOGTAG, "Points are too far apart $dsq")
            val adjust = (Math.sqrt(dsq) / 1.99999).toFloat()
            drawArc(p, x0, y0, x1, y1, a * adjust,
                    b * adjust, theta, isMoreThanHalf, isPositiveArc)
            return /* Points are too far apart */
        }
        val s = Math.sqrt(disc)
        val sdx = s * dx
        val sdy = s * dy
        var cx: Double
        var cy: Double
        if (isMoreThanHalf == isPositiveArc) {
            cx = xm - sdy
            cy = ym + sdx
        } else {
            cx = xm + sdy
            cy = ym - sdx
        }

        val eta0 = Math.atan2(y0p - cy, x0p - cx)

        val eta1 = Math.atan2(y1p - cy, x1p - cx)

        var sweep = eta1 - eta0
        if (isPositiveArc != (sweep >= 0)) {
            if (sweep > 0) {
                sweep -= 2 * Math.PI
            } else {
                sweep += 2 * Math.PI
            }
        }

        cx *= a
        cy *= b
        val tcx = cx
        cx = cx * cosTheta - cy * sinTheta
        cy = tcx * sinTheta + cy * cosTheta

        arcToBezier(p, cx, cy, a, b, x0, y0, thetaD,
            eta0, sweep)
    }

    /**
     * Converts an arc to cubic Bezier segments and records them in p.
     *
     * @param p The target for the cubic Bezier segments
     * @param cx The x coordinate center of the ellipse
     * @param cy The y coordinate center of the ellipse
     * @param a The radius of the ellipse in the horizontal direction
     * @param b The radius of the ellipse in the vertical direction
     * @param e1x E(eta1) x coordinate of the starting point of the arc
     * @param e1y E(eta2) y coordinate of the starting point of the arc
     * @param theta The angle that the ellipse bounding rectangle makes with horizontal plane
     * @param start The start angle of the arc on the ellipse
     * @param sweep The angle (positive or negative) of the sweep of the arc on the ellipse
     */
    private fun arcToBezier(
        p: Path,
        cx: Double,
        cy: Double,
        a: Double,
        b: Double,
        e1x: Double,
        e1y: Double,
        theta: Double,
        start: Double,
        sweep: Double
    ) {
        var eta1x = e1x
        var eta1y = e1y
        // Taken from equations at: http://spaceroots.org/documents/ellipse/node8.html
        // and http://www.spaceroots.org/documents/ellipse/node22.html

        // Maximum of 45 degrees per cubic Bezier segment
        val numSegments = Math.ceil(Math.abs(sweep * 4 / Math.PI)).toInt()

        var eta1 = start
        val cosTheta = Math.cos(theta)
        val sinTheta = Math.sin(theta)
        val cosEta1 = Math.cos(eta1)
        val sinEta1 = Math.sin(eta1)
        var ep1x = (-a * cosTheta * sinEta1) - (b * sinTheta * cosEta1)
        var ep1y = (-a * sinTheta * sinEta1) + (b * cosTheta * cosEta1)

        val anglePerSegment = sweep / numSegments
        for (i in 0 until numSegments) {
            val eta2 = eta1 + anglePerSegment
            val sinEta2 = Math.sin(eta2)
            val cosEta2 = Math.cos(eta2)
            val e2x = cx + (a * cosTheta * cosEta2) - (b * sinTheta * sinEta2)
            val e2y = cy + (a * sinTheta * cosEta2) + (b * cosTheta * sinEta2)
            val ep2x = (-a * cosTheta * sinEta2) - (b * sinTheta * cosEta2)
            val ep2y = (-a * sinTheta * sinEta2) + (b * cosTheta * cosEta2)
            val tanDiff2 = Math.tan((eta2 - eta1) / 2)
            val alpha = Math.sin(eta2 - eta1) * (Math.sqrt(4 + 3.0 * tanDiff2 * tanDiff2) - 1) / 3
            val q1x = eta1x + alpha * ep1x
            val q1y = eta1y + alpha * ep1y
            val q2x = e2x - alpha * ep2x
            val q2y = e2y - alpha * ep2y

            // TODO (njawad) figure out if this is still necessary?
//            // Adding this no-op call to workaround a proguard related issue.
//            p.relativeLineTo(0.0, 0.0)

            p.cubicTo(q1x.toFloat(),
                    q1y.toFloat(),
                    q2x.toFloat(),
                    q2y.toFloat(),
                    e2x.toFloat(),
                    e2y.toFloat())
            eta1 = eta2
            eta1x = e2x
            eta1y = e2y
            ep1x = ep2x
            ep1y = ep2y
        }
    }

    private fun PathCommand.isQuad(): Boolean =
        when (this) {
            PathCommand.QuadTo,
            PathCommand.RelativeQuadTo,
            PathCommand.ReflectiveQuadTo,
            PathCommand.RelativeReflectiveQuadTo -> true
            else -> false
        }

    private fun reflectiveQuadTo(prevCmd: PathCommand, target: Path, args: FloatArray) {
        forEachPathArg(args, NUM_REFLECTIVE_QUAD_TO_ARGS) { index ->
            val x1 = args[index]
            val y1 = args[index + 1]
            if (prevCmd.isQuad()) {
                reflectiveCtrlPoint.x = 2 * currentPoint.x - ctrlPoint.x
                reflectiveCtrlPoint.y = 2 * currentPoint.y - ctrlPoint.y
            } else {
                reflectiveCtrlPoint.x = currentPoint.x
                reflectiveCtrlPoint.y = currentPoint.y
            }
            target.quadraticBezierTo(reflectiveCtrlPoint.x,
                reflectiveCtrlPoint.y, x1, y1)
            ctrlPoint.x = reflectiveCtrlPoint.x
            ctrlPoint.y = reflectiveCtrlPoint.y
            currentPoint.x = x1
            currentPoint.y = y1
        }
    }

    private fun relativeReflectiveQuadTo(prevCmd: PathCommand, target: Path, args: FloatArray) {
        forEachPathArg(args, NUM_REFLECTIVE_QUAD_TO_ARGS) { index ->
            val x1 = args[index]
            val y1 = args[index + 1]
            if (prevCmd.isQuad()) {
                reflectiveCtrlPoint.x = currentPoint.x - ctrlPoint.x
                reflectiveCtrlPoint.y = currentPoint.y - ctrlPoint.y
            } else {
                reflectiveCtrlPoint.reset()
            }

            target.relativeQuadraticBezierTo(reflectiveCtrlPoint.x,
                reflectiveCtrlPoint.y, x1, y1)
            ctrlPoint.x = currentPoint.x + reflectiveCtrlPoint.x
            ctrlPoint.y = currentPoint.y + reflectiveCtrlPoint.y
            currentPoint.x += x1
            currentPoint.y += y1
        }
    }

    private fun quadTo(target: Path, args: FloatArray) {
        forEachPathArg(args, NUM_QUAD_TO_ARGS) { index ->
            val x1 = args[index]
            val y1 = args[index + 1]
            val x2 = args[index + 2]
            val y2 = args[index + 3]
            target.quadraticBezierTo(x1, y1, x2, y2)
            ctrlPoint.x = x1
            ctrlPoint.y = y1
            currentPoint.x = x2
            currentPoint.y = y2
        }
    }

    private fun relativeQuadTo(target: Path, args: FloatArray) {
        forEachPathArg(args, NUM_QUAD_TO_ARGS) { index ->
            val x1 = args[index]
            val y1 = args[index + 1]
            val x2 = args[index + 2]
            val y2 = args[index + 3]
            target.relativeQuadraticBezierTo(x1, y1, x2, y2)
            ctrlPoint.x = currentPoint.x + x1
            ctrlPoint.y = currentPoint.y + y1
            currentPoint.x += x1
            currentPoint.y += y1
        }
    }

    private fun quadTo(target: Path, args: FloatArray, relative: Boolean = false) {
        for (i in 0..args.size step 4) {
            val x1 = args[0]
            val y1 = args[1]
            val x2 = args[2]
            val y2 = args[3]
            if (relative) {
                target.relativeQuadraticBezierTo(x1, y1,
                    x2, y2)
                ctrlPoint.x = currentPoint.x + x1
                ctrlPoint.y = currentPoint.y + y1
                currentPoint.x += x1
                currentPoint.y += y1
            } else {
                target.quadraticBezierTo(x1, y1,
                    x2, y2)
                ctrlPoint.x = x1
                ctrlPoint.y = y1
                currentPoint.x = x2
                currentPoint.y = y2
            }
        }
    }

    private fun PathCommand.isCurve(): Boolean = when (this) {
                PathCommand.CurveTo,
                PathCommand.RelativeCurveTo,
                PathCommand.ReflectiveCurveTo,
                PathCommand.RelativeReflectiveCurveTo -> true
                else -> false
            }

    private fun reflectiveCurveTo(prevCmd: PathCommand, target: Path, args: FloatArray) {
        forEachPathArg(args, NUM_REFLECTIVE_CURVE_TO_ARGS) { index ->
            val x1 = args[index]
            val y1 = args[index + 1]
            val x2 = args[index + 2]
            val y2 = args[index + 3]

            if (prevCmd.isCurve()) {
                reflectiveCtrlPoint.x = 2 * currentPoint.x - ctrlPoint.x
                reflectiveCtrlPoint.y = 2 * currentPoint.y - ctrlPoint.y
            } else {
                reflectiveCtrlPoint.x = currentPoint.x
                reflectiveCtrlPoint.y = currentPoint.y
            }

            target.cubicTo(reflectiveCtrlPoint.x, reflectiveCtrlPoint.y,
                x1, y1, x2, y2)
            ctrlPoint.x = x1
            ctrlPoint.y = y1
            currentPoint.x = x2
            currentPoint.y = y2
        }
    }

    private fun relativeReflectiveCurveTo(prevCmd: PathCommand, target: Path, args: FloatArray) {
        forEachPathArg(args, NUM_REFLECTIVE_CURVE_TO_ARGS) { index ->
            val x1 = args[index]
            val y1 = args[index + 1]
            val x2 = args[index + 2]
            val y2 = args[index + 3]
            if (prevCmd.isCurve()) {
                reflectiveCtrlPoint.x = currentPoint.x - ctrlPoint.x
                reflectiveCtrlPoint.y = currentPoint.y - ctrlPoint.y
            } else {
                reflectiveCtrlPoint.reset()
            }

            target.relativeCubicTo(
                reflectiveCtrlPoint.x, reflectiveCtrlPoint.y,
                x1, y1,
                x2, y2)
            ctrlPoint.x = currentPoint.x + x1
            ctrlPoint.y = currentPoint.y + y1
            currentPoint.x += x2
            currentPoint.y += y2
        }
    }

    private fun curveTo(target: Path, args: FloatArray) {
        forEachPathArg(args, NUM_CURVE_TO_ARGS) { index ->
            val x1 = args[index]
            val y1 = args[index + 1]
            val x2 = args[index + 2]
            val y2 = args[index + 3]
            val x3 = args[index + 4]
            val y3 = args[index + 5]
            target.cubicTo(x1, y1,
                x2, y2,
                x3, y3)
            ctrlPoint.x = x2
            ctrlPoint.y = y2
            currentPoint.x = x3
            currentPoint.y = y3
        }
    }

    private fun relativeCurveTo(target: Path, args: FloatArray) {
        forEachPathArg(args, NUM_CURVE_TO_ARGS) { index ->
            val dx1 = args[index]
            val dy1 = args[index + 1]
            val dx2 = args[index + 2]
            val dy2 = args[index + 3]
            val dx3 = args[index + 4]
            val dy3 = args[index + 5]
            target.relativeCubicTo(dx1, dy1,
                dx2, dy2,
                dx3, dy3)
            ctrlPoint.x = currentPoint.x + dx2
            ctrlPoint.y = currentPoint.y + dy2
            currentPoint.x += dx3
            currentPoint.y += dy3
        }
    }

    private fun verticalTo(target: Path, args: FloatArray) {
        forEachPathArg(args, NUM_VERTICAL_TO_ARGS) { index ->
            val y = args[index]
            target.lineTo(currentPoint.x, y)
            currentPoint.y = y
        }
    }

    private fun relativeVerticalTo(target: Path, args: FloatArray) {
        forEachPathArg(args, NUM_VERTICAL_TO_ARGS) { index ->
            val y = args[index]
            target.relativeLineTo(0.0f, y)
            currentPoint.y += y
        }
    }

    private fun horizontalTo(target: Path, args: FloatArray) {
        forEachPathArg(args, NUM_HORIZONTAL_TO_ARGS) { index ->
            val x = args[index]
            target.lineTo(x, currentPoint.y)
            currentPoint.x = x
        }
    }

    private fun relativeHorizontalTo(target: Path, args: FloatArray) {
        forEachPathArg(args, NUM_HORIZONTAL_TO_ARGS) { index ->
            val x = args[index]
            target.relativeLineTo(x, 0.0f)
            currentPoint.x += x
        }
    }

    private fun lineTo(target: Path, args: FloatArray) {
        forEachPathArg(args, NUM_LINE_TO_ARGS) { index ->
            val x = args[index]
            val y = args[index + 1]
            target.lineTo(x, y)
            currentPoint.x = x
            currentPoint.y = y
        }
    }

    private fun relativeLineTo(target: Path, args: FloatArray) {
        forEachPathArg(args, NUM_LINE_TO_ARGS) { index ->
            val x = args[index]
            val y = args[index + 1]
            target.relativeLineTo(x, y)
            currentPoint.x += x
            currentPoint.y += y
        }
    }

    private fun moveTo(target: Path, args: FloatArray) {
        forEachPathArg(args, NUM_MOVE_TO_ARGS) { index ->
            val x = args[index]
            val y = args[index + 1]
            currentPoint.x = x
            currentPoint.y = y
            if (index > 0) {
                // According to the spec, if a moveto is followed by multiple
                // pairs of coordinates, the subsequent pairs are treated as
                // implicit lineto commands.
                target.lineTo(x, y)
            } else {
                target.moveTo(x, y)
                segmentPoint.x = currentPoint.x
                segmentPoint.y = currentPoint.y
            }
        }
    }

    private fun relativeMoveTo(target: Path, args: FloatArray) {
        forEachPathArg(args, NUM_MOVE_TO_ARGS) { index ->
            val x = args[index]
            val y = args[index + 1]
            currentPoint.x += x
            currentPoint.y += y
            if (index > 0) {
                // According to the spec, if a moveto is followed by multiple
                // pairs of coordinates, the subsequent pairs are treated as
                // implicit lineto commands.
                target.relativeLineTo(x, y)
            } else {
                target.relativeMoveTo(x, y)
                segmentPoint.x = currentPoint.x
                segmentPoint.y = currentPoint.y
            }
        }
    }

    private inline fun forEachPathArg(
        args: FloatArray,
        numArgs: Int,
        op: (index: Int) -> Unit
    ) {
        for (i in 0..args.size - numArgs step numArgs) {
            op(i)
        }
    }

    private fun close(target: Path) {
        currentPoint.x = segmentPoint.x
        currentPoint.y = segmentPoint.y
        ctrlPoint.x = segmentPoint.x
        ctrlPoint.y = segmentPoint.y

        target.close()
        target.moveTo(currentPoint.x, currentPoint.y)
    }

    @Throws(IllegalArgumentException::class)
    private fun addNode(cmd: Char, args: FloatArray) {
        nodes.add(
            PathNode(cmd.toPathCommand(), args)
        )
    }

    private fun nextStart(s: String, end: Int): Int {
        var index = end
        var c: Char

        while (index < s.length) {
            c = s[index]
            // Note that 'e' or 'E' are not valid path commands, but could be
            // used for floating point numbers' scientific notation.
            // Therefore, when searching for next command, we should ignore 'e'
            // and 'E'.
            if (((c - 'A') * (c - 'Z') <= 0 || (c - 'a') * (c - 'z') <= 0) &&
                c != 'e' && c != 'E') {
                return index
            }
            index++
        }
        return index
    }

    @Throws(NumberFormatException::class)
    private fun getFloats(s: String): FloatArray {
        if (s[0] == 'z' || s[0] == 'Z') {
            return FloatArray(0)
        }
        val results = FloatArray(s.length)
        var count = 0
        var startPosition = 1
        var endPosition: Int

        val result = ExtractFloatResult()
        val totalLength = s.length

        // The startPosition should always be the first character of the
        // current number, and endPosition is the character after the current
        // number.
        while (startPosition < totalLength) {
            extract(s, startPosition, result)
            endPosition = result.endPosition

            if (startPosition < endPosition) {
                results[count++] = java.lang.Float.parseFloat(
                        s.substring(startPosition, endPosition))
            }

            if (result.endWithNegativeOrDot) {
                // Keep the '-' or '.' sign with next number.
                startPosition = endPosition
            } else {
                startPosition = endPosition + 1
            }
        }
        return copyOfRange(results, 0, count)
    }

    internal fun copyOfRange(original: FloatArray, start: Int, end: Int): FloatArray {
        if (start > end) {
            throw IllegalArgumentException()
        }
        val originalLength = original.size
        if (start < 0 || start > originalLength) {
            throw ArrayIndexOutOfBoundsException()
        }
        val resultLength = end - start
        val copyLength = Math.min(resultLength, originalLength - start)
        val result = FloatArray(resultLength)
        System.arraycopy(original, start, result, 0, copyLength)
        return result
    }

    private fun extract(s: String, start: Int, result: ExtractFloatResult) {
        // Now looking for ' ', ',', '.' or '-' from the start.
        var currentIndex = start
        var foundSeparator = false
        result.endWithNegativeOrDot = false
        var secondDot = false
        var isExponential = false
        while (currentIndex < s.length) {
            val isPrevExponential = isExponential
            isExponential = false
            val currentChar = s[currentIndex]
            when (currentChar) {
                ' ', ',' -> foundSeparator = true
                '-' ->
                    // The negative sign following a 'e' or 'E' is not a separator.
                    if (currentIndex != start && !isPrevExponential) {
                        foundSeparator = true
                        result.endWithNegativeOrDot = true
                    }
                '.' ->
                    if (!secondDot) {
                        secondDot = true
                    } else {
                        // This is the second dot, and it is considered as a separator.
                        foundSeparator = true
                        result.endWithNegativeOrDot = true
                    }
                'e', 'E' -> isExponential = true
            }
            if (foundSeparator) {
                break
            }
            currentIndex++
        }
        // When there is nothing found, then we put the end position to the end
        // of the string.
        result.endPosition = currentIndex
    }

    private data class ExtractFloatResult(
        // We need to return the position of the next separator and whether the
        // next float starts with a '-' or a '.'.
        var endPosition: Int = 0,
        var endWithNegativeOrDot: Boolean = false
    )
}