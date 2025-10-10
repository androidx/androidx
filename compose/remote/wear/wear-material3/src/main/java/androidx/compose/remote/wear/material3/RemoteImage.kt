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

package androidx.compose.remote.wear.material3

import androidx.annotation.RestrictTo
import androidx.compose.foundation.layout.Box
import androidx.compose.remote.creation.compose.capture.LocalRemoteComposeCreationState
import androidx.compose.remote.creation.compose.capture.RecordingCanvas
import androidx.compose.remote.creation.compose.layout.RemoteComposable
import androidx.compose.remote.creation.compose.modifier.RemoteModifier
import androidx.compose.remote.creation.compose.modifier.semantics
import androidx.compose.remote.creation.compose.modifier.toComposeUiLayout
import androidx.compose.remote.creation.compose.state.RemoteBitmap
import androidx.compose.remote.creation.compose.state.RemoteFloat
import androidx.compose.remote.creation.compose.state.RemoteString
import androidx.compose.remote.creation.compose.state.rememberRemoteFloatValue
import androidx.compose.remote.wear.material3.image.toRemoteCompose
import androidx.compose.runtime.Composable
import androidx.compose.ui.draw.DrawModifier
import androidx.compose.ui.graphics.DefaultAlpha
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.graphics.drawscope.ContentDrawScope
import androidx.compose.ui.graphics.drawscope.drawIntoCanvas
import androidx.compose.ui.graphics.nativeCanvas
import androidx.compose.ui.layout.ContentScale

internal class RemoteComposeImageModifier(
    private val modifier: RemoteModifier,
    val bitmapId: Int,
    val contentScale: ContentScale,
    val alpha: RemoteFloat,
) : DrawModifier {
    override fun ContentDrawScope.draw() {
        drawIntoCanvas {
            if (it.nativeCanvas is RecordingCanvas) {
                (it.nativeCanvas as RecordingCanvas)
                    .document
                    .image(
                        modifier.toRemoteCompose(),
                        bitmapId,
                        contentScale.toRemoteCompose(),
                        alpha.internalAsFloat(),
                    )
            }
        }
    }
}

/**
 * A composable that lays out and draws a given [ImageBitmap]. This is the remote equivalent of
 * [androidx.compose.foundation.Image].
 *
 * @param bitmap The [ImageBitmap] to be drawn.
 * @param contentDescription Text used by accessibility services to describe what this image
 *   represents.
 * @param modifier The [RemoteModifier] to be applied to this layout node.
 * @param contentScale The rule to apply to scale the image when its size does not match the layout
 *   size. Defaults to [ContentScale.Fit].
 * @param alpha Optional opacity to be applied to the [ImageBitmap] when it is rendered.
 */
@Composable
@RemoteComposable
public fun RemoteImage(
    bitmap: ImageBitmap,
    contentDescription: RemoteString?,
    modifier: RemoteModifier = RemoteModifier,
    contentScale: ContentScale = ContentScale.Fit,
    alpha: RemoteFloat = rememberRemoteFloatValue { DefaultAlpha },
) {
    val bitmapId = LocalRemoteComposeCreationState.current.document.addBitmap(bitmap)
    Box(
        modifier =
            RemoteComposeImageModifier(modifier, bitmapId, contentScale, alpha)
                .then(
                    modifier.semantics { contentDescription?.let { text = it } }.toComposeUiLayout()
                )
    )
}

/**
 * A composable that lays out and draws a given [RemoteBitmap]. This is the remote equivalent of
 * [androidx.compose.foundation.Image].
 *
 * @param remoteBitmap The [RemoteBitmap] to be drawn.
 * @param contentDescription Text used by accessibility services to describe what this image
 *   represents.
 * @param modifier The [RemoteModifier] to be applied to this layout node.
 * @param contentScale The rule to apply to scale the image when its size does not match the layout
 *   size. Defaults to [ContentScale.Fit].
 * @param alpha Optional opacity to be applied to the [remoteBitmap] when it is rendered.
 */
@Composable
@RemoteComposable
public fun RemoteImage(
    remoteBitmap: RemoteBitmap,
    contentDescription: RemoteString?,
    modifier: RemoteModifier = RemoteModifier,
    contentScale: ContentScale = ContentScale.Fit,
    alpha: RemoteFloat = rememberRemoteFloatValue { DefaultAlpha },
) {
    Box(
        modifier =
            RemoteComposeImageModifier(modifier, remoteBitmap.id, contentScale, alpha)
                .then(
                    modifier.semantics { contentDescription?.let { text = it } }.toComposeUiLayout()
                )
    )
}
