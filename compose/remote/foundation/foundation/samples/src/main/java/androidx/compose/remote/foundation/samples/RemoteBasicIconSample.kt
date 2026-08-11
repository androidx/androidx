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
import androidx.compose.remote.creation.compose.capture.RemoteImageVector
import androidx.compose.remote.creation.compose.capture.path
import androidx.compose.remote.creation.compose.modifier.RemoteModifier
import androidx.compose.remote.creation.compose.modifier.size
import androidx.compose.remote.creation.compose.state.RemoteColor
import androidx.compose.remote.creation.compose.state.rc
import androidx.compose.remote.creation.compose.state.rdp
import androidx.compose.remote.creation.compose.state.rf
import androidx.compose.remote.creation.compose.state.rs
import androidx.compose.remote.foundation.icon.RemoteBasicIcon
import androidx.compose.remote.tooling.preview.RemotePreviewWrapper
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.tooling.preview.PreviewWrapper

@Sampled
@PreviewWrapper(RemotePreviewWrapper::class)
@Composable
fun RemoteBasicIconSample() {
    val remoteImageVector =
        RemoteImageVector.Builder(
                viewportWidth = 24.0f.rf,
                viewportHeight = 24.0f.rf,
                tintColor = RemoteColor(Color.White),
                name = "Sample Icon",
            )
            .path(fill = SolidColor(Color.White)) {
                moveTo(4.0f.rf, 4.0f.rf)
                lineTo(20.0f.rf, 4.0f.rf)
                lineTo(20.0f.rf, 20.0f.rf)
                lineTo(4.0f.rf, 20.0f.rf)
                close()
            }
            .build()

    RemoteBasicIcon(
        imageVector = remoteImageVector,
        contentDescription = "Sample Icon".rs,
        modifier = RemoteModifier.size(24.rdp),
        tint = Color.Red.rc,
    )
}
