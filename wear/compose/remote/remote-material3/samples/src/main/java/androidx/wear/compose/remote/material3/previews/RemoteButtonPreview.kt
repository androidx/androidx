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

package androidx.wear.compose.remote.material3.previews

import androidx.compose.remote.creation.compose.action.HostAction
import androidx.compose.remote.creation.compose.layout.RemoteAlignment
import androidx.compose.remote.creation.compose.layout.RemoteArrangement
import androidx.compose.remote.creation.compose.layout.RemoteBox
import androidx.compose.remote.creation.compose.layout.RemoteComposable
import androidx.compose.remote.creation.compose.modifier.RemoteModifier
import androidx.compose.remote.creation.compose.modifier.fillMaxSize
import androidx.compose.remote.creation.compose.modifier.size
import androidx.compose.remote.creation.compose.state.RemoteColor
import androidx.compose.remote.creation.compose.state.RemoteString
import androidx.compose.remote.creation.compose.state.rb
import androidx.compose.remote.creation.compose.state.rdp
import androidx.compose.remote.creation.compose.state.rf
import androidx.compose.remote.creation.compose.state.rs
import androidx.compose.remote.creation.profile.Profile
import androidx.compose.remote.tooling.preview.RemotePreview
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.tooling.preview.PreviewParameter
import androidx.wear.compose.remote.material3.RemoteButton
import androidx.wear.compose.remote.material3.RemoteButtonDefaults
import androidx.wear.compose.remote.material3.RemoteCompactButton
import androidx.wear.compose.remote.material3.RemoteIcon
import androidx.wear.compose.remote.material3.RemoteText
import androidx.wear.compose.remote.material3.buttonSizeModifier
import androidx.wear.compose.ui.tooling.preview.WearPreviewDevices

@Composable
@RemoteComposable
fun RemoteButtonEnabled() {
    RemoteButton(
        onClick = testAction,
        modifier = RemoteModifier.buttonSizeModifier(),
        enabled = true.rb,
        content = { RemoteText("button_enabled".rs) },
    )
}

@WearPreviewDevices
@Composable
private fun RemoteButtonEnabledPreview(
    @PreviewParameter(ProfilePreviewParameterProvider::class) profile: Profile
) = RemotePreview(profile = profile) { Container { RemoteButtonEnabled() } }

@Composable
@RemoteComposable
fun RemoteButtonWithBorder() {
    RemoteButton(
        onClick = testAction,
        modifier = RemoteModifier.buttonSizeModifier(),
        border = 8.rdp,
        borderColor = RemoteColor(Color.Green),
    ) {
        RemoteText("button_with_border".rs)
    }
}

@WearPreviewDevices
@Composable
private fun RemoteButtonWithBorderPreview(
    @PreviewParameter(ProfilePreviewParameterProvider::class) profile: Profile
) = RemotePreview(profile = profile) { Container { RemoteButtonWithBorder() } }

@Composable
@RemoteComposable
fun RemoteButtonWithSecondaryLabel() {
    RemoteButton(
        onClick = testAction,
        modifier = RemoteModifier.buttonSizeModifier(),
        secondaryLabel = { RemoteText(RemoteString("secondaryLabel")) },
        label = { RemoteText(RemoteString("label")) },
    )
}

@Composable
@RemoteComposable
fun RemoteButtonWithIcon() {
    RemoteButton(
        onClick = testAction,
        modifier = RemoteModifier.buttonSizeModifier(),
        icon = {
            RemoteIcon(
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
private fun RemoteButtonWithIconPreview(
    @PreviewParameter(ProfilePreviewParameterProvider::class) profile: Profile
) = RemotePreview(profile = profile) { Container { RemoteButtonWithIcon() } }

@Composable
@RemoteComposable
fun RemoteButtonWithIconAndSecondaryLabel() {
    RemoteButton(
        onClick = testAction,
        modifier = RemoteModifier.buttonSizeModifier(),
        icon = {
            RemoteIcon(
                imageVector = TestImageVectors.VolumeUp,
                contentDescription = null,
                tint = RemoteButtonDefaults.buttonColors().iconColor,
            )
        },
        secondaryLabel = { RemoteText("secondaryLabel".rs) },
        label = { RemoteText("label".rs) },
    )
}

@WearPreviewDevices
@Composable
private fun RemoteButtonWithIconAndSecondaryLabelPreview(
    @PreviewParameter(ProfilePreviewParameterProvider::class) profile: Profile
) = RemotePreview(profile = profile) { Container { RemoteButtonWithIconAndSecondaryLabel() } }

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
) = RemotePreview(profile = profile) { Container { RemoteCompactButtonWithIcon() } }

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
) = RemotePreview(profile = profile) { Container { RemoteCompactButtonWithLabel() } }

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
) = RemotePreview(profile = profile) { Container { RemoteCompactButtonWithIconAndLabel() } }

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

private val testAction = HostAction("testAction".rs, 1.rf)
