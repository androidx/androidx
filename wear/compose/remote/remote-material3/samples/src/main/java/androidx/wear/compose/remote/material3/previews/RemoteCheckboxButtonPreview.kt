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
import androidx.wear.compose.remote.material3.RemoteCheckboxButton
import androidx.wear.compose.remote.material3.RemoteIcon
import androidx.wear.compose.remote.material3.RemoteText
import androidx.wear.compose.remote.material3.previews.utils.ProfilePreviewParameterProvider
import androidx.wear.compose.remote.material3.previews.utils.TestImageVectors
import androidx.wear.compose.ui.tooling.preview.WearPreviewDevices

@WearPreviewDevices
@Composable
public fun RemoteCheckboxButtonCheckedPreview(
    @PreviewParameter(ProfilePreviewParameterProvider::class) profile: Profile
): Unit = RemoteContentPreview(profile = profile) { Container { RemoteCheckboxButtonChecked() } }

@WearPreviewDevices
@Composable
public fun RemoteCheckboxButtonUncheckedPreview(
    @PreviewParameter(ProfilePreviewParameterProvider::class) profile: Profile
): Unit = RemoteContentPreview(profile = profile) { Container { RemoteCheckboxButtonUnchecked() } }

@WearPreviewDevices
@Composable
public fun RemoteCheckboxButtonDisabledCheckedPreview(
    @PreviewParameter(ProfilePreviewParameterProvider::class) profile: Profile
): Unit =
    RemoteContentPreview(profile = profile) { Container { RemoteCheckboxButtonDisabledChecked() } }

@WearPreviewDevices
@Composable
public fun RemoteCheckboxButtonDisabledUncheckedPreview(
    @PreviewParameter(ProfilePreviewParameterProvider::class) profile: Profile
): Unit =
    RemoteContentPreview(profile = profile) {
        Container { RemoteCheckboxButtonDisabledUnchecked() }
    }

@WearPreviewDevices
@Composable
public fun RemoteCheckboxButtonWithIconPreview(
    @PreviewParameter(ProfilePreviewParameterProvider::class) profile: Profile
): Unit = RemoteContentPreview(profile = profile) { Container { RemoteCheckboxButtonWithIcon() } }

@WearPreviewDevices
@Composable
public fun RemoteCheckboxButtonWithSecondaryLabelPreview(
    @PreviewParameter(ProfilePreviewParameterProvider::class) profile: Profile
): Unit =
    RemoteContentPreview(profile = profile) {
        Container { RemoteCheckboxButtonWithSecondaryLabel() }
    }

@Composable
@RemoteComposable
public fun RemoteCheckboxButtonChecked() {
    RemoteCheckboxButton(
        checked = true.rb,
        onCheckedChange = Action.Empty,
        label = { RemoteText("Checked".rs) },
    )
}

@Composable
@RemoteComposable
public fun RemoteCheckboxButtonUnchecked() {
    RemoteCheckboxButton(
        checked = false.rb,
        onCheckedChange = Action.Empty,
        label = { RemoteText("Unchecked".rs) },
    )
}

@Composable
@RemoteComposable
public fun RemoteCheckboxButtonDisabledChecked() {
    RemoteCheckboxButton(
        checked = true.rb,
        onCheckedChange = Action.Empty,
        enabled = false.rb,
        label = { RemoteText("Disabled Checked".rs) },
    )
}

@Composable
@RemoteComposable
public fun RemoteCheckboxButtonDisabledUnchecked() {
    RemoteCheckboxButton(
        checked = false.rb,
        onCheckedChange = Action.Empty,
        enabled = false.rb,
        label = { RemoteText("Disabled Unchecked".rs) },
    )
}

@Composable
@RemoteComposable
public fun RemoteCheckboxButtonWithIcon() {
    RemoteCheckboxButton(
        checked = true.rb,
        onCheckedChange = Action.Empty,
        icon = { RemoteIcon(TestImageVectors.VolumeUp, contentDescription = null) },
        label = { RemoteText("With Icon".rs) },
    )
}

@Composable
@RemoteComposable
public fun RemoteCheckboxButtonWithSecondaryLabel() {
    RemoteCheckboxButton(
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
