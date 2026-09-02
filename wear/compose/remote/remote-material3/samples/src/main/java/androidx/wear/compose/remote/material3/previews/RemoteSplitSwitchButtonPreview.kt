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

package androidx.wear.compose.remote.material3.previews

import androidx.compose.remote.creation.compose.action.hostAction
import androidx.compose.remote.creation.compose.layout.RemoteAlignment
import androidx.compose.remote.creation.compose.layout.RemoteBox
import androidx.compose.remote.creation.compose.layout.RemoteComposable
import androidx.compose.remote.creation.compose.modifier.RemoteModifier
import androidx.compose.remote.creation.compose.modifier.fillMaxSize
import androidx.compose.remote.creation.compose.state.rb
import androidx.compose.remote.creation.compose.state.rf
import androidx.compose.remote.creation.compose.state.rs
import androidx.compose.remote.creation.profile.Profile
import androidx.compose.remote.tooling.preview.RemoteContentPreview
import androidx.compose.runtime.Composable
import androidx.compose.ui.tooling.preview.PreviewParameter
import androidx.wear.compose.remote.material3.RemoteSplitSwitchButton
import androidx.wear.compose.remote.material3.RemoteText
import androidx.wear.compose.remote.material3.previews.utils.ProfilePreviewParameterProvider
import androidx.wear.compose.ui.tooling.preview.WearPreviewDevices

@Composable
@RemoteComposable
fun RemoteSplitSwitchButtonChecked() {
    RemoteSplitSwitchButton(
        checked = true.rb,
        onCheckedChange = testAction,
        toggleContentDescription = "Checked".rs,
        onContainerClick = testAction,
        label = { RemoteText("Checked".rs) },
    )
}

@WearPreviewDevices
@Composable
private fun RemoteSplitSwitchButtonCheckedPreview(
    @PreviewParameter(ProfilePreviewParameterProvider::class) profile: Profile
) = RemoteContentPreview(profile = profile) { Container { RemoteSplitSwitchButtonChecked() } }

@Composable
@RemoteComposable
fun RemoteSplitSwitchButtonUnchecked() {
    RemoteSplitSwitchButton(
        checked = false.rb,
        onCheckedChange = testAction,
        toggleContentDescription = "Unchecked".rs,
        onContainerClick = testAction,
        label = { RemoteText("Unchecked".rs) },
    )
}

@WearPreviewDevices
@Composable
private fun RemoteSplitSwitchButtonUncheckedPreview(
    @PreviewParameter(ProfilePreviewParameterProvider::class) profile: Profile
) = RemoteContentPreview(profile = profile) { Container { RemoteSplitSwitchButtonUnchecked() } }

@Composable
@RemoteComposable
fun RemoteSplitSwitchButtonDisabledChecked() {
    RemoteSplitSwitchButton(
        checked = true.rb,
        enabled = false.rb,
        onCheckedChange = testAction,
        toggleContentDescription = "Disabled Checked".rs,
        onContainerClick = testAction,
        label = { RemoteText("Disabled Checked".rs) },
    )
}

@WearPreviewDevices
@Composable
private fun RemoteSplitSwitchButtonDisabledCheckedPreview(
    @PreviewParameter(ProfilePreviewParameterProvider::class) profile: Profile
) =
    RemoteContentPreview(profile = profile) {
        Container { RemoteSplitSwitchButtonDisabledChecked() }
    }

@Composable
@RemoteComposable
fun RemoteSplitSwitchButtonDisabledUnchecked() {
    RemoteSplitSwitchButton(
        checked = false.rb,
        enabled = false.rb,
        onCheckedChange = testAction,
        toggleContentDescription = "Disabled Unchecked".rs,
        onContainerClick = testAction,
        label = { RemoteText("Disabled Unchecked".rs) },
    )
}

@WearPreviewDevices
@Composable
private fun RemoteSplitSwitchButtonDisabledUncheckedPreview(
    @PreviewParameter(ProfilePreviewParameterProvider::class) profile: Profile
) =
    RemoteContentPreview(profile = profile) {
        Container { RemoteSplitSwitchButtonDisabledUnchecked() }
    }

@Composable
@RemoteComposable
fun RemoteSplitSwitchButtonWithSecondaryLabel() {
    RemoteSplitSwitchButton(
        checked = true.rb,
        onCheckedChange = testAction,
        toggleContentDescription = "With secondary label".rs,
        onContainerClick = testAction,
        label = { RemoteText("Primary label".rs) },
        secondaryLabel = { RemoteText("Secondary label".rs) },
    )
}

@WearPreviewDevices
@Composable
private fun RemoteSplitSwitchButtonWithSecondaryLabelPreview(
    @PreviewParameter(ProfilePreviewParameterProvider::class) profile: Profile
) =
    RemoteContentPreview(profile = profile) {
        Container { RemoteSplitSwitchButtonWithSecondaryLabel() }
    }

@Composable
@RemoteComposable
private fun Container(
    modifier: RemoteModifier = RemoteModifier.fillMaxSize(),
    content: @Composable @RemoteComposable () -> Unit,
) {
    RemoteBox(modifier, contentAlignment = RemoteAlignment.Center, content = content)
}

private val testAction = hostAction("testAction".rs, 1.rf)
