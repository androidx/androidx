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

import androidx.compose.remote.creation.compose.action.valueChange
import androidx.compose.remote.creation.compose.layout.RemoteAlignment
import androidx.compose.remote.creation.compose.layout.RemoteBox
import androidx.compose.remote.creation.compose.layout.RemoteComposable
import androidx.compose.remote.creation.compose.modifier.RemoteModifier
import androidx.compose.remote.creation.compose.modifier.fillMaxSize
import androidx.compose.remote.creation.compose.modifier.padding
import androidx.compose.remote.creation.compose.state.RemoteColor
import androidx.compose.remote.creation.compose.state.max
import androidx.compose.remote.creation.compose.state.min
import androidx.compose.remote.creation.compose.state.rb
import androidx.compose.remote.creation.compose.state.rdp
import androidx.compose.remote.creation.compose.state.rememberMutableRemoteFloat
import androidx.compose.remote.creation.compose.state.rf
import androidx.compose.remote.creation.profile.Profile
import androidx.compose.remote.tooling.preview.RemoteContentPreview
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.tooling.preview.PreviewParameter
import androidx.wear.compose.remote.material3.RemoteSlider
import androidx.wear.compose.remote.material3.RemoteSliderDefaults
import androidx.wear.compose.remote.material3.previews.utils.ProfilePreviewParameterProvider
import androidx.wear.compose.ui.tooling.preview.WearPreviewDevices

@Composable
@RemoteComposable
public fun RemoteSliderDefault() {
    val value = rememberMutableRemoteFloat(2f)
    RemoteSlider(
        value = value,
        steps = 4,
        decreaseAction = valueChange(value, max(value - 1f.rf, 0f.rf)),
        increaseAction = valueChange(value, min(value + 1f.rf, 5f.rf)),
    )
}

@WearPreviewDevices
@Composable
private fun RemoteSliderDefaultPreview(
    @PreviewParameter(ProfilePreviewParameterProvider::class) profile: Profile
) = RemoteContentPreview(profile = profile) { Container { RemoteSliderDefault() } }

@Composable
@RemoteComposable
public fun RemoteSliderDisabled() {
    RemoteSlider(value = 2f.rf, steps = 4, enabled = false.rb)
}

@WearPreviewDevices
@Composable
private fun RemoteSliderDisabledPreview(
    @PreviewParameter(ProfilePreviewParameterProvider::class) profile: Profile
) = RemoteContentPreview(profile = profile) { Container { RemoteSliderDisabled() } }

@Composable
@RemoteComposable
public fun RemoteSliderNotSegmented() {
    val value = rememberMutableRemoteFloat(2f)
    RemoteSlider(
        value = value,
        steps = 4,
        segmented = false,
        decreaseAction = valueChange(value, max(value - 1f.rf, 0f.rf)),
        increaseAction = valueChange(value, min(value + 1f.rf, 5f.rf)),
    )
}

@WearPreviewDevices
@Composable
private fun RemoteSliderNotSegmentedPreview(
    @PreviewParameter(ProfilePreviewParameterProvider::class) profile: Profile
) = RemoteContentPreview(profile = profile) { Container { RemoteSliderNotSegmented() } }

@Composable
@RemoteComposable
public fun RemoteSliderCustomColors() {
    val value = rememberMutableRemoteFloat(3f)
    RemoteSlider(
        value = value,
        steps = 4,
        colors =
            RemoteSliderDefaults.sliderColors(
                selectedBarColor = RemoteColor(Color(0xFFFF6F61)),
                containerColor = RemoteColor(Color.DarkGray),
            ),
        decreaseAction = valueChange(value, max(value - 1f.rf, 0f.rf)),
        increaseAction = valueChange(value, min(value + 1f.rf, 5f.rf)),
    )
}

@WearPreviewDevices
@Composable
private fun RemoteSliderCustomColorsPreview(
    @PreviewParameter(ProfilePreviewParameterProvider::class) profile: Profile
) = RemoteContentPreview(profile = profile) { Container { RemoteSliderCustomColors() } }

@Composable
@RemoteComposable
private fun Container(
    modifier: RemoteModifier = RemoteModifier.fillMaxSize().padding(horizontal = 10.rdp),
    content: @Composable @RemoteComposable () -> Unit,
) {
    RemoteBox(modifier, contentAlignment = RemoteAlignment.Center, content = content)
}
