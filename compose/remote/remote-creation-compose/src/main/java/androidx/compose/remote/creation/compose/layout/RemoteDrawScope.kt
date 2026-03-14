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
import androidx.compose.remote.core.operations.DrawTextOnCircle.Alignment
import androidx.compose.remote.core.operations.DrawTextOnCircle.Placement
import androidx.compose.remote.creation.RemotePath
import androidx.compose.remote.creation.compose.capture.RemoteComposeCreationState
import androidx.compose.remote.creation.compose.state.RemoteBitmap
import androidx.compose.remote.creation.compose.state.RemoteBoolean
import androidx.compose.remote.creation.compose.state.RemoteFloat
import androidx.compose.remote.creation.compose.state.RemotePaint
import androidx.compose.remote.creation.compose.state.RemoteStateScope
import androidx.compose.remote.creation.compose.state.RemoteString
import androidx.compose.remote.creation.compose.state.creationState
import androidx.compose.remote.creation.compose.state.rf
import androidx.compose.ui.graphics.ClipOp
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.layout.ContentScale
import androidx.graphics.shapes.RoundedPolygon

/**
 * A remote-compatible drawing scope for RemoteCompose. Unlike [DrawScope], this class uses remote
 * types consistently and does not attempt to implement the standard [DrawScope] interface to avoid
 * API incompatibilities.
 */
public open class RemoteDrawScope
internal constructor(
    @get:RestrictTo(RestrictTo.Scope.LIBRARY_GROUP) public val remoteCanvas: RemoteCanvas
) : RemoteStateScope by remoteCanvas {
    @get:RestrictTo(RestrictTo.Scope.LIBRARY_GROUP)
    public val remoteComposeCreationState: RemoteComposeCreationState
        get() = remoteCanvas.creationState

    /** The width of the drawing area as a [RemoteFloat]. */
    public val width: RemoteFloat
        get() = remoteCanvas.remote.component.width

    /** The height of the drawing area as a [RemoteFloat]. */
    public val height: RemoteFloat
        get() = remoteCanvas.remote.component.height

    /** The center of the drawing area as a [RemoteOffset]. */
    public val center: RemoteOffset
        get() = RemoteOffset(width / 2f, height / 2f)

    /** The size of the drawing area as a [RemoteSize]. */
    public val size: RemoteSize
        get() = RemoteSize(width, height)

    public fun usePaint(paint: RemotePaint, block: () -> Unit) {
        remoteCanvas.usePaint(paint)
        block()
    }

    public fun drawRect(
        paint: RemotePaint?,
        topLeft: RemoteOffset = RemoteOffset.Zero,
        size: RemoteSize = this@RemoteDrawScope.size,
    ) {
        remoteCanvas.drawRect(
            topLeft.x,
            topLeft.y,
            topLeft.x + size.width,
            topLeft.y + size.height,
            paint,
        )
    }

    public fun drawRoundRect(
        paint: RemotePaint?,
        topLeft: RemoteOffset = RemoteOffset.Zero,
        size: RemoteSize = this@RemoteDrawScope.size,
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
    @RestrictTo(RestrictTo.Scope.LIBRARY_GROUP)
    public fun drawCircle(
        paint: RemotePaint?,
        center: RemoteOffset = this@RemoteDrawScope.center,
        radius: RemoteFloat,
    ) {
        RemoteSize(radius * 2f, radius * 2f)
        remoteCanvas.drawCircle(center.x, center.y, radius, paint)
    }

    @RestrictTo(RestrictTo.Scope.LIBRARY_GROUP)
    public fun drawOval(
        paint: RemotePaint?,
        topLeft: RemoteOffset = RemoteOffset.Zero,
        size: RemoteSize = this@RemoteDrawScope.size,
    ) {
        remoteCanvas.drawOval(
            topLeft.x,
            topLeft.y,
            topLeft.x + size.width,
            topLeft.y + size.height,
            paint,
        )
    }

    public fun drawArc(
        paint: RemotePaint?,
        startAngle: RemoteFloat,
        sweepAngle: RemoteFloat,
        useCenter: Boolean,
        topLeft: RemoteOffset = RemoteOffset.Zero,
        size: RemoteSize = this@RemoteDrawScope.size,
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
    @RestrictTo(RestrictTo.Scope.LIBRARY_GROUP)
    public fun drawLine(paint: RemotePaint?, start: RemoteOffset, end: RemoteOffset) {
        remoteCanvas.drawLine(start.x, start.y, end.x, end.y, paint)
    }

    @RestrictTo(RestrictTo.Scope.LIBRARY_GROUP)
    public fun drawImage(
        image: RemoteBitmap,
        topLeft: RemoteOffset = RemoteOffset.Zero,
        paint: RemotePaint? = RemotePaint(),
    ) {
        RemoteSize(image.width, image.height)
        remoteCanvas.drawBitmap(image, topLeft.x, topLeft.y, paint)
    }

    /** Draws a bitmap scaled to the destination rectangle. */
    @RestrictTo(RestrictTo.Scope.LIBRARY_GROUP)
    public fun drawScaledBitmap(
        image: RemoteBitmap,
        srcOffset: RemoteOffset = RemoteOffset.Zero,
        srcSize: RemoteSize = RemoteSize(image.width, image.height),
        dstOffset: RemoteOffset = RemoteOffset.Zero,
        dstSize: RemoteSize = size,
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
    @RestrictTo(RestrictTo.Scope.LIBRARY_GROUP)
    public fun drawPath(path: RemotePath, paint: RemotePaint?) {
        remoteCanvas.drawPath(path, paint)
    }

    /** Draws a rounded polygon. */
    @RestrictTo(RestrictTo.Scope.LIBRARY_GROUP)
    public fun drawRoundedPolygon(roundedPolygon: RoundedPolygon, paint: RemotePaint?) {
        remoteCanvas.drawRoundedPolygon(roundedPolygon, paint)
    }

    /** Draws a morph between two rounded polygons. */
    @RestrictTo(RestrictTo.Scope.LIBRARY_GROUP)
    public fun drawRoundedPolygonMorph(
        from: RoundedPolygon,
        to: RoundedPolygon,
        progress: RemoteFloat,
        paint: RemotePaint?,
    ) {
        remoteCanvas.drawRoundedPolygonMorph(from, to, progress, paint)
    }

    /** Draws a tween path. */
    @RestrictTo(RestrictTo.Scope.LIBRARY_GROUP)
    public fun drawTweenPath(
        path1: RemotePath,
        path2: RemotePath,
        tween: RemoteFloat,
        start: RemoteFloat = 0f.rf,
        stop: RemoteFloat = 1f.rf,
        paint: RemotePaint?,
    ) {
        remoteCanvas.drawTweenPath(path1, path2, tween, start, stop, paint)
    }

    /** Draws text. */
    @RestrictTo(RestrictTo.Scope.LIBRARY_GROUP)
    public fun drawText(text: RemoteString, x: RemoteFloat, y: RemoteFloat, paint: RemotePaint?) {
        remoteCanvas.drawText(text, x, y, paint)
    }

    @RestrictTo(RestrictTo.Scope.LIBRARY_GROUP)
    public fun drawAnchoredText(
        text: RemoteString,
        anchorX: RemoteFloat,
        anchorY: RemoteFloat,
        panX: RemoteFloat = 0f.rf,
        panY: RemoteFloat = 0f.rf,
        flags: Int = 0,
        paint: RemotePaint?,
    ) {
        remoteCanvas.drawAnchoredText(text, anchorX, anchorY, panX, panY, flags, paint)
    }

    @RestrictTo(RestrictTo.Scope.LIBRARY_GROUP)
    public fun drawTextOnPath(
        text: RemoteString,
        path: RemotePath,
        hOffset: RemoteFloat = 0f.rf,
        vOffset: RemoteFloat = 0f.rf,
        paint: RemotePaint?,
    ) {
        remoteCanvas.drawTextOnPath(text, path, hOffset, vOffset, paint)
    }

    /** Performs a rotation. */
    @RestrictTo(RestrictTo.Scope.LIBRARY_GROUP)
    public fun rotate(degrees: RemoteFloat, block: RemoteDrawScope.() -> Unit) {
        withTransform({ rotate(degrees) }, block)
    }

    /** Performs a rotation. */
    @RestrictTo(RestrictTo.Scope.LIBRARY_GROUP)
    public fun rotate(
        degrees: RemoteFloat,
        pivot: RemoteOffset,
        block: RemoteDrawScope.() -> Unit,
    ) {
        withTransform({ rotate(degrees, pivot) }, block)
    }

    /** Performs a translation. */
    @RestrictTo(RestrictTo.Scope.LIBRARY_GROUP)
    public fun translate(left: RemoteFloat, top: RemoteFloat, block: RemoteDrawScope.() -> Unit) {
        withTransform({ translate(left, top) }, block)
    }

    /** Performs a scaling. */
    @RestrictTo(RestrictTo.Scope.LIBRARY_GROUP)
    public fun scale(scale: RemoteFloat, block: RemoteDrawScope.() -> Unit) {
        withTransform({ scale(scale, scale) }, block)
    }

    /** Performs a scaling. */
    @RestrictTo(RestrictTo.Scope.LIBRARY_GROUP)
    public fun scale(scaleX: RemoteFloat, scaleY: RemoteFloat, block: RemoteDrawScope.() -> Unit) {
        withTransform({ scale(scaleX, scaleY) }, block)
    }

    /** Performs a scaling. */
    @RestrictTo(RestrictTo.Scope.LIBRARY_GROUP)
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
    @RestrictTo(RestrictTo.Scope.LIBRARY_GROUP)
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
     * @param warpRadiusOffset the offset of the text from the circle.
     * @param paint The [RemotePaint] to use for drawing.
     */
    public fun drawTextOnCircle(
        text: RemoteString,
        centerX: RemoteFloat,
        centerY: RemoteFloat,
        radius: RemoteFloat,
        startAngle: RemoteFloat,
        warpRadiusOffset: RemoteFloat,
        paint: RemotePaint? = null,
    ) {
        remoteCanvas.drawTextOnCircle(
            text,
            centerX,
            centerY,
            radius,
            startAngle,
            warpRadiusOffset,
            Alignment.CENTER,
            Placement.OUTSIDE,
            paint,
        )
    }

    /** Clips the drawing area to the specified rectangle and executes [block] within it. */
    @RestrictTo(RestrictTo.Scope.LIBRARY_GROUP)
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
    @RestrictTo(RestrictTo.Scope.LIBRARY_GROUP)
    public fun clipPath(
        path: RemotePath,
        clipOp: ClipOp = ClipOp.Intersect,
        block: RemoteDrawScope.() -> Unit,
    ) {
        withTransform({ clipPath(path, clipOp) }, block)
    }

    /** Executes [body] if [condition] evaluates to true. */
    @RestrictTo(RestrictTo.Scope.LIBRARY_GROUP)
    public fun drawConditionally(condition: RemoteBoolean, body: RemoteDrawScope.() -> Unit) {
        remoteCanvas.drawConditionally(condition) { this.body() }
    }

    /** Draws into an offscreen bitmap and executes [body]. */
    @RestrictTo(RestrictTo.Scope.LIBRARY_GROUP)
    public fun drawToOffscreenBitmap(bitmap: RemoteBitmap, body: RemoteDrawScope.() -> Unit) {
        remoteCanvas.drawToOffscreenBitmap(bitmap) { this.body() }
    }

    /** Executes [body] commands in a loop. */
    @RestrictTo(RestrictTo.Scope.LIBRARY_GROUP)
    public fun loop(
        from: RemoteFloat,
        until: RemoteFloat,
        step: RemoteFloat,
        body: RemoteDrawScope.(index: RemoteFloat) -> Unit,
    ) {
        remoteCanvas.loop(from, until, step) { index -> this.body(index) }
    }

    /** Access to remote-specific utilities like time and animations. */
    @get:RestrictTo(RestrictTo.Scope.LIBRARY_GROUP)
    public val remote: RemoteAccess = RemoteAccess(this)
}
