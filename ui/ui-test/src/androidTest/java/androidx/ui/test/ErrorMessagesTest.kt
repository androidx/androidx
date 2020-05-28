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

import androidx.compose.Composable
import androidx.compose.state
import androidx.test.filters.MediumTest
import androidx.ui.core.Modifier
import androidx.ui.core.semantics.semantics
import androidx.ui.core.testTag
import androidx.ui.foundation.Box
import androidx.ui.foundation.Text
import androidx.ui.foundation.clickable
import androidx.ui.layout.Column
import androidx.ui.material.MaterialTheme
import androidx.ui.material.Surface
import androidx.ui.semantics.SemanticsActions
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
                "Id: X, Position: LTRB(X.px, X.px, X.px, X.px)\n" +
                "- TestTag = 'MyButton'\n" +
                "- Enabled = 'false'\n" +
                "- AccessibilityLabel = 'Toggle'\n" +
                "- MergeDescendants = 'true'\n" +
                "Selector used: (TestTag = 'MyButton')"
        ) {
            findByTag("MyButton")
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
            findByTag("MyButton3")
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
            findByTag("MyButton3")
                .doClick()
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
            find(hasTestTag("MyButton3") and hasClickAction())
                .doClick()
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
                "(AccessibilityLabel = 'Toggle' (ignoreCase: false))\n" +
                "Nodes found:\n" +
                "1) Id: X, Position: LTRB(X.px, X.px, X.px, X.px)\n" +
                "- TestTag = 'MyButton'"
        ) {
            findByText("Toggle")
                .doClick()
        }
    }

    @Test
    fun findByTag_callNonExistentSemanticsAction() {
        composeTestRule.setContent {
            ComposeSimpleCase()
        }

        expectErrorMessageStartsWith("" +
                "Failed to call OnClick action as it is not defined on the node.\n" +
                "Semantics of the node:"
        ) {
            findByTag("MyButton")
                .callSemanticsAction(SemanticsActions.OnClick)
        }
    }

    @Test
    fun findByTag_callSemanticsAction_butElementDoesNotExist() {
        composeTestRule.setContent {
            ComposeSimpleCase()
        }

        expectErrorMessageStartsWith("" +
                "Failed to call OnClick action.\n" +
                "Reason: Expected exactly '1' node but could not find any node that satisfies: " +
                "(TestTag = 'MyButton3')"
        ) {
            findByTag("MyButton3")
                .callSemanticsAction(SemanticsActions.OnClick)
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
                "Id: X, Position: LTRB(X.px, X.px, X.px, X.px)\n" +
                "- TestTag = 'MyButton'\n"
        ) {
            findByTag("MyButton")
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
                "(AccessibilityLabel = 'Toggle' (ignoreCase: false))\n" +
                "Nodes found:\n" +
                "1) Id: X, Position: LTRB(X.px, X.px, X.px, X.px)"
        ) {
            findAllByText("Toggle")
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
                "(AccessibilityLabel = 'Toggle2' (ignoreCase: false))"
        ) {
            findAllByText("Toggle2")
                .assertCountEquals(3)
        }
    }

    @Test
    fun findOne_hideIt_tryToClickIt_butDoesNotExist() {
        composeTestRule.setContent {
            ComposeTextToHideCase()
        }

        val node = findByText("Hello")
            .assertExists()

        findByTag("MyButton")
            .doClick()

        expectErrorMessage("" +
                "Failed to perform a gesture.\n" +
                "The node is no longer in the tree, last known semantics:\n" +
                "Id: X, Position: LTRB(X.px, X.px, X.px, X.px)\n" +
                "- AccessibilityLabel = 'Hello'\n" +
                "Original selector: AccessibilityLabel = 'Hello' (ignoreCase: false)"
        ) {
            node.doClick()
        }
    }

    @Test
    fun findOne_removeIt_assertExists_butDoesNotExist() {
        composeTestRule.setContent {
            ComposeTextToHideCase()
        }

        val node = findByText("Hello")
            .assertExists()

        // Hide text
        findByTag("MyButton")
            .doClick()

        expectErrorMessage("" +
                "Failed: assertExists.\n" +
                "The node is no longer in the tree, last known semantics:\n" +
                "Id: X, Position: LTRB(X.px, X.px, X.px, X.px)\n" +
                "- AccessibilityLabel = 'Hello'\n" +
                "Original selector: AccessibilityLabel = 'Hello' (ignoreCase: false)") {
            node.assertExists()
        }
    }

    @Test
    fun findOne_removeIt_assertHasClickAction_butDoesNotExist() {
        composeTestRule.setContent {
            ComposeTextToHideCase()
        }

        val node = findByText("Hello")
            .assertExists()

        // Hide text
        findByTag("MyButton")
            .doClick()

        expectErrorMessage("" +
                "Failed to assert the following: (OnClick is defined)\n" +
                "The node is no longer in the tree, last known semantics:\n" +
                "Id: X, Position: LTRB(X.px, X.px, X.px, X.px)\n" +
                "- AccessibilityLabel = 'Hello'\n" +
                "Original selector: AccessibilityLabel = 'Hello' (ignoreCase: false)"
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
            val (showText, toggle) = state { true }
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
        // Since we're adding layouts in between the clickable layer and the content, we need to
        // merge all descendants, or we'll get multiple nodes
        Surface {
            Box(modifier.semantics(mergeAllDescendants = true)
                        .clickable(onClick = onClick ?: {}, enabled = onClick != null)) {
                Box(children = children)
            }
        }
    }
}