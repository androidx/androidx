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

package androidx.compose.foundation.copyPasteAndroidTests.lazy.grid

import androidx.compose.foundation.assertThat
import androidx.compose.foundation.assertWithMessage
import androidx.compose.foundation.isEqualTo
import androidx.compose.foundation.isTrue
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyGridState
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.lazy.grid.rememberLazyGridState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Modifier
import androidx.compose.ui.layout.layout
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.semantics.SemanticsNode
import androidx.compose.ui.test.ExperimentalTestApi
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.assertIsNotDisplayed
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.onRoot
import androidx.compose.ui.test.runSkikoComposeUiTest
import androidx.compose.ui.unit.Density
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.dp
import androidx.compose.ui.util.fastForEach
import kotlin.test.Ignore
import kotlin.test.Test
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.launch
import kotlinx.test.IgnoreJsTarget

@OptIn(ExperimentalTestApi::class)
class LazyGridSlotsReuseTest {

    private val density = Density(1f)
    private val itemsSizePx = 30f
    private val itemsSizeDp = with(density) { itemsSizePx.toDp() }

    @Test
    fun scroll1ItemScrolledOffItemIsKeptForReuse() = runSkikoComposeUiTest {
        lateinit var state: LazyGridState
        lateinit var scope: CoroutineScope
        setContent {
            state = rememberLazyGridState()
            scope = rememberCoroutineScope()
            LazyVerticalGrid(GridCells.Fixed(1), Modifier.height(itemsSizeDp * 1.5f), state) {
                items(100) { Spacer(Modifier.height(itemsSizeDp).fillMaxWidth().testTag("$it")) }
            }
        }

        val id0 = onNodeWithTag("0").fetchSemanticsNode().id
        onNodeWithTag("0").assertIsDisplayed()

        runOnIdle { scope.launch { state.scrollToItem(1) } }

        onRoot().fetchSemanticsNode().assertLayoutDeactivatedById(id0)
        onNodeWithTag("1").assertIsDisplayed()
    }

    @Test
    fun scroll2ItemsScrolledOffItemsAreKeptForReuse() = runSkikoComposeUiTest {
        lateinit var state: LazyGridState
        lateinit var scope: CoroutineScope
        setContent {
            state = rememberLazyGridState()
            scope = rememberCoroutineScope()
            LazyVerticalGrid(GridCells.Fixed(1), Modifier.height(itemsSizeDp * 1.5f), state) {
                items(100) { Spacer(Modifier.height(itemsSizeDp).fillMaxWidth().testTag("$it")) }
            }
        }
        // Semantics IDs must be fetched before scrolling.
        val id0 = onNodeWithTag("0").fetchSemanticsNode().id
        val id1 = onNodeWithTag("1").fetchSemanticsNode().id
        onNodeWithTag("0").assertIsDisplayed()
        onNodeWithTag("1").assertIsDisplayed()

        runOnIdle { scope.launch { state.scrollToItem(2) } }

        onRoot().fetchSemanticsNode().assertLayoutDeactivatedById(id0)
        onRoot().fetchSemanticsNode().assertLayoutDeactivatedById(id1)
        onNodeWithTag("2").assertIsDisplayed()
    }

    @Test
    fun checkMaxItemsKeptForReuse() = runSkikoComposeUiTest {
        lateinit var state: LazyGridState
        lateinit var scope: CoroutineScope
        setContent {
            state = rememberLazyGridState()
            scope = rememberCoroutineScope()
            LazyVerticalGrid(
                GridCells.Fixed(1),
                Modifier.height(itemsSizeDp * (DefaultMaxItemsToRetain + 0.5f)),
                state
            ) {
                items(100) { Spacer(Modifier.height(itemsSizeDp).fillMaxWidth().testTag("$it")) }
            }
        }
        val deactivatedIds = mutableListOf<Int>()
        repeat(DefaultMaxItemsToRetain) {
            deactivatedIds.add(onNodeWithTag("$it").fetchSemanticsNode().id)
        }

        runOnIdle { scope.launch { state.scrollToItem(DefaultMaxItemsToRetain + 1) } }

        deactivatedIds.fastForEach {
            onRoot().fetchSemanticsNode().assertLayoutDeactivatedById(it)
        }
        onNodeWithTag("$DefaultMaxItemsToRetain").assertDoesNotExist()
        onNodeWithTag("${DefaultMaxItemsToRetain + 1}").assertIsDisplayed()
    }

    @Test
    fun scroll3Items2OfScrolledOffItemsAreKeptForReuse() = runSkikoComposeUiTest {
        lateinit var state: LazyGridState
        lateinit var scope: CoroutineScope
        setContent {
            state = rememberLazyGridState()
            scope = rememberCoroutineScope()
            LazyVerticalGrid(GridCells.Fixed(1), Modifier.height(itemsSizeDp * 1.5f), state) {
                items(100) { Spacer(Modifier.height(itemsSizeDp).fillMaxWidth().testTag("$it")) }
            }
        }

        val id0 = onNodeWithTag("0").fetchSemanticsNode().id
        val id1 = onNodeWithTag("1").fetchSemanticsNode().id
        onNodeWithTag("0").assertIsDisplayed()
        onNodeWithTag("1").assertIsDisplayed()

        runOnIdle {
            scope.launch {
                // after this step 0 and 1 are in reusable buffer
                state.scrollToItem(2)

                // this step requires one item and will take the last item from the buffer - item
                // 1 plus will put 2 in the buffer. so expected buffer is items 2 and 0
                state.scrollToItem(3)
            }
        }

        // recycled
        onNodeWithTag("1").assertDoesNotExist()

        // in buffer
        onRoot().fetchSemanticsNode().assertLayoutDeactivatedById(id0)
        onRoot().fetchSemanticsNode().assertLayoutDeactivatedById(id1)

        // visible
        onNodeWithTag("3").assertIsDisplayed()
        onNodeWithTag("4").assertIsDisplayed()
    }

    @Test
    fun doMultipleScrollsOneByOne() = runSkikoComposeUiTest {
        lateinit var state: LazyGridState
        lateinit var scope: CoroutineScope
        setContent {
            state = rememberLazyGridState()
            scope = rememberCoroutineScope()
            LazyVerticalGrid(GridCells.Fixed(1), Modifier.height(itemsSizeDp * 1.5f), state) {
                items(100) { Spacer(Modifier.height(itemsSizeDp).fillMaxWidth().testTag("$it")) }
            }
        }
        runOnIdle {
            scope.launch {
                state.scrollToItem(1) // buffer is [0]
                state.scrollToItem(2) // 0 used, buffer is [1]
            }
        }

        // 3 should be visible at this point, so save its ID to check later
        val id3 = onNodeWithTag("3").fetchSemanticsNode().id

        runOnIdle {
            scope.launch {
                state.scrollToItem(3) // 1 used, buffer is [2]
                state.scrollToItem(4) // 2 used, buffer is [3]
            }
        }

        // recycled
        onNodeWithTag("0").assertDoesNotExist()
        onNodeWithTag("1").assertDoesNotExist()
        onNodeWithTag("2").assertDoesNotExist()

        // in buffer
        onRoot().fetchSemanticsNode().assertLayoutDeactivatedById(id3)

        // visible
        onNodeWithTag("4").assertIsDisplayed()
        onNodeWithTag("5").assertIsDisplayed()
    }

    @Test
    fun scrollBackwardOnce() = runSkikoComposeUiTest {
        lateinit var state: LazyGridState
        lateinit var scope: CoroutineScope
        setContent {
            state = rememberLazyGridState(10)
            scope = rememberCoroutineScope()
            LazyVerticalGrid(GridCells.Fixed(1), Modifier.height(itemsSizeDp * 1.5f), state) {
                items(100) { Spacer(Modifier.height(itemsSizeDp).fillMaxWidth().testTag("$it")) }
            }
        }

        val id10 = onNodeWithTag("10").fetchSemanticsNode().id
        val id11 = onNodeWithTag("11").fetchSemanticsNode().id

        runOnIdle {
            scope.launch {
                state.scrollToItem(8) // buffer is [10, 11]
            }
        }

        // in buffer
        onRoot().fetchSemanticsNode().assertLayoutDeactivatedById(id10)
        onRoot().fetchSemanticsNode().assertLayoutDeactivatedById(id11)

        // visible
        onNodeWithTag("8").assertIsDisplayed()
        onNodeWithTag("9").assertIsDisplayed()
    }

    @Test
    fun scrollBackwardOneByOne() = runSkikoComposeUiTest {
        lateinit var state: LazyGridState
        lateinit var scope: CoroutineScope
        setContent {
            state = rememberLazyGridState(10)
            scope = rememberCoroutineScope()
            LazyVerticalGrid(GridCells.Fixed(1), Modifier.height(itemsSizeDp * 1.5f), state) {
                items(100) { Spacer(Modifier.height(itemsSizeDp).fillMaxWidth().testTag("$it")) }
            }
        }
        runOnIdle {
            scope.launch {
                state.scrollToItem(9) // buffer is [11]
                state.scrollToItem(7) // 11 reused, buffer is [9]
            }
        }
        // 8 should be visible at this point, so save its ID to check later
        val id8 = onNodeWithTag("8").fetchSemanticsNode().id
        runOnIdle {
            scope.launch {
                state.scrollToItem(6) // 9 reused, buffer is [8]
            }
        }

        // in buffer
        onRoot().fetchSemanticsNode().assertLayoutDeactivatedById(id8)

        // visible
        onNodeWithTag("6").assertIsDisplayed()
        onNodeWithTag("7").assertIsDisplayed()
    }

    @Test
    fun scrollingBackReusesTheSameSlot() = runSkikoComposeUiTest {
        lateinit var state: LazyGridState
        lateinit var scope: CoroutineScope
        var counter0 = 0
        var counter1 = 0

        val measureCountModifier0 =
            Modifier.layout { measurable, constraints ->
                counter0++
                val placeable = measurable.measure(constraints)
                layout(placeable.width, placeable.height) { placeable.place(IntOffset.Zero) }
            }

        val measureCountModifier1 =
            Modifier.layout { measurable, constraints ->
                counter1++
                val placeable = measurable.measure(constraints)
                layout(placeable.width, placeable.height) { placeable.place(IntOffset.Zero) }
            }

        setContent {
            state = rememberLazyGridState()
            scope = rememberCoroutineScope()
            LazyVerticalGrid(GridCells.Fixed(1), Modifier.height(itemsSizeDp * 1.5f), state) {
                items(100) {
                    val modifier =
                        when (it) {
                            0 -> measureCountModifier0
                            1 -> measureCountModifier1
                            else -> Modifier
                        }
                    Spacer(Modifier.height(itemsSizeDp).testTag("$it").then(modifier))
                }
            }
        }
        runOnIdle {
            scope.launch {
                state.scrollToItem(2) // buffer is [0, 1]
            }
        }

        // 2 and 3 should be visible at this point, so save its ID to check later
        val id2 = onNodeWithTag("2").fetchSemanticsNode().id
        val id3 = onNodeWithTag("3").fetchSemanticsNode().id

        runOnIdle {
            scope.launch {
                counter0 = 0
                counter1 = 0
                state.scrollToItem(0) // scrolled back, 0 and 1 are reused back. buffer: [2, 3]
            }
        }

        runOnIdle {
            assertWithMessage("Item 0 measured $counter0 times, expected 0.")
                .that(counter0)
                .isEqualTo(0)
            assertWithMessage("Item 1 measured $counter1 times, expected 0.")
                .that(counter1)
                .isEqualTo(0)
        }

        onNodeWithTag("0").assertIsDisplayed()
        onNodeWithTag("1").assertIsDisplayed()

        onRoot().fetchSemanticsNode().assertLayoutDeactivatedById(id2)
        onRoot().fetchSemanticsNode().assertLayoutDeactivatedById(id3)
    }

    @Test
    fun differentContentTypes() = runSkikoComposeUiTest {
        lateinit var state: LazyGridState
        lateinit var scope: CoroutineScope
        val visibleItemsCount = (DefaultMaxItemsToRetain + 1) * 2
        val startOfType1 = DefaultMaxItemsToRetain + 1
        setContent {
            state = rememberLazyGridState()
            scope = rememberCoroutineScope()
            LazyVerticalGrid(
                GridCells.Fixed(1),
                Modifier.height(itemsSizeDp * (visibleItemsCount - 0.5f)),
                state
            ) {
                items(100, contentType = { if (it >= startOfType1) 1 else 0 }) {
                    Spacer(Modifier.height(itemsSizeDp).fillMaxWidth().testTag("$it"))
                }
            }
        }

        val deactivatedIds = mutableListOf<Int>()
        for (i in 0 until visibleItemsCount) {
            deactivatedIds.add(onNodeWithTag("$i").fetchSemanticsNode().id)
            onNodeWithTag("$i").assertIsDisplayed()
        }
        for (i in startOfType1 until startOfType1 + DefaultMaxItemsToRetain) {
            deactivatedIds.add(onNodeWithTag("$i").fetchSemanticsNode().id)
        }

        runOnIdle { scope.launch { state.scrollToItem(visibleItemsCount) } }

        onNodeWithTag("$visibleItemsCount").assertIsDisplayed()

        // [DefaultMaxItemsToRetain] items of type 0 are left for reuse and 7 items of type 1
        deactivatedIds.fastForEach {
            onRoot().fetchSemanticsNode().assertLayoutDeactivatedById(it)
        }

        onNodeWithTag("$DefaultMaxItemsToRetain").assertDoesNotExist()
        onNodeWithTag("${startOfType1 + DefaultMaxItemsToRetain}").assertDoesNotExist()
    }

    @Test
    fun differentTypesFromDifferentItemCalls() = runSkikoComposeUiTest {
        lateinit var state: LazyGridState
        lateinit var scope: CoroutineScope
        setContent {
            state = rememberLazyGridState()
            scope = rememberCoroutineScope()
            LazyVerticalGrid(GridCells.Fixed(1), Modifier.height(itemsSizeDp * 2.5f), state) {
                val content =
                    @Composable { tag: String ->
                        Spacer(Modifier.height(itemsSizeDp).width(10.dp).testTag(tag))
                    }
                item(contentType = "not-to-reuse-0") { content("0") }
                item(contentType = "reuse") { content("1") }
                items(
                    List(100) { it + 2 },
                    contentType = { if (it == 10) "reuse" else "not-to-reuse-$it" }
                ) {
                    content("$it")
                }
            }
        }

        val id0 = onNodeWithTag("0").fetchSemanticsNode().id
        val id1 = onNodeWithTag("1").fetchSemanticsNode().id

        runOnIdle {
            scope.launch {
                state.scrollToItem(2)
                // now items 0 and 1 are put into reusables
            }
        }

        onRoot().fetchSemanticsNode().assertLayoutDeactivatedById(id0)
        onRoot().fetchSemanticsNode().assertLayoutDeactivatedById(id1)

        runOnIdle {
            scope.launch {
                state.scrollToItem(9)
                // item 10 should reuse slot 1
            }
        }

        onRoot().fetchSemanticsNode().assertLayoutDeactivatedById(id0)
        onNodeWithTag("1").assertDoesNotExist()
        onNodeWithTag("9").assertIsDisplayed()
        onNodeWithTag("10").assertIsDisplayed()
        onNodeWithTag("11").assertIsDisplayed()
    }

    private fun SemanticsNode.assertLayoutDeactivatedById(id: Int) {
        children.fastForEach {
            if (it.id == id) {
                assertThat(it.layoutInfo.isDeactivated).isTrue()
            }
        }
    }
}

private val DefaultMaxItemsToRetain = 7
