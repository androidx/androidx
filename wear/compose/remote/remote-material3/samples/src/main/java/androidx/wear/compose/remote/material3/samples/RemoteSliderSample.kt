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

package androidx.wear.compose.remote.material3.samples

import androidx.annotation.Sampled
import androidx.compose.remote.creation.compose.action.valueChange
import androidx.compose.remote.creation.compose.modifier.RemoteModifier
import androidx.compose.remote.creation.compose.state.max
import androidx.compose.remote.creation.compose.state.min
import androidx.compose.remote.creation.compose.state.rememberMutableRemoteFloat
import androidx.compose.remote.creation.compose.state.rememberMutableRemoteInt
import androidx.compose.remote.creation.compose.state.rf
import androidx.compose.remote.creation.compose.state.ri
import androidx.compose.runtime.Composable
import androidx.compose.ui.tooling.preview.PreviewWrapper
import androidx.wear.compose.remote.material3.RemoteSlider
import androidx.wear.compose.remote.material3.previews.utils.RemoteComponentPreviewWrapper
import androidx.wear.compose.ui.tooling.preview.WearPreviewDevices

@Sampled
@Composable
@WearPreviewDevices
@PreviewWrapper(RemoteComponentPreviewWrapper::class)
fun RemoteSliderSample(modifier: RemoteModifier = RemoteModifier) {
    val value = rememberMutableRemoteFloat(2f)
    RemoteSlider(
        value = value,
        steps = 4,
        decreaseAction = valueChange(value, max(value - 1f.rf, 0f.rf)),
        increaseAction = valueChange(value, min(value + 1f.rf, 5f.rf)),
        modifier = modifier,
    )
}

@Sampled
@Composable
@WearPreviewDevices
@PreviewWrapper(RemoteComponentPreviewWrapper::class)
fun RemoteSliderIntegerSample(modifier: RemoteModifier = RemoteModifier) {
    val value = rememberMutableRemoteInt(2)
    RemoteSlider(
        value = value,
        steps = 4,
        decreaseAction = valueChange(value, max(value - 1.ri, 0.ri)),
        increaseAction = valueChange(value, min(value + 1.ri, 5.ri)),
        modifier = modifier,
    )
}
