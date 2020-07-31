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

package androidx.ui.test

import androidx.compose.foundation.Box
import androidx.compose.foundation.Text
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Column
import androidx.compose.material.MaterialTheme
import androidx.compose.material.Surface
import androidx.compose.runtime.Composable
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.semantics.SemanticsActions
import androidx.test.filters.MediumTest
import androidx.ui.test.util.expectErrorMessage
import androidx.ui.test.util.expectErrorMessageStartsWith
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.junit.runners.JUnit4

@MediumTest
@RunWith(JUnit4::class)
class ErrorMessagesTest {

    @get:Rule
    val composeTestRule = createComposeRule(disableTransitions = true)

    @Test
    fun findByTag_assertHasClickAction_predicateShouldFail() {
        composeTestRule.setContent {
            ComposeSimpleCase()
        }

        expectErrorMessage("" +
                "Failed to assert the following: (OnClick is defined)\n" +
                "Semantics of the node:\n" +
                "Node #X at (X, X, X, X)px, Tag: 'MyButton'\n" +
                "Disabled = 'kotlin.Unit'\n" +
                "Text = 'Toggle'\n" +
                "GetTextLayoutResult = 'AccessibilityAction(label=null, action=Function1<" +
                "java.util.List<androidx.compose.ui.text.TextLayoutResult>, " +
                "java.lang.Boolean>)'\n" +
                "MergeDescendants = 'true'\n" +
                "Has 1 sibling\n" +
                "Selector used: (TestTag = 'MyButton')"
        ) {
            onNodeWithTag("MyButton")
                .assertHasClickAction()
        }
    }

    @Test
    fun findByTag_assertExists_butNoElementFound() {
        composeTestRule.setContent {
            ComposeSimpleCase()
        }

        expectErrorMessage("" +
                "Failed: assertExists.\n" +
                "Reason: Expected exactly '1' node but could not find any node that satisfies: " +
                "(TestTag = 'MyButton3')"
        ) {
            onNodeWithTag("MyButton3")
                .assertExists()
        }
    }

    @Test
    fun findByTag_doClick_butNoElementFound() {
        composeTestRule.setContent {
            ComposeSimpleCase()
        }

        expectErrorMessage("" +
                "Failed to perform a gesture.\n" +
                "Reason: Expected exactly '1' node but could not find any node that satisfies: " +
                "(TestTag = 'MyButton3')"
        ) {
            onNodeWithTag("MyButton3")
                .performClick()
        }
    }

    @Test
    fun findByPredicate_doClick_butNoElementFound() {
        composeTestRule.setContent {
            ComposeSimpleCase()
        }

        expectErrorMessage("" +
                "Failed to perform a gesture.\n" +
                "Reason: Expected exactly '1' node but could not find any node that satisfies: " +
                "((TestTag = 'MyButton3') && (OnClick is defined))"
        ) {
            onNode(hasTestTag("MyButton3") and hasClickAction())
                .performClick()
        }
    }

    @Test
    fun findByText_doClick_butMoreThanOneElementFound() {
        composeTestRule.setContent {
            ComposeSimpleCase()
        }

        expectErrorMessageStartsWith("" +
                "Failed to perform a gesture.\n" +
                "Reason: Expected exactly '1' node but found '2' nodes that satisfy: " +
                "(Text = 'Toggle' (ignoreCase: false))\n" +
                "Nodes found:\n" +
                "1) Node #X at (X, X, X, X)px, Tag: 'MyButton'"
        ) {
            onNodeWithText("Toggle")
                .performClick()
        }
    }

    @Test
    fun findByTag_callNonExistentSemanticsAction() {
        composeTestRule.setContent {
            ComposeSimpleCase()
        }

        expectErrorMessageStartsWith("" +
                "Failed to perform OnClick action as it is not defined on the node.\n" +
                "Semantics of the node:"
        ) {
            onNodeWithTag("MyButton")
                .performSemanticsAction(SemanticsActions.OnClick)
        }
    }

    @Test
    fun findByTag_callSemanticsAction_butElementDoesNotExist() {
        composeTestRule.setContent {
            ComposeSimpleCase()
        }

        expectErrorMessageStartsWith("" +
                "Failed to perform OnClick action.\n" +
                "Reason: Expected exactly '1' node but could not find any node that satisfies: " +
                "(TestTag = 'MyButton3')"
        ) {
            onNodeWithTag("MyButton3")
                .performSemanticsAction(SemanticsActions.OnClick)
        }
    }

    @Test
    fun findByTag_assertDoesNotExist_butElementFound() {
        composeTestRule.setContent {
            ComposeSimpleCase()
        }

        expectErrorMessageStartsWith("" +
                "Failed: assertDoesNotExist.\n" +
                "Reason: Did not expect any node but found '1' node that satisfies: " +
                "(TestTag = 'MyButton')\n" +
                "Node found:\n" +
                "Node #X at (X, X, X, X)px, Tag: 'MyButton'"
        ) {
            onNodeWithTag("MyButton")
                .assertDoesNotExist()
        }
    }

    @Test
    fun findAll_assertMultiple_butIsDifferentAmount() {
        composeTestRule.setContent {
            ComposeSimpleCase()
        }

        expectErrorMessageStartsWith("" +
                "Failed to assert count of nodes.\n" +
                "Reason: Expected '3' nodes but found '2' nodes that satisfy: " +
                "(Text = 'Toggle' (ignoreCase: false))\n" +
                "Nodes found:\n" +
                "1) Node #X at (X, X, X, X)px"
        ) {
            onAllNodesWithText("Toggle")
                .assertCountEquals(3)
        }
    }

    @Test
    fun findAll_assertMultiple_butIsZero() {
        composeTestRule.setContent {
            ComposeSimpleCase()
        }

        expectErrorMessage("" +
                "Failed to assert count of nodes.\n" +
                "Reason: Expected '3' nodes but could not find any node that satisfies: " +
                "(Text = 'Toggle2' (ignoreCase: false))"
        ) {
            onAllNodesWithText("Toggle2")
                .assertCountEquals(3)
        }
    }

    @Test
    fun findOne_hideIt_tryToClickIt_butDoesNotExist() {
        composeTestRule.setContent {
            ComposeTextToHideCase()
        }

        val node = onNodeWithText("Hello")
            .assertExists()

        onNodeWithTag("MyButton")
            .performClick()

        expectErrorMessage("" +
                "Failed to perform a gesture.\n" +
                "The node is no longer in the tree, last known semantics:\n" +
                "Node #X at (X, X, X, X)px\n" +
                "Text = 'Hello'\n" +
                "GetTextLayoutResult = 'AccessibilityAction(label=null, action=Function1<" +
                "java.util.List<androidx.compose.ui.text.TextLayoutResult>," +
                " java.lang.Boolean>)'\n" +
                "Has 1 sibling\n" +
                "Original selector: Text = 'Hello' (ignoreCase: false)"
        ) {
            node.performClick()
        }
    }

    @Test
    fun findOne_removeIt_assertExists_butDoesNotExist() {
        composeTestRule.setContent {
            ComposeTextToHideCase()
        }

        val node = onNodeWithText("Hello")
            .assertExists()

        // Hide text
        onNodeWithTag("MyButton")
            .performClick()

        expectErrorMessage("" +
                "Failed: assertExists.\n" +
                "The node is no longer in the tree, last known semantics:\n" +
                "Node #X at (X, X, X, X)px\n" +
                "Text = 'Hello'\n" +
                "GetTextLayoutResult = 'AccessibilityAction(label=null, action=Function1<" +
                "java.util.List<androidx.compose.ui.text.TextLayoutResult>," +
                " java.lang.Boolean>)'\n" +
                "Has 1 sibling\n" +
                "Original selector: Text = 'Hello' (ignoreCase: false)"
        ) {
            node.assertExists()
        }
    }

    @Test
    fun findOne_removeIt_assertHasClickAction_butDoesNotExist() {
        composeTestRule.setContent {
            ComposeTextToHideCase()
        }

        val node = onNodeWithText("Hello")
            .assertExists()

        // Hide text
        onNodeWithTag("MyButton")
            .performClick()

        expectErrorMessage("" +
                "Failed to assert the following: (OnClick is defined)\n" +
                "The node is no longer in the tree, last known semantics:\n" +
                "Node #X at (X, X, X, X)px\n" +
                "Text = 'Hello'\n" +
                "GetTextLayoutResult = 'AccessibilityAction(label=null, action=Function1<" +
                "java.util.List<androidx.compose.ui.text.TextLayoutResult>," +
                " java.lang.Boolean>)'\n" +
                "Has 1 sibling\n" +
                "Original selector: Text = 'Hello' (ignoreCase: false)"
        ) {
            node.assertHasClickAction()
        }
    }

    @Composable
    fun ComposeSimpleCase() {
        MaterialTheme {
            Column {
                TestButton(Modifier.testTag("MyButton")) {
                    Text("Toggle")
                }
                TestButton(Modifier.testTag("MyButton2")) {
                    Text("Toggle")
                }
            }
        }
    }

    @Composable
    fun ComposeTextToHideCase() {
        MaterialTheme {
            val (showText, toggle) = remember { mutableStateOf(true) }
            Column {
                TestButton(
                    modifier = Modifier.testTag("MyButton"),
                    onClick = { toggle(!showText) }
                ) {
                    Text("Toggle")
                }
                if (showText) {
                    Text("Hello")
                }
            }
        }
    }

    @Composable
    fun TestButton(
        modifier: Modifier = Modifier,
        onClick: (() -> Unit)? = null,
        children: @Composable () -> Unit
    ) {
        Surface {
            Box(modifier.clickable(onClick = onClick ?: {}, enabled = onClick != null)) {
                Box(children = children)
            }
        }
    }
}