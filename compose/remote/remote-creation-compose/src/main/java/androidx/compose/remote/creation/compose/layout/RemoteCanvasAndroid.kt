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
import androidx.compose.remote.creation.compose.capture.RemoteComposePath
import androidx.compose.remote.creation.compose.shapes.MorphTweenUtility
import androidx.compose.remote.creation.compose.state.RemoteBitmapFont
import androidx.compose.remote.creation.compose.state.RemoteFloat
import androidx.compose.remote.creation.compose.state.RemoteImageBitmap
import androidx.compose.remote.creation.compose.state.RemotePaint
import androidx.compose.remote.creation.compose.state.RemoteString
import androidx.compose.remote.creation.compose.state.asRemotePaint
import androidx.compose.remote.creation.compose.state.rf
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.graphics.asAndroidBitmap
import androidx.compose.ui.graphics.asAndroidPath
import androidx.compose.ui.graphics.asImageBitmap
import androidx.graphics.shapes.RoundedPolygon

/** Fills the canvas with the specified [drawColor]. */
@RestrictTo(RestrictTo.Scope.LIBRARY_GROUP)
public fun RemoteCanvas.drawColor(drawColor: Int) {
    drawRect(
        0f.rf,
        0f.rf,
        creationState.creationDisplayInfo.size.width.toInt().rf,
        creationState.creationDisplayInfo.size.height.toInt().rf,
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
    val pathSnapshot = Path(path)
    recordRenderingOp(paint.asRemotePaint()) {
        document.drawTextOnPath(text, pathSnapshot, hOffset, vOffset)
    }
}

/**
 * Draws text along a given [Path].
 *
 * @param text The text to draw.
 * @param path The [Path] along which to draw the text.
 * @param hOffset The horizontal offset along the path.
 * @param vOffset The vertical offset from the path.
 * @param paint The [Paint] object for styling the text.
 */
@RestrictTo(RestrictTo.Scope.LIBRARY_GROUP)
public fun RemoteCanvas.drawTextOnPath(
    text: String,
    path: Path,
    hOffset: RemoteFloat,
    vOffset: RemoteFloat,
    paint: Paint,
) {
    val pathSnapshot = Path(path)
    val op =
        recordRenderingOp(paint.asRemotePaint()) {
            document.drawTextOnPath(
                text,
                pathSnapshot,
                hOffset.getFloatIdForCreationState(creationState),
                vOffset.getFloatIdForCreationState(creationState),
            )
        }
    buffer.addRoots(op, hOffset, vOffset)
}

/**
 * Draws text along a given [RemotePath].
 *
 * @param text The text to draw.
 * @param path The [RemotePath] along which to draw the text.
 * @param hOffset The horizontal offset along the path.
 * @param vOffset The vertical offset from the path.
 * @param paint The [Paint] object for styling the text.
 */
@RestrictTo(RestrictTo.Scope.LIBRARY_GROUP)
public fun RemoteCanvas.drawTextOnPath(
    text: String,
    path: RemotePath,
    hOffset: RemoteFloat,
    vOffset: RemoteFloat,
    paint: Paint,
) {
    drawTextOnPath(text, path, hOffset, vOffset, paint.asRemotePaint())
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
    val pathSnapshot = Path(path)
    val op =
        recordRenderingOp(paint.asRemotePaint()) {
            document.drawTextOnPath(
                text.getIdForCreationState(creationState),
                pathSnapshot,
                hOffset.getFloatIdForCreationState(creationState),
                vOffset.getFloatIdForCreationState(creationState),
            )
        }
    buffer.addRoots(op, text, hOffset, vOffset)
}

/**
 * Draws text from a [RemoteString] along a given [RemotePath].
 *
 * @param text The [RemoteString] to draw.
 * @param path The [RemotePath] along which to draw the text.
 * @param hOffset The horizontal offset along the path.
 * @param vOffset The vertical offset from the path.
 * @param paint The [Paint] object for styling the text.
 */
@RestrictTo(RestrictTo.Scope.LIBRARY_GROUP)
public fun RemoteCanvas.drawTextOnPath(
    text: RemoteString,
    path: RemotePath,
    hOffset: RemoteFloat,
    vOffset: RemoteFloat,
    paint: Paint,
) {
    drawTextOnPath(text, path, hOffset, vOffset, paint.asRemotePaint())
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
    val pathSnapshot = Path(path)
    val op =
        recordRenderingOp(paint.asRemotePaint()) {
            document.drawBitmapFontTextRunOnPath(
                text.getIdForCreationState(creationState),
                bitmapFont.getIdForCreationState(creationState),
                pathSnapshot,
                start,
                end,
                yAdj,
                glyphSpacing,
            )
        }
    buffer.addRoots(op, text, bitmapFont)
}

/** Draws a [Path] using the specified [paint]. */
@RestrictTo(RestrictTo.Scope.LIBRARY_GROUP)
public fun RemoteCanvas.drawPath(path: Path, paint: Paint) {
    val pathSnapshot = Path(path)
    recordRenderingOp(paint.asRemotePaint()) { document.drawPath(pathSnapshot) }
}

/**
 * Draws a [RemotePath] onto the canvas using the specified [Paint].
 *
 * @param path The [RemotePath] to draw.
 * @param paint The [Paint] object to use for drawing the path.
 */
@RestrictTo(RestrictTo.Scope.LIBRARY_GROUP)
public fun RemoteCanvas.drawRPath(path: RemotePath, paint: Paint) {
    drawPath(path, paint = paint.asRemotePaint())
}

/**
 * Draws a path that is an interpolation (tween) between two Compose UI
 * [androidx.compose.ui.graphics.Path] objects.
 *
 * This allows for smooth animation between different path shapes. If the input paths are
 * [RemoteComposePath] instances, it optimizes by directly using their remote representations.
 * Otherwise, it converts them to Android `Path` objects.
 *
 * @param path1 The starting Compose UI [androidx.compose.ui.graphics.Path].
 * @param path2 The ending Compose UI [androidx.compose.ui.graphics.Path].
 * @param tween The interpolation factor (0.0 for `path1`, 1.0 for `path2`).
 * @param start The start value for internal tween calculations (often 0.0).
 * @param stop The stop value for internal tween calculations (often 1.0).
 * @param paint The Compose UI [androidx.compose.ui.graphics.Paint] object for styling.
 */
@RestrictTo(RestrictTo.Scope.LIBRARY_GROUP)
public fun RemoteCanvas.drawTweenPath(
    path1: androidx.compose.ui.graphics.Path,
    path2: androidx.compose.ui.graphics.Path,
    tween: RemoteFloat,
    start: RemoteFloat,
    stop: RemoteFloat,
    paint: androidx.compose.ui.graphics.Paint,
) {
    val op =
        recordRenderingOp(paint.asRemotePaint()) {
            if (path1 is RemoteComposePath && path2 is RemoteComposePath) {
                document.drawTweenPath(
                    path1.remote,
                    path2.remote,
                    tween.getFloatIdForCreationState(creationState),
                    start.getFloatIdForCreationState(creationState),
                    stop.getFloatIdForCreationState(creationState),
                )
            } else {
                document.drawTweenPath(
                    path1.asAndroidPath(),
                    path2.asAndroidPath(),
                    tween.getFloatIdForCreationState(creationState),
                    start.getFloatIdForCreationState(creationState),
                    stop.getFloatIdForCreationState(creationState),
                )
            }
        }
    buffer.addRoots(op, tween, start, stop)
}

/**
 * Draws a path, interpolated using tween, between path1, & path2 [RemotePath] with the given
 * [androidx.compose.ui.graphics.Paint].
 *
 * @param path1 The starting [RemotePath].
 * @param path2 The ending [RemotePath].
 * @param tween The interpolation factor (0.0 for `path1`, 1.0 for `path2`).
 * @param start The start value for internal tween calculations (often 0.0).
 * @param stop The stop value for internal tween calculations (often 1.0).
 * @param paint The Compose UI [androidx.compose.ui.graphics.Paint] object for styling.
 */
@RestrictTo(RestrictTo.Scope.LIBRARY_GROUP)
public fun RemoteCanvas.drawTweenPath(
    path1: RemotePath,
    path2: RemotePath,
    tween: RemoteFloat,
    start: RemoteFloat,
    stop: RemoteFloat,
    paint: androidx.compose.ui.graphics.Paint,
) {
    drawTweenPath(path1, path2, tween, start, stop, paint.asRemotePaint())
}

/** Draws an [ImageBitmap] at ([left], [top]) using the specified [paint]. */
@RestrictTo(RestrictTo.Scope.LIBRARY_GROUP)
public fun RemoteCanvas.drawBitmap(bitmap: ImageBitmap, left: Float, top: Float, paint: Paint?) {
    recordRenderingOp(paint?.asRemotePaint()) {
        val androidBitmap = bitmap.asAndroidBitmap()
        document.drawBitmap(
            androidBitmap,
            left,
            top,
            left + androidBitmap.width.toFloat(),
            top + androidBitmap.height.toFloat(),
            "",
        )
    }
}

/** Draws a [Bitmap] at ([left], [top]) using the specified [paint]. */
@RestrictTo(RestrictTo.Scope.LIBRARY_GROUP)
public fun RemoteCanvas.drawBitmap(bitmap: Bitmap, left: Float, top: Float, paint: Paint?) {
    drawBitmap(bitmap.asImageBitmap(), left, top, paint)
}

/** Draws a [RemoteImageBitmap] at ([left], [top]) using the specified [paint]. */
@RestrictTo(RestrictTo.Scope.LIBRARY_GROUP)
public fun RemoteCanvas.drawBitmap(
    bitmap: RemoteImageBitmap,
    left: RemoteFloat,
    top: RemoteFloat,
    paint: Paint?,
) {
    drawBitmap(bitmap, left, top, paint?.asRemotePaint())
}

/** Draws an [ImageBitmap] into [dst] using the specified [paint]. */
@RestrictTo(RestrictTo.Scope.LIBRARY_GROUP)
public fun RemoteCanvas.drawBitmap(
    bitmap: ImageBitmap,
    @Suppress("UNUSED_PARAMETER") src: Rect?,
    dst: Rect,
    paint: Paint?,
) {
    val dstLeft = dst.left.toFloat()
    val dstTop = dst.top.toFloat()
    val dstRight = dst.right.toFloat()
    val dstBottom = dst.bottom.toFloat()
    recordRenderingOp(paint?.asRemotePaint()) {
        val androidBitmap = bitmap.asAndroidBitmap()
        document.drawBitmap(androidBitmap, dstLeft, dstTop, dstRight, dstBottom, "")
    }
}

/** Draws a [Bitmap] into [dst] using the specified [paint]. */
@RestrictTo(RestrictTo.Scope.LIBRARY_GROUP)
public fun RemoteCanvas.drawBitmap(bitmap: Bitmap, src: Rect?, dst: Rect, paint: Paint?) {
    drawBitmap(bitmap.asImageBitmap(), src, dst, paint)
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
        dst.left.toFloat(),
        dst.top.toFloat(),
        dst.right.toFloat(),
        dst.bottom.toFloat(),
        paint?.asRemotePaint(),
    )
}

/** Draws an [ImageBitmap] into [dst] using the specified [paint]. */
@RestrictTo(RestrictTo.Scope.LIBRARY_GROUP)
public fun RemoteCanvas.drawBitmap(
    bitmap: ImageBitmap,
    @Suppress("UNUSED_PARAMETER") src: Rect?,
    dst: RectF,
    paint: Paint?,
) {
    val dstLeft = dst.left
    val dstTop = dst.top
    val dstRight = dst.right
    val dstBottom = dst.bottom
    recordRenderingOp(paint?.asRemotePaint()) {
        val androidBitmap = bitmap.asAndroidBitmap()
        document.drawBitmap(androidBitmap, dstLeft, dstTop, dstRight, dstBottom, "")
    }
}

/** Draws a [Bitmap] into [dst] using the specified [paint]. */
@RestrictTo(RestrictTo.Scope.LIBRARY_GROUP)
public fun RemoteCanvas.drawBitmap(bitmap: Bitmap, src: Rect?, dst: RectF, paint: Paint?) {
    drawBitmap(bitmap.asImageBitmap(), src, dst, paint)
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
    val op =
        recordRenderingOp(paint?.asRemotePaint()) {
            document.drawBitmap(
                bitmap,
                left.getFloatIdForCreationState(creationState),
                top.getFloatIdForCreationState(creationState),
                right.getFloatIdForCreationState(creationState),
                bottom.getFloatIdForCreationState(creationState),
                "",
            )
        }
    buffer.addRoots(op, left, top, right, bottom)
}

/**
 * Draws a scaled portion of a [Bitmap] into a destination rectangle.
 *
 * @param image The [Bitmap] image to draw.
 * @param srcLeft The left coordinate of the source rectangle in the image.
 * @param srcTop The top coordinate of the source rectangle in the image.
 * @param srcRight The right coordinate of the source rectangle in the image.
 * @param srcBottom The bottom coordinate of the source rectangle in the image.
 * @param dstLeft The left coordinate of the destination rectangle on the canvas.
 * @param dstTop The top coordinate of the destination rectangle on the canvas.
 * @param dstRight The right coordinate of the destination rectangle on the canvas.
 * @param dstBottom The bottom coordinate of the destination rectangle on the canvas.
 * @param scaleType An integer representing the scale type (e.g., FIT_XY, CENTER_CROP).
 * @param scaleFactor A general scale factor.
 * @param contentDescription An optional content description for accessibility.
 */
@RestrictTo(RestrictTo.Scope.LIBRARY_GROUP)
public fun RemoteCanvas.drawScaledBitmap(
    image: Bitmap,
    srcLeft: RemoteFloat,
    srcTop: RemoteFloat,
    srcRight: RemoteFloat,
    srcBottom: RemoteFloat,
    dstLeft: RemoteFloat,
    dstTop: RemoteFloat,
    dstRight: RemoteFloat,
    dstBottom: RemoteFloat,
    scaleType: Int,
    scaleFactor: RemoteFloat,
    contentDescription: String?,
) {
    val op = recordRenderingOp {
        document.drawScaledBitmap(
            image,
            srcLeft.getFloatIdForCreationState(creationState),
            srcTop.getFloatIdForCreationState(creationState),
            srcRight.getFloatIdForCreationState(creationState),
            srcBottom.getFloatIdForCreationState(creationState),
            dstLeft.getFloatIdForCreationState(creationState),
            dstTop.getFloatIdForCreationState(creationState),
            dstRight.getFloatIdForCreationState(creationState),
            dstBottom.getFloatIdForCreationState(creationState),
            scaleType,
            scaleFactor.getFloatIdForCreationState(creationState),
            contentDescription,
        )
    }
    buffer.addRoots(
        op,
        srcLeft,
        srcTop,
        srcRight,
        srcBottom,
        dstLeft,
        dstTop,
        dstRight,
        dstBottom,
        scaleFactor,
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
