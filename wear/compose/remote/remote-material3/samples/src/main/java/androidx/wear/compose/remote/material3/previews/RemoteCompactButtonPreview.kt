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

import androidx.compose.remote.creation.compose.action.HostAction
import androidx.compose.remote.creation.compose.layout.RemoteAlignment
import androidx.compose.remote.creation.compose.layout.RemoteBox
import androidx.compose.remote.creation.compose.layout.RemoteComposable
import androidx.compose.remote.creation.compose.modifier.RemoteModifier
import androidx.compose.remote.creation.compose.modifier.fillMaxSize
import androidx.compose.remote.creation.compose.modifier.size
import androidx.compose.remote.creation.compose.shapes.RemoteRoundedCornerShape
import androidx.compose.remote.creation.compose.state.RemoteColor
import androidx.compose.remote.creation.compose.state.rdp
import androidx.compose.remote.creation.compose.state.rf
import androidx.compose.remote.creation.compose.state.rs
import androidx.compose.remote.creation.profile.Profile
import androidx.compose.remote.tooling.preview.RemoteContentPreview
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.tooling.preview.PreviewParameter
import androidx.wear.compose.remote.material3.RemoteButtonDefaults
import androidx.wear.compose.remote.material3.RemoteCompactButton
import androidx.wear.compose.remote.material3.RemoteIcon
import androidx.wear.compose.remote.material3.RemoteText
import androidx.wear.compose.remote.material3.previews.utils.ProfilePreviewParameterProvider
import androidx.wear.compose.remote.material3.previews.utils.TestImageVectors
import androidx.wear.compose.ui.tooling.preview.WearPreviewDevices

@Composable
@RemoteComposable
fun RemoteCompactButtonWithIcon() {
    RemoteCompactButton(
        onClick = testAction,
        modifier = RemoteModifier,
        icon = {
            RemoteIcon(
                modifier = RemoteModifier.size(RemoteButtonDefaults.SmallIconSize),
                imageVector = TestImageVectors.VolumeUp,
                contentDescription = null,
                tint = RemoteButtonDefaults.buttonColors().iconColor,
            )
        },
        label = null,
    )
}

@WearPreviewDevices
@Composable
private fun RemoteCompactButtonWithIconPreview(
    @PreviewParameter(ProfilePreviewParameterProvider::class) profile: Profile
) = RemoteContentPreview(profile = profile) { Container { RemoteCompactButtonWithIcon() } }

@Composable
@RemoteComposable
fun RemoteCompactButtonWithLabel() {
    RemoteCompactButton(
        onClick = testAction,
        modifier = RemoteModifier,
        label = { RemoteText("label".rs) },
    )
}

@WearPreviewDevices
@Composable
private fun RemoteCompactButtonWithLabelPreview(
    @PreviewParameter(ProfilePreviewParameterProvider::class) profile: Profile
) = RemoteContentPreview(profile = profile) { Container { RemoteCompactButtonWithLabel() } }

@Composable
@RemoteComposable
fun RemoteCompactButtonWithIconAndLabel() {
    RemoteCompactButton(
        onClick = testAction,
        modifier = RemoteModifier,
        icon = {
            RemoteIcon(
                modifier = RemoteModifier.size(RemoteButtonDefaults.ExtraSmallIconSize),
                imageVector = TestImageVectors.VolumeUp,
                contentDescription = null,
                tint = RemoteButtonDefaults.buttonColors().iconColor,
            )
        },
        label = { RemoteText("label".rs) },
    )
}

@WearPreviewDevices
@Composable
private fun RemoteCompactButtonWithIconAndLabelPreview(
    @PreviewParameter(ProfilePreviewParameterProvider::class) profile: Profile
) = RemoteContentPreview(profile = profile) { Container { RemoteCompactButtonWithIconAndLabel() } }

@Composable
@RemoteComposable
fun RemoteCompactButtonWithBorder() {
    RemoteCompactButton(
        onClick = testAction,
        border = 2.rdp,
        borderColor = RemoteColor(Color.Cyan),
        label = { RemoteText("With border".rs) },
    )
}

@WearPreviewDevices
@Composable
private fun RemoteCompactButtonWithBorderPreview(
    @PreviewParameter(ProfilePreviewParameterProvider::class) profile: Profile
) = RemoteContentPreview(profile = profile) { Container { RemoteCompactButtonWithBorder() } }

@Composable
@RemoteComposable
fun RemoteCompactButtonWithShape() {
    RemoteCompactButton(
        onClick = testAction,
        shape = RemoteRoundedCornerShape(4.rdp),
        label = { RemoteText("Custom shape".rs) },
    )
}

@WearPreviewDevices
@Composable
private fun RemoteCompactButtonWithShapePreview(
    @PreviewParameter(ProfilePreviewParameterProvider::class) profile: Profile
) = RemoteContentPreview(profile = profile) { Container { RemoteCompactButtonWithShape() } }

@Composable
@RemoteComposable
private fun Container(
    modifier: RemoteModifier = RemoteModifier.fillMaxSize(),
    content: @Composable @RemoteComposable () -> Unit,
) {
    RemoteBox(modifier, contentAlignment = RemoteAlignment.Center, content = content)
}

private val testAction = HostAction("testAction".rs, 1.rf)
