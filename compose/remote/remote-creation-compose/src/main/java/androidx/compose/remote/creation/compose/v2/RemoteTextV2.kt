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

package androidx.compose.remote.creation.compose.v2

import androidx.annotation.RestrictTo
import androidx.compose.remote.creation.compose.layout.RemoteComposable
import androidx.compose.remote.creation.compose.modifier.RemoteModifier
import androidx.compose.remote.creation.compose.state.RemoteColor
import androidx.compose.remote.creation.compose.state.RemoteFloat
import androidx.compose.remote.creation.compose.state.RemoteString
import androidx.compose.remote.creation.compose.state.rc
import androidx.compose.remote.creation.compose.state.rf
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontStyle
import androidx.compose.ui.text.font.FontVariation
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.ui.text.style.TextOverflow

@Composable
@RemoteComposable
@RestrictTo(RestrictTo.Scope.LIBRARY_GROUP)
public fun RemoteTextV2(
    text: RemoteString,
    modifier: RemoteModifier = RemoteModifier,
    color: RemoteColor = Color.Black.rc,
    fontSize: RemoteFloat = 14f.rf,
    fontWeight: RemoteFloat = 400f.rf,
    fontStyle: FontStyle = FontStyle.Normal,
    fontFamily: String? = null,
    textAlign: TextAlign = TextAlign.Unspecified,
    overflow: TextOverflow = TextOverflow.Clip,
    maxLines: Int = Int.MAX_VALUE,
    minFontSize: Float? = null,
    maxFontSize: Float? = null,
    letterSpacing: Float? = null,
    lineHeightAdd: Float? = null,
    lineHeightMultiply: Float? = null,
    textDecoration: TextDecoration = TextDecoration.None,
    fontVariationSettings: FontVariation.Settings? = null,
) {
    RemoteComposeNode(
        factory = ::RemoteTextNodeV2,
        update = {
            set(text) { this.text = it }
            set(modifier) { this.modifier = it }
            set(color) { this.color = it }
            set(fontSize) { this.fontSize = it }
            set(fontWeight) { this.fontWeight = it }
            set(fontStyle) { this.fontStyle = it }
            set(fontFamily) { this.fontFamily = it }
            set(textAlign) { this.textAlign = it }
            set(overflow) { this.overflow = it }
            set(maxLines) { this.maxLines = it }
            set(minFontSize) { this.minFontSize = it }
            set(maxFontSize) { this.maxFontSize = it }
            set(letterSpacing) { this.letterSpacing = it }
            set(lineHeightAdd) { this.lineHeightAdd = it }
            set(lineHeightMultiply) { this.lineHeightMultiply = it }
            set(textDecoration) { this.textDecoration = it }
            set(fontVariationSettings) { this.fontVariationSettings = it }
        },
    )
}
