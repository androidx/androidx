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

package androidx.wear.compose.foundation.lazy

import android.os.Build
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.scrollBy
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.requiredSize
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.text.BasicText
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.MutableState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.key
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.runtime.snapshotFlow
import androidx.compose.runtime.withFrameNanos
import androidx.compose.testutils.WithTouchSlop
import androidx.compose.testutils.assertPixels
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.onFocusChanged
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.input.nestedscroll.NestedScrollConnection
import androidx.compose.ui.input.nestedscroll.nestedScroll
import androidx.compose.ui.layout.LocalPinnableContainer
import androidx.compose.ui.layout.PinnableContainer
import androidx.compose.ui.layout.SubcomposeLayout
import androidx.compose.ui.layout.SubcomposeLayoutState
import androidx.compose.ui.layout.SubcomposeSlotReusePolicy
import androidx.compose.ui.layout.layout
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.test.ExperimentalTestApi
import androidx.compose.ui.test.SemanticsNodeInteraction
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.assertIsNotDisplayed
import androidx.compose.ui.test.assertTopPositionInRootIsEqualTo
import androidx.compose.ui.test.captureToImage
import androidx.compose.ui.test.hasScrollAction
import androidx.compose.ui.test.junit4.v2.createComposeRule
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performRotaryScrollInput
import androidx.compose.ui.test.performTouchInput
import androidx.compose.ui.test.swipeUp
import androidx.compose.ui.test.swipeWithVelocity
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.Velocity
import androidx.compose.ui.unit.dp
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.filters.MediumTest
import androidx.test.filters.SdkSuppress
import androidx.wear.compose.foundation.ExperimentalWearFoundationApi
import androidx.wear.compose.foundation.TEST_TAG
import androidx.wear.compose.foundation.WearComposeFoundationFlags
import androidx.wear.compose.foundation.isRoundDevice
import androidx.wear.compose.foundation.rotary.HighResSnapRotaryScrollableBehavior
import androidx.wear.compose.foundation.rotary.LowResSnapRotaryScrollableBehavior
import androidx.wear.compose.foundation.rotary.RotaryHapticHandler
import androidx.wear.compose.foundation.rotary.RotaryScrollHandler
import androidx.wear.compose.foundation.rotary.RotaryScrollableDefaults
import androidx.wear.compose.foundation.rotary.RotarySnapSensitivityValues
import androidx.wear.compose.foundation.rotary.ThresholdHandler
import androidx.wear.compose.foundation.rotary.TransformingLazyColumnSnapHandler
import com.google.common.truth.Truth.assertThat
import com.google.common.truth.Truth.assertWithMessage
import kotlin.concurrent.thread
import kotlin.math.abs
import kotlin.math.sign
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.launch
import kotlinx.coroutines.runBlocking
import org.junit.After
import org.junit.Assert.assertTrue
import org.junit.Assume.assumeTrue
import org.junit.Before
import org.junit.Rule
import org.junit.runner.RunWith

@MediumTest
@RunWith(AndroidJUnit4::class)
class TransformingLazyColumnTest {
    private val firstItemTag = "firstItemTag"
    private val lastItemTag = "lastItemTag"
    private val lazyListTag = "LazyListTag"

    @get:Rule val rule = createComposeRule()

    private var savedSnapSegmentsEnabled: Boolean = false

    @OptIn(ExperimentalWearFoundationApi::class)
    @Before
    fun saveFlags() {
        savedSnapSegmentsEnabled =
            WearComposeFoundationFlags.isTransformingLazyColumnSnapSegmentsEnabled
    }

    @OptIn(ExperimentalWearFoundationApi::class)
    @After
    fun restoreFlags() {
        WearComposeFoundationFlags.isTransformingLazyColumnSnapSegmentsEnabled =
            savedSnapSegmentsEnabled
    }

    @Test
    fun firstItemIsDisplayed() {
        rule.setContent {
            TransformingLazyColumn {
                items(100) {
                    Box(
                        Modifier.requiredSize(50.dp)
                            .testTag(
                                when (it) {
                                    0 -> firstItemTag
                                    99 -> lastItemTag
                                    else -> "empty"
                                }
                            )
                    )
                }
            }
        }
        rule.onNodeWithTag(firstItemTag).assertExists()
        // TransformingLazyColumn is really lazy and doesn't put on the screen until it's scrolled
        // to.
        rule.onNodeWithTag(lastItemTag).assertIsNotPlaced()
    }

    @Test
    fun compositionsAreDisposed_whenDataIsChanged() {
        var composed = 0
        var disposals = 0
        val data1 = (1..3).toList()
        val data2 = (4..5).toList() // smaller, to ensure removal is handled properly

        var part2 by mutableStateOf(false)

        rule.setContentWithTestViewConfiguration {
            TransformingLazyColumn(Modifier.testTag(lazyListTag).fillMaxSize()) {
                items(if (!part2) data1 else data2) {
                    DisposableEffect(NeverEqualObject) {
                        composed++
                        onDispose { disposals++ }
                    }

                    Spacer(Modifier.height(50.dp))
                }
            }
        }

        rule.runOnIdle {
            assertWithMessage("Not all items were composed").that(composed).isEqualTo(data1.size)
            composed = 0

            part2 = true
        }

        rule.runOnIdle {
            assertWithMessage(
                    "No additional items were composed after data change, something didn't work"
                )
                .that(composed)
                .isEqualTo(data2.size)

            // We may need to modify this test once we prefetch/cache items outside the viewport
            assertWithMessage(
                    "Not enough compositions were disposed after scrolling, compositions were leaked"
                )
                .that(disposals)
                .isEqualTo(data1.size)
        }
    }

    @Test
    fun compositionsAreDisposed_whenLazyListIsDisposed() {
        var emitLazyList by mutableStateOf(true)
        var disposeCalledOnFirstItem = false
        var disposeCalledOnSecondItem = false

        rule.setContentWithTestViewConfiguration {
            if (emitLazyList) {
                TransformingLazyColumn(Modifier.fillMaxSize()) {
                    items(2) {
                        Box(Modifier.requiredSize(100.dp))
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
                .that(disposeCalledOnFirstItem)
                .isFalse()
            assertWithMessage("Second item was incorrectly immediately disposed")
                .that(disposeCalledOnFirstItem)
                .isFalse()
            emitLazyList = false
        }

        rule.runOnIdle {
            assertWithMessage("First item was not correctly disposed")
                .that(disposeCalledOnFirstItem)
                .isTrue()
            assertWithMessage("Second item was not correctly disposed")
                .that(disposeCalledOnSecondItem)
                .isTrue()
        }
    }

    @Test
    fun removeItemsTest() {
        var itemCount by mutableStateOf(3)
        val tag = "List"
        rule.setContentWithTestViewConfiguration {
            TransformingLazyColumn(Modifier.testTag(tag)) {
                items((0 until itemCount).toList()) { BasicText("$it") }
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
    fun changeData() {
        val dataLists = listOf((1..3).toList(), (4..8).toList(), (3..4).toList())
        var dataModel by mutableStateOf(dataLists[0])
        val tag = "List"
        rule.setContentWithTestViewConfiguration {
            TransformingLazyColumn(Modifier.testTag(tag)) {
                items(dataModel.size) { BasicText("${dataModel[it]}") }
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

    @Test
    fun changeDataIndexed() {
        val dataLists = listOf((1..3).toList(), (4..8).toList(), (3..4).toList())
        var dataModel by mutableStateOf(emptyList<Int>())
        val tag = "List"
        rule.setContentWithTestViewConfiguration {
            TransformingLazyColumn(Modifier.testTag(tag)) {
                itemsIndexed(dataModel) { index, element -> BasicText("$index - $element") }
            }
        }

        for (data in dataLists) {
            rule.runOnIdle { dataModel = data }

            // Confirm the children's content
            for (index in data.indices) {
                rule.onNodeWithText("$index - ${data[index]}").assertIsDisplayed()
            }
        }
    }

    @Test
    fun removalWithMutableStateListOf() {
        val items = mutableStateListOf("1", "2", "3")
        val itemSize = with(rule.density) { 15.toDp() }
        rule.setContentWithTestViewConfiguration {
            TransformingLazyColumn {
                items(items.size) { Spacer(Modifier.size(itemSize).testTag(items[it])) }
            }
        }

        rule.runOnIdle { items.removeAt(items.lastIndex) }

        rule.onNodeWithTag("1").assertIsDisplayed()
        rule.onNodeWithTag("2").assertIsDisplayed()
        rule.onNodeWithTag("3").assertIsNotPlaced()
    }

    @Test
    fun recompositionOrder() {
        val outerState = mutableStateOf(0)
        val innerState = mutableStateOf(0)
        val recompositions = mutableListOf<Pair<Int, Int>>()

        rule.setContent {
            val localOuterState = outerState.value
            TransformingLazyColumn {
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

        rule.runOnIdle { assertThat(recompositions).isEqualTo(listOf(0 to 0, 1 to 1)) }
    }

    @SdkSuppress(minSdkVersion = Build.VERSION_CODES.O)
    @Test
    fun scrolledAwayItemIsNotDisplayedAnymore() {
        lateinit var state: TransformingLazyColumnState
        rule.setContentWithTestViewConfiguration {
            state = rememberTransformingLazyColumnState()
            TransformingLazyColumn(
                Modifier.requiredSize(10.dp)
                    .testTag(lazyListTag)
                    .graphicsLayer()
                    .background(Color.Blue),
                state = state,
            ) {
                items(2) {
                    val size = if (it == 0) 5.dp else 100.dp
                    val color = if (it == 0) Color.Red else Color.Transparent
                    Box(Modifier.fillMaxWidth().height(size).background(color).testTag("$it"))
                }
            }
        }

        rule.runOnIdle {
            with(rule.density) {
                runBlocking {
                    // Scroll enough to put Red item's center on top.
                    state.scrollBy(5.dp.toPx())

                    // Scroll half size of the Red item.
                    state.scrollBy(3.dp.toPx())
                }
            }
        }

        // and verify there is no Red item displayed
        rule.onNodeWithTag(lazyListTag).captureToImage().assertPixels { Color.Blue }
    }

    @Test
    fun scrollingDeactivatedListIsNotCrashing() {
        val itemSize = 10f
        val itemSizeDp = with(rule.density) { itemSize.toDp() }

        val subcomposeState = SubcomposeLayoutState(SubcomposeSlotReusePolicy(1))
        val state = TransformingLazyColumnState()
        var compose by mutableStateOf(true)
        rule.setContent {
            SubcomposeLayout(state = subcomposeState) { constraints ->
                val node =
                    if (compose) {
                        subcompose(Unit) {
                                TransformingLazyColumn(Modifier.size(itemSizeDp), state) {
                                    items(100) { Box(Modifier.size(itemSizeDp)) }
                                }
                            }
                            .first()
                            .measure(constraints)
                    } else {
                        null
                    }
                layout(10, 10) { node?.place(0, 0) }
            }
        }
        rule.runOnIdle { compose = false }
        rule.runOnIdle { runBlocking { state.scrollBy(itemSize) } }
    }

    @Test
    fun isNotFocusedWithDisabledRotary() {
        var focusSet = false

        rule.setContent {
            TransformingLazyColumn(
                modifier = Modifier.onFocusChanged { focusSet = it.isFocused },
                // Disable rotary and focus as well
                rotaryScrollableBehavior = null,
            ) {
                items(100) { BasicText("item $it") }
            }
        }

        assert(!focusSet)
    }

    @Test
    fun rotaryInputWhenScrollEnabled() {
        testTransformingLazyColumnRotary(true, 2)
    }

    @Test
    fun rotaryInputWhenScrollDisabled() {
        testTransformingLazyColumnRotary(false, 0)
    }

    @Test
    fun supportsEmptyItems() {
        rule.setContent {
            TransformingLazyColumn(
                state = rememberTransformingLazyColumnState(),
                modifier = Modifier.testTag(lazyListTag),
            ) {
                items(10) {}
            }
        }
        rule.onNodeWithTag(lazyListTag).assertIsDisplayed()
    }

    @Test
    fun supportsInitialScrollPosition() {
        lateinit var state: TransformingLazyColumnState

        rule.setContent {
            state = rememberTransformingLazyColumnState(initialAnchorItemIndex = 8)
            TransformingLazyColumn(
                state = state,
                contentPadding = PaddingValues(0.dp),
                modifier = Modifier.testTag(lazyListTag).size(90.dp),
            ) {
                items(10) { Spacer(Modifier.size(30.dp).testTag("item $it")) }
            }
        }
        rule.waitForIdle()
        rule.onNodeWithTag("item 8").assertIsDisplayed()
    }

    @Test
    fun nextItemSnapsToListTop_whenAnchoredItemIsRemoved() {
        val items =
            mutableStateListOf("item 0", "item 1", "item 2", "Anchor", "item 4", "item 5", "item 6")
        val itemSize = 48.dp
        val anchorSize = itemSize * 3 // Bigger than the TLC height
        lateinit var state: TransformingLazyColumnState

        rule.setContent {
            state = rememberTransformingLazyColumnState()
            TransformingLazyColumn(
                modifier = Modifier.testTag(lazyListTag).height(itemSize * 2),
                state = state,
            ) {
                items(items.size, key = { items[it] }) {
                    Spacer(
                        Modifier.height(if (items[it] == "Anchor") anchorSize else itemSize)
                            .testTag(items[it])
                    )
                }
            }
        }

        // Scroll to "Anchor item".
        rule.runOnIdle { runBlocking { state.scrollToItem(3) } }
        rule.waitForIdle()
        // Remove the anchor
        rule.runOnIdle { items.removeAt(3) }
        rule.waitForIdle()

        // Assert that "item 4" now is aligned to the top of the screen.
        val finalOffset =
            state.layoutInfo.visibleItems.first { it.key == "item 4" }.offset.toFloat()
        assertThat(finalOffset).isEqualTo(0)

        // Assert that the anchor index has been correctly updated.
        assertThat(state.anchorItemIndex).isEqualTo(3) // The index of "item 4" is now 3.
    }

    @Test
    fun scrollPositionMaintained_whenItemBeforeAnchorIsRemoved() {
        val items = mutableStateListOf("item 0", "item 1", "item 2", "item 3", "item 4")
        val itemSize = 48.dp
        val state = setupTlcWithMutableList(items, itemSize)

        // Scroll to "item 3".
        rule.runOnIdle {
            runBlocking {
                state.scrollToItem(3) // Index of "item 3"
            }
        }
        rule.waitForIdle()

        // Record the current scroll offset of "item 3".
        val initialOffset = state.layoutInfo.visibleItems.first { it.key == "item 3" }.offset

        // Remove "item 2", which is before the current anchor.
        rule.runOnIdle { items.removeAt(2) }
        rule.waitForIdle()

        // Assert that "item 3" is still visible and at the same offset.
        val finalOffset = state.layoutInfo.visibleItems.first { it.key == "item 3" }.offset
        assertThat(finalOffset).isEqualTo(initialOffset)

        // Assert that the anchor index has been correctly updated to "item 3"'s new position.
        assertThat(state.anchorItemIndex).isEqualTo(2) // The index of "item 3" is now 2.
    }

    @Test
    fun listSnapsToNextItem_whenAnchoredItemIsRemoved() {
        val items = mutableStateListOf("item 0", "item 1", "item 2", "item 3", "item 4")
        val itemSize = 48.dp
        val state = setupTlcWithMutableList(items, itemSize)

        // Scroll to "item 2"
        rule.runOnIdle {
            runBlocking {
                state.scrollToItem(2) // Index of "item 2"
            }
        }
        rule.waitForIdle()

        // Record the current scroll offset of "item 2".
        val initialOffset = state.layoutInfo.visibleItems.first { it.key == "item 2" }.offset

        // Remove the anchored item, "item 2"
        rule.runOnIdle { items.removeAt(2) }
        rule.waitForIdle()

        // Assert that "item 3" has the same offset as deleted item.
        val finalOffset = state.layoutInfo.visibleItems.first { it.key == "item 3" }.offset
        assertThat(finalOffset).isEqualTo(initialOffset)

        // Assert that the new anchor is the next item, "item 3", at its new index.
        assertThat(state.anchorItemIndex).isEqualTo(2) // The index of "item 3" is now 2.
    }

    @Test
    fun reverseLayout_compositionsAreDisposed_whenLazyListIsDisposed() {
        var emitLazyList by mutableStateOf(true)
        var disposeCalledOnFirstItem = false
        var disposeCalledOnSecondItem = false

        rule.setContentWithTestViewConfiguration {
            if (emitLazyList) {
                TransformingLazyColumn(Modifier.fillMaxSize(), reverseLayout = true) {
                    items(2) {
                        Box(Modifier.requiredSize(100.dp))
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
                .that(disposeCalledOnFirstItem)
                .isFalse()
            assertWithMessage("Second item was incorrectly immediately disposed")
                .that(disposeCalledOnFirstItem)
                .isFalse()
            emitLazyList = false
        }

        rule.runOnIdle {
            assertWithMessage("First item was not correctly disposed")
                .that(disposeCalledOnFirstItem)
                .isTrue()
            assertWithMessage("Second item was not correctly disposed")
                .that(disposeCalledOnSecondItem)
                .isTrue()
        }
    }

    @Test
    fun reverseLayout_removeItemsTest() {
        var itemCount by mutableStateOf(3)
        val tag = "List"
        rule.setContentWithTestViewConfiguration {
            TransformingLazyColumn(Modifier.testTag(tag), reverseLayout = true) {
                items((0 until itemCount).toList()) { BasicText("$it") }
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
    fun reverseLayout_changeData() {
        val dataLists = listOf((1..3).toList(), (4..8).toList(), (3..4).toList())
        var dataModel by mutableStateOf(dataLists[0])
        val tag = "List"
        rule.setContentWithTestViewConfiguration {
            TransformingLazyColumn(Modifier.testTag(tag), reverseLayout = true) {
                items(dataModel.size) { BasicText("${dataModel[it]}") }
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

    @Test
    fun reverseLayout_changeDataIndexed() {
        val dataLists = listOf((1..3).toList(), (4..8).toList(), (3..4).toList())
        var dataModel by mutableStateOf(emptyList<Int>())
        val tag = "List"
        rule.setContentWithTestViewConfiguration {
            TransformingLazyColumn(Modifier.testTag(tag), reverseLayout = true) {
                itemsIndexed(dataModel) { index, element -> BasicText("$index - $element") }
            }
        }

        for (data in dataLists) {
            rule.runOnIdle { dataModel = data }

            // Confirm the children's content
            for (index in data.indices) {
                rule.onNodeWithText("$index - ${data[index]}").assertIsDisplayed()
            }
        }
    }

    @Test
    fun reverseLayout_removalWithMutableStateListOf() {
        val items = mutableStateListOf("1", "2", "3")

        val itemSize = with(rule.density) { 15.toDp() }

        rule.setContentWithTestViewConfiguration {
            TransformingLazyColumn(reverseLayout = true) {
                items(items.size) { Spacer(Modifier.size(itemSize).testTag(items[it])) }
            }
        }

        rule.runOnIdle { items.removeAt(items.lastIndex) }

        rule.onNodeWithTag("1").assertIsDisplayed()

        rule.onNodeWithTag("2").assertIsDisplayed()

        rule.onNodeWithTag("3").assertIsNotPlaced()
    }

    @Test
    fun reverseLayout_recompositionOrder() {
        val outerState = mutableStateOf(0)
        val innerState = mutableStateOf(0)
        val recompositions = mutableListOf<Pair<Int, Int>>()

        rule.setContent {
            val localOuterState = outerState.value
            TransformingLazyColumn(reverseLayout = true) {
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

        rule.runOnIdle { assertThat(recompositions).isEqualTo(listOf(0 to 0, 1 to 1)) }
    }

    @SdkSuppress(minSdkVersion = Build.VERSION_CODES.O)
    @Test
    fun reverseLayout_scrolledAwayItemIsNotDisplayedAnymore() {
        lateinit var state: TransformingLazyColumnState
        rule.setContentWithTestViewConfiguration {
            state = rememberTransformingLazyColumnState()
            TransformingLazyColumn(
                Modifier.requiredSize(10.dp)
                    .testTag(lazyListTag)
                    .graphicsLayer()
                    .background(Color.Blue),
                state = state,
                reverseLayout = true,
            ) {
                items(2) {
                    val size = if (it == 0) 5.dp else 100.dp
                    val color = if (it == 0) Color.Red else Color.Transparent
                    Box(Modifier.fillMaxWidth().height(size).background(color).testTag("$it"))
                }
            }
        }

        rule.runOnIdle {
            with(rule.density) {
                runBlocking {
                    // Scroll enough to put Red item's center on top.
                    state.scrollBy(5.dp.toPx())

                    // Scroll half size of the Red item.
                    state.scrollBy(3.dp.toPx())
                }
            }
        }

        // and verify there is no Red item displayed
        rule.onNodeWithTag(lazyListTag).captureToImage().assertPixels { Color.Blue }
    }

    @Test
    fun reverseLayout_scrollingDeactivatedListIsNotCrashing() {
        val itemSize = 10f
        val itemSizeDp = with(rule.density) { itemSize.toDp() }

        val subcomposeState = SubcomposeLayoutState(SubcomposeSlotReusePolicy(1))
        val state = TransformingLazyColumnState()
        var compose by mutableStateOf(true)
        rule.setContent {
            SubcomposeLayout(state = subcomposeState) { constraints ->
                val node =
                    if (compose) {
                        subcompose(Unit) {
                                TransformingLazyColumn(
                                    Modifier.size(itemSizeDp),
                                    state,
                                    reverseLayout = true,
                                ) {
                                    items(100) { Box(Modifier.size(itemSizeDp)) }
                                }
                            }
                            .first()
                            .measure(constraints)
                    } else {
                        null
                    }
                layout(10, 10) { node?.place(0, 0) }
            }
        }
        rule.runOnIdle { compose = false }
        rule.runOnIdle { runBlocking { state.scrollBy(itemSize) } }
    }

    @Test
    fun reverseLayout_isNotFocusedWithDisabledRotary() {
        var focusSet = false

        rule.setContent {
            TransformingLazyColumn(
                modifier = Modifier.onFocusChanged { focusSet = it.isFocused },
                // Disable rotary and focus as well
                rotaryScrollableBehavior = null,
                reverseLayout = true,
            ) {
                items(100) { BasicText("item $it") }
            }
        }

        assert(!focusSet)
    }

    @Test
    fun reverseLayout_rotaryInputWhenScrollEnabled() {
        testTransformingLazyColumnRotary(
            userScrollEnabled = true,
            scrollTarget = 2,
            reverseLayout = true,
        )
    }

    @Test
    fun reverseLayout_rotaryInputWhenScrollDisabled() {
        testTransformingLazyColumnRotary(
            userScrollEnabled = false,
            scrollTarget = 0,
            reverseLayout = true,
        )
    }

    @Test
    fun reverseLayout_supportsEmptyItems() {
        rule.setContent {
            TransformingLazyColumn(
                state = rememberTransformingLazyColumnState(),
                modifier = Modifier.testTag(lazyListTag),
                reverseLayout = true,
            ) {
                items(10) {}
            }
        }
        rule.onNodeWithTag(lazyListTag).assertIsDisplayed()
    }

    @Test
    fun reverseLayout_supportsInitialScrollPosition() {
        lateinit var state: TransformingLazyColumnState

        rule.setContent {
            state = rememberTransformingLazyColumnState(initialAnchorItemIndex = 8)
            TransformingLazyColumn(
                state = state,
                contentPadding = PaddingValues(0.dp),
                modifier = Modifier.testTag(lazyListTag).size(90.dp),
                reverseLayout = true,
            ) {
                items(10) { Spacer(Modifier.size(30.dp).testTag("item $it")) }
            }
        }
        rule.waitForIdle()
        rule.onNodeWithTag("item 8").assertIsDisplayed()
    }

    @Test
    fun reverseLayout_nextItemSnapsToListBeginning_whenAnchoredItemIsRemoved() {
        val items =
            mutableStateListOf("item 0", "item 1", "item 2", "Anchor", "item 4", "item 5", "item 6")
        val itemSize = 48.dp
        val anchorSize = itemSize * 3 // Bigger than the TLC height
        lateinit var state: TransformingLazyColumnState

        rule.setContent {
            state = rememberTransformingLazyColumnState()
            TransformingLazyColumn(
                modifier = Modifier.testTag(lazyListTag).height(itemSize * 2),
                state = state,
                reverseLayout = true,
            ) {
                items(items.size, key = { items[it] }) {
                    Spacer(
                        Modifier.height(if (items[it] == "Anchor") anchorSize else itemSize)
                            .testTag(items[it])
                    )
                }
            }
        }

        // Scroll to "Anchor item".
        rule.runOnIdle { runBlocking { state.scrollToItem(3) } }
        rule.waitForIdle()
        // Remove the anchor
        rule.runOnIdle { items.removeAt(3) }
        rule.waitForIdle()

        // Assert that "item 4" now is aligned to the top of the screen.
        val finalOffset =
            state.layoutInfo.visibleItems.first { it.key == "item 4" }.offset.toFloat()
        assertThat(finalOffset).isEqualTo(0)

        // Assert that the anchor index has been correctly updated.
        assertThat(state.anchorItemIndex).isEqualTo(3) // The index of "item 4" is now 3.
    }

    @Test
    fun reverseLayout_scrollPositionMaintained_whenItemBeforeAnchorIsRemoved() {
        val items = mutableStateListOf("item 0", "item 1", "item 2", "item 3", "item 4")
        val itemSize = 48.dp
        val state = setupTlcWithMutableList(items, itemSize, reverseLayout = true)

        // Scroll to "item 3".
        rule.runOnIdle {
            runBlocking {
                state.scrollToItem(3) // Index of "item 3"
            }
        }
        rule.waitForIdle()

        // Record the current scroll offset of "item 3".
        val initialOffset = state.layoutInfo.visibleItems.first { it.key == "item 3" }.offset

        // Remove "item 2", which is before the current anchor.
        rule.runOnIdle { items.removeAt(2) }
        rule.waitForIdle()

        // Assert that "item 3" is still visible and at the same offset.
        val finalOffset = state.layoutInfo.visibleItems.first { it.key == "item 3" }.offset
        assertThat(finalOffset).isEqualTo(initialOffset)

        // Assert that the anchor index has been correctly updated to "item 3"'s new position.
        assertThat(state.anchorItemIndex).isEqualTo(2) // The index of "item 3" is now 2.
    }

    @Test
    fun reverseLayout_listSnapsToNextItem_whenAnchoredItemIsRemoved() {
        val items = mutableStateListOf("item 0", "item 1", "item 2", "item 3", "item 4")
        val itemSize = 48.dp
        val state = setupTlcWithMutableList(items, itemSize, reverseLayout = true)

        // Scroll to "item 2"
        rule.runOnIdle {
            runBlocking {
                state.scrollToItem(2) // Index of "item 2"
            }
        }
        rule.waitForIdle()

        // Record the current scroll offset of "item 2".
        val initialOffset = state.layoutInfo.visibleItems.first { it.key == "item 2" }.offset

        // Remove the anchored item, "item 2"
        rule.runOnIdle { items.removeAt(2) }
        rule.waitForIdle()

        // Assert that "item 3" has the same offset as deleted item.
        val finalOffset = state.layoutInfo.visibleItems.first { it.key == "item 3" }.offset
        assertThat(finalOffset).isEqualTo(initialOffset)

        // Assert that the new anchor is the next item, "item 3", at its new index.
        assertThat(state.anchorItemIndex).isEqualTo(2) // The index of "item 3" is now 2.
    }

    @Test
    fun reverseLayout_firstItemIsDisplayed() {
        rule.setContent {
            TransformingLazyColumn(reverseLayout = true) {
                items(100) {
                    Box(
                        Modifier.requiredSize(50.dp)
                            .testTag(
                                when (it) {
                                    0 -> firstItemTag
                                    99 -> lastItemTag
                                    else -> "empty"
                                }
                            )
                    )
                }
            }
        }
        rule.onNodeWithTag(firstItemTag).assertExists()
        // TransformingLazyColumn is really lazy and doesn't put on the screen until it's scrolled
        // to.
        rule.onNodeWithTag(lastItemTag).assertIsNotPlaced()
    }

    @Test
    fun reverseLayout_compositionsAreDisposed_whenDataIsChanged() {
        var composed = 0
        var disposals = 0
        val data1 = (1..3).toList()
        val data2 = (4..5).toList() // smaller, to ensure removal is handled properly

        var part2 by mutableStateOf(false)

        rule.setContentWithTestViewConfiguration {
            TransformingLazyColumn(
                Modifier.testTag(lazyListTag).fillMaxSize(),
                reverseLayout = true,
            ) {
                items(if (!part2) data1 else data2) {
                    DisposableEffect(NeverEqualObject) {
                        composed++
                        onDispose { disposals++ }
                    }

                    Spacer(Modifier.height(50.dp))
                }
            }
        }

        rule.runOnIdle {
            assertWithMessage("Not all items were composed").that(composed).isEqualTo(data1.size)
            composed = 0

            part2 = true
        }

        rule.runOnIdle {
            assertWithMessage(
                    "No additional items were composed after data change, something didn't work"
                )
                .that(composed)
                .isEqualTo(data2.size)

            // We may need to modify this test once we prefetch/cache items outside the viewport
            assertWithMessage(
                    "Not enough compositions were disposed after scrolling, compositions were leaked"
                )
                .that(disposals)
                .isEqualTo(data1.size)
        }
    }

    @Test
    fun snapFlingBehavior_dragPastThreshold_snapsToNextItem() {
        lateinit var state: TransformingLazyColumnState
        val containerHeight = 200.dp
        val itemHeight = 50.dp
        val itemHeightPx = with(rule.density) { itemHeight.toPx() }
        val initialIndex = 10
        rule.setContent {
            WithTouchSlop(0f) {
                state = rememberTransformingLazyColumnState(initialAnchorItemIndex = initialIndex)
                TransformingLazyColumn(
                    state = state,
                    modifier = Modifier.size(containerHeight).testTag(TEST_TAG),
                    flingBehavior = TransformingLazyColumnDefaults.snapFlingBehavior(state),
                ) {
                    items(100) { Box(Modifier.fillMaxWidth().height(itemHeight)) }
                }
            }
        }
        // This should trigger a snap to the NEXT item (Index 11).
        val dragDistance = itemHeightPx * 0.6f

        rule.onNodeWithTag(TEST_TAG).performTouchInput {
            swipeWithVelocity(
                start = center,
                end = center.copy(y = center.y - dragDistance),
                endVelocity = 0f,
            )
        }
        rule.waitForIdle()

        assertThat(state.anchorItemIndex).isEqualTo(initialIndex + 1)
        assertThat(state.anchorItemScrollOffset).isEqualTo(0)
    }

    @Test
    fun snapFlingBehavior_smallDrag_snapsBackToCurrentItem() {
        lateinit var state: TransformingLazyColumnState
        val containerHeight = 200.dp
        val itemHeight = 50.dp
        val itemHeightPx = with(rule.density) { itemHeight.toPx() }
        val initialIndex = 10
        rule.setContent {
            WithTouchSlop(0f) {
                state = rememberTransformingLazyColumnState(initialAnchorItemIndex = initialIndex)
                TransformingLazyColumn(
                    state = state,
                    modifier = Modifier.size(containerHeight).testTag(TEST_TAG),
                    flingBehavior = TransformingLazyColumnDefaults.snapFlingBehavior(state),
                ) {
                    items(100) { Box(Modifier.fillMaxWidth().height(itemHeight)) }
                }
            }
        }
        // This should trigger a snap BACK to the current item (Index 10).
        val dragDistance = itemHeightPx * 0.2f

        rule.onNodeWithTag(TEST_TAG).performTouchInput {
            swipeWithVelocity(
                start = center,
                end = center.copy(y = center.y - dragDistance),
                endVelocity = 0f,
            )
        }
        rule.waitForIdle()

        assertThat(state.anchorItemIndex).isEqualTo(initialIndex)
        assertThat(state.anchorItemScrollOffset).isEqualTo(0)
    }

    @Test
    fun snapFlingBehavior_fling_snapsToItem() {
        lateinit var state: TransformingLazyColumnState
        val containerHeight = 200.dp
        val itemHeight = 50.dp
        val initialIndex = 10
        rule.setContent {
            WithTouchSlop(0f) {
                state = rememberTransformingLazyColumnState(initialAnchorItemIndex = initialIndex)
                TransformingLazyColumn(
                    state = state,
                    modifier = Modifier.size(containerHeight).testTag(TEST_TAG),
                    flingBehavior = TransformingLazyColumnDefaults.snapFlingBehavior(state),
                ) {
                    items(100) { Box(Modifier.fillMaxWidth().height(itemHeight)) }
                }
            }
        }

        rule.onNodeWithTag(TEST_TAG).performTouchInput {
            swipeUp(startY = bottom, endY = top, durationMillis = 100)
        }
        rule.waitForIdle()

        assertThat(state.anchorItemIndex).isGreaterThan(initialIndex)
        assertThat(state.anchorItemScrollOffset).isEqualTo(0)
    }

    @OptIn(ExperimentalWearFoundationApi::class)
    @Test
    fun snapFlingBehavior_shortFastFlingInTallItem_movesForward() {
        assumeTrue(WearComposeFoundationFlags.isTransformingLazyColumnSnapSegmentsEnabled)
        lateinit var state: TransformingLazyColumnState
        val initialIndex = 10
        rule.setContent {
            WithTouchSlop(0f) {
                state = rememberTransformingLazyColumnState(initialAnchorItemIndex = initialIndex)
                TransformingLazyColumn(
                    state = state,
                    modifier = Modifier.size(200.dp).testTag(TEST_TAG),
                    flingBehavior = TransformingLazyColumnDefaults.snapFlingBehavior(state),
                ) {
                    // Each item covers the whole viewport, so it has several widely spaced snap
                    // points and is the only visible item.
                    items(100) { Box(Modifier.fillMaxWidth().height(600.dp)) }
                }
            }
        }
        val dragDistance = with(rule.density) { 20.dp.toPx() }
        val velocity = with(rule.density) { 1000.dp.toPx() }
        // Settle onto a snap point first.
        rule.onNodeWithTag(TEST_TAG).performTouchInput {
            swipeWithVelocity(
                start = center,
                end = center.copy(y = center.y - 1f),
                endVelocity = 0f,
            )
        }
        val (settledIndex, settledOffset) =
            rule.runOnIdle { state.anchorItemIndex to state.anchorItemScrollOffset }

        rule.onNodeWithTag(TEST_TAG).performTouchInput {
            swipeWithVelocity(
                start = center,
                end = center.copy(y = center.y - dragDistance),
                endVelocity = velocity,
            )
        }

        // A short but fast fling moves to the next snap point instead of snapping back. Scrolling
        // forward increases the scroll offset of the anchor item.
        rule.runOnIdle {
            assertWithMessage(
                    "from $settledIndex/$settledOffset to " +
                        "${state.anchorItemIndex}/${state.anchorItemScrollOffset}"
                )
                .that(
                    state.anchorItemIndex > settledIndex ||
                        (state.anchorItemIndex == settledIndex &&
                            state.anchorItemScrollOffset > settledOffset)
                )
                .isTrue()
        }
    }

    @OptIn(ExperimentalWearFoundationApi::class)
    @Test
    fun snapFlingBehavior_respectsSnapOffset() {
        lateinit var state: TransformingLazyColumnState
        val containerHeight = 200.dp
        val itemHeight = 50.dp
        val snapOffset = 20.dp
        val itemHeightPx = with(rule.density) { itemHeight.toPx() }
        val snapOffsetPx = with(rule.density) { snapOffset.roundToPx() }
        val initialIndex = 10
        rule.setContent {
            WithTouchSlop(0f) {
                state = rememberTransformingLazyColumnState(initialAnchorItemIndex = initialIndex)
                TransformingLazyColumn(
                    state = state,
                    modifier = Modifier.size(containerHeight).testTag(TEST_TAG),
                    flingBehavior =
                        TransformingLazyColumnDefaults.snapFlingBehavior(
                            state = state,
                            snapOffset = snapOffset,
                        ),
                ) {
                    items(100) { Box(Modifier.fillMaxWidth().height(itemHeight)) }
                }
            }
        }
        val dragDistance = itemHeightPx * 0.6f

        rule.onNodeWithTag(TEST_TAG).performTouchInput {
            swipeWithVelocity(
                start = center,
                end = center.copy(y = center.y - dragDistance),
                endVelocity = 0f,
            )
        }
        rule.waitForIdle()

        if (WearComposeFoundationFlags.isTransformingLazyColumnSnapSegmentsEnabled) {
            // The item nearest to the snap line (snapOffset above the center) is snapped: the
            // initial item, which starts at the center and is now 0.6 item heights above it, rather
            // than the next item, which is nearer to the center.
            rule.runOnIdle {
                assertThat(state.anchorItemIndex).isEqualTo(initialIndex)
                assertThat(state.anchorItemScrollOffset).isEqualTo(snapOffsetPx)
            }

            // From there, the same slow drag moves the next item nearer to the snap line than the
            // initial item: it snaps to the next item, at the snap offset.
            rule.onNodeWithTag(TEST_TAG).performTouchInput {
                swipeWithVelocity(
                    start = center,
                    end = center.copy(y = center.y - dragDistance),
                    endVelocity = 0f,
                )
            }
        }

        rule.runOnIdle {
            assertThat(state.anchorItemIndex).isEqualTo(initialIndex + 1)
            assertThat(state.anchorItemScrollOffset).isEqualTo(snapOffsetPx)
        }
    }

    @OptIn(ExperimentalTestApi::class)
    @Test
    fun rotarySnapBehavior_snapsToNextItem() {
        lateinit var state: TransformingLazyColumnState
        val containerHeight = 200.dp
        val itemHeight = 50.dp
        val itemHeightPx = with(rule.density) { itemHeight.toPx() }
        val initialIndex = 10
        rule.setContent {
            state = rememberTransformingLazyColumnState(initialAnchorItemIndex = initialIndex)
            TransformingLazyColumn(
                state = state,
                modifier = Modifier.size(containerHeight).testTag(TEST_TAG),
                rotaryScrollableBehavior = RotaryScrollableDefaults.snapBehavior(state),
                flingBehavior = TransformingLazyColumnDefaults.snapFlingBehavior(state),
            ) {
                items(100) { Box(Modifier.fillMaxWidth().height(itemHeight)) }
            }
        }

        rule.onNode(hasScrollAction()).performRotaryScrollInput {
            rotateToScrollVertically(itemHeightPx * 1.2f)
        }
        rule.waitForIdle()

        assertThat(state.anchorItemIndex).isEqualTo(initialIndex + 1)
        assertThat(state.anchorItemScrollOffset).isEqualTo(0)
    }

    @OptIn(ExperimentalTestApi::class)
    @Test
    fun rotarySnapBehavior_respectsSnapOffset() {
        lateinit var state: TransformingLazyColumnState
        val containerHeight = 200.dp
        val itemHeight = 50.dp
        val itemHeightPx = with(rule.density) { itemHeight.toPx() }
        val snapOffset = 20.dp
        val snapOffsetPx = with(rule.density) { snapOffset.roundToPx() }
        val initialIndex = 10

        rule.setContent {
            state = rememberTransformingLazyColumnState(initialAnchorItemIndex = initialIndex)
            TransformingLazyColumn(
                state = state,
                modifier = Modifier.size(containerHeight).testTag(TEST_TAG),
                rotaryScrollableBehavior =
                    RotaryScrollableDefaults.snapBehavior(
                        scrollableState = state,
                        snapOffset = snapOffset,
                    ),
                flingBehavior = TransformingLazyColumnDefaults.snapFlingBehavior(state),
            ) {
                items(100) { Box(Modifier.fillMaxWidth().height(itemHeight)) }
            }
        }

        rule.onNode(hasScrollAction()).performRotaryScrollInput {
            rotateToScrollVertically(itemHeightPx * 1.2f)
        }
        rule.waitForIdle()

        assertThat(state.anchorItemIndex).isEqualTo(initialIndex + 1)
        assertThat(state.anchorItemScrollOffset).isEqualTo(snapOffsetPx)
    }

    @OptIn(ExperimentalTestApi::class)
    @Test
    fun rotarySnapBehavior_tallTransformedItems_settle() {
        lateinit var state: TransformingLazyColumnState
        val snapPoints = setContentWithTallTransformedItems { state = it }

        repeat(8) {
            val start = rule.mainClock.currentTime
            rule.onNode(hasScrollAction()).performRotaryScrollInput {
                rotateToScrollVertically(stepRotationPx)
            }
            // A single snap takes well under a second; it must not keep re-aiming at an anchor
            // that the list can't reach exactly.
            rule.runOnIdle {
                assertWithMessage("Snap $it at item ${state.anchorItemIndex}")
                    .that(rule.mainClock.currentTime - start)
                    .isLessThan(2_000L)
            }
            assertRestsOnSnapPoint(snapPoints)
        }
    }

    @Test
    fun snapFlingBehavior_tallTransformedItems_landsOnSnapPoints() {
        val snapPoints = setContentWithTallTransformedItems()

        // The fling target is computed once, several items ahead of the transformed items.
        // Including flings below the minimum fling velocity, which snap to the nearest point.
        for (velocity in listOf(800, 1500, 3000, -1500, -800, 300, -300)) {
            rule.onNodeWithTag(TEST_TAG).performTouchInput {
                // In dp, long enough for the fastest fling at any density.
                val distance = with(rule.density) { 80.dp.toPx() } * velocity.sign
                swipeWithVelocity(
                    start = center,
                    end = center.copy(y = center.y - distance),
                    endVelocity = with(rule.density) { abs(velocity).dp.toPx() },
                )
            }
            rule.waitForIdle()
            assertRestsOnSnapPoint(snapPoints)
        }
    }

    @OptIn(ExperimentalTestApi::class)
    @Test
    fun snapBehaviors_tallItemsWithSnapOffset_restOnSnapPoints() {
        val snapOffset = 20.dp
        val snapPoints = setContentWithTallTransformedItems(snapOffset)

        repeat(6) {
            rule.onNode(hasScrollAction()).performRotaryScrollInput {
                rotateToScrollVertically(stepRotationPx)
            }
            rule.waitForIdle()
            assertRestsOnSnapPoint(snapPoints)
        }
        for (velocity in listOf(1500, -1500)) {
            rule.onNodeWithTag(TEST_TAG).performTouchInput {
                swipeWithVelocity(
                    start = center,
                    end =
                        center.copy(
                            y = center.y - with(rule.density) { 80.dp.toPx() } * velocity.sign
                        ),
                    endVelocity = with(rule.density) { abs(velocity).dp.toPx() },
                )
            }
            rule.waitForIdle()
            assertRestsOnSnapPoint(snapPoints)
        }
    }

    @OptIn(ExperimentalTestApi::class)
    @Test
    fun snapBehaviors_endOfTransformedItemsWithContentPadding_restOnLastItem() {
        lateinit var state: TransformingLazyColumnState
        val snapPoints =
            setContentWithTallTransformedItems(
                itemCount = 20,
                // Like an EdgeButton: enough for the last item to reach the center, and more.
                contentPadding = PaddingValues(bottom = 120.dp),
            ) {
                state = it
            }
        fun assertLastItemAtCenter() {
            assertRestsOnSnapPoint(snapPoints)
            rule.runOnIdle {
                val layoutInfo = state.layoutInfo
                val last = layoutInfo.visibleItems.last()
                assertThat(last.index).isEqualTo(19)
                val center = last.offset + last.transformedHeight / 2f
                assertThat(abs(center - layoutInfo.viewportSize.height / 2f)).isAtMost(2f)
            }
        }

        repeat(6) {
            rule.onNodeWithTag(TEST_TAG).performTouchInput { swipeUp() }
            rule.waitForIdle()
        }
        assertLastItemAtCenter()

        repeat(5) {
            rule.onNode(hasScrollAction()).performRotaryScrollInput {
                rotateToScrollVertically(-stepRotationPx)
            }
            rule.waitForIdle()
        }
        repeat(10) {
            rule.onNode(hasScrollAction()).performRotaryScrollInput {
                rotateToScrollVertically(stepRotationPx)
            }
            rule.waitForIdle()
        }
        assertLastItemAtCenter()
    }

    @OptIn(ExperimentalTestApi::class)
    @Test
    fun snapBehaviors_endOfTransformedItems_restAtEnd() {
        lateinit var state: TransformingLazyColumnState
        val snapPoints = setContentWithTallTransformedItems(itemCount = 20) { state = it }

        repeat(6) {
            rule.onNodeWithTag(TEST_TAG).performTouchInput { swipeUp() }
            rule.waitForIdle()
        }
        assertRestsOnSnapPoint(snapPoints)
        rule.runOnIdle { assertThat(state.canScrollForward).isFalse() }

        repeat(5) {
            rule.onNode(hasScrollAction()).performRotaryScrollInput {
                rotateToScrollVertically(-stepRotationPx)
            }
            rule.waitForIdle()
        }
        rule.runOnIdle { assertThat(state.canScrollForward).isTrue() }
        repeat(10) {
            rule.onNode(hasScrollAction()).performRotaryScrollInput {
                rotateToScrollVertically(stepRotationPx)
            }
            rule.waitForIdle()
        }
        assertRestsOnSnapPoint(snapPoints)
        rule.runOnIdle { assertThat(state.canScrollForward).isFalse() }
    }

    @Test
    fun snapFlingBehavior_collapsedPointsAtEnd_singleFastFlingReachesEnd() {
        lateinit var state: TransformingLazyColumnState
        val snapPoints = setContentNearShrunkEnd { state = it }
        rule.runOnIdle {
            assertThat(snapPoints.current().collapsedPointsBeyond(1)).isGreaterThan(0)
        }

        rule.onNodeWithTag(TEST_TAG).performTouchInput {
            swipeWithVelocity(
                start = center,
                end = center.copy(y = center.y - with(rule.density) { 80.dp.toPx() }),
                endVelocity = with(rule.density) { 2500.dp.toPx() },
            )
        }
        rule.waitForIdle()

        assertRestsOnSnapPoint(snapPoints)
        rule.runOnIdle { assertThat(state.canScrollForward).isFalse() }
    }

    @OptIn(ExperimentalTestApi::class)
    @Test
    fun rotarySnapBehavior_collapsedPointsAtEnd_fastTurnReachesEnd() {
        lateinit var state: TransformingLazyColumnState
        val snapPoints = setContentNearShrunkEnd { state = it }
        val stepPx = rule.runOnIdle {
            val current = snapPoints.current()
            assertThat(current.collapsedPointsBeyond(1)).isGreaterThan(0)
            current.stepSize()
        }

        rule.onNode(hasScrollAction()).performRotaryScrollInput {
            repeat(8) {
                if (it > 0) advanceEventTime(15L)
                rotateToScrollVertically(stepPx * 1.5f)
            }
        }
        rule.waitForIdle()

        assertRestsOnSnapPoint(snapPoints)
        rule.runOnIdle { assertThat(state.canScrollForward).isFalse() }
    }

    /**
     * A list laid out near its end with its last tall item visible and shrunk, so that several of
     * its snap points are collapsed into the edge snap point while it can still scroll there.
     */
    @OptIn(ExperimentalWearFoundationApi::class)
    private fun setContentNearShrunkEnd(
        onState: (TransformingLazyColumnState) -> Unit
    ): TransformingLazyColumnSnapPointsProvider {
        assumeTrue(WearComposeFoundationFlags.isTransformingLazyColumnSnapSegmentsEnabled)
        lateinit var snapPoints: TransformingLazyColumnSnapPointsProvider
        val segmentThresholdPx = with(rule.density) { 50.dp.roundToPx() }
        rule.setContent {
            val state = rememberTransformingLazyColumnState(initialAnchorItemIndex = 4)
            onState(state)
            snapPoints = rememberTransformingLazyColumnSnapPoints(state, snapOffsetPx = 0)
            TransformingLazyColumn(
                state = state,
                modifier = Modifier.size(200.dp).testTag(TEST_TAG),
                rotaryScrollableBehavior = RotaryScrollableDefaults.snapBehavior(state),
                flingBehavior = TransformingLazyColumnDefaults.snapFlingBehavior(state),
            ) {
                items(7) { index ->
                    val height = if (index == 6) 300.dp else 50.dp
                    Box(
                        Modifier.fillMaxWidth()
                            .height(height)
                            .snapSegmentThreshold { _, itemHeight ->
                                if (index == 6) segmentThresholdPx else itemHeight
                            }
                            .transformedHeight { itemHeight, progress ->
                                if (progress.isUnspecified) {
                                    itemHeight
                                } else {
                                    val overflow =
                                        maxOf(0f, 0.25f - progress.topOffsetFraction) +
                                            maxOf(0f, progress.bottomOffsetFraction - 0.75f)
                                    itemHeight -
                                        (itemHeight * (overflow * 0.3f).coerceAtMost(0.5f)).toInt()
                                }
                            }
                    )
                }
            }
        }
        return snapPoints
    }

    @OptIn(ExperimentalTestApi::class, ExperimentalWearFoundationApi::class)
    @Test
    fun rotarySnapBehavior_tallItem_restsOnSegmentCenters() {
        assumeTrue(WearComposeFoundationFlags.isTransformingLazyColumnSnapSegmentsEnabled)
        lateinit var state: TransformingLazyColumnState
        // Hand computed rather than from the snap points under test: an item three thresholds
        // tall has snap points one threshold above its center, at its center and one below.
        val viewportPx = with(rule.density) { 200.dp.roundToPx() }
        val thresholdPx = viewportPx / 4
        rule.setContent {
            state = rememberTransformingLazyColumnState(initialAnchorItemIndex = 3)
            TransformingLazyColumn(
                state = state,
                modifier = Modifier.size(200.dp).testTag(TEST_TAG),
                rotaryScrollableBehavior = RotaryScrollableDefaults.snapBehavior(state),
            ) {
                items(10) { index ->
                    // Away from the start of the list, so that every snap point can be reached.
                    val heightPx = if (index == 3) thresholdPx * 3 else thresholdPx
                    Box(
                        Modifier.fillMaxWidth()
                            .height(with(rule.density) { heightPx.toDp() })
                            .snapSegmentThreshold { _, _ -> thresholdPx }
                    )
                }
            }
        }
        // Offset of the center of the tall item below the center of the viewport.
        fun tallItemCenterOffset(): Float {
            rule.mainClock.advanceTimeBy(1_000L)
            return rule.runOnIdle {
                val item = state.layoutInfo.visibleItems.first { it.index == 3 }
                item.offset + item.transformedHeight / 2f - viewportPx / 2f
            }
        }
        fun turn(steps: Int) {
            rule.onNode(hasScrollAction()).performRotaryScrollInput {
                rotateToScrollVertically(steps * thresholdPx * 1.2f)
            }
            rule.waitForIdle()
        }

        assertThat(abs(tallItemCenterOffset())).isAtMost(2f)
        turn(-1)
        assertThat(abs(tallItemCenterOffset() - thresholdPx)).isAtMost(2f)
        turn(1)
        assertThat(abs(tallItemCenterOffset())).isAtMost(2f)
        turn(1)
        assertThat(abs(tallItemCenterOffset() + thresholdPx)).isAtMost(2f)
    }

    @OptIn(ExperimentalTestApi::class)
    @Test
    fun lowResSnapBehavior_tallItems_stepsOneSnapPointPerTurn() {
        lateinit var state: TransformingLazyColumnState
        lateinit var snapPoints: TransformingLazyColumnSnapPointsProvider
        rule.setContent {
            state = rememberTransformingLazyColumnState(initialAnchorItemIndex = 10)
            snapPoints = rememberTransformingLazyColumnSnapPoints(state, snapOffsetPx = 0)
            // The bezel behavior, without depending on the device having a bezel.
            val behavior = remember {
                LowResSnapRotaryScrollableBehavior(
                    rotaryHaptics = NoRotaryHaptics,
                    snapHandlerFactory = { TransformingLazyColumnSnapHandler(state, snapPoints) },
                )
            }
            TransformingLazyColumn(
                state = state,
                modifier = Modifier.size(200.dp).testTag(TEST_TAG),
                rotaryScrollableBehavior = behavior,
            ) {
                items(100) { index ->
                    val heightPx =
                        if (index % 3 == 2) transformedTallItemPx else transformedShortItemPx
                    Box(
                        Modifier.fillMaxWidth()
                            .height(with(rule.density) { heightPx.toDp() })
                            .snapSegmentThreshold(testSnapSegmentThreshold(heightPx))
                    )
                }
            }
        }
        assertTallItemsSplit(snapPoints) { it % 3 == 2 }

        repeat(8) {
            val (expected, nextKey) =
                rule.runOnIdle {
                    val points = snapPoints.current()
                    val i = points.points.indexOf(points.nearest)
                    val next = points.points.getOrNull(i + 1)
                    assertWithMessage("Snap point after ${points.nearest?.key}")
                        .that(next)
                        .isNotNull()
                    points.nearest!!.key to next!!.key
                }
            rule.onNode(hasScrollAction()).performRotaryScrollInput {
                rotateToScrollVertically(10f)
            }
            rule.waitForIdle()
            assertRestsOnSnapPoint(snapPoints)
            rule.runOnIdle {
                assertWithMessage("Turn $it from $expected")
                    .that(snapPoints.current().nearest!!.key)
                    .isEqualTo(nextKey)
            }
        }
    }

    private object NoRotaryHaptics : RotaryHapticHandler {
        override fun handleScrollHaptic(
            timestamp: Long,
            deltaInPixels: Float,
            inputDeviceId: Int,
            axis: Int,
        ) {}

        override fun handleSnapHaptic(
            timestamp: Long,
            deltaInPixels: Float,
            inputDeviceId: Int,
            axis: Int,
        ) {}

        override fun handleLimitHaptic(isStart: Boolean, inputDeviceId: Int, axis: Int) {}
    }

    private val transformedShortItemPx = 101
    private val transformedTallItemPx = 641

    /** Snap segment threshold of the tall test items: half of the 200.dp viewport. */
    private val tallItemThresholdPx: Int
        get() = with(rule.density) { 200.dp.roundToPx() } / 2

    /** A crown turn past the snap threshold of every step between the test items. */
    private val stepRotationPx: Float
        get() = tallItemThresholdPx * 1.2f

    /**
     * Splits the tall test items in at least 2 snap segments and keeps the short ones whole,
     * whatever the density and screen of the device, unlike the default threshold.
     */
    private fun testSnapSegmentThreshold(heightPx: Int): (Int, Int) -> Int =
        if (heightPx == transformedTallItemPx) {
            val thresholdPx = tallItemThresholdPx
            { _, _ -> thresholdPx }
        } else {
            { _, height -> height }
        }

    /**
     * Asserts that the laid out tall items, as told by [isTall] from their index, have several snap
     * points and the others one, so that the test steps through segments.
     */
    private fun assertTallItemsSplit(
        snapPoints: TransformingLazyColumnSnapPointsProvider,
        isTall: (Int) -> Boolean,
    ) {
        rule.runOnIdle {
            val counts = snapPoints.current().points.groupingBy { it.key.itemIndex }.eachCount()
            counts.forEach { (index, count) ->
                if (isTall(index)) {
                    assertWithMessage("Snap points of tall item $index").that(count).isAtLeast(2)
                } else {
                    assertWithMessage("Snap points of item $index").that(count).isEqualTo(1)
                }
            }
        }
    }

    /**
     * Short and tall items that shrink at the edges of the screen, like the Material3 items, with
     * snap behaviors at [snapOffset]. Returns the snap points they snap to.
     */
    @OptIn(ExperimentalWearFoundationApi::class)
    private fun setContentWithTallTransformedItems(
        snapOffset: Dp = 0.dp,
        itemCount: Int = 100,
        contentPadding: PaddingValues = PaddingValues(),
        onState: (TransformingLazyColumnState) -> Unit = {},
    ): TransformingLazyColumnSnapPointsProvider {
        assumeTrue(WearComposeFoundationFlags.isTransformingLazyColumnSnapSegmentsEnabled)
        lateinit var snapPoints: TransformingLazyColumnSnapPointsProvider
        rule.setContent {
            val state = rememberTransformingLazyColumnState(initialAnchorItemIndex = 10)
            onState(state)
            val snapOffsetPx = with(rule.density) { snapOffset.roundToPx() }
            snapPoints = rememberTransformingLazyColumnSnapPoints(state, snapOffsetPx)
            TransformingLazyColumn(
                state = state,
                modifier = Modifier.size(200.dp).testTag(TEST_TAG),
                contentPadding = contentPadding,
                rotaryScrollableBehavior =
                    RotaryScrollableDefaults.snapBehavior(state, snapOffset = snapOffset),
                flingBehavior =
                    TransformingLazyColumnDefaults.snapFlingBehavior(
                        state,
                        snapOffset = snapOffset,
                    ),
            ) {
                items(itemCount) { index ->
                    // Odd heights put item and segment centers between two pixels.
                    val heightPx =
                        if (index % 3 == 2) transformedTallItemPx else transformedShortItemPx
                    Box(
                        Modifier.fillMaxWidth()
                            .height(with(rule.density) { heightPx.toDp() })
                            .snapSegmentThreshold(testSnapSegmentThreshold(heightPx))
                            .transformedHeight { height, progress ->
                                if (progress.isUnspecified) {
                                    height
                                } else {
                                    val overflow =
                                        maxOf(0f, 0.25f - progress.topOffsetFraction) +
                                            maxOf(0f, progress.bottomOffsetFraction - 0.75f)
                                    height - (overflow * height * 0.3f).toInt()
                                }
                            }
                    )
                }
            }
        }
        assertTallItemsSplit(snapPoints) { it % 3 == 2 }
        return snapPoints
    }

    @OptIn(ExperimentalTestApi::class, ExperimentalWearFoundationApi::class)
    @Test
    fun rotarySnapBehavior_turnAgainstRunningSnap_cancelsIt() {
        assumeTrue(WearComposeFoundationFlags.isTransformingLazyColumnSnapSegmentsEnabled)
        lateinit var state: TransformingLazyColumnState
        lateinit var snapPoints: TransformingLazyColumnSnapPointsProvider
        val itemPx = 101
        rule.setContent {
            state = rememberTransformingLazyColumnState(initialAnchorItemIndex = 10)
            snapPoints = rememberTransformingLazyColumnSnapPoints(state, snapOffsetPx = 0)
            TransformingLazyColumn(
                state = state,
                modifier = Modifier.size(200.dp).testTag(TEST_TAG),
                rotaryScrollableBehavior = RotaryScrollableDefaults.snapBehavior(state),
            ) {
                items(100) {
                    Box(Modifier.fillMaxWidth().height(with(rule.density) { itemPx.toDp() }))
                }
            }
        }
        val (initialIndex, initialOffset) =
            rule.runOnIdle { state.anchorItemIndex to state.anchorItemScrollOffset }

        rule.onNode(hasScrollAction()).performRotaryScrollInput {
            // Starts a snap to the next item...
            rotateToScrollVertically(itemPx * 1.2f)
            // ...and turns back, within the same gesture, early in the snap.
            advanceEventTime(50L)
            rotateToScrollVertically(-10f)
        }
        rule.waitForIdle()
        assertRestsOnSnapPoint(snapPoints)

        // The snap was cancelled: the list settles back on the item it started from instead of
        // completing the snap to the next one, within a pixel of where it started, as the initial
        // layout can round the center of the item to either pixel.
        rule.runOnIdle {
            assertThat(state.anchorItemIndex).isEqualTo(initialIndex)
            assertThat(abs(state.anchorItemScrollOffset - initialOffset)).isAtMost(1)
        }
    }

    @OptIn(ExperimentalTestApi::class)
    @Test
    fun rotarySnapBehavior_itemKeptWholeTallerThanViewport_movesToNextItem() {
        lateinit var state: TransformingLazyColumnState
        setContentWithItemsKeptWhole { state = it }
        val initialIndex = rule.runOnIdle { state.anchorItemIndex }

        rule.onNode(hasScrollAction()).performRotaryScrollInput {
            // Past the snap threshold, derived from the item size as with item based snapping.
            rotateToScrollVertically(wholeItemPx * 1.5f)
        }

        // The item is the only visible one and has a single snap point: the crown still moves on.
        rule.runOnIdle { assertThat(state.anchorItemIndex).isGreaterThan(initialIndex) }
    }

    @Test
    fun snapFlingBehavior_itemKeptWholeTallerThanViewport_flingsToNextItems() {
        lateinit var state: TransformingLazyColumnState
        setContentWithItemsKeptWhole { state = it }
        val initialIndex = rule.runOnIdle { state.anchorItemIndex }

        rule.onNodeWithTag(TEST_TAG).performTouchInput {
            swipeWithVelocity(
                start = center,
                end = center.copy(y = center.y - 150f),
                endVelocity = with(rule.density) { 2000.dp.toPx() },
            )
        }

        // A fast fling decays past the item instead of snapping back to it.
        rule.runOnIdle { assertThat(state.anchorItemIndex).isGreaterThan(initialIndex) }
    }

    private val wholeItemPx = 800

    /** Items twice as tall as the viewport, kept whole with snapSegmentThreshold. */
    @OptIn(ExperimentalWearFoundationApi::class)
    private fun setContentWithItemsKeptWhole(onState: (TransformingLazyColumnState) -> Unit) {
        assumeTrue(WearComposeFoundationFlags.isTransformingLazyColumnSnapSegmentsEnabled)
        rule.setContent {
            val state = rememberTransformingLazyColumnState(initialAnchorItemIndex = 10)
            onState(state)
            TransformingLazyColumn(
                state = state,
                modifier =
                    Modifier.size(with(rule.density) { (wholeItemPx / 2).toDp() })
                        .testTag(TEST_TAG),
                rotaryScrollableBehavior = RotaryScrollableDefaults.snapBehavior(state),
                flingBehavior = TransformingLazyColumnDefaults.snapFlingBehavior(state),
            ) {
                items(100) {
                    Box(
                        Modifier.fillMaxWidth()
                            .height(with(rule.density) { wholeItemPx.toDp() })
                            .snapSegmentThreshold { _, height -> height }
                    )
                }
            }
        }
    }

    @OptIn(ExperimentalTestApi::class, ExperimentalWearFoundationApi::class)
    @Test
    fun snapBehaviors_endOfListWithContentPadding_restOnLastItem() {
        assumeTrue(WearComposeFoundationFlags.isTransformingLazyColumnSnapSegmentsEnabled)
        lateinit var state: TransformingLazyColumnState
        rule.setContent {
            state = rememberTransformingLazyColumnState()
            TransformingLazyColumn(
                state = state,
                modifier = Modifier.size(200.dp).testTag(TEST_TAG),
                // Like an EdgeButton: enough for the last item to reach the center, and more.
                contentPadding = PaddingValues(bottom = 100.dp),
                rotaryScrollableBehavior = RotaryScrollableDefaults.snapBehavior(state),
                flingBehavior = TransformingLazyColumnDefaults.snapFlingBehavior(state),
            ) {
                items(20) { Box(Modifier.fillMaxWidth().height(50.dp)) }
            }
        }

        repeat(4) {
            rule.onNodeWithTag(TEST_TAG).performTouchInput { swipeUp() }
            rule.waitForIdle()
        }
        // The last item rests at the center rather than the list on its content padding.
        rule.runOnIdle {
            assertThat(state.anchorItemIndex).isEqualTo(19)
            assertThat(state.anchorItemScrollOffset).isEqualTo(0)
            assertThat(state.canScrollForward).isTrue()
        }

        repeat(3) {
            rule.onNode(hasScrollAction()).performRotaryScrollInput {
                rotateToScrollVertically(500f)
            }
            rule.waitForIdle()
        }
        rule.runOnIdle {
            assertThat(state.anchorItemIndex).isEqualTo(19)
            assertThat(state.anchorItemScrollOffset).isEqualTo(0)
        }
    }

    @Test
    fun snapFlingBehavior_fastFlingToPaddedEnd_animatesBackToLastSnapPoint() {
        lateinit var state: TransformingLazyColumnState
        val snapPoints =
            setContentWithTallTransformedItems(
                itemCount = 12,
                contentPadding = PaddingValues(bottom = 120.dp),
            ) {
                state = it
            }

        rule.mainClock.autoAdvance = false
        try {
            rule.onNodeWithTag(TEST_TAG).performTouchInput {
                swipeWithVelocity(
                    start = center,
                    end = center.copy(y = center.y - with(rule.density) { 80.dp.toPx() }),
                    endVelocity = with(rule.density) { 4000.dp.toPx() },
                )
            }
            var reachedScrollBound = false
            for (frame in 0 until 120) {
                rule.mainClock.advanceTimeByFrame()
                if (rule.runOnIdle { !state.canScrollForward }) {
                    reachedScrollBound = true
                    break
                }
            }
            assertThat(reachedScrollBound).isTrue()

            val distanceAtStop = rule.runOnIdle {
                val points = snapPoints.current()
                abs(points.distanceTo(points.nearest!!))
            }
            assertThat(distanceAtStop).isGreaterThan(with(rule.density) { 40.dp.toPx() })

            // Settling off content padding animates over multiple frames instead of jumping at
            // once.
            rule.mainClock.advanceTimeByFrame()
            val distanceAfterOneFrame = rule.runOnIdle {
                val points = snapPoints.current()
                abs(points.distanceTo(points.nearest!!))
            }
            assertThat(distanceAfterOneFrame).isGreaterThan(with(rule.density) { 10.dp.toPx() })
        } finally {
            rule.mainClock.autoAdvance = true
        }
        rule.waitForIdle()
        assertRestsOnSnapPoint(snapPoints)
    }

    @Test
    fun snapFlingBehavior_fastFlingToUnpaddedEnd_reportsRemainingVelocity() {
        lateinit var state: TransformingLazyColumnState
        var postFlingAvailableY = 0f
        val connection =
            object : NestedScrollConnection {
                override suspend fun onPostFling(
                    consumed: Velocity,
                    available: Velocity,
                ): Velocity {
                    postFlingAvailableY = available.y
                    return Velocity.Zero
                }
            }
        rule.setContent {
            state = rememberTransformingLazyColumnState(initialAnchorItemIndex = 10)
            TransformingLazyColumn(
                state = state,
                modifier = Modifier.nestedScroll(connection).size(200.dp).testTag(TEST_TAG),
                flingBehavior = TransformingLazyColumnDefaults.snapFlingBehavior(state),
            ) {
                items(12) { Box(Modifier.fillMaxWidth().height(50.dp)) }
            }
        }

        rule.onNodeWithTag(TEST_TAG).performTouchInput {
            swipeWithVelocity(
                start = center,
                end = center.copy(y = center.y - with(rule.density) { 80.dp.toPx() }),
                endVelocity = with(rule.density) { 4000.dp.toPx() },
            )
        }
        rule.waitForIdle()

        rule.runOnIdle {
            assertThat(state.canScrollForward).isFalse()
            assertThat(postFlingAvailableY).isLessThan(0f)
        }
    }

    @OptIn(ExperimentalTestApi::class)
    @Test
    fun rotarySnapBehavior_stepAddedWhileFinishingSnap_isNotDropped() {
        lateinit var state: TransformingLazyColumnState
        lateinit var snapPoints: TransformingLazyColumnSnapPointsProvider
        val initialIndex = 10
        var pausedInPostFling = false
        var pauseOnce = true
        val connection =
            object : NestedScrollConnection {
                override suspend fun onPostFling(
                    consumed: Velocity,
                    available: Velocity,
                ): Velocity {
                    if (pauseOnce) {
                        pauseOnce = false
                        pausedInPostFling = true
                        withFrameNanos {}
                    }
                    return Velocity.Zero
                }
            }
        rule.setContent {
            state = rememberTransformingLazyColumnState(initialAnchorItemIndex = initialIndex)
            snapPoints = rememberTransformingLazyColumnSnapPoints(state, snapOffsetPx = 0)
            val behavior = remember {
                LowResSnapRotaryScrollableBehavior(
                    rotaryHaptics = NoRotaryHaptics,
                    snapHandlerFactory = { TransformingLazyColumnSnapHandler(state, snapPoints) },
                )
            }
            TransformingLazyColumn(
                state = state,
                modifier = Modifier.nestedScroll(connection).size(200.dp).testTag(TEST_TAG),
                rotaryScrollableBehavior = behavior,
            ) {
                items(100) { Box(Modifier.fillMaxWidth().height(50.dp)) }
            }
        }

        rule.mainClock.autoAdvance = false
        try {
            rule.onNode(hasScrollAction()).performRotaryScrollInput {
                rotateToScrollVertically(10f)
            }
            var lastEventTime = rule.mainClock.currentTime
            for (frame in 0 until 200) {
                rule.mainClock.advanceTimeByFrame()
                if (rule.runOnIdle { pausedInPostFling }) {
                    break
                }
                if (rule.mainClock.currentTime - lastEventTime >= 100L) {
                    // Sub-threshold rotary input keeps the gesture active across the spring.
                    rule.onNode(hasScrollAction()).performRotaryScrollInput {
                        rotateToScrollVertically(0.01f)
                    }
                    lastEventTime = rule.mainClock.currentTime
                }
            }
            rule.runOnIdle { assertThat(pausedInPostFling).isTrue() }

            // springToTarget has already returned while snapJob is still active inside fling(0f):
            // a sequential step arriving now must still be animated.
            rule.onNode(hasScrollAction()).performRotaryScrollInput {
                rotateToScrollVertically(10f)
            }
        } finally {
            rule.mainClock.autoAdvance = true
        }
        rule.waitForIdle()

        assertRestsOnSnapPoint(snapPoints)
        rule.runOnIdle { assertThat(state.anchorItemIndex).isEqualTo(initialIndex + 2) }
    }

    @OptIn(ExperimentalTestApi::class, ExperimentalWearFoundationApi::class)
    @Test
    fun snapBehaviors_reverseLayout_reachBothEndsOnSnapPoints() {
        assumeTrue(WearComposeFoundationFlags.isTransformingLazyColumnSnapSegmentsEnabled)
        lateinit var state: TransformingLazyColumnState
        lateinit var snapPoints: TransformingLazyColumnSnapPointsProvider
        rule.setContent {
            state = rememberTransformingLazyColumnState(initialAnchorItemIndex = 10)
            snapPoints = rememberTransformingLazyColumnSnapPoints(state, snapOffsetPx = 0)
            TransformingLazyColumn(
                state = state,
                modifier = Modifier.size(200.dp).testTag(TEST_TAG),
                reverseLayout = true,
                rotaryScrollableBehavior = RotaryScrollableDefaults.snapBehavior(state),
                flingBehavior = TransformingLazyColumnDefaults.snapFlingBehavior(state),
            ) {
                items(20) { Box(Modifier.fillMaxWidth().height(50.dp)) }
            }
        }
        // Which ends were reached: true for the end of the list, false for its start.
        val reached = mutableSetOf<Boolean>()
        fun assertAtAnEnd() {
            assertRestsOnSnapPoint(snapPoints)
            rule.runOnIdle {
                val atStart = !state.canScrollBackward
                val atEnd = !state.canScrollForward
                assertWithMessage("At item ${state.anchorItemIndex}")
                    .that(atStart || atEnd)
                    .isTrue()
                reached += atEnd
            }
        }

        // Flings to one end.
        repeat(4) {
            rule.onNodeWithTag(TEST_TAG).performTouchInput { swipeUp() }
            rule.waitForIdle()
        }
        assertAtAnEnd()

        // The crown, both ways, to both ends.
        for (delta in listOf(150f, -150f)) {
            repeat(30) {
                rule.onNode(hasScrollAction()).performRotaryScrollInput {
                    rotateToScrollVertically(delta)
                }
                rule.waitForIdle()
            }
            assertAtAnEnd()
        }
        assertThat(reached).containsExactly(true, false)
    }

    @Test
    fun snapSegmentThreshold_withAnimateItem_appliesInEitherOrder() {
        lateinit var snapPoints: TransformingLazyColumnSnapPointsProvider
        val itemHeight = with(rule.density) { 300.toDp() }
        rule.setContent {
            val state = rememberTransformingLazyColumnState(initialAnchorItemIndex = 1)
            snapPoints = rememberTransformingLazyColumnSnapPoints(state, snapOffsetPx = 0)
            TransformingLazyColumn(state = state, modifier = Modifier.size(200.dp)) {
                item { Box(Modifier.fillMaxWidth().height(200.dp)) }
                item {
                    Box(
                        Modifier.fillMaxWidth()
                            .height(itemHeight)
                            .snapSegmentThreshold { _, _ -> 100 }
                            .animateItem()
                    )
                }
                item {
                    Box(
                        Modifier.fillMaxWidth()
                            .height(itemHeight)
                            .animateItem()
                            .snapSegmentThreshold { _, _ -> 100 }
                    )
                }
                items(10) { Box(Modifier.fillMaxWidth().height(200.dp)) }
            }
        }
        rule.runOnIdle {
            val points = snapPoints.current().points
            // 300 px in segments of at most 100 px, whichever modifier comes first.
            assertThat(points.count { it.key.itemIndex == 1 }).isEqualTo(3)
            assertThat(points.count { it.key.itemIndex == 2 }).isEqualTo(3)
        }
    }

    @Test
    fun snapSegmentThreshold_withMinimumVerticalContentPadding_appliesInEitherOrder() {
        lateinit var snapPoints: TransformingLazyColumnSnapPointsProvider
        val itemHeight = with(rule.density) { 300.toDp() }
        rule.setContent {
            val state = rememberTransformingLazyColumnState(initialAnchorItemIndex = 1)
            snapPoints = rememberTransformingLazyColumnSnapPoints(state, snapOffsetPx = 0)
            TransformingLazyColumn(state = state, modifier = Modifier.size(200.dp)) {
                item { Box(Modifier.fillMaxWidth().height(200.dp)) }
                item {
                    Box(
                        Modifier.fillMaxWidth()
                            .height(itemHeight)
                            .snapSegmentThreshold { _, _ -> 100 }
                            .minimumVerticalContentPadding(4.dp)
                    )
                }
                item {
                    Box(
                        Modifier.fillMaxWidth()
                            .height(itemHeight)
                            .minimumVerticalContentPadding(4.dp)
                            .snapSegmentThreshold { _, _ -> 100 }
                    )
                }
                items(10) { Box(Modifier.fillMaxWidth().height(200.dp)) }
            }
        }
        rule.runOnIdle {
            val points = snapPoints.current().points
            // 300 px in segments of at most 100 px, whichever modifier comes first.
            assertThat(points.count { it.key.itemIndex == 1 }).isEqualTo(3)
            assertThat(points.count { it.key.itemIndex == 2 }).isEqualTo(3)
        }
    }

    @Test
    fun snapSegmentThreshold_withTransformedHeight_appliesInEitherOrder() {
        lateinit var state: TransformingLazyColumnState
        lateinit var snapPoints: TransformingLazyColumnSnapPointsProvider
        val itemHeight = with(rule.density) { 300.toDp() }
        rule.setContent {
            state = rememberTransformingLazyColumnState(initialAnchorItemIndex = 1)
            snapPoints = rememberTransformingLazyColumnSnapPoints(state, snapOffsetPx = 0)
            TransformingLazyColumn(state = state, modifier = Modifier.size(200.dp)) {
                item { Box(Modifier.fillMaxWidth().height(200.dp)) }
                item {
                    Box(
                        Modifier.fillMaxWidth()
                            .height(itemHeight)
                            .snapSegmentThreshold { _, _ -> 100 }
                            .transformedHeight { height, _ -> height - 10 }
                    )
                }
                item {
                    Box(
                        Modifier.fillMaxWidth()
                            .height(itemHeight)
                            .transformedHeight { height, _ -> height - 10 }
                            .snapSegmentThreshold { _, _ -> 100 }
                    )
                }
                items(10) { Box(Modifier.fillMaxWidth().height(200.dp)) }
            }
        }

        rule.runOnIdle {
            val points = snapPoints.current().points
            // 300 px in segments of at most 100 px, whichever modifier comes first...
            assertThat(points.count { it.key.itemIndex == 1 }).isEqualTo(3)
            assertThat(points.count { it.key.itemIndex == 2 }).isEqualTo(3)
            // ...and the height is still transformed.
            state.layoutInfo.visibleItems
                .filter { it.index == 1 || it.index == 2 }
                .forEach { assertThat(it.transformedHeight).isEqualTo(290) }
        }
    }

    @Test
    fun minimumVerticalContentPadding_withSnapSegmentThreshold_appliesInEitherOrder() {
        lateinit var state: TransformingLazyColumnState
        var thresholdFirst by mutableStateOf(true)
        rule.setContent {
            state = rememberTransformingLazyColumnState()
            TransformingLazyColumn(state = state, modifier = Modifier.size(200.dp)) {
                item {
                    val threshold = Modifier.snapSegmentThreshold { _, _ -> 100 }
                    val padding = Modifier.minimumVerticalContentPadding(24.dp)
                    Box(
                        Modifier.fillMaxWidth()
                            .height(50.dp)
                            .then(
                                if (thresholdFirst) threshold.then(padding)
                                else padding.then(threshold)
                            )
                    )
                }
                items(10) { Box(Modifier.fillMaxWidth().height(50.dp)) }
            }
        }
        val paddingPx = with(rule.density) { 24.dp.roundToPx() }

        rule.runOnIdle { assertThat(state.layoutInfo.beforeContentPadding).isEqualTo(paddingPx) }
        thresholdFirst = false
        rule.runOnIdle { assertThat(state.layoutInfo.beforeContentPadding).isEqualTo(paddingPx) }
    }

    @Test
    fun snapSegmentThreshold_changed_updatesSegments() {
        lateinit var snapPoints: TransformingLazyColumnSnapPointsProvider
        var threshold by mutableStateOf(100)
        var itemCompositions = 0
        val itemHeight = with(rule.density) { 300.toDp() }
        rule.setContent {
            val state = rememberTransformingLazyColumnState(initialAnchorItemIndex = 1)
            snapPoints = rememberTransformingLazyColumnSnapPoints(state, snapOffsetPx = 0)
            TransformingLazyColumn(state = state, modifier = Modifier.size(200.dp)) {
                item { Box(Modifier.fillMaxWidth().height(200.dp)) }
                item {
                    itemCompositions++
                    Box(
                        Modifier.fillMaxWidth().height(itemHeight).snapSegmentThreshold { _, _ ->
                            threshold
                        }
                    )
                }
                items(10) { Box(Modifier.fillMaxWidth().height(200.dp)) }
            }
        }
        rule.runOnIdle {
            assertThat(snapPoints.current().points.count { it.key.itemIndex == 1 }).isEqualTo(3)
            assertThat(itemCompositions).isEqualTo(1)
        }

        threshold = 1000

        rule.runOnIdle {
            assertThat(snapPoints.current().points.count { it.key.itemIndex == 1 }).isEqualTo(1)
            assertThat(itemCompositions).isEqualTo(1)
        }
    }

    @Test
    fun snapSegmentThreshold_lambdaInstanceChanged_updatesSegments() {
        lateinit var snapPoints: TransformingLazyColumnSnapPointsProvider
        var keepWhole by mutableStateOf(false)
        val itemHeight = with(rule.density) { 300.toDp() }
        rule.setContent {
            val state = rememberTransformingLazyColumnState(initialAnchorItemIndex = 1)
            snapPoints = rememberTransformingLazyColumnSnapPoints(state, snapOffsetPx = 0)
            TransformingLazyColumn(state = state, modifier = Modifier.size(200.dp)) {
                item { Box(Modifier.fillMaxWidth().height(200.dp)) }
                item {
                    val thresholdLambda: (Int, Int) -> Int =
                        if (keepWhole) {
                            { _, height -> height }
                        } else {
                            { _, _ -> 100 }
                        }
                    Box(
                        Modifier.fillMaxWidth()
                            .height(itemHeight)
                            .snapSegmentThreshold(thresholdLambda)
                    )
                }
                items(10) { Box(Modifier.fillMaxWidth().height(200.dp)) }
            }
        }
        rule.runOnIdle {
            assertThat(snapPoints.current().points.count { it.key.itemIndex == 1 }).isEqualTo(3)
        }

        keepWhole = true

        rule.runOnIdle {
            assertThat(snapPoints.current().points.count { it.key.itemIndex == 1 }).isEqualTo(1)
        }
    }

    @OptIn(ExperimentalWearFoundationApi::class)
    @Test
    fun snapSegmentThreshold_segmentsDisabled_lambdaNotInvoked() {
        WearComposeFoundationFlags.isTransformingLazyColumnSnapSegmentsEnabled = false
        var thresholdCalls = 0
        rule.setContent {
            TransformingLazyColumn(modifier = Modifier.size(200.dp)) {
                items(5) {
                    Box(
                        Modifier.fillMaxWidth().height(300.dp).snapSegmentThreshold { _, _ ->
                            thresholdCalls++
                            100
                        }
                    )
                }
            }
        }
        rule.runOnIdle { assertThat(thresholdCalls).isEqualTo(0) }
    }

    @OptIn(ExperimentalTestApi::class)
    @Test
    fun snapBehaviors_contentShorterThanViewport_doNotMove() {
        lateinit var state: TransformingLazyColumnState
        rule.setContent {
            state = rememberTransformingLazyColumnState()
            TransformingLazyColumn(
                state = state,
                modifier = Modifier.size(200.dp).testTag(TEST_TAG),
                rotaryScrollableBehavior = RotaryScrollableDefaults.snapBehavior(state),
                flingBehavior = TransformingLazyColumnDefaults.snapFlingBehavior(state),
            ) {
                items(2) { Box(Modifier.fillMaxWidth().height(40.dp)) }
            }
        }
        val (index, offset) =
            rule.runOnIdle { state.anchorItemIndex to state.anchorItemScrollOffset }

        rule.onNodeWithTag(TEST_TAG).performTouchInput { swipeUp() }
        rule.waitForIdle()
        rule.onNode(hasScrollAction()).performRotaryScrollInput { rotateToScrollVertically(300f) }
        rule.mainClock.advanceTimeBy(1_000L)

        rule.runOnIdle {
            assertThat(state.anchorItemIndex).isEqualTo(index)
            assertThat(state.anchorItemScrollOffset).isEqualTo(offset)
        }
    }

    @OptIn(ExperimentalTestApi::class)
    @Test
    fun rotarySnapBehavior_contentShorterThanViewport_noSnapPointsHapticsOrScroll() {
        lateinit var state: TransformingLazyColumnState
        lateinit var snapPoints: TransformingLazyColumnSnapPointsProvider
        var snapHaptics = 0
        var scrollStarts = 0
        val haptics =
            object : RotaryHapticHandler by NoRotaryHaptics {
                override fun handleSnapHaptic(
                    timestamp: Long,
                    deltaInPixels: Float,
                    inputDeviceId: Int,
                    axis: Int,
                ) {
                    snapHaptics++
                }
            }
        rule.setContent {
            state = rememberTransformingLazyColumnState()
            snapPoints = rememberTransformingLazyColumnSnapPoints(state, snapOffsetPx = 0)
            val behavior = remember { highResSnapBehavior(state, snapPoints, haptics) }
            LaunchedEffect(state) {
                snapshotFlow { state.isScrollInProgress }.collect { if (it) scrollStarts++ }
            }
            TransformingLazyColumn(
                state = state,
                modifier = Modifier.size(200.dp).testTag(TEST_TAG),
                rotaryScrollableBehavior = behavior,
            ) {
                items(2) { Box(Modifier.fillMaxWidth().height(40.dp)) }
            }
        }
        rule.runOnIdle { assertThat(snapPoints.current().points).isEmpty() }

        // Turns well past a snap threshold both ways, and short of it.
        listOf(300f, -300f, 5f).forEach { delta ->
            rule.onNode(hasScrollAction()).performRotaryScrollInput {
                rotateToScrollVertically(delta)
            }
            rule.mainClock.advanceTimeBy(1_000L)
        }

        rule.runOnIdle {
            assertThat(snapHaptics).isEqualTo(0)
            assertThat(scrollStarts).isEqualTo(0)
        }
    }

    /** Asserts that the list rests with its nearest snap point on the snap line. */
    private fun assertRestsOnSnapPoint(snapPoints: TransformingLazyColumnSnapPointsProvider) {
        // Past the delay before a scroll short of the snap threshold settles back.
        rule.mainClock.advanceTimeBy(1_000L)
        rule.runOnIdle {
            val points = snapPoints.current()
            val nearest = points.nearest
            assertThat(nearest).isNotNull()
            assertThat(abs(points.distanceTo(nearest!!))).isAtMost(2f)
        }
    }

    @OptIn(ExperimentalTestApi::class, ExperimentalWearFoundationApi::class)
    @Test
    fun rotarySnapBehavior_reverseLayout_stepsThroughTallItems() {
        assumeTrue(WearComposeFoundationFlags.isTransformingLazyColumnSnapSegmentsEnabled)
        lateinit var state: TransformingLazyColumnState
        lateinit var snapPoints: TransformingLazyColumnSnapPointsProvider
        val tallItemPx = transformedTallItemPx
        val initialIndex = 10

        rule.setContent {
            state = rememberTransformingLazyColumnState(initialAnchorItemIndex = initialIndex)
            snapPoints = rememberTransformingLazyColumnSnapPoints(state, snapOffsetPx = 0)
            TransformingLazyColumn(
                state = state,
                modifier = Modifier.size(200.dp).testTag(TEST_TAG),
                reverseLayout = true,
                rotaryScrollableBehavior = RotaryScrollableDefaults.snapBehavior(state),
                flingBehavior = TransformingLazyColumnDefaults.snapFlingBehavior(state),
            ) {
                items(100) {
                    Box(
                        Modifier.fillMaxWidth()
                            .height(with(rule.density) { tallItemPx.toDp() })
                            .snapSegmentThreshold(testSnapSegmentThreshold(tallItemPx))
                    )
                }
            }
        }
        assertTallItemsSplit(snapPoints) { true }

        // Each turn advances by one snap segment: the list moves in a single direction and never
        // skips a whole tall item, which has several segments.
        val positions =
            mutableListOf(rule.runOnIdle { state.anchorItemIndex to state.anchorItemScrollOffset })
        repeat(6) {
            val start = rule.mainClock.currentTime
            rule.onNode(hasScrollAction()).performRotaryScrollInput {
                rotateToScrollVertically(tallItemPx / 2f)
            }
            positions += rule.runOnIdle {
                assertWithMessage("Snap $it settles")
                    .that(rule.mainClock.currentTime - start)
                    .isLessThan(2_000L)
                state.anchorItemIndex to state.anchorItemScrollOffset
            }
        }
        val steps = positions.zipWithNext { (i1, o1), (i2, o2) ->
            if (i1 != i2) (i2 - i1).sign else (o2 - o1).sign
        }
        assertWithMessage("Positions $positions").that(steps.distinct()).hasSize(1)
        assertWithMessage("Positions $positions").that(steps.first()).isNotEqualTo(0)
        positions.zipWithNext { (i1, _), (i2, _) ->
            assertWithMessage("Positions $positions").that(abs(i2 - i1)).isAtMost(1)
        }
    }

    @OptIn(ExperimentalTestApi::class, ExperimentalWearFoundationApi::class)
    @Test
    fun snapBehaviors_snapSegmentsFlag_switchesBetweenItemAndSegmentSnapping() {
        lateinit var state: TransformingLazyColumnState
        val itemHeight = 300.dp
        val snapOffset = 16.dp
        val itemHeightPx = with(rule.density) { itemHeight.toPx() }
        val snapOffsetPx = with(rule.density) { snapOffset.roundToPx() }
        // Divides the 300.dp item into 5 equal 60.dp segments, with segment 2 at the item center.
        val segmentThresholdPx = with(rule.density) { 60.dp.roundToPx() }
        val initialIndex = 5
        var flagEnabled by mutableStateOf(false)
        WearComposeFoundationFlags.isTransformingLazyColumnSnapSegmentsEnabled = false

        rule.setContent {
            key(flagEnabled) {
                WithTouchSlop(0f) {
                    state =
                        rememberTransformingLazyColumnState(
                            initialAnchorItemIndex = initialIndex,
                            initialAnchorItemScrollOffset = snapOffsetPx,
                        )
                    TransformingLazyColumn(
                        state = state,
                        modifier = Modifier.size(200.dp).testTag(TEST_TAG),
                        rotaryScrollableBehavior =
                            RotaryScrollableDefaults.snapBehavior(state, snapOffset = snapOffset),
                        flingBehavior =
                            TransformingLazyColumnDefaults.snapFlingBehavior(
                                state,
                                snapOffset = snapOffset,
                            ),
                    ) {
                        items(20) {
                            Box(
                                Modifier.fillMaxWidth().height(itemHeight).snapSegmentThreshold {
                                    _,
                                    _ ->
                                    segmentThresholdPx
                                }
                            )
                        }
                    }
                }
            }
        }

        // With segment snapping disabled, one rotary step skips the entire tall item onto the next
        // item's center (plus snapOffset) instead of stopping on an interior segment.
        rule.onNode(hasScrollAction()).performRotaryScrollInput {
            rotateToScrollVertically(itemHeightPx * 1.2f)
        }
        rule.runOnIdle {
            assertThat(state.anchorItemIndex).isEqualTo(initialIndex + 1)
            assertThat(abs(state.anchorItemScrollOffset - snapOffsetPx)).isAtMost(1)
        }

        // A slow drag past half a segment (45.dp > 30.dp) that keeps the same item nearest to the
        // center snaps back to the item center rather than to an interior segment.
        rule.onNodeWithTag(TEST_TAG).performTouchInput {
            swipeWithVelocity(
                start = center,
                end = center.copy(y = center.y - with(rule.density) { 45.dp.toPx() }),
                endVelocity = 0f,
            )
        }
        rule.runOnIdle {
            assertThat(state.anchorItemIndex).isEqualTo(initialIndex + 1)
            assertThat(abs(state.anchorItemScrollOffset - snapOffsetPx)).isAtMost(1)
        }

        // Flip the flag on for a fresh composition: rotary and touch now step through segments.
        rule.runOnIdle {
            WearComposeFoundationFlags.isTransformingLazyColumnSnapSegmentsEnabled = true
            flagEnabled = true
        }
        rule.waitForIdle()

        rule.onNode(hasScrollAction()).performRotaryScrollInput {
            rotateToScrollVertically(segmentThresholdPx * 1.3f)
        }
        rule.runOnIdle {
            assertThat(state.anchorItemIndex).isEqualTo(initialIndex)
            assertThat(abs(state.anchorItemScrollOffset - (snapOffsetPx + segmentThresholdPx)))
                .isAtMost(2)
        }

        rule.onNodeWithTag(TEST_TAG).performTouchInput {
            swipeWithVelocity(
                start = center,
                end = center.copy(y = center.y - with(rule.density) { 45.dp.toPx() }),
                endVelocity = 0f,
            )
        }
        rule.runOnIdle {
            assertThat(state.anchorItemIndex).isEqualTo(initialIndex)
            assertThat(abs(state.anchorItemScrollOffset - (snapOffsetPx + 2 * segmentThresholdPx)))
                .isAtMost(2)
        }
    }

    @OptIn(ExperimentalTestApi::class)
    @Test
    fun rotarySnapBehavior_subThresholdTurn_scrollsLinearlyAndSettlesBackAndCancelsOnTurn() {
        lateinit var state: TransformingLazyColumnState
        lateinit var snapPoints: TransformingLazyColumnSnapPointsProvider
        val initialIndex = 10
        rule.setContent {
            state = rememberTransformingLazyColumnState(initialAnchorItemIndex = initialIndex)
            snapPoints = rememberTransformingLazyColumnSnapPoints(state, snapOffsetPx = 0)
            val behavior = remember { highResSnapBehavior(state, snapPoints) }
            TransformingLazyColumn(
                state = state,
                modifier = Modifier.size(200.dp).testTag(TEST_TAG),
                rotaryScrollableBehavior = behavior,
            ) {
                items(30) { Box(Modifier.fillMaxWidth().height(100.dp)) }
            }
        }
        val stepPx = rule.runOnIdle { snapPoints.current().stepSize() }
        val subThresholdDelta = stepPx * 0.45f

        rule.mainClock.autoAdvance = false
        try {
            rule.onNode(hasScrollAction()).performRotaryScrollInput {
                rotateToScrollVertically(subThresholdDelta)
            }
            // Before the 100ms settle-back delay, the list pre-scrolls linearly by delta / 3.
            rule.mainClock.advanceTimeBy(32L)
            rule.runOnIdle {
                assertThat(state.anchorItemIndex).isEqualTo(initialIndex)
                val expectedOffset =
                    subThresholdDelta / RotarySnapSensitivityValues.Default.resistanceFactor
                assertThat(abs(state.anchorItemScrollOffset - expectedOffset)).isAtMost(2f)
            }
        } finally {
            rule.mainClock.autoAdvance = true
        }
        rule.mainClock.advanceTimeBy(1_000L)
        rule.waitForIdle()
        assertRestsOnSnapPoint(snapPoints)
        rule.runOnIdle { assertThat(state.anchorItemIndex).isEqualTo(initialIndex) }

        // Start a second sub-threshold turn, wait until settle-back (snapDirection == 0) is active,
        // then turn past the threshold: the settle-back is cancelled and the list advances.
        rule.mainClock.autoAdvance = false
        try {
            rule.mainClock.advanceTimeBy(300L)
            rule.onNode(hasScrollAction()).performRotaryScrollInput {
                rotateToScrollVertically(subThresholdDelta)
            }
            rule.mainClock.advanceTimeBy(120L)
            rule.onNode(hasScrollAction()).performRotaryScrollInput {
                rotateToScrollVertically(stepPx * 1.2f)
            }
        } finally {
            rule.mainClock.autoAdvance = true
        }
        rule.waitForIdle()
        assertRestsOnSnapPoint(snapPoints)
        rule.runOnIdle { assertThat(state.anchorItemIndex).isEqualTo(initialIndex + 1) }
    }

    @OptIn(ExperimentalTestApi::class)
    @Test
    fun rotarySnapBehavior_fastMultiEventTurnCancelledBySmallReverse_doesNotReverseSnap() {
        lateinit var state: TransformingLazyColumnState
        lateinit var snapPoints: TransformingLazyColumnSnapPointsProvider
        val initialIndex = 10
        rule.setContent {
            state = rememberTransformingLazyColumnState(initialAnchorItemIndex = initialIndex)
            snapPoints = rememberTransformingLazyColumnSnapPoints(state, snapOffsetPx = 0)
            val behavior = remember { highResSnapBehavior(state, snapPoints) }
            TransformingLazyColumn(
                state = state,
                modifier = Modifier.size(200.dp).testTag(TEST_TAG),
                rotaryScrollableBehavior = behavior,
            ) {
                items(50) { Box(Modifier.fillMaxWidth().height(100.dp)) }
            }
        }
        val stepPx = rule.runOnIdle { snapPoints.current().stepSize() }

        rule.onNode(hasScrollAction()).performRotaryScrollInput {
            // Multi-event fast forward turn builds high smoothed velocity and starts a forward
            // snap.
            rotateToScrollVertically(stepPx * 0.4f)
            advanceEventTime(10L)
            rotateToScrollVertically(stepPx * 0.8f)
            advanceEventTime(10L)
            rotateToScrollVertically(stepPx * 0.3f)
            // Reversing resets velocity tracking, so -0.7 * stepPx stays below the unboosted
            // threshold (1.0 * stepPx) instead of snapping backward to initialIndex - 1.
            advanceEventTime(20L)
            rotateToScrollVertically(-stepPx * 0.7f)
        }
        rule.waitForIdle()
        assertRestsOnSnapPoint(snapPoints)

        rule.runOnIdle { assertThat(state.anchorItemIndex).isEqualTo(initialIndex) }
    }

    @OptIn(ExperimentalTestApi::class)
    @Test
    fun rotarySnapBehavior_tinyReverseReboundDuringSnap_doesNotCancelSnap() {
        lateinit var state: TransformingLazyColumnState
        lateinit var snapPoints: TransformingLazyColumnSnapPointsProvider
        val initialIndex = 10
        rule.setContent {
            state = rememberTransformingLazyColumnState(initialAnchorItemIndex = initialIndex)
            snapPoints = rememberTransformingLazyColumnSnapPoints(state, snapOffsetPx = 0)
            val behavior = remember { highResSnapBehavior(state, snapPoints) }
            TransformingLazyColumn(
                state = state,
                modifier = Modifier.size(200.dp).testTag(TEST_TAG),
                rotaryScrollableBehavior = behavior,
            ) {
                items(50) { Box(Modifier.fillMaxWidth().height(100.dp)) }
            }
        }
        val stepPx = rule.runOnIdle { snapPoints.current().stepSize() }
        val cancelThresholdPx = with(rule.density) { 2.dp.toPx() }

        // A single -1f crown rebound event right after starting a forward snap is ignored.
        rule.onNode(hasScrollAction()).performRotaryScrollInput {
            rotateToScrollVertically(stepPx * 1.2f)
            advanceEventTime(15L)
            rotateToScrollVertically(-1f)
        }
        rule.waitForIdle()
        assertRestsOnSnapPoint(snapPoints)
        rule.runOnIdle { assertThat(state.anchorItemIndex).isEqualTo(initialIndex + 1) }

        // Starting from initialIndex + 1 toward initialIndex + 2, sustained small reverse deltas
        // exceeding the jitter threshold cancel the forward snap so it settles back at
        // initialIndex + 1.
        rule.mainClock.advanceTimeBy(300L)
        val perEventReverse = -(cancelThresholdPx * 0.6f)
        rule.onNode(hasScrollAction()).performRotaryScrollInput {
            rotateToScrollVertically(stepPx * 1.2f)
            advanceEventTime(15L)
            rotateToScrollVertically(perEventReverse)
            advanceEventTime(15L)
            rotateToScrollVertically(perEventReverse)
        }
        rule.waitForIdle()
        assertRestsOnSnapPoint(snapPoints)
        rule.runOnIdle { assertThat(state.anchorItemIndex).isEqualTo(initialIndex + 1) }
    }

    @OptIn(ExperimentalTestApi::class, ExperimentalWearFoundationApi::class)
    @Test
    fun rotarySnapBehavior_emptyItems_areNotSnapPoints() {
        assumeTrue(WearComposeFoundationFlags.isTransformingLazyColumnSnapSegmentsEnabled)
        lateinit var state: TransformingLazyColumnState
        lateinit var snapPoints: TransformingLazyColumnSnapPointsProvider
        val initialIndex = 9
        rule.setContent {
            state = rememberTransformingLazyColumnState(initialAnchorItemIndex = initialIndex)
            snapPoints = rememberTransformingLazyColumnSnapPoints(state, snapOffsetPx = 0)
            val behavior = remember { highResSnapBehavior(state, snapPoints) }
            TransformingLazyColumn(
                state = state,
                modifier = Modifier.size(200.dp).testTag(TEST_TAG),
                verticalArrangement = Arrangement.spacedBy(0.dp),
                rotaryScrollableBehavior = behavior,
            ) {
                // Two empty items, as collapsed or hidden items, after each item with a size: their
                // snap points would coincide.
                items(30) { Box(Modifier.fillMaxWidth().height(if (it % 3 == 0) 60.dp else 0.dp)) }
            }
        }
        rule.runOnIdle {
            val points = snapPoints.current().points
            assertThat(points).isNotEmpty()
            assertThat(points.all { it.key.itemIndex % 3 == 0 }).isTrue()
        }
        val stepPx = rule.runOnIdle { snapPoints.current().stepSize() }

        // Each turn moves on to the next item with a size, rather than resting on empty items.
        repeat(3) { step ->
            rule.mainClock.advanceTimeBy(300L)
            rule.onNode(hasScrollAction()).performRotaryScrollInput {
                rotateToScrollVertically(stepPx * 1.3f)
            }
            rule.waitForIdle()
            assertRestsOnSnapPoint(snapPoints)
            rule.runOnIdle {
                assertThat(snapPoints.current().nearest?.key?.itemIndex)
                    .isEqualTo(initialIndex + 3 * (step + 1))
            }
        }
    }

    @OptIn(ExperimentalTestApi::class)
    @Test
    fun rotarySnapBehavior_tallItemAndEdges_triggersHapticPerSegmentAndStopsAtEdges() {
        lateinit var state: TransformingLazyColumnState
        lateinit var snapPoints: TransformingLazyColumnSnapPointsProvider
        var snapHaptics = 0
        val haptics =
            object : RotaryHapticHandler by NoRotaryHaptics {
                override fun handleSnapHaptic(
                    timestamp: Long,
                    deltaInPixels: Float,
                    inputDeviceId: Int,
                    axis: Int,
                ) {
                    snapHaptics++
                }
            }
        val viewportPx = with(rule.density) { 200.dp.roundToPx() }
        val thresholdPx = viewportPx / 4

        rule.setContent {
            state = rememberTransformingLazyColumnState(initialAnchorItemIndex = 0)
            snapPoints = rememberTransformingLazyColumnSnapPoints(state, snapOffsetPx = 0)
            val behavior = remember { highResSnapBehavior(state, snapPoints, haptics) }
            TransformingLazyColumn(
                state = state,
                modifier = Modifier.size(200.dp).testTag(TEST_TAG),
                contentPadding = PaddingValues(vertical = 100.dp),
                rotaryScrollableBehavior = behavior,
            ) {
                item {
                    Box(
                        Modifier.fillMaxWidth()
                            .height(with(rule.density) { thresholdPx.toDp() })
                            .snapSegmentThreshold { _, height -> height }
                    )
                }
                item {
                    Box(
                        Modifier.fillMaxWidth()
                            .height(with(rule.density) { (thresholdPx * 3).toDp() })
                            .snapSegmentThreshold { _, _ -> thresholdPx }
                    )
                }
                item {
                    Box(
                        Modifier.fillMaxWidth()
                            .height(with(rule.density) { thresholdPx.toDp() })
                            .snapSegmentThreshold { _, height -> height }
                    )
                }
            }
        }
        fun turn(direction: Int) {
            rule.mainClock.advanceTimeBy(300L)
            rule.onNode(hasScrollAction()).performRotaryScrollInput {
                rotateToScrollVertically(direction * thresholdPx * 1.3f)
            }
            rule.waitForIdle()
        }

        // At the first snap point (0, 0), turning backward does not fire a snap haptic.
        turn(-1)
        assertThat(snapHaptics).isEqualTo(0)

        // Stepping through (1, 0), (1, 1), (1, 2) and onto (2, 0) fires one haptic per segment.
        repeat(4) { step ->
            turn(1)
            assertThat(snapHaptics).isEqualTo(step + 1)
        }
        rule.runOnIdle { assertThat(state.anchorItemIndex).isEqualTo(2) }

        // At the last snap point (2, 0), turning forward does not fire another snap haptic.
        turn(1)
        assertThat(snapHaptics).isEqualTo(4)
    }

    @OptIn(ExperimentalTestApi::class, ExperimentalWearFoundationApi::class)
    @Test
    fun snapBehaviors_emptyList_doNotCrashOrSnap() {
        assumeTrue(WearComposeFoundationFlags.isTransformingLazyColumnSnapSegmentsEnabled)
        lateinit var state: TransformingLazyColumnState
        lateinit var snapPoints: TransformingLazyColumnSnapPointsProvider
        rule.setContent {
            state = rememberTransformingLazyColumnState()
            snapPoints = rememberTransformingLazyColumnSnapPoints(state, snapOffsetPx = 0)
            TransformingLazyColumn(
                state = state,
                modifier = Modifier.size(200.dp).testTag(TEST_TAG),
                rotaryScrollableBehavior = RotaryScrollableDefaults.snapBehavior(state),
                flingBehavior = TransformingLazyColumnDefaults.snapFlingBehavior(state),
            ) {
                items(0) {}
            }
        }

        rule.runOnIdle {
            assertThat(snapPoints.current().points).isEmpty()
            assertThat(snapPoints.current().nearest).isNull()
            assertThat(snapPoints.current().stepSize()).isEqualTo(0f)
        }

        rule.onNodeWithTag(TEST_TAG).performTouchInput { swipeUp() }
        rule.waitForIdle()
        rule.onNode(hasScrollAction()).performRotaryScrollInput { rotateToScrollVertically(300f) }
        rule.mainClock.advanceTimeBy(1_000L)
        rule.waitForIdle()

        rule.runOnIdle {
            assertThat(state.anchorItemIndex).isEqualTo(0)
            assertThat(state.anchorItemScrollOffset).isEqualTo(0)
        }
    }

    @OptIn(ExperimentalTestApi::class, ExperimentalWearFoundationApi::class)
    @Test
    fun snapBehaviors_collapsedPointsAtStart_fastFlingAndTurnReachStart() {
        assumeTrue(WearComposeFoundationFlags.isTransformingLazyColumnSnapSegmentsEnabled)
        lateinit var state: TransformingLazyColumnState
        lateinit var snapPoints: TransformingLazyColumnSnapPointsProvider
        val segmentThresholdPx = with(rule.density) { 50.dp.roundToPx() }
        rule.setContent {
            state = rememberTransformingLazyColumnState(initialAnchorItemIndex = 2)
            snapPoints = rememberTransformingLazyColumnSnapPoints(state, snapOffsetPx = 0)
            TransformingLazyColumn(
                state = state,
                modifier = Modifier.size(200.dp).testTag(TEST_TAG),
                rotaryScrollableBehavior = RotaryScrollableDefaults.snapBehavior(state),
                flingBehavior = TransformingLazyColumnDefaults.snapFlingBehavior(state),
            ) {
                items(7) { index ->
                    val height = if (index == 0) 300.dp else 50.dp
                    Box(
                        Modifier.fillMaxWidth()
                            .height(height)
                            .snapSegmentThreshold { _, itemHeight ->
                                if (index == 0) segmentThresholdPx else itemHeight
                            }
                            .transformedHeight { itemHeight, progress ->
                                if (progress.isUnspecified) {
                                    itemHeight
                                } else {
                                    val overflow =
                                        maxOf(0f, 0.25f - progress.topOffsetFraction) +
                                            maxOf(0f, progress.bottomOffsetFraction - 0.75f)
                                    itemHeight -
                                        (itemHeight * (overflow * 0.3f).coerceAtMost(0.5f)).toInt()
                                }
                            }
                    )
                }
            }
        }
        rule.runOnIdle {
            assertThat(snapPoints.current().collapsedPointsBeyond(-1)).isGreaterThan(0)
        }

        // Single fast downward swipe reaches the start of the list.
        rule.onNodeWithTag(TEST_TAG).performTouchInput {
            swipeWithVelocity(
                start = center,
                end = center.copy(y = center.y + with(rule.density) { 80.dp.toPx() }),
                endVelocity = with(rule.density) { 2500.dp.toPx() },
            )
        }
        rule.waitForIdle()
        assertRestsOnSnapPoint(snapPoints)
        rule.runOnIdle { assertThat(state.canScrollBackward).isFalse() }

        // Reset to item 2 and verify a fast backward rotary turn also reaches the start.
        rule.runOnIdle { runBlocking { state.scrollToItem(2) } }
        val stepPx = rule.runOnIdle {
            val current = snapPoints.current()
            assertThat(current.collapsedPointsBeyond(-1)).isGreaterThan(0)
            current.stepSize()
        }
        rule.mainClock.advanceTimeBy(300L)
        rule.onNode(hasScrollAction()).performRotaryScrollInput {
            repeat(8) {
                if (it > 0) advanceEventTime(15L)
                rotateToScrollVertically(-stepPx * 1.5f)
            }
        }
        rule.waitForIdle()
        assertRestsOnSnapPoint(snapPoints)
        rule.runOnIdle { assertThat(state.canScrollBackward).isFalse() }
    }

    @Test
    fun snapPoints_defaultThreshold_splitsFullWidthTallItemAndKeepsNarrowItemWhole() {
        lateinit var snapPoints: TransformingLazyColumnSnapPointsProvider
        var isRound = false
        rule.setContent {
            isRound = isRoundDevice()
            val screenWidth = LocalConfiguration.current.screenWidthDp.dp
            val testItemHeight = screenWidth * 0.8f
            val state = rememberTransformingLazyColumnState(initialAnchorItemIndex = 1)
            snapPoints = rememberTransformingLazyColumnSnapPoints(state, snapOffsetPx = 0)
            TransformingLazyColumn(
                state = state,
                modifier = Modifier.size(screenWidth),
                contentPadding = PaddingValues(vertical = screenWidth),
            ) {
                item { Box(Modifier.fillMaxWidth().height(screenWidth * 2.5f)) }
                item { Box(Modifier.fillMaxWidth().height(testItemHeight)) }
                item { Box(Modifier.width(52.dp).height(testItemHeight)) }
            }
        }

        rule.runOnIdle {
            val points = snapPoints.current().points
            // An item 2.5x the viewport height is split even without Modifier.snapSegmentThreshold.
            assertThat(points.count { it.key.itemIndex == 0 }).isAtLeast(3)
            if (isRound) {
                // On a round screen, a 0.8D-tall full-width item exceeds the visible chord (~0.62D)
                // and is split into 2 segments, while a narrow 52.dp item of the same height fits
                // the vertical diameter and stays whole.
                assertThat(points.count { it.key.itemIndex == 1 }).isEqualTo(2)
                assertThat(points.count { it.key.itemIndex == 2 }).isEqualTo(1)
            }
        }
    }

    @OptIn(ExperimentalTestApi::class, ExperimentalWearFoundationApi::class)
    @Test
    fun snapBehaviors_itemCountShrinksMidSnap_settlesAtNewEnd() {
        assumeTrue(WearComposeFoundationFlags.isTransformingLazyColumnSnapSegmentsEnabled)
        lateinit var state: TransformingLazyColumnState
        lateinit var snapPoints: TransformingLazyColumnSnapPointsProvider
        var count by mutableStateOf(25)
        rule.setContent {
            state = rememberTransformingLazyColumnState(initialAnchorItemIndex = 10)
            snapPoints = rememberTransformingLazyColumnSnapPoints(state, snapOffsetPx = 0)
            TransformingLazyColumn(
                state = state,
                modifier = Modifier.size(200.dp).testTag(TEST_TAG),
                rotaryScrollableBehavior = RotaryScrollableDefaults.snapBehavior(state),
                flingBehavior = TransformingLazyColumnDefaults.snapFlingBehavior(state),
            ) {
                items(count) { Box(Modifier.fillMaxWidth().height(50.dp)) }
            }
        }

        // Shrink the list mid-fling so the targeted items no longer exist.
        rule.mainClock.autoAdvance = false
        try {
            rule.onNodeWithTag(TEST_TAG).performTouchInput {
                swipeWithVelocity(
                    start = center,
                    end = center.copy(y = center.y - with(rule.density) { 80.dp.toPx() }),
                    endVelocity = with(rule.density) { 3000.dp.toPx() },
                )
            }
            rule.mainClock.advanceTimeBy(32L)
            count = 12
        } finally {
            rule.mainClock.autoAdvance = true
        }
        rule.waitForIdle()
        assertRestsOnSnapPoint(snapPoints)
        rule.runOnIdle { assertThat(state.canScrollForward).isFalse() }

        // Reset and shrink the list mid-rotary-snap so the targeted item no longer exists.
        count = 25
        rule.runOnIdle { runBlocking { state.scrollToItem(10) } }
        val stepPx = rule.runOnIdle { snapPoints.current().stepSize() }
        rule.mainClock.autoAdvance = false
        try {
            rule.mainClock.advanceTimeBy(300L)
            rule.onNode(hasScrollAction()).performRotaryScrollInput {
                rotateToScrollVertically(stepPx * 2.5f)
            }
            rule.mainClock.advanceTimeBy(32L)
            count = 12
        } finally {
            rule.mainClock.autoAdvance = true
        }
        rule.waitForIdle()
        assertRestsOnSnapPoint(snapPoints)
        rule.runOnIdle { assertThat(state.canScrollForward).isFalse() }
    }

    private fun highResSnapBehavior(
        state: TransformingLazyColumnState,
        snapPoints: TransformingLazyColumnSnapPointsProvider,
        rotaryHaptics: RotaryHapticHandler = NoRotaryHaptics,
    ): HighResSnapRotaryScrollableBehavior {
        val sensitivity = RotarySnapSensitivityValues.Default
        val reverseSnapCancelThresholdPx = with(rule.density) { 2.dp.toPx() }
        return HighResSnapRotaryScrollableBehavior(
            rotaryHaptics = rotaryHaptics,
            scrollDistanceDivider = sensitivity.resistanceFactor,
            thresholdHandlerFactory = {
                ThresholdHandler(
                    minThresholdDivider = sensitivity.minThresholdDivider,
                    maxThresholdDivider = sensitivity.maxThresholdDivider,
                    averageItemSize = { snapPoints.current().stepSize() },
                )
            },
            snapHandlerFactory = { TransformingLazyColumnSnapHandler(state, snapPoints) },
            scrollHandlerFactory = { RotaryScrollHandler(state) },
            followRotaryInput = true,
            reverseSnapCancelThresholdPx = reverseSnapCancelThresholdPx,
        )
    }

    @Test
    fun scrollItemToEdge_ableToFlingOverEdge() {
        lateinit var state: TransformingLazyColumnState

        val itemHeight = 50.dp
        val itemHeightPx = with(rule.density) { itemHeight.toPx() }
        val itemIndexToCheck = 2

        rule.setContent {
            state = rememberTransformingLazyColumnState()
            TransformingLazyColumn(
                state = state,
                verticalArrangement = Arrangement.spacedBy(space = 0.dp, alignment = Alignment.Top),
            ) {
                items(100) {
                    Box(
                        Modifier.fillMaxWidth()
                            .height(itemHeight)
                            .testTag(if (it == itemIndexToCheck) TEST_TAG else "")
                    )
                }
            }
        }

        // Distance from top of test item to top edge of TLC.
        val topDistance = itemIndexToCheck * itemHeightPx

        rule.onNodeWithTag(TEST_TAG).performTouchInput {
            // swipeUp's default startY value is bottom,
            // so scroll distance  = itemHeight + topDistance.
            // After scrolling, the item's bottom edge must reach at least the top of the TLC.
            swipeUp(endY = -topDistance)
        }
        rule.waitForIdle()

        // With fling, we expect that current item will be scrolled more and disappear. So, first
        // visible item should be updated to one of next items.
        assertTrue(state.layoutInfo.visibleItems.first().index > itemIndexToCheck)
    }

    @Test
    fun topItemBiggerThanThreshold_itemIsClickable() {
        assertEdgeClick(isTopEdge = true, isBiggerThanThreshold = true)
    }

    @Test
    fun topItemSmallerThanThreshold_itemIsNotClickable() {
        assertEdgeClick(isTopEdge = true, isBiggerThanThreshold = false)
    }

    @Test
    fun bottomItemBiggerThanThreshold_itemIsClickable() {
        assertEdgeClick(isTopEdge = false, isBiggerThanThreshold = true)
    }

    @Test
    fun bottomItemSmallerThanThreshold_itemIsNotClickable() {
        assertEdgeClick(isTopEdge = false, isBiggerThanThreshold = false)
    }

    private fun assertEdgeClick(
        isTopEdge: Boolean,
        isBiggerThanThreshold: Boolean,
        visibleThreshold: Dp = 20.dp,
    ) {
        lateinit var state: TransformingLazyColumnState
        lateinit var coroutineScope: CoroutineScope
        val itemHeightDp = 50
        var clickedIndex = -1
        val itemHeightPx = with(rule.density) { itemHeightDp.dp.toPx().toInt() }

        rule.setContent {
            state = rememberTransformingLazyColumnState()
            coroutineScope = rememberCoroutineScope()
            TransformingLazyColumn(state = state, modifier = Modifier.testTag(lazyListTag)) {
                items(100) { index ->
                    Box(
                        Modifier.height(itemHeightDp.dp)
                            .clickable(onClick = { clickedIndex = index })
                    )
                }
            }
        }
        val expectedClickedIndex =
            if (isBiggerThanThreshold) {
                if (isTopEdge) 0 else 99
            } else {
                -1
            }
        val scrollDistance =
            with(rule.density) {
                if (isBiggerThanThreshold) {
                    (itemHeightDp.dp - visibleThreshold - 1.dp).toPx()
                } else {
                    (itemHeightDp.dp - visibleThreshold + 1.dp).toPx()
                }
            }

        rule.runOnIdle {
            coroutineScope.launch {
                if (isTopEdge) {
                    // Scroll up until top item is partly visible
                    state.scrollBy(scrollDistance)
                } else {
                    // Scroll up until last item is fully visible in the bottom
                    state.scrollToItem(
                        99,
                        -state.layoutInfo.viewportSize.height / 2 + itemHeightPx / 2,
                    )
                    // Scroll down until last item is partly visible
                    state.scrollBy(-scrollDistance)
                }
            }
        }
        rule.waitForIdle()

        val clickOffset =
            with(rule.density) {
                Offset(
                    state.layoutInfo.viewportSize.width / 2f,
                    if (isTopEdge) {
                        visibleThreshold.toPx() / 2
                    } else {
                        state.layoutInfo.viewportSize.height - visibleThreshold.toPx() / 2
                    },
                )
            }

        rule.onNodeWithTag(lazyListTag).performTouchInput {
            down(clickOffset)
            move()
            up()
        }
        assertEquals(expectedClickedIndex, clickedIndex)
    }

    @Composable
    fun TransformingLazyColumnWithSnap(state: TransformingLazyColumnState) {
        val itemHeight = 50.dp
        val testTag = "List"
        TransformingLazyColumn(
            state = state,
            modifier = Modifier.fillMaxSize().testTag(testTag),
            flingBehavior = TransformingLazyColumnDefaults.snapFlingBehavior(state),
        ) {
            items(100) { Box(Modifier.fillMaxWidth().height(itemHeight)) }
        }
    }

    @Test
    fun minimumVerticalContentPadding_pinnedToStart_appliedToTopPadding() {
        val padding = 30.dp
        val state = TransformingLazyColumnState()

        rule.setContent {
            TransformingLazyColumn(state = state, modifier = Modifier.size(200.dp)) {
                item {
                    Box(
                        Modifier.size(50.dp)
                            .minimumVerticalContentPadding(top = padding, bottom = 0.dp)
                            .testTag(firstItemTag)
                    )
                }
                items(10) { Box(Modifier.size(50.dp)) }
            }
        }

        rule.onNodeWithTag(firstItemTag).assertTopPositionInRootIsEqualTo(padding)
    }

    @Test
    fun minimumVerticalContentPadding_pinnedToEnd_appliedToBottomPadding() {
        val padding = 30.dp
        val containerHeight = 200.dp
        val itemCount = 10
        val itemSize = 50.dp
        lateinit var state: TransformingLazyColumnState
        rule.setContent {
            state = rememberTransformingLazyColumnState()
            TransformingLazyColumn(state = state, modifier = Modifier.size(containerHeight)) {
                items(itemCount) { Box(Modifier.size(itemSize)) }
                item {
                    Box(
                        Modifier.size(itemSize)
                            .minimumVerticalContentPadding(top = 0.dp, bottom = padding)
                            .testTag(lastItemTag)
                    )
                }
            }
        }

        rule.runOnIdle { runBlocking { state.scrollToItem(itemCount) } }

        rule
            .onNodeWithTag(lastItemTag)
            .assertTopPositionInRootIsEqualTo(
                containerHeight - padding - itemSize,
                tolerance = 1.dp,
            )
    }

    @Test
    fun bigPaddingsDontCrash() {
        rule.setContent {
            TransformingLazyColumn(
                modifier = Modifier.size(300.dp),
                contentPadding = PaddingValues(400.dp),
            ) {
                items(10) { Box(Modifier.size(50.dp)) }
            }
        }
    }

    private fun setupTlcWithMutableList(
        items: MutableList<String>,
        itemSize: Dp,
        reverseLayout: Boolean = false,
    ): TransformingLazyColumnState {
        lateinit var state: TransformingLazyColumnState
        rule.setContent {
            state = rememberTransformingLazyColumnState()
            TransformingLazyColumn(
                modifier = Modifier.testTag(lazyListTag).height(itemSize * 2),
                state = state,
                reverseLayout = reverseLayout,
            ) {
                items(items, key = { it }) { item ->
                    Spacer(Modifier.height(itemSize).testTag(item))
                }
            }
        }
        return state
    }

    private fun testTransformingLazyColumnRotary(
        userScrollEnabled: Boolean,
        scrollTarget: Int,
        itemsToScroll: Int = 2,
        reverseLayout: Boolean = false,
    ) {
        lateinit var state: TransformingLazyColumnState
        val itemSizePx = 50
        var itemSizeDp: Dp
        rule.setContent {
            with(rule.density) { itemSizeDp = itemSizePx.toDp() }
            state = rememberTransformingLazyColumnState()
            TransformingLazyColumn(
                state = state,
                modifier = Modifier.testTag(lazyListTag).size(itemSizeDp * 2),
                userScrollEnabled = userScrollEnabled,
                reverseLayout = reverseLayout,
            ) {
                items(100) {
                    BasicText(text = "item $it", modifier = Modifier.requiredSize(itemSizeDp))
                }
            }
        }
        rule.onNodeWithTag(lazyListTag).performRotaryScrollInput {
            // try to scroll by N items
            rotateToScrollVertically(
                (if (reverseLayout) -1 else 1) * itemSizePx.toFloat() * itemsToScroll
            )
        }
        rule.waitForIdle()

        assertThat(state.anchorItemIndex).isEqualTo(scrollTarget)
    }

    @Test
    fun pinnedItemIsComposedAndPlacedWhenScrolledOut() {
        val itemSizeDp = with(rule.density) { 10.toDp() }
        lateinit var state: TransformingLazyColumnState
        var pinnableContainer: PinnableContainer? = null
        val composed = mutableSetOf<Int>()

        rule.setContent {
            state = rememberTransformingLazyColumnState()
            TransformingLazyColumn(
                Modifier.size(itemSizeDp * 2).testTag(lazyListTag),
                state = state,
                verticalArrangement = Arrangement.Top,
            ) {
                items(100, key = { it }) { index ->
                    if (index == 1) {
                        pinnableContainer = LocalPinnableContainer.current
                    }
                    Box(Modifier.size(itemSizeDp).testTag("$index"))
                    DisposableEffect(index) {
                        composed.add(index)
                        onDispose { composed.remove(index) }
                    }
                }
            }
        }

        rule.runOnIdle { requireNotNull(pinnableContainer).pin() }

        rule.runOnIdle {
            assertThat(composed).contains(1)
            runBlocking { state.scrollToItem(10) }
        }

        rule.waitUntil {
            // not visible items were disposed
            !composed.contains(0)
        }

        rule.runOnIdle {
            // item 1 is still pinned
            assertThat(composed).contains(1)
        }

        rule.onNodeWithTag("1").assertExists().assertIsNotDisplayed().assertIsPlaced()
    }

    @Test
    fun pinnedItemIsDisposedWhenReleased() {
        val itemSizeDp = with(rule.density) { 10.toDp() }
        lateinit var state: TransformingLazyColumnState
        var pinnableContainer: PinnableContainer? = null
        val composed = mutableSetOf<Int>()

        rule.setContent {
            state = rememberTransformingLazyColumnState()
            TransformingLazyColumn(
                Modifier.size(itemSizeDp * 2).testTag(lazyListTag),
                state = state,
                verticalArrangement = Arrangement.Top,
            ) {
                items(100, key = { it }) { index ->
                    if (index == 1) {
                        pinnableContainer = LocalPinnableContainer.current
                    }
                    Box(Modifier.size(itemSizeDp).testTag("$index"))
                    DisposableEffect(index) {
                        composed.add(index)
                        onDispose { composed.remove(index) }
                    }
                }
            }
        }

        lateinit var handle: PinnableContainer.PinnedHandle
        rule.runOnIdle { handle = requireNotNull(pinnableContainer).pin() }

        rule.runOnIdle {
            assertThat(composed).contains(1)
            runBlocking { state.scrollToItem(10) }
        }

        rule.runOnIdle { assertThat(composed).contains(1) }

        rule.runOnIdle { handle.release() }

        rule.waitUntil { !composed.contains(1) }
    }

    @Test
    fun pinnedItemIsComposedAndPlacedWhenScrolledOut_extraItemsAfter() {
        val itemSizeDp = with(rule.density) { 10.toDp() }
        lateinit var state: TransformingLazyColumnState
        var pinnableContainer: PinnableContainer? = null
        val composed = mutableSetOf<Int>()

        rule.setContent {
            state = rememberTransformingLazyColumnState()
            TransformingLazyColumn(
                Modifier.size(itemSizeDp * 2).testTag(lazyListTag),
                state = state,
                verticalArrangement = Arrangement.Top,
            ) {
                items(100, key = { it }) { index ->
                    if (index == 11) {
                        pinnableContainer = LocalPinnableContainer.current
                    }
                    Box(Modifier.size(itemSizeDp).testTag("$index"))
                    DisposableEffect(index) {
                        composed.add(index)
                        onDispose { composed.remove(index) }
                    }
                }
            }
        }

        rule.runOnIdle { runBlocking { state.scrollToItem(10) } }

        rule.runOnIdle { assertThat(composed).contains(11) }

        rule.runOnIdle { requireNotNull(pinnableContainer).pin() }

        rule.runOnIdle { runBlocking { state.scrollToItem(0) } }

        rule.waitUntil { !composed.contains(10) }

        rule.runOnIdle { assertThat(composed).contains(11) }

        rule.onNodeWithTag("11").assertExists().assertIsNotDisplayed().assertIsPlaced()
    }

    @Test
    fun pinnedMultipleItemsAreComposedAndPlaced_mixed() {
        val itemSizeDp = with(rule.density) { 10.toDp() }
        lateinit var state: TransformingLazyColumnState
        var pinnableContainer1: PinnableContainer? = null
        var pinnableContainer11: PinnableContainer? = null
        val composed = mutableSetOf<Int>()

        rule.setContent {
            state = rememberTransformingLazyColumnState()
            TransformingLazyColumn(
                Modifier.size(itemSizeDp * 2).testTag(lazyListTag),
                state = state,
                verticalArrangement = Arrangement.Top,
            ) {
                items(100, key = { it }) { index ->
                    if (index == 1) {
                        pinnableContainer1 = LocalPinnableContainer.current
                    }
                    if (index == 11) {
                        pinnableContainer11 = LocalPinnableContainer.current
                    }
                    Box(Modifier.size(itemSizeDp).testTag("$index"))
                    DisposableEffect(index) {
                        composed.add(index)
                        onDispose { composed.remove(index) }
                    }
                }
            }
        }

        rule.runOnIdle { requireNotNull(pinnableContainer1).pin() }

        rule.runOnIdle { runBlocking { state.scrollToItem(10) } }

        rule.runOnIdle { requireNotNull(pinnableContainer11).pin() }

        rule.runOnIdle { runBlocking { state.scrollToItem(5) } }

        rule.waitUntil { !composed.contains(0) && !composed.contains(10) }

        rule.runOnIdle {
            assertThat(composed).contains(1)
            assertThat(composed).contains(11)
        }

        rule.onNodeWithTag("1").assertExists().assertIsNotDisplayed().assertIsPlaced()
        rule.onNodeWithTag("11").assertExists().assertIsNotDisplayed().assertIsPlaced()
    }

    @Test
    fun stateWrittenFromBackgroundThreadDuringMeasure_isNotLost() {
        lateinit var itemState: MutableState<String?>
        var valueReadInPlacement: String? = null
        var measuredOnce = false
        rule.setContent {
            TransformingLazyColumn(modifier = Modifier.size(100.dp)) {
                item {
                    val state = remember { mutableStateOf<String?>(null) }
                    itemState = state
                    Box(
                        Modifier.size(50.dp).layout { measurable, constraints ->
                            val placeable = measurable.measure(constraints)
                            if (!measuredOnce) {
                                measuredOnce = true
                                // Write from another thread while the list is still measuring.
                                thread { state.value = "loaded" }.join()
                            }
                            layout(placeable.width, placeable.height) {
                                valueReadInPlacement = state.value
                                placeable.place(0, 0)
                            }
                        }
                    )
                }
            }
        }

        rule.runOnIdle {
            assertThat(itemState.value).isEqualTo("loaded")
            assertThat(valueReadInPlacement).isEqualTo("loaded")
        }
    }
}

/**
 * Asserts that the current semantics node is not placed.
 *
 * Throws [AssertionError] if the node is placed.
 */
internal fun SemanticsNodeInteraction.assertIsNotPlaced() {
    // TODO(b/187188981): We don't have a non-throwing API to check whether an item exists.
    //  So until this bug is fixed, we are going to catch the assertion error and then check
    //  whether the node is placed or not.
    try {
        // If the node does not exist, it implies that it is also not placed.
        assertDoesNotExist()
    } catch (e: AssertionError) {
        // If the node exists, we need to assert that it is not placed.
        val errorMessageOnFail = "Assert failed: The component is placed!"
        if (fetchSemanticsNode().layoutInfo.isPlaced) {
            throw AssertionError(errorMessageOnFail)
        }
    }
}

/**
 * Asserts that the current semantics node is placed.
 *
 * Throws [AssertionError] if the node is not placed.
 */
internal fun SemanticsNodeInteraction.assertIsPlaced(): SemanticsNodeInteraction {
    val errorMessageOnFail = "Assert failed: The component is not placed!"
    if (!fetchSemanticsNode(errorMessageOnFail).layoutInfo.isPlaced) {
        throw AssertionError(errorMessageOnFail)
    }
    return this
}

internal val NeverEqualObject =
    object {
        override fun equals(other: Any?): Boolean {
            return false
        }
    }
