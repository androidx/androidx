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
import androidx.wear.compose.remote.material3.RemoteRadioButton
import androidx.wear.compose.remote.material3.RemoteText
import androidx.wear.compose.remote.material3.previews.utils.ProfilePreviewParameterProvider
import androidx.wear.compose.remote.material3.previews.utils.TestImageVectors
import androidx.wear.compose.ui.tooling.preview.WearPreviewDevices

@WearPreviewDevices
@Composable
public fun RemoteRadioButtonSelectedPreview(
    @PreviewParameter(ProfilePreviewParameterProvider::class) profile: Profile
): Unit = RemoteContentPreview(profile = profile) { Container { RemoteRadioButtonSelected() } }

@WearPreviewDevices
@Composable
public fun RemoteRadioButtonUnselectedPreview(
    @PreviewParameter(ProfilePreviewParameterProvider::class) profile: Profile
): Unit = RemoteContentPreview(profile = profile) { Container { RemoteRadioButtonUnselected() } }

@WearPreviewDevices
@Composable
public fun RemoteRadioButtonDisabledSelectedPreview(
    @PreviewParameter(ProfilePreviewParameterProvider::class) profile: Profile
): Unit =
    RemoteContentPreview(profile = profile) { Container { RemoteRadioButtonDisabledSelected() } }

@WearPreviewDevices
@Composable
public fun RemoteRadioButtonDisabledUnselectedPreview(
    @PreviewParameter(ProfilePreviewParameterProvider::class) profile: Profile
): Unit =
    RemoteContentPreview(profile = profile) { Container { RemoteRadioButtonDisabledUnselected() } }

@WearPreviewDevices
@Composable
public fun RemoteRadioButtonWithIconPreview(
    @PreviewParameter(ProfilePreviewParameterProvider::class) profile: Profile
): Unit = RemoteContentPreview(profile = profile) { Container { RemoteRadioButtonWithIcon() } }

@WearPreviewDevices
@Composable
public fun RemoteRadioButtonWithSecondaryLabelPreview(
    @PreviewParameter(ProfilePreviewParameterProvider::class) profile: Profile
): Unit =
    RemoteContentPreview(profile = profile) { Container { RemoteRadioButtonWithSecondaryLabel() } }

@Composable
@RemoteComposable
public fun RemoteRadioButtonSelected() {
    RemoteRadioButton(
        selected = true.rb,
        onSelect = Action.Empty,
        label = { RemoteText("Selected".rs) },
    )
}

@Composable
@RemoteComposable
public fun RemoteRadioButtonUnselected() {
    RemoteRadioButton(
        selected = false.rb,
        onSelect = Action.Empty,
        label = { RemoteText("Unselected".rs) },
    )
}

@Composable
@RemoteComposable
public fun RemoteRadioButtonDisabledSelected() {
    RemoteRadioButton(
        selected = true.rb,
        onSelect = Action.Empty,
        enabled = false.rb,
        label = { RemoteText("Disabled Selected".rs) },
    )
}

@Composable
@RemoteComposable
public fun RemoteRadioButtonDisabledUnselected() {
    RemoteRadioButton(
        selected = false.rb,
        onSelect = Action.Empty,
        enabled = false.rb,
        label = { RemoteText("Disabled Unselected".rs) },
    )
}

@Composable
@RemoteComposable
public fun RemoteRadioButtonWithIcon() {
    RemoteRadioButton(
        selected = true.rb,
        onSelect = Action.Empty,
        icon = { RemoteIcon(TestImageVectors.VolumeUp, contentDescription = null) },
        label = { RemoteText("With Icon".rs) },
    )
}

@Composable
@RemoteComposable
public fun RemoteRadioButtonWithSecondaryLabel() {
    RemoteRadioButton(
        selected = true.rb,
        onSelect = Action.Empty,
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
