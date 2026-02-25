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
@file:Suppress("RestrictedApiAndroidX")

package androidx.wear.compose.remote.material3.previews

import androidx.compose.remote.creation.compose.layout.RemoteAlignment
import androidx.compose.remote.creation.compose.layout.RemoteArrangement
import androidx.compose.remote.creation.compose.layout.RemoteBox
import androidx.compose.remote.creation.compose.layout.RemoteComposable
import androidx.compose.remote.creation.compose.modifier.RemoteModifier
import androidx.compose.remote.creation.compose.modifier.fillMaxSize
import androidx.compose.remote.creation.compose.state.RemoteColor
import androidx.compose.remote.creation.compose.state.rs
import androidx.compose.remote.creation.compose.state.rsp
import androidx.compose.remote.creation.compose.text.RemoteTextStyle
import androidx.compose.remote.creation.profile.Profile
import androidx.compose.remote.tooling.preview.RemotePreview
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontStyle
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.tooling.preview.PreviewParameter
import androidx.wear.compose.remote.material3.RemoteText
import androidx.wear.compose.ui.tooling.preview.WearPreviewDevices

@WearPreviewDevices
@Composable
private fun RemoteTextStylePreview(
    @PreviewParameter(ProfilePreviewParameterProvider::class) profile: Profile
) =
    RemotePreview(profile = profile) {
        Container {
            val text = "textWithStyle".rs

            RemoteText(
                text,
                style = RemoteTextStyle(fontStyle = FontStyle.Italic, fontSize = 32.rsp),
            )
        }
    }

@WearPreviewDevices
@Composable
private fun RemoteTextOverflowPreview(
    @PreviewParameter(ProfilePreviewParameterProvider::class) profile: Profile
) =
    RemotePreview(profile = profile) {
        Container {
            val text =
                "a piece of writing in which the expression of feelings and ideas is given intensity by particular attention to diction (sometimes involving rhyme), rhythm, and imagery."
                    .rs

            val color = RemoteColor(Color.Green)

            RemoteText(
                text = text,
                fontSize = 18.rsp,
                color = color,
                overflow = TextOverflow.Ellipsis,
                maxLines = 1,
            )
        }
    }

@Composable
@RemoteComposable
private fun Container(
    modifier: RemoteModifier = RemoteModifier.fillMaxSize(),
    content: @Composable @RemoteComposable () -> Unit,
) {
    RemoteBox(
        modifier,
        horizontalAlignment = RemoteAlignment.CenterHorizontally,
        verticalArrangement = RemoteArrangement.Center,
        content = content,
    )
}
