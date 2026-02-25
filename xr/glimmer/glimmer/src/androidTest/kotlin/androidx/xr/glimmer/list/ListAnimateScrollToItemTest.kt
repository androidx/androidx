/*
 * Copyright 2026 The Android Open Source Project
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
import androidx.compose.foundation.gestures.FlingBehavior
import androidx.compose.foundation.gestures.Orientation
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsFocusedAsState
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.requiredSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.test.SemanticsNodeInteraction
import androidx.compose.ui.test.SemanticsNodeInteractionsProvider
import androidx.compose.ui.test.assertIsFocused
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.xr.glimmer.Text
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.launch
import org.junit.Test
import org.junit.runner.RunWith
import org.junit.runners.Parameterized

@RunWith(Parameterized::class)
class ListAnimateScrollToItemTest(private val testCase: ScrollMovementTestCase) :
    BaseListTestWithOrientation(testCase.orientation) {

    data class ScrollMovementTestCase(
        val startIndex: Int,
        val targetIndex: Int,
        val orientation: Orientation,
    ) {
        override fun toString(): String = "Start:$startIndex -> End:$targetIndex | $orientation"
    }

    companion object {
        @JvmStatic
        @Parameterized.Parameters(name = "{0}")
        fun data(): Collection<ScrollMovementTestCase> {
            val scenarios =
                listOf(
                    // Top
                    0 to 1,
                    0 to 2,
                    2 to 4,
                    0 to 98,

                    // Middle
                    50 to 48,
                    50 to 52,
                    50 to 97,
                    50 to 99,
                    50 to 45,
                    50 to 0,
                    50 to 1,

                    // Bottom
                    98 to 97,
                    97 to 96,
                    99 to 97,
                    99 to 50,
                    99 to 0,
                    99 to 1,

                    // No Movement
                    0 to 0,
                    1 to 1,
                    10 to 10,
                    99 to 99,
                )

            return scenarios.flatMap { (start, end) ->
                listOf(Orientation.Vertical, Orientation.Horizontal).map { orientation ->
                    ScrollMovementTestCase(
                        startIndex = start,
                        targetIndex = end,
                        orientation = orientation,
                    )
                }
            }
        }
    }

    @Test
    fun animateScrollToItem_fromStartToTargetIndex() {
        lateinit var scope: CoroutineScope
        val state = ListState()

        rule.setContent {
            scope = rememberCoroutineScope()
            FocusableTestList(state = state, itemsCount = 100)
        }

        rule.runOnIdle { scope.launch { state.animateScrollToItem(testCase.startIndex) } }

        rule.waitForIdle()

        rule.onListItem(testCase.startIndex).assertIsFocused()

        rule.runOnIdle { scope.launch { state.animateScrollToItem(testCase.targetIndex) } }

        rule.waitForIdle()

        rule.onListItem(testCase.targetIndex).assertIsFocused()
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
        itemsCount: Int = 100,
        userScrollEnabled: Boolean = true,
        listOrientation: Orientation = orientation,
        state: ListState = rememberListState(),
        flingBehavior: FlingBehavior = VerticalListDefaults.flingBehavior(state),
        itemContent: @Composable (Int) -> Unit = { FocusableListItem(it) },
    ) {
        TestList(
            state = state,
            itemsCount = itemsCount,
            listOrientation = listOrientation,
            flingBehavior = flingBehavior,
            userScrollEnabled = userScrollEnabled,
            modifier = Modifier.requiredSize(ItemWidth * 3, ItemHeight * ItemsPerScreen),
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

    private val ListState.focusLinePosition: Float
        get() {
            val focusScroll = requireNotNull(autoFocusState.properties).focusScroll.toFloat()
            val startPadding = layoutInfo.beforeContentPadding
            return startPadding + focusScroll
        }

    private val focusLinePositionTolerance: Float
        get() = with(rule.density) { 1.dp.toPx() }

    private val SemanticsNodeInteraction.boundsCenterInRoot: Float
        get() {
            val node = fetchSemanticsNode()
            return if (orientation == Orientation.Vertical) {
                node.positionInRoot.y + node.size.height / 2f
            } else {
                node.positionInRoot.x + node.size.width / 2f
            }
        }
}

private val ItemWidth: Dp = 100.dp
private val ItemHeight: Dp = 100.dp
private const val ItemsPerScreen: Int = 5
