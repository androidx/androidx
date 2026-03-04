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

import androidx.compose.remote.creation.compose.layout.RemoteAlignment
import androidx.compose.remote.creation.compose.layout.RemoteArrangement
import androidx.compose.remote.creation.compose.layout.RemoteBox
import androidx.compose.remote.creation.compose.layout.RemoteComposable
import androidx.compose.remote.creation.compose.modifier.RemoteModifier
import androidx.compose.remote.creation.compose.modifier.fillMaxSize
import androidx.compose.remote.creation.compose.modifier.size
import androidx.compose.remote.creation.compose.state.rb
import androidx.compose.remote.creation.compose.state.rc
import androidx.compose.remote.creation.compose.state.rdp
import androidx.compose.remote.creation.compose.state.rf
import androidx.compose.remote.creation.profile.Profile
import androidx.compose.remote.tooling.preview.RemotePreview
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.tooling.preview.PreviewParameter
import androidx.wear.compose.remote.material3.RemoteCircularProgressIndicator
import androidx.wear.compose.remote.material3.RemoteProgressIndicatorDefaults
import androidx.wear.compose.remote.material3.samples.RemoteCircularProgressIndicatorAnimatedSample
import androidx.wear.compose.ui.tooling.preview.WearPreviewDevices

@WearPreviewDevices
@Composable
private fun RemoteCircularProgressIndicatorPreview(
    @PreviewParameter(ProfilePreviewParameterProvider::class) profile: Profile
) {
    RemotePreview(profile = profile) { RemoteCircularProgressEnabled() }
}

@Composable
fun RemoteCircularProgressEnabled() {
    Container {
        RemoteCircularProgressIndicator(
            modifier = RemoteModifier.size(100.rdp),
            progress = 0.75f.rf,
        )
    }
}

@WearPreviewDevices
@Composable
private fun RemoteCircularProgressNoGapCustomAnglePreview(
    @PreviewParameter(ProfilePreviewParameterProvider::class) profile: Profile
) {
    RemotePreview(profile = profile) { RemoteCircularProgressNoGapCustomAngle() }
}

@Composable
fun RemoteCircularProgressNoGapCustomAngle() {
    Container {
        RemoteCircularProgressIndicator(
            modifier = RemoteModifier.size(150.rdp),
            progress = 0.75f.rf,
            startAngle = 135f.rf,
            endAngle = 45f.rf,
            gapSize = 0.rdp,
        )
    }
}

@WearPreviewDevices
@Composable
private fun RemoteCircularProgressIndicatorCustomColorPreview(
    @PreviewParameter(ProfilePreviewParameterProvider::class) profile: Profile
) {
    RemotePreview(profile = profile) { RemoteCircularProgressIndicatorCustomColor() }
}

@Composable
fun RemoteCircularProgressIndicatorCustomColor() {
    Container {
        RemoteCircularProgressIndicator(
            modifier = RemoteModifier.size(150.rdp),
            progress = 0.75f.rf,
            colors =
                RemoteProgressIndicatorDefaults.colors(
                    indicatorColor = Color.Red.rc,
                    trackColor = Color.Blue.rc,
                ),
        )
    }
}

@WearPreviewDevices
@Composable
private fun RemoteCircularProgressIndicatorDisabledPreview(
    @PreviewParameter(ProfilePreviewParameterProvider::class) profile: Profile
) {
    RemotePreview(profile = profile) { RemoteCircularProgressIndicatorDisabled() }
}

@Composable
fun RemoteCircularProgressIndicatorDisabled() {
    Container {
        RemoteCircularProgressIndicator(
            modifier = RemoteModifier.size(150.rdp),
            progress = 0.75f.rf,
            enabled = false.rb,
        )
    }
}

@WearPreviewDevices
@Composable
public fun RemoteCircularProgressIndicatorAnimatedPreview() {
    RemotePreview { Container { RemoteCircularProgressIndicatorAnimatedSample() } }
}

@Composable
@RemoteComposable
private fun Container(
    modifier: RemoteModifier = RemoteModifier.fillMaxSize(),
    content: @Composable @RemoteComposable () -> Unit,
) {
    RemoteBox(
        modifier,
        horizontalAlignment = RemoteAlignment.CenterHorizontally,
        verticalArrangement = RemoteArrangement.Center,
        content = content,
    )
}
