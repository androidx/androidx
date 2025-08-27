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

package androidx.compose.remote.frontend.modifier

import androidx.annotation.RestrictTo
import androidx.annotation.RestrictTo.Scope
import androidx.compose.remote.creation.modifiers.RecordingModifier
import androidx.compose.remote.frontend.capture.LocalRemoteComposeCreationState
import androidx.compose.remote.frontend.capture.NoRemoteCompose
import androidx.compose.remote.frontend.layout.RemoteDrawWithContentScope
import androidx.compose.remote.frontend.layout.RemoteDrawWithContentScopeImpl
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.draw.drawWithContent

class DrawWithContentModifier(val content: (RemoteDrawWithContentScope).() -> Unit) :
    RemoteLayoutModifier {
    override fun toRemoteComposeElement(): RecordingModifier.Element {
        return androidx.compose.remote.creation.modifiers.DrawWithContentModifier()
    }

    @Composable
    override fun Modifier.toComposeUi(): Modifier {
        val captureMode = LocalRemoteComposeCreationState.current
        if (captureMode is NoRemoteCompose) {
            return this.drawWithContent {
                RemoteDrawWithContentScopeImpl(captureMode, drawScope = this).content()
            }
        }
        return this.drawBehind {
            captureMode.document.startCanvasOperations()
            RemoteDrawWithContentScopeImpl(captureMode, drawScope = this).content()
            captureMode.document.endCanvasOperations()
        }
    }
}

@Composable
fun RemoteModifier.drawWithContent(
    content: (RemoteDrawWithContentScope).() -> Unit
): RemoteModifier {
    return then(DrawWithContentModifier(content))
}
