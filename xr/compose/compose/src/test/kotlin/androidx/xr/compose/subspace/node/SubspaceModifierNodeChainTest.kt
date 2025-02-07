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

package androidx.xr.compose.subspace.node

import androidx.compose.material3.Button
import androidx.compose.material3.Text
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.test.junit4.createAndroidComposeRule
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.performClick
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.xr.compose.subspace.SpatialPanel
import androidx.xr.compose.subspace.layout.CoreEntity
import androidx.xr.compose.subspace.layout.CoreEntityNode
import androidx.xr.compose.subspace.layout.SubspaceModifier
import androidx.xr.compose.testing.SubspaceTestingActivity
import androidx.xr.compose.testing.setSubspaceContent
import com.google.common.truth.Truth.assertThat
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

/** Tests for [SubspaceModifierNodeChain]. */
@RunWith(AndroidJUnit4::class)
class SubspaceModifierNodeChainTest {
    @get:Rule val composeTestRule = createAndroidComposeRule<SubspaceTestingActivity>()

    // This is used to track the number of times CountNode is reused.
    var nodeCount = 0

    @Before
    fun setUp() {
        nodeCount = 0
    }

    @Test
    fun nodeChain_statefulModifierNodesAreReused() {
        composeTestRule.setSubspaceContent {
            var count by remember { mutableStateOf(100) }
            SpatialPanel(SubspaceModifier.count(count = count)) {
                Button(modifier = Modifier.testTag("button"), onClick = { count += 1 }) {
                    Text(text = "Click to recompose")
                }
            }
        }

        // There should be a single initial composition.
        assertThat(nodeCount).isEqualTo(1)

        // Trigger one recomposition.
        composeTestRule.onNodeWithTag("button").performClick()
        composeTestRule.waitForIdle()
        assertThat(nodeCount).isEqualTo(2)

        // Trigger one recomposition.
        composeTestRule.onNodeWithTag("button").performClick()
        composeTestRule.waitForIdle()
        assertThat(nodeCount).isEqualTo(3)

        // Trigger two recompositions.
        composeTestRule.onNodeWithTag("button").performClick()
        composeTestRule.onNodeWithTag("button").performClick()
        composeTestRule.waitForIdle()
        assertThat(nodeCount).isEqualTo(5)
    }

    private fun SubspaceModifier.count(count: Int): SubspaceModifier =
        this.then(CountElement(count))

    private inner class CountElement(private val count: Int) :
        SubspaceModifierElement<CountNode>() {

        override fun create(): CountNode = CountNode(count)

        override fun update(node: CountNode) {
            node.count = count
        }

        override fun hashCode(): Int {
            return count.hashCode()
        }

        override fun equals(other: Any?): Boolean {
            if (this === other) return true
            if (other !is CountElement) return false

            return count == other.count
        }
    }

    private inner class CountNode(public var count: Int) : SubspaceModifier.Node(), CoreEntityNode {
        // This is used to track the number of times the node is reused.
        private var internalCount = 0

        override fun modifyCoreEntity(coreEntity: CoreEntity) {
            nodeCount = ++internalCount
        }
    }
}
