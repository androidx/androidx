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
import androidx.ui.test.hasAnyAncestorThat
import androidx.ui.test.hasTestTag
import androidx.ui.test.util.BoundaryNode
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.junit.runners.JUnit4

@MediumTest
@RunWith(JUnit4::class)
class HasAnyAncestorTest {

    @get:Rule
    val composeTestRule =
        createComposeRule(disableTransitions = true)

    @Test
    fun findByAncestor_oneAncestor_oneMatch() {
        composeTestRule.setContent {
            BoundaryNode(testTag = "Node")
            BoundaryNode(testTag = "Parent") {
                BoundaryNode(testTag = "Child")
            }
        }

        find(hasAnyAncestorThat(hasTestTag("Parent")))
            .assert(hasTestTag("Child"))
    }

    @Test
    fun findByAncestor_oneAncestor_twoChildren_twoMatches() {
        composeTestRule.setContent {
            BoundaryNode(testTag = "Node")
            BoundaryNode(testTag = "Grandparent") {
                BoundaryNode(testTag = "Parent") {
                    BoundaryNode(testTag = "Child1")
                    BoundaryNode(testTag = "Child2")
                }
            }
        }

        findAll(hasAnyAncestorThat(hasTestTag("Parent")))
            .assertCountEquals(2)
    }

    @Test
    fun findByAncestor_twoAncestors_oneMatch() {
        composeTestRule.setContent {
            BoundaryNode(testTag = "Node")
            BoundaryNode(testTag = "Grandparent") {
                BoundaryNode(testTag = "Parent") {
                    BoundaryNode(testTag = "Child")
                }
            }
        }

        find(hasAnyAncestorThat(hasTestTag("Grandparent"))
                and !hasTestTag("Parent"))
            .assert(hasTestTag("Child"))
    }

    @Test
    fun findByAncestor_twoAncestors_twoMatches() {
        composeTestRule.setContent {
            BoundaryNode(testTag = "Node")
            BoundaryNode(testTag = "Grandparent") {
                BoundaryNode(testTag = "Parent") {
                    BoundaryNode(testTag = "Child")
                }
            }
        }

        findAll(hasAnyAncestorThat(hasTestTag("Parent") or hasTestTag("Grandparent")))
            .assertCountEquals(2)
    }

    @Test
    fun findByAncestor_justSelf_noMatch() {
        composeTestRule.setContent {
            BoundaryNode(testTag = "Node")
        }

        find(hasAnyAncestorThat(hasTestTag("Node")))
            .assertDoesNotExist()
    }

    @Test
    fun findByAncestor_oneAncestor_noMatch() {
        composeTestRule.setContent {
            BoundaryNode(testTag = "Parent") {
                BoundaryNode(testTag = "Child")
            }
        }

        find(hasAnyAncestorThat(hasTestTag("Child")))
            .assertDoesNotExist()
    }
}