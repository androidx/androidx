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

package androidx.compose.remote.creation.compose.layout

import android.graphics.Bitmap
import android.graphics.Paint
import android.graphics.Path
import android.graphics.Rect
import android.graphics.RectF
import androidx.annotation.RestrictTo
import androidx.compose.remote.core.RcPlatformServices.RcPathArrayCreator
import androidx.compose.remote.creation.RemotePath
import androidx.compose.remote.creation.compose.shapes.MorphTweenUtility
import androidx.compose.remote.creation.compose.state.RemoteBitmapFont
import androidx.compose.remote.creation.compose.state.RemoteFloat
import androidx.compose.remote.creation.compose.state.RemoteImageBitmap
import androidx.compose.remote.creation.compose.state.RemotePaint
import androidx.compose.remote.creation.compose.state.RemoteString
import androidx.compose.remote.creation.compose.state.asRemotePaint
import androidx.compose.remote.creation.compose.state.rf
import androidx.compose.ui.graphics.PathIterator
import androidx.compose.ui.graphics.PathSegment
import androidx.compose.ui.graphics.asComposePath
import androidx.compose.ui.graphics.asImageBitmap
import androidx.graphics.shapes.RoundedPolygon

/** Fills the canvas with the specified [drawColor]. */
@RestrictTo(RestrictTo.Scope.LIBRARY_GROUP)
public fun RemoteCanvas.drawColor(drawColor: Int) {
    drawRect(
        0f.rf,
        0f.rf,
        creationDisplayInfo.size.width.toInt().rf,
        creationDisplayInfo.size.height.toInt().rf,
        Paint().apply {
            color = drawColor
            style = Paint.Style.FILL
        },
    )
}

/** Draws a rectangle from ([left], [top]) to ([right], [bottom]) using the specified [paint]. */
@RestrictTo(RestrictTo.Scope.LIBRARY_GROUP)
public fun RemoteCanvas.drawRect(
    left: Float,
    top: Float,
    right: Float,
    bottom: Float,
    paint: Paint,
) {
    drawRect(left.rf, top.rf, right.rf, bottom.rf, paint)
}

/** Draws a rectangle from ([left], [top]) to ([right], [bottom]) using the specified [paint]. */
@RestrictTo(RestrictTo.Scope.LIBRARY_GROUP)
public fun RemoteCanvas.drawRect(
    left: RemoteFloat,
    top: RemoteFloat,
    right: RemoteFloat,
    bottom: RemoteFloat,
    paint: Paint,
) {
    drawRect(left, top, right, bottom, paint.asRemotePaint())
}

/** Draws a [rect] using the specified [paint]. */
@RestrictTo(RestrictTo.Scope.LIBRARY_GROUP)
public fun RemoteCanvas.drawRect(rect: Rect, paint: Paint) {
    drawRect(
        rect.left.toFloat(),
        rect.top.toFloat(),
        rect.right.toFloat(),
        rect.bottom.toFloat(),
        paint,
    )
}

/** Draws a [rect] using the specified [paint]. */
@RestrictTo(RestrictTo.Scope.LIBRARY_GROUP)
public fun RemoteCanvas.drawRect(rect: RectF, paint: Paint) {
    drawRect(rect.left, rect.top, rect.right, rect.bottom, paint)
}

/** Draws an oval from ([left], [top]) to ([right], [bottom]) using the specified [paint]. */
@RestrictTo(RestrictTo.Scope.LIBRARY_GROUP)
public fun RemoteCanvas.drawOval(
    left: Float,
    top: Float,
    right: Float,
    bottom: Float,
    paint: Paint,
) {
    drawOval(left.rf, top.rf, right.rf, bottom.rf, paint)
}

/** Draws an oval from ([left], [top]) to ([right], [bottom]) using the specified [paint]. */
@RestrictTo(RestrictTo.Scope.LIBRARY_GROUP)
public fun RemoteCanvas.drawOval(
    left: RemoteFloat,
    top: RemoteFloat,
    right: RemoteFloat,
    bottom: RemoteFloat,
    paint: Paint,
) {
    drawOval(left, top, right, bottom, paint.asRemotePaint())
}

/**
 * Draws a rounded rectangle from ([left], [top]) to ([right], [bottom]) with the specified [rx],
 * [ry], and [paint].
 */
@RestrictTo(RestrictTo.Scope.LIBRARY_GROUP)
public fun RemoteCanvas.drawRoundRect(
    left: Float,
    top: Float,
    right: Float,
    bottom: Float,
    rx: Float,
    ry: Float,
    paint: Paint,
) {
    drawRoundRect(left.rf, top.rf, right.rf, bottom.rf, rx.rf, ry.rf, paint)
}

/**
 * Draws a rounded rectangle from ([left], [top]) to ([right], [bottom]) with the specified [rx],
 * [ry], and [paint].
 */
@RestrictTo(RestrictTo.Scope.LIBRARY_GROUP)
public fun RemoteCanvas.drawRoundRect(
    left: RemoteFloat,
    top: RemoteFloat,
    right: RemoteFloat,
    bottom: RemoteFloat,
    rx: RemoteFloat,
    ry: RemoteFloat,
    paint: Paint,
) {
    drawRoundRect(left, top, right, bottom, rx, ry, paint.asRemotePaint())
}

/** Draws a line from ([startX], [startY]) to ([stopX], [stopY]) using the specified [paint]. */
@RestrictTo(RestrictTo.Scope.LIBRARY_GROUP)
public fun RemoteCanvas.drawLine(
    startX: Float,
    startY: Float,
    stopX: Float,
    stopY: Float,
    paint: Paint,
) {
    drawLine(startX.rf, startY.rf, stopX.rf, stopY.rf, paint)
}

/** Draws a line from ([startX], [startY]) to ([stopX], [stopY]) using the specified [paint]. */
@RestrictTo(RestrictTo.Scope.LIBRARY_GROUP)
public fun RemoteCanvas.drawLine(
    startX: RemoteFloat,
    startY: RemoteFloat,
    stopX: RemoteFloat,
    stopY: RemoteFloat,
    paint: Paint,
) {
    drawLine(startX, startY, stopX, stopY, paint.asRemotePaint())
}

/**
 * Draws an arc from ([left], [top]) to ([right], [bottom]) starting at [startAngle] and sweeping by
 * [sweepAngle] using the specified [paint].
 */
@RestrictTo(RestrictTo.Scope.LIBRARY_GROUP)
public fun RemoteCanvas.drawArc(
    left: Float,
    top: Float,
    right: Float,
    bottom: Float,
    startAngle: Float,
    sweepAngle: Float,
    useCenter: Boolean,
    paint: Paint,
) {
    drawArc(left.rf, top.rf, right.rf, bottom.rf, startAngle.rf, sweepAngle.rf, useCenter, paint)
}

/**
 * Draws an arc from ([left], [top]) to ([right], [bottom]) starting at [startAngle] and sweeping by
 * [sweepAngle] using the specified [paint].
 */
@RestrictTo(RestrictTo.Scope.LIBRARY_GROUP)
public fun RemoteCanvas.drawArc(
    left: RemoteFloat,
    top: RemoteFloat,
    right: RemoteFloat,
    bottom: RemoteFloat,
    startAngle: RemoteFloat,
    sweepAngle: RemoteFloat,
    useCenter: Boolean,
    paint: Paint,
) {
    drawArc(left, top, right, bottom, startAngle, sweepAngle, useCenter, paint.asRemotePaint())
}

/** Draws a circle at ([cx], [cy]) with the specified [radius] and [paint]. */
@RestrictTo(RestrictTo.Scope.LIBRARY_GROUP)
public fun RemoteCanvas.drawCircle(cx: Float, cy: Float, radius: Float, paint: Paint) {
    drawCircle(cx.rf, cy.rf, radius.rf, paint)
}

/** Draws a circle at ([cx], [cy]) with the specified [radius] and [paint]. */
@RestrictTo(RestrictTo.Scope.LIBRARY_GROUP)
public fun RemoteCanvas.drawCircle(
    cx: RemoteFloat,
    cy: RemoteFloat,
    radius: RemoteFloat,
    paint: Paint,
) {
    drawCircle(cx, cy, radius, paint.asRemotePaint())
}

/** Draws text from [text] at ([x], [y]) using the specified [paint]. */
@RestrictTo(RestrictTo.Scope.LIBRARY_GROUP)
public fun RemoteCanvas.drawText(text: String, x: Float, y: Float, paint: Paint) {
    drawTextRun(text, 0, text.length, 0, text.length, x, y, false, paint)
}

/** Draws text from [text] at ([x], [y]) using the specified [paint]. */
@RestrictTo(RestrictTo.Scope.LIBRARY_GROUP)
public fun RemoteCanvas.drawText(text: String, x: RemoteFloat, y: RemoteFloat, paint: Paint) {
    drawText(text, x, y, paint.asRemotePaint())
}

/**
 * Draws text from a [RemoteString] at the specified position.
 *
 * @param text The [RemoteString] to draw.
 * @param length The number of characters to draw from the [RemoteString].
 * @param x The X coordinate of the text's origin.
 * @param y The Y coordinate of the text's origin.
 * @param paint The [Paint] object used for styling the text.
 */
@RestrictTo(RestrictTo.Scope.LIBRARY_GROUP)
public fun RemoteCanvas.drawText(
    text: RemoteString,
    length: Int,
    x: RemoteFloat,
    y: RemoteFloat,
    paint: Paint,
) {
    drawTextRun(text, 0, length, 0, length, x, y, false, paint)
}

/** Draws a run of text from [text] at ([x], [y]) using the specified [paint]. */
@RestrictTo(RestrictTo.Scope.LIBRARY_GROUP)
public fun RemoteCanvas.drawTextRun(
    text: CharSequence,
    start: Int,
    end: Int,
    contextStart: Int,
    contextEnd: Int,
    x: Float,
    y: Float,
    isRtl: Boolean,
    paint: Paint,
) {
    drawTextRun(text, start, end, contextStart, contextEnd, x, y, isRtl, paint.asRemotePaint())
}

/**
 * Draws a run of text from a [RemoteString] at a specified position.
 *
 * @param text The [RemoteString] to draw.
 * @param start The index of the first character to draw.
 * @param end The index after the last character to draw.
 * @param contextStart The index of the first character of the context for glyph shaping.
 * @param contextEnd The index after the last character of the context for glyph shaping.
 * @param x The X coordinate of the text's origin.
 * @param y The Y coordinate of the text's origin.
 * @param isRtl `true` if the text is right-to-left.
 * @param paint The [Paint] object used for styling.
 */
@RestrictTo(RestrictTo.Scope.LIBRARY_GROUP)
public fun RemoteCanvas.drawTextRun(
    text: RemoteString,
    start: Int,
    end: Int,
    contextStart: Int,
    contextEnd: Int,
    x: RemoteFloat,
    y: RemoteFloat,
    isRtl: Boolean,
    paint: Paint,
) {
    drawTextRun(text, start, end, contextStart, contextEnd, x, y, isRtl, paint.asRemotePaint())
}

/** Draws text along a given [Path] using the specified [paint]. */
@RestrictTo(RestrictTo.Scope.LIBRARY_GROUP)
public fun RemoteCanvas.drawTextOnPath(
    text: String,
    path: Path,
    hOffset: Float,
    vOffset: Float,
    paint: Paint,
) {
    drawTextOnPath(text, path.asRemotePath(), hOffset.rf, vOffset.rf, paint.asRemotePaint())
}

/**
 * Draws text from a [RemoteString] along a given [Path].
 *
 * @param text The [RemoteString] to draw.
 * @param path The [Path] along which to draw the text.
 * @param hOffset The horizontal offset along the path.
 * @param vOffset The vertical offset from the path.
 * @param paint The [Paint] object for styling the text.
 */
@RestrictTo(RestrictTo.Scope.LIBRARY_GROUP)
public fun RemoteCanvas.drawTextOnPath(
    text: RemoteString,
    path: Path,
    hOffset: RemoteFloat,
    vOffset: RemoteFloat,
    paint: Paint,
) {
    drawTextOnPath(text, path.asRemotePath(), hOffset, vOffset, paint.asRemotePaint())
}

/**
 * Draws a substring of [text] with [bitmapFont] at position ([x], [y]).
 *
 * @param text The [RemoteString] to draw.
 * @param bitmapFont The [RemoteBitmapFont] to draw [text] with.
 * @param start The character to start drawing from.
 * @param end The character to stop drawing at. Note if this is -1 then all characters from [start]
 *   until the last character of [text] are drawn.
 * @param x The left x-coordinate to start rendering from.
 * @param y The top y-coordinate to start rendering from.
 * @param glyphSpacing Horizontal adjustment in pixels between glyphs.
 * @param paint The [Paint] to render with.
 */
@RestrictTo(RestrictTo.Scope.LIBRARY_GROUP)
public fun RemoteCanvas.drawBitmapFontTextRun(
    text: RemoteString,
    bitmapFont: RemoteBitmapFont,
    start: Int,
    end: Int,
    x: RemoteFloat,
    y: RemoteFloat,
    glyphSpacing: RemoteFloat,
    paint: Paint,
) {
    drawBitmapFontTextRun(text, bitmapFont, start, end, x, y, glyphSpacing, paint.asRemotePaint())
}

/**
 * Draws a substring of [text] with [bitmapFont] along [path].
 *
 * @param text The [RemoteString] to draw.
 * @param bitmapFont The [RemoteBitmapFont] to draw [text] with.
 * @param path The [Path] to draw along.
 * @param start The character to start drawing from.
 * @param end The character to stop drawing at. Note if this is -1 then all characters from [start]
 *   until the last character of [text] are drawn.
 * @param yAdj Adjustment away from the path along the normal at that point.
 * @param glyphSpacing Horizontal adjustment in pixels between glyphs.
 * @param paint The [Paint] to render with.
 */
@RestrictTo(RestrictTo.Scope.LIBRARY_GROUP)
public fun RemoteCanvas.drawBitmapFontTextRunOnPath(
    text: RemoteString,
    bitmapFont: RemoteBitmapFont,
    path: Path,
    start: Int,
    end: Int,
    yAdj: Float,
    glyphSpacing: Float,
    paint: Paint,
) {
    drawBitmapFontTextRunOnPath(
        text,
        bitmapFont,
        path.asRemotePath(),
        start,
        end,
        yAdj,
        glyphSpacing,
        paint.asRemotePaint(),
    )
}

/** Draws a [Path] using the specified [paint]. */
@RestrictTo(RestrictTo.Scope.LIBRARY_GROUP)
public fun RemoteCanvas.drawPath(path: Path, paint: Paint) {
    drawPath(path.asRemotePath(), paint = paint.asRemotePaint())
}

/** Draws a [Bitmap] at ([left], [top]) using the specified [paint]. */
@RestrictTo(RestrictTo.Scope.LIBRARY_GROUP)
public fun RemoteCanvas.drawBitmap(bitmap: Bitmap, left: Float, top: Float, paint: Paint?) {
    drawBitmap(
        RemoteImageBitmap(bitmap.asImageBitmap()),
        left.rf,
        top.rf,
        (left + bitmap.width).rf,
        (top + bitmap.height).rf,
        paint?.asRemotePaint(),
    )
}

/** Draws a [Bitmap] into [dst] using the specified [paint]. */
@RestrictTo(RestrictTo.Scope.LIBRARY_GROUP)
public fun RemoteCanvas.drawBitmap(
    bitmap: Bitmap,
    @Suppress("UNUSED_PARAMETER") src: Rect?,
    dst: Rect,
    paint: Paint?,
) {
    drawBitmap(
        RemoteImageBitmap(bitmap.asImageBitmap()),
        dst.left.rf,
        dst.top.rf,
        dst.right.rf,
        dst.bottom.rf,
        paint?.asRemotePaint(),
    )
}

/** Draws a [RemoteImageBitmap] into [dst] using the specified [paint]. */
@RestrictTo(RestrictTo.Scope.LIBRARY_GROUP)
public fun RemoteCanvas.drawBitmap(
    bitmap: RemoteImageBitmap,
    @Suppress("UNUSED_PARAMETER") src: Rect?,
    dst: Rect,
    paint: Paint?,
) {
    drawBitmap(
        bitmap,
        dst.left.rf,
        dst.top.rf,
        dst.right.rf,
        dst.bottom.rf,
        paint?.asRemotePaint(),
    )
}

/** Draws a [Bitmap] into [dst] using the specified [paint]. */
@RestrictTo(RestrictTo.Scope.LIBRARY_GROUP)
public fun RemoteCanvas.drawBitmap(
    bitmap: Bitmap,
    @Suppress("UNUSED_PARAMETER") src: Rect?,
    dst: RectF,
    paint: Paint?,
) {
    drawBitmap(
        RemoteImageBitmap(bitmap.asImageBitmap()),
        dst.left.rf,
        dst.top.rf,
        dst.right.rf,
        dst.bottom.rf,
        paint?.asRemotePaint(),
    )
}

/** Draws a [Bitmap] from ([left], [top]) to ([right], [bottom]) using the specified [paint]. */
@RestrictTo(RestrictTo.Scope.LIBRARY_GROUP)
public fun RemoteCanvas.drawBitmap(
    bitmap: Bitmap,
    left: RemoteFloat,
    top: RemoteFloat,
    right: RemoteFloat,
    bottom: RemoteFloat,
    paint: Paint?,
) {
    drawBitmap(
        RemoteImageBitmap(bitmap.asImageBitmap()),
        left,
        top,
        right,
        bottom,
        paint?.asRemotePaint(),
    )
}

/** Draws a [RoundedPolygon] using the specified [paint]. */
@RestrictTo(RestrictTo.Scope.LIBRARY_GROUP)
public fun RemoteCanvas.drawRoundedPolygon(roundedPolygon: RoundedPolygon, paint: RemotePaint?) {
    recordRenderingOp(paint) {
        val pathData = MorphTweenUtility.cubicsToPathData(roundedPolygon.cubics)
        val id =
            document.addPathData(
                object : RcPathArrayCreator {
                    override fun createFloatArray(): FloatArray = pathData
                }
            )
        document.buffer.addDrawPath(id)
    }
}

/** Draws a morph between two [RoundedPolygon]s using the specified [paint]. */
@RestrictTo(RestrictTo.Scope.LIBRARY_GROUP)
public fun RemoteCanvas.drawRoundedPolygonMorph(
    from: RoundedPolygon,
    to: RoundedPolygon,
    progress: RemoteFloat,
    paint: RemotePaint?,
) {
    val op =
        recordRenderingOp(paint) {
            MorphTweenUtility.emitMorphAsTweens(
                document,
                from,
                to,
                progress.getFloatIdForCreationState(creationState),
            )
        }
    buffer.addRoots(op, progress)
}

/** Converts an [android.graphics.Path] to a [RemotePath]. */
private fun Path.asRemotePath(): RemotePath {
    val remotePath = RemotePath()
    val pathIterator = asComposePath().iterator(PathIterator.ConicEvaluation.AsConic)
    val points = FloatArray(8)
    while (pathIterator.hasNext()) {
        val type = pathIterator.next(points)
        when (type) {
            PathSegment.Type.Move -> remotePath.moveTo(points[0], points[1])
            PathSegment.Type.Line -> remotePath.lineTo(points[2], points[3])
            PathSegment.Type.Quadratic ->
                remotePath.quadTo(points[2], points[3], points[4], points[5])
            PathSegment.Type.Conic ->
                remotePath.conicTo(points[2], points[3], points[4], points[5], points[6])
            PathSegment.Type.Cubic ->
                remotePath.cubicTo(
                    points[2],
                    points[3],
                    points[4],
                    points[5],
                    points[6],
                    points[7],
                )
            PathSegment.Type.Close -> remotePath.close()
            PathSegment.Type.Done -> break
        }
    }
    return remotePath
}
