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
import androidx.compose.foundation.gestures.scrollBy
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.requiredSize
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.text.BasicText
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.testutils.assertPixels
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.onFocusChanged
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.layout.SubcomposeLayout
import androidx.compose.ui.layout.SubcomposeLayoutState
import androidx.compose.ui.layout.SubcomposeSlotReusePolicy
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.test.ExperimentalTestApi
import androidx.compose.ui.test.SemanticsNodeInteraction
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.captureToImage
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performRotaryScrollInput
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.filters.MediumTest
import androidx.test.filters.SdkSuppress
import com.google.common.truth.Truth.assertThat
import com.google.common.truth.Truth.assertWithMessage
import kotlin.test.Test
import kotlinx.coroutines.runBlocking
import org.junit.Rule
import org.junit.runner.RunWith

@MediumTest
@RunWith(AndroidJUnit4::class)
class TransformingLazyColumnTest {
    private val firstItemTag = "firstItemTag"
    private val lastItemTag = "lastItemTag"
    private val lazyListTag = "LazyListTag"

    @get:Rule val rule = createComposeRule()

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
                state = state
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

    @OptIn(ExperimentalTestApi::class)
    private fun testTransformingLazyColumnRotary(
        userScrollEnabled: Boolean,
        scrollTarget: Int,
        itemsToScroll: Int = 2
    ) {
        lateinit var state: TransformingLazyColumnState
        val itemSizePx = 50
        var itemSizeDp: Dp
        rule.setContent {
            with(rule.density) { itemSizeDp = itemSizePx.toDp() }
            state = rememberTransformingLazyColumnState()
            TransformingLazyColumn(
                state = state,
                modifier = Modifier.testTag(lazyListTag),
                userScrollEnabled = userScrollEnabled
            ) {
                items(100) {
                    BasicText(text = "item $it", modifier = Modifier.requiredSize(itemSizeDp))
                }
            }
        }
        rule.onNodeWithTag(lazyListTag).performRotaryScrollInput {
            // try to scroll by N items
            rotateToScrollVertically(itemSizePx.toFloat() * itemsToScroll)
        }
        rule.waitForIdle()

        assertThat(state.anchorItemIndex).isEqualTo(scrollTarget)
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
