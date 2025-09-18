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
@file:RestrictTo(RestrictTo.Scope.LIBRARY_GROUP)

package androidx.compose.remote.foundation.icons

import android.graphics.Path
import androidx.annotation.RestrictTo
import androidx.compose.remote.creation.RemotePath
import androidx.compose.ui.graphics.BlendMode
import androidx.compose.ui.graphics.Paint
import androidx.compose.ui.graphics.PaintingStyle
import androidx.compose.ui.graphics.PathFillType
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.StrokeJoin
import androidx.compose.ui.graphics.vector.DefaultFillType
import androidx.compose.ui.graphics.vector.DefaultStrokeLineCap
import androidx.compose.ui.graphics.vector.DefaultStrokeLineJoin
import androidx.compose.ui.graphics.vector.DefaultStrokeLineMiter
import androidx.compose.ui.graphics.vector.DefaultStrokeLineWidth

/**
 * A base class for defining vector graphics that can be drawn in a remote compose. Subclasses
 * define the vector path by implementing the [buildPath] method.
 *
 * @param intrinsicWidth The intrinsic width of the vector graphic in pixels. Defaults to
 *   [DefaultIconSize].
 * @param intrinsicHeight The intrinsic height of the vector graphic in pixels. Defaults to
 *   [DefaultIconSize].
 * @param tintBlendMode The [BlendMode] to apply when drawing the tintColor. Defaults to
 *   [BlendMode.SrcIn].
 * @param autoMirror Determines if the vector asset should automatically be mirrored for
 *   right-to-left (RTL) locales. If true, the image will be flipped horizontally in an RTL context.
 * @param paintAlpha The alpha (opacity) to apply to the entire vector, from 0.0f (transparent) to
 *   1.0f (opaque). Defaults to 1.0f.
 * @param paintingStyle The style to use for drawing the path. Can be [PaintingStyle.Fill] to fill
 *   the path's area or [PaintingStyle.Stroke] to draw its outline.
 * @param strokeLineWidth The width of the stroke, used only when [paintingStyle] is
 *   [PaintingStyle.Stroke].
 * @param strokeLineCap The shape to draw at the beginning and end of stroked lines. Used only when
 *   [paintingStyle] is [PaintingStyle.Stroke].
 * @param strokeLineJoin The shape to draw at the corners of stroked lines. Used only when
 *   [paintingStyle] is [PaintingStyle.Stroke].
 * @param strokeLineMiter The miter limit for stroked lines, used to control how sharp a corner can
 *   be before it is beveled. Used only when [paintingStyle] is [PaintingStyle.Stroke] and
 *   [strokeLineJoin] is [StrokeJoin.Miter].
 */
@RestrictTo(RestrictTo.Scope.LIBRARY_GROUP)
public abstract class RemoteImageVector
internal constructor(
    internal val intrinsicWidth: Float = DefaultIconSize,
    internal val intrinsicHeight: Float = DefaultIconSize,
    internal val tintBlendMode: BlendMode = BlendMode.SrcIn,
    internal val autoMirror: Boolean = false,
    internal val paintAlpha: Float = 1.0f,
    internal val paintingStyle: PaintingStyle = PaintingStyle.Fill,
    internal val pathFillType: PathFillType = DefaultFillType,
    internal val strokeLineWidth: Float = DefaultStrokeLineWidth,
    internal val strokeLineCap: StrokeCap = DefaultStrokeLineCap,
    internal val strokeLineJoin: StrokeJoin = DefaultStrokeLineJoin,
    internal val strokeLineMiter: Float = DefaultStrokeLineMiter,
) {
    internal val path: RemotePath by lazy {
        // Reset state before building the path.
        lastWasCurve = false
        RemotePath().apply {
            path.fillType = pathFillType.asFrameworkPathFillType()
            buildPath()
        }
    }

    private var lastWasCurve = false
    private var lastCX2: Float = 0f
    private var lastCY2: Float = 0f

    public abstract fun RemotePath.buildPath()

    public fun RemotePath.curveTo(
        x1: Float,
        y1: Float,
        x2: Float,
        y2: Float,
        x3: Float,
        y3: Float,
    ) {
        this.cubicTo(x1, y1, x2, y2, x3, y3)
        lastCX2 = x2
        lastCY2 = y2
        lastWasCurve = true
    }

    public fun RemotePath.curveToRelative(
        x1: Float,
        y1: Float,
        x2: Float,
        y2: Float,
        x3: Float,
        y3: Float,
    ) {
        val startX = currentX
        val startY = currentY
        this.rCubicTo(x1, y1, x2, y2, x3, y3)
        lastCX2 = startX + x2
        lastCY2 = startY + y2
        lastWasCurve = true
    }

    public fun RemotePath.reflectiveCurveToRelative(
        dx1: Float,
        dy1: Float,
        dx2: Float,
        dy2: Float,
    ) {
        val startX = currentX
        val startY = currentY

        // The first control point is the reflection of the previous curve's second control point.
        // If there was no previous curve, the first control point is the current point (delta 0,0).
        val rdx1 = if (lastWasCurve) startX - lastCX2 else 0f
        val rdy1 = if (lastWasCurve) startY - lastCY2 else 0f

        this.rCubicTo(rdx1, rdy1, dx1, dy1, dx2, dy2)

        // Cache the second control point for the next reflective curve.
        lastCX2 = startX + dx1
        lastCY2 = startY + dy1
        lastWasCurve = true
    }

    public fun RemotePath.reflectiveCurveTo(x1: Float, y1: Float, x2: Float, y2: Float) {
        val startX = currentX
        val startY = currentY

        // The first control point is the reflection of the previous curve's second control point.
        // If there was no previous curve, the first control point is the current point.
        val c1x = if (lastWasCurve) 2 * startX - lastCX2 else startX
        val c1y = if (lastWasCurve) 2 * startY - lastCY2 else startY

        this.cubicTo(c1x, c1y, x1, y1, x2, y2)

        // Cache the second control point for the next reflective curve.
        lastCX2 = x1
        lastCY2 = y1
        lastWasCurve = true
    }

    public fun RemotePath.verticalLineToRelative(f: Float) {
        this.rLineTo(0f, f)
        lastWasCurve = false
    }

    public fun RemotePath.horizontalLineToRelative(f: Float) {
        this.rLineTo(f, 0f)
        lastWasCurve = false
    }

    public fun RemotePath.verticalLineTo(f: Float) {
        this.lineTo(currentX, f)
        lastWasCurve = false
    }

    public fun RemotePath.horizontalLineTo(f: Float) {
        this.lineTo(f, currentY)
        lastWasCurve = false
    }

    public fun RemotePath.lineToRelative(x: Float, y: Float) {
        this.rLineTo(x, y)
        lastWasCurve = false
    }

    internal fun paint() =
        Paint()
            .apply {
                this.alpha = paintAlpha
                this.style = paintingStyle
                this.strokeJoin = strokeLineJoin
                this.strokeCap = strokeLineCap
                this.strokeWidth = strokeLineWidth
                this.strokeMiterLimit = strokeLineMiter
                this.isAntiAlias = true
            }
            .asFrameworkPaint()

    private fun PathFillType.asFrameworkPathFillType(): Path.FillType =
        when (this) {
            PathFillType.EvenOdd -> Path.FillType.EVEN_ODD
            else -> Path.FillType.WINDING
        }
}

/** For image vectors that don't have an intrinsic size. */
internal const val DefaultIconSize = 24f
