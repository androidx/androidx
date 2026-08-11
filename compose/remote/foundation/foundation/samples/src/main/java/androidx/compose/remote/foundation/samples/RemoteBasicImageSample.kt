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

package androidx.compose.remote.foundation.samples

import androidx.annotation.Sampled
import androidx.compose.remote.creation.compose.modifier.RemoteModifier
import androidx.compose.remote.creation.compose.modifier.size
import androidx.compose.remote.creation.compose.state.rdp
import androidx.compose.remote.creation.compose.state.rememberRemoteImageBitmap
import androidx.compose.remote.creation.compose.state.rs
import androidx.compose.remote.foundation.image.RemoteBasicImage
import androidx.compose.remote.tooling.preview.RemotePreviewWrapper
import androidx.compose.runtime.Composable
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.tooling.preview.PreviewWrapper

@Sampled
@PreviewWrapper(RemotePreviewWrapper::class)
@Composable
fun RemoteBasicImageSample() {
    val remoteBitmap = rememberRemoteImageBitmap(url = "https://example.com/placeholder.png")
    RemoteBasicImage(
        remoteBitmap = remoteBitmap,
        contentDescription = "Sample Image".rs,
        modifier = RemoteModifier.size(100.rdp),
    )
}

@Sampled
@PreviewWrapper(RemotePreviewWrapper::class)
@Composable
fun RemoteBasicImageFitSample() {
    val remoteBitmap = rememberRemoteImageBitmap(url = "https://example.com/placeholder.png")
    RemoteBasicImage(
        remoteBitmap = remoteBitmap,
        contentDescription = "Sample Image Fit".rs,
        modifier = RemoteModifier.size(100.rdp),
        contentScale = ContentScale.Fit,
    )
}
