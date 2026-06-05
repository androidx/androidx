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

package androidx.compose.remote.creation.compose.painter

import androidx.annotation.RestrictTo
import androidx.compose.remote.creation.compose.layout.RemoteDrawScope
import androidx.compose.remote.creation.compose.layout.RemoteOffset
import androidx.compose.remote.creation.compose.layout.RemoteSize
import androidx.compose.remote.creation.compose.state.RemoteImageBitmap
import androidx.compose.ui.layout.ContentScale

/**
 * A [RemotePainter] that draws a [RemoteImageBitmap] into the provided RemoteCanvas.
 *
 * @param image The [RemoteImageBitmap] to draw.
 * @param srcOffset The offset into the [image] to start drawing from.
 * @param srcSize The size of the section of the [image] to draw.
 */
@RestrictTo(RestrictTo.Scope.LIBRARY_GROUP)
public class RemoteImageBitmapPainter(
    private val image: RemoteImageBitmap,
    private val srcOffset: RemoteOffset = RemoteOffset.Zero,
    private val srcSize: RemoteSize = RemoteSize(image.width, image.height),
) : RemotePainter() {
    override fun RemoteDrawScope.onDraw() {
        drawScaledBitmap(
            image = image,
            srcOffset = srcOffset,
            srcSize = srcSize,
            scaleType = ContentScale.FillBounds,
        )
    }

    override val intrinsicSize: RemoteSize
        get() = RemoteSize(image.width, image.height)
}

/**
 * Creates a [RemoteImageBitmapPainter] from a [RemoteImageBitmap].
 *
 * @param image The [RemoteImageBitmap] to create the painter for.
 */
public fun painterRemoteImageBitmap(image: RemoteImageBitmap): RemotePainter {
    return RemoteImageBitmapPainter(image)
}
