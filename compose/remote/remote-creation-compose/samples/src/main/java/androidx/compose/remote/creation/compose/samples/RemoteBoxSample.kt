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
import androidx.compose.remote.creation.compose.layout.RemoteBox
import androidx.compose.remote.creation.compose.layout.RemoteText
import androidx.compose.remote.creation.compose.modifier.RemoteModifier
import androidx.compose.remote.creation.compose.modifier.background
import androidx.compose.remote.creation.compose.modifier.size
import androidx.compose.remote.creation.compose.previews.utils.RemoteComponentPreviewWrapper
import androidx.compose.remote.creation.compose.state.rc
import androidx.compose.remote.creation.compose.state.rdp
import androidx.compose.remote.creation.compose.state.rs
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.tooling.preview.PreviewWrapper

@Sampled
@PreviewWrapper(RemoteComponentPreviewWrapper::class)
@Composable
fun RemoteBoxSample() {
    RemoteBox(
        modifier = RemoteModifier.size(200.rdp).background(Color.LightGray.rc),
        contentAlignment = RemoteAlignment.Center,
    ) {
        RemoteBox(modifier = RemoteModifier.size(100.rdp).background(Color.Blue.rc))
        RemoteText(text = "Centered".rs, color = Color.White.rc)
    }
}

@Sampled
@PreviewWrapper(RemoteComponentPreviewWrapper::class)
@Composable
fun RemoteBoxStackingSample() {
    // Outer box centered
    RemoteBox(
        modifier = RemoteModifier.size(200.rdp).background(Color.LightGray.rc),
        contentAlignment = RemoteAlignment.Center,
    ) {
        // Red box (150dp) also centered in outer box
        // It aligns its own children to TopStart
        RemoteBox(
            modifier = RemoteModifier.size(150.rdp).background(Color.Red.rc),
            contentAlignment = RemoteAlignment.TopStart,
        ) {
            RemoteBox(modifier = RemoteModifier.size(50.rdp).background(Color.Yellow.rc))

            // Nested depth to show stacking within aligned container
            RemoteBox(
                modifier = RemoteModifier.size(30.rdp).background(Color.Black.rc),
                contentAlignment = RemoteAlignment.Center,
            ) {
                RemoteBox(modifier = RemoteModifier.size(10.rdp).background(Color.White.rc))
            }
        }

        // Green box (100dp) centered in outer box (overlaps Red)
        // It aligns its own children to BottomEnd
        RemoteBox(
            modifier = RemoteModifier.size(100.rdp).background(Color.Green.rc),
            contentAlignment = RemoteAlignment.BottomEnd,
        ) {
            RemoteBox(modifier = RemoteModifier.size(40.rdp).background(Color.Magenta.rc))
        }

        // Blue box (50dp) centered in outer box (overlaps Green)
        // It aligns its own children to Center
        RemoteBox(
            modifier = RemoteModifier.size(50.rdp).background(Color.Blue.rc),
            contentAlignment = RemoteAlignment.Center,
        ) {
            RemoteBox(modifier = RemoteModifier.size(20.rdp).background(Color.Cyan.rc))
        }
    }
}
