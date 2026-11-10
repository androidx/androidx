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
import androidx.compose.remote.creation.compose.action.hostAction
import androidx.compose.remote.creation.compose.layout.RemoteComposable
import androidx.compose.remote.creation.compose.modifier.RemoteModifier
import androidx.compose.remote.creation.compose.modifier.size
import androidx.compose.remote.creation.compose.state.rs
import androidx.compose.runtime.Composable
import androidx.compose.ui.tooling.preview.PreviewWrapper
import androidx.wear.compose.remote.material3.RemoteIcon
import androidx.wear.compose.remote.material3.RemoteIconButton
import androidx.wear.compose.remote.material3.RemoteIconButtonDefaults
import androidx.wear.compose.remote.material3.previews.utils.RemoteComponentPreviewWrapper
import androidx.wear.compose.remote.material3.previews.utils.TestImageVectors
import androidx.wear.compose.ui.tooling.preview.WearPreviewDevices

@Sampled
@Composable
@WearPreviewDevices
@PreviewWrapper(RemoteComponentPreviewWrapper::class)
fun RemoteIconButtonSimpleSample(modifier: RemoteModifier = RemoteModifier) {
    RemoteIconButton(
        onClick = hostAction("volume_up".rs),
        modifier = modifier,
        colors = RemoteIconButtonDefaults.filledTonalIconButtonColors(),
    ) {
        RemoteIcon(
            imageVector = TestImageVectors.VolumeUp,
            contentDescription = null,
            modifier = RemoteModifier.size(RemoteIconButtonDefaults.DefaultIconSize),
        )
    }
}

@Sampled
@RemoteComposable
@Composable
@WearPreviewDevices
@PreviewWrapper(RemoteComponentPreviewWrapper::class)
fun RemoteIconButtonWithCornerAnimationSample(modifier: RemoteModifier = RemoteModifier) {
    RemoteIconButton(
        onClick = hostAction("volume_up".rs),
        modifier = modifier,
        shapes = RemoteIconButtonDefaults.animatedShapes(),
        colors = RemoteIconButtonDefaults.filledIconButtonColors(),
    ) {
        RemoteIcon(
            imageVector = TestImageVectors.VolumeUp,
            contentDescription = null,
            modifier = RemoteModifier.size(RemoteIconButtonDefaults.DefaultIconSize),
        )
    }
}
