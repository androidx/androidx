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
import androidx.compose.ui.unit.dp
import androidx.xr.compose.subspace.SpatialPanel
import androidx.xr.compose.subspace.SubspaceComposable
import androidx.xr.compose.subspace.layout.SubspaceModifier
import androidx.xr.compose.subspace.layout.transformingResizable
import androidx.xr.compose.unit.DpVolumeSize

/** A sample demonstrating a simple resizable component where the system manages the layout. */
@Sampled
@SubspaceComposable
@Composable
public fun BasicTransformingResizableSample() {
    SpatialPanel(
        modifier =
            SubspaceModifier.transformingResizable(
                minimumSize = DpVolumeSize(100.dp, 100.dp, 0.dp),
                maximumSize = DpVolumeSize(800.dp, 800.dp, 0.dp),
            )
    ) {
        Text("Transforming Resizable")
    }
}
