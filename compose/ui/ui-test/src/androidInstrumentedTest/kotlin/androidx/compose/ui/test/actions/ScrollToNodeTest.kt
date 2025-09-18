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

package androidx.compose.ui.test.actions

import android.os.Looper
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.requiredSize
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyListScope
import androidx.compose.foundation.lazy.LazyListState
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.text.BasicText
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.testutils.expectError
import androidx.compose.ui.Modifier
import androidx.compose.ui.composed
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.nestedscroll.NestedScrollConnection
import androidx.compose.ui.input.nestedscroll.NestedScrollSource
import androidx.compose.ui.input.nestedscroll.nestedScroll
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.LocalLayoutDirection
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.semantics.scrollBy
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.semantics.testTag
import androidx.compose.ui.test.actions.ScrollToNodeTest.Orientation.HorizontalRtl
import androidx.compose.ui.test.actions.ScrollToNodeTest.Orientation.Vertical
import androidx.compose.ui.test.actions.ScrollToNodeTest.StartPosition.FullyAfter
import androidx.compose.ui.test.actions.ScrollToNodeTest.StartPosition.FullyBefore
import androidx.compose.ui.test.actions.ScrollToNodeTest.StartPosition.NotInList
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.getBoundsInRoot
import androidx.compose.ui.test.getUnclippedBoundsInRoot
import androidx.compose.ui.test.hasTestTag
import androidx.compose.ui.test.hasText
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.performScrollToNode
import androidx.compose.ui.test.util.ClickableTestBox
import androidx.compose.ui.test.util.ClickableTestBox.defaultTag
import androidx.compose.ui.unit.DpRect
import androidx.compose.ui.unit.LayoutDirection
import androidx.compose.ui.unit.dp
import com.google.common.truth.Truth.assertWithMessage
import java.util.concurrent.atomic.AtomicBoolean
import kotlin.test.assertTrue
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.junit.runners.Parameterized

@RunWith(Parameterized::class)
class ScrollToNodeTest(private val config: TestConfig) {
    data class TestConfig(
        val orientation: Orientation,
        val reverseLayout: Boolean,
        val viewportSize: ViewportSize,
        val targetPosition: StartPosition,
        val hasNestedScrollConsumer: Boolean,
    ) {
        val viewportSizePx: Int
            get() = viewportSize.sizePx

        private val initialScrollIndexWithoutReverseLayout: Int =
            when (viewportSize) {
                ViewportSize.SmallerThanItem -> targetPosition.indexForSmallViewport
                ViewportSize.BiggerThenItem -> targetPosition.indexForBigViewport
            }

        private val initialScrollOffsetWithoutReverseLayout: Int =
            when (viewportSize) {
                ViewportSize.SmallerThanItem -> targetPosition.offsetForSmallViewport
                ViewportSize.BiggerThenItem -> targetPosition.offsetForBigViewport
            }

        val initialScrollIndex: Int
            get() {
                val index = initialScrollIndexWithoutReverseLayout
                if (!reverseLayout) return index
                // Need to invert the index/offset pair for reverseScrolling so the target
                // is on the correct side of the viewport according to the [StartPosition]
                val offset = initialScrollOffsetWithoutReverseLayout
                val totalOffset = index * itemSizePx + offset
                val range = (2 * itemsAround + 1) * itemSizePx - viewportSizePx
                // For the index, how many items fit in the inverted totalOffset?
                return (range - totalOffset) / itemSizePx
            }

        val initialScrollOffset: Int
            get() {
                val offset = initialScrollOffsetWithoutReverseLayout
                if (!reverseLayout) return offset
                // Need to invert the index/offset pair for reverseScrolling so the target
                // is on the correct side of the viewport according to the [StartPosition]
                val index = initialScrollIndexWithoutReverseLayout
                val totalOffset = index * itemSizePx + offset
                val range = (2 * itemsAround + 1) * itemSizePx - viewportSizePx
                // For the offset, how many pixels are left in the inverted totalOffset?
                return (range - totalOffset) % itemSizePx
            }

        override fun toString(): String =
            "orientation=$orientation, " +
                "reverseScrolling=$reverseLayout, " +
                "viewport=$viewportSize, " +
                "targetIs=" +
                when (targetPosition) {
                    NotInList -> "$targetPosition, "
                    else -> "${targetPosition}Viewport, "
                } +
                "nestedScrollConsumer=$hasNestedScrollConsumer"
    }

    companion object {
        private const val containerTag = "container"
        private const val itemTag = "target"
        private const val itemsAround = 5
        private const val itemSizePx = 100
        private const val bigViewport = 150
        private const val smallViewport = 80

        @JvmStatic
        @Parameterized.Parameters(name = "{0}")
        fun params() =
            mutableListOf<TestConfig>().apply {
                for (orientation in Orientation.values()) {
                    for (reverseScrolling in listOf(false, true)) {
                        for (viewportSize in ViewportSize.values()) {
                            for (targetPosition in StartPosition.values()) {
                                for (nestedScrollConsumer in listOf(true, false)) {
                                    TestConfig(
                                            orientation = orientation,
                                            reverseLayout = reverseScrolling,
                                            viewportSize = viewportSize,
                                            targetPosition = targetPosition,
                                            hasNestedScrollConsumer = nestedScrollConsumer,
                                        )
                                        .also { add(it) }
                                }
                            }
                        }
                    }
                }
            }
    }

    @get:Rule val rule = createComposeRule()

    @Test
    fun scrollToTarget() {
        val state = LazyListState(config.initialScrollIndex, config.initialScrollOffset)
        val isRtl = config.orientation == HorizontalRtl
        val isVertical = config.orientation == Vertical

        // Some boxes in a row/col with a specific initialScrollOffset so that the target we want
        // to bring into view is either before, partially before, in, partially after or after
        // the viewport.
        rule.setContent {
            val direction = if (isRtl) LayoutDirection.Rtl else LayoutDirection.Ltr
            CompositionLocalProvider(LocalLayoutDirection provides direction) {
                if (isVertical) {
                    LazyColumn(columnModifier(), state, reverseLayout = config.reverseLayout) {
                        Boxes()
                    }
                } else {
                    LazyRow(rowModifier(), state, reverseLayout = config.reverseLayout) { Boxes() }
                }
            }
        }

        if (config.targetPosition in listOf(FullyAfter, FullyBefore, NotInList)) {
            rule.onNodeWithTag(itemTag).assertDoesNotExist()
        } else {
            rule.onNodeWithTag(itemTag).assertIsDisplayed()
        }

        // If the target is not in the list at all we should check
        // that an exception is thrown, and stop the test after that
        expectError<AssertionError>(
            expectError = config.targetPosition == NotInList,
            expectedMessage =
                "No node found that matches TestTag = 'target' in scrollable " + "container.*",
        ) {
            rule.onNodeWithTag(containerTag).performScrollToNode(hasTestTag(itemTag))
        }
        if (config.targetPosition == NotInList) {
            return
        }

        rule.onNodeWithTag(itemTag).assertIsDisplayed()

        val viewportBounds = rule.onNodeWithTag(containerTag).getBoundsInRoot().toPx()
        val targetBounds = rule.onNodeWithTag(itemTag).getUnclippedBoundsInRoot().toPx()

        if (config.viewportSize == ViewportSize.SmallerThanItem) {
            assertWithMessage("item needs to cover the whole viewport")
                .that(targetBounds.leftOrTop)
                .isAtMost(viewportBounds.leftOrTop)
            assertWithMessage("item needs to cover the whole viewport")
                .that(targetBounds.rightOrBottom)
                .isAtLeast(viewportBounds.rightOrBottom)
        } else {
            assertWithMessage("item needs to be fully inside the viewport")
                .that(targetBounds.leftOrTop)
                .isAtLeast(viewportBounds.leftOrTop)
            assertWithMessage("item needs to be fully inside the viewport")
                .that(targetBounds.rightOrBottom)
                .isAtMost(viewportBounds.rightOrBottom)
        }
    }

    @Test
    fun performScrollToNode_invokesActionOnOnUiThread() {
        val wasActionOnUIThread = AtomicBoolean(false)
        val testItems = List(100) { index -> "Item $index" }
        val targetItemText = "Item 50"
        val tag = "LazyColumnTag"

        rule.setContent {
            val lazyListState = rememberLazyListState()
            LazyColumn(
                state = lazyListState,
                modifier =
                    Modifier.height(200.dp).semantics {
                        testTag = tag
                        scrollBy { _, y ->
                            val isOnUiThread =
                                Thread.currentThread() == Looper.getMainLooper().thread
                            wasActionOnUIThread.set(isOnUiThread)
                            lazyListState.dispatchRawDelta(y)
                            true
                        }
                    },
            ) {
                items(testItems) { itemText ->
                    BasicText(text = itemText, modifier = Modifier.fillMaxWidth().height(20.dp))
                }
            }
        }

        rule.onNodeWithTag(tag).performScrollToNode(hasText(targetItemText))
        assertTrue(
            wasActionOnUIThread.get(),
            "The scroll action, triggered by performScrollToNode, did not occur on the UI thread.",
        )
    }

    private val Rect.leftOrTop: Float
        get() = if (config.orientation == Vertical) top else left

    private val Rect.rightOrBottom: Float
        get() = if (config.orientation == Vertical) right else bottom

    private fun DpRect.toPx(): Rect = with(rule.density) { toRect() }

    private fun rowModifier(): Modifier =
        Modifier.composed {
            with(LocalDensity.current) {
                Modifier.testTag(containerTag)
                    .then(
                        if (config.hasNestedScrollConsumer)
                            Modifier.nestedScroll(horizontalNestedScrollConsumer)
                        else Modifier
                    )
                    .requiredSize(config.viewportSizePx.toDp(), itemSizePx.toDp())
            }
        }

    private fun columnModifier(): Modifier =
        Modifier.composed {
            with(LocalDensity.current) {
                Modifier.testTag(containerTag)
                    .then(
                        if (config.hasNestedScrollConsumer)
                            Modifier.nestedScroll(verticalNestedScrollConsumer)
                        else Modifier
                    )
                    .requiredSize(itemSizePx.toDp(), config.viewportSizePx.toDp())
            }
        }

    private fun LazyListScope.Boxes() {
        items(itemsAround) { ClickableTestBox(color = if (it % 2 == 0) Color.Blue else Color.Red) }
        item {
            ClickableTestBox(
                color = Color.Yellow,
                // Don't add the tag if the test says there is no target in the list
                tag = if (config.targetPosition != NotInList) itemTag else defaultTag,
            )
        }
        items(itemsAround) {
            ClickableTestBox(color = if (it % 2 == 0) Color.Green else Color.Cyan)
        }
    }

    enum class Orientation {
        HorizontalLtr,
        HorizontalRtl,
        Vertical,
    }

    enum class ViewportSize(val sizePx: Int) {
        SmallerThanItem(smallViewport),
        BiggerThenItem(bigViewport),
    }

    enum class StartPosition(
        val indexForSmallViewport: Int,
        val offsetForSmallViewport: Int,
        val indexForBigViewport: Int,
        val offsetForBigViewport: Int,
    ) {
        FullyAfter(0, 0, 0, 0),
        PartiallyAfter(itemsAround - 1, 50, itemsAround - 1, 0),
        CenterAlignedIn(itemsAround, 10, itemsAround - 1, 75),
        PartiallyBefore(itemsAround, 70, itemsAround, 50),
        FullyBefore(2 * itemsAround, 20, 2 * itemsAround - 1, 50),
        NotInList(0, 0, 0, 0),
    }

    private val verticalNestedScrollConsumer =
        object : NestedScrollConnection {
            override fun onPreScroll(available: Offset, source: NestedScrollSource): Offset {
                return Offset(0f, available.y / 2f)
            }
        }

    private val horizontalNestedScrollConsumer =
        object : NestedScrollConnection {
            override fun onPreScroll(available: Offset, source: NestedScrollSource): Offset {
                return Offset(available.x / 2f, 0f)
            }
        }
}
