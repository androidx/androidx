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
import androidx.xr.compose.spatial.Subspace
import androidx.xr.compose.subspace.SpatialColumn
import androidx.xr.compose.subspace.SpatialPanel
import androidx.xr.compose.subspace.layout.SpatialAlignment
import androidx.xr.compose.subspace.layout.SpatialArrangement
import androidx.xr.compose.subspace.layout.SubspaceModifier

@Sampled
@Composable
public fun SpatialColumnSample() {
    Subspace {
        SpatialColumn(
            horizontalAlignment = SpatialAlignment.CenterHorizontally,
            verticalArrangement = SpatialArrangement.Center,
        ) {
            SpatialPanel(SubspaceModifier.weight(0.3f)) { Text("Top Panel") }
            SpatialPanel(SubspaceModifier.weight(0.3f)) { Text("Middle Panel") }
            SpatialPanel(SubspaceModifier.weight(0.3f)) { Text("Bottom Panel") }
        }
    }
}
