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
@file:JvmName("RemoteImageKt")
@file:JvmMultifileClass
@file:Suppress("RestrictedApiAndroidX")

package androidx.compose.remote.creation.compose.layout

import androidx.annotation.RestrictTo
import androidx.compose.remote.creation.compose.capture.RemoteComposeCreationState
import androidx.compose.remote.creation.compose.modifier.RemoteModifier
import androidx.compose.remote.creation.compose.modifier.toRecordingModifier
import androidx.compose.remote.creation.compose.state.RemoteFloat
import androidx.compose.remote.creation.compose.state.RemoteImageBitmap
import androidx.compose.remote.creation.compose.state.RemoteString
import androidx.compose.runtime.Composable

internal class RemoteImageNode : RemoteComposeNode() {
    var image: Any? = null
    var remoteBitmap: RemoteImageBitmap? = null
    var contentScale: RemoteContentScale = RemoteContentScale.Fit
    var alpha: RemoteFloat = RemoteFloat(1f)
    var contentDescription: RemoteString? = null

    override fun render(creationState: RemoteComposeCreationState, remoteCanvas: RemoteCanvas) {
        val bitmapId =
            remoteBitmap?.getIdForCreationState(creationState)
                ?: image?.let { creationState.document.addBitmap(it) }
                ?: 0
        val scope = overriddenScope(creationState)
        creationState.document.image(
            scope.toRecordingModifier(modifier),
            bitmapId,
            contentScale.value,
            alpha.getFloatIdForCreationState(creationState),
        )
    }
}

@RestrictTo(RestrictTo.Scope.LIBRARY_GROUP)
@Composable
@RemoteComposable
public fun RemoteImage(
    remoteBitmap: RemoteImageBitmap,
    contentDescription: RemoteString?,
    modifier: RemoteModifier = RemoteModifier,
    contentScale: RemoteContentScale,
    alpha: RemoteFloat,
) {
    RemoteComposeNode(
        factory = ::RemoteImageNode,
        update = {
            set(modifier) { nodeModifier -> this.modifier = nodeModifier }
            set(remoteBitmap) { this.remoteBitmap = it }
            set(contentScale) { this.contentScale = it }
            set(alpha) { this.alpha = it }
            set(contentDescription) { this.contentDescription = it }
        },
    )
}
