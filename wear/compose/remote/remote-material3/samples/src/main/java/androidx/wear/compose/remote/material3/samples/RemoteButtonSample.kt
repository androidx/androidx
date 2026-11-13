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

package androidx.wear.compose.remote.material3.samples

import androidx.annotation.Sampled
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Favorite
import androidx.compose.remote.creation.compose.action.hostAction
import androidx.compose.remote.creation.compose.action.valueChangeAction
import androidx.compose.remote.creation.compose.capture.toRemoteImageVector
import androidx.compose.remote.creation.compose.modifier.RemoteModifier
import androidx.compose.remote.creation.compose.modifier.size
import androidx.compose.remote.creation.compose.state.MutableRemoteInt
import androidx.compose.remote.creation.compose.state.RemoteImageBitmap
import androidx.compose.remote.creation.compose.state.createNamedRemoteImageBitmap
import androidx.compose.remote.creation.compose.state.rs
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.graphics.Canvas
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.graphics.Paint
import androidx.compose.ui.tooling.preview.PreviewWrapper
import androidx.wear.compose.remote.material3.RemoteButton
import androidx.wear.compose.remote.material3.RemoteButtonDefaults
import androidx.wear.compose.remote.material3.RemoteIcon
import androidx.wear.compose.remote.material3.RemoteText
import androidx.wear.compose.remote.material3.previews.utils.RemoteComponentPreviewWrapper
import androidx.wear.compose.ui.tooling.preview.WearPreviewDevices

@Sampled
@Composable
@WearPreviewDevices
@PreviewWrapper(RemoteComponentPreviewWrapper::class)
fun RemoteButtonSimpleSample(modifier: RemoteModifier = RemoteModifier) {
    val tapCount = remember { MutableRemoteInt(0) }
    val countSuffix = " (".rs + tapCount.toRemoteString() + " taps)".rs

    RemoteButton(valueChangeAction(tapCount, tapCount + 1), modifier = modifier) {
        RemoteText("Tap me!".rs + countSuffix)
    }
}

@Sampled
@Composable
@WearPreviewDevices
@PreviewWrapper(RemoteComponentPreviewWrapper::class)
fun RemoteButtonWithIconAndSecondaryLabelSample(modifier: RemoteModifier = RemoteModifier) {
    RemoteButton(
        onClick = hostAction("favorite_click".rs),
        modifier = modifier,
        icon = {
            RemoteIcon(
                imageVector = Icons.Filled.Favorite.toRemoteImageVector(),
                contentDescription = "Favorite".rs,
                modifier = RemoteModifier.size(RemoteButtonDefaults.IconSize),
            )
        },
        secondaryLabel = { RemoteText("Secondary label".rs) },
        label = { RemoteText("Primary label".rs) },
    )
}

@Sampled
@Composable
@WearPreviewDevices
@PreviewWrapper(RemoteComponentPreviewWrapper::class)
fun RemoteButtonWithContainerPainterSample(modifier: RemoteModifier = RemoteModifier) {
    val backgroundImage = remember {
        RemoteImageBitmap.createNamedRemoteImageBitmap(
            name = "buttonBackground",
            defaultValue =
                ImageBitmap(200, 100).also { bitmap ->
                    val canvas = Canvas(bitmap)
                    canvas.drawRect(
                        Rect(0f, 0f, 200f, 100f),
                        Paint().apply { color = Color(0xFF1A5276) },
                    )
                },
        )
    }
    RemoteButton(
        onClick = hostAction("open_image".rs),
        containerPainter = RemoteButtonDefaults.containerPainter(backgroundImage),
        modifier = modifier,
    ) {
        RemoteText("Image background".rs)
    }
}
