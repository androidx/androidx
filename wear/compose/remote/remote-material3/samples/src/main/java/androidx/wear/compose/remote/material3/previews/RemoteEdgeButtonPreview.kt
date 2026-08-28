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

@file:Suppress("RestrictedApiAndroidX")

package androidx.wear.compose.remote.material3.previews

import androidx.compose.remote.creation.compose.action.hostAction
import androidx.compose.remote.creation.compose.layout.RemoteAlignment
import androidx.compose.remote.creation.compose.layout.RemoteBox
import androidx.compose.remote.creation.compose.layout.RemoteComposable
import androidx.compose.remote.creation.compose.modifier.RemoteModifier
import androidx.compose.remote.creation.compose.modifier.fillMaxSize
import androidx.compose.remote.creation.compose.modifier.size
import androidx.compose.remote.creation.compose.state.RemoteColor
import androidx.compose.remote.creation.compose.state.rb
import androidx.compose.remote.creation.compose.state.rdp
import androidx.compose.remote.creation.compose.state.rf
import androidx.compose.remote.creation.compose.state.rs
import androidx.compose.remote.creation.profile.Profile
import androidx.compose.remote.tooling.preview.RemoteContentPreview
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.tooling.preview.PreviewParameter
import androidx.wear.compose.remote.material3.RemoteEdgeButton
import androidx.wear.compose.remote.material3.RemoteEdgeButtonDefaults
import androidx.wear.compose.remote.material3.RemoteEdgeButtonSize
import androidx.wear.compose.remote.material3.RemoteIcon
import androidx.wear.compose.remote.material3.RemoteText
import androidx.wear.compose.remote.material3.previews.utils.ProfilePreviewParameterProvider
import androidx.wear.compose.remote.material3.previews.utils.TestImageVectors
import androidx.wear.compose.ui.tooling.preview.WearPreviewDevices

@Composable
@RemoteComposable
fun RemoteEdgeButtonExtraSmall() {
    RemoteEdgeButton(onClick = testAction, buttonSize = RemoteEdgeButtonSize.ExtraSmall) {
        RemoteText("Extra Small".rs)
    }
}

@WearPreviewDevices
@Composable
private fun RemoteEdgeButtonExtraSmallPreview(
    @PreviewParameter(ProfilePreviewParameterProvider::class) profile: Profile
) = RemoteContentPreview(profile = profile) { Container { RemoteEdgeButtonExtraSmall() } }

@Composable
@RemoteComposable
fun RemoteEdgeButtonSmall() {
    RemoteEdgeButton(onClick = testAction, buttonSize = RemoteEdgeButtonSize.Small) {
        RemoteText("Small".rs)
    }
}

@WearPreviewDevices
@Composable
private fun RemoteEdgeButtonSmallPreview(
    @PreviewParameter(ProfilePreviewParameterProvider::class) profile: Profile
) = RemoteContentPreview(profile = profile) { Container { RemoteEdgeButtonSmall() } }

@Composable
@RemoteComposable
fun RemoteEdgeButtonMedium() {
    RemoteEdgeButton(onClick = testAction, buttonSize = RemoteEdgeButtonSize.Medium) {
        RemoteText("Medium".rs)
    }
}

@WearPreviewDevices
@Composable
private fun RemoteEdgeButtonMediumPreview(
    @PreviewParameter(ProfilePreviewParameterProvider::class) profile: Profile
) = RemoteContentPreview(profile = profile) { Container { RemoteEdgeButtonMedium() } }

@Composable
@RemoteComposable
fun RemoteEdgeButtonLarge() {
    RemoteEdgeButton(onClick = testAction, buttonSize = RemoteEdgeButtonSize.Large) {
        RemoteText("Longer multi-line\nshort text".rs)
    }
}

@WearPreviewDevices
@Composable
private fun RemoteEdgeButtonLargePreview(
    @PreviewParameter(ProfilePreviewParameterProvider::class) profile: Profile
) = RemoteContentPreview(profile = profile) { Container { RemoteEdgeButtonLarge() } }

@Composable
@RemoteComposable
fun RemoteEdgeButtonFilledTonal() {
    RemoteEdgeButton(
        onClick = testAction,
        buttonSize = RemoteEdgeButtonSize.Small,
        colors = RemoteEdgeButtonDefaults.filledTonalButtonColors(),
    ) {
        RemoteText("Filled Tonal".rs)
    }
}

@WearPreviewDevices
@Composable
private fun RemoteEdgeButtonFilledTonalPreview(
    @PreviewParameter(ProfilePreviewParameterProvider::class) profile: Profile
) = RemoteContentPreview(profile = profile) { Container { RemoteEdgeButtonFilledTonal() } }

@Composable
@RemoteComposable
fun RemoteEdgeButtonFilledVariant() {
    RemoteEdgeButton(
        onClick = testAction,
        buttonSize = RemoteEdgeButtonSize.Small,
        colors = RemoteEdgeButtonDefaults.filledVariantButtonColors(),
    ) {
        RemoteText("Filled Variant".rs)
    }
}

@WearPreviewDevices
@Composable
private fun RemoteEdgeButtonFilledVariantPreview(
    @PreviewParameter(ProfilePreviewParameterProvider::class) profile: Profile
) = RemoteContentPreview(profile = profile) { Container { RemoteEdgeButtonFilledVariant() } }

@Composable
@RemoteComposable
fun RemoteEdgeButtonOutlined() {
    RemoteEdgeButton(
        onClick = testAction,
        buttonSize = RemoteEdgeButtonSize.Small,
        colors = RemoteEdgeButtonDefaults.outlinedButtonColors(),
        border = 2.rdp,
        borderColor = RemoteColor(Color.Cyan),
    ) {
        RemoteText("Outlined".rs)
    }
}

@WearPreviewDevices
@Composable
private fun RemoteEdgeButtonOutlinedPreview(
    @PreviewParameter(ProfilePreviewParameterProvider::class) profile: Profile
) = RemoteContentPreview(profile = profile) { Container { RemoteEdgeButtonOutlined() } }

@Composable
@RemoteComposable
fun RemoteEdgeButtonWithBorder() {
    RemoteEdgeButton(
        onClick = testAction,
        buttonSize = RemoteEdgeButtonSize.Small,
        border = 2.rdp,
        borderColor = RemoteColor(Color.Green),
    ) {
        RemoteText("With Border".rs)
    }
}

@WearPreviewDevices
@Composable
private fun RemoteEdgeButtonWithBorderPreview(
    @PreviewParameter(ProfilePreviewParameterProvider::class) profile: Profile
) = RemoteContentPreview(profile = profile) { Container { RemoteEdgeButtonWithBorder() } }

@Composable
@RemoteComposable
fun RemoteEdgeButtonDisabled() {
    RemoteEdgeButton(
        onClick = testAction,
        buttonSize = RemoteEdgeButtonSize.Small,
        enabled = false.rb,
    ) {
        RemoteText("Disabled".rs)
    }
}

@WearPreviewDevices
@Composable
private fun RemoteEdgeButtonDisabledPreview(
    @PreviewParameter(ProfilePreviewParameterProvider::class) profile: Profile
) = RemoteContentPreview(profile = profile) { Container { RemoteEdgeButtonDisabled() } }

@Composable
@RemoteComposable
fun RemoteEdgeButtonWithIcon() {
    RemoteEdgeButton(onClick = testAction, buttonSize = RemoteEdgeButtonSize.Small) {
        RemoteIcon(
            imageVector = TestImageVectors.VolumeUp,
            contentDescription = "Volume Up".rs,
            modifier =
                RemoteModifier.size(
                    RemoteEdgeButtonDefaults.iconSizeFor(RemoteEdgeButtonSize.Small)
                ),
        )
    }
}

@WearPreviewDevices
@Composable
private fun RemoteEdgeButtonWithIconPreview(
    @PreviewParameter(ProfilePreviewParameterProvider::class) profile: Profile
) = RemoteContentPreview(profile = profile) { Container { RemoteEdgeButtonWithIcon() } }

@Composable
@RemoteComposable
private fun Container(
    modifier: RemoteModifier = RemoteModifier.fillMaxSize(),
    content: @Composable @RemoteComposable () -> Unit,
) {
    RemoteBox(modifier, contentAlignment = RemoteAlignment.BottomCenter, content = content)
}

private val testAction = hostAction("testAction".rs, 1.rf)
