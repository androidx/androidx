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

import androidx.compose.foundation.OverscrollEffect
import androidx.compose.foundation.background
import androidx.compose.foundation.gestures.Orientation
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberOverscrollEffect
import androidx.compose.ui.ExperimentalComposeUiApi
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.drawscope.ContentDrawScope
import androidx.compose.ui.input.nestedscroll.NestedScrollSource
import androidx.compose.ui.layout.LocalPinnableContainer
import androidx.compose.ui.node.DelegatableNode
import androidx.compose.ui.node.DrawModifierNode
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.assertIsNotDisplayed
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performScrollToIndex
import androidx.compose.ui.test.performScrollToKey
import androidx.compose.ui.unit.Velocity
import androidx.compose.ui.unit.dp
import androidx.test.filters.MediumTest
import androidx.xr.glimmer.Text
import androidx.xr.glimmer.performIndirectSwipe
import androidx.xr.glimmer.setGlimmerThemeContent
import com.google.common.truth.Truth.assertThat
import org.junit.Test
import org.junit.runner.RunWith
import org.junit.runners.Parameterized

@MediumTest
@RunWith(Parameterized::class)
@OptIn(ExperimentalComposeUiApi::class)
class ListInteractionTest(orientation: Orientation) : BaseListTestWithOrientation(orientation) {

    @Test
    fun performScrollToIndex() {
        rule.setGlimmerThemeContent { TestList { Text("Item ($it)") } }

        // Make sure the far items are not laid out.
        val targetIndex = Int.MAX_VALUE / 2
        rule.onNodeWithText("Item ($targetIndex)").assertDoesNotExist()

        // Scroll to the far index and check that the item become visible.
        rule.onNodeWithTag(LIST_TEST_TAG).performScrollToIndex(targetIndex)
        rule.onNodeWithText("Item ($targetIndex)").assertIsDisplayed()
    }

    @Test
    fun performScrollToKey() {
        val targetIndex = 1000

        rule.setGlimmerThemeContent {
            TestList(keyProvider = { it }, itemContent = { index -> Text("Item ($index)") })
        }

        // Scroll to the target key and check that the item become visible.
        rule.onNodeWithTag(LIST_TEST_TAG).performScrollToKey(targetIndex)
        rule.onNodeWithText("Item ($targetIndex)").assertIsDisplayed()
    }

    @Test
    fun pinnedItem_remainsLaidOut() {
        rule.setGlimmerThemeContent {
            TestList { index ->
                Text("Item ($index)")
                if (index == 3) {
                    // Pin the item #3.
                    val pinnableContainer = requireNotNull(LocalPinnableContainer.current)
                    pinnableContainer.pin()
                }
            }
        }

        // Scroll until the first items are no longer visible.
        rule.onNodeWithTag(LIST_TEST_TAG).performScrollToIndex(5_000)

        // The non-pinned item should not be laid out.
        rule.onNodeWithText("Item (4)").assertIsNotDisplayed().assertDoesNotExist()
        // The pinned item must be laid out but not visible.
        rule.onNodeWithText("Item (3)").assertIsNotDisplayed().assertExists()
    }

    @Test
    fun itemsCountZero_displaysNoItems() {
        val itemTag = "BoxItem"
        rule.setGlimmerThemeContent {
            TestList(
                modifier = Modifier.size(100.dp).background(Color.Black),
                itemsCount = 0,
                itemContent = { _ ->
                    Box(Modifier.size(20.dp).background(Color.Red).testTag(itemTag))
                },
            )
        }

        rule.onNodeWithTag(itemTag).assertIsNotDisplayed()
    }

    @Test
    fun itemsExtensionOverload_forList() {
        val itemsList = listOf("A", "B", "C", "D", "E")
        rule.setGlimmerThemeContent { VerticalList { items(itemsList) { item -> Text(item) } } }

        for (item in itemsList) {
            rule.onNodeWithText(item).assertIsDisplayed()
        }
    }

    @Test
    fun itemsExtension_forIndexedList() {
        val itemsList = listOf("A", "B", "C", "D", "E")
        rule.setGlimmerThemeContent {
            VerticalList {
                itemsIndexed(itemsList) { index, item ->
                    assertThat(index).isEqualTo(itemsList.indexOf(item))
                    Text(item)
                }
            }
        }

        for (item in itemsList) {
            rule.onNodeWithText(item).assertIsDisplayed()
        }
    }

    @Test
    fun defaultOverscroll_isAttached() {
        var overscroll: OverscrollEffect? = null
        rule.setGlimmerThemeContent {
            overscroll = rememberOverscrollEffect()
            TestList(overscrollEffect = overscroll) { FocusableItem(it) }
        }

        rule.runOnIdle { assertThat(overscroll?.node?.node?.isAttached).isTrue() }
    }

    @Test
    fun customOverscroll_isApplied() {
        val overscroll = TestOverscrollEffect()
        rule.setGlimmerThemeContent {
            TestList(
                modifier = Modifier.size(200.dp),
                itemContent = { index -> FocusableItem(index, Modifier.size(50.dp)) },
                overscrollEffect = overscroll,
            )
        }

        // The overscroll modifier should be added / drawn
        rule.runOnIdle { assertThat(overscroll.drawCalled).isTrue() }

        rule
            .onNodeWithTag(LIST_TEST_TAG)
            .performIndirectSwipe(rule = rule, distance = 2000f, moveDuration = 10L)

        rule.runOnIdle {
            // The swipe will result in multiple scroll deltas
            assertThat(overscroll.applyToScrollCalledCount).isGreaterThan(1)
            assertThat(overscroll.applyToFlingCalledCount).isEqualTo(1)
        }
    }

    private class TestOverscrollEffect : OverscrollEffect {
        var applyToScrollCalledCount: Int = 0
            private set

        var applyToFlingCalledCount: Int = 0
            private set

        var drawCalled: Boolean = false

        override fun applyToScroll(
            delta: Offset,
            source: NestedScrollSource,
            performScroll: (Offset) -> Offset,
        ): Offset {
            applyToScrollCalledCount++
            return performScroll(delta)
        }

        override suspend fun applyToFling(
            velocity: Velocity,
            performFling: suspend (Velocity) -> Velocity,
        ) {
            applyToFlingCalledCount++
            performFling(velocity)
        }

        override val isInProgress: Boolean = false
        override val node: DelegatableNode =
            object : Modifier.Node(), DrawModifierNode {
                override fun ContentDrawScope.draw() {
                    drawContent()
                    drawCalled = true
                }
            }
    }

    companion object {
        @JvmStatic
        @Parameterized.Parameters(name = "{0}")
        fun params() = arrayOf(Orientation.Vertical, Orientation.Horizontal)
    }
}
