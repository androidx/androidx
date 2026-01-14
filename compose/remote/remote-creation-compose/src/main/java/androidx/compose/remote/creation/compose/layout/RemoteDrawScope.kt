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
import androidx.compose.remote.creation.compose.shaders.RemoteSolidColor
import androidx.compose.remote.creation.compose.state.RemoteBitmap
import androidx.compose.remote.creation.compose.state.RemoteFloat
import androidx.compose.remote.creation.compose.state.RemotePaint
import androidx.compose.remote.creation.compose.state.RemoteString
import androidx.compose.remote.creation.compose.state.rf
import androidx.compose.ui.graphics.asAndroidPath
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.unit.LayoutDirection

/**
 * A remote-compatible drawing scope for RemoteCompose. Unlike [DrawScope], this class uses remote
 * types consistently and does not attempt to implement the standard [DrawScope] interface to avoid
 * API incompatibilities.
 */
@RestrictTo(RestrictTo.Scope.LIBRARY_GROUP)
public class RemoteDrawScope(
    public val remoteCanvas: RemoteCanvas,
    public val fontScale: RemoteFloat,
    public val layoutDirection: LayoutDirection,
) {
    public val remoteComposeCreationState: RemoteComposeCreationState
        get() = remoteCanvas.creationState

    /** The width of the drawing area as a [RemoteFloat]. */
    public val remoteWidth: RemoteFloat
        get() = remoteCanvas.componentWidth

    /** The height of the drawing area as a [RemoteFloat]. */
    public val remoteHeight: RemoteFloat
        get() = remoteCanvas.componentHeight

    /** The center of the drawing area as a [RemoteOffset]. */
    public val remoteCenter: RemoteOffset
        get() = RemoteOffset(remoteWidth / 2f, remoteHeight / 2f)

    /** The size of the drawing area as a [RemoteSize]. */
    public val remoteSize: RemoteSize
        get() = RemoteSize(remoteWidth, remoteHeight)

    public fun usePaint(paint: RemotePaint, block: () -> Unit) {
        remoteCanvas.internalCanvas.usePaint(paint)
        block()
    }

    private fun resolveRemoteBrush(paint: RemotePaint, size: RemoteSize) {
        paint.remoteBrush?.let { brush ->
            if (brush.hasShader) {
                paint.shader = brush.createShader(size)
            } else if (brush is RemoteSolidColor) {
                paint.remoteColor = brush.color
            }
        }
    }

    /** Draws a rectangle. */
    public fun drawRect(
        paint: RemotePaint,
        topLeft: RemoteOffset = RemoteOffset.Zero,
        size: RemoteSize = remoteSize,
    ) {
        resolveRemoteBrush(paint, size)
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
        resolveRemoteBrush(paint, size)
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

    /** Draws an image. */
    public fun drawImage(
        image: RemoteBitmap,
        topLeft: RemoteOffset = RemoteOffset.Zero,
        paint: RemotePaint = RemotePaint(),
    ) {
        resolveRemoteBrush(paint, RemoteSize(image.width, image.height))
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
            scaleType.toRemoteCompose(),
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

    /** Performs a rotation. */
    public fun rotate(degrees: RemoteFloat, block: RemoteDrawScope.() -> Unit) {
        withTransform({ rotate(degrees) }, block)
    }

    /** Performs a translation. */
    public fun translate(left: RemoteFloat, top: RemoteFloat, block: RemoteDrawScope.() -> Unit) {
        withTransform({ translate(left, top) }, block)
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
}
