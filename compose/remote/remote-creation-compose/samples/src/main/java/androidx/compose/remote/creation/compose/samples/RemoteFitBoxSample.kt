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

package androidx.compose.remote.creation.compose.samples

import androidx.annotation.Sampled
import androidx.compose.remote.creation.compose.layout.RemoteAlignment
import androidx.compose.remote.creation.compose.layout.RemoteArrangement
import androidx.compose.remote.creation.compose.layout.RemoteBox
import androidx.compose.remote.creation.compose.layout.RemoteFitBox
import androidx.compose.remote.creation.compose.modifier.RemoteModifier
import androidx.compose.remote.creation.compose.modifier.background
import androidx.compose.remote.creation.compose.modifier.size
import androidx.compose.remote.creation.compose.previews.utils.RemoteComponentPreviewWrapper
import androidx.compose.remote.creation.compose.state.rc
import androidx.compose.remote.creation.compose.state.rdp
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.tooling.preview.PreviewWrapper

@Sampled
@PreviewWrapper(RemoteComponentPreviewWrapper::class)
@Composable
fun RemoteFitBoxSample() {
    // The parent RemoteBox constrains the available space to 40.rdp.
    // RemoteFitBox will measure its children against these constraints.
    // Child 1 (50.rdp) is too large to fit, so the RemoteFitBox skips it and selects
    // Child 2 (30.rdp) which fits within the 40.rdp constraints.
    RemoteBox(modifier = RemoteModifier.size(40.rdp).background(Color.LightGray.rc)) {
        RemoteFitBox(
            horizontalAlignment = RemoteAlignment.CenterHorizontally,
            verticalArrangement = RemoteArrangement.Center,
        ) {
            // Child 1: Too large for 40.rdp container
            RemoteBox(RemoteModifier.size(50.rdp).background(Color.Red.rc))

            // Child 2: Fits in 40.rdp container and will be rendered
            RemoteBox(RemoteModifier.size(30.rdp).background(Color.Blue.rc))
        }
    }
}
