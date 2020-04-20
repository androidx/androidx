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
import androidx.ui.test.child
import androidx.ui.test.createComposeRule
import androidx.ui.test.findByTag
import androidx.ui.test.hasTestTag
import androidx.ui.test.util.BoundaryNode
import androidx.ui.test.util.expectErrorMessageStartsWith
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.junit.runners.JUnit4

@MediumTest
@RunWith(JUnit4::class)
class ChildSelectorTest {

    @get:Rule
    val composeTestRule = createComposeRule()

    @Test
    fun oneChild() {
        composeTestRule.setContent {
            BoundaryNode(testTag = "Parent") {
                BoundaryNode(testTag = "Child")
            }
        }

        findByTag("Parent")
            .child()
            .assert(hasTestTag("Child"))
    }

    @Test
    fun noChild() {
        composeTestRule.setContent {
            BoundaryNode(testTag = "Parent")
        }

        findByTag("Parent")
            .child()
            .assertDoesNotExist()
    }

    @Test(expected = AssertionError::class)
    fun noChild_fail() {
        composeTestRule.setContent {
            BoundaryNode(testTag = "Parent")
        }

        findByTag("Parent")
            .child()
            .assertExists()
    }

    @Test
    fun twoChildren_fail() {
        composeTestRule.setContent {
            BoundaryNode(testTag = "Parent") {
                BoundaryNode(testTag = "Child1")
                BoundaryNode(testTag = "Child2")
            }
        }

        expectErrorMessageStartsWith("" +
                "Failed: assertExists.\n" +
                "Reason: Expected exactly '1' node but found '2' nodes that satisfy: " +
                "((TestTag = 'Parent').child)\n" +
                "Nodes found:"
        ) {
            findByTag("Parent")
                .child()
                .assertExists()
        }
    }
}