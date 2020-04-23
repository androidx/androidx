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
import androidx.ui.test.hasParentThat
import androidx.ui.test.hasTestTag
import androidx.ui.test.util.BoundaryNode
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.junit.runners.JUnit4

@MediumTest
@RunWith(JUnit4::class)
class HasParentTest {

    @get:Rule
    val composeTestRule =
        createComposeRule(disableTransitions = true)

    @Test
    fun findByParent_oneSubtree_oneChild_matches() {
        composeTestRule.setContent {
            BoundaryNode(testTag = "Node")
            BoundaryNode(testTag = "Parent") {
                BoundaryNode(testTag = "Child")
            }
        }

        find(hasParentThat(hasTestTag("Parent")))
            .assert(hasTestTag("Child"))
    }

    @Test
    fun findByParent_oneSubtree_twoChildren_matches() {
        composeTestRule.setContent {
            BoundaryNode(testTag = "Node")
            BoundaryNode(testTag = "Parent") {
                BoundaryNode(testTag = "Child1")
                BoundaryNode(testTag = "Child2")
            }
        }

        findAll(hasParentThat(hasTestTag("Parent")))
            .assertCountEquals(2)
    }

    @Test
    fun findByParent_twoSubtrees_twoChildren_matches() {
        composeTestRule.setContent {
            BoundaryNode(testTag = "Node")
            BoundaryNode(testTag = "Parent") {
                BoundaryNode(testTag = "Child1")
                BoundaryNode(testTag = "Child2")
            }
            BoundaryNode(testTag = "Parent") {
                BoundaryNode(testTag = "Child3")
                BoundaryNode(testTag = "Child4")
            }
        }

        findAll(hasParentThat(hasTestTag("Parent")))
            .assertCountEquals(4)
    }

    @Test
    fun findByParent_nothingFound() {
        composeTestRule.setContent {
            BoundaryNode(testTag = "Parent") {
                BoundaryNode(testTag = "ExtraNode") {
                    BoundaryNode(testTag = "Child")
                }
            }
        }

        find(hasParentThat(hasTestTag("Parent"))
                and hasTestTag("Child"))
            .assertDoesNotExist()
    }

    @Test
    fun findByGrandParent_oneFound() {
        composeTestRule.setContent {
            BoundaryNode(testTag = "Parent") {
                BoundaryNode(testTag = "ExtraNode") {
                    BoundaryNode(testTag = "Child")
                }
            }
        }

        find(hasParentThat(hasParentThat(hasTestTag("Parent"))))
            .assert(hasTestTag("Child"))
    }
}