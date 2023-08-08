/*
 * Copyright 2022 The Android Open Source Project
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

package androidx.tv.foundation.lazy.list

import android.os.Build
import androidx.compose.foundation.background
import androidx.compose.foundation.focusable
import androidx.compose.foundation.gestures.scrollBy
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.requiredSize
import androidx.compose.foundation.layout.requiredWidth
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.text.BasicText
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.testutils.assertPixels
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.assertPositionInRootIsEqualTo
import androidx.compose.ui.test.captureToImage
import androidx.compose.ui.test.getUnclippedBoundsInRoot
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.unit.dp
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.filters.FlakyTest
import androidx.test.filters.LargeTest
import androidx.test.filters.SdkSuppress
import androidx.tv.foundation.PivotOffsets
import androidx.tv.foundation.lazy.AutoTestFrameClock
import com.google.common.truth.Truth.assertThat
import com.google.common.truth.Truth.assertWithMessage
import kotlinx.coroutines.runBlocking
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

@LargeTest
@RunWith(AndroidJUnit4::class)
/**
 * This class contains all LazyColumn-specific tests, as well as (by convention) tests that don't
 * need to be run in both orientations.
 *
 * To have a test run in both orientations (LazyRow and LazyColumn), add it to [LazyListTest]
 */
class LazyColumnTest {
    private val LazyListTag = "LazyListTag"

    @get:Rule
    val rule = createComposeRule()

    @Test
    fun compositionsAreDisposed_whenDataIsChanged() {
        var composed = 0
        var disposals = 0
        val data1 = (1..3).toList()
        val data2 = (4..5).toList() // smaller, to ensure removal is handled properly

        var part2 by mutableStateOf(false)

        rule.setContentWithTestViewConfiguration {
            TvLazyColumn(
                Modifier.testTag(LazyListTag).fillMaxSize(),
                pivotOffsets = PivotOffsets(parentFraction = 0f)
            ) {
                items(if (!part2) data1 else data2) {
                    DisposableEffect(NeverEqualObject) {
                        composed++
                        onDispose {
                            disposals++
                        }
                    }

                    Box(Modifier.height(50.dp).focusable())
                }
            }
        }

        rule.runOnIdle {
            assertWithMessage("Not all items were composed")
                .that(composed).isEqualTo(data1.size)
            composed = 0

            part2 = true
        }

        rule.runOnIdle {
            assertWithMessage(
                "No additional items were composed after data change, something didn't work"
            ).that(composed).isEqualTo(data2.size)

            // We may need to modify this test once we prefetch/cache items outside the viewport
            assertWithMessage(
                "Not enough compositions were disposed after scrolling, compositions were leaked"
            ).that(disposals).isEqualTo(data1.size)
        }
    }

    @Test
    fun compositionsAreDisposed_whenLazyListIsDisposed() {
        var emitLazyList by mutableStateOf(true)
        var disposeCalledOnFirstItem = false
        var disposeCalledOnSecondItem = false

        rule.setContentWithTestViewConfiguration {
            if (emitLazyList) {
                TvLazyColumn(
                    Modifier.fillMaxSize(),
                    pivotOffsets = PivotOffsets(parentFraction = 0f)
                ) {
                    items(2) {
                        Box(Modifier.requiredSize(100.dp).focusable())
                        DisposableEffect(Unit) {
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
            }
        }

        rule.runOnIdle {
            assertWithMessage("First item was incorrectly immediately disposed")
                .that(disposeCalledOnFirstItem).isFalse()
            assertWithMessage("Second item was incorrectly immediately disposed")
                .that(disposeCalledOnFirstItem).isFalse()
            emitLazyList = false
        }

        rule.runOnIdle {
            assertWithMessage("First item was not correctly disposed")
                .that(disposeCalledOnFirstItem).isTrue()
            assertWithMessage("Second item was not correctly disposed")
                .that(disposeCalledOnSecondItem).isTrue()
        }
    }

    @Test
    fun removeItemsTest() {
        var itemCount by mutableStateOf(3)
        val tag = "List"
        rule.setContentWithTestViewConfiguration {
            TvLazyColumn(Modifier.testTag(tag)) {
                items((0 until itemCount).toList()) {
                    BasicText("$it")
                }
            }
        }

        while (itemCount >= 0) {
            // Confirm the children's content
            for (i in 0 until 3) {
                rule.onNodeWithText("$i").apply {
                    if (i < itemCount) {
                        assertIsPlaced()
                    } else {
                        assertIsNotPlaced()
                    }
                }
            }
            itemCount--
        }
    }

    @Test
    fun changeItemsCountAndScrollImmediately() {
        lateinit var state: TvLazyListState
        var count by mutableStateOf(100)
        val composedIndexes = mutableListOf<Int>()
        rule.setContent {
            state = rememberTvLazyListState()
            TvLazyColumn(
                Modifier.fillMaxWidth().height(10.dp),
                state,
                pivotOffsets = PivotOffsets(parentFraction = 0f)
            ) {
                items(count) { index ->
                    composedIndexes.add(index)
                    Box(Modifier.size(20.dp).focusable())
                }
            }
        }

        rule.runOnIdle {
            composedIndexes.clear()
            count = 10
            runBlocking(AutoTestFrameClock()) {
                state.scrollToItem(50)
            }
            composedIndexes.forEach {
                assertThat(it).isLessThan(count)
            }
            assertThat(state.firstVisibleItemIndex).isEqualTo(9)
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
        rule.setContentWithTestViewConfiguration {
            TvLazyColumn(
                Modifier.testTag(tag),
                pivotOffsets = PivotOffsets(parentFraction = 0f)
            ) {
                items(dataModel) {
                    BasicText("$it")
                }
            }
        }

        for (data in dataLists) {
            rule.runOnIdle { dataModel = data }

            // Confirm the children's content
            for (index in 1..8) {
                if (index in data) {
                    rule.onNodeWithText("$index").assertIsDisplayed()
                } else {
                    rule.onNodeWithText("$index").assertIsNotPlaced()
                }
            }
        }
    }

    private val firstItemTag = "firstItemTag"
    private val secondItemTag = "secondItemTag"

    private fun prepareLazyColumnsItemsAlignment(horizontalGravity: Alignment.Horizontal) {
        rule.setContentWithTestViewConfiguration {
            TvLazyColumn(
                Modifier.testTag(LazyListTag).requiredWidth(100.dp),
                horizontalAlignment = horizontalGravity,
                pivotOffsets = PivotOffsets(parentFraction = 0f)
            ) {
                items(listOf(1, 2)) {
                    if (it == 1) {
                        Box(Modifier.size(50.dp).testTag(firstItemTag).focusable())
                    } else {
                        Box(Modifier.size(70.dp).testTag(secondItemTag).focusable())
                    }
                }
            }
        }

        rule.onNodeWithTag(firstItemTag)
            .assertIsDisplayed()

        rule.onNodeWithTag(secondItemTag)
            .assertIsDisplayed()

        val lazyColumnBounds = rule.onNodeWithTag(LazyListTag)
            .getUnclippedBoundsInRoot()

        with(rule.density) {
            // Verify the width of the column
            assertThat(lazyColumnBounds.left.roundToPx()).isWithin1PixelFrom(0.dp.roundToPx())
            assertThat(lazyColumnBounds.right.roundToPx()).isWithin1PixelFrom(100.dp.roundToPx())
        }
    }

    @Test
    fun lazyColumnAlignmentCenterHorizontally() {
        prepareLazyColumnsItemsAlignment(Alignment.CenterHorizontally)

        rule.onNodeWithTag(firstItemTag)
            .assertPositionInRootIsEqualTo(25.dp, 0.dp)

        rule.onNodeWithTag(secondItemTag)
            .assertPositionInRootIsEqualTo(15.dp, 50.dp)
    }

    @Test
    fun lazyColumnAlignmentStart() {
        prepareLazyColumnsItemsAlignment(Alignment.Start)

        rule.onNodeWithTag(firstItemTag)
            .assertPositionInRootIsEqualTo(0.dp, 0.dp)

        rule.onNodeWithTag(secondItemTag)
            .assertPositionInRootIsEqualTo(0.dp, 50.dp)
    }

    @Test
    fun lazyColumnAlignmentEnd() {
        prepareLazyColumnsItemsAlignment(Alignment.End)

        rule.onNodeWithTag(firstItemTag)
            .assertPositionInRootIsEqualTo(50.dp, 0.dp)

        rule.onNodeWithTag(secondItemTag)
            .assertPositionInRootIsEqualTo(30.dp, 50.dp)
    }

    @FlakyTest(bugId = 259297305)
    @Test
    fun removalWithMutableStateListOf() {
        val items = mutableStateListOf("1", "2", "3")

        val itemSize = with(rule.density) { 15.toDp() }

        rule.setContentWithTestViewConfiguration {
            TvLazyColumn {
                items(items) { item ->
                    Spacer(Modifier.size(itemSize).testTag(item))
                }
            }
        }

        rule.runOnIdle {
            items.removeLast()
        }

        rule.onNodeWithTag("1")
            .assertIsDisplayed()

        rule.onNodeWithTag("2")
            .assertIsDisplayed()

        rule.onNodeWithTag("3")
            .assertIsNotPlaced()
    }

    @Test
    fun recompositionOrder() {
        val outerState = mutableStateOf(0)
        val innerState = mutableStateOf(0)
        val recompositions = mutableListOf<Pair<Int, Int>>()

        rule.setContent {
            val localOuterState = outerState.value
            TvLazyColumn {
                items(count = 1) {
                    recompositions.add(localOuterState to innerState.value)
                    Box(Modifier.fillMaxSize())
                }
            }
        }

        rule.runOnIdle {
            innerState.value++
            outerState.value++
        }

        rule.runOnIdle {
            assertThat(recompositions).isEqualTo(
                listOf(0 to 0, 1 to 1)
            )
        }
    }

    @SdkSuppress(minSdkVersion = Build.VERSION_CODES.O)
    @Test
    fun scrolledAwayItemIsNotDisplayedAnymore() {
        lateinit var state: TvLazyListState
        rule.setContentWithTestViewConfiguration {
            state = rememberTvLazyListState()
            TvLazyColumn(
                Modifier
                    .requiredSize(10.dp)
                    .testTag(LazyListTag)
                    .graphicsLayer()
                    .background(Color.Blue),
                state = state,
                pivotOffsets = PivotOffsets(parentFraction = 0f)
            ) {
                items(2) {
                    val size = if (it == 0) 5.dp else 100.dp
                    val color = if (it == 0) Color.Red else Color.Transparent
                    Box(
                        Modifier
                            .fillMaxWidth()
                            .height(size)
                            .background(color)
                            .testTag("$it")
                            .focusable()
                    )
                }
            }
        }

        rule.runOnIdle {
            with(rule.density) {
                runBlocking {
                    // we scroll enough to make the Red item not visible anymore
                    state.scrollBy(6.dp.toPx())
                }
            }
        }

        // and verify there is no Red item displayed
        rule.onNodeWithTag(LazyListTag)
            .captureToImage()
            .assertPixels {
                Color.Blue
            }
    }

    @Test
    fun wrappedNestedLazyRowDisplayCorrectContent() {
        lateinit var state: TvLazyListState
        rule.setContentWithTestViewConfiguration {
            state = rememberTvLazyListState()
            TvLazyColumn(
                Modifier.size(20.dp),
                state = state,
                pivotOffsets = PivotOffsets(parentFraction = 0f)
            ) {
                items(100) {
                    LazyRowWrapped {
                        BasicText("$it", Modifier.size(21.dp))
                    }
                }
            }
        }

        (1..10).forEach { item ->
            rule.runOnIdle {
                runBlocking {
                    state.scrollToItem(item)
                }
            }

            rule.onNodeWithText("$item")
                .assertIsDisplayed()
        }
    }

    @Composable
    private fun LazyRowWrapped(content: @Composable () -> Unit) {
        TvLazyRow {
            items(count = 1) {
                content()
            }
        }
    }
}

internal fun Modifier.drawOutsideOfBounds() = drawBehind {
    val inflate = 20.dp.roundToPx().toFloat()
    drawRect(
        Color.Red,
        Offset(-inflate, -inflate),
        Size(size.width + inflate * 2, size.height + inflate * 2)
    )
}
