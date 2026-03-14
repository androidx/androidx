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
import androidx.compose.remote.creation.compose.layout.RemoteBox
import androidx.compose.remote.creation.compose.layout.RemoteComposable
import androidx.compose.remote.creation.compose.modifier.RemoteModifier
import androidx.compose.remote.creation.compose.modifier.fillMaxSize
import androidx.compose.remote.creation.compose.state.RemoteColor
import androidx.compose.remote.creation.compose.state.rb
import androidx.compose.remote.creation.compose.state.rdp
import androidx.compose.remote.creation.compose.state.rf
import androidx.compose.remote.creation.compose.state.rs
import androidx.compose.remote.creation.profile.Profile
import androidx.compose.remote.tooling.preview.RemotePreview
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.tooling.preview.PreviewParameter
import androidx.wear.compose.remote.material3.RemoteMaterialTheme
import androidx.wear.compose.remote.material3.RemoteText
import androidx.wear.compose.remote.material3.RemoteTextButton
import androidx.wear.compose.remote.material3.RemoteTextButtonDefaults
import androidx.wear.compose.ui.tooling.preview.WearPreviewDevices

@Composable
@RemoteComposable
fun RemoteTextButtonEnabled() {
    RemoteTextButton(testAction, enabled = true.rb) { RemoteText("ABC".rs) }
}

@WearPreviewDevices
@Composable
private fun RemoteTextButtonEnabledPreview(
    @PreviewParameter(ProfilePreviewParameterProvider::class) profile: Profile
) = RemotePreview(profile = profile) { Container { RemoteTextButtonEnabled() } }

@Composable
@RemoteComposable
fun RemoteTextButtonTonal() {
    RemoteTextButton(testAction, enabled = true.rb, colors = filledTonalColor()) {
        RemoteText("ABC".rs)
    }
}

@WearPreviewDevices
@Composable
private fun RemoteTextButtonTonalPreview(
    @PreviewParameter(ProfilePreviewParameterProvider::class) profile: Profile
) = RemotePreview(profile = profile) { Container { RemoteTextButtonTonal() } }

@Composable
@RemoteComposable
fun RemoteTextButtonOutline() {
    RemoteTextButton(
        testAction,
        border = 1.rdp,
        borderColor = RemoteMaterialTheme.colorScheme.outline,
        enabled = true.rb,
        colors = outlineColor(),
    ) {
        RemoteText("ABC".rs)
    }
}

@WearPreviewDevices
@Composable
private fun RemoteTextButtonOutlinePreview(
    @PreviewParameter(ProfilePreviewParameterProvider::class) profile: Profile
) = RemotePreview(profile = profile) { Container { RemoteTextButtonOutline() } }

@Composable
private fun filledTonalColor() =
    RemoteTextButtonDefaults.textButtonColors()
        .copy(
            containerColor = RemoteMaterialTheme.colorScheme.primary,
            contentColor = RemoteMaterialTheme.colorScheme.onPrimary,
            disabledContainerColor = RemoteMaterialTheme.colorScheme.primary.copy(alpha = 0.12f.rf),
            disabledContentColor = RemoteMaterialTheme.colorScheme.primary.copy(0.38f.rf),
        )

@Composable
private fun outlineColor() =
    RemoteTextButtonDefaults.textButtonColors()
        .copy(
            containerColor = RemoteColor(Color.Transparent),
            contentColor = RemoteMaterialTheme.colorScheme.onPrimary,
            disabledContainerColor = RemoteColor(Color.Transparent),
            disabledContentColor = RemoteMaterialTheme.colorScheme.primary.copy(0.38f.rf),
        )

@Composable
@RemoteComposable
private fun Container(
    modifier: RemoteModifier = RemoteModifier.fillMaxSize(),
    content: @Composable @RemoteComposable () -> Unit,
) {
    RemoteBox(modifier, contentAlignment = RemoteAlignment.Center, content = content)
}

private val testAction = HostAction("testAction".rs, 1.rf)
