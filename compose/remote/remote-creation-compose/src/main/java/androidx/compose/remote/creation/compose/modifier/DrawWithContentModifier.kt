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

package androidx.compose.remote.creation.compose.modifier

import androidx.annotation.RestrictTo
import androidx.compose.remote.creation.compose.capture.LocalRemoteComposeCreationState
import androidx.compose.remote.creation.compose.capture.RecordingCanvas
import androidx.compose.remote.creation.compose.layout.RemoteCanvas
import androidx.compose.remote.creation.compose.layout.RemoteContentDrawScope
import androidx.compose.remote.creation.compose.state.RemoteStateScope
import androidx.compose.remote.creation.modifiers.RecordingModifier
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.graphics.nativeCanvas

/**
 * Creates a [RemoteModifier] that allows drawing with the component's content.
 *
 * @param onDraw The drawing block that provides access to [RemoteContentDrawScope].
 */
public fun RemoteModifier.drawWithContent(
    onDraw: RemoteContentDrawScope.() -> Unit
): RemoteModifier = then(DrawWithContentModifier(onDraw))

internal class DrawWithContentModifier(val onDraw: RemoteContentDrawScope.() -> Unit) :
    RemoteModifier.Element {
    @RestrictTo(RestrictTo.Scope.LIBRARY_GROUP)
    override fun RemoteStateScope.toRecordingModifierElement(): RecordingModifier.Element {
        return androidx.compose.remote.creation.modifiers.DrawWithContentModifier()
    }

    @Composable
    @RestrictTo(RestrictTo.Scope.LIBRARY_GROUP)
    override fun Modifier.toComposeUi(): Modifier {
        val captureMode = LocalRemoteComposeCreationState.current
        return this.drawBehind {
            val drawScope =
                RemoteContentDrawScope(
                    remoteCanvas =
                        RemoteCanvas(
                            internalCanvas = drawContext.canvas.nativeCanvas as RecordingCanvas
                        )
                )
            captureMode.document.startCanvasOperations()
            drawScope.onDraw()
            captureMode.document.endCanvasOperations()
        }
    }
}
