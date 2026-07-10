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

package androidx.xr.compose.samples

import androidx.annotation.Sampled
import androidx.compose.foundation.layout.Row
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.unit.dp
import androidx.xr.compose.spatial.PlanarEmbeddedSubspace
import androidx.xr.compose.subspace.SpatialPanel
import androidx.xr.compose.subspace.SubspaceComposable
import androidx.xr.compose.subspace.layout.SubspaceModifier
import androidx.xr.compose.subspace.layout.offset

@Sampled
public fun PlanarEmbeddedSubspaceSample() {

    @Composable
    @SubspaceComposable
    fun PanelWithPlanarEmbeddedSubspace() {
        // A PlanarEmbeddedSubspace is placed inside a SpatialPanel.
        // The 3D content inside the PlanarEmbeddedSubspace is positioned
        // relative to the 2D area of the SpatialPanel.
        // Here we place it in a Row to demonstrate that it participates in 2D layout.
        SpatialPanel {
            Row {
                Text("2D content")

                PlanarEmbeddedSubspace {
                    // The content of a PlanarEmbeddedSubspace must be a SubspaceComposable.
                    // Here we use another SpatialPanel to host 2D content. However,
                    // this could be any 3D content (i.e. glTFs).
                    SpatialPanel(SubspaceModifier.offset(z = (-50).dp)) {
                        Text("Embedded 3D content")
                    }
                }
            }
        }
    }
}
