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
import androidx.xr.compose.spatial.Subspace
import androidx.xr.compose.subspace.SpatialPanel
import androidx.xr.compose.subspace.layout.SubspaceModifier
import androidx.xr.compose.subspace.layout.fillMaxSize
import androidx.xr.compose.subspace.layout.requiredSizeIn

@Sampled
public fun SubspaceSample() {
    @Composable
    fun AppContent() {
        // By default, Subspace is automatically bounded by the system's recommended content box.
        // SubspaceModifiers like fillMaxSize() will expand to fill this recommended box.
        Subspace {
            SpatialPanel(SubspaceModifier.fillMaxSize()) {
                Text("Panel filling the default recommended content box")
            }
        }

        // To escape the default recommended content box constraints and define a custom
        // bounded or unbounded Subspace, apply the requiredSizeIn modifier.
        Subspace(
            modifier =
                SubspaceModifier.requiredSizeIn(
                    maxWidth = 10000.dp,
                    maxHeight = 10000.dp,
                    maxDepth = 10000.dp,
                )
        ) {
            SpatialPanel(SubspaceModifier.fillMaxSize()) {
                Text("Panel in a custom sized Subspace escaping default constraints")
            }
        }
    }
}
