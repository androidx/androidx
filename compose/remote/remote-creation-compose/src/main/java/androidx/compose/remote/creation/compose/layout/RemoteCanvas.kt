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
import androidx.compose.remote.core.RcPlatformServices.RcPathArrayCreator
import androidx.compose.remote.core.operations.ConditionalOperations
import androidx.compose.remote.core.operations.paint.PaintBundle
import androidx.compose.remote.creation.RemotePath
import androidx.compose.remote.creation.compose.capture.CanvasOp
import androidx.compose.remote.creation.compose.capture.CanvasOperationBuffer
import androidx.compose.remote.creation.compose.capture.PaintTracker
import androidx.compose.remote.creation.compose.capture.PendingOp
import androidx.compose.remote.creation.compose.capture.RemoteComposeCreationState
import androidx.compose.remote.creation.compose.capture.RemoteCreationDisplayInfo
import androidx.compose.remote.creation.compose.modifier.RemoteModifier
import androidx.compose.remote.creation.compose.shapes.MorphTweenUtility
import androidx.compose.remote.creation.compose.state.MutableRemoteFloat
import androidx.compose.remote.creation.compose.state.RemoteBoolean
import androidx.compose.remote.creation.compose.state.RemoteFloat
import androidx.compose.remote.creation.compose.state.RemoteImageBitmap
import androidx.compose.remote.creation.compose.state.RemotePaint
import androidx.compose.remote.creation.compose.state.RemoteStateScope
import androidx.compose.remote.creation.compose.state.RemoteString
import androidx.compose.remote.creation.compose.state.StandardRemotePaint
import androidx.compose.ui.graphics.ClipOp
import androidx.compose.ui.graphics.PathFillType
import androidx.graphics.shapes.RoundedPolygon

/**
 * A remote canvas providing overloads for remote types and avoiding platform types in its public
 * API where possible.
 */
@RestrictTo(RestrictTo.Scope.LIBRARY_GROUP)
public class RemoteCanvas(
    internal val creationState: RemoteComposeCreationState,
    enableOptimizations: Boolean = false,
) : RemoteStateScope by creationState {

    public val creationDisplayInfo: RemoteCreationDisplayInfo = creationState.creationDisplayInfo

    internal val tracker: PaintTracker = PaintTracker()

    internal val buffer: CanvasOperationBuffer = CanvasOperationBuffer(enableOptimizations)

    private var forceSendingPaint: Boolean = false

    private var currentDrawToBitmapId: Int = 0

    public val drawScope: RemoteDrawScope = RemoteDrawScope(this)
    public val remote: RemoteAccess = RemoteAccess(drawScope)

    /** Flushes recorded operations to the underlying creation state. */
    internal fun flush() {
        buffer.flush(creationState)
    }

    /** Draws the content of the component. */
    internal fun drawComponentContent() {
        recordRenderingOp { document.drawComponentContent() }
    }

    /** Emits a custom component with custom properties. */
    internal fun custom(
        config: String,
        modifier: RemoteModifier = RemoteModifier,
        content: (() -> Unit)? = null,
        properties: RemoteCustomPropertiesScope.() -> Unit = {},
    ) {
        val scope = RemoteCustomPropertiesScope().apply(properties)
        val childSpan =
            if (content != null) {
                val span = buffer.recordInChildSpan(content)
                if (buffer.enableOptimizations) {
                    buffer.optimizeSpan(span)
                }
                span
            } else {
                null
            }

        val op =
            buffer.recordRenderingOp(
                CanvasOp.CustomComponent(config, modifier, scope.entries, childSpan)
            )
        for (i in scope.entries.indices) {
            val state = scope.entries[i].state
            if (state != null) {
                buffer.addRoots(op, state)
            }
        }
    }

    /** Processes a [RemotePaint] object and serializes its changes to the remote document. */
    public fun usePaint(paint: RemotePaint?) {
        paint?.let {
            val paintSnapshot = snapshotPaint(it)
            recordRenderingOp { usePaintInternal(paintSnapshot) }
        }
    }

    internal fun snapshotPaint(paint: RemotePaint?): RemotePaint? = paint?.let {
        StandardRemotePaint(it)
    }

    internal fun usePaintInternal(paint: RemotePaint?) {
        if (paint == null) {
            return
        }

        val paintBundle = PaintBundle()

        tracker.reset(forceSendingPaint || document.checkAndClearForceSendingNewPaint())
        tracker.updateWithPaint(paint, paintBundle, creationState)

        if (tracker.isChanged) {
            document.buffer.addPaint(paintBundle)
        }
        forceSendingPaint = false
    }

    internal fun recordRenderingOp(action: () -> Unit): CanvasOperationBuffer.SpanOp {
        return buffer.recordRenderingOp(CanvasOp.Draw { action() })
    }

    internal fun recordRenderingOp(
        paint: RemotePaint?,
        action: () -> Unit,
    ): CanvasOperationBuffer.SpanOp {
        val paintSnapshot = snapshotPaint(paint)
        return recordRenderingOp {
            usePaintInternal(paintSnapshot)
            action()
        }
    }

    internal inline fun recordInOffscreenChildSpan(
        bitmapId: Int,
        action: () -> Unit,
    ): CanvasOperationBuffer.Span {
        val lastDrawToBitmapId = currentDrawToBitmapId
        return buffer.recordInChildSpan {
            currentDrawToBitmapId = bitmapId
            try {
                action()
            } finally {
                currentDrawToBitmapId = lastDrawToBitmapId
            }
        }
    }

    /** Saves the current canvas state. */
    public fun save() {
        buffer.save()
    }

    /** Restores the previous canvas state. */
    public fun restore() {
        buffer.restore()
    }

    /** Restores canvas state to the given [saveCount]. */
    internal fun restoreToCount(saveCount: Int) {
        buffer.restoreToCount(saveCount)
    }

    /**
     * Translates the canvas by [dx] and [dy].
     *
     * @param dx The translation along the X axis.
     * @param dy The translation along the Y axis.
     */
    public fun translate(dx: RemoteFloat, dy: RemoteFloat) {
        val op = buffer.recordRenderingOp(CanvasOp.Transform(PendingOp.Translate(dx, dy)))
        buffer.addRoots(op, dx, dy)
    }

    /**
     * Scales the canvas by [sx] and [sy].
     *
     * @param sx The scale factor along the X axis.
     * @param sy The scale factor along the Y axis.
     */
    public fun scale(sx: RemoteFloat, sy: RemoteFloat) {
        val op = buffer.recordRenderingOp(CanvasOp.Transform(PendingOp.Scale(sx, sy, null, null)))
        buffer.addRoots(op, sx, sy)
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
        scale(sx, sy, pivot.x, pivot.y)
    }

    /** Scales the canvas by [sx] and [sy] around the pivot point ([px], [py]). */
    internal fun scale(sx: RemoteFloat, sy: RemoteFloat, px: RemoteFloat, py: RemoteFloat) {
        val op = buffer.recordRenderingOp(CanvasOp.Transform(PendingOp.Scale(sx, sy, px, py)))
        buffer.addRoots(op, sx, sy, px, py)
    }

    /**
     * Rotates the canvas by [degrees].
     *
     * @param degrees The angle of rotation in degrees.
     */
    public fun rotate(degrees: RemoteFloat) {
        val op = buffer.recordRenderingOp(CanvasOp.Transform(PendingOp.Rotate(degrees, null, null)))
        buffer.addRoots(op, degrees)
    }

    /**
     * Rotates the canvas by [degrees].
     *
     * @param degrees The angle of rotation in degrees.
     * @param pivot The pivot point around which to rotate.
     */
    public fun rotate(degrees: RemoteFloat, pivot: RemoteOffset) {
        rotate(degrees, pivot.x, pivot.y)
    }

    /** Rotates the canvas by [degrees] around the pivot point ([px], [py]). */
    internal fun rotate(degrees: RemoteFloat, px: RemoteFloat, py: RemoteFloat) {
        val op = buffer.recordRenderingOp(CanvasOp.Transform(PendingOp.Rotate(degrees, px, py)))
        buffer.addRoots(op, degrees, px, py)
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
        val op =
            recordRenderingOp(paint) {
                document.drawRect(left.floatId, top.floatId, right.floatId, bottom.floatId)
            }
        buffer.addRoots(op, left, top, right, bottom)
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
        val op =
            recordRenderingOp(paint) {
                document.drawRoundRect(
                    left.floatId,
                    top.floatId,
                    right.floatId,
                    bottom.floatId,
                    rx.floatId,
                    ry.floatId,
                )
            }
        buffer.addRoots(op, left, top, right, bottom, rx, ry)
    }

    public fun drawCircle(
        centerX: RemoteFloat,
        centerY: RemoteFloat,
        radius: RemoteFloat,
        paint: RemotePaint? = null,
    ) {
        val op =
            recordRenderingOp(paint) {
                document.drawCircle(centerX.floatId, centerY.floatId, radius.floatId)
            }
        buffer.addRoots(op, centerX, centerY, radius)
    }

    public fun drawOval(
        left: RemoteFloat,
        top: RemoteFloat,
        right: RemoteFloat,
        bottom: RemoteFloat,
        paint: RemotePaint? = null,
    ) {
        val op =
            recordRenderingOp(paint) {
                document.drawOval(left.floatId, top.floatId, right.floatId, bottom.floatId)
            }
        buffer.addRoots(op, left, top, right, bottom)
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
        val op =
            recordRenderingOp(paint) {
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
        buffer.addRoots(op, left, top, right, bottom, startAngle, sweepAngle)
    }

    public fun drawLine(
        startX: RemoteFloat,
        startY: RemoteFloat,
        stopX: RemoteFloat,
        stopY: RemoteFloat,
        paint: RemotePaint? = null,
    ) {
        val op =
            recordRenderingOp(paint) {
                document.drawLine(startX.floatId, startY.floatId, stopX.floatId, stopY.floatId)
            }
        buffer.addRoots(op, startX, startY, stopX, stopY)
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
        val op =
            recordRenderingOp(paint) {
                document.drawTweenPath(path1, path2, tween.floatId, start.floatId, stop.floatId)
            }
        buffer.addRoots(op, path1, path2, tween, start, stop)
    }

    /** Draws text from [text] at ([x], [y]) using the specified [paint]. */
    public fun drawText(
        text: RemoteString,
        x: RemoteFloat,
        y: RemoteFloat,
        paint: RemotePaint? = null,
    ) {
        val op =
            recordRenderingOp(paint) {
                document.drawTextRun(text.id, 0, -1, 0, -1, x.floatId, y.floatId, false)
            }
        buffer.addRoots(op, text, x, y)
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
        val op =
            recordRenderingOp(paint) {
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
        buffer.addRoots(op, text, x, y)
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
        val op =
            recordRenderingOp(paint) {
                document.drawTextAnchored(
                    text.id,
                    anchorX.floatId,
                    anchorY.floatId,
                    panx.floatId,
                    pany.floatId,
                    flags,
                )
            }
        buffer.addRoots(op, text, anchorX, anchorY, panx, pany)
    }

    public fun drawTextOnPath(
        text: RemoteString,
        path: RemotePath,
        hOffset: RemoteFloat,
        vOffset: RemoteFloat,
        paint: RemotePaint? = null,
    ) {
        val op =
            recordRenderingOp(paint) {
                document.drawTextOnPath(text.id, path, hOffset.floatId, vOffset.floatId)
            }
        buffer.addRoots(op, text, path, hOffset, vOffset)
    }

    /** Draws a path using the specified [paint]. */
    public fun drawPath(
        path: RemotePath,
        pathFillType: PathFillType = PathFillType.NonZero,
        paint: RemotePaint? = null,
    ) {
        val op =
            recordRenderingOp(paint) {
                val pathId =
                    if (pathFillType == PathFillType.EvenOdd) {
                        document.addPathData(path, 1)
                    } else {
                        document.addPathData(path)
                    }
                document.drawPath(pathId)
            }
        buffer.addRoots(op, path)
    }

    /** Draws a [RoundedPolygon] using the specified [paint]. */
    public fun drawRoundedPolygon(roundedPolygon: RoundedPolygon, paint: RemotePaint?) {
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
    public fun drawRoundedPolygonMorph(
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

    /** Draws a bitmap at ([left], [top]) using the specified [paint]. */
    public fun drawBitmap(
        bitmap: RemoteImageBitmap,
        left: RemoteFloat,
        top: RemoteFloat,
        paint: RemotePaint? = null,
    ) {
        val op =
            recordRenderingOp(paint) {
                document.drawBitmap(bitmap.id, left.floatId, top.floatId, "")
            }
        buffer.addRoots(op, bitmap, left, top)
    }

    /** Draws a bitmap scaled to the destination rectangle. */
    public fun drawScaledBitmap(
        bitmap: RemoteImageBitmap,
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
        val op =
            recordRenderingOp(paint) {
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
        buffer.addRoots(
            op,
            bitmap,
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
        val op =
            recordRenderingOp(paint) {
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
        buffer.addRoots(op, text, centerX, centerY, radius, startAngle, warpRadiusOffset)
    }

    /** Clips the current canvas state to the specified rectangle. */
    public fun clipRect(
        left: RemoteFloat,
        top: RemoteFloat,
        right: RemoteFloat,
        bottom: RemoteFloat,
        clipOp: ClipOp = ClipOp.Intersect,
    ) {
        val op =
            buffer.recordRenderingOp(
                CanvasOp.Clip { writer ->
                    writer.clipRect(
                        left.getFloatIdForCreationState(creationState),
                        top.getFloatIdForCreationState(creationState),
                        right.getFloatIdForCreationState(creationState),
                        bottom.getFloatIdForCreationState(creationState),
                    )
                }
            )
        buffer.addRoots(op, left, top, right, bottom)
    }

    /** Clips the current canvas state to the specified [path]. */
    public fun clipPath(path: RemotePath, clipOp: ClipOp = ClipOp.Intersect) {
        val op = recordRenderingOp {
            val pathId = document.addPathData(path)
            document.addClipPath(pathId)
        }
        buffer.addRoots(op, path)
    }

    /**
     * Instructs the player to conditionally execute [drawCommands] if [condition] evaluates to
     * true.
     */
    public fun drawConditionally(condition: RemoteBoolean, drawCommands: () -> Unit) {
        val childSpan = buffer.recordInChildSpan(drawCommands)
        if (buffer.enableOptimizations) {
            buffer.optimizeSpan(childSpan)
        }
        if (!childSpan.emitsWireCommands()) {
            buffer.insertPoint.removeChildSpan(childSpan)
            return
        }

        val op =
            buffer.recordRenderingOp(
                CanvasOp.DrawConditionally(condition, childSpan) { writer, creationState ->
                    if (condition.hasConstantValue) {
                        if (condition.constantValue) {
                            childSpan.record(writer, creationState)
                        }
                    } else {
                        writer.conditionalOperations(
                            ConditionalOperations.TYPE_NEQ,
                            condition.toRemoteInt().toRemoteFloat().floatId,
                            0f,
                        ) {
                            forceSendingPaint = true
                            childSpan.record(writer, creationState)
                            forceSendingPaint = true
                        }
                    }
                }
            )
        buffer.addRoots(op, condition)
    }

    /** Instructs the player to draw [drawCommands] into [bitmap]. */
    public fun drawToOffscreenBitmap(bitmap: RemoteImageBitmap, drawCommands: () -> Unit) {
        val bitmapId = bitmap.id
        val lastDrawToBitmapId = currentDrawToBitmapId
        val childSpan = recordInOffscreenChildSpan(bitmapId, drawCommands)

        val op =
            buffer.recordRenderingOp(
                CanvasOp.Draw(switchesCanvas = true) { writer ->
                    writer.drawOnBitmap(bitmapId, 1, 0)
                    forceSendingPaint = true
                    childSpan.record(writer, creationState)
                    forceSendingPaint = true
                    writer.drawOnBitmap(lastDrawToBitmapId, 1, 0)
                }
            )
        buffer.addRoots(op, bitmap)
    }

    /**
     * Instructs the player to draw [drawCommands] into [bitmap] which will be cleared with
     * [clearColor] before any [drawCommands] are processed.
     */
    public fun drawToOffscreenBitmap(
        bitmap: RemoteImageBitmap,
        @androidx.annotation.ColorInt clearColor: Int,
        drawCommands: () -> Unit,
    ) {
        val bitmapId = bitmap.id
        val lastDrawToBitmapId = currentDrawToBitmapId
        val childSpan = recordInOffscreenChildSpan(bitmapId, drawCommands)

        val op =
            buffer.recordRenderingOp(
                CanvasOp.Draw(switchesCanvas = true) { writer ->
                    writer.drawOnBitmap(bitmapId, 0, clearColor)
                    forceSendingPaint = true
                    childSpan.record(writer, creationState)
                    forceSendingPaint = true
                    writer.drawOnBitmap(lastDrawToBitmapId, 1, 0)
                }
            )
        buffer.addRoots(op, bitmap)
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
        val loopVariable = MutableRemoteFloat()
        val childSpan = buffer.recordInChildSpan { body(loopVariable) }

        val op =
            buffer.recordRenderingOp(
                CanvasOp.Draw { writer ->
                    writer.loop(loopVariable.id, from.floatId, step.floatId, until.floatId) {
                        childSpan.record(writer, creationState)
                    }
                }
            )
        buffer.addRoots(op, from, until, step)
    }

    /** Starts a state layout. */
    public fun startStateLayout(
        modifier: androidx.compose.remote.creation.modifiers.RecordingModifier,
        currentStateId: Int,
    ) {
        recordRenderingOp { document.startStateLayout(modifier, currentStateId) }
    }

    /** Ends a state layout. */
    public fun endStateLayout() {
        recordRenderingOp { document.endStateLayout() }
    }
}
