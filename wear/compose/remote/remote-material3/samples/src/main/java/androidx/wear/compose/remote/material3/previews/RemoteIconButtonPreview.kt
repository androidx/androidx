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

import androidx.compose.remote.creation.compose.action.hostAction
import androidx.compose.remote.creation.compose.layout.RemoteAlignment
import androidx.compose.remote.creation.compose.layout.RemoteBox
import androidx.compose.remote.creation.compose.layout.RemoteComposable
import androidx.compose.remote.creation.compose.modifier.RemoteModifier
import androidx.compose.remote.creation.compose.modifier.fillMaxSize
import androidx.compose.remote.creation.compose.modifier.size
import androidx.compose.remote.creation.compose.state.rb
import androidx.compose.remote.creation.compose.state.rdp
import androidx.compose.remote.creation.compose.state.rf
import androidx.compose.remote.creation.compose.state.rs
import androidx.compose.remote.creation.profile.Profile
import androidx.compose.remote.tooling.preview.RemoteContentPreview
import androidx.compose.runtime.Composable
import androidx.compose.ui.tooling.preview.PreviewParameter
import androidx.wear.compose.remote.material3.RemoteIcon
import androidx.wear.compose.remote.material3.RemoteIconButton
import androidx.wear.compose.remote.material3.RemoteIconButtonDefaults
import androidx.wear.compose.remote.material3.RemoteMaterialTheme
import androidx.wear.compose.remote.material3.previews.utils.ProfilePreviewParameterProvider
import androidx.wear.compose.remote.material3.previews.utils.TestImageVectors
import androidx.wear.compose.ui.tooling.preview.WearPreviewDevices

@Composable
@RemoteComposable
fun RemoteIconButtonEnabled() {
    RemoteIconButton(testAction, enabled = true.rb) {
        RemoteIcon(
            imageVector = TestImageVectors.VolumeUp,
            contentDescription = null,
            modifier = RemoteModifier.size(RemoteIconButtonDefaults.DefaultIconSize),
        )
    }
}

@WearPreviewDevices
@Composable
private fun RemoteIconButtonEnabledPreview(
    @PreviewParameter(ProfilePreviewParameterProvider::class) profile: Profile
) = RemoteContentPreview(profile = profile) { Container { RemoteIconButtonEnabled() } }

@Composable
@RemoteComposable
fun RemoteIconButtonLarge() {
    RemoteIconButton(
        testAction,
        modifier = RemoteModifier.size(RemoteIconButtonDefaults.LargeButtonSize),
        enabled = true.rb,
    ) {
        RemoteIcon(
            imageVector = TestImageVectors.VolumeUp,
            contentDescription = null,
            modifier = RemoteModifier.size(RemoteIconButtonDefaults.LargeIconSize),
        )
    }
}

@WearPreviewDevices
@Composable
private fun RemoteIconButtonLargePreview(
    @PreviewParameter(ProfilePreviewParameterProvider::class) profile: Profile
) = RemoteContentPreview(profile = profile) { Container { RemoteIconButtonLarge() } }

@Composable
@RemoteComposable
fun RemoteIconButtonSmall() {
    RemoteIconButton(
        testAction,
        modifier = RemoteModifier.size(RemoteIconButtonDefaults.SmallButtonSize),
        enabled = true.rb,
    ) {
        RemoteIcon(
            imageVector = TestImageVectors.VolumeUp,
            contentDescription = null,
            modifier = RemoteModifier.size(RemoteIconButtonDefaults.SmallIconSize),
        )
    }
}

@WearPreviewDevices
@Composable
private fun RemoteIconButtonSmallPreview(
    @PreviewParameter(ProfilePreviewParameterProvider::class) profile: Profile
) = RemoteContentPreview(profile = profile) { Container { RemoteIconButtonSmall() } }

@Composable
@RemoteComposable
fun RemoteIconButtonExtraSmall() {
    RemoteIconButton(
        testAction,
        modifier = RemoteModifier.size(RemoteIconButtonDefaults.ExtraSmallButtonSize),
        enabled = true.rb,
    ) {
        RemoteIcon(
            imageVector = TestImageVectors.VolumeUp,
            contentDescription = null,
            modifier = RemoteModifier.size(RemoteIconButtonDefaults.SmallIconSize),
        )
    }
}

@WearPreviewDevices
@Composable
private fun RemoteIconButtonExtraSmallPreview(
    @PreviewParameter(ProfilePreviewParameterProvider::class) profile: Profile
) = RemoteContentPreview(profile = profile) { Container { RemoteIconButtonExtraSmall() } }

@Composable
@RemoteComposable
fun RemoteIconButtonFilled() {
    RemoteIconButton(
        testAction,
        enabled = true.rb,
        colors = RemoteIconButtonDefaults.filledIconButtonColors(),
    ) {
        RemoteIcon(
            imageVector = TestImageVectors.VolumeUp,
            contentDescription = null,
            modifier = RemoteModifier.size(RemoteIconButtonDefaults.DefaultIconSize),
        )
    }
}

@WearPreviewDevices
@Composable
private fun RemoteIconButtonFilledPreview(
    @PreviewParameter(ProfilePreviewParameterProvider::class) profile: Profile
) = RemoteContentPreview(profile = profile) { Container { RemoteIconButtonFilled() } }

@Composable
@RemoteComposable
fun RemoteIconButtonTonal() {
    RemoteIconButton(
        testAction,
        enabled = true.rb,
        colors = RemoteIconButtonDefaults.filledTonalIconButtonColors(),
    ) {
        RemoteIcon(
            imageVector = TestImageVectors.VolumeUp,
            contentDescription = null,
            modifier = RemoteModifier.size(RemoteIconButtonDefaults.DefaultIconSize),
        )
    }
}

@WearPreviewDevices
@Composable
private fun RemoteIconButtonTonalPreview(
    @PreviewParameter(ProfilePreviewParameterProvider::class) profile: Profile
) = RemoteContentPreview(profile = profile) { Container { RemoteIconButtonTonal() } }

@Composable
@RemoteComposable
fun RemoteIconButtonOutlined() {
    RemoteIconButton(
        testAction,
        border = 1.rdp,
        borderColor = RemoteMaterialTheme.colorScheme.outline,
        enabled = true.rb,
        colors = RemoteIconButtonDefaults.outlinedIconButtonColors(),
    ) {
        RemoteIcon(
            imageVector = TestImageVectors.VolumeUp,
            contentDescription = null,
            modifier = RemoteModifier.size(RemoteIconButtonDefaults.DefaultIconSize),
        )
    }
}

@WearPreviewDevices
@Composable
private fun RemoteIconButtonOutlinedPreview(
    @PreviewParameter(ProfilePreviewParameterProvider::class) profile: Profile
) = RemoteContentPreview(profile = profile) { Container { RemoteIconButtonOutlined() } }

@Composable
@RemoteComposable
private fun Container(
    modifier: RemoteModifier = RemoteModifier.fillMaxSize(),
    content: @Composable @RemoteComposable () -> Unit,
) {
    RemoteBox(modifier, contentAlignment = RemoteAlignment.Center, content = content)
}

private val testAction = hostAction("testAction".rs, 1.rf)
