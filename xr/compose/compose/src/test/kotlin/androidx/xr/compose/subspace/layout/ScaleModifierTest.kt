/*
 * Copyright 2024 The Android Open Source Project
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

package androidx.xr.compose.subspace.layout

import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.test.junit4.createAndroidComposeRule
import androidx.compose.ui.unit.dp
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.xr.compose.subspace.SpatialPanel
import androidx.xr.compose.subspace.SubspaceComposable
import androidx.xr.compose.subspace.node.SubspaceSemanticsInfo
import androidx.xr.compose.testing.SubspaceTestingActivity
import androidx.xr.compose.testing.onSubspaceNodeWithTag
import androidx.xr.compose.testing.setSubspaceContent
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith
import kotlin.test.assertNotNull
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

/** Tests for scale modifiere. */
@RunWith(AndroidJUnit4::class)
class ScaleModifierTest {
    @get:Rule val composeTestRule = createAndroidComposeRule<SubspaceTestingActivity>()

    @Test
    fun scale_modifierAppliedToEntity() {
        composeTestRule.setSubspaceContent { PanelContent("panel", 0.5f) }

        val panelNode = assertSingleNode("panel")
        assertEquals(0.5f, panelNode.scale)
    }

    @Test
    fun negativeScale_throwsException() {
        assertFailsWith<IllegalArgumentException> {
            composeTestRule.setSubspaceContent { PanelContent("panel", -0.5f) }
        }
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
