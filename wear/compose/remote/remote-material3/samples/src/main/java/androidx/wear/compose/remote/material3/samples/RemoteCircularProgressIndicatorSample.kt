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
import androidx.compose.remote.creation.compose.action.ValueChange
import androidx.compose.remote.creation.compose.layout.RemoteAlignment
import androidx.compose.remote.creation.compose.layout.RemoteArrangement
import androidx.compose.remote.creation.compose.layout.RemoteBox
import androidx.compose.remote.creation.compose.layout.RemoteComposable
import androidx.compose.remote.creation.compose.modifier.RemoteModifier
import androidx.compose.remote.creation.compose.modifier.clickable
import androidx.compose.remote.creation.compose.modifier.fillMaxSize
import androidx.compose.remote.creation.compose.modifier.padding
import androidx.compose.remote.creation.compose.state.animateRemoteFloat
import androidx.compose.remote.creation.compose.state.rdp
import androidx.compose.remote.creation.compose.state.rememberMutableRemoteFloat
import androidx.compose.remote.creation.compose.state.rf
import androidx.compose.remote.tooling.preview.RemotePreview
import androidx.compose.runtime.Composable
import androidx.wear.compose.remote.material3.RemoteCircularProgressIndicator
import androidx.wear.compose.ui.tooling.preview.WearPreviewDevices

@Sampled
@RemoteComposable
@Composable
public fun RemoteCircularProgressIndicatorSample(modifier: RemoteModifier = RemoteModifier) {
    RemoteCircularProgressIndicator(modifier = modifier, progress = 0.75f.rf)
}

@Sampled
@RemoteComposable
@Composable
public fun RemoteCircularProgressIndicatorAnimatedSample(
    modifier: RemoteModifier = RemoteModifier
) {
    val progress = rememberMutableRemoteFloat { 0.25f.rf }
    val animatedProgress = animateRemoteFloat(0.25f) { progress }

    val toggleAction = ValueChange(progress, (progress + 0.25f) % 1f)

    Container(modifier = modifier.clickable(toggleAction).padding(8.rdp)) {
        RemoteCircularProgressIndicator(progress = animatedProgress)
    }
}

@WearPreviewDevices
@Composable
fun RemoteCircularProgressIndicatorAnimatedSamplePreview() = RemotePreview {
    Container { RemoteCircularProgressIndicatorAnimatedSample() }
}

@WearPreviewDevices
@Composable
fun RemoteCircularProgressIndicatorSamplePreview() = RemotePreview {
    Container { RemoteCircularProgressIndicatorSample() }
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
