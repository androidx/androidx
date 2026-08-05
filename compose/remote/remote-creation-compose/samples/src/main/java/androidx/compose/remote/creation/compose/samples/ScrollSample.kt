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
import androidx.compose.remote.creation.compose.layout.RemoteColumn
import androidx.compose.remote.creation.compose.layout.RemoteRow
import androidx.compose.remote.creation.compose.layout.RemoteText
import androidx.compose.remote.creation.compose.modifier.RemoteModifier
import androidx.compose.remote.creation.compose.modifier.background
import androidx.compose.remote.creation.compose.modifier.horizontalScroll
import androidx.compose.remote.creation.compose.modifier.padding
import androidx.compose.remote.creation.compose.modifier.rememberRemoteScrollState
import androidx.compose.remote.creation.compose.modifier.size
import androidx.compose.remote.creation.compose.modifier.verticalScroll
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
fun VerticalScrollSample() {
    val scrollState = rememberRemoteScrollState()
    RemoteColumn(
        modifier =
            RemoteModifier.size(200.rdp).background(Color.LightGray.rc).verticalScroll(scrollState)
    ) {
        for (i in 1..10) {
            RemoteBox(
                modifier =
                    RemoteModifier.size(180.rdp, 50.rdp).padding(5.rdp).background(Color.Red.rc),
                contentAlignment = RemoteAlignment.Center,
            ) {
                RemoteText("Item $i".rs, color = Color.White.rc)
            }
        }
    }
}

@Sampled
@PreviewWrapper(RemoteComponentPreviewWrapper::class)
@Composable
fun HorizontalScrollSample() {
    val scrollState = rememberRemoteScrollState()
    RemoteRow(
        modifier =
            RemoteModifier.size(200.rdp, 100.rdp)
                .background(Color.LightGray.rc)
                .horizontalScroll(scrollState),
        verticalAlignment = RemoteAlignment.CenterVertically,
    ) {
        for (i in 1..10) {
            RemoteBox(
                modifier =
                    RemoteModifier.size(80.rdp, 80.rdp).padding(5.rdp).background(Color.Red.rc),
                contentAlignment = RemoteAlignment.Center,
            ) {
                RemoteText("Item $i".rs, color = Color.White.rc)
            }
        }
    }
}
