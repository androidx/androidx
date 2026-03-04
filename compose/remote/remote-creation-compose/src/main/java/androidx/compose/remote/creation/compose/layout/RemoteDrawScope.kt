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
import androidx.compose.remote.core.operations.DrawTextOnCircle
import androidx.compose.remote.creation.RemotePath
import androidx.compose.remote.creation.compose.capture.RemoteComposeCreationState
import androidx.compose.remote.creation.compose.state.RemoteBitmap
import androidx.compose.remote.creation.compose.state.RemoteBoolean
import androidx.compose.remote.creation.compose.state.RemoteFloat
import androidx.compose.remote.creation.compose.state.RemotePaint
import androidx.compose.remote.creation.compose.state.RemoteStateScope
import androidx.compose.remote.creation.compose.state.RemoteString
import androidx.compose.remote.creation.compose.state.rf
import androidx.compose.ui.graphics.ClipOp
import androidx.compose.ui.graphics.asAndroidPath
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.layout.ContentScale
import androidx.graphics.shapes.RoundedPolygon

/**
 * A remote-compatible drawing scope for RemoteCompose. Unlike [DrawScope], this class uses remote
 * types consistently and does not attempt to implement the standard [DrawScope] interface to avoid
 * API incompatibilities.
 */
@RestrictTo(RestrictTo.Scope.LIBRARY_GROUP)
public open class RemoteDrawScope internal constructor(public val remoteCanvas: RemoteCanvas) :
    RemoteStateScope by remoteCanvas {
    public val remoteComposeCreationState: RemoteComposeCreationState
        get() = remoteCanvas.creationState

    /** The width of the drawing area as a [RemoteFloat]. */
    public val remoteWidth: RemoteFloat
        get() = remoteCanvas.remote.component.width

    /** The height of the drawing area as a [RemoteFloat]. */
    public val remoteHeight: RemoteFloat
        get() = remoteCanvas.remote.component.height

    /** The center of the drawing area as a [RemoteOffset]. */
    public val remoteCenter: RemoteOffset
        get() = RemoteOffset(remoteWidth / 2f, remoteHeight / 2f)

    /** The x-coordinate of the center of the drawing area. */
    public val centerX: RemoteFloat
        get() = remoteWidth / 2f

    /** The y-coordinate of the center of the drawing area. */
    public val centerY: RemoteFloat
        get() = remoteHeight / 2f

    /** The size of the drawing area as a [RemoteSize]. */
    public val remoteSize: RemoteSize
        get() = RemoteSize(remoteWidth, remoteHeight)

    public fun usePaint(paint: RemotePaint, block: () -> Unit) {
        remoteCanvas.internalCanvas.usePaint(paint)
        block()
    }

    /** Draws a rectangle. */
    public fun drawRect(
        paint: RemotePaint,
        topLeft: RemoteOffset = RemoteOffset.Zero,
        size: RemoteSize = remoteSize,
    ) {
        remoteCanvas.drawRect(
            topLeft.x,
            topLeft.y,
            topLeft.x + size.width,
            topLeft.y + size.height,
            paint,
        )
    }

    /** Draws a rounded rectangle. */
    public fun drawRoundRect(
        paint: RemotePaint,
        topLeft: RemoteOffset = RemoteOffset.Zero,
        size: RemoteSize = remoteSize,
        cornerRadius: RemoteOffset = RemoteOffset.Zero,
    ) {
        remoteCanvas.drawRoundRect(
            topLeft.x,
            topLeft.y,
            topLeft.x + size.width,
            topLeft.y + size.height,
            cornerRadius.x,
            cornerRadius.y,
            paint,
        )
    }

    /** Draws a circle. */
    public fun drawCircle(
        paint: RemotePaint,
        center: RemoteOffset = remoteCenter,
        radius: RemoteFloat,
    ) {
        RemoteSize(radius * 2f, radius * 2f)
        remoteCanvas.drawCircle(center.x, center.y, radius, paint)
    }

    /** Draws an oval. */
    public fun drawOval(
        paint: RemotePaint,
        topLeft: RemoteOffset = RemoteOffset.Zero,
        size: RemoteSize = remoteSize,
    ) {
        remoteCanvas.drawOval(
            topLeft.x,
            topLeft.y,
            topLeft.x + size.width,
            topLeft.y + size.height,
            paint,
        )
    }

    /**
     * Draws an arc.
     *
     * @param useCenter If true, include the center of the oval in the arc, which creates a sector.
     */
    public fun drawArc(
        paint: RemotePaint,
        startAngle: RemoteFloat,
        sweepAngle: RemoteFloat,
        useCenter: Boolean,
        topLeft: RemoteOffset = RemoteOffset.Zero,
        size: RemoteSize = remoteSize,
    ) {
        remoteCanvas.drawArc(
            topLeft.x,
            topLeft.y,
            topLeft.x + size.width,
            topLeft.y + size.height,
            startAngle,
            sweepAngle,
            useCenter,
            paint,
        )
    }

    /** Draws a line. */
    public fun drawLine(paint: RemotePaint, start: RemoteOffset, end: RemoteOffset) {
        remoteCanvas.drawLine(start.x, start.y, end.x, end.y, paint)
    }

    /** Draws an image. */
    public fun drawImage(
        image: RemoteBitmap,
        topLeft: RemoteOffset = RemoteOffset.Zero,
        paint: RemotePaint = RemotePaint(),
    ) {
        RemoteSize(image.width, image.height)
        remoteCanvas.drawBitmap(image, topLeft.x, topLeft.y, paint)
    }

    /** Draws a bitmap scaled to the destination rectangle. */
    public fun drawScaledBitmap(
        image: RemoteBitmap,
        srcOffset: RemoteOffset = RemoteOffset.Zero,
        srcSize: RemoteSize = RemoteSize(image.width, image.height),
        dstOffset: RemoteOffset = RemoteOffset.Zero,
        dstSize: RemoteSize = remoteSize,
        scaleType: ContentScale = ContentScale.Fit,
        scaleFactor: RemoteFloat = 1f.rf,
        contentDescription: String? = null,
    ) {
        remoteCanvas.drawScaledBitmap(
            image,
            srcOffset.x,
            srcOffset.y,
            srcOffset.x + srcSize.width,
            srcOffset.y + srcSize.height,
            dstOffset.x,
            dstOffset.y,
            dstOffset.x + dstSize.width,
            dstOffset.y + dstSize.height,
            scaleType.toImageScalingInt(),
            scaleFactor,
            contentDescription,
        )
    }

    /** Draws a path. */
    public fun drawPath(path: RemotePath, paint: RemotePaint) {
        remoteCanvas.drawPath(path, paint)
    }

    /** Draws a path. */
    public fun drawPath(path: androidx.compose.ui.graphics.Path, paint: RemotePaint) {
        remoteCanvas.drawPath(path.asAndroidPath(), paint)
    }

    /** Draws a rounded polygon. */
    public fun drawRoundedPolygon(
        roundedPolygon: RoundedPolygon,
        paint: androidx.compose.ui.graphics.Paint,
    ) {
        remoteCanvas.drawRoundedPolygon(roundedPolygon, paint)
    }

    /** Draws a morph between two rounded polygons. */
    public fun drawRoundedPolygonMorph(
        from: RoundedPolygon,
        to: RoundedPolygon,
        progress: RemoteFloat,
        paint: androidx.compose.ui.graphics.Paint,
    ) {
        remoteCanvas.drawRoundedPolygonMorph(from, to, progress, paint)
    }

    public fun drawTweenPath(
        path1: androidx.compose.ui.graphics.Path,
        path2: androidx.compose.ui.graphics.Path,
        tween: RemoteFloat,
        start: RemoteFloat = 0f.rf,
        stop: RemoteFloat = 1f.rf,
        paint: androidx.compose.ui.graphics.Paint,
    ) {
        remoteCanvas.drawTweenPath(path1, path2, tween, start, stop, paint)
    }

    /** Draws a tween path. */
    public fun drawTweenPath(
        path1: RemotePath,
        path2: RemotePath,
        tween: RemoteFloat,
        start: RemoteFloat = 0f.rf,
        stop: RemoteFloat = 1f.rf,
        paint: androidx.compose.ui.graphics.Paint,
    ) {
        remoteCanvas.drawTweenPath(path1, path2, tween, start, stop, paint)
    }

    /** Draws text. */
    public fun drawText(text: RemoteString, x: RemoteFloat, y: RemoteFloat, paint: RemotePaint) {
        remoteCanvas.drawText(text, x, y, paint)
    }

    /** Draws anchored text. */
    public fun drawAnchoredText(
        text: RemoteString,
        anchorX: RemoteFloat,
        anchorY: RemoteFloat,
        panX: RemoteFloat = 0f.rf,
        panY: RemoteFloat = 0f.rf,
        flags: Int = 0,
        paint: RemotePaint,
    ) {
        remoteCanvas.drawAnchoredText(text, anchorX, anchorY, panX, panY, flags, paint)
    }

    /** Draws text along a path. */
    public fun drawTextOnPath(
        text: RemoteString,
        path: androidx.compose.ui.graphics.Path,
        hOffset: RemoteFloat = 0f.rf,
        vOffset: RemoteFloat = 0f.rf,
        paint: RemotePaint,
    ) {
        remoteCanvas.drawTextOnPath(text, path.asAndroidPath(), hOffset, vOffset, paint)
    }

    /** Performs a rotation. */
    public fun rotate(degrees: RemoteFloat, block: RemoteDrawScope.() -> Unit) {
        withTransform({ rotate(degrees) }, block)
    }

    /** Performs a rotation. */
    public fun rotate(
        degrees: RemoteFloat,
        pivot: RemoteOffset,
        block: RemoteDrawScope.() -> Unit,
    ) {
        withTransform({ rotate(degrees, pivot) }, block)
    }

    /** Performs a translation. */
    public fun translate(left: RemoteFloat, top: RemoteFloat, block: RemoteDrawScope.() -> Unit) {
        withTransform({ translate(left, top) }, block)
    }

    /** Performs a scaling. */
    public fun scale(scale: RemoteFloat, block: RemoteDrawScope.() -> Unit) {
        withTransform({ scale(scale, scale) }, block)
    }

    /** Performs a scaling. */
    public fun scale(scaleX: RemoteFloat, scaleY: RemoteFloat, block: RemoteDrawScope.() -> Unit) {
        withTransform({ scale(scaleX, scaleY) }, block)
    }

    /** Performs a scaling. */
    public fun scale(
        scaleX: RemoteFloat,
        scaleY: RemoteFloat,
        pivot: RemoteOffset,
        block: RemoteDrawScope.() -> Unit,
    ) {
        withTransform({ scale(scaleX, scaleY, pivot) }, block)
    }

    /**
     * Executes [drawBlock] with the specified transformation applied.
     *
     * @param transformBlock The block defining the transformations to apply.
     * @param drawBlock The block containing drawing operations to execute with the transformations
     *   applied.
     */
    public fun withTransform(
        transformBlock: RemoteCanvas.() -> Unit,
        drawBlock: RemoteDrawScope.() -> Unit,
    ) {
        remoteCanvas.save()
        remoteCanvas.transformBlock()
        this.drawBlock()
        remoteCanvas.restore()
    }

    /**
     * Draws text along a circle.
     *
     * @param text The text to draw.
     * @param centerX The x-coordinate of the circle's center.
     * @param centerY The y-coordinate of the circle's center.
     * @param radius The radius of the circle.
     * @param startAngle The starting angle for the text.
     * @param paint The [RemotePaint] to use for drawing.
     */
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
        remoteCanvas.drawTextOnCircle(
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

    /** Clips the drawing area to the specified rectangle and executes [block] within it. */
    public fun clipRect(
        left: RemoteFloat,
        top: RemoteFloat,
        right: RemoteFloat,
        bottom: RemoteFloat,
        clipOp: ClipOp = ClipOp.Intersect,
        block: RemoteDrawScope.() -> Unit,
    ) {
        withTransform({ clipRect(left, top, right, bottom, clipOp) }, block)
    }

    /** Clips the drawing area to the specified [path] and executes [block] within it. */
    public fun clipPath(
        path: RemotePath,
        clipOp: ClipOp = ClipOp.Intersect,
        block: RemoteDrawScope.() -> Unit,
    ) {
        withTransform({ clipPath(path, clipOp) }, block)
    }

    /** Executes [body] if [condition] evaluates to true. */
    public fun drawConditionally(condition: RemoteBoolean, body: RemoteDrawScope.() -> Unit) {
        remoteCanvas.drawConditionally(condition) { this.body() }
    }

    /** Draws into an offscreen bitmap and executes [body]. */
    public fun drawToOffscreenBitmap(bitmap: RemoteBitmap, body: RemoteDrawScope.() -> Unit) {
        remoteCanvas.drawToOffscreenBitmap(bitmap) { this.body() }
    }

    /** Executes [body] commands in a loop. */
    public fun loop(
        from: RemoteFloat,
        until: RemoteFloat,
        step: RemoteFloat,
        body: RemoteDrawScope.(index: RemoteFloat) -> Unit,
    ) {
        remoteCanvas.loop(from, until, step) { index -> this.body(index) }
    }

    /** Access to remote-specific utilities like time and animations. */
    public val remote: RemoteAccess = RemoteAccess(this)
}
