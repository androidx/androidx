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

package androidx.xr.compose.samples

import androidx.annotation.Sampled
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.unit.dp
import androidx.xr.compose.subspace.SpatialPanel
import androidx.xr.compose.subspace.SubspaceComposable
import androidx.xr.compose.subspace.layout.SpatialResizeEventType
import androidx.xr.compose.subspace.layout.SubspaceModifier
import androidx.xr.compose.subspace.layout.height
import androidx.xr.compose.subspace.layout.resizable
import androidx.xr.compose.subspace.layout.width

/**
 * A sample demonstrating a resizable component where the developer manages the state and applies
 * the resulting size.
 */
@Sampled
@SubspaceComposable
@Composable
public fun ResizableWithStateSample() {
    val density = LocalDensity.current
    var panelWidth by remember { mutableStateOf(400.dp) }
    var panelHeight by remember { mutableStateOf(300.dp) }

    SpatialPanel(
        modifier =
            SubspaceModifier.width(panelWidth)
                .height(panelHeight)
                .resizable(
                    onResize = { event ->
                        // The developer decides when and how to apply the new size.
                        // In this example, we update our state when the resize interaction ends.
                        if (event.type == SpatialResizeEventType.End) {
                            with(density) {
                                panelWidth = event.size.width.toDp()
                                panelHeight = event.size.height.toDp()
                            }
                        }
                    }
                )
    ) {
        Text("Resizable with size state.")
    }
}
