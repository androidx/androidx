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
import androidx.compose.remote.creation.compose.state.RemoteString
import androidx.compose.runtime.Composable
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontStyle
import androidx.compose.ui.text.font.FontVariation
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.TextUnit

@Composable
@RemoteComposable
@RestrictTo(RestrictTo.Scope.LIBRARY_GROUP)
public fun RemoteTextV2(
    text: String? = null,
    remoteText: RemoteString? = null,
    modifier: RemoteModifier = RemoteModifier,
    color: RemoteColor? = null,
    fontSize: TextUnit = TextUnit.Unspecified,
    fontWeight: FontWeight? = null,
    fontStyle: FontStyle? = null,
    fontFamily: FontFamily? = null,
    textAlign: TextAlign? = null,
    overflow: TextOverflow = TextOverflow.Clip,
    maxLines: Int = Int.MAX_VALUE,
    textDecoration: TextDecoration = TextDecoration.None,
    fontVariationSettings: FontVariation.Settings? = null,
) {
    RemoteComposeNode(
        factory = { RemoteTextNodeV2() },
        update = {
            set(text) { this.text = it }
            set(remoteText) { this.remoteText = it }
            set(modifier) { this.modifier = it }
            set(color) { this.color = it }
            set(fontSize) { this.fontSize = it }
            set(fontWeight) { this.fontWeight = it }
            set(fontStyle) { this.fontStyle = it }
            set(fontFamily) { this.fontFamily = it }
            set(textAlign) { this.textAlign = it }
            set(overflow) { this.overflow = it }
            set(maxLines) { this.maxLines = it }
            set(textDecoration) { this.textDecoration = it }
            set(fontVariationSettings) { this.fontVariationSettings = it }
        },
    )
}
