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

package androidx.compose.foundation.lazy

import androidx.compose.getValue
import androidx.compose.mutableStateOf
import androidx.compose.onCommit
import androidx.compose.onDispose
import androidx.compose.setValue
import androidx.test.filters.LargeTest
import androidx.ui.core.Modifier
import androidx.ui.core.gesture.TouchSlop
import androidx.ui.core.testTag
import androidx.ui.geometry.Offset
import androidx.compose.foundation.Box
import androidx.compose.foundation.Text
import androidx.compose.foundation.layout.InnerPadding
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.preferredHeight
import androidx.compose.foundation.layout.preferredSize
import androidx.compose.foundation.layout.size
import androidx.ui.test.SemanticsNodeInteraction
import androidx.ui.test.assertCountEquals
import androidx.ui.test.assertIsDisplayed
import androidx.ui.test.assertIsNotDisplayed
import androidx.ui.test.center
import androidx.ui.test.onChildren
import androidx.ui.test.createComposeRule
import androidx.ui.test.getBoundsInRoot
import androidx.ui.test.performGesture
import androidx.ui.test.onNodeWithTag
import androidx.ui.test.onNodeWithText
import androidx.ui.test.runOnIdle
import androidx.ui.test.swipeUp
import androidx.ui.test.swipeWithVelocity
import androidx.ui.test.waitForIdle
import androidx.ui.unit.Density
import androidx.ui.unit.Dp
import androidx.ui.unit.dp
import com.google.common.collect.Range
import com.google.common.truth.IntegerSubject
import com.google.common.truth.Truth
import com.google.common.truth.Truth.assertThat
import com.google.common.truth.Truth.assertWithMessage
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.junit.runners.JUnit4
import java.util.concurrent.CountDownLatch

@LargeTest
@RunWith(JUnit4::class)
class LazyColumnItemsTest {
    private val LazyColumnItemsTag = "TestLazyColumnItems"

    @get:Rule
    val composeTestRule = createComposeRule()

    @Test
    fun compositionsAreDisposed_whenNodesAreScrolledOff() {
        var composed: Boolean
        var disposed = false
        // Ten 31dp spacers in a 300dp list
        val latch = CountDownLatch(10)
        // Make it long enough that it's _definitely_ taller than the screen
        val data = (1..50).toList()

        composeTestRule.setContent {
            // Fixed height to eliminate device size as a factor
            Box(Modifier.testTag(LazyColumnItemsTag).preferredHeight(300.dp)) {
                LazyColumnItems(items = data, modifier = Modifier.fillMaxSize()) {
                    onCommit {
                        composed = true
                        // Signal when everything is done composing
                        latch.countDown()
                        onDispose {
                            disposed = true
                        }
                    }

                    // There will be 10 of these in the 300dp box
                    Spacer(Modifier.preferredHeight(31.dp))
                }
            }
        }

        latch.await()
        composed = false

        assertWithMessage("Compositions were disposed before we did any scrolling")
            .that(disposed).isFalse()

        // Mostly a sanity check, this is not part of the behavior under test
        assertWithMessage("Additional composition occurred for no apparent reason")
            .that(composed).isFalse()

        onNodeWithTag(LazyColumnItemsTag)
            .performGesture { swipeUp() }

        waitForIdle()

        assertWithMessage("No additional items were composed after scroll, scroll didn't work")
            .that(composed).isTrue()

        // We may need to modify this test once we prefetch/cache items outside the viewport
        assertWithMessage(
            "No compositions were disposed after scrolling, compositions were leaked"
        ).that(disposed).isTrue()
    }

    @Test
    fun compositionsAreDisposed_whenDataIsChanged() {
        var composed: Boolean
        var disposals = 0
        val latch1 = CountDownLatch(3)
        val latch2 = CountDownLatch(2)
        val data1 = (1..3).toList()
        val data2 = (4..5).toList() // smaller, to ensure removal is handled properly

        var part2 by mutableStateOf(false)

        composeTestRule.setContent {
            LazyColumnItems(
                items = if (!part2) data1 else data2,
                modifier = Modifier.testTag(LazyColumnItemsTag).fillMaxSize()
            ) {
                onCommit {
                    composed = true
                    // Signal when everything is done composing
                    if (!part2) {
                        latch1.countDown()
                    } else {
                        latch2.countDown()
                    }
                    onDispose {
                        disposals++
                    }
                }

                Spacer(Modifier.height(50.dp))
            }
        }

        latch1.await()

        composed = false

        runOnIdle { part2 = true }

        latch2.await()

        assertWithMessage(
            "No additional items were composed after data change, something didn't work"
        ).that(composed).isTrue()

        // We may need to modify this test once we prefetch/cache items outside the viewport
        assertWithMessage(
            "Not enough compositions were disposed after scrolling, compositions were leaked"
        ).that(disposals).isEqualTo(data1.size)
    }

    @Test
    fun compositionsAreDisposed_whenAdapterListIsDisposed() {
        var emitAdapterList by mutableStateOf(true)
        var disposeCalledOnFirstItem = false
        var disposeCalledOnSecondItem = false

        composeTestRule.setContent {
            if (emitAdapterList) {
                LazyColumnItems(
                    items = listOf(0, 1),
                    modifier = Modifier.fillMaxSize()
                ) {
                    Box(Modifier.size(100.dp))
                    onDispose {
                        if (it == 1) {
                            disposeCalledOnFirstItem = true
                        } else {
                            disposeCalledOnSecondItem = true
                        }
                    }
                }
            }
        }

        runOnIdle {
            assertWithMessage("First item is not immediately disposed")
                .that(disposeCalledOnFirstItem).isFalse()
            assertWithMessage("Second item is not immediately disposed")
                .that(disposeCalledOnFirstItem).isFalse()
            emitAdapterList = false
        }

        runOnIdle {
            assertWithMessage("First item is correctly disposed")
                .that(disposeCalledOnFirstItem).isTrue()
            assertWithMessage("Second item is correctly disposed")
                .that(disposeCalledOnSecondItem).isTrue()
        }
    }

    @Test
    fun removeItemsTest() {
        val startingNumItems = 3
        var numItems = startingNumItems
        var numItemsModel by mutableStateOf(numItems)
        val tag = "List"
        composeTestRule.setContent {
            LazyColumnItems((1..numItemsModel).toList(), modifier = Modifier.testTag(tag)) {
                Text("$it")
            }
        }

        while (numItems >= 0) {
            // Confirm the number of children to ensure there are no extra items
            onNodeWithTag(tag)
                .onChildren()
                .assertCountEquals(numItems)

            // Confirm the children's content
            for (i in 1..3) {
                onNodeWithText("$i").apply {
                    if (i <= numItems) {
                        assertExists()
                    } else {
                        assertDoesNotExist()
                    }
                }
            }
            numItems--
            if (numItems >= 0) {
                // Don't set the model to -1
                runOnIdle { numItemsModel = numItems }
            }
        }
    }

    @Test
    fun changingDataTest() {
        val dataLists = listOf(
            (1..3).toList(),
            (4..8).toList(),
            (3..4).toList()
        )
        var dataModel by mutableStateOf(dataLists[0])
        val tag = "List"
        composeTestRule.setContent {
            LazyColumnItems(dataModel, modifier = Modifier.testTag(tag)) {
                Text("$it")
            }
        }

        for (data in dataLists) {
            runOnIdle { dataModel = data }

            // Confirm the number of children to ensure there are no extra items
            val numItems = data.size
            onNodeWithTag(tag)
                .onChildren()
                .assertCountEquals(numItems)

            // Confirm the children's content
            for (item in data) {
                onNodeWithText("$item").assertExists()
            }
        }
    }

    @Test
    fun whenItemsAreInitiallyCreatedWith0SizeWeCanScrollWhenTheyExpanded() {
        val thirdTag = "third"
        val items = (1..3).toList()
        var thirdHasSize by mutableStateOf(false)

        composeTestRule.setContent {
            LazyColumnItems(
                items = items,
                modifier = Modifier.fillMaxWidth()
                    .preferredHeight(100.dp)
                    .testTag(LazyColumnItemsTag)
            ) {
                if (it == 3) {
                    Spacer(Modifier.testTag(thirdTag)
                        .fillMaxWidth()
                        .preferredHeight(if (thirdHasSize) 60.dp else 0.dp))
                } else {
                    Spacer(Modifier.fillMaxWidth().preferredHeight(60.dp))
                }
            }
        }

        onNodeWithTag(LazyColumnItemsTag)
            .scrollBy(y = 21.dp, density = composeTestRule.density)

        onNodeWithTag(thirdTag)
            .assertExists()
            .assertIsNotDisplayed()

        runOnIdle {
            thirdHasSize = true
        }

        waitForIdle()

        onNodeWithTag(LazyColumnItemsTag)
            .scrollBy(y = 10.dp, density = composeTestRule.density)

        onNodeWithTag(thirdTag)
            .assertIsDisplayed()
    }

    @Test
    fun contentPaddingIsApplied() = with(composeTestRule.density) {
        val itemTag = "item"

        composeTestRule.setContent {
            LazyColumnItems(
                items = listOf(1),
                modifier = Modifier.size(100.dp)
                    .testTag(LazyColumnItemsTag),
                contentPadding = InnerPadding(
                    start = 10.dp,
                    top = 50.dp,
                    end = 10.dp,
                    bottom = 50.dp
                )
            ) {
                Spacer(Modifier.fillMaxWidth().preferredHeight(50.dp).testTag(itemTag))
            }
        }

        var itemBounds = onNodeWithTag(itemTag)
            .getBoundsInRoot()

        assertThat(itemBounds.top.toIntPx()).isWithin1PixelFrom(50.dp.toIntPx())
        assertThat(itemBounds.bottom.toIntPx()).isWithin1PixelFrom(100.dp.toIntPx())
        assertThat(itemBounds.left.toIntPx()).isWithin1PixelFrom(10.dp.toIntPx())
        assertThat(itemBounds.right.toIntPx())
            .isWithin1PixelFrom(100.dp.toIntPx() - 10.dp.toIntPx())

        onNodeWithTag(LazyColumnItemsTag)
            .scrollBy(y = 51.dp, density = composeTestRule.density)

        itemBounds = onNodeWithTag(itemTag)
            .getBoundsInRoot()

        assertThat(itemBounds.top.toIntPx()).isWithin1PixelFrom(0)
        assertThat(itemBounds.bottom.toIntPx()).isWithin1PixelFrom(50.dp.toIntPx())
    }

    @Test
    fun lazyColumnWrapsContent() = with(composeTestRule.density) {
        val itemInsideLazyColumn = "itemInsideLazyColumn"
        val itemOutsideLazyColumn = "itemOutsideLazyColumn"
        var sameSizeItems by mutableStateOf(true)

        composeTestRule.setContent {
            Row {
                LazyColumnItems(
                    items = listOf(1, 2),
                    modifier = Modifier.testTag(LazyColumnItemsTag)
                ) {
                    if (it == 1) {
                        Spacer(Modifier.preferredSize(50.dp).testTag(itemInsideLazyColumn))
                    } else {
                        Spacer(Modifier.preferredSize(if (sameSizeItems) 50.dp else 70.dp))
                    }
                }
                Spacer(Modifier.preferredSize(50.dp).testTag(itemOutsideLazyColumn))
            }
        }

        onNodeWithTag(itemInsideLazyColumn)
            .assertIsDisplayed()

        onNodeWithTag(itemOutsideLazyColumn)
            .assertIsDisplayed()

        var lazyColumnBounds = onNodeWithTag(LazyColumnItemsTag)
            .getBoundsInRoot()

        Truth.assertThat(lazyColumnBounds.left.toIntPx()).isWithin1PixelFrom(0.dp.toIntPx())
        Truth.assertThat(lazyColumnBounds.right.toIntPx()).isWithin1PixelFrom(50.dp.toIntPx())
        Truth.assertThat(lazyColumnBounds.top.toIntPx()).isWithin1PixelFrom(0.dp.toIntPx())
        // TODO: wrap-content on the main-axis must be implemented
//        Truth.assertThat(itemInsideBounds.bottom.toIntPx()).isWithin1PixelFrom(100.dp.toIntPx())

        runOnIdle {
            sameSizeItems = false
        }

        waitForIdle()

        onNodeWithTag(itemInsideLazyColumn)
            .assertIsDisplayed()

        onNodeWithTag(itemOutsideLazyColumn)
            .assertIsDisplayed()

        lazyColumnBounds = onNodeWithTag(LazyColumnItemsTag)
            .getBoundsInRoot()

        Truth.assertThat(lazyColumnBounds.left.toIntPx()).isWithin1PixelFrom(0.dp.toIntPx())
        Truth.assertThat(lazyColumnBounds.right.toIntPx()).isWithin1PixelFrom(70.dp.toIntPx())
        Truth.assertThat(lazyColumnBounds.top.toIntPx()).isWithin1PixelFrom(0.dp.toIntPx())
        // TODO: wrap-content on the main-axis must be implemented
//        Truth.assertThat(itemInsideBounds.bottom.toIntPx()).isWithin1PixelFrom(120.dp.toIntPx())
    }
}

internal fun IntegerSubject.isWithin1PixelFrom(expected: Int) {
    isIn(Range.closed(expected - 1, expected + 1))
}

internal fun SemanticsNodeInteraction.scrollBy(x: Dp = 0.dp, y: Dp = 0.dp, density: Density) =
    performGesture {
        with(density) {
            val touchSlop = TouchSlop.toIntPx()
            val xPx = x.toIntPx()
            val yPx = y.toIntPx()
            val offsetX = if (xPx > 0) xPx + touchSlop else if (xPx < 0) xPx - touchSlop else 0
            val offsetY = if (yPx > 0) yPx + touchSlop else if (yPx < 0) xPx - touchSlop else 0
            swipeWithVelocity(
                start = center,
                end = Offset(center.x - offsetX, center.y - offsetY),
                endVelocity = 0f
            )
        }
    }
