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
import androidx.compose.remote.core.operations.ConditionalOperations
import androidx.compose.remote.core.operations.DrawTextOnCircle
import androidx.compose.remote.creation.RemotePath
import androidx.compose.remote.creation.compose.capture.CanvasOp
import androidx.compose.remote.creation.compose.capture.CanvasOperationBuffer
import androidx.compose.remote.creation.compose.capture.PaintTracker
import androidx.compose.remote.creation.compose.capture.PendingOp
import androidx.compose.remote.creation.compose.capture.RemoteComposeCaptureState
import androidx.compose.remote.creation.compose.capture.RemoteComposeCreationState
import androidx.compose.remote.creation.compose.capture.RemoteCreationDisplayInfo
import androidx.compose.remote.creation.compose.modifier.RemoteModifier
import androidx.compose.remote.creation.compose.modifier.prepareModifiers
import androidx.compose.remote.creation.compose.state.InternalRemoteStateScope
import androidx.compose.remote.creation.compose.state.MutableRemoteFloat
import androidx.compose.remote.creation.compose.state.RemoteBitmapFont
import androidx.compose.remote.creation.compose.state.RemoteBoolean
import androidx.compose.remote.creation.compose.state.RemoteColor
import androidx.compose.remote.creation.compose.state.RemoteFloat
import androidx.compose.remote.creation.compose.state.RemoteFloatArray
import androidx.compose.remote.creation.compose.state.RemoteImageBitmap
import androidx.compose.remote.creation.compose.state.RemoteInt
import androidx.compose.remote.creation.compose.state.RemoteIntArray
import androidx.compose.remote.creation.compose.state.RemoteLong
import androidx.compose.remote.creation.compose.state.RemotePaint
import androidx.compose.remote.creation.compose.state.RemotePathFillType
import androidx.compose.remote.creation.compose.state.RemoteString
import androidx.compose.remote.creation.compose.state.RemoteStringArray
import androidx.compose.remote.creation.compose.state.RemoteValue
import androidx.compose.remote.creation.compose.state.StandardRemotePaint
import androidx.compose.remote.creation.compose.state.getIdListForCreationState
import androidx.compose.remote.document.RemoteArrayId
import androidx.compose.remote.document.RemoteBitmapId
import androidx.compose.remote.document.RemoteDocumentWriter
import androidx.compose.remote.document.RemoteFloatId
import androidx.compose.remote.document.RemoteFontId
import androidx.compose.remote.document.RemotePathId
import androidx.compose.remote.document.RemoteStringId
import androidx.compose.remote.document.addIdList
import androidx.compose.remote.document.mvf

/**
 * A remote canvas providing overloads for remote types and avoiding platform types in its public
 * API where possible.
 */
@RestrictTo(RestrictTo.Scope.LIBRARY_GROUP)
public class RemoteCanvas(
    internal val creationState: RemoteComposeCaptureState,
    enableOptimizations: Boolean = false,
) : InternalRemoteStateScope by creationState {

    override val layoutDirection: RemoteLayoutDirection
        get() = creationState.layoutDirection

    public val creationDisplayInfo: RemoteCreationDisplayInfo = creationState.creationDisplayInfo

    internal val tracker: PaintTracker = PaintTracker()

    internal val buffer: CanvasOperationBuffer = CanvasOperationBuffer(enableOptimizations)

    private var forceSendingPaint: Boolean = false

    private var currentDrawToBitmapId: Int = 0

    public val drawScope: RemoteDrawScope = RemoteDrawScope(this)
    internal val remote: RemoteAccess = RemoteAccess(drawScope)

    /** Flushes recorded operations to the underlying creation state. */
    public fun flush() {
        buffer.flush(creationState)
    }

    /** Draws the content of the component. */
    internal fun drawComponentContent() {
        recordRenderingOp { document.drawContent() }
    }

    /** Emits a custom component with custom properties. */
    public fun custom(
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

    /**
     * Snapshots [path] at record time. The returned function resolves the wire ID of the path at
     * write time, writing the path data on first use in the current span.
     */
    private fun snapshotPath(path: RemotePath, winding: Int = 0): () -> RemotePathId {
        val span = buffer.insertPoint
        val pathData = path.createFloatArray()
        return { RemotePathId(span.getOrPutPathData(document, pathData, winding)) }
    }

    internal fun usePaintInternal(paint: RemotePaint?) {
        if (paint == null) {
            return
        }

        tracker.reset(forceSendingPaint || document.checkAndClearForceSendingNewPaint())
        document.writePaint { paintWriter ->
            tracker.updateWithPaint(paint, paintWriter, creationState)
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

    /** The current number of saves on the canvas state stack. */
    public val saveCount: Int
        get() = buffer.saveCount

    /**
     * Saves the current canvas state.
     *
     * @return the value to pass to [restoreToCount] to balance this [save]
     */
    public fun save(): Int = buffer.save()

    /** Restores the previous canvas state. */
    public fun restore() {
        buffer.restore()
    }

    /** Restores canvas state to the given [saveCount]. */
    public fun restoreToCount(saveCount: Int) {
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
    public fun scale(sx: RemoteFloat, sy: RemoteFloat, px: RemoteFloat, py: RemoteFloat) {
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
    public fun rotate(degrees: RemoteFloat, px: RemoteFloat, py: RemoteFloat) {
        val op = buffer.recordRenderingOp(CanvasOp.Transform(PendingOp.Rotate(degrees, px, py)))
        buffer.addRoots(op, degrees, px, py)
    }

    /**
     * Skews the canvas by [sx] and [sy].
     *
     * @param sx The amount to skew in X.
     * @param sy The amount to skew in Y.
     */
    public fun skew(sx: RemoteFloat, sy: RemoteFloat) {
        val op = buffer.recordRenderingOp(CanvasOp.Transform(PendingOp.Skew(sx, sy)))
        buffer.addRoots(op, sx, sy)
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
                document.drawRect(
                    left.maybeVariableFloat,
                    top.maybeVariableFloat,
                    right.maybeVariableFloat,
                    bottom.maybeVariableFloat,
                )
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
                    left.maybeVariableFloat,
                    top.maybeVariableFloat,
                    right.maybeVariableFloat,
                    bottom.maybeVariableFloat,
                    rx.maybeVariableFloat,
                    ry.maybeVariableFloat,
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
                document.drawCircle(
                    centerX.maybeVariableFloat,
                    centerY.maybeVariableFloat,
                    radius.maybeVariableFloat,
                )
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
                document.drawOval(
                    left.maybeVariableFloat,
                    top.maybeVariableFloat,
                    right.maybeVariableFloat,
                    bottom.maybeVariableFloat,
                )
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
                        left.maybeVariableFloat,
                        top.maybeVariableFloat,
                        right.maybeVariableFloat,
                        bottom.maybeVariableFloat,
                        startAngle.maybeVariableFloat,
                        sweepAngle.maybeVariableFloat,
                    )
                } else {
                    document.drawArc(
                        left.maybeVariableFloat,
                        top.maybeVariableFloat,
                        right.maybeVariableFloat,
                        bottom.maybeVariableFloat,
                        startAngle.maybeVariableFloat,
                        sweepAngle.maybeVariableFloat,
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
                document.drawLine(
                    startX.maybeVariableFloat,
                    startY.maybeVariableFloat,
                    stopX.maybeVariableFloat,
                    stopY.maybeVariableFloat,
                )
            }
        buffer.addRoots(op, startX, startY, stopX, stopY)
    }

    /**
     * Draws a path that is an interpolation (tween) between [path1] and [path2] based on [tween].
     *
     * @param path1 The first [RemotePath]
     * @param path2 The second [RemotePath]
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
        val pathId1 = snapshotPath(path1)
        val pathId2 = snapshotPath(path2)
        val op =
            recordRenderingOp(paint) {
                document.drawTweenPath(
                    pathId1(),
                    pathId2(),
                    tween.maybeVariableFloat,
                    start.maybeVariableFloat,
                    stop.maybeVariableFloat,
                )
            }
        buffer.addRoots(op, tween, start, stop)
    }

    /** Draws text from [text] at ([x], [y]) using the specified [paint]. */
    public fun drawText(
        text: String,
        x: RemoteFloat,
        y: RemoteFloat,
        paint: RemotePaint? = null,
    ) {
        val op =
            recordRenderingOp(paint) {
                val textId = document.addText(text)
                document.drawTextRun(
                    textId,
                    0,
                    text.length,
                    0,
                    text.length,
                    x.maybeVariableFloat,
                    y.maybeVariableFloat,
                    false,
                )
            }
        buffer.addRoots(op, x, y)
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
                document.drawTextRun(
                    RemoteStringId(text.id),
                    0,
                    -1,
                    0,
                    -1,
                    x.maybeVariableFloat,
                    y.maybeVariableFloat,
                    false,
                )
            }
        buffer.addRoots(op, text, x, y)
    }

    public fun drawTextRun(
        text: CharSequence,
        start: Int,
        end: Int,
        contextStart: Int,
        contextEnd: Int,
        x: Float,
        y: Float,
        isRtl: Boolean,
        paint: RemotePaint? = null,
    ) {
        val textString = text.toString()
        recordRenderingOp(paint) {
            val textId = document.addText(textString)
            document.drawTextRun(
                textId,
                start,
                end,
                contextStart,
                contextEnd,
                x.mvf,
                y.mvf,
                isRtl,
            )
        }
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
                    RemoteStringId(text.id),
                    start,
                    end,
                    contextStart,
                    contextEnd,
                    x.maybeVariableFloat,
                    y.maybeVariableFloat,
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
                    RemoteStringId(text.id),
                    anchorX.maybeVariableFloat,
                    anchorY.maybeVariableFloat,
                    panx.maybeVariableFloat,
                    pany.maybeVariableFloat,
                    flags,
                )
            }
        buffer.addRoots(op, text, anchorX, anchorY, panx, pany)
    }

    public fun drawTextOnPath(
        text: String,
        path: RemotePath,
        hOffset: RemoteFloat,
        vOffset: RemoteFloat,
        paint: RemotePaint? = null,
    ) {
        val pathId = snapshotPath(path)
        val op =
            recordRenderingOp(paint) {
                val textId = document.addText(text)
                document.drawTextOnPath(
                    textId,
                    pathId(),
                    hOffset.maybeVariableFloat,
                    vOffset.maybeVariableFloat,
                )
            }
        buffer.addRoots(op, hOffset, vOffset)
    }

    public fun drawTextOnPath(
        text: RemoteString,
        path: RemotePath,
        hOffset: RemoteFloat,
        vOffset: RemoteFloat,
        paint: RemotePaint? = null,
    ) {
        val pathId = snapshotPath(path)
        val op =
            recordRenderingOp(paint) {
                document.drawTextOnPath(
                    RemoteStringId(text.id),
                    pathId(),
                    hOffset.maybeVariableFloat,
                    vOffset.maybeVariableFloat,
                )
            }
        buffer.addRoots(op, text, hOffset, vOffset)
    }

    /**
     * Draws a substring of [text] with [bitmapFont] at position ([x], [y]).
     *
     * @param text The [RemoteString] to draw.
     * @param bitmapFont The [RemoteBitmapFont] to draw [text] with.
     * @param start The character to start drawing from.
     * @param end The character to stop drawing at. Note if this is -1 then all characters from
     *   [start] until the last character of [text] are drawn.
     * @param x The left x-coordinate to start rendering from.
     * @param y The top y-coordinate to start rendering from.
     * @param glyphSpacing Horizontal adjustment in pixels between glyphs.
     * @param paint The [RemotePaint] to render with.
     */
    public fun drawBitmapFontTextRun(
        text: RemoteString,
        bitmapFont: RemoteBitmapFont,
        start: Int,
        end: Int,
        x: RemoteFloat,
        y: RemoteFloat,
        glyphSpacing: RemoteFloat,
        paint: RemotePaint? = null,
    ) {
        val op =
            recordRenderingOp(paint) {
                document.drawBitmapFontTextRun(
                    RemoteStringId(creationState.getId(text)),
                    RemoteFontId(creationState.getId(bitmapFont)),
                    start,
                    end,
                    x.maybeVariableFloat,
                    y.maybeVariableFloat,
                    glyphSpacing.maybeVariableFloat,
                )
            }
        buffer.addRoots(op, text, bitmapFont, x, y, glyphSpacing)
    }

    /**
     * Draws a run of bitmap font text along a [path], starting from [start] to [end] (exclusive) of
     * [text], with an additional vertical offset [yAdj] and horizontal [glyphSpacing] between
     * glyphs.
     *
     * @param text The [RemoteString] to draw.
     * @param bitmapFont The [RemoteBitmapFont] used to render the glyphs.
     * @param path The [RemotePath] along which to draw the text run.
     * @param start The start index in [text] (inclusive).
     * @param end The end index in [text] (exclusive), or `-1` for the end of the string.
     * @param yAdj Vertical adjustment applied to each glyph relative to the path.
     * @param glyphSpacing Extra spacing in pixels added between adjacent glyphs.
     * @param paint The [RemotePaint] to use for rendering, or `null` to use the current paint.
     */
    public fun drawBitmapFontTextRunOnPath(
        text: RemoteString,
        bitmapFont: RemoteBitmapFont,
        path: RemotePath,
        start: Int,
        end: Int,
        yAdj: Float,
        glyphSpacing: Float,
        paint: RemotePaint? = null,
    ) {
        val pathId = snapshotPath(path)
        val op =
            recordRenderingOp(paint) {
                document.drawBitmapFontTextRunOnPath(
                    RemoteStringId(text.id),
                    RemoteFontId(bitmapFont.id),
                    pathId(),
                    start,
                    end,
                    yAdj,
                    glyphSpacing,
                )
            }
        buffer.addRoots(op, text, bitmapFont)
    }

    /** Draws a path using the specified [pathFillType] and [paint]. */
    public fun drawPath(
        path: RemotePath,
        pathFillType: RemotePathFillType = RemotePathFillType.NonZero,
        paint: RemotePaint? = null,
    ) {
        val pathId = snapshotPath(path, pathFillType.value)
        recordRenderingOp(paint) {
            document.drawPath(pathId())
        }
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
                document.drawBitmap(
                    RemoteBitmapId(bitmap.id),
                    left.maybeVariableFloat,
                    top.maybeVariableFloat,
                    "",
                )
            }
        buffer.addRoots(op, bitmap, left, top)
    }

    /**
     * Draws a [bitmap] into the destination rectangle `[left, top, right, bottom]`.
     *
     * @param bitmap The [RemoteImageBitmap] to draw.
     * @param left The left coordinate of the destination rectangle.
     * @param top The top coordinate of the destination rectangle.
     * @param right The right coordinate of the destination rectangle.
     * @param bottom The bottom coordinate of the destination rectangle.
     * @param paint The [RemotePaint] to use for rendering, or `null` to use the current paint.
     */
    public fun drawBitmap(
        bitmap: RemoteImageBitmap,
        left: RemoteFloat,
        top: RemoteFloat,
        right: RemoteFloat,
        bottom: RemoteFloat,
        paint: RemotePaint? = null,
    ) {
        val op =
            recordRenderingOp(paint) {
                document.drawBitmap(
                    RemoteBitmapId(bitmap.id),
                    left.maybeVariableFloat,
                    top.maybeVariableFloat,
                    right.maybeVariableFloat,
                    bottom.maybeVariableFloat,
                    "",
                )
            }
        buffer.addRoots(op, bitmap, left, top, right, bottom)
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
                    RemoteBitmapId(bitmap.id),
                    srcLeft.maybeVariableFloat,
                    srcTop.maybeVariableFloat,
                    srcRight.maybeVariableFloat,
                    srcBottom.maybeVariableFloat,
                    dstLeft.maybeVariableFloat,
                    dstTop.maybeVariableFloat,
                    dstRight.maybeVariableFloat,
                    dstBottom.maybeVariableFloat,
                    scaleType,
                    scaleFactor.maybeVariableFloat,
                    contentDescription,
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
        alignment: DrawTextOnCircle.Alignment,
        placement: DrawTextOnCircle.Placement,
        paint: RemotePaint? = null,
    ) {
        val op =
            recordRenderingOp(paint) {
                document.drawTextOnCircle(
                    RemoteStringId(text.id),
                    centerX.maybeVariableFloat,
                    centerY.maybeVariableFloat,
                    radius.maybeVariableFloat,
                    startAngle.maybeVariableFloat,
                    warpRadiusOffset.maybeVariableFloat,
                    alignment.ordinal,
                    placement.ordinal,
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
    ) {
        val op =
            buffer.recordRenderingOp(
                CanvasOp.Clip { writer ->
                    writer.clipRect(
                        creationState.getMaybeVariableFloat(left),
                        creationState.getMaybeVariableFloat(top),
                        creationState.getMaybeVariableFloat(right),
                        creationState.getMaybeVariableFloat(bottom),
                    )
                }
            )
        buffer.addRoots(op, left, top, right, bottom)
    }

    /** Clips the current canvas state to the specified [path]. */
    public fun clipPath(path: RemotePath) {
        val pathId = snapshotPath(path)
        buffer.recordRenderingOp(CanvasOp.Clip { writer -> writer.clipPath(pathId()) })
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
                            ConditionalOperations.TYPE_NEQ.toInt(),
                            condition.toRemoteInt().toRemoteFloat().maybeVariableFloat,
                            0f.mvf,
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
                    writer.drawOnBitmap(RemoteBitmapId(bitmapId), 1, 0)
                    forceSendingPaint = true
                    childSpan.record(writer, creationState)
                    forceSendingPaint = true
                    writer.drawOnBitmap(RemoteBitmapId(lastDrawToBitmapId), 1, 0)
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
        @ColorInt clearColor: Int,
        drawCommands: () -> Unit,
    ) {
        val bitmapId = bitmap.id
        val lastDrawToBitmapId = currentDrawToBitmapId
        val childSpan = recordInOffscreenChildSpan(bitmapId, drawCommands)

        val op =
            buffer.recordRenderingOp(
                CanvasOp.Draw(switchesCanvas = true) { writer ->
                    writer.drawOnBitmap(RemoteBitmapId(bitmapId), 0, clearColor)
                    forceSendingPaint = true
                    childSpan.record(writer, creationState)
                    forceSendingPaint = true
                    writer.drawOnBitmap(RemoteBitmapId(lastDrawToBitmapId), 1, 0)
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
        val loopVariable = MutableRemoteFloat.createReserved()
        val childSpan = buffer.recordInChildSpan { body(loopVariable) }

        val op =
            buffer.recordRenderingOp(
                CanvasOp.Draw { writer ->
                    writer.loop(
                        loopVariable.id,
                        from.maybeVariableFloat,
                        step.maybeVariableFloat,
                        until.maybeVariableFloat,
                    ) {
                        childSpan.record(writer, creationState)
                    }
                }
            )
        buffer.addRoots(op, from, until, step)
    }

    /**
     * Executes [body] commands in a loop, with the index in the range [from .. until).
     *
     * Note there is a maximum number of operations that will be executed, which may be less than
     * the total number of iterations needed to fully realize this loop.
     *
     * @param from The initial value of index.
     * @param until The loop will be executed until the index is >= this value.
     * @param body Code that generates draw calls to run in a loop.
     */
    public fun loop(from: Int, until: RemoteInt, body: (index: RemoteInt) -> Unit) {
        val loopVariable = MutableRemoteFloat.createReserved()
        val childSpan = buffer.recordInChildSpan { body(loopVariable.toRemoteInt()) }

        val op =
            buffer.recordRenderingOp(
                CanvasOp.Draw { writer ->
                    writer.loop(
                        loopVariable.id,
                        from.toFloat().mvf,
                        1f.mvf,
                        until.maybeVariableFloat,
                    ) {
                        childSpan.record(writer, creationState)
                    }
                }
            )
        buffer.addRoots(op, until)
    }

    private fun generateFormalArg(id: Int, actualArg: Any?): RemoteValue<*> {
        return when (actualArg) {
            is RemoteFloat -> RemoteFloat(RemoteFloatId(id))
            is RemoteInt -> RemoteInt.createForId(id.toLong() + 0x100000000L)
            is RemoteString -> RemoteString.createForId(id)
            is RemoteColor -> RemoteColor.createForId(id)
            is RemoteBoolean -> RemoteBoolean.createForId(id)
            is RemoteImageBitmap -> RemoteImageBitmap.createForId(id)
            is RemoteLong -> RemoteLong.createForId(id)
            is RemoteFloatArray -> RemoteFloatArray.createForId(id)
            is RemoteStringArray -> RemoteStringArray.createForId(id)
            is RemoteIntArray -> RemoteIntArray.createForId(id)
            else -> throw IllegalArgumentException("Unsupported arg type: ${actualArg?.javaClass}")
        }
    }

    /**
     * Maps an actual argument value to its corresponding document variable ID for a pattern call.
     *
     * @param creationState creation state associated with the document being written
     * @return document ID for the pattern argument
     */
    private fun RemoteValue<*>.getPatternArgId(creationState: RemoteComposeCreationState): Int {
        // RemoteFloatArray defaults to a primitive float array (DataListFloat) for spline and
        // dereference efficiency, but PatternForEach requires variable IDs (DataListIds) to remap
        // elements during pattern expansion.
        return if (this is RemoteFloatArray) {
            getIdListForCreationState(creationState)
        } else {
            creationState.getId(this)
        }
    }

    private fun writeRemoteModifier(
        modifier: RemoteModifier,
        writer: RemoteDocumentWriter = document,
    ) {
        val writeModifiers = creationState.prepareModifiers(modifier)
        writeModifiers(writer)
    }

    /**
     * Defines a reusable drawing pattern with no parameters.
     *
     * Records [action] as a macro template on first call. Subsequent invocations inflate the
     * template.
     *
     * @param action drawing commands to record into the pattern template
     * @return a [RemotePattern0] that inflates the recorded pattern
     */
    public fun definePattern(action: () -> Unit): RemotePattern0 {
        var patternId = -1
        return RemotePattern0 { modifier ->
            if (patternId == -1) {
                patternId = document.allocateId()
                val patternBodySpan = buffer.recordInChildSpan(action)
                buffer.recordRenderingOp(
                    CanvasOp.Draw { writer ->
                        writer.definePattern(patternId, intArrayOf())
                        patternBodySpan.record(writer, creationState)
                        writer.endPatternDefine()
                    }
                )
            }
            buffer.recordRenderingOp(
                CanvasOp.Draw { writer ->
                    writer.inflatePattern(patternId, intArrayOf())
                    modifier?.let { writeRemoteModifier(it, writer) }
                    writer.endPatternInflation()
                }
            )
        }
    }

    /**
     * Defines a reusable drawing pattern with 1 parameter.
     *
     * Records [action] as a macro template on first call. Subsequent invocations inflate the
     * template with the provided argument.
     *
     * @param action drawing commands parameterized by [T1]
     * @return a [RemotePattern1] that inflates the recorded pattern
     */
    @Suppress("UNCHECKED_CAST")
    public fun <T1 : RemoteValue<*>> definePattern(action: (T1) -> Unit): RemotePattern1<T1> {
        var patternId = -1
        return RemotePattern1 { arg1, modifier ->
            if (patternId == -1) {
                patternId = document.allocateId()
                val argId1 = document.allocateId()
                val paramIds = intArrayOf(argId1)
                val formal1 = generateFormalArg(argId1, arg1) as T1
                val patternBodySpan = buffer.recordInChildSpan { action(formal1) }
                buffer.recordRenderingOp(
                    CanvasOp.Draw { writer ->
                        writer.definePattern(patternId, paramIds)
                        patternBodySpan.record(writer, creationState)
                        writer.endPatternDefine()
                    }
                )
            }
            val opInflate =
                buffer.recordRenderingOp(
                    CanvasOp.Draw { writer ->
                        writer.inflatePattern(
                            patternId,
                            intArrayOf(arg1.getPatternArgId(creationState)),
                        )
                        modifier?.let { writeRemoteModifier(it, writer) }
                        writer.endPatternInflation()
                    }
                )
            buffer.addRoots(opInflate, arg1)
        }
    }

    /**
     * Defines a reusable drawing pattern with 2 parameters.
     *
     * Records [action] as a macro template on first call. Subsequent invocations inflate the
     * template with the provided arguments.
     *
     * @param action drawing commands parameterized by [T1] and [T2]
     * @return a [RemotePattern2] that inflates the recorded pattern
     */
    @Suppress("UNCHECKED_CAST")
    public fun <T1 : RemoteValue<*>, T2 : RemoteValue<*>> definePattern(
        action: (T1, T2) -> Unit
    ): RemotePattern2<T1, T2> {
        var patternId = -1
        return RemotePattern2 { arg1, arg2, modifier ->
            if (patternId == -1) {
                patternId = document.allocateId()
                val argId1 = document.allocateId()
                val argId2 = document.allocateId()
                val paramIds = intArrayOf(argId1, argId2)
                val formal1 = generateFormalArg(argId1, arg1) as T1
                val formal2 = generateFormalArg(argId2, arg2) as T2
                val patternBodySpan = buffer.recordInChildSpan { action(formal1, formal2) }
                buffer.recordRenderingOp(
                    CanvasOp.Draw { writer ->
                        writer.definePattern(patternId, paramIds)
                        patternBodySpan.record(writer, creationState)
                        writer.endPatternDefine()
                    }
                )
            }
            val opInflate =
                buffer.recordRenderingOp(
                    CanvasOp.Draw { writer ->
                        writer.inflatePattern(
                            patternId,
                            intArrayOf(
                                arg1.getPatternArgId(creationState),
                                arg2.getPatternArgId(creationState),
                            ),
                        )
                        modifier?.let { writeRemoteModifier(it, writer) }
                        writer.endPatternInflation()
                    }
                )
            buffer.addRoots(opInflate, arg1, arg2)
        }
    }

    /**
     * Defines a reusable drawing pattern with 3 parameters.
     *
     * Records [action] as a macro template on first call. Subsequent invocations inflate the
     * template with the provided arguments.
     *
     * @param action drawing commands parameterized by [T1], [T2], and [T3]
     * @return a [RemotePattern3] that inflates the recorded pattern
     */
    @Suppress("UNCHECKED_CAST")
    public fun <
        T1 : RemoteValue<*>,
        T2 : RemoteValue<*>,
        T3 : RemoteValue<*>,
    > definePattern(action: (T1, T2, T3) -> Unit): RemotePattern3<T1, T2, T3> {
        var patternId = -1
        return RemotePattern3 { arg1, arg2, arg3, modifier ->
            if (patternId == -1) {
                patternId = document.allocateId()
                val argId1 = document.allocateId()
                val argId2 = document.allocateId()
                val argId3 = document.allocateId()
                val paramIds = intArrayOf(argId1, argId2, argId3)
                val formal1 = generateFormalArg(argId1, arg1) as T1
                val formal2 = generateFormalArg(argId2, arg2) as T2
                val formal3 = generateFormalArg(argId3, arg3) as T3
                val patternBodySpan = buffer.recordInChildSpan { action(formal1, formal2, formal3) }
                buffer.recordRenderingOp(
                    CanvasOp.Draw { writer ->
                        writer.definePattern(patternId, paramIds)
                        patternBodySpan.record(writer, creationState)
                        writer.endPatternDefine()
                    }
                )
            }
            val opInflate =
                buffer.recordRenderingOp(
                    CanvasOp.Draw { writer ->
                        writer.inflatePattern(
                            patternId,
                            intArrayOf(
                                arg1.getPatternArgId(creationState),
                                arg2.getPatternArgId(creationState),
                                arg3.getPatternArgId(creationState),
                            ),
                        )
                        modifier?.let { writeRemoteModifier(it, writer) }
                        writer.endPatternInflation()
                    }
                )
            buffer.addRoots(opInflate, arg1, arg2, arg3)
        }
    }

    /**
     * Defines a reusable drawing pattern with 4 parameters.
     *
     * Records [action] as a macro template on first call. Subsequent invocations inflate the
     * template with the provided arguments.
     *
     * @param action drawing commands parameterized by [T1], [T2], [T3], and [T4]
     * @return a [RemotePattern4] that inflates the recorded pattern
     */
    @Suppress("UNCHECKED_CAST")
    public fun <
        T1 : RemoteValue<*>,
        T2 : RemoteValue<*>,
        T3 : RemoteValue<*>,
        T4 : RemoteValue<*>,
    > definePattern(action: (T1, T2, T3, T4) -> Unit): RemotePattern4<T1, T2, T3, T4> {
        var patternId = -1
        return RemotePattern4 { arg1, arg2, arg3, arg4, modifier ->
            if (patternId == -1) {
                patternId = document.allocateId()
                val argId1 = document.allocateId()
                val argId2 = document.allocateId()
                val argId3 = document.allocateId()
                val argId4 = document.allocateId()
                val paramIds = intArrayOf(argId1, argId2, argId3, argId4)
                val formal1 = generateFormalArg(argId1, arg1) as T1
                val formal2 = generateFormalArg(argId2, arg2) as T2
                val formal3 = generateFormalArg(argId3, arg3) as T3
                val formal4 = generateFormalArg(argId4, arg4) as T4
                val patternBodySpan = buffer.recordInChildSpan {
                    action(formal1, formal2, formal3, formal4)
                }
                buffer.recordRenderingOp(
                    CanvasOp.Draw { writer ->
                        writer.definePattern(patternId, paramIds)
                        patternBodySpan.record(writer, creationState)
                        writer.endPatternDefine()
                    }
                )
            }
            val opInflate =
                buffer.recordRenderingOp(
                    CanvasOp.Draw { writer ->
                        writer.inflatePattern(
                            patternId,
                            intArrayOf(
                                arg1.getPatternArgId(creationState),
                                arg2.getPatternArgId(creationState),
                                arg3.getPatternArgId(creationState),
                                arg4.getPatternArgId(creationState),
                            ),
                        )
                        modifier?.let { writeRemoteModifier(it, writer) }
                        writer.endPatternInflation()
                    }
                )
            buffer.addRoots(opInflate, arg1, arg2, arg3, arg4)
        }
    }

    /**
     * Defines a reusable drawing pattern with 5 parameters.
     *
     * Records [action] as a macro template on first call. Subsequent invocations inflate the
     * template with the provided arguments.
     *
     * @param action drawing commands parameterized by [T1], [T2], [T3], [T4], and [T5]
     * @return a [RemotePattern5] that inflates the recorded pattern
     */
    @Suppress("UNCHECKED_CAST")
    public fun <
        T1 : RemoteValue<*>,
        T2 : RemoteValue<*>,
        T3 : RemoteValue<*>,
        T4 : RemoteValue<*>,
        T5 : RemoteValue<*>,
    > definePattern(action: (T1, T2, T3, T4, T5) -> Unit): RemotePattern5<T1, T2, T3, T4, T5> {
        var patternId = -1
        return RemotePattern5 { arg1, arg2, arg3, arg4, arg5, modifier ->
            if (patternId == -1) {
                patternId = document.allocateId()
                val argId1 = document.allocateId()
                val argId2 = document.allocateId()
                val argId3 = document.allocateId()
                val argId4 = document.allocateId()
                val argId5 = document.allocateId()
                val paramIds = intArrayOf(argId1, argId2, argId3, argId4, argId5)
                val formal1 = generateFormalArg(argId1, arg1) as T1
                val formal2 = generateFormalArg(argId2, arg2) as T2
                val formal3 = generateFormalArg(argId3, arg3) as T3
                val formal4 = generateFormalArg(argId4, arg4) as T4
                val formal5 = generateFormalArg(argId5, arg5) as T5
                val patternBodySpan = buffer.recordInChildSpan {
                    action(formal1, formal2, formal3, formal4, formal5)
                }
                buffer.recordRenderingOp(
                    CanvasOp.Draw { writer ->
                        writer.definePattern(patternId, paramIds)
                        patternBodySpan.record(writer, creationState)
                        writer.endPatternDefine()
                    }
                )
            }
            val opInflate =
                buffer.recordRenderingOp(
                    CanvasOp.Draw { writer ->
                        writer.inflatePattern(
                            patternId,
                            intArrayOf(
                                arg1.getPatternArgId(creationState),
                                arg2.getPatternArgId(creationState),
                                arg3.getPatternArgId(creationState),
                                arg4.getPatternArgId(creationState),
                                arg5.getPatternArgId(creationState),
                            ),
                        )
                        modifier?.let { writeRemoteModifier(it, writer) }
                        writer.endPatternInflation()
                    }
                )
            buffer.addRoots(opInflate, arg1, arg2, arg3, arg4, arg5)
        }
    }

    /**
     * Defines a reusable drawing pattern with 6 parameters.
     *
     * Records [action] as a macro template on first call. Subsequent invocations inflate the
     * template with the provided arguments.
     *
     * @param action drawing commands parameterized by [T1] through [T6]
     * @return a [RemotePattern6] that inflates the recorded pattern
     */
    @Suppress("UNCHECKED_CAST")
    public fun <
        T1 : RemoteValue<*>,
        T2 : RemoteValue<*>,
        T3 : RemoteValue<*>,
        T4 : RemoteValue<*>,
        T5 : RemoteValue<*>,
        T6 : RemoteValue<*>,
    > definePattern(
        action: (T1, T2, T3, T4, T5, T6) -> Unit
    ): RemotePattern6<T1, T2, T3, T4, T5, T6> {
        var patternId = -1
        return RemotePattern6 { arg1, arg2, arg3, arg4, arg5, arg6, modifier ->
            if (patternId == -1) {
                patternId = document.allocateId()
                val argId1 = document.allocateId()
                val argId2 = document.allocateId()
                val argId3 = document.allocateId()
                val argId4 = document.allocateId()
                val argId5 = document.allocateId()
                val argId6 = document.allocateId()
                val paramIds = intArrayOf(argId1, argId2, argId3, argId4, argId5, argId6)
                val formal1 = generateFormalArg(argId1, arg1) as T1
                val formal2 = generateFormalArg(argId2, arg2) as T2
                val formal3 = generateFormalArg(argId3, arg3) as T3
                val formal4 = generateFormalArg(argId4, arg4) as T4
                val formal5 = generateFormalArg(argId5, arg5) as T5
                val formal6 = generateFormalArg(argId6, arg6) as T6
                val patternBodySpan = buffer.recordInChildSpan {
                    action(formal1, formal2, formal3, formal4, formal5, formal6)
                }
                buffer.recordRenderingOp(
                    CanvasOp.Draw { writer ->
                        writer.definePattern(patternId, paramIds)
                        patternBodySpan.record(writer, creationState)
                        writer.endPatternDefine()
                    }
                )
            }
            val opInflate =
                buffer.recordRenderingOp(
                    CanvasOp.Draw { writer ->
                        writer.inflatePattern(
                            patternId,
                            intArrayOf(
                                arg1.getPatternArgId(creationState),
                                arg2.getPatternArgId(creationState),
                                arg3.getPatternArgId(creationState),
                                arg4.getPatternArgId(creationState),
                                arg5.getPatternArgId(creationState),
                                arg6.getPatternArgId(creationState),
                            ),
                        )
                        modifier?.let { writeRemoteModifier(it, writer) }
                        writer.endPatternInflation()
                    }
                )
            buffer.addRoots(opInflate, arg1, arg2, arg3, arg4, arg5, arg6)
        }
    }

    /**
     * Iterates over [items] and records [action] for each element.
     *
     * @param items list of items to iterate over
     * @param action drawing commands to record for each element
     */
    @Suppress("UNCHECKED_CAST")
    public fun <T : RemoteValue<*>> forEach(items: List<T>, action: (T) -> Unit) {
        if (items.isEmpty()) return
        val localItemId = document.allocateId()
        val formal = generateFormalArg(localItemId, items.first()) as T
        val childSpan = buffer.recordInChildSpan { action(formal) }

        val op =
            buffer.recordRenderingOp(
                CanvasOp.Draw { writer ->
                    val ids = IntArray(items.size)
                    for (i in items.indices) {
                        ids[i] = creationState.getId(items[i])
                    }
                    val collectionId = writer.addIdList(ids).id
                    writer.startPatternForEach(collectionId, localItemId)
                    childSpan.record(writer, creationState)
                    writer.endPatternForEach()
                }
            )
        for (i in items.indices) {
            buffer.addRoots(op, items[i])
        }
    }

    /**
     * Iterates over [array] and records [action] for each float element.
     *
     * @param array remote float array to iterate over
     * @param action drawing commands to record for each element
     */
    public fun forEach(array: RemoteFloatArray, action: (RemoteFloat) -> Unit) {
        val localItemId = document.allocateId()
        val formal = RemoteFloat(RemoteFloatId(localItemId))
        val childSpan = buffer.recordInChildSpan { action(formal) }

        val op =
            buffer.recordRenderingOp(
                CanvasOp.Draw { writer ->
                    // RemoteFloatArray requires an ID list so PatternForEach can map elements.
                    val collectionId = array.getIdListForCreationState(creationState)
                    writer.startPatternForEach(collectionId, localItemId)
                    childSpan.record(writer, creationState)
                    writer.endPatternForEach()
                }
            )
        buffer.addRoots(op, array)
    }

    /**
     * Iterates over [array] and records [action] for each string element.
     *
     * @param array remote string array to iterate over
     * @param action drawing commands to record for each element
     */
    public fun forEach(array: RemoteStringArray, action: (RemoteString) -> Unit) {
        val localItemId = document.allocateId()
        val formal = RemoteString.createForId(localItemId)
        val childSpan = buffer.recordInChildSpan { action(formal) }

        val op =
            buffer.recordRenderingOp(
                CanvasOp.Draw { writer ->
                    val collectionId = creationState.getId(array)
                    writer.startPatternForEach(collectionId, localItemId)
                    childSpan.record(writer, creationState)
                    writer.endPatternForEach()
                }
            )
        buffer.addRoots(op, array)
    }

    /**
     * Iterates over [array] and records [action] for each integer element.
     *
     * @param array remote integer array to iterate over
     * @param action drawing commands to record for each element
     */
    public fun forEach(array: RemoteIntArray, action: (RemoteInt) -> Unit) {
        val localItemId = document.allocateId()
        val formal = RemoteInt.createForId(localItemId.toLong() + 0x100000000L)
        val childSpan = buffer.recordInChildSpan { action(formal) }

        val op =
            buffer.recordRenderingOp(
                CanvasOp.Draw { writer ->
                    val collectionId = creationState.getId(array)
                    writer.startPatternForEach(collectionId, localItemId)
                    childSpan.record(writer, creationState)
                    writer.endPatternForEach()
                }
            )
        buffer.addRoots(op, array)
    }

    /**
     * Iterates over [items1] and [items2] pairwise and records [action] for each pair.
     *
     * Iteration stops at the length of the shorter list.
     *
     * @param items1 first list of items to iterate over
     * @param items2 second list of items to iterate over
     * @param action drawing commands to record for each pair
     */
    @Suppress("UNCHECKED_CAST")
    public fun <T1 : RemoteValue<*>, T2 : RemoteValue<*>> forEach(
        items1: List<T1>,
        items2: List<T2>,
        action: (T1, T2) -> Unit,
    ) {
        val size = minOf(items1.size, items2.size)
        if (size == 0) return

        val localTupleId = document.allocateId()
        val localTupleArrayId = RemoteArrayId(localTupleId)
        val id1 = document.allocateId()
        val id2 = document.allocateId()
        val formal1 = generateFormalArg(id1, items1.first()) as T1
        val formal2 = generateFormalArg(id2, items2.first()) as T2

        val childSpan = buffer.recordInChildSpan {
            buffer.recordRenderingOp(
                CanvasOp.Draw { writer ->
                    writer.idLookup(localTupleArrayId, 0f.mvf, destId = id1)
                    writer.idLookup(localTupleArrayId, 1f.mvf, destId = id2)
                }
            )
            action(formal1, formal2)
        }

        val op =
            buffer.recordRenderingOp(
                CanvasOp.Draw { writer ->
                    val tupleIds = IntArray(size)
                    for (i in 0 until size) {
                        tupleIds[i] =
                            writer
                                .addIdList(
                                    creationState.getId(items1[i]),
                                    creationState.getId(items2[i]),
                                )
                                .id
                    }
                    val outerListId = writer.addIdList(tupleIds).id
                    writer.startPatternForEach(outerListId, localTupleId)
                    childSpan.record(writer, creationState)
                    writer.endPatternForEach()
                }
            )
        for (i in items1.indices) buffer.addRoots(op, items1[i])
        for (i in items2.indices) buffer.addRoots(op, items2[i])
    }

    /**
     * Iterates over [items1], [items2], and [items3] and records [action] for each triple.
     *
     * Iteration stops at the length of the shortest list.
     *
     * @param items1 first list of items to iterate over
     * @param items2 second list of items to iterate over
     * @param items3 third list of items to iterate over
     * @param action drawing commands to record for each triple
     */
    @Suppress("UNCHECKED_CAST")
    public fun <
        T1 : RemoteValue<*>,
        T2 : RemoteValue<*>,
        T3 : RemoteValue<*>,
    > forEach(
        items1: List<T1>,
        items2: List<T2>,
        items3: List<T3>,
        action: (T1, T2, T3) -> Unit,
    ) {
        val size = minOf(items1.size, items2.size, items3.size)
        if (size == 0) return

        val localTupleId = document.allocateId()
        val localTupleArrayId = RemoteArrayId(localTupleId)
        val id1 = document.allocateId()
        val id2 = document.allocateId()
        val id3 = document.allocateId()
        val formal1 = generateFormalArg(id1, items1.first()) as T1
        val formal2 = generateFormalArg(id2, items2.first()) as T2
        val formal3 = generateFormalArg(id3, items3.first()) as T3

        val childSpan = buffer.recordInChildSpan {
            buffer.recordRenderingOp(
                CanvasOp.Draw { writer ->
                    writer.idLookup(localTupleArrayId, 0f.mvf, destId = id1)
                    writer.idLookup(localTupleArrayId, 1f.mvf, destId = id2)
                    writer.idLookup(localTupleArrayId, 2f.mvf, destId = id3)
                }
            )
            action(formal1, formal2, formal3)
        }

        val op =
            buffer.recordRenderingOp(
                CanvasOp.Draw { writer ->
                    val tupleIds = IntArray(size)
                    for (i in 0 until size) {
                        tupleIds[i] =
                            writer
                                .addIdList(
                                    creationState.getId(items1[i]),
                                    creationState.getId(items2[i]),
                                    creationState.getId(items3[i]),
                                )
                                .id
                    }
                    val outerListId = writer.addIdList(tupleIds).id
                    writer.startPatternForEach(outerListId, localTupleId)
                    childSpan.record(writer, creationState)
                    writer.endPatternForEach()
                }
            )
        for (i in items1.indices) buffer.addRoots(op, items1[i])
        for (i in items2.indices) buffer.addRoots(op, items2[i])
        for (i in items3.indices) buffer.addRoots(op, items3[i])
    }

    /**
     * Iterates over [items1], [items2], [items3], and [items4] and records [action] for each
     * 4-tuple.
     *
     * Iteration stops at the length of the shortest list.
     *
     * @param items1 first list of items to iterate over
     * @param items2 second list of items to iterate over
     * @param items3 third list of items to iterate over
     * @param items4 fourth list of items to iterate over
     * @param action drawing commands to record for each 4-tuple
     */
    @Suppress("UNCHECKED_CAST")
    public fun <
        T1 : RemoteValue<*>,
        T2 : RemoteValue<*>,
        T3 : RemoteValue<*>,
        T4 : RemoteValue<*>,
    > forEach(
        items1: List<T1>,
        items2: List<T2>,
        items3: List<T3>,
        items4: List<T4>,
        action: (T1, T2, T3, T4) -> Unit,
    ) {
        val size = minOf(items1.size, items2.size, items3.size, items4.size)
        if (size == 0) return

        val localTupleId = document.allocateId()
        val localTupleArrayId = RemoteArrayId(localTupleId)
        val id1 = document.allocateId()
        val id2 = document.allocateId()
        val id3 = document.allocateId()
        val id4 = document.allocateId()
        val formal1 = generateFormalArg(id1, items1.first()) as T1
        val formal2 = generateFormalArg(id2, items2.first()) as T2
        val formal3 = generateFormalArg(id3, items3.first()) as T3
        val formal4 = generateFormalArg(id4, items4.first()) as T4

        val childSpan = buffer.recordInChildSpan {
            buffer.recordRenderingOp(
                CanvasOp.Draw { writer ->
                    writer.idLookup(localTupleArrayId, 0f.mvf, destId = id1)
                    writer.idLookup(localTupleArrayId, 1f.mvf, destId = id2)
                    writer.idLookup(localTupleArrayId, 2f.mvf, destId = id3)
                    writer.idLookup(localTupleArrayId, 3f.mvf, destId = id4)
                }
            )
            action(formal1, formal2, formal3, formal4)
        }

        val op =
            buffer.recordRenderingOp(
                CanvasOp.Draw { writer ->
                    val tupleIds = IntArray(size)
                    for (i in 0 until size) {
                        tupleIds[i] =
                            writer
                                .addIdList(
                                    creationState.getId(items1[i]),
                                    creationState.getId(items2[i]),
                                    creationState.getId(items3[i]),
                                    creationState.getId(items4[i]),
                                )
                                .id
                    }
                    val outerListId = writer.addIdList(tupleIds).id
                    writer.startPatternForEach(outerListId, localTupleId)
                    childSpan.record(writer, creationState)
                    writer.endPatternForEach()
                }
            )
        for (i in items1.indices) buffer.addRoots(op, items1[i])
        for (i in items2.indices) buffer.addRoots(op, items2[i])
        for (i in items3.indices) buffer.addRoots(op, items3[i])
        for (i in items4.indices) buffer.addRoots(op, items4[i])
    }

    /**
     * Iterates over [items1], [items2], [items3], [items4], and [items5] and records [action] for
     * each 5-tuple.
     *
     * Iteration stops at the length of the shortest list.
     *
     * @param items1 first list of items to iterate over
     * @param items2 second list of items to iterate over
     * @param items3 third list of items to iterate over
     * @param items4 fourth list of items to iterate over
     * @param items5 fifth list of items to iterate over
     * @param action drawing commands to record for each 5-tuple
     */
    @Suppress("UNCHECKED_CAST")
    public fun <
        T1 : RemoteValue<*>,
        T2 : RemoteValue<*>,
        T3 : RemoteValue<*>,
        T4 : RemoteValue<*>,
        T5 : RemoteValue<*>,
    > forEach(
        items1: List<T1>,
        items2: List<T2>,
        items3: List<T3>,
        items4: List<T4>,
        items5: List<T5>,
        action: (T1, T2, T3, T4, T5) -> Unit,
    ) {
        val size = minOf(items1.size, items2.size, items3.size, items4.size, items5.size)
        if (size == 0) return

        val localTupleId = document.allocateId()
        val localTupleArrayId = RemoteArrayId(localTupleId)
        val id1 = document.allocateId()
        val id2 = document.allocateId()
        val id3 = document.allocateId()
        val id4 = document.allocateId()
        val id5 = document.allocateId()
        val formal1 = generateFormalArg(id1, items1.first()) as T1
        val formal2 = generateFormalArg(id2, items2.first()) as T2
        val formal3 = generateFormalArg(id3, items3.first()) as T3
        val formal4 = generateFormalArg(id4, items4.first()) as T4
        val formal5 = generateFormalArg(id5, items5.first()) as T5

        val childSpan = buffer.recordInChildSpan {
            buffer.recordRenderingOp(
                CanvasOp.Draw { writer ->
                    writer.idLookup(localTupleArrayId, 0f.mvf, destId = id1)
                    writer.idLookup(localTupleArrayId, 1f.mvf, destId = id2)
                    writer.idLookup(localTupleArrayId, 2f.mvf, destId = id3)
                    writer.idLookup(localTupleArrayId, 3f.mvf, destId = id4)
                    writer.idLookup(localTupleArrayId, 4f.mvf, destId = id5)
                }
            )
            action(formal1, formal2, formal3, formal4, formal5)
        }

        val op =
            buffer.recordRenderingOp(
                CanvasOp.Draw { writer ->
                    val tupleIds = IntArray(size)
                    for (i in 0 until size) {
                        tupleIds[i] =
                            writer
                                .addIdList(
                                    creationState.getId(items1[i]),
                                    creationState.getId(items2[i]),
                                    creationState.getId(items3[i]),
                                    creationState.getId(items4[i]),
                                    creationState.getId(items5[i]),
                                )
                                .id
                    }
                    val outerListId = writer.addIdList(tupleIds).id
                    writer.startPatternForEach(outerListId, localTupleId)
                    childSpan.record(writer, creationState)
                    writer.endPatternForEach()
                }
            )
        for (i in items1.indices) buffer.addRoots(op, items1[i])
        for (i in items2.indices) buffer.addRoots(op, items2[i])
        for (i in items3.indices) buffer.addRoots(op, items3[i])
        for (i in items4.indices) buffer.addRoots(op, items4[i])
        for (i in items5.indices) buffer.addRoots(op, items5[i])
    }

    /**
     * Iterates over [items1] through [items6] and records [action] for each 6-tuple.
     *
     * Iteration stops at the length of the shortest list.
     *
     * @param items1 first list of items to iterate over
     * @param items2 second list of items to iterate over
     * @param items3 third list of items to iterate over
     * @param items4 fourth list of items to iterate over
     * @param items5 fifth list of items to iterate over
     * @param items6 sixth list of items to iterate over
     * @param action drawing commands to record for each 6-tuple
     */
    @Suppress("UNCHECKED_CAST")
    public fun <
        T1 : RemoteValue<*>,
        T2 : RemoteValue<*>,
        T3 : RemoteValue<*>,
        T4 : RemoteValue<*>,
        T5 : RemoteValue<*>,
        T6 : RemoteValue<*>,
    > forEach(
        items1: List<T1>,
        items2: List<T2>,
        items3: List<T3>,
        items4: List<T4>,
        items5: List<T5>,
        items6: List<T6>,
        action: (T1, T2, T3, T4, T5, T6) -> Unit,
    ) {
        val size =
            minOf(items1.size, items2.size, items3.size, items4.size, items5.size, items6.size)
        if (size == 0) return

        val localTupleId = document.allocateId()
        val localTupleArrayId = RemoteArrayId(localTupleId)
        val id1 = document.allocateId()
        val id2 = document.allocateId()
        val id3 = document.allocateId()
        val id4 = document.allocateId()
        val id5 = document.allocateId()
        val id6 = document.allocateId()
        val formal1 = generateFormalArg(id1, items1.first()) as T1
        val formal2 = generateFormalArg(id2, items2.first()) as T2
        val formal3 = generateFormalArg(id3, items3.first()) as T3
        val formal4 = generateFormalArg(id4, items4.first()) as T4
        val formal5 = generateFormalArg(id5, items5.first()) as T5
        val formal6 = generateFormalArg(id6, items6.first()) as T6

        val childSpan = buffer.recordInChildSpan {
            buffer.recordRenderingOp(
                CanvasOp.Draw { writer ->
                    writer.idLookup(localTupleArrayId, 0f.mvf, destId = id1)
                    writer.idLookup(localTupleArrayId, 1f.mvf, destId = id2)
                    writer.idLookup(localTupleArrayId, 2f.mvf, destId = id3)
                    writer.idLookup(localTupleArrayId, 3f.mvf, destId = id4)
                    writer.idLookup(localTupleArrayId, 4f.mvf, destId = id5)
                    writer.idLookup(localTupleArrayId, 5f.mvf, destId = id6)
                }
            )
            action(formal1, formal2, formal3, formal4, formal5, formal6)
        }

        val op =
            buffer.recordRenderingOp(
                CanvasOp.Draw { writer ->
                    val tupleIds = IntArray(size)
                    for (i in 0 until size) {
                        tupleIds[i] =
                            writer
                                .addIdList(
                                    creationState.getId(items1[i]),
                                    creationState.getId(items2[i]),
                                    creationState.getId(items3[i]),
                                    creationState.getId(items4[i]),
                                    creationState.getId(items5[i]),
                                    creationState.getId(items6[i]),
                                )
                                .id
                    }
                    val outerListId = writer.addIdList(tupleIds).id
                    writer.startPatternForEach(outerListId, localTupleId)
                    childSpan.record(writer, creationState)
                    writer.endPatternForEach()
                }
            )
        for (i in items1.indices) buffer.addRoots(op, items1[i])
        for (i in items2.indices) buffer.addRoots(op, items2[i])
        for (i in items3.indices) buffer.addRoots(op, items3[i])
        for (i in items4.indices) buffer.addRoots(op, items4[i])
        for (i in items5.indices) buffer.addRoots(op, items5[i])
        for (i in items6.indices) buffer.addRoots(op, items6[i])
    }

    /** Starts a state layout. */
    public fun startStateLayout(
        modifier: RemoteModifier = RemoteModifier,
        currentStateId: Int,
    ) {
        val writeModifiers = creationState.prepareModifiers(modifier)
        recordRenderingOp {
            document.startStateLayout(currentStateId, componentId = writeModifiers.componentId) {
                writeModifiers(document)
            }
        }
    }

    /** Ends a state layout. */
    public fun endStateLayout() {
        recordRenderingOp { document.endStateLayout() }
    }
}

/**
 * Reusable drawing pattern with no parameters.
 *
 * Call to inflate the recorded template into the canvas stream.
 */
@RestrictTo(RestrictTo.Scope.LIBRARY_GROUP)
public fun interface RemotePattern0 : (RemoteModifier?) -> Unit {
    public operator fun invoke(): Unit = invoke(null)

    public override operator fun invoke(modifier: RemoteModifier?)
}

/**
 * Reusable drawing pattern with 1 parameter.
 *
 * Call to inflate the recorded template with the provided argument.
 */
@RestrictTo(RestrictTo.Scope.LIBRARY_GROUP)
public fun interface RemotePattern1<T1 : RemoteValue<*>> : (T1, RemoteModifier?) -> Unit {
    public operator fun invoke(arg1: T1): Unit = invoke(arg1, null)

    public override operator fun invoke(arg1: T1, modifier: RemoteModifier?)
}

/**
 * Reusable drawing pattern with 2 parameters.
 *
 * Call to inflate the recorded template with the provided arguments.
 */
@RestrictTo(RestrictTo.Scope.LIBRARY_GROUP)
public fun interface RemotePattern2<T1 : RemoteValue<*>, T2 : RemoteValue<*>> :
    (T1, T2, RemoteModifier?) -> Unit {
    public operator fun invoke(arg1: T1, arg2: T2): Unit = invoke(arg1, arg2, null)

    public override operator fun invoke(arg1: T1, arg2: T2, modifier: RemoteModifier?)
}

/**
 * Reusable drawing pattern with 3 parameters.
 *
 * Call to inflate the recorded template with the provided arguments.
 */
@RestrictTo(RestrictTo.Scope.LIBRARY_GROUP)
public fun interface RemotePattern3<
    T1 : RemoteValue<*>,
    T2 : RemoteValue<*>,
    T3 : RemoteValue<*>,
> : (T1, T2, T3, RemoteModifier?) -> Unit {
    public operator fun invoke(arg1: T1, arg2: T2, arg3: T3): Unit = invoke(arg1, arg2, arg3, null)

    public override operator fun invoke(arg1: T1, arg2: T2, arg3: T3, modifier: RemoteModifier?)
}

/**
 * Reusable drawing pattern with 4 parameters.
 *
 * Call to inflate the recorded template with the provided arguments.
 */
@RestrictTo(RestrictTo.Scope.LIBRARY_GROUP)
public fun interface RemotePattern4<
    T1 : RemoteValue<*>,
    T2 : RemoteValue<*>,
    T3 : RemoteValue<*>,
    T4 : RemoteValue<*>,
> : (T1, T2, T3, T4, RemoteModifier?) -> Unit {
    public operator fun invoke(arg1: T1, arg2: T2, arg3: T3, arg4: T4): Unit =
        invoke(arg1, arg2, arg3, arg4, null)

    public override operator fun invoke(
        arg1: T1,
        arg2: T2,
        arg3: T3,
        arg4: T4,
        modifier: RemoteModifier?,
    )
}

/**
 * Reusable drawing pattern with 5 parameters.
 *
 * Call to inflate the recorded template with the provided arguments.
 */
@RestrictTo(RestrictTo.Scope.LIBRARY_GROUP)
public fun interface RemotePattern5<
    T1 : RemoteValue<*>,
    T2 : RemoteValue<*>,
    T3 : RemoteValue<*>,
    T4 : RemoteValue<*>,
    T5 : RemoteValue<*>,
> : (T1, T2, T3, T4, T5, RemoteModifier?) -> Unit {
    public operator fun invoke(arg1: T1, arg2: T2, arg3: T3, arg4: T4, arg5: T5): Unit =
        invoke(arg1, arg2, arg3, arg4, arg5, null)

    public override operator fun invoke(
        arg1: T1,
        arg2: T2,
        arg3: T3,
        arg4: T4,
        arg5: T5,
        modifier: RemoteModifier?,
    )
}

/**
 * Reusable drawing pattern with 6 parameters.
 *
 * Call to inflate the recorded template with the provided arguments.
 */
@RestrictTo(RestrictTo.Scope.LIBRARY_GROUP)
public fun interface RemotePattern6<
    T1 : RemoteValue<*>,
    T2 : RemoteValue<*>,
    T3 : RemoteValue<*>,
    T4 : RemoteValue<*>,
    T5 : RemoteValue<*>,
    T6 : RemoteValue<*>,
> : (T1, T2, T3, T4, T5, T6, RemoteModifier?) -> Unit {
    public operator fun invoke(arg1: T1, arg2: T2, arg3: T3, arg4: T4, arg5: T5, arg6: T6): Unit =
        invoke(arg1, arg2, arg3, arg4, arg5, arg6, null)

    public override operator fun invoke(
        arg1: T1,
        arg2: T2,
        arg3: T3,
        arg4: T4,
        arg5: T5,
        arg6: T6,
        modifier: RemoteModifier?,
    )
}
