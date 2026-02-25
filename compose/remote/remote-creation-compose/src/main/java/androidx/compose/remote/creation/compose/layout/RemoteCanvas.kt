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

package androidx.compose.remote.creation.compose.layout

import androidx.annotation.ColorInt
import androidx.annotation.RestrictTo
import androidx.compose.remote.core.operations.DrawTextOnCircle
import androidx.compose.remote.creation.RemotePath
import androidx.compose.remote.creation.compose.capture.RecordingCanvas
import androidx.compose.remote.creation.compose.state.RemoteBitmap
import androidx.compose.remote.creation.compose.state.RemoteBoolean
import androidx.compose.remote.creation.compose.state.RemoteFloat
import androidx.compose.remote.creation.compose.state.RemotePaint
import androidx.compose.remote.creation.compose.state.RemoteStateScope
import androidx.compose.remote.creation.compose.state.RemoteString
import androidx.compose.ui.graphics.ClipOp
import androidx.compose.ui.graphics.Matrix
import androidx.compose.ui.graphics.Paint
import androidx.compose.ui.graphics.Path

/**
 * A wrapper around [RecordingCanvas] that provides overloads for remote types and avoids platform
 * types in its public API where possible.
 */
@RestrictTo(RestrictTo.Scope.LIBRARY_GROUP)
public class RemoteCanvas(
    /** The underlying [RecordingCanvas] being wrapped. */
    public val internalCanvas: RecordingCanvas
) : RemoteStateScope by internalCanvas {
    public val drawScope: RemoteDrawScope = RemoteDrawScope(this)
    public val remote: RemoteAccess = RemoteAccess(drawScope)

    /** Saves the current canvas state. */
    public fun save() {
        internalCanvas.save()
    }

    /** Restores the previous canvas state. */
    public fun restore() {
        internalCanvas.restore()
    }

    /**
     * Translates the canvas by [dx] and [dy].
     *
     * @param dx The translation along the X axis.
     * @param dy The translation along the Y axis.
     */
    public fun translate(dx: RemoteFloat, dy: RemoteFloat) {
        internalCanvas.translate(dx, dy)
    }

    /**
     * Scales the canvas by [sx] and [sy].
     *
     * @param sx The scale factor along the X axis.
     * @param sy The scale factor along the Y axis.
     */
    public fun scale(sx: RemoteFloat, sy: RemoteFloat) {
        internalCanvas.scale(sx, sy)
    }

    /**
     * Scales the canvas by [sx] and [sy] around the pivot point ([px], [py]).
     *
     * @param sx The scale factor along the X axis.
     * @param sy The scale factor along the Y axis.
     * @param pivot The pivot point around which to scale.
     */
    public fun scale(sx: RemoteFloat, sy: RemoteFloat, pivot: RemoteOffset) {
        internalCanvas.scale(sx, sy, pivot.x, pivot.y)
    }

    /**
     * Rotates the canvas by [degrees].
     *
     * @param degrees The angle of rotation in degrees.
     */
    public fun rotate(degrees: RemoteFloat) {
        internalCanvas.rotate(degrees)
    }

    /**
     * Rotates the canvas by [degrees].
     *
     * @param degrees The angle of rotation in degrees.
     * @param pivot The pivot point around which to rotate.
     */
    public fun rotate(degrees: RemoteFloat, pivot: RemoteOffset) {
        internalCanvas.rotate(degrees, pivot.x, pivot.y)
    }

    /**
     * Applies a transformation [matrix] to the canvas.
     *
     * @param matrix The [Matrix] to concatenate with the current canvas transformation.
     */
    public fun transform(matrix: Matrix) {
        internalCanvas.concat(
            android.graphics.Matrix().apply {
                matrix.values.let { v ->
                    setValues(floatArrayOf(v[0], v[1], v[2], v[3], v[4], v[5], v[6], v[7], v[8]))
                }
            }
        )
    }

    /**
     * Draws a rectangle from ([left], [top]) to ([right], [bottom]) using the specified [paint].
     */
    public fun drawRect(
        left: RemoteFloat,
        top: RemoteFloat,
        right: RemoteFloat,
        bottom: RemoteFloat,
        paint: RemotePaint,
    ) {
        internalCanvas.drawRect(left, top, right, bottom, paint)
    }

    /**
     * Draws a rounded rectangle from ([left], [top]) to ([right], [bottom]) with the specified [rx]
     * , [ry], and [paint].
     */
    public fun drawRoundRect(
        left: RemoteFloat,
        top: RemoteFloat,
        right: RemoteFloat,
        bottom: RemoteFloat,
        rx: RemoteFloat,
        ry: RemoteFloat,
        paint: RemotePaint,
    ) {
        internalCanvas.drawRoundRect(left, top, right, bottom, rx, ry, paint)
    }

    /** Draws a circle at ([centerX], [centerY]) with the specified [radius] and [paint]. */
    public fun drawCircle(
        centerX: RemoteFloat,
        centerY: RemoteFloat,
        radius: RemoteFloat,
        paint: RemotePaint,
    ) {
        internalCanvas.drawCircle(centerX, centerY, radius, paint)
    }

    /** Draws an oval from ([left], [top]) to ([right], [bottom]) using the specified [paint]. */
    public fun drawOval(
        left: RemoteFloat,
        top: RemoteFloat,
        right: RemoteFloat,
        bottom: RemoteFloat,
        paint: RemotePaint,
    ) {
        internalCanvas.drawOval(left, top, right, bottom, paint)
    }

    /**
     * Draws an arc from ([left], [top]) to ([right], [bottom]) starting at [startAngle] and
     * sweeping by [sweepAngle] using the specified [paint].
     *
     * @param useCenter If true, include the center of the oval in the arc, which creates a sector.
     */
    public fun drawArc(
        left: RemoteFloat,
        top: RemoteFloat,
        right: RemoteFloat,
        bottom: RemoteFloat,
        startAngle: RemoteFloat,
        sweepAngle: RemoteFloat,
        useCenter: Boolean,
        paint: RemotePaint,
    ) {
        internalCanvas.drawArc(left, top, right, bottom, startAngle, sweepAngle, useCenter, paint)
    }

    /** Draws a line from ([startX], [startY]) to ([stopX], [stopY]) using the specified [paint]. */
    public fun drawLine(
        startX: RemoteFloat,
        startY: RemoteFloat,
        stopX: RemoteFloat,
        stopY: RemoteFloat,
        paint: RemotePaint,
    ) {
        internalCanvas.drawLine(startX, startY, stopX, stopY, paint)
    }

    /**
     * Draws a path that is an interpolation (tween) between [path1] and [path2] based on [tween].
     *
     * @param tween The interpolation factor (0.0 for [path1], 1.0 for [path2]).
     * @param start The start value for internal tween calculations (often 0.0).
     * @param stop The stop value for internal tween calculations (often 1.0).
     */
    public fun drawTweenPath(
        path1: Path,
        path2: Path,
        tween: RemoteFloat,
        start: RemoteFloat,
        stop: RemoteFloat,
        paint: Paint,
    ) {
        internalCanvas.drawTweenPath(path1, path2, tween, start, stop, paint)
    }

    /**
     * Draws a path that is an interpolation (tween) between [path1] and [path2] based on [tween].
     */
    public fun drawTweenPath(
        path1: RemotePath,
        path2: RemotePath,
        tween: RemoteFloat,
        start: RemoteFloat,
        stop: RemoteFloat,
        paint: Paint,
    ) {
        internalCanvas.drawTweenPath(path1, path2, tween, start, stop, paint)
    }

    /** Draws text from [text] at ([x], [y]) using the specified [paint]. */
    public fun drawText(text: RemoteString, x: RemoteFloat, y: RemoteFloat, paint: RemotePaint) {
        internalCanvas.drawText(text, -1, x, y, paint)
    }

    /** Draws a run of text at a specified position. */
    public fun drawTextRun(
        text: RemoteString,
        start: Int,
        end: Int,
        contextStart: Int,
        contextEnd: Int,
        x: RemoteFloat,
        y: RemoteFloat,
        isRtl: Boolean,
        paint: RemotePaint,
    ) {
        internalCanvas.drawTextRun(text, start, end, contextStart, contextEnd, x, y, isRtl, paint)
    }

    /**
     * Draws text with an anchor point and translation factors.
     *
     * @param panx A horizontal translation factor (-1 = left, 0 = center, 1 = right).
     * @param pany A vertical translation factor (-1 = top, 0 = center, 1 = bottom).
     */
    public fun drawAnchoredText(
        text: RemoteString,
        anchorX: RemoteFloat,
        anchorY: RemoteFloat,
        panx: RemoteFloat,
        pany: RemoteFloat,
        flags: Int,
        paint: RemotePaint,
    ) {
        internalCanvas.drawAnchoredText(text, anchorX, anchorY, panx, pany, flags, paint)
    }

    /** Draws text along a given [path] using the specified [paint]. */
    public fun drawTextOnPath(
        text: RemoteString,
        path: RemotePath,
        hOffset: RemoteFloat,
        vOffset: RemoteFloat,
        paint: RemotePaint,
    ) {
        internalCanvas.drawTextOnPath(text, path, hOffset, vOffset, paint)
    }

    /** Draws text from [text] along a given [path] using the specified [paint]. */
    public fun drawTextOnPath(
        text: RemoteString,
        path: android.graphics.Path,
        hOffset: RemoteFloat,
        vOffset: RemoteFloat,
        paint: RemotePaint,
    ) {
        internalCanvas.drawTextOnPath(text, path, hOffset, vOffset, paint)
    }

    /** Draws a path using the specified [paint]. */
    public fun drawPath(path: android.graphics.Path, paint: RemotePaint) {
        internalCanvas.drawPath(path, paint)
    }

    /** Draws a remote path using the specified [paint]. */
    public fun drawPath(path: RemotePath, paint: RemotePaint) {
        internalCanvas.drawRPath(path, paint)
    }

    /** Draws a bitmap at ([left], [top]) using the specified [paint]. */
    public fun drawBitmap(
        bitmap: RemoteBitmap,
        left: RemoteFloat,
        top: RemoteFloat,
        paint: RemotePaint,
    ) {
        internalCanvas.drawBitmap(bitmap, left, top, paint)
    }

    /** Draws a bitmap scaled to the destination rectangle. */
    public fun drawScaledBitmap(
        bitmap: RemoteBitmap,
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
        internalCanvas.drawScaledBitmap(
            bitmap,
            srcLeft,
            srcTop,
            srcRight,
            srcBottom,
            dstLeft,
            dstTop,
            dstRight,
            dstBottom,
            scaleType,
            scaleFactor,
            contentDescription,
        )
    }

    public fun drawTextOnCircle(
        text: RemoteString,
        centerX: RemoteFloat,
        centerY: RemoteFloat,
        radius: RemoteFloat,
        startAngle: RemoteFloat,
        warpRadiusOffset: RemoteFloat,
        alignment: DrawTextOnCircle.Alignment,
        placement: DrawTextOnCircle.Placement,
        paint: RemotePaint,
    ) {
        internalCanvas.drawTextOnCircle(
            text,
            centerX,
            centerY,
            radius,
            startAngle,
            warpRadiusOffset,
            alignment,
            placement,
            paint,
        )
    }

    /** Clips the current canvas state to the specified rectangle. */
    public fun clipRect(
        left: RemoteFloat,
        top: RemoteFloat,
        right: RemoteFloat,
        bottom: RemoteFloat,
        clipOp: ClipOp = ClipOp.Intersect,
    ) {
        internalCanvas.clipRect(
            left.getFloatIdForCreationState(creationState),
            top.getFloatIdForCreationState(creationState),
            right.getFloatIdForCreationState(creationState),
            bottom.getFloatIdForCreationState(creationState),
            // TODO: add ClipOp support to internalCanvas
        )
    }

    /** Clips the current canvas state to the specified [path]. */
    public fun clipPath(path: RemotePath, clipOp: ClipOp = ClipOp.Intersect) {
        internalCanvas.clipPath(path.path)
        // TODO: add ClipOp support to internalCanvas
    }

    /**
     * Instructs the player to conditionally execute [drawCommands] if [condition] evaluates to
     * true.
     */
    public fun drawConditionally(condition: RemoteBoolean, drawCommands: () -> Unit) {
        internalCanvas.drawConditionally(condition, drawCommands)
    }

    /** Instructs the player to draw [drawCommands] into [bitmap]. */
    public fun drawToOffscreenBitmap(bitmap: RemoteBitmap, drawCommands: () -> Unit) {
        internalCanvas.drawToOffscreenBitmap(bitmap, drawCommands)
    }

    /**
     * Instructs the player to draw [drawCommands] into [bitmap] which will be cleared with
     * [clearColor] before any [drawCommands] are processed.
     */
    public fun drawToOffscreenBitmap(
        bitmap: RemoteBitmap,
        @ColorInt clearColor: Int,
        drawCommands: () -> Unit,
    ) {
        internalCanvas.drawToOffscreenBitmap(bitmap, clearColor, drawCommands)
    }

    /**
     * Executes [body] commands in a loop, with the index in the range
     * [from .. until) with a stride of [step].
     */
    public fun loop(
        from: RemoteFloat,
        until: RemoteFloat,
        step: RemoteFloat,
        body: (index: RemoteFloat) -> Unit,
    ) {
        internalCanvas.loop(from, until, step, body)
    }
}
