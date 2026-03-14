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

package androidx.xr.glimmer.list

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.focusable
import androidx.compose.foundation.gestures.Orientation
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsFocusedAsState
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.requiredSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.FocusDirection
import androidx.compose.ui.focus.FocusManager
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.indirect.IndirectPointerEvent
import androidx.compose.ui.input.indirect.IndirectPointerEventType
import androidx.compose.ui.input.indirect.IndirectPointerInputModifierNode
import androidx.compose.ui.input.pointer.PointerEventPass
import androidx.compose.ui.node.ModifierNodeElement
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.semantics.SemanticsActions.ScrollBy
import androidx.compose.ui.test.SemanticsNodeInteraction
import androidx.compose.ui.test.SemanticsNodeInteractionsProvider
import androidx.compose.ui.test.assertIsFocused
import androidx.compose.ui.test.assertIsNotFocused
import androidx.compose.ui.test.junit4.ComposeContentTestRule
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.onRoot
import androidx.compose.ui.test.performScrollToIndex
import androidx.compose.ui.test.performSemanticsAction
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.util.fastAny
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.filters.MediumTest
import androidx.xr.glimmer.Text
import androidx.xr.glimmer.performIndirectSwipe
import com.google.common.truth.Truth
import org.junit.Test
import org.junit.runner.RunWith

@MediumTest
@RunWith(AndroidJUnit4::class)
class GlimmerListAutoFocusTest : BaseListTestWithOrientation(Orientation.Vertical) {

    private lateinit var focusManager: FocusManager

    @Test
    fun firstItem_is_initiallyFocused() {
        rule.setAutoFocusContent { FocusableTestList() }

        rule.onListItem(0).assertIsFocused()
    }

    @Test
    fun performScrollToIndex_movesAutoFocus() {
        rule.setAutoFocusContent { FocusableTestList(itemsCount = 100) }

        rule.onNodeWithTag(LIST_TEST_TAG).performScrollToIndex(25)

        // TODO: b/433687753 - performScrollToIndex() isn't aligned with the auto-focused item.
        // We brought item-25 to the top, but centered item-27 is focused.
        rule.onListItem(27).assertIsFocused()
    }

    @Test
    fun scrollBy_movesAutoFocus_whenUserScrollIsDisabled() {
        val state = ListState()
        rule.setAutoFocusContent { FocusableTestList(userScrollEnabled = false, state = state) }

        // Default size of items is 100.dp
        state.scrollByAndWaitForIdle(250.dp)

        // The third item must be focused.
        rule.onListItem(2).assertIsFocused()
    }

    @Test
    fun performSemanticsAction_scrollBy_movesAutoFocus() {
        rule.setAutoFocusContent { FocusableTestList(itemsCount = 100) }

        val scroll = with(rule.density) { ItemHeight.toPx() * 5.5f }
        rule.onNodeWithTag(LIST_TEST_TAG).performSemanticsAction(ScrollBy) { it.invoke(0f, scroll) }
        rule.waitForIdle()

        rule.onListItem(5).assertIsFocused()
    }

    @Test
    fun indirectPointer_movesAutoFocus() {
        rule.setAutoFocusContent { FocusableTestList(itemsCount = 100) }

        val swipe = with(rule.density) { ItemHeight.toPx() * 5.5f }
        rule.onNodeWithTag(LIST_TEST_TAG).performIndirectSwipe(rule, swipe)
        rule.waitForIdle()

        rule.onListItem(5).assertIsFocused()
    }

    @Test
    fun nonScrollableList_doesNotConsumed_indirectPointerEvents() {
        var downEventReceivedByParentWasConsumed = false
        var moveEventReceivedByParentWasConsumed = false
        var upEventReceivedByParentWasConsumed = false
        rule.setAutoFocusContent {
            Box(
                Modifier.elementFor(
                    IndirectPointerInputNode(
                        onEvent = {
                            indirectPointerEvent: IndirectPointerEvent,
                            pointerEventPass: PointerEventPass ->
                            if (pointerEventPass == PointerEventPass.Main) {
                                val indirectConsumed =
                                    indirectPointerEvent.changes.fastAny { it.isConsumed }

                                when (indirectPointerEvent.type) {
                                    IndirectPointerEventType.Press -> {
                                        downEventReceivedByParentWasConsumed = indirectConsumed
                                    }
                                    IndirectPointerEventType.Move -> {
                                        moveEventReceivedByParentWasConsumed = indirectConsumed
                                    }
                                    IndirectPointerEventType.Release -> {
                                        // Check 'Release' since 'Press' is always propagated.
                                        upEventReceivedByParentWasConsumed = indirectConsumed
                                    }
                                }
                            }
                        }
                    )
                )
            ) {
                FocusableTestList(itemsCount = 3)
            }
        }

        rule.onRoot().performIndirectSwipe(rule, 1500f)

        Truth.assertThat(downEventReceivedByParentWasConsumed).isFalse()
        Truth.assertThat(moveEventReceivedByParentWasConsumed).isFalse()
        Truth.assertThat(upEventReceivedByParentWasConsumed).isFalse()
    }

    @Test
    fun scrollableList_afterTurningIntoNonScrollable_stopsConsumingEvents() {
        var downEventReceivedByParentWasConsumed = false
        var moveEventReceivedByParentWasConsumed = false
        var upEventReceivedByParentWasConsumed = false

        val itemsCount = mutableIntStateOf(10)
        rule.setAutoFocusContent {
            Box(
                Modifier.elementFor(
                    IndirectPointerInputNode(
                        onEvent = {
                            indirectPointerEvent: IndirectPointerEvent,
                            pointerEventPass: PointerEventPass ->
                            if (pointerEventPass == PointerEventPass.Main) {
                                val indirectConsumed =
                                    indirectPointerEvent.changes.fastAny { it.isConsumed }

                                when (indirectPointerEvent.type) {
                                    IndirectPointerEventType.Press -> {
                                        downEventReceivedByParentWasConsumed = indirectConsumed
                                    }
                                    IndirectPointerEventType.Move -> {
                                        moveEventReceivedByParentWasConsumed = indirectConsumed
                                    }
                                    IndirectPointerEventType.Release -> {
                                        // Check 'Release' since 'Press' is always propagated.
                                        upEventReceivedByParentWasConsumed = indirectConsumed
                                    }
                                }
                            }
                        }
                    )
                )
            ) {
                FocusableTestList(itemsCount = itemsCount.intValue)
            }
        }

        // List is scrollable, so it will consume the move events
        rule.onRoot().performIndirectSwipe(rule, 200f)
        Truth.assertThat(downEventReceivedByParentWasConsumed).isFalse()
        Truth.assertThat(moveEventReceivedByParentWasConsumed).isTrue()
        Truth.assertThat(upEventReceivedByParentWasConsumed).isFalse()

        // Reduce amount of items in the list.
        itemsCount.intValue = 3

        downEventReceivedByParentWasConsumed = false
        moveEventReceivedByParentWasConsumed = false
        upEventReceivedByParentWasConsumed = false

        // List is non-scrollable now, so events must be propagated further.
        rule.onRoot().performIndirectSwipe(rule, 200f)
        Truth.assertThat(downEventReceivedByParentWasConsumed).isFalse()
        Truth.assertThat(moveEventReceivedByParentWasConsumed).isFalse()
        Truth.assertThat(upEventReceivedByParentWasConsumed).isFalse()
    }

    /**
     * It's hard to detect a bug when the focus line goes beyond last item. The reason is that the
     * focus remains on the last focused item if the focus line is no longer aligned with any
     * visible item (we don't clear the focus if the focus line is above an empty space). So even if
     * there's an overscroll beyond the expected range, it’s not visually obvious — the focus
     * appears correct because it's still on the last item.
     *
     * To reliably reproduce the bug, we need a really fast "swing" — jumping from somewhere in the
     * middle to beyond the end of the list — which reveals that focus didn’t actually move to the
     * correct item. It's easier to do that for short list with very fast "swing" like in this test.
     */
    @Test
    fun lastItem_is_focused_after_fastScrollToBottom() {
        rule.setAutoFocusContent { FocusableTestList(itemsCount = 6) }
        val largeScroll = with(rule.density) { 10000.dp.toPx() }

        rule.onNodeWithTag(LIST_TEST_TAG).performSemanticsAction(ScrollBy) {
            it.invoke(0f, largeScroll)
        }
        rule.waitForIdle()

        rule.onListItem(5).assertIsFocused()
    }

    @Test
    fun mixture_of_focusable_and_nonFocusable_items() {
        rule.setAutoFocusContent {
            FocusableTestList { index ->
                val focusable = (index == 0) || (index == 3)
                FocusableListItem(index = index, focusable = focusable)
            }
        }

        // Center of the item-0 (focusable)
        scrollListBy(ItemHeight / 2)
        rule.onListItem(0).assertIsFocused()

        // Center of the item-1 (non-focusable)
        scrollListBy(ItemHeight)
        rule.onListItem(0).assertIsFocused()

        // Center of the item-2 (non-focusable)
        scrollListBy(ItemHeight)
        rule.onListItem(0).assertIsFocused()

        // Center of the item-3 (focusable)
        scrollListBy(ItemHeight)
        rule.onListItem(3).assertIsFocused()
    }

    @Test
    fun moveFocus_from_thePreviousFocusableElement_to_theList() {
        rule.setAutoFocusContent {
            FocusableItem(text = "Button", modifier = Modifier.testTag("button"))
            FocusableTestList(itemsCount = 1)
        }

        // Check initial focus.
        rule.onNodeWithTag("button").assertIsFocused()
        rule.onListItem(0).assertIsNotFocused()

        // Move focus into the list.
        moveFocusForward()
        rule.onNodeWithTag("button").assertIsNotFocused()
        rule.onListItem(0).assertIsFocused()
    }

    @Test
    fun moveFocus_from_theListWithSingleElement_to_theNextFocusableElement() {
        rule.setAutoFocusContent {
            FocusableTestList(itemsCount = 1)
            FocusableItem(text = "Button", modifier = Modifier.testTag("button"))
        }

        // Check initial focus.
        rule.onListItem(0).assertIsFocused()
        rule.onNodeWithTag("button").assertIsNotFocused()

        // Move focus out of the list.
        moveFocusForward()
        rule.onListItem(0).assertIsNotFocused()
        rule.onNodeWithTag("button").assertIsFocused()
    }

    @Test
    fun moveFocus_from_theLongList_to_theNextFocusableElement() {
        rule.setAutoFocusContent {
            FocusableTestList(itemsCount = 10)
            FocusableItem(text = "Button", modifier = Modifier.testTag("button"))
        }

        // Check initial focus.
        rule.onListItem(0).assertIsFocused()
        rule.onNodeWithTag("button").assertIsNotFocused()

        // Move focus to the last item in the list.
        val scroll = with(rule.density) { (ItemHeight * 10).toPx() }
        rule.onNodeWithTag(LIST_TEST_TAG).performSemanticsAction(ScrollBy) { it.invoke(0f, scroll) }
        rule.waitForIdle()
        rule.onListItem(9).assertIsFocused()

        // Move focus out of the list.
        moveFocusForward()
        rule.onListItem(9).assertIsNotFocused()
        rule.onNodeWithTag("button").assertIsFocused()
    }

    @Test
    fun list_doesNotStealFocus_whenAdded() {
        val addList = mutableStateOf(false)
        rule.setAutoFocusContent {
            if (addList.value) {
                FocusableTestList(itemsCount = 1)
            }
            FocusableItem(text = "Button", modifier = Modifier.testTag("button"))
        }

        // Check the button is focused.
        rule.onNodeWithTag("button").assertIsFocused()

        // Bring the list to the screen.
        addList.value = true
        rule.waitForIdle()

        // Check that focus remains on the button.
        rule.onListItem(0).assertIsNotFocused()
        rule.onNodeWithTag("button").assertIsFocused()
    }

    @Test
    fun listWithLargePadding_focusesFirstListItemAutomatically() {
        rule.setAutoFocusContent {
            FocusableTestList(
                modifier = Modifier.padding(start = ItemWidth * 2, top = ItemHeight * 2)
            )
        }
        rule.onListItem(0).assertIsFocused()
    }

    @Test
    fun listWithLargeContentPadding_focusesFirstListItemAutomatically() {
        rule.setAutoFocusContent {
            FocusableTestList(
                contentPadding = PaddingValues(start = ItemWidth * 2, top = ItemHeight * 2)
            )
        }
        rule.onListItem(0).assertIsFocused()
    }

    private fun scrollListBy(scroll: Dp) {
        val pixels = with(rule.density) { scroll.toPx() }
        rule.onNodeWithTag(LIST_TEST_TAG).performSemanticsAction(ScrollBy) { it.invoke(0f, pixels) }
        rule.waitForIdle()
    }

    private fun moveFocusForward() {
        rule.runOnIdle { focusManager.moveFocus(FocusDirection.Next) }
        rule.waitForIdle()
    }

    private fun ComposeContentTestRule.setAutoFocusContent(content: @Composable () -> Unit) {
        setContent {
            focusManager = LocalFocusManager.current
            content()
        }
    }

    /**
     *      __________________
     *     |  _____________   | 0
     *     | |    item-0   |  |
     *     | |_____________|  |
     *     |  _____________   | 100
     *     | |    item-1   |  |
     *     | |_____________|  |
     *     |  _____________   | 200
     *     | |    item-2   |  |
     *     | |_____________|  |
     *     |  _____________   | 300
     *     | |    item-3   |  |
     *     | |_____________|  |
     *     |  _____________   | 400
     *     | |    item-4   |  |
     *     | |_____________|  |
     *     |__________________| 500
     *
     * The list can display up to 5 fully visible items at a time.
     */
    @Composable
    fun FocusableTestList(
        modifier: Modifier = Modifier,
        itemsCount: Int = 100,
        userScrollEnabled: Boolean = true,
        listOrientation: Orientation = orientation,
        state: ListState = rememberListState(),
        contentPadding: PaddingValues = PaddingValues(),
        itemContent: @Composable (Int) -> Unit = { FocusableListItem(it) },
    ) {
        TestList(
            state = state,
            itemsCount = itemsCount,
            listOrientation = listOrientation,
            userScrollEnabled = userScrollEnabled,
            contentPadding = contentPadding,
            modifier = modifier.requiredSize(ItemWidth * 3, ItemHeight * ItemsPerScreen),
        ) { index ->
            itemContent(index)
        }
    }

    @Composable
    private fun FocusableListItem(index: Int, focusable: Boolean = true) {
        FocusableItem(
            text = index.toString(),
            modifier = Modifier.testTag("item-$index"),
            focusable = focusable,
        )
    }

    @Composable
    private fun FocusableItem(
        text: String,
        modifier: Modifier = Modifier,
        focusable: Boolean = true,
    ) {
        val interactionSource = remember { MutableInteractionSource() }
        val isFocused = interactionSource.collectIsFocusedAsState().value
        Box(
            contentAlignment = Alignment.Center,
            modifier =
                modifier
                    .requiredSize(ItemWidth, ItemHeight)
                    .background(color = if (isFocused) Color.Red else Color.Green)
                    .border(1.dp, Color.Black)
                    .focusable(focusable, interactionSource),
        ) {
            Text(text = text, fontSize = 30.sp, color = Color.Black, fontWeight = FontWeight.Bold)
        }
    }

    fun SemanticsNodeInteractionsProvider.onListItem(index: Int): SemanticsNodeInteraction {
        return onNodeWithTag("item-$index")
    }
}

internal fun Modifier.elementFor(node: Modifier.Node): Modifier {
    return this then NodeElement(node)
}

internal data class NodeElement(val node: Modifier.Node) : ModifierNodeElement<Modifier.Node>() {
    override fun create(): Modifier.Node = node

    override fun update(node: Modifier.Node) {}
}

internal class IndirectPointerInputNode(
    var onEvent: (IndirectPointerEvent, PointerEventPass) -> Unit
) : IndirectPointerInputModifierNode, Modifier.Node() {
    override fun onIndirectPointerEvent(event: IndirectPointerEvent, pass: PointerEventPass) {
        onEvent(event, pass)
    }

    override fun onCancelIndirectPointerInput() {}
}

private val ItemWidth: Dp = 100.dp

private val ItemHeight: Dp = 100.dp

private const val ItemsPerScreen: Int = 5
