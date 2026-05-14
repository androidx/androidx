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

@file:Suppress("RestrictedApiAndroidX")

package androidx.wear.compose.remote.material3.previews

import androidx.compose.remote.creation.compose.action.hostAction
import androidx.compose.remote.creation.compose.layout.RemoteAlignment
import androidx.compose.remote.creation.compose.layout.RemoteBox
import androidx.compose.remote.creation.compose.layout.RemoteComposable
import androidx.compose.remote.creation.compose.modifier.RemoteModifier
import androidx.compose.remote.creation.compose.modifier.fillMaxSize
import androidx.compose.remote.creation.compose.painter.painterRemoteBitmap
import androidx.compose.remote.creation.compose.shapes.RemoteRoundedCornerShape
import androidx.compose.remote.creation.compose.state.RemoteColor
import androidx.compose.remote.creation.compose.state.RemoteString
import androidx.compose.remote.creation.compose.state.rb
import androidx.compose.remote.creation.compose.state.rdp
import androidx.compose.remote.creation.compose.state.rememberNamedRemoteBitmap
import androidx.compose.remote.creation.compose.state.rf
import androidx.compose.remote.creation.compose.state.rs
import androidx.compose.remote.creation.profile.Profile
import androidx.compose.remote.tooling.preview.RemoteContentPreview
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.tooling.preview.PreviewParameter
import androidx.wear.compose.remote.material3.RemoteButton
import androidx.wear.compose.remote.material3.RemoteButtonDefaults
import androidx.wear.compose.remote.material3.RemoteIcon
import androidx.wear.compose.remote.material3.RemoteText
import androidx.wear.compose.remote.material3.buttonSizeModifier
import androidx.wear.compose.remote.material3.previews.utils.ProfilePreviewParameterProvider
import androidx.wear.compose.remote.material3.previews.utils.TestImageVectors
import androidx.wear.compose.remote.material3.previews.utils.createImage
import androidx.wear.compose.ui.tooling.preview.WearPreviewDevices

@Composable
@RemoteComposable
fun RemoteButtonEnabled() {
    RemoteButton(
        onClick = testAction,
        modifier = RemoteModifier.buttonSizeModifier(),
        enabled = true.rb,
        content = { RemoteText("button_enabled".rs) },
    )
}

@WearPreviewDevices
@Composable
private fun RemoteButtonEnabledPreview(
    @PreviewParameter(ProfilePreviewParameterProvider::class) profile: Profile
) = RemoteContentPreview(profile = profile) { Container { RemoteButtonEnabled() } }

@Composable
@RemoteComposable
fun RemoteButtonWithBorder() {
    RemoteButton(
        onClick = testAction,
        modifier = RemoteModifier.buttonSizeModifier(),
        border = 8.rdp,
        borderColor = RemoteColor(Color.Green),
    ) {
        RemoteText("button_with_border".rs)
    }
}

@WearPreviewDevices
@Composable
private fun RemoteButtonWithBorderPreview(
    @PreviewParameter(ProfilePreviewParameterProvider::class) profile: Profile
) = RemoteContentPreview(profile = profile) { Container { RemoteButtonWithBorder() } }

@Composable
@RemoteComposable
fun RemoteButtonWithSecondaryLabel() {
    RemoteButton(
        onClick = testAction,
        modifier = RemoteModifier.buttonSizeModifier(),
        secondaryLabel = { RemoteText(RemoteString("secondaryLabel")) },
        label = { RemoteText(RemoteString("label")) },
    )
}

@Composable
@RemoteComposable
fun RemoteButtonWithIcon() {
    RemoteButton(
        onClick = testAction,
        modifier = RemoteModifier.buttonSizeModifier(),
        icon = {
            RemoteIcon(
                imageVector = TestImageVectors.VolumeUp,
                contentDescription = null,
                tint = RemoteButtonDefaults.buttonColors().iconColor,
            )
        },
        label = { RemoteText("label".rs) },
    )
}

@WearPreviewDevices
@Composable
private fun RemoteButtonWithIconPreview(
    @PreviewParameter(ProfilePreviewParameterProvider::class) profile: Profile
) = RemoteContentPreview(profile = profile) { Container { RemoteButtonWithIcon() } }

@Composable
@RemoteComposable
fun RemoteButtonWithIconAndSecondaryLabel() {
    RemoteButton(
        onClick = testAction,
        modifier = RemoteModifier.buttonSizeModifier(),
        icon = {
            RemoteIcon(
                imageVector = TestImageVectors.VolumeUp,
                contentDescription = null,
                tint = RemoteButtonDefaults.buttonColors().iconColor,
            )
        },
        secondaryLabel = { RemoteText("secondaryLabel".rs) },
        label = { RemoteText("label".rs) },
    )
}

@WearPreviewDevices
@Composable
private fun RemoteButtonWithIconAndSecondaryLabelPreview(
    @PreviewParameter(ProfilePreviewParameterProvider::class) profile: Profile
) =
    RemoteContentPreview(profile = profile) {
        Container { RemoteButtonWithIconAndSecondaryLabel() }
    }

@Composable
@RemoteComposable
fun RemoteButtonWithBackground() {
    val backgroundImage =
        rememberNamedRemoteBitmap(name = "backgroundImage") {
            createImage(200, 200).asImageBitmap()
        }
    val containerPainter =
        RemoteButtonDefaults.containerPainter(painterRemoteBitmap(backgroundImage))
    RemoteButton(
        onClick = testAction,
        modifier = RemoteModifier.buttonSizeModifier(),
        containerPainter = containerPainter,
    ) {
        RemoteText("image_background".rs)
    }
}

@WearPreviewDevices
@Composable
private fun RemoteButtonWithBackgroundPreview(
    @PreviewParameter(ProfilePreviewParameterProvider::class) profile: Profile
) = RemoteContentPreview(profile = profile) { Container { RemoteButtonWithBackground() } }

@Composable
@RemoteComposable
fun RemoteButtonWithShape() {
    RemoteButton(
        onClick = testAction,
        modifier = RemoteModifier.buttonSizeModifier(),
        shape = RemoteRoundedCornerShape(4.rdp),
        content = { RemoteText("Custom shape".rs) },
    )
}

@WearPreviewDevices
@Composable
private fun RemoteButtonWithShapePreview(
    @PreviewParameter(ProfilePreviewParameterProvider::class) profile: Profile
) = RemoteContentPreview(profile = profile) { Container { RemoteButtonWithShape() } }

@Composable
@RemoteComposable
private fun Container(
    modifier: RemoteModifier = RemoteModifier.fillMaxSize(),
    content: @Composable @RemoteComposable () -> Unit,
) {
    RemoteBox(modifier, contentAlignment = RemoteAlignment.Center, content = content)
}

private val testAction = hostAction("testAction".rs, 1.rf)
