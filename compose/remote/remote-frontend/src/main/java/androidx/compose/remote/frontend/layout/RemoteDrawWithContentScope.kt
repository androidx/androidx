/*
 * Copyright (C) 2025 The Android Open Source Project
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

package androidx.compose.remote.frontend.layout

import androidx.annotation.RestrictTo
import androidx.compose.remote.frontend.capture.NoRemoteCompose
import androidx.compose.remote.frontend.capture.RecordingCanvas
import androidx.compose.remote.frontend.capture.RemoteComposeCreationState
import androidx.compose.remote.frontend.capture.RemoteDrawScope
import androidx.compose.ui.graphics.drawscope.ContentDrawScope
import androidx.compose.ui.graphics.drawscope.DrawContext
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.nativeCanvas
import androidx.compose.ui.unit.LayoutDirection

@RestrictTo(RestrictTo.Scope.LIBRARY_GROUP)
public interface RemoteDrawWithContentScope : RemoteDrawScope {
    public fun drawContent()
}

@RestrictTo(RestrictTo.Scope.LIBRARY_GROUP)
public class RemoteDrawWithContentScopeImpl(
    remoteComposeCreationState: RemoteComposeCreationState,
    drawScope: DrawScope,
    density: Float = drawScope.density,
    fontScale: Float = drawScope.fontScale,
    drawContext: DrawContext = drawScope.drawContext,
    layoutDirection: LayoutDirection = drawScope.layoutDirection,
) :
    RemoteCanvasDrawScope(
        remoteComposeCreationState = remoteComposeCreationState,
        drawScope = drawScope,
        density = density,
        fontScale = fontScale,
        drawContext = drawContext,
        layoutDirection = layoutDirection,
    ),
    RemoteDrawWithContentScope {

    override fun drawContent() {
        if (remoteComposeCreationState is NoRemoteCompose && drawScope is ContentDrawScope) {
            drawScope.drawContent()
        } else {
            val canvas = drawScope.drawContext.canvas.nativeCanvas
            if (canvas is RecordingCanvas) {
                canvas.document.drawComponentContent()
            }
        }
    }
}
