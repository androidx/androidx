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
import androidx.compose.remote.creation.compose.layout.RemoteContentScale
import androidx.compose.remote.creation.compose.modifier.RemoteModifier
import androidx.compose.remote.creation.compose.modifier.background
import androidx.compose.remote.creation.compose.modifier.size
import androidx.compose.remote.creation.compose.state.RemoteImageBitmap
import androidx.compose.remote.creation.compose.state.rc
import androidx.compose.remote.creation.compose.state.rdp
import androidx.compose.remote.creation.compose.state.rs
import androidx.compose.remote.foundation.image.RemoteBasicImage
import androidx.compose.remote.tooling.preview.RemotePreviewWrapper
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Canvas
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.graphics.Paint
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.tooling.preview.PreviewWrapper

@Sampled
@Preview
@PreviewWrapper(RemotePreviewWrapper::class)
@Composable
fun RemoteBasicImageSample() {
    val remoteBitmap = remember {
        val imageBitmap = ImageBitmap(120, 60)
        Canvas(imageBitmap).apply {
            drawRect(0f, 0f, 120f, 60f, Paint().apply { color = Color(0xFF1E88E5) })
            drawCircle(Offset(60f, 30f), 20f, Paint().apply { color = Color(0xFFFFCA28) })
        }
        RemoteImageBitmap(imageBitmap)
    }
    RemoteBasicImage(
        bitmap = remoteBitmap,
        contentDescription = "Sample Image".rs,
        modifier = RemoteModifier.size(100.rdp),
        contentScale = RemoteContentScale.Crop,
    )
}

@Sampled
@Preview
@PreviewWrapper(RemotePreviewWrapper::class)
@Composable
fun RemoteBasicImageFitSample() {
    val remoteBitmap = remember {
        val imageBitmap = ImageBitmap(120, 60)
        Canvas(imageBitmap).apply {
            drawRect(0f, 0f, 120f, 60f, Paint().apply { color = Color(0xFF1E88E5) })
            drawCircle(Offset(60f, 30f), 20f, Paint().apply { color = Color(0xFFFFCA28) })
        }
        RemoteImageBitmap(imageBitmap)
    }
    RemoteBasicImage(
        bitmap = remoteBitmap,
        contentDescription = "Sample Image Fit".rs,
        modifier = RemoteModifier.size(100.rdp).background(Color.LightGray.rc),
        contentScale = RemoteContentScale.Fit,
    )
}
