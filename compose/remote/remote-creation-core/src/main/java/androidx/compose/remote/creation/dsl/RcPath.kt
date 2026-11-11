/*
 * Copyright 2026 The Android Open Source Project
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

@file:RestrictTo(RestrictTo.Scope.LIBRARY_GROUP)
@file:JvmName("RcPathKt")

package androidx.compose.remote.creation.dsl

import androidx.annotation.RestrictTo
import androidx.compose.remote.core.RemotePathBase
import androidx.compose.remote.creation.Rc
import androidx.compose.remote.creation.RemoteComposeWriter
import kotlin.math.abs

@RestrictTo(RestrictTo.Scope.LIBRARY_GROUP)
@JvmInline
public value class RcDynamicPath internal constructor(internal val id: Int)

/** Path types for polar and other generated paths. */
@RestrictTo(RestrictTo.Scope.LIBRARY_GROUP)
@JvmInline
public value class RcPathType internal constructor(internal val value: Int) {
    public companion object {
        public val Spline: RcPathType = RcPathType(Rc.PathExpression.SPLINE_PATH)
        public val Loop: RcPathType = RcPathType(Rc.PathExpression.LOOP_PATH)
        public val Monotonic: RcPathType = RcPathType(Rc.PathExpression.MONOTONIC_PATH)
        public val Linear: RcPathType = RcPathType(Rc.PathExpression.LINEAR_PATH)
        public val Polar: RcPathType = RcPathType(Rc.PathExpression.POLAR_PATH)
    }
}

/** Path operations for combining paths. */
@RestrictTo(RestrictTo.Scope.LIBRARY_GROUP)
@JvmInline
public value class RcPathCombineOp internal constructor(internal val value: Byte) {
    public companion object {
        public val Difference: RcPathCombineOp =
            RcPathCombineOp(RemoteComposeWriter.COMBINE_DIFFERENCE)
        public val Intersect: RcPathCombineOp =
            RcPathCombineOp(RemoteComposeWriter.COMBINE_INTERSECT)
        public val ReverseDifference: RcPathCombineOp =
            RcPathCombineOp(RemoteComposeWriter.COMBINE_REVERSE_DIFFERENCE)
        public val Union: RcPathCombineOp = RcPathCombineOp(RemoteComposeWriter.COMBINE_UNION)
        public val Xor: RcPathCombineOp = RcPathCombineOp(RemoteComposeWriter.COMBINE_XOR)
    }
}

@RestrictTo(RestrictTo.Scope.LIBRARY_GROUP)
@JvmInline
public value class RcPath internal constructor(internal val id: Int)

/** Path fill types for complex paths. */
@RestrictTo(RestrictTo.Scope.LIBRARY_GROUP)
@JvmInline
public value class RcPathFillType internal constructor(internal val value: Int) {
    public companion object {
        public val Winding: RcPathFillType = RcPathFillType(0)
        public val EvenOdd: RcPathFillType = RcPathFillType(1)
    }
}

/**
 * Builder for static or NaN-encoded path geometry registered via [RcScope.remotePathData].
 *
 * Supports imperative contour commands ([moveTo], [lineTo], [quadTo], [conicTo], [cubicTo],
 * [arcTo], [close]) as well as parsing Android Vector Drawable (AVD) `android:pathData` strings
 * (`M`/`m`, `L`/`l`, `H`/`h`, `V`/`v`, `C`/`c`, `S`/`s`, `Q`/`q`, `T`/`t`, `A`/`a`, `Z`/`z`).
 */
@RestrictTo(RestrictTo.Scope.LIBRARY_GROUP)
@RcDslMarker
public class RcPathData internal constructor(internal val base: RemotePathBase) {
    /** Creates an empty [RcPathData] builder with default initial capacity. */
    public constructor() : this(RemotePathBase())

    /** Creates an empty [RcPathData] builder with [bufferSize] float slots reserved. */
    public constructor(bufferSize: Int) : this(RemotePathBase(bufferSize))

    /**
     * Creates an [RcPathData] populated by parsing an Android Vector Drawable (AVD) [avdPathData]
     * string.
     */
    public constructor(avdPathData: String) : this(parseAvdPathDataToBase(avdPathData))

    /** Returns `true` if this path contains no contours or segments. */
    public val isEmpty: Boolean
        get() = base.isEmpty

    /** Current contour X coordinate. */
    public val currentX: Float
        get() = base.currentX

    /** Current contour Y coordinate. */
    public val currentY: Float
        get() = base.currentY

    /** Clears all segments from this path. */
    public fun reset() {
        base.reset()
    }

    /** Clears all segments from this path while retaining allocated capacity. */
    public fun rewind() {
        base.rewind()
    }

    /** Starts a new contour at `(x, y)`. */
    public fun moveTo(x: Float, y: Float) {
        base.moveTo(x, y)
    }

    /** Starts a new contour at `(currentX + dx, currentY + dy)`. */
    public fun rMoveTo(dx: Float, dy: Float) {
        base.rMoveTo(dx, dy)
    }

    /** Appends a line segment from the current point to `(x, y)`. */
    public fun lineTo(x: Float, y: Float) {
        base.lineTo(x, y)
    }

    /** Appends a line segment relative to the current point by `(dx, dy)`. */
    public fun rLineTo(dx: Float, dy: Float) {
        base.rLineTo(dx, dy)
    }

    /** Appends a quadratic Bézier curve with control point `(x1, y1)` ending at `(x2, y2)`. */
    public fun quadTo(x1: Float, y1: Float, x2: Float, y2: Float) {
        base.quadTo(x1, y1, x2, y2)
    }

    /** Appends a relative quadratic Bézier curve from the current point. */
    public fun rQuadTo(dx1: Float, dy1: Float, dx2: Float, dy2: Float) {
        base.rQuadTo(dx1, dy1, dx2, dy2)
    }

    /** Appends a weighted conic curve with control point `(x1, y1)` ending at `(x2, y2)`. */
    public fun conicTo(x1: Float, y1: Float, x2: Float, y2: Float, weight: Float) {
        base.conicTo(x1, y1, x2, y2, weight)
    }

    /** Appends a relative weighted conic curve from the current point. */
    public fun rConicTo(dx1: Float, dy1: Float, dx2: Float, dy2: Float, weight: Float) {
        base.rConicTo(dx1, dy1, dx2, dy2, weight)
    }

    /** Appends a cubic Bézier curve with control points `(x1, y1)`, `(x2, y2)` to `(x3, y3)`. */
    public fun cubicTo(x1: Float, y1: Float, x2: Float, y2: Float, x3: Float, y3: Float) {
        base.cubicTo(x1, y1, x2, y2, x3, y3)
    }

    /** Appends a relative cubic Bézier curve from the current point. */
    public fun rCubicTo(dx1: Float, dy1: Float, dx2: Float, dy2: Float, dx3: Float, dy3: Float) {
        base.rCubicTo(dx1, dy1, dx2, dy2, dx3, dy3)
    }

    /** Appends an oval arc bounded by `(left, top, right, bottom)` in degrees. */
    public fun arcTo(
        left: Float,
        top: Float,
        right: Float,
        bottom: Float,
        startAngle: Float,
        sweepAngle: Float,
        forceMoveTo: Boolean = false,
    ) {
        base.addArc(left, top, right, bottom, startAngle, sweepAngle, forceMoveTo)
    }

    /** Appends an oval arc bounded by `(left, top, right, bottom)` in degrees. */
    public fun addArc(
        left: Float,
        top: Float,
        right: Float,
        bottom: Float,
        startAngle: Float,
        sweepAngle: Float,
        forceMoveTo: Boolean = false,
    ) {
        base.addArc(left, top, right, bottom, startAngle, sweepAngle, forceMoveTo)
    }

    /** Appends an endpoint-parameterized elliptical arc from the current point to `(x, y)`. */
    public fun arcTo(
        rx: Float,
        ry: Float,
        angle: Float,
        largeArc: Boolean,
        sweep: Boolean,
        x: Float,
        y: Float,
    ) {
        base.arcTo(rx, ry, angle, largeArc, sweep, x, y)
    }

    /** Appends an endpoint-parameterized elliptical arc from `(x0, y0)` to `(x1, y1)`. */
    public fun arcTo(
        x0: Float,
        y0: Float,
        rx: Float,
        ry: Float,
        angle: Float,
        largeArc: Boolean,
        sweep: Boolean,
        x1: Float,
        y1: Float,
    ) {
        base.arcTo(x0, y0, rx, ry, angle, largeArc, sweep, x1, y1)
    }

    /** Closes the current contour. */
    public fun close() {
        base.close()
    }
}

/**
 * Parses an Android Vector Drawable (AVD) `android:pathData` string (`M`/`m`, `L`/`l`, `H`/`h`,
 * `V`/`v`, `C`/`c`, `S`/`s`, `Q`/`q`, `T`/`t`, `A`/`a`, `Z`/`z`) into an [RcPathData] builder.
 */
@RestrictTo(RestrictTo.Scope.LIBRARY_GROUP)
public fun parseAvdPathData(avdPathData: String): RcPathData =
    RcPathData(parseAvdPathDataToBase(avdPathData))

private val AVD_PATH_TOKEN_REGEX = Regex("([-+]?[0-9]*\\.?[0-9]+(?:[eE][-+]?[0-9]+)?)|([A-Za-z])")

private fun parseAvdPathDataToBase(pathData: String): RemotePathBase {
    val path = RemotePathBase()
    val tokens = AVD_PATH_TOKEN_REGEX.findAll(pathData).map { it.value }.toList()

    var currentX = 0f
    var currentY = 0f
    var startX = 0f
    var startY = 0f
    var lastControlX = 0f
    var lastControlY = 0f
    var lastCmdChar = ' '
    var i = 0
    var cmdChar = ' '

    while (i < tokens.size) {
        val firstChar = tokens[i][0]
        if (firstChar.isLetter() && firstChar != 'e' && firstChar != 'E') {
            cmdChar = firstChar
            i++
        }

        val isRelative = cmdChar.isLowerCase()
        val cmd = cmdChar.uppercaseChar()

        when (cmd) {
            'M' -> {
                val x = tokens[i++].toFloat().let { if (isRelative) it + currentX else it }
                val y = tokens[i++].toFloat().let { if (isRelative) it + currentY else it }
                path.moveTo(x, y)
                currentX = x
                currentY = y
                startX = x
                startY = y
                lastControlX = x
                lastControlY = y
                cmdChar = if (isRelative) 'l' else 'L'
            }
            'L' -> {
                val x = tokens[i++].toFloat().let { if (isRelative) it + currentX else it }
                val y = tokens[i++].toFloat().let { if (isRelative) it + currentY else it }
                path.lineTo(x, y)
                currentX = x
                currentY = y
                lastControlX = x
                lastControlY = y
            }
            'H' -> {
                val x = tokens[i++].toFloat().let { if (isRelative) it + currentX else it }
                path.lineTo(x, currentY)
                currentX = x
                lastControlX = x
                lastControlY = currentY
            }
            'V' -> {
                val y = tokens[i++].toFloat().let { if (isRelative) it + currentY else it }
                path.lineTo(currentX, y)
                currentY = y
                lastControlX = currentX
                lastControlY = y
            }
            'C' -> {
                val x1 = tokens[i++].toFloat().let { if (isRelative) it + currentX else it }
                val y1 = tokens[i++].toFloat().let { if (isRelative) it + currentY else it }
                val x2 = tokens[i++].toFloat().let { if (isRelative) it + currentX else it }
                val y2 = tokens[i++].toFloat().let { if (isRelative) it + currentY else it }
                val x = tokens[i++].toFloat().let { if (isRelative) it + currentX else it }
                val y = tokens[i++].toFloat().let { if (isRelative) it + currentY else it }
                path.cubicTo(x1, y1, x2, y2, x, y)
                lastControlX = x2
                lastControlY = y2
                currentX = x
                currentY = y
            }
            'S' -> {
                val x2 = tokens[i++].toFloat().let { if (isRelative) it + currentX else it }
                val y2 = tokens[i++].toFloat().let { if (isRelative) it + currentY else it }
                val x = tokens[i++].toFloat().let { if (isRelative) it + currentX else it }
                val y = tokens[i++].toFloat().let { if (isRelative) it + currentY else it }
                val x1 =
                    if (lastCmdChar == 'C' || lastCmdChar == 'S') 2 * currentX - lastControlX
                    else currentX
                val y1 =
                    if (lastCmdChar == 'C' || lastCmdChar == 'S') 2 * currentY - lastControlY
                    else currentY
                path.cubicTo(x1, y1, x2, y2, x, y)
                lastControlX = x2
                lastControlY = y2
                currentX = x
                currentY = y
            }
            'Q' -> {
                val x1 = tokens[i++].toFloat().let { if (isRelative) it + currentX else it }
                val y1 = tokens[i++].toFloat().let { if (isRelative) it + currentY else it }
                val x = tokens[i++].toFloat().let { if (isRelative) it + currentX else it }
                val y = tokens[i++].toFloat().let { if (isRelative) it + currentY else it }
                path.quadTo(x1, y1, x, y)
                lastControlX = x1
                lastControlY = y1
                currentX = x
                currentY = y
            }
            'T' -> {
                val x = tokens[i++].toFloat().let { if (isRelative) it + currentX else it }
                val y = tokens[i++].toFloat().let { if (isRelative) it + currentY else it }
                val x1 =
                    if (lastCmdChar == 'Q' || lastCmdChar == 'T') 2 * currentX - lastControlX
                    else currentX
                val y1 =
                    if (lastCmdChar == 'Q' || lastCmdChar == 'T') 2 * currentY - lastControlY
                    else currentY
                path.quadTo(x1, y1, x, y)
                lastControlX = x1
                lastControlY = y1
                currentX = x
                currentY = y
            }
            'A' -> {
                val rx = abs(tokens[i++].toFloat())
                val ry = abs(tokens[i++].toFloat())
                val angle = tokens[i++].toFloat()
                val largeArc = tokens[i++].toFloat() != 0f
                val sweep = tokens[i++].toFloat() != 0f
                val x = tokens[i++].toFloat().let { if (isRelative) it + currentX else it }
                val y = tokens[i++].toFloat().let { if (isRelative) it + currentY else it }
                if (rx == 0f || ry == 0f) {
                    path.lineTo(x, y)
                } else {
                    path.arcTo(currentX, currentY, rx, ry, angle, largeArc, sweep, x, y)
                }
                currentX = x
                currentY = y
                lastControlX = x
                lastControlY = y
            }
            'Z' -> {
                path.close()
                currentX = startX
                currentY = startY
                lastControlX = startX
                lastControlY = startY
            }
            else -> throw IllegalArgumentException("Unsupported AVD path command: $cmdChar")
        }
        lastCmdChar = cmd
    }
    return path
}

/** Adds an arc to the [RemotePathBase] using bounding box and angles. */
internal fun RemotePathBase.arcTo(
    left: Float,
    top: Float,
    right: Float,
    bottom: Float,
    startAngle: Float,
    sweepAngle: Float,
    forceMoveTo: Boolean = false,
) {
    this.addArc(left, top, right, bottom, startAngle, sweepAngle, forceMoveTo)
}

/** Adds an endpoint-parameterized elliptical arc to the [RemotePathBase]. */
internal fun RemotePathBase.arcTo(
    rx: Float,
    ry: Float,
    angle: Float,
    largeArc: Boolean,
    sweep: Boolean,
    x: Float,
    y: Float,
) {
    this.arcTo(rx, ry, angle, largeArc, sweep, x, y)
}
