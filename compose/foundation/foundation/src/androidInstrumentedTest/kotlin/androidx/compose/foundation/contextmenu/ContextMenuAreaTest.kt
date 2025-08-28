/*
 * Copyright 2024 The Android Open Source Project
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

package androidx.compose.foundation.contextmenu

import androidx.compose.foundation.background
import androidx.compose.foundation.contextmenu.ContextMenuState.Status
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.wrapContentSize
import androidx.compose.foundation.text.selection.assertThatOffset
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.LayoutCoordinates
import androidx.compose.ui.layout.onGloballyPositioned
import androidx.compose.ui.layout.positionOnScreen
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.test.assertHeightIsEqualTo
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.assertWidthIsEqualTo
import androidx.compose.ui.test.click
import androidx.compose.ui.test.getBoundsInRoot
import androidx.compose.ui.test.isPopup
import androidx.compose.ui.test.isRoot
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onFirst
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performMouseInput
import androidx.compose.ui.test.rightClick
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.roundToIntRect
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.filters.MediumTest
import androidx.test.platform.app.InstrumentationRegistry
import androidx.test.uiautomator.UiDevice
import com.google.common.truth.Truth.assertThat
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
@MediumTest
class ContextMenuAreaTest {
    @get:Rule val rule = createComposeRule()

    private val tag = "testTag"
    private val itemTag = "itemTag"
    private val contentTag = "contentTag"

    // region ContextMenu Tests
    @Composable
    private fun TestMenu(
        state: ContextMenuState,
        modifier: Modifier = Modifier,
        onDismiss: () -> Unit = {},
        contextMenuBuilderBlock: ContextMenuScope.() -> Unit = { testItem() },
    ) {
        ContextMenu(
            state = state,
            onDismiss = onDismiss,
            contextMenuBuilderBlock = contextMenuBuilderBlock,
            modifier = modifier.testTag(tag),
        )
    }

    @Test
    fun whenContextMenu_defaultStatus_noPopup() {
        rule.setContent { TestMenu(state = ContextMenuState()) }
        rule.onNodeWithTag(tag).assertDoesNotExist()
    }

    @Test
    fun whenContextMenu_toggleStatus_popupAppearsAndDisappears() {
        val state = ContextMenuState(Status.Closed)
        rule.setContent { TestMenu(state = state) }

        val interaction = rule.onNodeWithTag(tag)
        interaction.assertDoesNotExist()

        repeat(2) {
            state.open()
            rule.waitForIdle()
            interaction.assertIsDisplayed()

            state.close()
            rule.waitForIdle()
            interaction.assertDoesNotExist()
        }
    }

    @Test
    fun whenContextMenu_statusOpen_popupAdheresToStatusOffset() {
        val popupOffset = Offset(10f, 10f)
        val state = ContextMenuState(Status.Open(popupOffset))

        val contentTag = "content"
        val menuTag = "menu"

        lateinit var contentLayout: LayoutCoordinates
        lateinit var menuLayout: LayoutCoordinates
        rule.setContent {
            // move our actual layout from the edge of screen
            Box(modifier = Modifier.fillMaxSize().padding(32.dp)) {
                Box(
                    Modifier.size(100.dp).testTag(contentTag).onGloballyPositioned {
                        contentLayout = it
                    }
                ) {
                    TestMenu(
                        state = state,
                        modifier =
                            Modifier.testTag(menuTag).onGloballyPositioned { menuLayout = it },
                    )
                }
            }
        }

        rule.onNodeWithTag(contentTag).assertIsDisplayed()
        rule.onNodeWithTag(menuTag).assertIsDisplayed()

        assertThat(contentLayout).isNotNull()
        assertThat(menuLayout).isNotNull()

        val contentPosition = contentLayout.positionOnScreen()
        val menuPosition = menuLayout.positionOnScreen()

        assertThatOffset(menuPosition - contentPosition).equalsWithTolerance(popupOffset)
    }

    @Test
    fun whenContextMenu_clickOnItemThatClosesState_popupCloses() {
        val state = ContextMenuState(Status.Open(Offset.Zero))
        rule.setContent {
            TestMenu(
                state = state,
                contextMenuBuilderBlock = {
                    testItem(modifier = Modifier.testTag(itemTag)) { state.close() }
                },
            )
        }

        val itemInteraction = rule.onNodeWithTag(itemTag)
        assertThatContextMenuState(state).statusIsOpen()
        itemInteraction.assertIsDisplayed()

        itemInteraction.performClick()
        rule.waitForIdle()
        assertThatContextMenuState(state).statusIsClosed()
        itemInteraction.assertDoesNotExist()
    }

    @Test
    fun whenContextMenu_clickOffPopup_closesPopup() {
        val state = ContextMenuState(Status.Open(Offset.Zero))
        rule.setContent {
            Box(Modifier.fillMaxSize()) { TestMenu(state = state, onDismiss = { state.close() }) }
        }

        val interaction = rule.onNodeWithTag(tag)
        assertThatContextMenuState(state).statusIsOpen()
        interaction.assertIsDisplayed()

        // Need the click to register above Compose's test framework,
        // else it won't be directed to the popup properly. So,
        // we use a different way of dispatching the click.
        val rootRect =
            with(rule.density) { rule.onAllNodes(isRoot()).onFirst().getBoundsInRoot().toRect() }
        val offset = rootRect.roundToIntRect().bottomRight - IntOffset(1, 1)
        UiDevice.getInstance(InstrumentationRegistry.getInstrumentation()).click(offset.x, offset.y)
        rule.waitUntil { state.status == Status.Closed }
        interaction.assertDoesNotExist()
    }

    // endregion ContextMenu Tests

    // region ContextMenuArea Tests
    @Composable
    private fun TestArea(
        modifier: Modifier = Modifier,
        state: ContextMenuState = ContextMenuState(),
        onDismiss: () -> Unit = {},
        contextMenuBuilderBlock: ContextMenuScope.() -> Unit = { testItem() },
        enabled: Boolean = true,
        content: @Composable () -> Unit = {},
    ) {
        ContextMenuArea(
            state = state,
            onDismiss = onDismiss,
            contextMenuBuilderBlock = contextMenuBuilderBlock,
            enabled = enabled,
            content = content,
            modifier = modifier.testTag(tag),
        )
    }

    @Test
    fun whenContextMenuArea_toggleOpenClose_displayedAsExpected() {
        val state = ContextMenuState()
        rule.setContent {
            Box(modifier = Modifier.fillMaxSize().wrapContentSize()) {
                TestArea(
                    state = state,
                    contextMenuBuilderBlock = { testItem(modifier = Modifier.testTag(itemTag)) },
                ) {
                    Spacer(
                        modifier =
                            Modifier.background(Color.LightGray).size(100.dp).testTag(contentTag)
                    )
                }
            }
        }

        val areaInteraction = rule.onNodeWithTag(tag)
        val contentInteraction = rule.onNodeWithTag(contentTag)
        val itemInteraction = rule.onNodeWithTag(itemTag)
        val popupInteraction = rule.onNode(isPopup())

        repeat(2) {
            state.close()
            rule.waitForIdle()

            areaInteraction.assertIsDisplayed()
            contentInteraction.assertIsDisplayed()
            popupInteraction.assertDoesNotExist()
            itemInteraction.assertDoesNotExist()

            state.open()
            rule.waitForIdle()

            areaInteraction.assertIsDisplayed()
            contentInteraction.assertIsDisplayed()
            popupInteraction.assertIsDisplayed()
            itemInteraction.assertIsDisplayed()
        }
    }

    @Test
    fun whenContextMenuArea_minConstraintsPropagated() {
        val sideLength = 100.dp
        rule.setContent {
            TestArea(modifier = Modifier.size(sideLength)) {
                Box(modifier = Modifier.background(Color.LightGray).testTag(contentTag))
            }
        }

        rule.onNodeWithTag(contentTag).run {
            assertWidthIsEqualTo(sideLength)
            assertHeightIsEqualTo(sideLength)
        }
    }

    @Test
    fun whenContextMenuArea_leftClick_contextMenuDoesNotAppear() {
        val state = ContextMenuState()
        rule.setContent {
            TestArea(
                modifier = Modifier.background(Color.LightGray).size(100.dp),
                state = state,
                contextMenuBuilderBlock = { testItem(modifier = Modifier.testTag(itemTag)) },
            )
        }

        rule.onNodeWithTag(tag).performMouseInput { click() }
        rule.onNodeWithTag(itemTag).assertDoesNotExist()
    }

    @Test
    fun whenContextMenuArea_rightClick_contextMenuAppears() {
        val state = ContextMenuState()
        rule.setContent {
            TestArea(
                modifier = Modifier.background(Color.LightGray).size(100.dp),
                state = state,
                contextMenuBuilderBlock = {
                    testItem(modifier = Modifier.testTag(itemTag)) { state.close() }
                },
            )
        }

        val itemInteraction = rule.onNodeWithTag(itemTag)

        rule.onNodeWithTag(tag).performMouseInput { rightClick() }
        itemInteraction.assertIsDisplayed()

        itemInteraction.performClick()
        itemInteraction.assertDoesNotExist()
    }

    @Test
    fun whenContextMenuArea_disabled_rightClick_contextMenuDoesNotAppear() {
        val state = ContextMenuState()
        rule.setContent {
            TestArea(
                modifier = Modifier.background(Color.LightGray).size(100.dp),
                state = state,
                enabled = false,
                contextMenuBuilderBlock = {
                    testItem(modifier = Modifier.testTag(itemTag)) { state.close() }
                },
            )
        }

        rule.onNodeWithTag(tag).performMouseInput { rightClick() }
        rule.onNodeWithTag(itemTag).assertDoesNotExist()
    }
    // endregion ContextMenuArea Tests
}
