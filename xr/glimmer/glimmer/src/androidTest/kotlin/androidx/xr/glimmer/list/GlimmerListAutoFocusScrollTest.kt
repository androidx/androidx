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

import androidx.compose.foundation.gestures.Orientation
import androidx.compose.foundation.layout.requiredSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.FocusDirection
import androidx.compose.ui.focus.FocusManager
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.test.assert
import androidx.compose.ui.test.isFocused
import androidx.compose.ui.test.junit4.ComposeContentTestRule
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.printToString
import androidx.compose.ui.unit.Density
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.test.filters.MediumTest
import org.junit.Test
import org.junit.runner.RunWith
import org.junit.runners.Parameterized

@MediumTest
@RunWith(Parameterized::class)
internal class GlimmerListAutoFocusScrollTest(private val testCase: FocusStrategyScrollTestCase) :
    BaseListTestWithOrientation(Orientation.Vertical) {

    @Test
    fun verifyFocusIsSetCorrectlyAtEachScrollStep() {
        val state = ListState()
        val density = Density(1f)
        rule.setContentForTestCase {
            CompositionLocalProvider(LocalDensity provides density) {
                FocusableTestList(state, testCase)
            }
        }

        runTestCase(state, density, testCase)
    }

    private fun runTestCase(
        state: ListState,
        density: Density,
        testCase: FocusStrategyScrollTestCase,
    ) {
        val itemSizePx = with(density) { testCase.itemSize.toPx() }

        // Set the initial scroll offset in half of item size.
        // To make sure that the focus line in the middle of the first item.
        state.scrollByAndWaitForIdle(itemSizePx / 2)

        var expectedFocusIndex = 0
        do {
            // Checks if expected items is focused.
            val expectedFocusTag = "item-$expectedFocusIndex"
            rule.onNodeWithTag(expectedFocusTag).assert(isFocused()) {
                val debugListTree = rule.onNodeWithTag(LIST_TEST_TAG).printToString()
                val totalScroll = itemSizePx / 2 + (expectedFocusIndex + 1) * itemSizePx
                buildString {
                    append("When the user scrolls by $totalScroll px, ")
                    appendLine("item with tag [$expectedFocusTag] should gain focus.")
                    appendLine("Test case: $testCase.")
                    appendLine("Debug list tree: $debugListTree")
                }
            }

            // Scroll the list by an item's height - the focus line should land exactly in the
            // middle of the next item.
            state.scrollByAndWaitForIdle(itemSizePx)
            ++expectedFocusIndex
        } while (expectedFocusIndex < testCase.itemsCount)
    }

    private fun ComposeContentTestRule.setContentForTestCase(content: @Composable () -> Unit) {
        lateinit var focusManager: FocusManager
        setContent {
            focusManager = LocalFocusManager.current
            content()
        }
        // Move focus to the list.
        rule.runOnIdle { focusManager.moveFocus(FocusDirection.Next) }
        rule.waitForIdle()
    }

    @Composable
    private fun FocusableTestList(state: ListState, testCase: FocusStrategyScrollTestCase) {
        TestList(
            state = state,
            itemsCount = testCase.itemsCount,
            modifier = Modifier.requiredSize(200.dp, testCase.listSize),
        ) { index ->
            FocusableItem(index, Modifier.requiredSize(200.dp, testCase.itemSize))
        }
    }

    internal data class FocusStrategyScrollTestCase(
        val itemsCount: Int,
        val itemSize: Dp,
        val listSize: Dp,
    )

    companion object {
        @JvmStatic
        @Parameterized.Parameters(name = "{index}-{0}")
        internal fun params(): Array<FocusStrategyScrollTestCase> =
            arrayOf(
                // Large list, content length > 2 * list height.
                FocusStrategyScrollTestCase(itemsCount = 15, itemSize = 100.dp, listSize = 500.dp),
                // Large list, content length > 2 * list height.
                FocusStrategyScrollTestCase(itemsCount = 20, itemSize = 50.dp, listSize = 300.dp),
                // Large list, content length > 2 * list height.
                FocusStrategyScrollTestCase(itemsCount = 11, itemSize = 60.dp, listSize = 300.dp),
                // Short list, content length == 2 * list height.
                FocusStrategyScrollTestCase(itemsCount = 10, itemSize = 50.dp, listSize = 250.dp),
                // Short list, content length == 2 * list height.
                FocusStrategyScrollTestCase(itemsCount = 10, itemSize = 100.dp, listSize = 500.dp),
                // Short list, content length < 2 * list height.
                FocusStrategyScrollTestCase(itemsCount = 10, itemSize = 30.dp, listSize = 250.dp),
                // Short list, content length < 2 * list height.
                FocusStrategyScrollTestCase(itemsCount = 10, itemSize = 30.dp, listSize = 250.dp),
                // Short list, content length == 2 * list height.
                FocusStrategyScrollTestCase(itemsCount = 6, itemSize = 60.dp, listSize = 300.dp),
            )
    }
}
