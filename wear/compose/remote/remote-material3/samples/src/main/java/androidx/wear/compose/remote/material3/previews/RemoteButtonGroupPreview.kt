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

import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Call
import androidx.compose.material.icons.filled.Favorite
import androidx.compose.material.icons.filled.MailOutline
import androidx.compose.remote.creation.compose.action.HostAction
import androidx.compose.remote.creation.compose.layout.RemoteAlignment
import androidx.compose.remote.creation.compose.layout.RemoteBox
import androidx.compose.remote.creation.compose.layout.RemoteComposable
import androidx.compose.remote.creation.compose.modifier.RemoteModifier
import androidx.compose.remote.creation.compose.modifier.fillMaxSize
import androidx.compose.remote.creation.compose.modifier.fillMaxWidth
import androidx.compose.remote.creation.compose.modifier.size
import androidx.compose.remote.creation.compose.modifier.widthIn
import androidx.compose.remote.creation.compose.state.rb
import androidx.compose.remote.creation.compose.state.rf
import androidx.compose.remote.creation.compose.state.rs
import androidx.compose.remote.creation.profile.Profile
import androidx.compose.remote.tooling.preview.RemotePreview
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.tooling.preview.PreviewParameter
import androidx.wear.compose.remote.material3.RemoteButtonDefaults
import androidx.wear.compose.remote.material3.RemoteButtonGroup
import androidx.wear.compose.remote.material3.RemoteButtonGroupDefaults
import androidx.wear.compose.remote.material3.RemoteIcon
import androidx.wear.compose.remote.material3.RemoteIconButton
import androidx.wear.compose.remote.material3.RemoteIconButtonDefaults
import androidx.wear.compose.remote.material3.RemoteMaterialTheme
import androidx.wear.compose.ui.tooling.preview.WearPreviewDevices

@Composable
@RemoteComposable
fun RemoteButtonGroupThreeButtons() {
    RemoteButtonGroup(modifier = RemoteModifier.fillMaxWidth()) {
        Button(Icons.Filled.MailOutline, RemoteModifier.weight(1f))
        Button(Icons.Filled.Favorite, RemoteModifier.weight(1.5f))
        Button(Icons.Filled.Call, RemoteModifier.weight(1f))
    }
}

@WearPreviewDevices
@Composable
private fun RemoteButtonGroupThreeButtonsPreview(
    @PreviewParameter(ProfilePreviewParameterProvider::class) profile: Profile
) = RemotePreview(profile = profile) { Container { RemoteButtonGroupThreeButtons() } }

@Composable
@RemoteComposable
fun RemoteButtonGroupTwoButtons() {
    RemoteButtonGroup(modifier = RemoteModifier.fillMaxWidth()) {
        Button(Icons.Filled.MailOutline, RemoteModifier.weight(1f))
        Button(Icons.Filled.Call, RemoteModifier.weight(1f))
    }
}

@WearPreviewDevices
@Composable
private fun RemoteButtonGroupTwoButtonsPreview(
    @PreviewParameter(ProfilePreviewParameterProvider::class) profile: Profile
) = RemotePreview(profile = profile) { Container { RemoteButtonGroupTwoButtons() } }

@Composable
@RemoteComposable
private fun Button(imageVector: ImageVector, modifier: RemoteModifier) {
    RemoteIconButton(
        testAction,
        modifier = modifier.widthIn(RemoteButtonGroupDefaults.MinWidth),
        enabled = true.rb,
        colors = tonalColors,
        shape = RemoteButtonDefaults.shape,
    ) {
        RemoteIcon(
            modifier = RemoteModifier.size(RemoteIconButtonDefaults.SmallIconSize),
            imageVector = imageVector,
            contentDescription = null,
        )
    }
}

@Composable
@RemoteComposable
private fun Spacer() {
    RemoteBox(RemoteModifier.size(RemoteButtonGroupDefaults.Spacing))
}

@Composable
@RemoteComposable
private fun Container(
    modifier: RemoteModifier = RemoteModifier.fillMaxSize(),
    content: @Composable @RemoteComposable () -> Unit,
) {
    RemoteBox(modifier, contentAlignment = RemoteAlignment.Center, content = content)
}

private val tonalColors
    @Composable
    get() =
        RemoteIconButtonDefaults.iconButtonColors()
            .copy(
                containerColor = RemoteMaterialTheme.colorScheme.primary,
                contentColor = RemoteMaterialTheme.colorScheme.onPrimary,
                disabledContainerColor =
                    RemoteMaterialTheme.colorScheme.primary.copy(alpha = 0.12f.rf),
                disabledContentColor = RemoteMaterialTheme.colorScheme.primary.copy(0.38f.rf),
            )

private val testAction = HostAction("testAction".rs, 1.rf)
