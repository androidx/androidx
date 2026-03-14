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
import androidx.compose.remote.creation.compose.state.RemoteFloat
import androidx.compose.remote.creation.compose.state.RemotePaint
import androidx.compose.remote.creation.compose.state.rc
import androidx.compose.ui.graphics.Color

@Suppress("RestrictedApiAndroidX")
internal fun remoteContainerPainter(
    painter: RemotePainter,
    scrim: RemoteBrush? = null,
    alpha: RemoteFloat,
): RemotePainter = DefaultRemoteContainerPainter(painter, scrim, alpha)

@Suppress("RestrictedApiAndroidX")
private class DefaultRemoteContainerPainter(
    private val painter: RemotePainter,
    private val scrim: RemoteBrush?,
    private val alpha: RemoteFloat,
    override val intrinsicSize: RemoteSize? = painter.intrinsicSize,
) : RemotePainter() {
    override fun RemoteDrawScope.onDraw() {
        with(painter) { draw(alpha = alpha) }
        scrim?.let {
            drawRect(
                paint =
                    RemotePaint {
                        applyRemoteBrush(scrim, size)
                        color =
                            Color.Black.rc.copy(alpha = this@DefaultRemoteContainerPainter.alpha)
                    }
            )
        }
    }
}

@Suppress("RestrictedApiAndroidX")
internal fun disabledRemoteContainerPainter(
    painter: RemotePainter,
    alpha: RemoteFloat,
): RemotePainter = DefaultDisabledRemoteContainerPainter(painter, alpha)

@Suppress("RestrictedApiAndroidX")
private class DefaultDisabledRemoteContainerPainter(
    private val painter: RemotePainter,
    private val alpha: RemoteFloat,
    override val intrinsicSize: RemoteSize? = painter.intrinsicSize,
) : RemotePainter() {
    override fun RemoteDrawScope.onDraw() {
        with(painter) { draw(alpha = alpha) }
    }
}
