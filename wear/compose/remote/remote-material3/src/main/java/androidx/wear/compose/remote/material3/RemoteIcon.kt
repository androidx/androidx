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

package androidx.wear.compose.remote.material3

import android.annotation.SuppressLint
import androidx.annotation.RestrictTo
import androidx.compose.remote.creation.compose.capture.RemoteImageVector
import androidx.compose.remote.creation.compose.layout.RemoteBox
import androidx.compose.remote.creation.compose.layout.RemoteCanvas
import androidx.compose.remote.creation.compose.layout.RemoteComposable
import androidx.compose.remote.creation.compose.modifier.RemoteModifier
import androidx.compose.remote.creation.compose.modifier.fillMaxSize
import androidx.compose.remote.creation.compose.modifier.semantics
import androidx.compose.remote.creation.compose.modifier.size
import androidx.compose.remote.creation.compose.state.RemoteColor
import androidx.compose.remote.creation.compose.state.RemoteString
import androidx.compose.remote.creation.compose.state.rdp
import androidx.compose.remote.creation.compose.vector.painterRemoteVector
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Paint
import androidx.compose.ui.graphics.nativePaint
import androidx.compose.ui.graphics.vector.ImageVector

/**
 * Composable function that displays an icon using an [RemoteImageVector].
 *
 * @sample androidx.wear.compose.remote.material3.samples.RemoteIconSimpleSample
 *
 * This function provides a way to display icons consistently across both local and remote Compose
 * environments.
 *
 * @param imageVector The [ImageVector] representing the icon to display.
 * @param modifier The [RemoteModifier] to apply to the icon.
 * @param tint The color to apply to the icon. Defaults to the current content color provided by
 *   [DefaultTint].
 */
@SuppressLint("RestrictedApiAndroidX")
@RemoteComposable
@Composable
public fun RemoteIcon(
    imageVector: ImageVector,
    contentDescription: RemoteString?,
    modifier: RemoteModifier = RemoteModifier.size(DefaultIconDimension),
    tint: RemoteColor = LocalRemoteContentColor.current,
) {
    RemoteBox(modifier.semantics { this.contentDescription = contentDescription }) {
        val painter = painterRemoteVector(imageVector, tint)
        RemoteCanvas(modifier = RemoteModifier.fillMaxSize()) {
            with(painter) { onDraw() }
            // TODO(b/474687917): Temporary fix to reset tinted paint
            remoteCanvas.internalCanvas.usePaint(Paint().nativePaint)
        }
    }
}

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
@SuppressLint("RestrictedApiAndroidX")
@RemoteComposable
@Composable
public fun RemoteIcon(
    imageVector: RemoteImageVector,
    contentDescription: RemoteString?,
    modifier: RemoteModifier = RemoteModifier.size(DefaultIconDimension),
    tint: RemoteColor = LocalRemoteContentColor.current,
) {
    RemoteBox(modifier.semantics { this.contentDescription = contentDescription }) {
        val painter = painterRemoteVector(imageVector, tint)
        RemoteCanvas(modifier = RemoteModifier.fillMaxSize()) { with(painter) { onDraw() } }
    }
}

// Default icon size
internal val DefaultIconDimension = 24.rdp
internal val DefaultTint = Color.White
