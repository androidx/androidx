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
import androidx.compose.remote.creation.compose.action.HostAction
import androidx.compose.remote.creation.compose.layout.RemoteAlignment
import androidx.compose.remote.creation.compose.layout.RemoteBox
import androidx.compose.remote.creation.compose.layout.RemoteComposable
import androidx.compose.remote.creation.compose.modifier.RemoteModifier
import androidx.compose.remote.creation.compose.modifier.fillMaxSize
import androidx.compose.remote.creation.compose.state.rf
import androidx.compose.remote.creation.compose.state.rs
import androidx.compose.remote.tooling.preview.RemotePreview
import androidx.compose.runtime.Composable
import androidx.wear.compose.remote.material3.RemoteIcon
import androidx.wear.compose.remote.material3.RemoteIconButton
import androidx.wear.compose.remote.material3.RemoteIconButtonDefaults
import androidx.wear.compose.remote.material3.RemoteMaterialTheme
import androidx.wear.compose.remote.material3.previews.TestImageVectors
import androidx.wear.compose.ui.tooling.preview.WearPreviewDevices

@Sampled
@Composable
fun RemoteIconButtonSimpleSample(modifier: RemoteModifier = RemoteModifier) {
    RemoteIconButton(testAction, modifier = modifier, colors = tonalColors) {
        RemoteIcon(imageVector = TestImageVectors.VolumeUp, contentDescription = null)
    }
}

@WearPreviewDevices
@Composable
fun RemoteIconButtonSimpleSamplePreview() = RemotePreview {
    Container { RemoteIconButtonSimpleSample() }
}

@Composable
@RemoteComposable
private fun Container(
    modifier: RemoteModifier = RemoteModifier.fillMaxSize(),
    content: @Composable @RemoteComposable () -> Unit,
) {
    RemoteBox(modifier, contentAlignment = RemoteAlignment.Center, content = content)
}

private val tonalColors
    @Composable
    get() =
        RemoteIconButtonDefaults.iconButtonColors()
            .copy(
                containerColor = RemoteMaterialTheme.colorScheme.primary,
                contentColor = RemoteMaterialTheme.colorScheme.onPrimary,
                disabledContainerColor =
                    RemoteMaterialTheme.colorScheme.primary.copy(alpha = 0.12f.rf),
                disabledContentColor = RemoteMaterialTheme.colorScheme.primary.copy(0.38f.rf),
            )

private val testAction = HostAction("testAction".rs, 1.rf)
