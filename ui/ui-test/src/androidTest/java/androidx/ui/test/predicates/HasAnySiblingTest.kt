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
import androidx.ui.test.hasAnySiblingThat
import androidx.ui.test.hasParentThat
import androidx.ui.test.hasTestTag
import androidx.ui.test.util.BoundaryNode
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.junit.runners.JUnit4

@MediumTest
@RunWith(JUnit4::class)
class HasAnySiblingTest {

    @get:Rule
    val composeTestRule =
        createComposeRule(disableTransitions = true)

    @Test
    fun findBySibling_oneSubtree_oneSibling_matches() {
        composeTestRule.setContent {
            BoundaryNode(testTag = "Node")
            BoundaryNode(testTag = "Parent") {
                BoundaryNode(testTag = "Me")
                BoundaryNode(testTag = "Sibling")
            }
        }

        find(hasAnySiblingThat(hasTestTag("Sibling")))
            .assert(hasTestTag("Me"))
    }

    @Test
    fun findBySibling_oneSubtree_twoSiblings_matchesTwo() {
        composeTestRule.setContent {
            BoundaryNode(testTag = "Node")
            BoundaryNode(testTag = "Parent") {
                BoundaryNode(testTag = "Me")
                BoundaryNode(testTag = "Sibling")
                BoundaryNode(testTag = "SomeoneElse")
            }
        }

        findAll(hasAnySiblingThat(hasTestTag("Sibling")))
            .assertCountEquals(2)
    }

    @Test
    fun findBySibling_oneSubtree_oneSibling_matchesTwo() {
        composeTestRule.setContent {
            BoundaryNode(testTag = "Node")
            BoundaryNode(testTag = "Parent") {
                BoundaryNode(testTag = "Sibling")
                BoundaryNode(testTag = "Sibling")
            }
        }

        findAll(hasAnySiblingThat(hasTestTag("Sibling")))
            .assertCountEquals(2)
    }

    @Test
    fun findBySibling_twoSubtrees_twoChildren_matches() {
        composeTestRule.setContent {
            BoundaryNode(testTag = "Node")
            BoundaryNode(testTag = "Parent") {
                BoundaryNode(testTag = "Me")
                BoundaryNode(testTag = "Sibling")
            }
            BoundaryNode(testTag = "Parent") {
                BoundaryNode(testTag = "Me")
                BoundaryNode(testTag = "Sibling")
            }
        }

        findAll(hasAnySiblingThat(hasTestTag("Sibling")))
            .assertCountEquals(2)
    }

    @Test
    fun findBySibling_noSiblings_nothingFound() {
        composeTestRule.setContent {
            BoundaryNode(testTag = "Parent") {
                BoundaryNode(testTag = "Me")
            }
        }

        find(hasAnySiblingThat(hasTestTag("Me")))
            .assertDoesNotExist()
    }

    @Test
    fun findBySibling_oneSiblings_differentTag_nothingFound() {
        composeTestRule.setContent {
            BoundaryNode(testTag = "Parent") {
                BoundaryNode(testTag = "Me")
                BoundaryNode(testTag = "Sibling")
            }
        }

        find(hasAnySiblingThat(hasTestTag("Sibling2")))
            .assertDoesNotExist()
    }

    @Test
    fun findBySibling_oneSiblings_notMySibling_nothingFound() {
        composeTestRule.setContent {
            BoundaryNode(testTag = "Parent") {
                BoundaryNode(testTag = "Me")
            }
            BoundaryNode(testTag = "Parent") {
                BoundaryNode(testTag = "SomeoneElse")
                BoundaryNode(testTag = "Sibling")
            }
        }

        find(hasAnySiblingThat(hasTestTag("Sibling")) and hasTestTag("Me"))
            .assertDoesNotExist()
    }

    @Test
    fun findByParentSibling_oneFound() {
        composeTestRule.setContent {
            BoundaryNode(testTag = "Grandparent") {
                BoundaryNode(testTag = "Parent") {
                    BoundaryNode(testTag = "Me")
                }
                BoundaryNode(testTag = "ParentSibling") {
                    BoundaryNode(testTag = "SomeoneElse")
                }
            }
        }

        find(hasParentThat(hasAnySiblingThat(hasTestTag("ParentSibling"))))
            .assert(hasTestTag("Me"))
    }
}