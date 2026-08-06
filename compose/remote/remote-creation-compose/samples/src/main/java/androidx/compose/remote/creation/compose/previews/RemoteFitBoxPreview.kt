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

package androidx.compose.remote.creation.compose.previews

import androidx.compose.remote.creation.compose.action.valueChange
import androidx.compose.remote.creation.compose.layout.RemoteAlignment
import androidx.compose.remote.creation.compose.layout.RemoteArrangement
import androidx.compose.remote.creation.compose.layout.RemoteBox
import androidx.compose.remote.creation.compose.layout.RemoteColumn
import androidx.compose.remote.creation.compose.layout.RemoteFitBox
import androidx.compose.remote.creation.compose.layout.RemoteText
import androidx.compose.remote.creation.compose.modifier.RemoteModifier
import androidx.compose.remote.creation.compose.modifier.background
import androidx.compose.remote.creation.compose.modifier.clickable
import androidx.compose.remote.creation.compose.modifier.padding
import androidx.compose.remote.creation.compose.modifier.size
import androidx.compose.remote.creation.compose.previews.utils.RemoteComponentPreviewWrapper
import androidx.compose.remote.creation.compose.state.rc
import androidx.compose.remote.creation.compose.state.rdp
import androidx.compose.remote.creation.compose.state.rememberMutableRemoteBoolean
import androidx.compose.remote.creation.compose.state.rs
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.tooling.preview.PreviewWrapper

@Preview
@PreviewWrapper(RemoteComponentPreviewWrapper::class)
@Composable
private fun RemoteFitBoxPreview() {
    val useLargeSize = rememberMutableRemoteBoolean(true)
    val containerSize = useLargeSize.select(200.rdp, 100.rdp)

    RemoteColumn(horizontalAlignment = RemoteAlignment.CenterHorizontally) {
        // Toggle Button
        RemoteBox(
            modifier =
                RemoteModifier.padding(10.rdp)
                    .size(150.rdp, 50.rdp)
                    .clickable(valueChange(useLargeSize, !useLargeSize))
                    .background(Color.Blue.rc),
            contentAlignment = RemoteAlignment.Center,
        ) {
            RemoteText(useLargeSize.select("Size: 200".rs, "Size: 100".rs), color = Color.White.rc)
        }

        // Parent container that constrains the size
        RemoteBox(modifier = RemoteModifier.size(containerSize).background(Color.LightGray.rc)) {
            // RemoteFitBox wraps/fills the parent constraints
            RemoteFitBox(
                horizontalAlignment = RemoteAlignment.CenterHorizontally,
                verticalArrangement = RemoteArrangement.Center,
            ) {
                // Child 1 (Large, fits in 200, but not 100)
                RemoteBox(
                    modifier = RemoteModifier.size(150.rdp).background(Color.Red.rc),
                    contentAlignment = RemoteAlignment.Center,
                ) {
                    RemoteText("Child 1 (150)".rs, color = Color.White.rc)
                }

                // Child 2 (Small, fits in 100)
                RemoteBox(
                    modifier = RemoteModifier.size(80.rdp).background(Color.Green.rc),
                    contentAlignment = RemoteAlignment.Center,
                ) {
                    RemoteText("Child 2 (80)".rs, color = Color.White.rc)
                }
            }
        }
    }
}
