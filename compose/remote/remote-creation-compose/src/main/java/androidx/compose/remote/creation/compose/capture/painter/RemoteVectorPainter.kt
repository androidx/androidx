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
@file:RestrictTo(RestrictTo.Scope.LIBRARY_GROUP)

package androidx.compose.remote.creation.compose.capture.painter

import android.os.Build
import androidx.annotation.RequiresApi
import androidx.annotation.RestrictTo
import androidx.compose.remote.creation.compose.capture.RemoteDrawScope
import androidx.compose.remote.creation.compose.capture.RemoteImageVector
import androidx.compose.remote.creation.compose.capture.scale
import androidx.compose.remote.creation.compose.layout.RemoteOffset
import androidx.compose.remote.creation.compose.layout.RemoteSize
import androidx.compose.remote.creation.compose.state.RemoteColor
import androidx.compose.remote.creation.compose.state.RemoteFloat
import androidx.compose.remote.creation.compose.state.RemotePaint
import androidx.compose.remote.creation.compose.state.rf
import androidx.compose.ui.unit.LayoutDirection

/**
 * A [RemotePainter] that draws a [RemoteImageVector] into the provided RemoteCanvas.
 *
 * @param vector The [RemoteImageVector] to draw.
 * @param srcSize The source size of the [vector] to draw.
 */
@RestrictTo(RestrictTo.Scope.LIBRARY_GROUP)
public class RemoteVectorPainter(
    private val vector: RemoteImageVector,
    private val srcSize: RemoteSize =
        RemoteSize(RemoteFloat(vector.intrinsicWidth), RemoteFloat(vector.intrinsicHeight)),
    private val tintColor: RemoteColor,
) : RemotePainter() {
    @RequiresApi(Build.VERSION_CODES.Q)
    override fun RemoteDrawScope.onDraw() {
        val size = componentSize()
        val viewportSize = size.width.min(size.height)

        val scale = viewportSize / (intrinsicSize.width.min(intrinsicSize.height))

        // Handles autoMirror
        val isRtl = drawContext.layoutDirection == LayoutDirection.Rtl
        val shouldAutoMirror = vector.autoMirror && isRtl

        val scaleX = if (shouldAutoMirror) -scale else scale
        val scaleY = scale

        val pivotX = if (shouldAutoMirror) viewportSize / (-scale + 1f) else 0f.rf
        val pivot = RemoteOffset(pivotX, 0f)

        val paint = RemotePaint(vector.paint()).apply { remoteColor = tintColor }

        scale(scaleX = scaleX.internalAsFloat(), scaleY = scaleY.internalAsFloat(), pivot = pivot) {
            canvas.drawRPath(path = vector.path, paint = paint)
        }
    }

    override val intrinsicSize: RemoteSize
        get() = srcSize
}

/**
 * Creates a [RemoteVectorPainter] from a [RemoteImageVector].
 *
 * @param vector The [RemoteImageVector] to create the painter for.
 */
@RestrictTo(RestrictTo.Scope.LIBRARY_GROUP)
public fun painterRemoteVector(
    vector: RemoteImageVector,
    tintColor: RemoteColor,
): RemoteVectorPainter {
    return RemoteVectorPainter(vector = vector, tintColor = tintColor)
}
