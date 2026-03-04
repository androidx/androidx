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

import androidx.compose.remote.creation.compose.capture.NoRemoteCompose
import androidx.compose.remote.creation.compose.capture.RemoteImageVector
import androidx.compose.remote.creation.compose.layout.RemoteAlignment
import androidx.compose.remote.creation.compose.layout.RemoteArrangement
import androidx.compose.remote.creation.compose.layout.RemoteBox
import androidx.compose.remote.creation.compose.layout.RemoteComposable
import androidx.compose.remote.creation.compose.modifier.RemoteModifier
import androidx.compose.remote.creation.compose.modifier.fillMaxSize
import androidx.compose.remote.creation.compose.state.RemoteColor
import androidx.compose.remote.creation.compose.state.rememberNamedRemoteColor
import androidx.compose.remote.creation.compose.state.rf
import androidx.compose.remote.creation.compose.vector.RemotePathData
import androidx.compose.remote.creation.profile.Profile
import androidx.compose.remote.tooling.preview.RemotePreview
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.res.vectorResource
import androidx.compose.ui.tooling.preview.PreviewParameter
import androidx.wear.compose.remote.material3.RemoteIcon
import androidx.wear.compose.remote.material3.samples.R
import androidx.wear.compose.ui.tooling.preview.WearPreviewDevices

@Composable
@RemoteComposable
fun RemoteIconDefault() {
    RemoteIcon(imageVector = TestImageVectors.VolumeUp, contentDescription = null)
}

@WearPreviewDevices
@Composable
private fun RemoteIconPreview(
    @PreviewParameter(ProfilePreviewParameterProvider::class) profile: Profile
) = RemotePreview(profile = profile) { Container { RemoteIconDefault() } }

@Composable
@RemoteComposable
fun RemoteIconFromResource() {
    RemoteIcon(
        imageVector = ImageVector.vectorResource(R.drawable.gs_map_wght500rond100_vd_theme_24),
        contentDescription = null,
    )
}

@WearPreviewDevices
@Composable
private fun RemoteIconFromResourcePreview(
    @PreviewParameter(ProfilePreviewParameterProvider::class) profile: Profile
) = RemotePreview(profile = profile) { Container { RemoteIconFromResource() } }

@Composable
@RemoteComposable
fun RemoteIconColor() {
    val color = rememberNamedRemoteColor("testColor", Color.Red)
    RemoteIcon(imageVector = VolumeUp, contentDescription = null, tint = color)
}

@WearPreviewDevices
@Composable
private fun RemoteIconColorPreview(
    @PreviewParameter(ProfilePreviewParameterProvider::class) profile: Profile
) = RemotePreview(profile = profile) { Container { RemoteIconColor() } }

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

val testRemoteStateScope = NoRemoteCompose()

private val VolumeUp =
    RemoteImageVector.Builder(
            testRemoteStateScope,
            name = "Volume up",
            viewportWidth = 24.0f.rf,
            viewportHeight = 24.0f.rf,
            tintColor = RemoteColor(Color.White),
        )
        .addPath(
            RemotePathData(testRemoteStateScope) {
                moveTo(3.0f.rf, 9.0f.rf)
                verticalLineToRelative(6.0f.rf)
                horizontalLineToRelative(4.0f.rf)
                lineToRelative(5.0f.rf, 5.0f.rf)
                lineTo(12.0f.rf, 4.0f.rf)
                lineTo(7.0f.rf, 9.0f.rf)
                lineTo(3.0f.rf, 9.0f.rf)
                close()
                moveTo(16.5f.rf, 12.0f.rf)
                curveToRelative(
                    0.0f.rf,
                    (-1.77f).rf,
                    (-1.02f).rf,
                    (-3.29f).rf,
                    (-2.5f).rf,
                    (-4.03f).rf,
                )
                verticalLineToRelative(8.05f.rf)
                curveToRelative(1.48f.rf, (-0.73f).rf, 2.5f.rf, (-2.25f).rf, 2.5f.rf, (-4.02f).rf)
                close()
                moveTo(14.0f.rf, 3.23f.rf)
                verticalLineToRelative(2.06f.rf)
                curveToRelative(2.89f.rf, 0.86f.rf, 5.0f.rf, 3.54f.rf, 5.0f.rf, 6.71f.rf)
                reflectiveCurveToRelative((-2.11f).rf, 5.85f.rf, (-5.0f).rf, 6.71f.rf)
                verticalLineToRelative(2.06f.rf)
                curveToRelative(4.01f.rf, (-0.91f).rf, 7.0f.rf, (-4.49f).rf, 7.0f.rf, (-8.77f).rf)
                reflectiveCurveToRelative((-2.99f).rf, (-7.86f).rf, (-7.0f).rf, (-8.77f).rf)
                close()
            },
            fill = SolidColor(Color.Black),
        )
        .build()
