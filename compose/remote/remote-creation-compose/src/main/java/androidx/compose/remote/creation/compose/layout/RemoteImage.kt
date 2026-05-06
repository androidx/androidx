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
@file:Suppress("RestrictedApiAndroidX")

package androidx.compose.remote.creation.compose.layout

import androidx.annotation.RestrictTo
import androidx.compose.remote.creation.compose.capture.RemoteComposeCreationState
import androidx.compose.remote.creation.compose.modifier.RemoteModifier
import androidx.compose.remote.creation.compose.modifier.toRecordingModifier
import androidx.compose.remote.creation.compose.state.RemoteBitmap
import androidx.compose.remote.creation.compose.state.RemoteFloat
import androidx.compose.remote.creation.compose.state.RemoteString
import androidx.compose.remote.creation.compose.state.rb
import androidx.compose.remote.creation.compose.state.rf
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.DefaultAlpha
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.layout.ContentScale

internal class RemoteImageNode : RemoteComposeNode() {
    var image: Any? = null
    var remoteBitmap: RemoteBitmap? = null
    var contentScale: ContentScale = ContentScale.Fit
    var alpha: RemoteFloat = RemoteFloat(1f)
    var contentDescription: RemoteString? = null

    override fun render(creationState: RemoteComposeCreationState, remoteCanvas: RemoteCanvas) {
        val bitmapId =
            remoteBitmap?.getIdForCreationState(creationState)
                ?: image?.let { creationState.document.addBitmap(it) }
                ?: 0
        creationState.document.image(
            creationState.toRecordingModifier(modifier),
            bitmapId,
            contentScale.toImageScalingInt(),
            alpha.getFloatIdForCreationState(creationState),
        )
    }
}

/**
 * A composable that lays out and draws a given [ImageBitmap]. This is the remote equivalent of
 * [androidx.compose.foundation.Image].
 *
 * @param bitmap the [ImageBitmap] to be drawn.
 * @param contentDescription the Text used by accessibility services to describe what this image
 *   represents.
 * @param modifier the [RemoteModifier] to be applied to this layout node.
 * @param contentScale the rule to apply to scale the image when its size does not match the layout
 *   size, Defaults to [ContentScale.Fit].
 * @param alpha the optional opacity to be applied to the [ImageBitmap] when it is rendered.
 */
@RestrictTo(RestrictTo.Scope.LIBRARY_GROUP)
@Composable
@RemoteComposable
public fun RemoteImage(
    bitmap: ImageBitmap,
    contentDescription: RemoteString?,
    modifier: RemoteModifier = RemoteModifier,
    contentScale: ContentScale = ContentScale.Fit,
    alpha: RemoteFloat = DefaultAlpha.rf,
) {
    RemoteImage(bitmap.rb, contentDescription, modifier, contentScale, alpha)
}

/**
 * A composable that lays out and draws a given [RemoteBitmap]. This is the remote equivalent of
 * [androidx.compose.foundation.Image].
 *
 * @param remoteBitmap the [RemoteBitmap] to be drawn.
 * @param contentDescription the Text used by accessibility services to describe what this image
 *   represents.
 * @param modifier the [RemoteModifier] to be applied to this layout node.
 * @param contentScale the rule to apply to scale the image when its size does not match the layout
 *   size, Defaults to [ContentScale.Fit].
 * @param alpha the Optional opacity to be applied to the [remoteBitmap] when it is rendered.
 */
@Composable
@RemoteComposable
public fun RemoteImage(
    remoteBitmap: RemoteBitmap,
    contentDescription: RemoteString?,
    modifier: RemoteModifier = RemoteModifier,
    contentScale: ContentScale = ContentScale.Fit,
    alpha: RemoteFloat = DefaultAlpha.rf,
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
