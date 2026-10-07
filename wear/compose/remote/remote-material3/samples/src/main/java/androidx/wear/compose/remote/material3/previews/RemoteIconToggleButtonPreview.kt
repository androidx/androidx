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
import androidx.compose.remote.creation.compose.state.RemoteDp
import androidx.compose.remote.creation.compose.state.rb
import androidx.compose.remote.creation.profile.Profile
import androidx.compose.remote.tooling.preview.RemoteContentPreview
import androidx.compose.runtime.Composable
import androidx.compose.ui.tooling.preview.PreviewParameter
import androidx.wear.compose.remote.material3.RemoteIcon
import androidx.wear.compose.remote.material3.RemoteIconToggleButton
import androidx.wear.compose.remote.material3.RemoteIconToggleButtonDefaults
import androidx.wear.compose.remote.material3.previews.utils.ProfilePreviewParameterProvider
import androidx.wear.compose.remote.material3.previews.utils.TestImageVectors
import androidx.wear.compose.ui.tooling.preview.WearPreviewDevices

@WearPreviewDevices
@Composable
public fun RemoteIconToggleButtonCheckedPreview(
    @PreviewParameter(ProfilePreviewParameterProvider::class) profile: Profile
): Unit = RemoteContentPreview(profile = profile) { Container { RemoteIconToggleButtonChecked() } }

@WearPreviewDevices
@Composable
public fun RemoteIconToggleButtonUncheckedPreview(
    @PreviewParameter(ProfilePreviewParameterProvider::class) profile: Profile
): Unit =
    RemoteContentPreview(profile = profile) { Container { RemoteIconToggleButtonUnchecked() } }

@WearPreviewDevices
@Composable
public fun RemoteIconToggleButtonDisabledCheckedPreview(
    @PreviewParameter(ProfilePreviewParameterProvider::class) profile: Profile
): Unit =
    RemoteContentPreview(profile = profile) {
        Container { RemoteIconToggleButtonDisabledChecked() }
    }

@WearPreviewDevices
@Composable
public fun RemoteIconToggleButtonDisabledUncheckedPreview(
    @PreviewParameter(ProfilePreviewParameterProvider::class) profile: Profile
): Unit =
    RemoteContentPreview(profile = profile) {
        Container { RemoteIconToggleButtonDisabledUnchecked() }
    }

@WearPreviewDevices
@Composable
public fun RemoteIconToggleButtonVariantCheckedPreview(
    @PreviewParameter(ProfilePreviewParameterProvider::class) profile: Profile
): Unit =
    RemoteContentPreview(profile = profile) { Container { RemoteIconToggleButtonVariantChecked() } }

@WearPreviewDevices
@Composable
public fun RemoteIconToggleButtonVariantUncheckedPreview(
    @PreviewParameter(ProfilePreviewParameterProvider::class) profile: Profile
): Unit =
    RemoteContentPreview(profile = profile) {
        Container { RemoteIconToggleButtonVariantUnchecked() }
    }

@WearPreviewDevices
@Composable
public fun RemoteIconToggleButtonSmallPreview(
    @PreviewParameter(ProfilePreviewParameterProvider::class) profile: Profile
): Unit = RemoteContentPreview(profile = profile) { Container { RemoteIconToggleButtonSmall() } }

@WearPreviewDevices
@Composable
public fun RemoteIconToggleButtonLargePreview(
    @PreviewParameter(ProfilePreviewParameterProvider::class) profile: Profile
): Unit = RemoteContentPreview(profile = profile) { Container { RemoteIconToggleButtonLarge() } }

@WearPreviewDevices
@Composable
public fun RemoteIconToggleButtonExtraLargePreview(
    @PreviewParameter(ProfilePreviewParameterProvider::class) profile: Profile
): Unit =
    RemoteContentPreview(profile = profile) { Container { RemoteIconToggleButtonExtraLarge() } }

@Composable
@RemoteComposable
public fun RemoteIconToggleButtonChecked() {
    RemoteIconToggleButton(checked = true.rb, onCheckedChange = Action.Empty) { ToggleIcon() }
}

@Composable
@RemoteComposable
public fun RemoteIconToggleButtonUnchecked() {
    RemoteIconToggleButton(checked = false.rb, onCheckedChange = Action.Empty) { ToggleIcon() }
}

@Composable
@RemoteComposable
public fun RemoteIconToggleButtonDisabledChecked() {
    RemoteIconToggleButton(
        checked = true.rb,
        onCheckedChange = Action.Empty,
        enabled = false.rb,
    ) {
        ToggleIcon()
    }
}

@Composable
@RemoteComposable
public fun RemoteIconToggleButtonDisabledUnchecked() {
    RemoteIconToggleButton(
        checked = false.rb,
        onCheckedChange = Action.Empty,
        enabled = false.rb,
    ) {
        ToggleIcon()
    }
}

@Composable
@RemoteComposable
public fun RemoteIconToggleButtonVariantChecked() {
    RemoteIconToggleButton(
        checked = true.rb,
        onCheckedChange = Action.Empty,
        shapes = RemoteIconToggleButtonDefaults.variantShapes(),
    ) {
        ToggleIcon()
    }
}

@Composable
@RemoteComposable
public fun RemoteIconToggleButtonVariantUnchecked() {
    RemoteIconToggleButton(
        checked = false.rb,
        onCheckedChange = Action.Empty,
        shapes = RemoteIconToggleButtonDefaults.variantShapes(),
    ) {
        ToggleIcon()
    }
}

@Composable
@RemoteComposable
public fun RemoteIconToggleButtonSmall() {
    RemoteIconToggleButton(
        checked = true.rb,
        onCheckedChange = Action.Empty,
        modifier = RemoteModifier.size(RemoteIconToggleButtonDefaults.SmallSize),
    ) {
        ToggleIcon(RemoteIconToggleButtonDefaults.SmallSize)
    }
}

@Composable
@RemoteComposable
public fun RemoteIconToggleButtonLarge() {
    RemoteIconToggleButton(
        checked = true.rb,
        onCheckedChange = Action.Empty,
        modifier = RemoteModifier.size(RemoteIconToggleButtonDefaults.LargeSize),
    ) {
        ToggleIcon(RemoteIconToggleButtonDefaults.LargeSize)
    }
}

@Composable
@RemoteComposable
public fun RemoteIconToggleButtonExtraLarge() {
    RemoteIconToggleButton(
        checked = true.rb,
        onCheckedChange = Action.Empty,
        modifier = RemoteModifier.size(RemoteIconToggleButtonDefaults.ExtraLargeSize),
    ) {
        ToggleIcon(RemoteIconToggleButtonDefaults.ExtraLargeSize)
    }
}

@Composable
@RemoteComposable
private fun ToggleIcon(buttonSize: RemoteDp = RemoteIconToggleButtonDefaults.Size) {
    RemoteIcon(
        imageVector = TestImageVectors.VolumeUp,
        contentDescription = null,
        modifier = RemoteModifier.size(RemoteIconToggleButtonDefaults.iconSizeFor(buttonSize)),
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
