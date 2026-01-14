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
import androidx.compose.remote.creation.compose.layout.RemoteComposable
import androidx.compose.remote.creation.compose.layout.RemoteDrawScope
import androidx.compose.remote.creation.compose.layout.RemoteDrawWithContentScope
import androidx.compose.remote.creation.compose.layout.RemoteDrawWithContentScope0
import androidx.compose.remote.creation.compose.layout.RemoteDrawWithContentScope0Impl
import androidx.compose.remote.creation.compose.layout.RemoteDrawWithContentScopeImpl
import androidx.compose.remote.creation.compose.state.rf
import androidx.compose.remote.creation.modifiers.RecordingModifier
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.graphics.nativeCanvas

@RestrictTo(RestrictTo.Scope.LIBRARY_GROUP)
public class DrawWithContentModifier0(
    public val content: (RemoteDrawWithContentScope0).() -> Unit
) : RemoteModifier.Element {
    override fun toRemoteComposeElement(): RecordingModifier.Element {
        return androidx.compose.remote.creation.modifiers.DrawWithContentModifier()
    }

    @Composable
    override fun Modifier.toComposeUi(): Modifier {
        val captureMode = LocalRemoteComposeCreationState.current
        return this.drawBehind {
            captureMode.document.startCanvasOperations()
            RemoteDrawWithContentScope0Impl(captureMode, drawScope = this).content()
            captureMode.document.endCanvasOperations()
        }
    }
}

@RestrictTo(RestrictTo.Scope.LIBRARY_GROUP)
@Composable
public fun RemoteModifier.drawWithContent0(
    onDraw: (RemoteDrawWithContentScope0).() -> Unit
): RemoteModifier {
    return then(DrawWithContentModifier0(onDraw))
}

@RestrictTo(RestrictTo.Scope.LIBRARY_GROUP)
@RemoteComposable
@Composable
public fun RemoteModifier.drawWithContent(
    onDraw: RemoteDrawWithContentScope.() -> Unit
): RemoteModifier = then(DrawWithContentModifier2(onDraw))

private class DrawWithContentModifier2(val onDraw: RemoteDrawWithContentScope.() -> Unit) :
    RemoteModifier.Element {
    override fun toRemoteComposeElement(): RecordingModifier.Element {
        return androidx.compose.remote.creation.modifiers.DrawWithContentModifier()
    }

    @Composable
    override fun Modifier.toComposeUi(): Modifier {
        val captureMode = LocalRemoteComposeCreationState.current
        return this.drawBehind {
            val drawScope =
                RemoteDrawScope(
                    remoteCanvas =
                        RemoteCanvas(this.drawContext.canvas.nativeCanvas as RecordingCanvas),
                    fontScale = this.fontScale.rf,
                    layoutDirection = this.layoutDirection,
                )
            captureMode.document.startCanvasOperations()
            RemoteDrawWithContentScopeImpl(drawScope = drawScope).onDraw()
            captureMode.document.endCanvasOperations()
        }
    }
}
