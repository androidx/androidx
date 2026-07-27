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
import androidx.compose.remote.creation.compose.layout.RemoteText
import androidx.compose.remote.creation.compose.previews.utils.RemoteComponentPreviewWrapper
import androidx.compose.remote.creation.compose.state.rc
import androidx.compose.remote.creation.compose.state.rs
import androidx.compose.remote.creation.compose.state.rsp
import androidx.compose.remote.creation.compose.text.RemoteFontFamily
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontStyle
import androidx.compose.ui.text.font.FontVariation
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.tooling.preview.PreviewWrapper

@Sampled
@PreviewWrapper(RemoteComponentPreviewWrapper::class)
@Composable
fun RemoteTextSample() {
    RemoteText(text = "Hello Remote Compose".rs, color = Color.Blue.rc, fontSize = 20.rsp)
}

@Sampled
@PreviewWrapper(RemoteComponentPreviewWrapper::class)
@Composable
fun RemoteTextStylingSample() {
    RemoteText(
        text = "Bold & Italic".rs,
        fontWeight = FontWeight.Bold,
        fontStyle = FontStyle.Italic,
    )
}

@Sampled
@PreviewWrapper(RemoteComponentPreviewWrapper::class)
@Composable
fun RemoteTextFontFamilySample() {
    RemoteColumn {
        RemoteText(text = "Serif".rs, fontFamily = RemoteFontFamily.Serif)
        RemoteText(text = "Sans Serif".rs, fontFamily = RemoteFontFamily.SansSerif)
        RemoteText(text = "Monospace".rs, fontFamily = RemoteFontFamily.Monospace)
        RemoteText(text = "ExtraBold".rs, fontWeight = FontWeight.ExtraBold)
        RemoteText(
            text = "Named Flex Font with Variations".rs,
            fontFamily = RemoteFontFamily.Named("device:roboto-flex"),
            fontVariationSettings =
                FontVariation.Settings(FontVariation.weight(300), FontVariation.width(70f)),
        )
    }
}
