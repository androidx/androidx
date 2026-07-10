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
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.xr.compose.spatial.Subspace
import androidx.xr.compose.subspace.SpatialMainPanel
import androidx.xr.compose.subspace.SpatialPanel
import androidx.xr.compose.subspace.SpatialRow

@Sampled
public fun SpatialMainPanelSample() {
    @Composable
    fun MainPanelContent() {
        Text("Main panel")
    }

    @Composable
    fun AppContent() {
        // 2D Content rendered to the main panel.
        MainPanelContent()

        // Spatial content rendered in full space mode.
        Subspace {
            SpatialRow {
                SpatialPanel { Text("Spatial panel") }
                SpatialMainPanel()
            }
        }
    }
}
