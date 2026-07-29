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
import androidx.compose.remote.creation.compose.modifier.RemoteModifier
import androidx.compose.remote.creation.compose.state.ri
import androidx.compose.runtime.Composable
import androidx.compose.ui.tooling.preview.PreviewWrapper
import androidx.wear.compose.remote.material3.RemoteHorizontalPageIndicator
import androidx.wear.compose.remote.material3.RemoteVerticalPageIndicator
import androidx.wear.compose.remote.material3.previews.utils.RemoteComponentPreviewWrapper
import androidx.wear.compose.remote.material3.rememberRemotePageIndicatorState
import androidx.wear.compose.ui.tooling.preview.WearPreviewDevices

@Sampled
@Composable
@WearPreviewDevices
@PreviewWrapper(RemoteComponentPreviewWrapper::class)
fun RemoteHorizontalPageIndicatorSample(modifier: RemoteModifier = RemoteModifier) {
    val state = rememberRemotePageIndicatorState(selectedPage = 1.ri, pageCount = 3)
    RemoteHorizontalPageIndicator(state = state, modifier = modifier)
}

@Sampled
@Composable
@WearPreviewDevices
@PreviewWrapper(RemoteComponentPreviewWrapper::class)
fun RemoteVerticalPageIndicatorSample(modifier: RemoteModifier = RemoteModifier) {
    val state = rememberRemotePageIndicatorState(selectedPage = 1.ri, pageCount = 3)
    RemoteVerticalPageIndicator(state = state, modifier = modifier)
}
