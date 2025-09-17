/*
 * Copyright 2025 The Android Open Source Project
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

package androidx.compose.ui.focus

import androidx.compose.foundation.focusable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.size
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.Row
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.test.assertIsFocused
import androidx.compose.ui.test.junit4.ComposeContentTestRule
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.requestFocus
import androidx.compose.ui.unit.dp
import androidx.test.filters.MediumTest
import androidx.test.platform.app.InstrumentationRegistry
import kotlin.test.Test
import org.junit.After
import org.junit.Before
import org.junit.Rule
import org.junit.runner.RunWith
import org.junit.runners.Parameterized

@MediumTest
@RunWith(Parameterized::class)
class WrapAroundFocusTest(private val touchMode: Boolean, private val shouldWrapAround: Boolean) {
    @get:Rule val rule = createComposeRule()

    private lateinit var focusOwner: FocusOwner

    @Before
    fun setTouchMode() {
        InstrumentationRegistry.getInstrumentation().setInTouchModeCompat(touchMode)
    }

    @After
    fun resetTouchMode() = InstrumentationRegistry.getInstrumentation().resetInTouchModeCompat()

    @Test
    fun noFocusableItem_next() {
        // Arrange.
        rule.setTestContent { Box(Modifier.testTag("item").size(10.dp)) }

        // Act.
        rule.runOnIdle { focusOwner.moveFocus(FocusDirection.Next, shouldWrapAround) }

        // Implicit Assert: The test passes if no exception is thrown.
    }

    @Test
    fun noFocusableItem_previous() {
        // Arrange.
        rule.setTestContent { Box(Modifier.testTag("item").size(10.dp)) }

        // Act.
        rule.runOnIdle { focusOwner.moveFocus(FocusDirection.Previous, shouldWrapAround) }

        // Implicit Assert: The test passes if no exception is thrown.
    }

    @Test
    fun singleItem_next() {
        // Arrange.
        rule.setTestContent { Box(Modifier.testTag("item").size(10.dp).focusable()) }
        rule.onNodeWithTag("item").requestFocus()

        // Act.
        rule.runOnIdle { focusOwner.moveFocus(FocusDirection.Next, shouldWrapAround) }

        // Assert.
        rule.onNodeWithTag("item").assertIsFocused()
    }

    @Test
    fun singleItem_previous() {
        // Arrange.
        rule.setTestContent { Box(Modifier.testTag("item").size(10.dp).focusable()) }
        rule.onNodeWithTag("item").requestFocus()

        // Act.
        rule.runOnIdle { focusOwner.moveFocus(FocusDirection.Previous, shouldWrapAround) }

        // Assert.
        rule.onNodeWithTag("item").assertIsFocused()
    }

    @Test
    fun singleRow_next() {
        // Arrange.
        rule.setTestContent {
            Row {
                Box(Modifier.testTag("item1").size(10.dp).focusable())
                Box(Modifier.testTag("item2").size(10.dp).focusable())
            }
        }
        rule.onNodeWithTag("item2").requestFocus()

        // Act.
        rule.runOnIdle { focusOwner.moveFocus(FocusDirection.Next, shouldWrapAround) }

        // Assert.
        rule.onNodeWithTag(if (shouldWrapAround) "item1" else "item2").assertIsFocused()
    }

    @Test
    fun singleRow_previous() {
        // Arrange.
        rule.setTestContent {
            Row {
                Box(Modifier.testTag("item1").size(10.dp).focusable())
                Box(Modifier.testTag("item2").size(10.dp).focusable())
            }
        }
        rule.onNodeWithTag("item1").requestFocus()

        // Act.
        rule.runOnIdle { focusOwner.moveFocus(FocusDirection.Previous, shouldWrapAround) }

        // Assert.
        rule.onNodeWithTag(if (shouldWrapAround) "item2" else "item1").assertIsFocused()
    }

    @Test
    fun twoRows_next_movesToSecondRow() {
        // Arrange.
        rule.setTestContent {
            Column {
                Row {
                    Box(Modifier.testTag("item1").size(10.dp).focusable())
                    Box(Modifier.testTag("item2").size(10.dp).focusable())
                }
                Row {
                    Box(Modifier.testTag("item3").size(10.dp).focusable())
                    Box(Modifier.testTag("item4").size(10.dp).focusable())
                }
            }
        }
        rule.onNodeWithTag("item2").requestFocus()

        // Act.
        rule.runOnIdle { focusOwner.moveFocus(FocusDirection.Next, shouldWrapAround) }

        // Assert - Focus should move to the next row regardless of the wrapAround setting.
        rule.onNodeWithTag("item3").assertIsFocused()
    }

    @Test
    fun twoRows_previous_movesToFirstRow() {
        // Arrange.
        rule.setTestContent {
            Column {
                Row {
                    Box(Modifier.testTag("item1").size(10.dp).focusable())
                    Box(Modifier.testTag("item2").size(10.dp).focusable())
                }
                Row {
                    Box(Modifier.testTag("item3").size(10.dp).focusable())
                    Box(Modifier.testTag("item4").size(10.dp).focusable())
                }
            }
        }
        rule.onNodeWithTag("item3").requestFocus()

        // Act.
        rule.runOnIdle { focusOwner.moveFocus(FocusDirection.Previous, shouldWrapAround) }

        // Assert - Focus should move to the previous row regardless of the wrapAround setting.
        rule.onNodeWithTag("item2").assertIsFocused()
    }

    @Test
    fun twoRows_next_wrapsAround() {
        // Arrange.
        rule.setTestContent {
            Column {
                Row {
                    Box(Modifier.testTag("item1").size(10.dp).focusable())
                    Box(Modifier.testTag("item2").size(10.dp).focusable())
                }
                Row {
                    Box(Modifier.testTag("item3").size(10.dp).focusable())
                    Box(Modifier.testTag("item4").size(10.dp).focusable())
                }
            }
        }
        rule.onNodeWithTag("item4").requestFocus()

        // Act.
        rule.runOnIdle { focusOwner.moveFocus(FocusDirection.Next, shouldWrapAround) }

        // Assert.
        rule.onNodeWithTag(if (shouldWrapAround) "item1" else "item4").assertIsFocused()
    }

    @Test
    fun twoRows_previous_wrapsAround() {
        // Arrange.
        rule.setTestContent {
            Column {
                Row {
                    Box(Modifier.testTag("item1").size(10.dp).focusable())
                    Box(Modifier.testTag("item2").size(10.dp).focusable())
                }
                Row {
                    Box(Modifier.testTag("item3").size(10.dp).focusable())
                    Box(Modifier.testTag("item4").size(10.dp).focusable())
                }
            }
        }
        rule.onNodeWithTag("item1").requestFocus()

        // Act.
        rule.runOnIdle { focusOwner.moveFocus(FocusDirection.Previous, shouldWrapAround) }

        // Assert.
        rule.onNodeWithTag(if (shouldWrapAround) "item4" else "item1").assertIsFocused()
    }

    companion object {
        @JvmStatic
        @Parameterized.Parameters(name = "touchMode = {0}, shouldWrapAround = {1}")
        fun initParameters() =
            listOf(
                arrayOf(false, false),
                arrayOf(false, true),
                arrayOf(true, false),
                arrayOf(true, true),
            )
    }

    private fun ComposeContentTestRule.setTestContent(content: @Composable () -> Unit) {
        setContent {
            focusOwner = LocalFocusManager.current as FocusOwner
            content()
        }
    }
}
