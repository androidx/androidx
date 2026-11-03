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
import androidx.compose.remote.creation.compose.layout.RemoteBox
import androidx.compose.remote.creation.compose.layout.RemoteColumn
import androidx.compose.remote.creation.compose.layout.RemoteOffset
import androidx.compose.remote.creation.compose.layout.remoteComponent
import androidx.compose.remote.creation.compose.modifier.RemoteModifier
import androidx.compose.remote.creation.compose.modifier.background
import androidx.compose.remote.creation.compose.modifier.drawWithContent
import androidx.compose.remote.creation.compose.modifier.fillMaxWidth
import androidx.compose.remote.creation.compose.modifier.height
import androidx.compose.remote.creation.compose.modifier.size
import androidx.compose.remote.creation.compose.previews.utils.RemoteComponentPreviewWrapper
import androidx.compose.remote.creation.compose.state.RemotePaint
import androidx.compose.remote.creation.compose.state.rc
import androidx.compose.remote.creation.compose.state.rdp
import androidx.compose.remote.creation.compose.state.toRemoteDp
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.tooling.preview.PreviewWrapper

@Sampled
@PreviewWrapper(RemoteComponentPreviewWrapper::class)
@Composable
fun RemoteComponentSample() {
    RemoteColumn(modifier = RemoteModifier.size(200.rdp).background(Color.LightGray.rc)) {
        val parentHeight = remoteComponent().height
        RemoteBox(
            modifier =
                RemoteModifier.fillMaxWidth()
                    .height((parentHeight * 0.5f).toRemoteDp())
                    .background(Color.Blue.rc)
        )
    }
}

@Sampled
@PreviewWrapper(RemoteComponentPreviewWrapper::class)
@Composable
fun RemoteComponentDrawScopeSample() {
    RemoteBox(
        modifier =
            RemoteModifier.size(160.rdp).drawWithContent {
                val component = remoteComponent()
                val paint = RemotePaint().apply { color = Color.Cyan.rc }
                drawCircle(
                    paint = paint,
                    radius = component.width * 0.4f,
                    center = RemoteOffset(component.centerX, component.centerY),
                )
                drawContent()
            }
    )
}
