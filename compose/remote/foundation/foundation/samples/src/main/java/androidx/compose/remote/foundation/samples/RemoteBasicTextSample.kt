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
import androidx.compose.remote.creation.compose.layout.RemoteColumn
import androidx.compose.remote.creation.compose.state.rc
import androidx.compose.remote.creation.compose.state.rs
import androidx.compose.remote.creation.compose.state.rsp
import androidx.compose.remote.creation.compose.text.RemoteFontFamily
import androidx.compose.remote.foundation.text.RemoteBasicText
import androidx.compose.remote.tooling.preview.RemotePreviewWrapper
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.tooling.preview.PreviewWrapper

@Sampled
@PreviewWrapper(RemotePreviewWrapper::class)
@Composable
fun RemoteBasicTextSample() {
    RemoteBasicText(text = "Hello Remote Compose".rs, color = Color.Black.rc, fontSize = 16.rsp)
}

@Sampled
@PreviewWrapper(RemotePreviewWrapper::class)
@Composable
fun RemoteBasicTextStylingSample() {
    RemoteColumn {
        RemoteBasicText(
            text = "Bold Text".rs,
            color = Color.Black.rc,
            fontSize = 18.rsp,
            fontWeight = FontWeight.Bold,
        )
        RemoteBasicText(
            text = "Italic Text".rs,
            color = Color.Gray.rc,
            fontSize = 14.rsp,
            fontStyle = FontStyle.Italic,
        )
    }
}

@Sampled
@PreviewWrapper(RemotePreviewWrapper::class)
@Composable
fun RemoteBasicTextFontFamilySample() {
    RemoteBasicText(
        text = "Monospace Font".rs,
        color = Color.Black.rc,
        fontSize = 16.rsp,
        fontFamily = RemoteFontFamily.Monospace,
    )
}
