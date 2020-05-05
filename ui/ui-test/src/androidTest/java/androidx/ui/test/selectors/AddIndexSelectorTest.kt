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

package androidx.ui.test.selectors

import androidx.test.filters.MediumTest
import androidx.ui.test.assert
import androidx.ui.test.childAt
import androidx.ui.test.children
import androidx.ui.test.createComposeRule
import androidx.ui.test.findByTag
import androidx.ui.test.first
import androidx.ui.test.hasTestTag
import androidx.ui.test.util.BoundaryNode
import androidx.ui.test.util.expectErrorMessageStartsWith
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.junit.runners.JUnit4

@MediumTest
@RunWith(JUnit4::class)
class AddIndexSelectorTest {

    @get:Rule
    val composeTestRule = createComposeRule()

    @Test
    fun getFirst() {
        composeTestRule.setContent {
            BoundaryNode(testTag = "Parent") {
                BoundaryNode(testTag = "Child1")
                BoundaryNode(testTag = "Child2")
            }
        }

        findByTag("Parent")
            .children()
            .first()
            .assert(hasTestTag("Child1"))
    }

    @Test
    fun getAtIndex() {
        composeTestRule.setContent {
            BoundaryNode(testTag = "Parent") {
                BoundaryNode(testTag = "Child1")
                BoundaryNode(testTag = "Child2")
            }
        }

        findByTag("Parent")
            .childAt(1)
            .assert(hasTestTag("Child2"))
    }

    @Test
    fun getAtIndex_wrongIndex_fail() {
        composeTestRule.setContent {
            BoundaryNode(testTag = "Parent") {
                BoundaryNode(testTag = "Child1")
                BoundaryNode(testTag = "Child2")
            }
        }

        expectErrorMessageStartsWith("" +
                "Failed: assertExists.\n" +
                "Can't retrieve node at index '2' of '(TestTag = 'Parent').children'\n" +
                "There are '2' nodes only:") {
            findByTag("Parent")
                .childAt(2)
                .assertExists()
        }
    }

    @Test
    fun getAtIndex_noItems() {
        composeTestRule.setContent {
            BoundaryNode(testTag = "Parent")
        }

        findByTag("Parent")
            .childAt(2)
            .assertDoesNotExist()
    }

    @Test
    fun getAtIndex_noItems_fail() {
        composeTestRule.setContent {
            BoundaryNode(testTag = "Parent")
        }

        expectErrorMessageStartsWith("" +
                "Failed: assertExists.\n" +
                "Can't retrieve node at index '2' of '(TestTag = 'Parent').children'\n" +
                "There are no existing nodes for that selector.") {
            findByTag("Parent")
                .childAt(2)
                .assertExists()
        }
    }
}