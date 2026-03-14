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
import androidx.compose.remote.creation.compose.layout.RemoteSize
import androidx.compose.remote.creation.compose.state.RemoteColor
import androidx.compose.remote.creation.compose.state.RemotePaint
import androidx.compose.ui.graphics.Color

/**
 * A [RemotePainter] that draws a [RemoteColor] into the provided RemoteCanvas.
 *
 * @param color The [RemoteColor] to draw.
 */
@RestrictTo(RestrictTo.Scope.LIBRARY_GROUP)
public class RemoteColorPainter(private val color: RemoteColor) : RemotePainter() {

    override val intrinsicSize: RemoteSize? = null

    override fun RemoteDrawScope.onDraw() {
        val paint = RemotePaint()
        paint.color = color
        drawRect(paint = paint)
    }
}

/**
 * Creates a [RemoteColorPainter] from a [RemoteColor].
 *
 * @param color The [RemoteColor] to create the painter for.
 */
public fun painterRemoteColor(color: RemoteColor): RemotePainter = RemoteColorPainter(color = color)

/**
 * Creates a [RemoteColorPainter] from a [Color].
 *
 * @param color The [Color] to create the painter for.
 */
@RestrictTo(RestrictTo.Scope.LIBRARY_GROUP)
public fun painterRemoteColor(color: Color): RemotePainter =
    painterRemoteColor(color = RemoteColor(color))
