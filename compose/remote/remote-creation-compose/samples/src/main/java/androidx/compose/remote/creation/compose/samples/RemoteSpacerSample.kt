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
import androidx.compose.remote.creation.compose.layout.RemoteColumn
import androidx.compose.remote.creation.compose.layout.RemoteRow
import androidx.compose.remote.creation.compose.layout.RemoteSpacer
import androidx.compose.remote.creation.compose.layout.RemoteText
import androidx.compose.remote.creation.compose.modifier.RemoteModifier
import androidx.compose.remote.creation.compose.modifier.height
import androidx.compose.remote.creation.compose.modifier.width
import androidx.compose.remote.creation.compose.previews.utils.RemoteComponentPreviewWrapper
import androidx.compose.remote.creation.compose.state.rc
import androidx.compose.remote.creation.compose.state.rdp
import androidx.compose.remote.creation.compose.state.rs
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.tooling.preview.PreviewWrapper

@Sampled
@PreviewWrapper(RemoteComponentPreviewWrapper::class)
@Composable
fun RemoteSpacerSample() {
    RemoteColumn {
        RemoteText("Above Spacer".rs, color = Color.Black.rc)
        RemoteSpacer(modifier = RemoteModifier.height(16.rdp))
        RemoteText("Below Spacer".rs, color = Color.Black.rc)
    }
}

@Sampled
@PreviewWrapper(RemoteComponentPreviewWrapper::class)
@Composable
fun RemoteSpacerWeightSample() {
    RemoteRow(modifier = RemoteModifier.width(200.rdp)) {
        RemoteText("Left".rs, color = Color.Black.rc)
        RemoteSpacer(modifier = RemoteModifier.weight(1f))
        RemoteText("Right".rs, color = Color.Black.rc)
    }
}
