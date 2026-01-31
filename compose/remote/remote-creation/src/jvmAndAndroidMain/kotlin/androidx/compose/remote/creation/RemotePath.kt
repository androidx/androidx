/*
 * Copyright 2025 The Android Open Source Project
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

package androidx.compose.remote.creation

import androidx.annotation.RestrictTo

@Suppress("KmpVisibilityMismatch") // actuals are internal
public expect class RectF

@Suppress("KmpVisibilityMismatch") // actuals are internal
public expect class Path

@Suppress("KmpVisibilityMismatch") // actuals are internal
public expect class MatrixTransform

internal expect val RectF.left: Float
internal expect val RectF.top: Float
internal expect val RectF.right: Float
internal expect val RectF.bottom: Float

@RestrictTo(RestrictTo.Scope.LIBRARY_GROUP)
public expect class RemotePath {
    public constructor(bufferSize: Int)

    public constructor()

    public constructor(pathData: String)

    public fun reset()

    public fun incReserve(extraPtCount: Int)

    public fun moveTo(x: Float, y: Float)

    public fun rMoveTo(dx: Float, dy: Float)

    public fun quadTo(x1: Float, y1: Float, x2: Float, y2: Float)

    public fun rQuadTo(dx1: Float, dy1: Float, dx2: Float, dy2: Float)

    public fun conicTo(x1: Float, y1: Float, x2: Float, y2: Float, weight: Float)

    public fun rConicTo(dx1: Float, dy1: Float, dx2: Float, dy2: Float, weight: Float)

    public fun lineTo(x: Float, y: Float)

    public fun rLineTo(dx: Float, dy: Float)

    public fun cubicTo(x1: Float, y1: Float, x2: Float, y2: Float, x3: Float, y3: Float)

    public fun rCubicTo(dx1: Float, dy1: Float, dx2: Float, dy2: Float, dx3: Float, dy3: Float)

    public fun close()

    public fun rewind()

    public fun isEmpty(): Boolean

    public fun createFloatArray(): FloatArray

    public val path: Path

    public fun transform(matrix: MatrixTransform)

    public fun addArc(oval: RectF, startAngle: Float, sweepAngle: Float)

    public fun arcTo(oval: RectF, startAngle: Float, sweepAngle: Float)

    public fun arcTo(oval: RectF, startAngle: Float, sweepAngle: Float, forceMoveTo: Boolean)

    public fun arcTo(
        left: Float,
        top: Float,
        right: Float,
        bottom: Float,
        startAngle: Float,
        sweepAngle: Float,
        forceMoveTo: Boolean,
    )

    public fun addArc(
        left: Float,
        top: Float,
        right: Float,
        bottom: Float,
        startAngle: Float,
        sweepAngle: Float,
        forceMoveTo: Boolean = false,
    )

    public companion object {
        public val MOVE: Int
        public val LINE: Int
        public val QUADRATIC: Int
        public val CONIC: Int
        public val CUBIC: Int
        public val CLOSE: Int
        public val DONE: Int
        public val MOVE_NAN: Float
        public val LINE_NAN: Float
        public val QUADRATIC_NAN: Float
        public val CONIC_NAN: Float
        public val CUBIC_NAN: Float
        public val CLOSE_NAN: Float
        public val DONE_NAN: Float

        public fun createCirclePath(
            rc: RemoteComposeWriter,
            x: Float,
            y: Float,
            rad: Float,
        ): RemotePath
    }
}
