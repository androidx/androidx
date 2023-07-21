/*
 * Copyright 2023 The Android Open Source Project
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

package androidx.tv.material3

import androidx.compose.foundation.border
import androidx.compose.foundation.focusable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.text.BasicText
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.remember
import androidx.compose.ui.ExperimentalComposeUiApi
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.key.Key
import androidx.compose.ui.platform.LocalLayoutDirection
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.semantics.SemanticsNode
import androidx.compose.ui.test.ExperimentalTestApi
import androidx.compose.ui.test.SemanticsNodeInteraction
import androidx.compose.ui.test.SemanticsNodeInteractionCollection
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.assertIsEqualTo
import androidx.compose.ui.test.assertIsFocused
import androidx.compose.ui.test.assertIsNotFocused
import androidx.compose.ui.test.assertWidthIsEqualTo
import androidx.compose.ui.test.getUnclippedBoundsInRoot
import androidx.compose.ui.test.junit4.StateRestorationTester
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onAllNodesWithText
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.onRoot
import androidx.compose.ui.test.performKeyInput
import androidx.compose.ui.test.pressKey
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.DpRect
import androidx.compose.ui.unit.LayoutDirection
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.toSize
import androidx.test.platform.app.InstrumentationRegistry
import org.junit.Rule
import org.junit.Test

@OptIn(ExperimentalTvMaterial3Api::class)
class NavigationDrawerTest {
    @get:Rule
    val rule = createComposeRule()

    @Test
    fun navigationDrawer_initialStateClosed_closedStateComposableDisplayed() {
        rule.setContent {
            NavigationDrawer(
                drawerState = remember { DrawerState(DrawerValue.Closed) },
                drawerContent = {
                    BasicText(text = if (it == DrawerValue.Open) "Opened" else "Closed")
                }
            ) { Box(Modifier.size(200.dp)) }
        }

        rule.onAllNodesWithText("Closed").assertAnyAreDisplayed()
    }

    @Test
    fun navigationDrawer_initialStateOpen_openStateComposableDisplayed() {
        rule.setContent {
            NavigationDrawer(
                drawerState = remember { DrawerState(DrawerValue.Open) },
                drawerContent = {
                    BasicText(text = if (it == DrawerValue.Open) "Opened" else "Closed")
                }) { BasicText("other content") }
        }

        rule.onAllNodesWithText("Opened").assertAnyAreDisplayed()
    }

    @Test
    fun navigationDrawer_focusInsideDrawer_openedStateComposableDisplayed() {
        InstrumentationRegistry.getInstrumentation().setInTouchMode(false)
        val drawerFocusRequester = FocusRequester()
        rule.setContent {
            val navigationDrawerValue = remember { DrawerState(DrawerValue.Closed) }
            NavigationDrawer(
                modifier = Modifier.focusRequester(drawerFocusRequester),
                drawerState = navigationDrawerValue,
                drawerContent = {
                    BasicText(
                        modifier = Modifier.focusable(),
                        text = if (it == DrawerValue.Open) "Opened" else "Closed"
                    )
                }) { BasicText("other content") }
        }

        rule.onAllNodesWithText("Closed").assertAnyAreDisplayed()

        rule.runOnIdle {
            drawerFocusRequester.requestFocus()
        }

        rule.onAllNodesWithText("Opened").assertAnyAreDisplayed()
    }

    @OptIn(ExperimentalTestApi::class, ExperimentalComposeUiApi::class)
    @Test
    fun navigationDrawer_focusMovesOutOfDrawer_closedStateComposableDisplayed() {
        InstrumentationRegistry.getInstrumentation().setInTouchMode(false)
        val drawerFocusRequester = FocusRequester()
        rule.setContent {
            val navigationDrawerValue = remember { DrawerState(DrawerValue.Closed) }
            Row {
                NavigationDrawer(
                    modifier = Modifier.focusRequester(drawerFocusRequester),
                    drawerState = navigationDrawerValue,
                    drawerContent = {
                        BasicText(
                            text = if (it == DrawerValue.Open) "Opened" else "Closed",
                            modifier = Modifier.focusable()
                        )
                    }) {
                    Box(modifier = Modifier.focusable()) {
                        BasicText("Button")
                    }
                }
            }
        }
        rule.runOnIdle {
            drawerFocusRequester.requestFocus()
        }
        rule.onAllNodesWithText("Opened").assertAnyAreDisplayed()
        rule.onRoot().performKeyInput { pressKey(Key.DirectionRight) }
        rule.onAllNodesWithText("Closed").assertAnyAreDisplayed()
    }

    @OptIn(ExperimentalComposeUiApi::class, ExperimentalTestApi::class)
    @Test
    fun navigationDrawer_focusMovesIntoDrawer_openStateComposableDisplayed() {
        InstrumentationRegistry.getInstrumentation().setInTouchMode(false)
        val buttonFocusRequester = FocusRequester()
        rule.setContent {
            val navigationDrawerValue = remember { DrawerState(DrawerValue.Closed) }
            Row {
                NavigationDrawer(
                    drawerState = navigationDrawerValue,
                    drawerContent = {
                        BasicText(
                            text = if (it == DrawerValue.Open) "Opened" else "Closed",
                            modifier = Modifier
                                .focusable()
                                .testTag("drawerItem")
                        )
                    }) {
                    Box(
                        modifier = Modifier
                            .focusRequester(buttonFocusRequester)
                            .focusable()
                    ) {
                        BasicText("Button")
                    }
                }
            }
        }
        rule.runOnIdle {
            buttonFocusRequester.requestFocus()
        }
        rule.onAllNodesWithText("Closed").assertAnyAreDisplayed()
        rule.onRoot().performKeyInput { pressKey(Key.DirectionLeft) }
        rule.onAllNodesWithText("Opened").assertAnyAreDisplayed()
        rule.onNodeWithTag("drawerItem").assertIsFocused()
    }

    @Test
    fun navigationDrawer_closedState_widthOfDrawerIsWidthOfContent() {
        val contentWidthBoxTag = "contentWidthBox"
        val totalWidth = 100.dp
        val closedDrawerContentWidth = 30.dp
        val expectedContentWidth = totalWidth - closedDrawerContentWidth
        rule.setContent {
            Box(modifier = Modifier.width(totalWidth)) {
                NavigationDrawer(
                    drawerState = remember { DrawerState(DrawerValue.Closed) },
                    drawerContent = {
                        Box(Modifier.width(closedDrawerContentWidth)) {
                            // extra long content wrapped in a drawer-width restricting box
                            Box(Modifier.width(closedDrawerContentWidth * 10))
                        }
                    }
                ) { Box(Modifier.fillMaxWidth().testTag(contentWidthBoxTag)) }
            }
        }

        rule.onNodeWithTag(contentWidthBoxTag).assertWidthIsEqualTo(expectedContentWidth)
    }

    @Test
    fun navigationDrawer_openState_widthOfDrawerIsWidthOfContent() {
        val contentWidthBoxTag = "contentWidthBox"
        val totalWidth = 100.dp
        val openDrawerContentWidth = 70.dp
        val expectedContentWidth = totalWidth - openDrawerContentWidth
        rule.setContent {
            Box(modifier = Modifier.width(totalWidth)) {
                NavigationDrawer(
                    drawerState = remember { DrawerState(DrawerValue.Closed) },
                    drawerContent = {
                        Box(Modifier.width(openDrawerContentWidth)) {
                            Box(Modifier.width(openDrawerContentWidth * 10))
                        }
                    }
                ) { Box(Modifier.fillMaxWidth().testTag(contentWidthBoxTag)) }
            }
        }

        rule.onNodeWithTag(contentWidthBoxTag).assertWidthIsEqualTo(expectedContentWidth)
    }

    @Test
    fun navigationDrawer_rtl_drawerIsDrawnAtTheStart() {
        val contentWidthBoxTag = "contentWidthBox"
        val drawerContentBoxTag = "drawerContentBox"
        rule.setContent {
            CompositionLocalProvider(LocalLayoutDirection provides LayoutDirection.Rtl) {
                NavigationDrawer(
                    drawerState = remember { DrawerState(DrawerValue.Closed) },
                    drawerContent = {
                        Box(Modifier.testTag(drawerContentBoxTag).border(2.dp, Color.Red)) {
                            BasicText(text = if (it == DrawerValue.Open) "Opened" else "Closed")
                        }
                    }
                ) { Box(Modifier.fillMaxWidth().testTag(contentWidthBoxTag)) }
            }
        }

        val rightEdgeOfRoot = rule.onRoot().getUnclippedBoundsInRoot().right
        rule.onNodeWithTag(drawerContentBoxTag).assertRightPositionInRootIsEqualTo(rightEdgeOfRoot)
    }

    @Test
    fun navigationDrawer_rtl_drawerExpandsTowardsEnd() {
        val contentWidthBoxTag = "contentWidthBox"
        val drawerContentBoxTag = "drawerContentBox"
        var drawerState: DrawerState? = null
        rule.setContent {
            CompositionLocalProvider(LocalLayoutDirection provides LayoutDirection.Rtl) {
                drawerState = remember { DrawerState(DrawerValue.Closed) }
                NavigationDrawer(
                    drawerState = drawerState!!,
                    drawerContent = {
                        Box(Modifier.testTag(drawerContentBoxTag).border(2.dp, Color.Red)) {
                            BasicText(text = if (it == DrawerValue.Open) "Opened" else "Closed")
                        }
                    }
                ) { Box(Modifier.fillMaxWidth().testTag(contentWidthBoxTag)) }
            }
        }

        val endPositionInClosedState =
            rule.onNodeWithTag(drawerContentBoxTag).getUnclippedBoundsInRoot().left

        rule.runOnIdle { drawerState?.setValue(DrawerValue.Open) }
        val endPositionInOpenState =
            rule.onNodeWithTag(drawerContentBoxTag).getUnclippedBoundsInRoot().left

        assert(endPositionInClosedState.value > endPositionInOpenState.value)
    }

    @OptIn(ExperimentalTestApi::class, ExperimentalComposeUiApi::class)
    @Test
    fun navigationDrawer_parentContainerGainsFocus_onBackPress() {
        val drawerFocusRequester = FocusRequester()
        rule.setContent {
            Box(
                modifier = Modifier
                    .testTag("box-container")
                    .fillMaxSize()
                    .focusable()
            ) {
                NavigationDrawer(
                    modifier = Modifier.focusRequester(drawerFocusRequester),
                    drawerState = remember { DrawerState(DrawerValue.Closed) },
                    drawerContent = {
                        BasicText(
                            text = if (it == DrawerValue.Open) "Opened" else "Closed",
                            modifier = Modifier.focusable()
                        )
                    }
                ) {
                    BasicText("other content")
                }
            }
        }

        rule.onAllNodesWithText("Closed").assertAnyAreDisplayed()

        rule.runOnIdle {
            drawerFocusRequester.requestFocus()
        }

        rule.onAllNodesWithText("Opened").assertAnyAreDisplayed()
        rule.onNodeWithTag("box-container").assertIsNotFocused()

        // Trigger back press
        rule.onRoot().performKeyInput { pressKey(Key.Back) }
        rule.waitForIdle()

        // Check if the parent container gains focus
        rule.onNodeWithTag("box-container").assertIsFocused()
    }

    @Test
    fun navigationDrawerState_restoreState_remembersRecordedState() {
        // Arrange
        InstrumentationRegistry.getInstrumentation().setInTouchMode(false)
        val stateRestorationTester = StateRestorationTester(rule)
        val drawerFocusRequester = FocusRequester()
        stateRestorationTester.setContent {
            val navigationDrawerValue = rememberDrawerState(initialValue = DrawerValue.Closed)
            NavigationDrawer(
                modifier = Modifier.focusRequester(drawerFocusRequester),
                drawerState = navigationDrawerValue,
                drawerContent = {
                    BasicText(
                        modifier = Modifier.focusable(),
                        text = if (it == DrawerValue.Open) "Opened" else "Closed"
                    )
                }) { BasicText("other content") }
        }

        rule.onAllNodesWithText("Closed").assertAnyAreDisplayed()

        // Act
        rule.runOnIdle {
            drawerFocusRequester.requestFocus()
        }

        rule.onAllNodesWithText("Opened").assertAnyAreDisplayed()

        stateRestorationTester.emulateSavedInstanceStateRestore()
        // Assert
        rule.onAllNodesWithText("Opened").assertAnyAreDisplayed()
    }

    private fun SemanticsNodeInteractionCollection.assertAnyAreDisplayed() {
        val result = (0 until fetchSemanticsNodes().size).map { get(it) }.any {
            try {
                it.assertIsDisplayed()
                true
            } catch (e: AssertionError) {
                false
            }
        }

        if (!result) throw AssertionError("Assert failed: None of the components are displayed!")
    }

    private fun SemanticsNodeInteraction.assertRightPositionInRootIsEqualTo(
        expectedRight: Dp
    ): SemanticsNodeInteraction {
        return withUnclippedBoundsInRoot {
            it.right.assertIsEqualTo(expectedRight, "right")
        }
    }

    private fun SemanticsNodeInteraction.withUnclippedBoundsInRoot(
        assertion: (DpRect) -> Unit
    ): SemanticsNodeInteraction {
        val node = fetchSemanticsNode("Failed to retrieve bounds of the node.")
        val bounds = with(node.layoutInfo.density) {
            node.unclippedBoundsInRoot.let {
                DpRect(it.left.toDp(), it.top.toDp(), it.right.toDp(), it.bottom.toDp())
            }
        }
        assertion.invoke(bounds)
        return this
    }

    private val SemanticsNode.unclippedBoundsInRoot: Rect
        get() {
            return if (layoutInfo.isPlaced) {
                Rect(positionInRoot, size.toSize())
            } else {
                Dp.Unspecified.value.let { Rect(it, it, it, it) }
            }
        }
}
