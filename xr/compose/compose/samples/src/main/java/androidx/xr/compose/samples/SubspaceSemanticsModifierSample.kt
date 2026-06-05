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
import androidx.compose.foundation.layout.Column
import androidx.compose.material3.Button
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.xr.compose.spatial.Subspace
import androidx.xr.compose.subspace.SpatialPanel
import androidx.xr.compose.subspace.layout.SubspaceModifier
import androidx.xr.compose.subspace.semantics.contentDescription
import androidx.xr.compose.subspace.semantics.semantics
import androidx.xr.compose.subspace.semantics.testTag

@Sampled
public fun SubspaceSemanticsModifierSample() {
    @Composable
    fun AppContent() {
        // Mental Model: The "Picture Frame" (3D Container) vs. the "Canvas" (2D Content)
        Subspace {
            SpatialPanel(
                modifier =
                    SubspaceModifier.semantics {
                        // Set spatial semantics on the 3D container (the "Picture Frame")
                        testTag = "main_settings_panel"
                        contentDescription = "System Settings Window"
                    }
            ) {
                // Standard 2D Compose UI goes inside (the "Canvas").
                // These elements use standard Modifier.semantics for TalkBack and interactions.
                Column {
                    Text("System Settings")
                    Button(
                        onClick = { /* do something */ },
                        modifier = Modifier.semantics { contentDescription = "Perform action" },
                    ) {
                        Text("Click Me")
                    }
                }
            }
        }
    }
}
