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
import androidx.compose.remote.creation.compose.state.RemoteColor
import androidx.compose.remote.creation.compose.state.max
import androidx.compose.remote.creation.compose.state.min
import androidx.compose.remote.creation.compose.state.rb
import androidx.compose.remote.creation.compose.state.rememberMutableRemoteFloat
import androidx.compose.remote.creation.compose.state.rf
import androidx.compose.remote.creation.compose.state.rs
import androidx.compose.remote.creation.profile.Profile
import androidx.compose.remote.tooling.preview.RemoteContentPreview
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.tooling.preview.PreviewParameter
import androidx.wear.compose.remote.material3.RemoteStepper
import androidx.wear.compose.remote.material3.RemoteStepperDefaults
import androidx.wear.compose.remote.material3.RemoteText
import androidx.wear.compose.remote.material3.previews.utils.ProfilePreviewParameterProvider
import androidx.wear.compose.ui.tooling.preview.WearPreviewDevices

@Composable
@RemoteComposable
public fun RemoteStepperDefault() {
    val value = rememberMutableRemoteFloat(2f)
    RemoteStepper(
        value = value,
        steps = 4,
        decreaseAction = valueChange(value, max(value - 1f.rf, 0f.rf)),
        increaseAction = valueChange(value, min(value + 1f.rf, 5f.rf)),
    ) {
        RemoteText(value.toRemoteInt().toRemoteString())
    }
}

@WearPreviewDevices
@Composable
private fun RemoteStepperDefaultPreview(
    @PreviewParameter(ProfilePreviewParameterProvider::class) profile: Profile
) = RemoteContentPreview(profile = profile) { Container { RemoteStepperDefault() } }

@Composable
@RemoteComposable
public fun RemoteStepperDisabled() {
    RemoteStepper(
        value = 2f.rf,
        steps = 4,
        enabled = false.rb,
    ) {
        RemoteText("2".rs)
    }
}

@WearPreviewDevices
@Composable
private fun RemoteStepperDisabledPreview(
    @PreviewParameter(ProfilePreviewParameterProvider::class) profile: Profile
) = RemoteContentPreview(profile = profile) { Container { RemoteStepperDisabled() } }

@Composable
@RemoteComposable
public fun RemoteStepperCustomColors() {
    val value = rememberMutableRemoteFloat(3f)
    RemoteStepper(
        value = value,
        steps = 4,
        colors =
            RemoteStepperDefaults.stepperColors(
                buttonContainerColor = RemoteColor(Color(0xFFFF6F61)),
                buttonIconColor = RemoteColor(Color.Black),
            ),
        decreaseAction = valueChange(value, max(value - 1f.rf, 0f.rf)),
        increaseAction = valueChange(value, min(value + 1f.rf, 5f.rf)),
    ) {
        RemoteText(value.toRemoteInt().toRemoteString())
    }
}

@WearPreviewDevices
@Composable
private fun RemoteStepperCustomColorsPreview(
    @PreviewParameter(ProfilePreviewParameterProvider::class) profile: Profile
) = RemoteContentPreview(profile = profile) { Container { RemoteStepperCustomColors() } }

@Composable
@RemoteComposable
private fun Container(
    modifier: RemoteModifier = RemoteModifier.fillMaxSize(),
    content: @Composable @RemoteComposable () -> Unit,
) {
    RemoteBox(modifier, contentAlignment = RemoteAlignment.Center, content = content)
}
