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

import androidx.compose.remote.creation.compose.action.Action
import androidx.compose.remote.creation.compose.layout.RemoteAlignment
import androidx.compose.remote.creation.compose.layout.RemoteBox
import androidx.compose.remote.creation.compose.layout.RemoteComposable
import androidx.compose.remote.creation.compose.modifier.RemoteModifier
import androidx.compose.remote.creation.compose.modifier.fillMaxSize
import androidx.compose.remote.creation.compose.state.rb
import androidx.compose.remote.creation.compose.state.rs
import androidx.compose.remote.creation.profile.Profile
import androidx.compose.remote.tooling.preview.RemoteContentPreview
import androidx.compose.runtime.Composable
import androidx.compose.ui.tooling.preview.PreviewParameter
import androidx.wear.compose.remote.material3.RemoteIcon
import androidx.wear.compose.remote.material3.RemoteSwitchButton
import androidx.wear.compose.remote.material3.RemoteText
import androidx.wear.compose.remote.material3.previews.utils.ProfilePreviewParameterProvider
import androidx.wear.compose.remote.material3.previews.utils.TestImageVectors
import androidx.wear.compose.ui.tooling.preview.WearPreviewDevices

@WearPreviewDevices
@Composable
public fun RemoteSwitchButtonCheckedPreview(
    @PreviewParameter(ProfilePreviewParameterProvider::class) profile: Profile
): Unit = RemoteContentPreview(profile = profile) { Container { RemoteSwitchButtonChecked() } }

@WearPreviewDevices
@Composable
public fun RemoteSwitchButtonUncheckedPreview(
    @PreviewParameter(ProfilePreviewParameterProvider::class) profile: Profile
): Unit = RemoteContentPreview(profile = profile) { Container { RemoteSwitchButtonUnchecked() } }

@WearPreviewDevices
@Composable
public fun RemoteSwitchButtonDisabledCheckedPreview(
    @PreviewParameter(ProfilePreviewParameterProvider::class) profile: Profile
): Unit =
    RemoteContentPreview(profile = profile) { Container { RemoteSwitchButtonDisabledChecked() } }

@WearPreviewDevices
@Composable
public fun RemoteSwitchButtonDisabledUncheckedPreview(
    @PreviewParameter(ProfilePreviewParameterProvider::class) profile: Profile
): Unit =
    RemoteContentPreview(profile = profile) { Container { RemoteSwitchButtonDisabledUnchecked() } }

@WearPreviewDevices
@Composable
public fun RemoteSwitchButtonWithIconPreview(
    @PreviewParameter(ProfilePreviewParameterProvider::class) profile: Profile
): Unit = RemoteContentPreview(profile = profile) { Container { RemoteSwitchButtonWithIcon() } }

@WearPreviewDevices
@Composable
public fun RemoteSwitchButtonWithSecondaryLabelPreview(
    @PreviewParameter(ProfilePreviewParameterProvider::class) profile: Profile
): Unit =
    RemoteContentPreview(profile = profile) { Container { RemoteSwitchButtonWithSecondaryLabel() } }

@Composable
@RemoteComposable
public fun RemoteSwitchButtonChecked() {
    RemoteSwitchButton(
        checked = true.rb,
        onCheckedChange = Action.Empty,
        label = { RemoteText("Checked".rs) },
    )
}

@Composable
@RemoteComposable
public fun RemoteSwitchButtonUnchecked() {
    RemoteSwitchButton(
        checked = false.rb,
        onCheckedChange = Action.Empty,
        label = { RemoteText("Unchecked".rs) },
    )
}

@Composable
@RemoteComposable
public fun RemoteSwitchButtonDisabledChecked() {
    RemoteSwitchButton(
        checked = true.rb,
        onCheckedChange = Action.Empty,
        enabled = false.rb,
        label = { RemoteText("Disabled Checked".rs) },
    )
}

@Composable
@RemoteComposable
public fun RemoteSwitchButtonDisabledUnchecked() {
    RemoteSwitchButton(
        checked = false.rb,
        onCheckedChange = Action.Empty,
        enabled = false.rb,
        label = { RemoteText("Disabled Unchecked".rs) },
    )
}

@Composable
@RemoteComposable
public fun RemoteSwitchButtonWithIcon() {
    RemoteSwitchButton(
        checked = true.rb,
        onCheckedChange = Action.Empty,
        icon = { RemoteIcon(TestImageVectors.VolumeUp, contentDescription = null) },
        label = { RemoteText("With Icon".rs) },
    )
}

@Composable
@RemoteComposable
public fun RemoteSwitchButtonWithSecondaryLabel() {
    RemoteSwitchButton(
        checked = true.rb,
        onCheckedChange = Action.Empty,
        label = { RemoteText("Main Label".rs) },
        secondaryLabel = { RemoteText("Secondary".rs) },
    )
}

@Composable
@RemoteComposable
private fun Container(
    modifier: RemoteModifier = RemoteModifier.fillMaxSize(),
    content: @Composable @RemoteComposable () -> Unit,
) {
    RemoteBox(modifier, contentAlignment = RemoteAlignment.Center, content = content)
}
