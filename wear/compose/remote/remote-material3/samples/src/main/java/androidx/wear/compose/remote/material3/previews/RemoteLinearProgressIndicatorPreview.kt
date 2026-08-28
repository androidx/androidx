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

@file:Suppress("RestrictedApiAndroidX")

package androidx.wear.compose.remote.material3.previews

import androidx.compose.remote.creation.compose.layout.RemoteAlignment
import androidx.compose.remote.creation.compose.layout.RemoteBox
import androidx.compose.remote.creation.compose.layout.RemoteComposable
import androidx.compose.remote.creation.compose.modifier.RemoteModifier
import androidx.compose.remote.creation.compose.modifier.fillMaxSize
import androidx.compose.remote.creation.compose.modifier.width
import androidx.compose.remote.creation.compose.state.rb
import androidx.compose.remote.creation.compose.state.rc
import androidx.compose.remote.creation.compose.state.rdp
import androidx.compose.remote.creation.compose.state.rf
import androidx.compose.remote.creation.profile.Profile
import androidx.compose.remote.tooling.preview.RemoteContentPreview
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.tooling.preview.PreviewParameter
import androidx.wear.compose.remote.material3.RemoteLinearProgressIndicator
import androidx.wear.compose.remote.material3.RemoteLinearProgressIndicatorDefaults
import androidx.wear.compose.remote.material3.RemoteProgressIndicatorDefaults
import androidx.wear.compose.remote.material3.previews.utils.ProfilePreviewParameterProvider
import androidx.wear.compose.ui.tooling.preview.WearPreviewDevices

@WearPreviewDevices
@Composable
private fun RemoteLinearProgressIndicatorPreview(
    @PreviewParameter(ProfilePreviewParameterProvider::class) profile: Profile
) {
    RemoteContentPreview(profile = profile) { Container { RemoteLinearProgressDefault() } }
}

@Composable
fun RemoteLinearProgressDefault() {
    RemoteLinearProgressIndicator(progress = 0.75f.rf)
}

@WearPreviewDevices
@Composable
private fun RemoteLinearProgressIndicatorCustomWidthPreview(
    @PreviewParameter(ProfilePreviewParameterProvider::class) profile: Profile
) {
    RemoteContentPreview(profile = profile) { Container { RemoteLinearProgressCustomWidth() } }
}

@Composable
fun RemoteLinearProgressCustomWidth() {
    RemoteLinearProgressIndicator(modifier = RemoteModifier.width(140.rdp), progress = 0.66f.rf)
}

@WearPreviewDevices
@Composable
private fun RemoteLinearProgressIndicatorScaledDotPreview(
    @PreviewParameter(ProfilePreviewParameterProvider::class) profile: Profile
) {
    RemoteContentPreview(profile = profile) { Container { RemoteLinearProgressScaledDot() } }
}

@Composable
fun RemoteLinearProgressScaledDot() {
    RemoteLinearProgressIndicator(modifier = RemoteModifier.width(140.rdp), progress = 0.925f.rf)
}

@WearPreviewDevices
@Composable
private fun RemoteLinearProgressIndicatorCustomColorPreview(
    @PreviewParameter(ProfilePreviewParameterProvider::class) profile: Profile
) {
    RemoteContentPreview(profile = profile) { Container { RemoteLinearProgressCustomColor() } }
}

@Composable
fun RemoteLinearProgressCustomColor() {
    RemoteLinearProgressIndicator(
        modifier = RemoteModifier.width(140.rdp),
        progress = 0.66f.rf,
        colors =
            RemoteProgressIndicatorDefaults.colors(
                indicatorColor = Color.Red.rc,
                trackColor = Color.Blue.rc,
            ),
    )
}

@WearPreviewDevices
@Composable
private fun RemoteLinearProgressIndicatorDisabledPreview(
    @PreviewParameter(ProfilePreviewParameterProvider::class) profile: Profile
) {
    RemoteContentPreview(profile = profile) { Container { RemoteLinearProgressDisabled() } }
}

@Composable
fun RemoteLinearProgressDisabled() {
    RemoteLinearProgressIndicator(
        modifier = RemoteModifier.width(140.rdp),
        progress = 0.66f.rf,
        enabled = false.rb,
    )
}

@WearPreviewDevices
@Composable
private fun RemoteLinearProgressSmallStrokePreview(
    @PreviewParameter(ProfilePreviewParameterProvider::class) profile: Profile
) {
    RemoteContentPreview(profile = profile) { Container { RemoteLinearProgressSmallStroke() } }
}

@Composable
fun RemoteLinearProgressSmallStroke() {
    RemoteLinearProgressIndicator(
        modifier = RemoteModifier.width(140.rdp),
        progress = 0.66f.rf,
        strokeWidth = RemoteLinearProgressIndicatorDefaults.StrokeWidthSmall,
    )
}

@WearPreviewDevices
@Composable
private fun RemoteLinearProgressZeroPreview(
    @PreviewParameter(ProfilePreviewParameterProvider::class) profile: Profile
) {
    RemoteContentPreview(profile = profile) { Container { RemoteLinearProgressZero() } }
}

@Composable
fun RemoteLinearProgressZero() {
    RemoteLinearProgressIndicator(modifier = RemoteModifier.width(140.rdp), progress = 0f.rf)
}

@WearPreviewDevices
@Composable
private fun RemoteLinearProgressCompletePreview(
    @PreviewParameter(ProfilePreviewParameterProvider::class) profile: Profile
) {
    RemoteContentPreview(profile = profile) { Container { RemoteLinearProgressComplete() } }
}

@Composable
fun RemoteLinearProgressComplete() {
    RemoteLinearProgressIndicator(modifier = RemoteModifier.width(140.rdp), progress = 1f.rf)
}

@RemoteComposable
@Composable
private fun Container(content: @Composable @RemoteComposable () -> Unit) {
    RemoteBox(
        modifier = RemoteModifier.fillMaxSize(),
        contentAlignment = RemoteAlignment.Center,
        content = content,
    )
}
