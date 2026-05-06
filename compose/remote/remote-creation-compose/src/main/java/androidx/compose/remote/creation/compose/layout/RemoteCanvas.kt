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

import androidx.annotation.RestrictTo
import androidx.compose.remote.core.operations.ConditionalOperations
import androidx.compose.remote.core.operations.Utils
import androidx.compose.remote.core.operations.paint.PaintBundle
import androidx.compose.remote.creation.RemotePath
import androidx.compose.remote.creation.compose.capture.RecordingCanvas
import androidx.compose.remote.creation.compose.state.MutableRemoteFloat
import androidx.compose.remote.creation.compose.state.RemoteBitmap
import androidx.compose.remote.creation.compose.state.RemoteBoolean
import androidx.compose.remote.creation.compose.state.RemoteFloat
import androidx.compose.remote.creation.compose.state.RemotePaint
import androidx.compose.remote.creation.compose.state.RemoteStateScope
import androidx.compose.remote.creation.compose.state.RemoteString
import androidx.compose.ui.graphics.ClipOp
import androidx.compose.ui.graphics.Matrix
import androidx.graphics.shapes.RoundedPolygon

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

    /** Processes a [RemotePaint] object and serializes its changes to the remote document. */
    public fun usePaint(paint: RemotePaint?) {
        if (paint == null) return
        val paintBundle = PaintBundle()

        internalCanvas.tracker.reset(
            internalCanvas.forceSendingPaint || document.checkAndClearForceSendingNewPaint()
        )
        internalCanvas.tracker.updateWithPaint(paint, paintBundle, internalCanvas)

        if (internalCanvas.tracker.isChanged) {
            document.buffer.addPaint(paintBundle)
        }
        internalCanvas.forceSendingPaint = false
    }

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
     * Scales the canvas by [sx] and [sy] around the pivot point ([RemoteOffset.x],
     * [RemoteOffset.y]) from [pivot].
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
        // Temporarily use Android graphics Canvas rotate, with does translate/rotate/translate
        internalCanvas.rotate(degrees, pivot.x, pivot.y)
    }

    /**
     * Rotates the canvas by [degrees] around the pivot point ([centerX], [centerY]).
     *
     * @param degrees The angle of rotation in degrees.
     * @param centerX The x-coordinate of the pivot point.
     * @param centerY The y-coordinate of the pivot point.
     */
    public fun rotate(degrees: RemoteFloat, centerX: RemoteFloat, centerY: RemoteFloat) {
        // Temporarily use Android graphics Canvas rotate
        internalCanvas.rotate(degrees.floatId, centerX.floatId, centerY.floatId)
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
        paint: RemotePaint? = null,
    ) {
        usePaint(paint)
        document.drawRect(left.floatId, top.floatId, right.floatId, bottom.floatId)
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
        paint: RemotePaint? = null,
    ) {
        usePaint(paint)
        document.drawRoundRect(
            left.floatId,
            top.floatId,
            right.floatId,
            bottom.floatId,
            rx.floatId,
            ry.floatId,
        )
    }

    public fun drawCircle(
        centerX: RemoteFloat,
        centerY: RemoteFloat,
        radius: RemoteFloat,
        paint: RemotePaint? = null,
    ) {
        usePaint(paint)
        document.drawCircle(centerX.floatId, centerY.floatId, radius.floatId)
    }

    public fun drawOval(
        left: RemoteFloat,
        top: RemoteFloat,
        right: RemoteFloat,
        bottom: RemoteFloat,
        paint: RemotePaint? = null,
    ) {
        usePaint(paint)
        document.drawOval(left.floatId, top.floatId, right.floatId, bottom.floatId)
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
        paint: RemotePaint? = null,
    ) {
        usePaint(paint)
        if (useCenter) {
            document.drawSector(
                left.floatId,
                top.floatId,
                right.floatId,
                bottom.floatId,
                startAngle.floatId,
                sweepAngle.floatId,
            )
        } else {
            document.drawArc(
                left.floatId,
                top.floatId,
                right.floatId,
                bottom.floatId,
                startAngle.floatId,
                sweepAngle.floatId,
            )
        }
    }

    public fun drawLine(
        startX: RemoteFloat,
        startY: RemoteFloat,
        stopX: RemoteFloat,
        stopY: RemoteFloat,
        paint: RemotePaint? = null,
    ) {
        usePaint(paint)
        document.drawLine(startX.floatId, startY.floatId, stopX.floatId, stopY.floatId)
    }

    /**
     * Draws a path that is an interpolation (tween) between [path1] and [path2] based on [tween].
     *
     * @param path1 The first [androidx.compose.remote.creation.RemotePath]
     * @param path2 The second [androidx.compose.remote.creation.RemotePath]
     * @param tween The interpolation factor between 0 and 1.
     * @param start The start of the path segment to draw.
     * @param stop The end of the path segment to draw.
     * @param paint The [RemotePaint] to use.
     */
    public fun drawTweenPath(
        path1: RemotePath,
        path2: RemotePath,
        tween: RemoteFloat,
        start: RemoteFloat,
        stop: RemoteFloat,
        paint: RemotePaint? = null,
    ) {
        usePaint(paint)
        document.drawTweenPath(path1, path2, tween.floatId, start.floatId, stop.floatId)
    }

    /** Draws text from [text] at ([x], [y]) using the specified [paint]. */
    public fun drawText(
        text: RemoteString,
        x: RemoteFloat,
        y: RemoteFloat,
        paint: RemotePaint? = null,
    ) {
        usePaint(paint)
        document.drawTextRun(text.id, 0, -1, 0, -1, x.floatId, y.floatId, false)
    }

    public fun drawTextRun(
        text: RemoteString,
        start: Int,
        end: Int,
        contextStart: Int,
        contextEnd: Int,
        x: RemoteFloat,
        y: RemoteFloat,
        isRtl: Boolean,
        paint: RemotePaint? = null,
    ) {
        usePaint(paint)
        document.drawTextRun(
            text.id,
            start,
            end,
            contextStart,
            contextEnd,
            x.floatId,
            y.floatId,
            isRtl,
        )
    }

    public fun drawAnchoredText(
        text: RemoteString,
        anchorX: RemoteFloat,
        anchorY: RemoteFloat,
        panx: RemoteFloat,
        pany: RemoteFloat,
        flags: Int,
        paint: RemotePaint? = null,
    ) {
        usePaint(paint)
        document.drawTextAnchored(
            text.id,
            anchorX.floatId,
            anchorY.floatId,
            panx.floatId,
            pany.floatId,
            flags,
        )
    }

    public fun drawTextOnPath(
        text: RemoteString,
        path: RemotePath,
        hOffset: RemoteFloat,
        vOffset: RemoteFloat,
        paint: RemotePaint? = null,
    ) {
        usePaint(paint)
        document.drawTextOnPath(text.id, path, hOffset.floatId, vOffset.floatId)
    }

    /** Draws a path using the specified [paint]. */
    public fun drawPath(path: RemotePath, paint: RemotePaint? = null) {
        usePaint(paint)
        val pathId = document.addPathData(path)
        document.drawPath(pathId)
    }

    /** Draws a [RoundedPolygon] using the specified [paint]. */
    public fun drawRoundedPolygon(roundedPolygon: RoundedPolygon, paint: RemotePaint?) {
        internalCanvas.drawRoundedPolygon(roundedPolygon, paint)
    }

    /** Draws a morph between two [RoundedPolygon]s using the specified [paint]. */
    public fun drawRoundedPolygonMorph(
        from: RoundedPolygon,
        to: RoundedPolygon,
        progress: RemoteFloat,
        paint: RemotePaint?,
    ) {
        internalCanvas.drawRoundedPolygonMorph(from, to, progress, paint)
    }

    /** Draws a bitmap at ([left], [top]) using the specified [paint]. */
    public fun drawBitmap(
        bitmap: RemoteBitmap,
        left: RemoteFloat,
        top: RemoteFloat,
        paint: RemotePaint? = null,
    ) {
        usePaint(paint)
        document.drawBitmap(bitmap.id, left.floatId, top.floatId, "")
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
        paint: RemotePaint? = null,
    ) {
        usePaint(paint)
        document.drawScaledBitmap(
            bitmap.id,
            srcLeft.floatId,
            srcTop.floatId,
            srcRight.floatId,
            srcBottom.floatId,
            dstLeft.floatId,
            dstTop.floatId,
            dstRight.floatId,
            dstBottom.floatId,
            scaleType,
            scaleFactor.floatId,
            contentDescription ?: "",
        )
    }

    public fun drawTextOnCircle(
        text: RemoteString,
        centerX: RemoteFloat,
        centerY: RemoteFloat,
        radius: RemoteFloat,
        startAngle: RemoteFloat,
        warpRadiusOffset: RemoteFloat,
        alignment: androidx.compose.remote.core.operations.DrawTextOnCircle.Alignment,
        placement: androidx.compose.remote.core.operations.DrawTextOnCircle.Placement,
        paint: RemotePaint? = null,
    ) {
        usePaint(paint)
        document.drawTextOnCircle(
            text.id,
            centerX.floatId,
            centerY.floatId,
            radius.floatId,
            startAngle.floatId,
            warpRadiusOffset.floatId,
            alignment,
            placement,
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
        document.clipRect(left.floatId, top.floatId, right.floatId, bottom.floatId)
    }

    /** Clips the current canvas state to the specified [path]. */
    public fun clipPath(path: RemotePath, clipOp: ClipOp = ClipOp.Intersect) {
        val pathId = document.addPathData(path)
        document.addClipPath(pathId)
    }

    /**
     * Instructs the player to conditionally execute [drawCommands] if [condition] evaluates to
     * true.
     */
    public fun drawConditionally(condition: RemoteBoolean, drawCommands: () -> Unit) {
        document.conditionalOperations(
            ConditionalOperations.TYPE_NEQ,
            condition.toRemoteInt().toRemoteFloat().floatId,
            0f,
        )
        internalCanvas.forceSendingPaint = true
        drawCommands()
        internalCanvas.forceSendingPaint = true
        document.endConditionalOperations()
    }

    /** Instructs the player to draw [drawCommands] into [bitmap]. */
    public fun drawToOffscreenBitmap(bitmap: RemoteBitmap, drawCommands: () -> Unit) {
        val bitmapId = bitmap.id
        document.drawOnBitmap(bitmapId, 1, 0)

        internalCanvas.forceSendingPaint = true
        val lastDrawToBitmapId = internalCanvas.currentDrawToBitmapId
        internalCanvas.currentDrawToBitmapId = bitmapId
        drawCommands()
        internalCanvas.currentDrawToBitmapId = lastDrawToBitmapId
        internalCanvas.forceSendingPaint = true
        // Switch back to the previous canvas without clearing it.
        document.drawOnBitmap(lastDrawToBitmapId, 1, 0)
    }

    /**
     * Instructs the player to draw [drawCommands] into [bitmap] which will be cleared with
     * [clearColor] before any [drawCommands] are processed.
     */
    public fun drawToOffscreenBitmap(
        bitmap: RemoteBitmap,
        @androidx.annotation.ColorInt clearColor: Int,
        drawCommands: () -> Unit,
    ) {
        val bitmapId = bitmap.id
        document.drawOnBitmap(bitmapId, 0, clearColor)

        internalCanvas.forceSendingPaint = true
        val lastDrawToBitmapId = internalCanvas.currentDrawToBitmapId
        internalCanvas.currentDrawToBitmapId = bitmapId
        drawCommands()
        internalCanvas.currentDrawToBitmapId = lastDrawToBitmapId
        internalCanvas.forceSendingPaint = true
        // Switch back to the previous canvas without clearing it.
        document.drawOnBitmap(lastDrawToBitmapId, 1, 0)
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
        val loopVariableId = document.createFloatId()
        val loopVariable = MutableRemoteFloat(loopVariableId)
        document.loop(Utils.idFromNan(loopVariableId), from.floatId, step.floatId, until.floatId) {
            body(loopVariable)
        }
    }

    /** Starts a state layout. */
    public fun startStateLayout(
        modifier: androidx.compose.remote.creation.modifiers.RecordingModifier,
        currentStateId: Int,
    ) {
        document.startStateLayout(modifier, currentStateId)
    }

    /** Ends a state layout. */
    public fun endStateLayout() {
        document.endStateLayout()
    }
}
