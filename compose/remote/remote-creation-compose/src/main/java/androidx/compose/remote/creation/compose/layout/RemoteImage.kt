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
import androidx.compose.foundation.layout.Box
import androidx.compose.remote.creation.compose.capture.LocalRemoteComposeCreationState
import androidx.compose.remote.creation.compose.modifier.RemoteModifier
import androidx.compose.remote.creation.compose.state.RemoteBitmap
import androidx.compose.remote.creation.compose.state.RemoteFloat
import androidx.compose.remote.creation.compose.state.RemoteString
import androidx.compose.remote.creation.compose.state.rb
import androidx.compose.remote.creation.compose.state.rf
import androidx.compose.remote.creation.compose.v2.RemoteComposeApplierV2
import androidx.compose.remote.creation.compose.v2.RemoteImageV2
import androidx.compose.runtime.Composable
import androidx.compose.runtime.currentComposer
import androidx.compose.ui.draw.DrawModifier
import androidx.compose.ui.graphics.DefaultAlpha
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.graphics.drawscope.ContentDrawScope
import androidx.compose.ui.layout.ContentScale

internal class RemoteComposeImageModifier(
    private val modifier: RemoteModifier,
    val bitmapId: Int,
    val contentScale: ContentScale,
    val alpha: RemoteFloat,
) : DrawModifier {
    override fun ContentDrawScope.draw() {
        drawIntoRemoteCanvas { canvas ->
            canvas.document.image(
                with(modifier) { canvas.toRecordingModifier() },
                bitmapId,
                contentScale.toImageScalingInt(),
                with(canvas) { alpha.floatId },
            )
        }
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
    if (currentComposer.applier is RemoteComposeApplierV2) {
        RemoteImageV2(
            remoteBitmap = remoteBitmap,
            modifier = modifier,
            contentScale = contentScale,
            alpha = alpha,
            contentDescription = contentDescription,
        )
        return
    }

    val creationState = LocalRemoteComposeCreationState.current
    @Suppress("COMPOSE_APPLIER_CALL_MISMATCH") // b/446706254
    Box(
        modifier =
            RemoteComposeImageModifier(
                modifier,
                with(creationState) { remoteBitmap.id },
                contentScale,
                alpha,
            )
    )
}
