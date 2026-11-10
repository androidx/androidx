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
import androidx.compose.remote.creation.compose.layout.RemoteContentScale
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
 * Lays out and draws a given [RemoteImageBitmap] in a remote layout.
 *
 * This is the remote equivalent of [androidx.compose.foundation.Image]. Provide a localized
 * [contentDescription] (for example, via [androidx.compose.ui.res.stringResource]) whenever the
 * image conveys meaning; pass `null` only for purely decorative images so accessibility services
 * ignore them.
 *
 * Example of a [RemoteBasicImage] cropped to fill its container:
 *
 * @sample androidx.compose.remote.foundation.samples.RemoteBasicImageSample
 *
 * Example of a [RemoteBasicImage] scaled with [ContentScale.Fit] inside its container:
 *
 * @sample androidx.compose.remote.foundation.samples.RemoteBasicImageFitSample
 * @param bitmap the [RemoteImageBitmap] to draw
 * @param contentDescription localized text used by accessibility services to describe what this
 *   image represents, or `null` if the image is purely decorative and should be ignored by
 *   accessibility services
 * @param modifier the [RemoteModifier] applied to this layout node
 * @param contentScale rule applied to scale the image when its layout bounds differ from the
 *   intrinsic size of [bitmap]. Defaults to [ContentScale.Fit]
 * @param alpha opacity applied to [bitmap] when rendered, from `0.0f` (transparent) to `1.0f`
 *   (opaque). Defaults to [DefaultAlpha]
 */
@Composable
@RemoteComposable
public fun RemoteBasicImage(
    bitmap: RemoteImageBitmap,
    contentDescription: RemoteString?,
    modifier: RemoteModifier = RemoteModifier,
    contentScale: ContentScale = ContentScale.Fit,
    alpha: RemoteFloat = DefaultAlpha.rf,
) {
    RemoteImage(
        bitmap = bitmap,
        contentDescription = contentDescription,
        modifier = modifier,
        contentScale = contentScale.toRemoteContentScale(),
        alpha = alpha,
    )
}

private fun ContentScale.toRemoteContentScale(): RemoteContentScale =
    when (this) {
        ContentScale.Crop -> RemoteContentScale.Crop
        ContentScale.Fit -> RemoteContentScale.Fit
        ContentScale.FillHeight -> RemoteContentScale.FillHeight
        ContentScale.FillWidth -> RemoteContentScale.FillWidth
        ContentScale.Inside -> RemoteContentScale.Inside
        ContentScale.None -> RemoteContentScale.None
        ContentScale.FillBounds -> RemoteContentScale.FillBounds
        else -> RemoteContentScale.None
    }
