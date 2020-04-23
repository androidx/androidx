/*
 * Copyright 2020 The Android Open Source Project
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

package androidx.ui.test.predicates

import androidx.test.filters.MediumTest
import androidx.ui.test.assert
import androidx.ui.test.assertCountEquals
import androidx.ui.test.createComposeRule
import androidx.ui.test.find
import androidx.ui.test.findAll
import androidx.ui.test.hasAnyDescendantThat
import androidx.ui.test.hasTestTag
import androidx.ui.test.util.BoundaryNode
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.junit.runners.JUnit4

@MediumTest
@RunWith(JUnit4::class)
class HasAnyDescendantTest {

    @get:Rule
    val composeTestRule =
        createComposeRule(disableTransitions = true)

    @Test
    fun findByDescendant_directDescendant_matches() {
        composeTestRule.setContent {
            BoundaryNode(testTag = "Node")
            BoundaryNode(testTag = "Parent") {
                BoundaryNode(testTag = "Child")
            }
        }

        find(hasAnyDescendantThat(hasTestTag("Child")) and hasTestTag("Parent"))
            .assert(hasTestTag("Parent"))
    }

    @Test
    fun findByDescendant_indirectDescendant_matches() {
        composeTestRule.setContent {
            BoundaryNode(testTag = "Node")
            BoundaryNode(testTag = "Grandparent") {
                BoundaryNode(testTag = "Parent") {
                    BoundaryNode(testTag = "Child")
                }
            }
        }

        find(hasAnyDescendantThat(hasTestTag("Child")) and !hasTestTag("Parent")
                and hasTestTag("Grandparent"))
            .assert(hasTestTag("Grandparent"))
    }

    @Test
    fun findByDescendant_justSelf_oneMatch() {
        composeTestRule.setContent {
            BoundaryNode(testTag = "Node")
        }

        find(hasAnyDescendantThat(hasTestTag("Node")))
            .assertExists() // Root node
    }

    @Test
    fun findByDescendant_twoSubtrees_threeMatches() {
        composeTestRule.setContent {
            BoundaryNode(testTag = "Node")
            BoundaryNode(testTag = "Parent") {
                BoundaryNode(testTag = "Child")
            }
            BoundaryNode(testTag = "Parent2") {
                BoundaryNode(testTag = "Child")
            }
        }

        findAll(hasAnyDescendantThat(hasTestTag("Child")))
            .assertCountEquals(3) // Parent, Parent2 and root
    }
}