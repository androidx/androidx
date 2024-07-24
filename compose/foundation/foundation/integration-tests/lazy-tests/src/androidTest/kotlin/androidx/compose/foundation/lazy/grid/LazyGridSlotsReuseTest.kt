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

package androidx.compose.foundation.lazy.grid

import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.width
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.layout.layout
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.semantics.SemanticsNode
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.onRoot
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.dp
import androidx.compose.ui.util.fastForEach
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.filters.LargeTest
import com.google.common.truth.Truth
import kotlinx.coroutines.runBlocking
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

@LargeTest
@RunWith(AndroidJUnit4::class)
class LazyGridSlotsReuseTest {

    @get:Rule val rule = createComposeRule()

    val itemsSizePx = 30f
    val itemsSizeDp = with(rule.density) { itemsSizePx.toDp() }

    @Test
    fun scroll1ItemScrolledOffItemIsKeptForReuse() {
        lateinit var state: LazyGridState
        rule.setContent {
            state = rememberLazyGridState()
            LazyVerticalGrid(GridCells.Fixed(1), Modifier.height(itemsSizeDp * 1.5f), state) {
                items(100) { Spacer(Modifier.height(itemsSizeDp).fillMaxWidth().testTag("$it")) }
            }
        }

        val id0 = rule.onNodeWithTag("0").semanticsId()
        rule.onNodeWithTag("0").assertIsDisplayed()

        rule.runOnIdle { runBlocking { state.scrollToItem(1) } }

        rule.onRoot().fetchSemanticsNode().assertLayoutDeactivatedById(id0)
        rule.onNodeWithTag("1").assertIsDisplayed()
    }

    @Test
    fun scroll2ItemsScrolledOffItemsAreKeptForReuse() {
        lateinit var state: LazyGridState
        rule.setContent {
            state = rememberLazyGridState()
            LazyVerticalGrid(GridCells.Fixed(1), Modifier.height(itemsSizeDp * 1.5f), state) {
                items(100) { Spacer(Modifier.height(itemsSizeDp).fillMaxWidth().testTag("$it")) }
            }
        }
        // Semantics IDs must be fetched before scrolling.
        val id0 = rule.onNodeWithTag("0").semanticsId()
        val id1 = rule.onNodeWithTag("1").semanticsId()
        rule.onNodeWithTag("0").assertIsDisplayed()
        rule.onNodeWithTag("1").assertIsDisplayed()

        rule.runOnIdle { runBlocking { state.scrollToItem(2) } }

        rule.onRoot().fetchSemanticsNode().assertLayoutDeactivatedById(id0)
        rule.onRoot().fetchSemanticsNode().assertLayoutDeactivatedById(id1)
        rule.onNodeWithTag("2").assertIsDisplayed()
    }

    @Test
    fun checkMaxItemsKeptForReuse() {
        lateinit var state: LazyGridState
        rule.setContent {
            state = rememberLazyGridState()
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
            deactivatedIds.add(rule.onNodeWithTag("$it").semanticsId())
        }

        rule.runOnIdle { runBlocking { state.scrollToItem(DefaultMaxItemsToRetain + 1) } }

        deactivatedIds.fastForEach {
            rule.onRoot().fetchSemanticsNode().assertLayoutDeactivatedById(it)
        }
        rule.onNodeWithTag("$DefaultMaxItemsToRetain").assertDoesNotExist()
        rule.onNodeWithTag("${DefaultMaxItemsToRetain + 1}").assertIsDisplayed()
    }

    @Test
    fun scroll3Items2OfScrolledOffItemsAreKeptForReuse() {
        lateinit var state: LazyGridState
        rule.setContent {
            state = rememberLazyGridState()
            LazyVerticalGrid(GridCells.Fixed(1), Modifier.height(itemsSizeDp * 1.5f), state) {
                items(100) { Spacer(Modifier.height(itemsSizeDp).fillMaxWidth().testTag("$it")) }
            }
        }

        val id0 = rule.onNodeWithTag("0").semanticsId()
        val id1 = rule.onNodeWithTag("1").semanticsId()
        rule.onNodeWithTag("0").assertIsDisplayed()
        rule.onNodeWithTag("1").assertIsDisplayed()

        rule.runOnIdle {
            runBlocking {
                // after this step 0 and 1 are in reusable buffer
                state.scrollToItem(2)

                // this step requires one item and will take the last item from the buffer - item
                // 1 plus will put 2 in the buffer. so expected buffer is items 2 and 0
                state.scrollToItem(3)
            }
        }

        // recycled
        rule.onNodeWithTag("1").assertDoesNotExist()

        // in buffer
        rule.onRoot().fetchSemanticsNode().assertLayoutDeactivatedById(id0)
        rule.onRoot().fetchSemanticsNode().assertLayoutDeactivatedById(id1)

        // visible
        rule.onNodeWithTag("3").assertIsDisplayed()
        rule.onNodeWithTag("4").assertIsDisplayed()
    }

    @Test
    fun doMultipleScrollsOneByOne() {
        lateinit var state: LazyGridState
        rule.setContent {
            state = rememberLazyGridState()
            LazyVerticalGrid(GridCells.Fixed(1), Modifier.height(itemsSizeDp * 1.5f), state) {
                items(100) { Spacer(Modifier.height(itemsSizeDp).fillMaxWidth().testTag("$it")) }
            }
        }
        rule.runOnIdle {
            runBlocking {
                state.scrollToItem(1) // buffer is [0]
                state.scrollToItem(2) // 0 used, buffer is [1]
            }
        }

        // 3 should be visible at this point, so save its ID to check later
        val id3 = rule.onNodeWithTag("3").semanticsId()

        rule.runOnIdle {
            runBlocking {
                state.scrollToItem(3) // 1 used, buffer is [2]
                state.scrollToItem(4) // 2 used, buffer is [3]
            }
        }

        // recycled
        rule.onNodeWithTag("0").assertDoesNotExist()
        rule.onNodeWithTag("1").assertDoesNotExist()
        rule.onNodeWithTag("2").assertDoesNotExist()

        // in buffer
        rule.onRoot().fetchSemanticsNode().assertLayoutDeactivatedById(id3)

        // visible
        rule.onNodeWithTag("4").assertIsDisplayed()
        rule.onNodeWithTag("5").assertIsDisplayed()
    }

    @Test
    fun scrollBackwardOnce() {
        lateinit var state: LazyGridState
        rule.setContent {
            state = rememberLazyGridState(10)
            LazyVerticalGrid(GridCells.Fixed(1), Modifier.height(itemsSizeDp * 1.5f), state) {
                items(100) { Spacer(Modifier.height(itemsSizeDp).fillMaxWidth().testTag("$it")) }
            }
        }

        val id10 = rule.onNodeWithTag("10").semanticsId()
        val id11 = rule.onNodeWithTag("11").semanticsId()

        rule.runOnIdle {
            runBlocking {
                state.scrollToItem(8) // buffer is [10, 11]
            }
        }

        // in buffer
        rule.onRoot().fetchSemanticsNode().assertLayoutDeactivatedById(id10)
        rule.onRoot().fetchSemanticsNode().assertLayoutDeactivatedById(id11)

        // visible
        rule.onNodeWithTag("8").assertIsDisplayed()
        rule.onNodeWithTag("9").assertIsDisplayed()
    }

    @Test
    fun scrollBackwardOneByOne() {
        lateinit var state: LazyGridState
        rule.setContent {
            state = rememberLazyGridState(10)
            LazyVerticalGrid(GridCells.Fixed(1), Modifier.height(itemsSizeDp * 1.5f), state) {
                items(100) { Spacer(Modifier.height(itemsSizeDp).fillMaxWidth().testTag("$it")) }
            }
        }
        rule.runOnIdle {
            runBlocking {
                state.scrollToItem(9) // buffer is [11]
                state.scrollToItem(7) // 11 reused, buffer is [9]
            }
        }
        // 8 should be visible at this point, so save its ID to check later
        val id8 = rule.onNodeWithTag("8").semanticsId()
        rule.runOnIdle {
            runBlocking {
                state.scrollToItem(6) // 9 reused, buffer is [8]
            }
        }

        // in buffer
        rule.onRoot().fetchSemanticsNode().assertLayoutDeactivatedById(id8)

        // visible
        rule.onNodeWithTag("6").assertIsDisplayed()
        rule.onNodeWithTag("7").assertIsDisplayed()
    }

    @Test
    fun scrollingBackReusesTheSameSlot() {
        lateinit var state: LazyGridState
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

        rule.setContent {
            state = rememberLazyGridState()
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
        rule.runOnIdle {
            runBlocking {
                state.scrollToItem(2) // buffer is [0, 1]
            }
        }

        // 2 and 3 should be visible at this point, so save its ID to check later
        val id2 = rule.onNodeWithTag("2").semanticsId()
        val id3 = rule.onNodeWithTag("3").semanticsId()

        rule.runOnIdle {
            runBlocking {
                counter0 = 0
                counter1 = 0
                state.scrollToItem(0) // scrolled back, 0 and 1 are reused back. buffer: [2, 3]
            }
        }

        rule.runOnIdle {
            Truth.assertWithMessage("Item 0 measured $counter0 times, expected 0.")
                .that(counter0)
                .isEqualTo(0)
            Truth.assertWithMessage("Item 1 measured $counter1 times, expected 0.")
                .that(counter1)
                .isEqualTo(0)
        }

        rule.onNodeWithTag("0").assertIsDisplayed()
        rule.onNodeWithTag("1").assertIsDisplayed()

        rule.onRoot().fetchSemanticsNode().assertLayoutDeactivatedById(id2)
        rule.onRoot().fetchSemanticsNode().assertLayoutDeactivatedById(id3)
    }

    @Test
    fun differentContentTypes() {
        lateinit var state: LazyGridState
        val visibleItemsCount = (DefaultMaxItemsToRetain + 1) * 2
        val startOfType1 = DefaultMaxItemsToRetain + 1
        rule.setContent {
            state = rememberLazyGridState()
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
            deactivatedIds.add(rule.onNodeWithTag("$i").semanticsId())
            rule.onNodeWithTag("$i").assertIsDisplayed()
        }
        for (i in startOfType1 until startOfType1 + DefaultMaxItemsToRetain) {
            deactivatedIds.add(rule.onNodeWithTag("$i").fetchSemanticsNode().id)
        }

        rule.runOnIdle { runBlocking { state.scrollToItem(visibleItemsCount) } }

        rule.onNodeWithTag("$visibleItemsCount").assertIsDisplayed()

        // [DefaultMaxItemsToRetain] items of type 0 are left for reuse and 7 items of type 1
        deactivatedIds.fastForEach {
            rule.onRoot().fetchSemanticsNode().assertLayoutDeactivatedById(it)
        }

        rule.onNodeWithTag("$DefaultMaxItemsToRetain").assertDoesNotExist()
        rule.onNodeWithTag("${startOfType1 + DefaultMaxItemsToRetain}").assertDoesNotExist()
    }

    @Test
    fun differentTypesFromDifferentItemCalls() {
        lateinit var state: LazyGridState
        rule.setContent {
            state = rememberLazyGridState()
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

        val id0 = rule.onNodeWithTag("0").semanticsId()
        val id1 = rule.onNodeWithTag("1").semanticsId()

        rule.runOnIdle {
            runBlocking {
                state.scrollToItem(2)
                // now items 0 and 1 are put into reusables
            }
        }

        rule.onRoot().fetchSemanticsNode().assertLayoutDeactivatedById(id0)
        rule.onRoot().fetchSemanticsNode().assertLayoutDeactivatedById(id1)

        rule.runOnIdle {
            runBlocking {
                state.scrollToItem(9)
                // item 10 should reuse slot 1
            }
        }

        rule.onRoot().fetchSemanticsNode().assertLayoutDeactivatedById(id0)
        rule.onNodeWithTag("1").assertDoesNotExist()
        rule.onNodeWithTag("9").assertIsDisplayed()
        rule.onNodeWithTag("10").assertIsDisplayed()
        rule.onNodeWithTag("11").assertIsDisplayed()
    }

    private fun SemanticsNode.assertLayoutDeactivatedById(id: Int) {
        children.fastForEach {
            if (it.id == id) {
                assert(it.layoutInfo.isDeactivated)
            }
        }
    }
}

private val DefaultMaxItemsToRetain = 7
