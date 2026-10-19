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

package androidx.xr.compose.subspace.draw

import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.ui.test.junit4.createAndroidComposeRule
import androidx.compose.ui.unit.dp
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.xr.compose.spatial.Subspace
import androidx.xr.compose.subspace.SpatialPanel
import androidx.xr.compose.subspace.SubspaceComposable
import androidx.xr.compose.subspace.layout.SubspaceModifier
import androidx.xr.compose.subspace.layout.size
import androidx.xr.compose.subspace.node.SubspaceSemanticsInfo
import androidx.xr.compose.subspace.semantics.testTag
import androidx.xr.compose.testing.SubspaceTestingActivity
import androidx.xr.compose.testing.onSubspaceNodeWithTag
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith
import kotlin.test.assertFalse
import kotlin.test.assertNotNull
import kotlin.test.assertTrue
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

/** Tests for scale modifier. */
@RunWith(AndroidJUnit4::class)
class ScaleModifierTest {

    // Migrate to `androidx.compose.ui.test.junit4.v2.createAndroidComposeRule`,
    // available starting with v1.11.0.
    // See API docs for details.
    @Suppress("DEPRECATION")
    @get:Rule
    val composeTestRule = createAndroidComposeRule<SubspaceTestingActivity>()

    @Test
    fun scale_modifierAppliedToEntity() {
        composeTestRule.setContent { Subspace { PanelContent("panel", 0.5f) } }

        val panelNode = assertSingleNode("panel")
        assertEquals(0.5f, panelNode.scale)
    }

    @Test
    fun negativeScale_throwsException() {
        assertFailsWith<IllegalArgumentException> {
            composeTestRule.setContent { Subspace { PanelContent("panel", -0.5f) } }
        }
    }

    @Test
    fun infiniteScale_throwsException() {
        assertFailsWith<IllegalArgumentException> {
            SubspaceModifier.scale(Float.POSITIVE_INFINITY)
        }
    }

    @Test
    fun nanScale_throwsException() {
        assertFailsWith<IllegalArgumentException> { SubspaceModifier.scale(Float.NaN) }
    }

    @Test
    fun scale_equalsAndHashCode() {
        val modifier1 = SubspaceModifier.scale(0.5f)
        val modifier2 = SubspaceModifier.scale(0.5f)
        val modifierDifferent = SubspaceModifier.scale(0.8f)

        assertTrue(modifier1 == modifier2)
        assertEquals(modifier1.hashCode(), modifier2.hashCode())
        assertFalse(modifier1 == modifierDifferent)
    }

    @Test
    fun scale_resetsToOneWhenModifierRemoved() {
        var scale by mutableStateOf<Float?>(0.5f)

        composeTestRule.setContent { Subspace { PanelContent("panel", scale) } }

        assertEquals(0.5f, assertSingleNode("panel").scale)

        scale = null

        assertEquals(1.0f, assertSingleNode("panel").scale)
    }

    @Test
    fun scale_multiple_updatesAndResetsWhenModifiersRemoved() {
        var scaleCount by mutableStateOf(2)

        composeTestRule.setContent {
            Subspace {
                val modifier =
                    when (scaleCount) {
                        2 -> SubspaceModifier.testTag("panel").size(100.dp).scale(0.5f).scale(0.5f)
                        1 -> SubspaceModifier.testTag("panel").size(100.dp).scale(0.5f)
                        else -> SubspaceModifier.testTag("panel").size(100.dp)
                    }
                SpatialPanel(modifier = modifier) { Text(text = "Panel") }
            }
        }

        assertEquals(0.25f, assertSingleNode("panel").scale)

        scaleCount = 1

        assertEquals(0.5f, assertSingleNode("panel").scale)

        scaleCount = 0

        assertEquals(1.0f, assertSingleNode("panel").scale)
    }

    private fun assertSingleNode(testTag: String): SubspaceSemanticsInfo {
        val subspaceNode = composeTestRule.onSubspaceNodeWithTag(testTag).fetchSemanticsNode()
        assertNotNull(subspaceNode)
        return subspaceNode
    }

    @SubspaceComposable
    @Composable
    private fun PanelContent(testTag: String, scale: Float? = null) {
        var modifier = SubspaceModifier.testTag(testTag).size(100.dp)

        scale?.let { modifier = modifier.scale(it) }

        SpatialPanel(modifier = modifier) { Text(text = "Panel") }
    }
}
