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
import androidx.compose.remote.creation.compose.modifier.size
import androidx.compose.remote.creation.compose.state.rb
import androidx.compose.remote.creation.compose.state.rc
import androidx.compose.remote.creation.compose.state.rdp
import androidx.compose.remote.creation.compose.state.rf
import androidx.compose.remote.creation.profile.Profile
import androidx.compose.remote.tooling.preview.RemoteContentPreview
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.tooling.preview.PreviewParameter
import androidx.wear.compose.remote.material3.RemoteCurvedProgressIndicator
import androidx.wear.compose.remote.material3.RemoteProgressIndicatorDefaults
import androidx.wear.compose.remote.material3.previews.utils.ProfilePreviewParameterProvider
import androidx.wear.compose.remote.material3.samples.RemoteCurvedProgressIndicatorAnimatedSample
import androidx.wear.compose.ui.tooling.preview.WearPreviewDevices

@WearPreviewDevices
@Composable
private fun RemoteCurvedProgressIndicatorPreview(
    @PreviewParameter(ProfilePreviewParameterProvider::class) profile: Profile
) {
    RemoteContentPreview(profile = profile) { Container { RemoteCurvedProgressEnabled() } }
}

@Composable
fun RemoteCurvedProgressEnabled() {
    RemoteCurvedProgressIndicator(
        modifier = RemoteModifier.size(150.rdp),
        progress = 0.75f.rf,
        startAngle = 135f.rf,
        sweepAngle = 90f.rf,
        animationDurationMillis = 0,
    )
}

@Composable
fun RemoteCurvedProgressEnabledRtl() {
    RemoteCurvedProgressIndicator(
        modifier = RemoteModifier.size(150.rdp),
        progress = 0.75f.rf,
        startAngle = 135f.rf,
        sweepAngle = 90f.rf,
        reverseDirection = true.rb,
        animationDurationMillis = 0,
    )
}

@Composable
fun RemoteCurvedProgressNoGap() {
    RemoteCurvedProgressIndicator(
        modifier = RemoteModifier.size(150.rdp),
        progress = 0.75f.rf,
        startAngle = 135f.rf,
        sweepAngle = 90f.rf,
        gapAngleDegrees = 0f.rf,
        animationDurationMillis = 0,
    )
}

// High Progress (99%) - Remaining Track Segment Collapse Demos
@Composable
fun RemoteCurvedProgressFullCollapseHigh() {
    RemoteCurvedProgressIndicator(
        modifier = RemoteModifier.size(150.rdp),
        progress = 0.99f.rf,
        startAngle = 135f.rf,
        sweepAngle = 90f.rf,
        dotCollapseFreezeFraction = 0f.rf,
        animationDurationMillis = 0,
    )
}

@Composable
fun RemoteCurvedProgressSemiCollapseHigh() {
    RemoteCurvedProgressIndicator(
        modifier = RemoteModifier.size(150.rdp),
        progress = 0.99f.rf,
        startAngle = 135f.rf,
        sweepAngle = 90f.rf,
        dotCollapseFreezeFraction = 0.5f.rf,
        animationDurationMillis = 0,
    )
}

@Composable
fun RemoteCurvedProgressNoCollapseHigh() {
    RemoteCurvedProgressIndicator(
        modifier = RemoteModifier.size(150.rdp),
        progress = 0.99f.rf,
        startAngle = 135f.rf,
        sweepAngle = 90f.rf,
        dotCollapseFreezeFraction = 1f.rf,
        animationDurationMillis = 0,
    )
}

@WearPreviewDevices
@Composable
private fun RemoteCurvedProgressFullCollapseHighPreview(
    @PreviewParameter(ProfilePreviewParameterProvider::class) profile: Profile
) {
    RemoteContentPreview(profile = profile) { Container { RemoteCurvedProgressFullCollapseHigh() } }
}

@WearPreviewDevices
@Composable
private fun RemoteCurvedProgressSemiCollapseHighPreview(
    @PreviewParameter(ProfilePreviewParameterProvider::class) profile: Profile
) {
    RemoteContentPreview(profile = profile) { Container { RemoteCurvedProgressSemiCollapseHigh() } }
}

@WearPreviewDevices
@Composable
private fun RemoteCurvedProgressNoCollapseHighPreview(
    @PreviewParameter(ProfilePreviewParameterProvider::class) profile: Profile
) {
    RemoteContentPreview(profile = profile) { Container { RemoteCurvedProgressNoCollapseHigh() } }
}

@WearPreviewDevices
@Composable
private fun RemoteCurvedProgressIndicatorCustomColorPreview(
    @PreviewParameter(ProfilePreviewParameterProvider::class) profile: Profile
) {
    RemoteContentPreview(profile = profile) {
        Container { RemoteCurvedProgressIndicatorCustomColor() }
    }
}

@Composable
fun RemoteCurvedProgressIndicatorCustomColor() {
    RemoteCurvedProgressIndicator(
        modifier = RemoteModifier.size(150.rdp),
        progress = 0.75f.rf,
        startAngle = 135f.rf,
        sweepAngle = 90f.rf,
        colors =
            RemoteProgressIndicatorDefaults.colors(
                indicatorColor = Color.Red.rc,
                trackColor = Color.Blue.rc,
            ),
        animationDurationMillis = 0,
    )
}

@WearPreviewDevices
@Composable
private fun RemoteCurvedProgressIndicatorDisabledPreview(
    @PreviewParameter(ProfilePreviewParameterProvider::class) profile: Profile
) {
    RemoteContentPreview(profile = profile) {
        Container { RemoteCurvedProgressIndicatorDisabled() }
    }
}

@Composable
fun RemoteCurvedProgressIndicatorDisabled() {
    RemoteCurvedProgressIndicator(
        modifier = RemoteModifier.size(150.rdp),
        progress = 0.75f.rf,
        startAngle = 135f.rf,
        sweepAngle = 90f.rf,
        enabled = false.rb,
        animationDurationMillis = 0,
    )
}

@WearPreviewDevices
@Composable
public fun RemoteCurvedProgressIndicatorAnimatedPreview() {
    RemoteContentPreview { Container { RemoteCurvedProgressIndicatorAnimatedSample() } }
}

@Composable
@RemoteComposable
private fun Container(
    modifier: RemoteModifier = RemoteModifier.fillMaxSize(),
    content: @Composable @RemoteComposable () -> Unit,
) {
    RemoteBox(modifier, contentAlignment = RemoteAlignment.Center, content = content)
}
