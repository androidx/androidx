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

package androidx.wear.compose.remote.material3

import androidx.compose.remote.creation.compose.layout.RemoteDrawScope
import androidx.compose.remote.creation.compose.layout.RemoteSize
import androidx.compose.remote.creation.compose.painter.RemotePainter
import androidx.compose.remote.creation.compose.shaders.RemoteBrush
import androidx.compose.remote.creation.compose.shaders.image
import androidx.compose.remote.creation.compose.shapes.RemoteShape
import androidx.compose.remote.creation.compose.state.RemoteFloat
import androidx.compose.remote.creation.compose.state.RemoteImageBitmap
import androidx.compose.remote.creation.compose.state.RemotePaint
import androidx.compose.remote.creation.compose.state.rc
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale

internal fun remoteContainerPainter(
    image: RemoteImageBitmap,
    alpha: RemoteFloat,
    shape: RemoteShape,
    contentScale: ContentScale,
    scrim: RemoteBrush? = null,
): RemotePainter =
    DefaultRemoteContainerPainter(
        ShapedBitmapPainter(shape, image, contentScale),
        scrim,
        alpha,
        shape,
    )

private class DefaultRemoteContainerPainter(
    private val painter: RemotePainter,
    private val scrim: RemoteBrush?,
    private val alpha: RemoteFloat,
    private val shape: RemoteShape,
    override val intrinsicSize: RemoteSize? = painter.intrinsicSize,
) : RemotePainter() {
    override fun RemoteDrawScope.onDraw() {
        // Draw Shaped image
        with(painter) { onDraw() }

        // Draw shaped scrim
        scrim?.let {
            val paint = RemotePaint {
                with(it) { applyTo(this@RemotePaint, size) }
                color = Color.Black.rc.copy(alpha = this@DefaultRemoteContainerPainter.alpha)
            }
            val outline = shape.createOutline(size, remoteDensity, layoutDirection)
            with(outline) { drawOutline(paint) }
        }
    }
}

/** Paints [image] and clips with [shape]. */
internal class ShapedBitmapPainter(
    val shape: RemoteShape,
    val image: RemoteImageBitmap,
    val contentScale: ContentScale,
) : RemotePainter() {
    override fun RemoteDrawScope.onDraw() {
        val paint = RemotePaint {
            with(RemoteBrush.image(image, contentScale = contentScale)) {
                applyTo(this@RemotePaint, size)
            }
        }

        val outline = shape.createOutline(size, remoteDensity, layoutDirection)
        with(outline) { drawOutline(paint) }
    }

    override val intrinsicSize: RemoteSize
        get() = RemoteSize(image.width, image.height)
}

internal fun disabledRemoteContainerPainter(
    painter: RemotePainter,
    alpha: RemoteFloat,
): RemotePainter = DefaultDisabledRemoteContainerPainter(painter, alpha)

private class DefaultDisabledRemoteContainerPainter(
    private val painter: RemotePainter,
    private val alpha: RemoteFloat,
    override val intrinsicSize: RemoteSize? = painter.intrinsicSize,
) : RemotePainter() {
    override fun RemoteDrawScope.onDraw() {
        with(painter) { draw(alpha = alpha) }
    }
}
