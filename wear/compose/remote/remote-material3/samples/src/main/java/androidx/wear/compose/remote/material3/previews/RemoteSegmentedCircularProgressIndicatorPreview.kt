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
import androidx.compose.remote.creation.compose.state.ri
import androidx.compose.remote.creation.profile.Profile
import androidx.compose.remote.tooling.preview.RemoteContentPreview
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.tooling.preview.PreviewParameter
import androidx.wear.compose.remote.material3.RemoteProgressIndicatorDefaults
import androidx.wear.compose.remote.material3.RemoteSegmentedCircularProgressIndicator
import androidx.wear.compose.remote.material3.previews.utils.ProfilePreviewParameterProvider
import androidx.wear.compose.ui.tooling.preview.WearPreviewDevices

@WearPreviewDevices
@Composable
public fun RemoteSegmentedCircularProgressHalfPreview(
    @PreviewParameter(ProfilePreviewParameterProvider::class) profile: Profile
): Unit =
    RemoteContentPreview(profile = profile) { Container { RemoteSegmentedCircularProgressHalf() } }

@WearPreviewDevices
@Composable
public fun RemoteSegmentedCircularProgressPartialSegmentPreview(
    @PreviewParameter(ProfilePreviewParameterProvider::class) profile: Profile
): Unit =
    RemoteContentPreview(profile = profile) {
        Container { RemoteSegmentedCircularProgressPartialSegment() }
    }

@WearPreviewDevices
@Composable
public fun RemoteSegmentedCircularProgressDotPreview(
    @PreviewParameter(ProfilePreviewParameterProvider::class) profile: Profile
): Unit =
    RemoteContentPreview(profile = profile) { Container { RemoteSegmentedCircularProgressDot() } }

@WearPreviewDevices
@Composable
public fun RemoteSegmentedCircularProgressFullPreview(
    @PreviewParameter(ProfilePreviewParameterProvider::class) profile: Profile
): Unit =
    RemoteContentPreview(profile = profile) { Container { RemoteSegmentedCircularProgressFull() } }

@WearPreviewDevices
@Composable
public fun RemoteSegmentedCircularProgressBinaryPreview(
    @PreviewParameter(ProfilePreviewParameterProvider::class) profile: Profile
): Unit =
    RemoteContentPreview(profile = profile) {
        Container { RemoteSegmentedCircularProgressBinary() }
    }

@WearPreviewDevices
@Composable
public fun RemoteSegmentedCircularProgressCustomAnglePreview(
    @PreviewParameter(ProfilePreviewParameterProvider::class) profile: Profile
): Unit =
    RemoteContentPreview(profile = profile) {
        Container { RemoteSegmentedCircularProgressCustomAngle() }
    }

@WearPreviewDevices
@Composable
public fun RemoteSegmentedCircularProgressCustomColorPreview(
    @PreviewParameter(ProfilePreviewParameterProvider::class) profile: Profile
): Unit =
    RemoteContentPreview(profile = profile) {
        Container { RemoteSegmentedCircularProgressCustomColor() }
    }

@WearPreviewDevices
@Composable
public fun RemoteSegmentedCircularProgressDisabledPreview(
    @PreviewParameter(ProfilePreviewParameterProvider::class) profile: Profile
): Unit =
    RemoteContentPreview(profile = profile) {
        Container { RemoteSegmentedCircularProgressDisabled() }
    }

@WearPreviewDevices
@Composable
public fun RemoteSegmentedCircularProgressSmallStrokePreview(
    @PreviewParameter(ProfilePreviewParameterProvider::class) profile: Profile
): Unit =
    RemoteContentPreview(profile = profile) {
        Container { RemoteSegmentedCircularProgressSmallStroke() }
    }

@Composable
@RemoteComposable
public fun RemoteSegmentedCircularProgressHalf() {
    RemoteSegmentedCircularProgressIndicator(
        segmentCount = 6.ri,
        progress = 0.5f.rf,
        modifier = RemoteModifier.size(150.rdp),
    )
}

@Composable
@RemoteComposable
public fun RemoteSegmentedCircularProgressPartialSegment() {
    RemoteSegmentedCircularProgressIndicator(
        segmentCount = 5.ri,
        progress = 0.5f.rf,
        modifier = RemoteModifier.size(150.rdp),
    )
}

@Composable
@RemoteComposable
public fun RemoteSegmentedCircularProgressDot() {
    RemoteSegmentedCircularProgressIndicator(
        segmentCount = 5.ri,
        progress = 0.21f.rf,
        modifier = RemoteModifier.size(150.rdp),
    )
}

@Composable
@RemoteComposable
public fun RemoteSegmentedCircularProgressFull() {
    RemoteSegmentedCircularProgressIndicator(
        segmentCount = 5.ri,
        progress = 1f.rf,
        modifier = RemoteModifier.size(150.rdp),
    )
}

@Composable
@RemoteComposable
public fun RemoteSegmentedCircularProgressBinary() {
    RemoteSegmentedCircularProgressIndicator(
        segmentCount = 8.ri,
        segmentValue = { segmentIndex -> (segmentIndex % 3 != 0).rb },
        modifier = RemoteModifier.size(150.rdp),
    )
}

@Composable
@RemoteComposable
public fun RemoteSegmentedCircularProgressCustomAngle() {
    RemoteSegmentedCircularProgressIndicator(
        segmentCount = 4.ri,
        progress = 0.6f.rf,
        modifier = RemoteModifier.size(150.rdp),
        startAngle = 120f.rf,
        endAngle = 60f.rf,
    )
}

@Composable
@RemoteComposable
public fun RemoteSegmentedCircularProgressCustomColor() {
    RemoteSegmentedCircularProgressIndicator(
        segmentCount = 5.ri,
        progress = 0.7f.rf,
        modifier = RemoteModifier.size(150.rdp),
        colors =
            RemoteProgressIndicatorDefaults.colors(
                indicatorColor = Color.Red.rc,
                trackColor = Color.Blue.rc,
            ),
    )
}

@Composable
@RemoteComposable
public fun RemoteSegmentedCircularProgressDisabled() {
    RemoteSegmentedCircularProgressIndicator(
        segmentCount = 5.ri,
        progress = 0.7f.rf,
        modifier = RemoteModifier.size(150.rdp),
        enabled = false.rb,
    )
}

@Composable
@RemoteComposable
public fun RemoteSegmentedCircularProgressSmallStroke() {
    RemoteSegmentedCircularProgressIndicator(
        segmentCount = 12.ri,
        progress = 0.75f.rf,
        modifier = RemoteModifier.size(150.rdp),
        strokeWidth = RemoteProgressIndicatorDefaults.smallStrokeWidth(150.rdp),
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
