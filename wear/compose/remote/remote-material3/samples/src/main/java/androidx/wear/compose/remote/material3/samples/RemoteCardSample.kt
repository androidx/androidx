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

package androidx.wear.compose.remote.material3.samples

import androidx.annotation.Sampled
import androidx.compose.remote.creation.compose.action.hostAction
import androidx.compose.remote.creation.compose.modifier.RemoteModifier
import androidx.compose.remote.creation.compose.modifier.padding
import androidx.compose.remote.creation.compose.state.RemoteImageBitmap
import androidx.compose.remote.creation.compose.state.createNamedRemoteImageBitmap
import androidx.compose.remote.creation.compose.state.rdp
import androidx.compose.remote.creation.compose.state.rs
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.graphics.Canvas
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.graphics.Paint
import androidx.compose.ui.tooling.preview.PreviewWrapper
import androidx.wear.compose.remote.material3.RemoteCard
import androidx.wear.compose.remote.material3.RemoteCardDefaults
import androidx.wear.compose.remote.material3.RemoteOutlinedCard
import androidx.wear.compose.remote.material3.RemoteText
import androidx.wear.compose.remote.material3.previews.utils.RemoteComponentPreviewWrapper
import androidx.wear.compose.ui.tooling.preview.WearPreviewDevices

@Sampled
@Composable
@WearPreviewDevices
@PreviewWrapper(RemoteComponentPreviewWrapper::class)
fun RemoteCardSample() {
    RemoteCard(onClick = hostAction("open_card".rs), modifier = RemoteModifier.padding(16.rdp)) {
        RemoteText("This is a basic card".rs)
    }
}

@Sampled
@Composable
@WearPreviewDevices
@PreviewWrapper(RemoteComponentPreviewWrapper::class)
fun RemoteCardWithImageSample() {
    val backgroundImage = remember {
        RemoteImageBitmap.createNamedRemoteImageBitmap(
            name = "backgroundImage",
            defaultValue =
                ImageBitmap(200, 200).also { bitmap ->
                    val canvas = Canvas(bitmap)
                    canvas.drawRect(
                        Rect(0f, 0f, 200f, 200f),
                        Paint().apply { color = Color(0xFF1A5276) },
                    )
                },
        )
    }
    val containerPainter = RemoteCardDefaults.containerPainter(backgroundImage)
    RemoteCard(
        onClick = hostAction("open_card".rs),
        containerPainter = containerPainter,
        modifier = RemoteModifier.padding(16.rdp),
    ) {
        RemoteText("Card with image".rs)
    }
}

@Sampled
@Composable
@WearPreviewDevices
@PreviewWrapper(RemoteComponentPreviewWrapper::class)
fun RemoteOutlinedCardSample() {
    RemoteOutlinedCard(
        onClick = hostAction("open_card".rs),
        modifier = RemoteModifier.padding(16.rdp),
    ) {
        RemoteText("This is an outlined card".rs)
    }
}
