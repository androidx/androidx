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

package androidx.compose.ui.test.selectors

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.test.assert
import androidx.compose.ui.test.assertCountEquals
import androidx.compose.ui.test.filter
import androidx.compose.ui.test.hasTestTag
import androidx.compose.ui.test.junit4.v2.createComposeRule
import androidx.compose.ui.test.onDescendants
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.util.BoundaryNode
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.filters.MediumTest
import kotlinx.coroutines.test.StandardTestDispatcher
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

@MediumTest
@RunWith(AndroidJUnit4::class)
class DescendantsSelectorTest {

    @get:Rule val rule = createComposeRule(StandardTestDispatcher())

    @Test
    fun deepDescendants() {
        rule.setContent {
            BoundaryNode(testTag = "NodeA") {
                BoundaryNode(testTag = "NodeB") { BoundaryNode(testTag = "NodeC") }
                BoundaryNode(testTag = "NodeD")
            }
        }

        rule.onNodeWithTag("NodeA").onDescendants().assertCountEquals(3).apply {
            get(0).assert(hasTestTag("NodeB"))
            get(1).assert(hasTestTag("NodeC"))
            get(2).assert(hasTestTag("NodeD"))
        }
    }

    @Test
    fun noDescendants() {
        rule.setContent { BoundaryNode(testTag = "Node") }

        rule.onNodeWithTag("Node").onDescendants().assertCountEquals(0)
    }

    @Test
    fun descendantsInUnmergedTree() {
        rule.setContent {
            Box(Modifier.testTag("NodeA").semantics(mergeDescendants = true) {}) {
                Box(Modifier.testTag("NodeB"))
            }
        }

        // By default, (useUnmergedTree = false), the NodeB is merged and NOT visible as a
        // descendant.
        rule.onNodeWithTag("NodeA").onDescendants().assertCountEquals(0)

        // With useUnmergedTree = true, the NodeB is accessible in the unmerged tree.
        rule
            .onNodeWithTag("NodeA", useUnmergedTree = true)
            .onDescendants()
            .assertCountEquals(1)
            .apply { get(0).assert(hasTestTag("NodeB")) }
    }

    @Test
    fun descendantsScoping() {
        rule.setContent {
            Column {
                BoundaryNode(testTag = "NodeA") { BoundaryNode(testTag = "NodeC") }
                BoundaryNode(testTag = "NodeB") { BoundaryNode(testTag = "NodeC") }
            }
        }

        rule.onAllNodes(hasTestTag("NodeC")).assertCountEquals(2)

        rule.onNodeWithTag("NodeA").onDescendants().filter(hasTestTag("NodeC")).assertCountEquals(1)
    }
}
