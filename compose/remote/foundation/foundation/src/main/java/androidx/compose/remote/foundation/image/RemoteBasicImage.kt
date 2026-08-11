/*
 * Copyright 2026 The Android Open Source Project
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
package androidx.compose.remote.foundation.image

import androidx.compose.remote.creation.compose.layout.RemoteComposable
import androidx.compose.remote.creation.compose.layout.RemoteImage
import androidx.compose.remote.creation.compose.modifier.RemoteModifier
import androidx.compose.remote.creation.compose.state.RemoteFloat
import androidx.compose.remote.creation.compose.state.RemoteImageBitmap
import androidx.compose.remote.creation.compose.state.RemoteString
import androidx.compose.remote.creation.compose.state.rf
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.DefaultAlpha
import androidx.compose.ui.layout.ContentScale

/**
 * A composable that lays out and draws a given [RemoteImageBitmap]. This is the remote equivalent
 * of [androidx.compose.foundation.Image].
 *
 * @param remoteBitmap the [RemoteImageBitmap] to be drawn.
 * @param contentDescription the Text used by accessibility services to describe what this image
 *   represents.
 * @param modifier the [RemoteModifier] to be applied to this layout node.
 * @param contentScale the rule to apply to scale the image when its size does not match the layout
 *   size, Defaults to [ContentScale.Fit].
 * @param alpha the Optional opacity to be applied to the [remoteBitmap] when it is rendered.
 * @sample androidx.compose.remote.foundation.samples.RemoteBasicImageSample
 * @sample androidx.compose.remote.foundation.samples.RemoteBasicImageFitSample
 */
@Composable
@RemoteComposable
public fun RemoteBasicImage(
    remoteBitmap: RemoteImageBitmap,
    contentDescription: RemoteString?,
    modifier: RemoteModifier = RemoteModifier,
    contentScale: ContentScale = ContentScale.Fit,
    alpha: RemoteFloat = DefaultAlpha.rf,
) {
    RemoteImage(
        remoteBitmap = remoteBitmap,
        contentDescription = contentDescription,
        modifier = modifier,
        contentScale = contentScale,
        alpha = alpha,
    )
}
