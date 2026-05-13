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

package androidx.xr.compose.testing.samples

import androidx.annotation.Sampled
import androidx.compose.material3.Button
import androidx.compose.material3.Text
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.compose.ui.unit.dp
import androidx.xr.compose.spatial.Subspace
import androidx.xr.compose.subspace.SpatialPanel
import androidx.xr.compose.subspace.layout.SubspaceModifier
import androidx.xr.compose.subspace.layout.height
import androidx.xr.compose.subspace.layout.width
import androidx.xr.compose.subspace.semantics.testTag
import androidx.xr.compose.testing.assertHeightIsEqualTo
import androidx.xr.compose.testing.assertPositionInRootIsEqualTo
import androidx.xr.compose.testing.assertWidthIsEqualTo
import androidx.xr.compose.testing.onSubspaceNodeWithTag
import androidx.xr.compose.testing.toDp

@Sampled
public fun subspacePanelRenderedAndInteractive() {
    var count = 0

    composeTestRule.setContent {
        Subspace {
            SpatialPanel(SubspaceModifier.testTag("spatialPanel")) {
                Button(onClick = { count++ }) { Text("Increment") }
            }
        }
    }

    // Assert subspace node existence, position, and dimensions in the Spatial hierarchy
    composeTestRule
        .onSubspaceNodeWithTag("spatialPanel")
        .assertExists()
        .assertPositionInRootIsEqualTo(0.toDp(), 0.toDp(), 0.toDp())

    // Interact with the 2D Compose node nested within the Spatial container
    composeTestRule.onNodeWithText("Increment").performClick()

    composeTestRule.waitForIdle()

    // Verify outcomes
    assert(count == 1)
}

@Sampled
public fun subspaceNodeMatcherProperties() {
    composeTestRule.setContent {
        Subspace {
            SpatialPanel(SubspaceModifier.width(100.dp).height(100.dp).testTag("myPanel")) {}
        }
    }

    // Check existence and exact spatial dimensions in DP using semantic matchers
    composeTestRule
        .onSubspaceNodeWithTag("myPanel")
        .assertExists()
        .assertPositionInRootIsEqualTo(0.dp, 0.dp, 0.dp)
        .assertWidthIsEqualTo(100.toDp())
        .assertHeightIsEqualTo(100.toDp())
}
