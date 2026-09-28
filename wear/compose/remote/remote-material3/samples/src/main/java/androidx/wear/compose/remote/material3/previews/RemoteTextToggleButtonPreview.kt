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
import androidx.compose.remote.creation.compose.modifier.size
import androidx.compose.remote.creation.compose.state.rb
import androidx.compose.remote.creation.compose.state.rs
import androidx.compose.remote.creation.profile.Profile
import androidx.compose.remote.tooling.preview.RemoteContentPreview
import androidx.compose.runtime.Composable
import androidx.compose.ui.tooling.preview.PreviewParameter
import androidx.wear.compose.remote.material3.RemoteText
import androidx.wear.compose.remote.material3.RemoteTextToggleButton
import androidx.wear.compose.remote.material3.RemoteTextToggleButtonDefaults
import androidx.wear.compose.remote.material3.previews.utils.ProfilePreviewParameterProvider
import androidx.wear.compose.ui.tooling.preview.WearPreviewDevices

@WearPreviewDevices
@Composable
public fun RemoteTextToggleButtonCheckedPreview(
    @PreviewParameter(ProfilePreviewParameterProvider::class) profile: Profile
): Unit = RemoteContentPreview(profile = profile) { Container { RemoteTextToggleButtonChecked() } }

@WearPreviewDevices
@Composable
public fun RemoteTextToggleButtonUncheckedPreview(
    @PreviewParameter(ProfilePreviewParameterProvider::class) profile: Profile
): Unit =
    RemoteContentPreview(profile = profile) { Container { RemoteTextToggleButtonUnchecked() } }

@WearPreviewDevices
@Composable
public fun RemoteTextToggleButtonDisabledCheckedPreview(
    @PreviewParameter(ProfilePreviewParameterProvider::class) profile: Profile
): Unit =
    RemoteContentPreview(profile = profile) {
        Container { RemoteTextToggleButtonDisabledChecked() }
    }

@WearPreviewDevices
@Composable
public fun RemoteTextToggleButtonDisabledUncheckedPreview(
    @PreviewParameter(ProfilePreviewParameterProvider::class) profile: Profile
): Unit =
    RemoteContentPreview(profile = profile) {
        Container { RemoteTextToggleButtonDisabledUnchecked() }
    }

@WearPreviewDevices
@Composable
public fun RemoteTextToggleButtonVariantCheckedPreview(
    @PreviewParameter(ProfilePreviewParameterProvider::class) profile: Profile
): Unit =
    RemoteContentPreview(profile = profile) { Container { RemoteTextToggleButtonVariantChecked() } }

@WearPreviewDevices
@Composable
public fun RemoteTextToggleButtonVariantUncheckedPreview(
    @PreviewParameter(ProfilePreviewParameterProvider::class) profile: Profile
): Unit =
    RemoteContentPreview(profile = profile) {
        Container { RemoteTextToggleButtonVariantUnchecked() }
    }

@WearPreviewDevices
@Composable
public fun RemoteTextToggleButtonLargePreview(
    @PreviewParameter(ProfilePreviewParameterProvider::class) profile: Profile
): Unit = RemoteContentPreview(profile = profile) { Container { RemoteTextToggleButtonLarge() } }

@WearPreviewDevices
@Composable
public fun RemoteTextToggleButtonExtraLargePreview(
    @PreviewParameter(ProfilePreviewParameterProvider::class) profile: Profile
): Unit =
    RemoteContentPreview(profile = profile) { Container { RemoteTextToggleButtonExtraLarge() } }

@Composable
@RemoteComposable
public fun RemoteTextToggleButtonChecked() {
    RemoteTextToggleButton(checked = true.rb, onCheckedChange = Action.Empty) { ToggleText() }
}

@Composable
@RemoteComposable
public fun RemoteTextToggleButtonUnchecked() {
    RemoteTextToggleButton(checked = false.rb, onCheckedChange = Action.Empty) { ToggleText() }
}

@Composable
@RemoteComposable
public fun RemoteTextToggleButtonDisabledChecked() {
    RemoteTextToggleButton(
        checked = true.rb,
        onCheckedChange = Action.Empty,
        enabled = false.rb,
    ) {
        ToggleText()
    }
}

@Composable
@RemoteComposable
public fun RemoteTextToggleButtonDisabledUnchecked() {
    RemoteTextToggleButton(
        checked = false.rb,
        onCheckedChange = Action.Empty,
        enabled = false.rb,
    ) {
        ToggleText()
    }
}

@Composable
@RemoteComposable
public fun RemoteTextToggleButtonVariantChecked() {
    RemoteTextToggleButton(
        checked = true.rb,
        onCheckedChange = Action.Empty,
        shapes = RemoteTextToggleButtonDefaults.variantShapes(),
    ) {
        ToggleText()
    }
}

@Composable
@RemoteComposable
public fun RemoteTextToggleButtonVariantUnchecked() {
    RemoteTextToggleButton(
        checked = false.rb,
        onCheckedChange = Action.Empty,
        shapes = RemoteTextToggleButtonDefaults.variantShapes(),
    ) {
        ToggleText()
    }
}

@Composable
@RemoteComposable
public fun RemoteTextToggleButtonLarge() {
    RemoteTextToggleButton(
        checked = true.rb,
        onCheckedChange = Action.Empty,
        modifier = RemoteModifier.size(RemoteTextToggleButtonDefaults.LargeSize),
    ) {
        RemoteText("ABC".rs, style = RemoteTextToggleButtonDefaults.largeTextStyle)
    }
}

@Composable
@RemoteComposable
public fun RemoteTextToggleButtonExtraLarge() {
    RemoteTextToggleButton(
        checked = true.rb,
        onCheckedChange = Action.Empty,
        modifier = RemoteModifier.size(RemoteTextToggleButtonDefaults.ExtraLargeSize),
    ) {
        RemoteText("ABC".rs, style = RemoteTextToggleButtonDefaults.extraLargeTextStyle)
    }
}

@Composable
@RemoteComposable
private fun ToggleText() {
    RemoteText("ABC".rs)
}

@Composable
@RemoteComposable
private fun Container(
    modifier: RemoteModifier = RemoteModifier.fillMaxSize(),
    content: @Composable @RemoteComposable () -> Unit,
) {
    RemoteBox(modifier, contentAlignment = RemoteAlignment.Center, content = content)
}
