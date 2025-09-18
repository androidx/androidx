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

package androidx.compose.remote.foundation

import androidx.annotation.RestrictTo
import androidx.compose.remote.foundation.icons.RemoteImageVector
import androidx.compose.remote.frontend.capture.RecordingCanvas
import androidx.compose.remote.frontend.capture.scale
import androidx.compose.remote.frontend.layout.ROffset
import androidx.compose.remote.frontend.layout.RemoteBox
import androidx.compose.remote.frontend.layout.RemoteCanvas
import androidx.compose.remote.frontend.layout.RemoteCanvasDrawScope
import androidx.compose.remote.frontend.layout.RemoteComposable
import androidx.compose.remote.frontend.modifier.RemoteModifier
import androidx.compose.remote.frontend.modifier.fillMaxSize
import androidx.compose.remote.frontend.modifier.semantics
import androidx.compose.remote.frontend.modifier.size
import androidx.compose.remote.frontend.state.RemoteColor
import androidx.compose.remote.frontend.state.RemoteString
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.nativeCanvas
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.unit.LayoutDirection
import androidx.compose.ui.unit.dp

/**
 * Composable function that displays an icon using an [RemoteImageVector].
 *
 * This function provides a way to display icons consistently across both local and remote Compose
 * environments.
 *
 * @param imageVector The [RemoteImageVector] representing the icon to display.
 * @param modifier The [RemoteModifier] to apply to the icon.
 * @param tint The color to apply to the icon. Defaults to the current content color provided by
 *   [DefaultTint].
 */
@RemoteComposable
@Composable
public fun RemoteIcon(
    imageVector: RemoteImageVector,
    contentDescription: RemoteString?,
    modifier: RemoteModifier = RemoteModifier.size(DefaultIconDimension),
    tint: RemoteColor = RemoteColor(DefaultTint.toArgb()),
) {
    RemoteBox(modifier.semantics { this.contentDescription = contentDescription }) {
        RemoteCanvas(modifier = RemoteModifier.fillMaxSize()) { drawImageVector(imageVector, tint) }
    }
}

private fun RemoteCanvasDrawScope.drawImageVector(
    remoteImageVector: RemoteImageVector,
    tint: RemoteColor,
) {
    val viewportSize = remote.component.width
    val canvas = drawContext.canvas.nativeCanvas
    val intrinsicSize = remoteImageVector.intrinsicWidth
    val scale = viewportSize / intrinsicSize

    // Handles autoMirror
    val isRtl = drawContext.layoutDirection == LayoutDirection.Rtl
    val shouldAutoMirror = remoteImageVector.autoMirror && isRtl

    val scaleX = if (shouldAutoMirror) -scale else scale
    val scaleY = scale

    val pivotX = if (shouldAutoMirror) viewportSize / (-scale + 1f) else 0f
    val pivot = ROffset(pivotX, 0f)

    val paint =
        remoteImageVector.paint().apply {
            if (android.os.Build.VERSION.SDK_INT < android.os.Build.VERSION_CODES.Q) return
            setColor(tint.getValueForCreationState(remote.remoteComposeCreationState))
        }

    if (canvas is RecordingCanvas) {
        scale(scaleX = scaleX.internalAsFloat(), scaleY = scaleY.internalAsFloat(), pivot = pivot) {
            canvas.drawRPath(path = remoteImageVector.path, paint = paint)
        }
    }
}

// Default icon size
internal val DefaultIconDimension = 24f.dp
internal val DefaultTint = Color.White
