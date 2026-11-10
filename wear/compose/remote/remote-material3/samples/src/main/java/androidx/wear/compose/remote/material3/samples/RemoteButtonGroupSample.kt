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

package androidx.wear.compose.remote.material3.samples

import androidx.annotation.Sampled
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Call
import androidx.compose.material.icons.filled.Favorite
import androidx.compose.material.icons.filled.MailOutline
import androidx.compose.remote.creation.compose.action.hostAction
import androidx.compose.remote.creation.compose.capture.toRemoteImageVector
import androidx.compose.remote.creation.compose.layout.RemoteComposable
import androidx.compose.remote.creation.compose.modifier.RemoteModifier
import androidx.compose.remote.creation.compose.modifier.fillMaxWidth
import androidx.compose.remote.creation.compose.state.rf
import androidx.compose.remote.creation.compose.state.rs
import androidx.compose.runtime.Composable
import androidx.compose.ui.tooling.preview.PreviewWrapper
import androidx.wear.compose.remote.material3.RemoteButtonDefaults
import androidx.wear.compose.remote.material3.RemoteButtonGroup
import androidx.wear.compose.remote.material3.RemoteIcon
import androidx.wear.compose.remote.material3.RemoteIconButton
import androidx.wear.compose.remote.material3.RemoteIconButtonDefaults
import androidx.wear.compose.remote.material3.previews.utils.RemoteComponentPreviewWrapper
import androidx.wear.compose.ui.tooling.preview.WearPreviewDevices

@Sampled
@Composable
@RemoteComposable
@WearPreviewDevices
@PreviewWrapper(RemoteComponentPreviewWrapper::class)
fun RemoteButtonGroupThreeButtonSample(modifier: RemoteModifier = RemoteModifier) {
    val icons = listOf(Icons.Filled.MailOutline, Icons.Filled.Favorite, Icons.Filled.Call)
    RemoteButtonGroup(modifier = modifier.fillMaxWidth()) {
        icons.forEach { icon ->
            RemoteIconButton(
                onClick = hostAction("button_click".rs),
                modifier = RemoteModifier.weight(1f.rf),
                colors = RemoteIconButtonDefaults.filledIconButtonColors(),
                shapes = RemoteIconButtonDefaults.shapes(RemoteButtonDefaults.shape),
            ) {
                RemoteIcon(imageVector = icon.toRemoteImageVector(), contentDescription = null)
            }
        }
    }
}

@Sampled
@Composable
@RemoteComposable
@WearPreviewDevices
@PreviewWrapper(RemoteComponentPreviewWrapper::class)
fun RemoteButtonGroupAnimatedSample(modifier: RemoteModifier = RemoteModifier) {
    val buttons =
        listOf(
            Icons.Filled.MailOutline to 1f.rf,
            Icons.Filled.Favorite to 1.5f.rf,
            Icons.Filled.Call to 1f.rf,
        )
    RemoteButtonGroup(modifier = modifier.fillMaxWidth()) {
        buttons.forEach { (icon, weight) ->
            RemoteIconButton(
                onClick = hostAction("button_click".rs),
                modifier = RemoteModifier.weight(weight).animateWidth(),
                colors = RemoteIconButtonDefaults.filledIconButtonColors(),
                shapes = RemoteIconButtonDefaults.shapes(RemoteButtonDefaults.shape),
            ) {
                RemoteIcon(imageVector = icon.toRemoteImageVector(), contentDescription = null)
            }
        }
    }
}
