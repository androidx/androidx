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
import androidx.compose.remote.creation.compose.layout.RemoteBox
import androidx.compose.remote.creation.compose.layout.RemoteCollapsibleColumn
import androidx.compose.remote.creation.compose.layout.RemoteCollapsibleRow
import androidx.compose.remote.creation.compose.layout.RemoteText
import androidx.compose.remote.creation.compose.modifier.RemoteModifier
import androidx.compose.remote.creation.compose.modifier.background
import androidx.compose.remote.creation.compose.modifier.height
import androidx.compose.remote.creation.compose.modifier.width
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
public fun RemoteCollapsibleColumnSample() {
    // A column with fixed height, children exceed this height.
    RemoteCollapsibleColumn(
        modifier = RemoteModifier.height(150.rdp).width(200.rdp).background(Color.LightGray.rc)
    ) {
        // Child 1: No explicit priority (Default: Float.MAX_VALUE - Highest)
        RemoteBox(
            modifier = RemoteModifier.height(100.rdp).width(200.rdp).background(Color.Red.rc)
        ) {
            RemoteText("Required (Default Priority)".rs)
        }

        // Child 2: Low priority (1f)
        RemoteBox(
            modifier =
                RemoteModifier.collapsiblePriority(1f)
                    .height(100.rdp)
                    .width(200.rdp)
                    .background(Color.Blue.rc)
        ) {
            RemoteText("Low Priority (1f)".rs)
        }

        // Child 3: Medium priority (5f)
        RemoteBox(
            modifier =
                RemoteModifier.collapsiblePriority(5f)
                    .height(100.rdp)
                    .width(200.rdp)
                    .background(Color.Green.rc)
        ) {
            RemoteText("Medium Priority (5f)".rs)
        }
    }
}

@Sampled
@PreviewWrapper(RemoteComponentPreviewWrapper::class)
@Composable
public fun RemoteCollapsibleRowSample() {
    // A row with fixed width, children exceed this width.
    RemoteCollapsibleRow(
        modifier = RemoteModifier.height(200.rdp).width(150.rdp).background(Color.LightGray.rc)
    ) {
        // Child 1: No explicit priority (Default: Float.MAX_VALUE - Highest)
        RemoteBox(
            modifier = RemoteModifier.width(100.rdp).height(200.rdp).background(Color.Red.rc)
        ) {
            RemoteText("Required".rs)
        }

        // Child 2: Low priority (1f)
        RemoteBox(
            modifier =
                RemoteModifier.collapsiblePriority(1f)
                    .width(100.rdp)
                    .height(200.rdp)
                    .background(Color.Blue.rc)
        ) {
            RemoteText("Low".rs)
        }

        // Child 3: Medium priority (5f)
        RemoteBox(
            modifier =
                RemoteModifier.collapsiblePriority(5f)
                    .width(100.rdp)
                    .height(200.rdp)
                    .background(Color.Green.rc)
        ) {
            RemoteText("Medium".rs)
        }
    }
}
