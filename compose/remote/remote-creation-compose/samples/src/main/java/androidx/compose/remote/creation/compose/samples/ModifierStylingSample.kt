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

package androidx.compose.remote.creation.compose.samples

import androidx.annotation.Sampled
import androidx.compose.remote.creation.compose.layout.RemoteAlignment
import androidx.compose.remote.creation.compose.layout.RemoteBox
import androidx.compose.remote.creation.compose.layout.RemoteText
import androidx.compose.remote.creation.compose.modifier.RemoteModifier
import androidx.compose.remote.creation.compose.modifier.background
import androidx.compose.remote.creation.compose.modifier.border
import androidx.compose.remote.creation.compose.modifier.clip
import androidx.compose.remote.creation.compose.modifier.padding
import androidx.compose.remote.creation.compose.previews.utils.RemoteComponentPreviewWrapper
import androidx.compose.remote.creation.compose.shapes.RemoteRoundedCornerShape
import androidx.compose.remote.creation.compose.state.rc
import androidx.compose.remote.creation.compose.state.rdp
import androidx.compose.remote.creation.compose.state.rs
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.tooling.preview.PreviewWrapper

@Sampled
@Preview
@PreviewWrapper(RemoteComponentPreviewWrapper::class)
@Composable
fun ModifierBackgroundAndClipSample() {
    RemoteBox(
        modifier =
            RemoteModifier.clip(RemoteRoundedCornerShape(16.rdp))
                .background(Color(0xFF1E88E5).rc)
                .padding(horizontal = 20.rdp, vertical = 12.rdp),
        contentAlignment = RemoteAlignment.Center,
    ) {
        RemoteText(text = "Clipped Background".rs, color = Color.White.rc)
    }
}

@Sampled
@Preview
@PreviewWrapper(RemoteComponentPreviewWrapper::class)
@Composable
fun ModifierBorderSample() {
    val shape = RemoteRoundedCornerShape(16.rdp)
    RemoteBox(
        modifier =
            RemoteModifier.border(width = 2.rdp, color = Color(0xFF1E88E5).rc, shape = shape)
                .clip(shape)
                .background(Color(0xFFE3F2FD).rc)
                .padding(horizontal = 20.rdp, vertical = 12.rdp),
        contentAlignment = RemoteAlignment.Center,
    ) {
        RemoteText(text = "Rounded Border".rs, color = Color(0xFF0D47A1).rc)
    }
}
