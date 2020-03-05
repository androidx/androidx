/*
 * Copyright 2019 The Android Open Source Project
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

package androidx.ui.material

import androidx.compose.Composable
import androidx.compose.state
import androidx.test.filters.MediumTest
import androidx.ui.core.LayoutCoordinates
import androidx.ui.core.OnChildPositioned
import androidx.ui.core.OnPositioned
import androidx.ui.core.TestTag
import androidx.ui.core.Text
import androidx.ui.core.currentTextStyle
import androidx.ui.layout.Center
import androidx.ui.layout.Column
import androidx.ui.layout.Stack
import androidx.ui.test.assertHasClickAction
import androidx.ui.test.assertHasNoClickAction
import androidx.ui.test.assertSemanticsIsEqualTo
import androidx.ui.test.createComposeRule
import androidx.ui.test.createFullSemantics
import androidx.ui.test.doClick
import androidx.ui.test.findByTag
import androidx.ui.test.findByText
import androidx.ui.text.TextStyle
import androidx.ui.unit.Dp
import androidx.ui.unit.PxPosition
import androidx.ui.unit.PxSize
import androidx.ui.unit.dp
import androidx.ui.unit.sp
import androidx.ui.unit.toPx
import com.google.common.truth.Truth.assertThat
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.junit.runners.JUnit4

@MediumTest
@RunWith(JUnit4::class)
class ButtonTest {

    @get:Rule
    val composeTestRule = createComposeRule()

    private val defaultButtonSemantics = createFullSemantics(
        isEnabled = true
    )

    @Test
    fun buttonTest_defaultSemantics() {
        composeTestRule.setMaterialContent {
            Center {
                TestTag(tag = "myButton") {
                    Button(onClick = {}) {
                        Text("myButton")
                    }
                }
            }
        }

        findByTag("myButton")
            .assertSemanticsIsEqualTo(defaultButtonSemantics)
    }

    @Test
    fun buttonTest_disabledSemantics() {
        composeTestRule.setMaterialContent {
            Center {
                TestTag(tag = "myButton") {
                    Button { Text("myButton") }
                }
            }
        }

        findByTag("myButton")
            .assertSemanticsIsEqualTo(
                createFullSemantics(
                    isEnabled = false
                )
            )
    }

    @Test
    fun buttonTest_findByTextAndClick() {
        var counter = 0
        val onClick: () -> Unit = { ++counter }
        val text = "myButton"

        composeTestRule.setMaterialContent {
            Center {
                Button(onClick = onClick) {
                    Text(text)
                }
            }
        }

        // TODO(b/129400818): this actually finds the text, not the button as
        // merge semantics aren't implemented yet
        findByText(text)
            .doClick()

        composeTestRule.runOnIdleCompose {
            assertThat(counter).isEqualTo(1)
        }
    }

    @Test
    fun buttonTest_canBeDisabled() {
        val tag = "myButton"

        composeTestRule.setMaterialContent {
            val enabled = state { true }
            val onClick: (() -> Unit)? = if (enabled.value) {
                { enabled.value = false }
            } else {
                null
            }
            Center {
                TestTag(tag = tag) {
                    Button(onClick = onClick) {
                        Text("Hello")
                    }
                }
            }
        }
        findByTag(tag)
            // Confirm the button starts off enabled, with a click action
            .assertHasClickAction()
            .assertSemanticsIsEqualTo(
                createFullSemantics(
                    isEnabled = true
                )
            )
            .doClick()
            // Then confirm it's disabled with no click action after clicking it
            .assertHasNoClickAction()
            .assertSemanticsIsEqualTo(
                createFullSemantics(
                    isEnabled = false
                )
            )
    }

    @Test
    fun buttonTest_ClickIsIndependentBetweenButtons() {
        var button1Counter = 0
        val button1OnClick: () -> Unit = { ++button1Counter }
        val button1Tag = "button1"

        var button2Counter = 0
        val button2OnClick: () -> Unit = { ++button2Counter }
        val button2Tag = "button2"

        val text = "myButton"

        composeTestRule.setMaterialContent {
            Column {
                TestTag(tag = button1Tag) {
                    Button(onClick = button1OnClick) {
                        Text(text)
                    }
                }
                TestTag(tag = button2Tag) {
                    Button(onClick = button2OnClick) {
                        Text(text)
                    }
                }
            }
        }

        findByTag(button1Tag)
            .doClick()

        composeTestRule.runOnIdleCompose {
            assertThat(button1Counter).isEqualTo(1)
            assertThat(button2Counter).isEqualTo(0)
        }

        findByTag(button2Tag)
            .doClick()

        composeTestRule.runOnIdleCompose {
            assertThat(button1Counter).isEqualTo(1)
            assertThat(button2Counter).isEqualTo(1)
        }
    }

    @Test
    fun buttonTest_ButtonHeightIsFromSpec() {
        if (composeTestRule.density.fontScale > 1f) {
            // This test can be reasonable failing on the non default font scales
            // so lets skip it.
            return
        }
        composeTestRule
            .setMaterialContentAndCollectSizes {
                Button(onClick = {}) {
                    Text("Test button")
                }
            }
            .assertHeightEqualsTo(36.dp)
    }

    @Test
    fun buttonTest_ButtonWithLargeFontSizeIsLargerThenMinHeight() {
        val realSize: PxSize = composeTestRule.setMaterialContentAndGetPixelSize {
            Button(onClick = {}) {
                Text(
                    text = "Test button",
                    style = TextStyle(fontSize = 50.sp)
                )
            }
        }

        with(composeTestRule.density) {
            assertThat(realSize.height.value)
                .isGreaterThan(36.dp.toIntPx().value.toFloat())
        }
    }

    @Test
    fun buttonTest_ContainedButtonPropagateDefaultTextStyle() {
        composeTestRule.setMaterialContent {
            Button(onClick = {}) {
                val style = MaterialTheme.typography().button
                    .copy(color = MaterialTheme.colors().onPrimary)
                assertThat(currentTextStyle()).isEqualTo(style)
            }
        }
    }

    @Test
    fun buttonTest_OutlinedButtonPropagateDefaultTextStyle() {
        composeTestRule.setMaterialContent {
            OutlinedButton(onClick = {}) {
                val style = MaterialTheme.typography().button
                    .copy(color = MaterialTheme.colors().primary)
                assertThat(currentTextStyle()).isEqualTo(style)
            }
        }
    }

    @Test
    fun buttonTest_TextButtonPropagateDefaultTextStyle() {
        composeTestRule.setMaterialContent {
            TextButton(onClick = {}) {
                val style = MaterialTheme.typography().button
                    .copy(color = MaterialTheme.colors().primary)
                assertThat(currentTextStyle()).isEqualTo(style)
            }
        }
    }

    @Test
    fun buttonTest_ContainedButtonHorPaddingIsFromSpec() {
        assertLeftPaddingIs(16.dp) { children ->
            Button(onClick = {}, children = children)
        }
    }

    @Test
    fun buttonTest_OutlinedButtonHorPaddingIsFromSpec() {
        assertLeftPaddingIs(16.dp) { children ->
            OutlinedButton(onClick = {}, children = children)
        }
    }

    @Test
    fun buttonTest_TextButtonHorPaddingIsFromSpec() {
        assertLeftPaddingIs(8.dp) { children ->
            TextButton(onClick = {}, children = children)
        }
    }

    private fun assertLeftPaddingIs(
        padding: Dp,
        button: @Composable() (@Composable() () -> Unit) -> Unit
    ) {
        var parentCoordinates: LayoutCoordinates? = null
        var childCoordinates: LayoutCoordinates? = null
        composeTestRule.setMaterialContent {
            Stack {
                button {
                    OnPositioned {
                        parentCoordinates = it
                    }
                    OnChildPositioned(onPositioned = {
                        childCoordinates = it
                    }) {
                        Text("Test button")
                    }
                }
            }
        }

        composeTestRule.runOnIdleCompose {
            val topLeft = childCoordinates!!.localToGlobal(PxPosition.Origin).x -
                    parentCoordinates!!.localToGlobal(PxPosition.Origin).x
            val currentPadding = with(composeTestRule.density) {
                padding.toIntPx().toPx()
            }
            assertThat(currentPadding).isEqualTo(topLeft)
        }
    }
}
