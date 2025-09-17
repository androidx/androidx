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

package androidx.compose.foundation.lazy.grid

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.ParentCapturingModifierNodeElement
import androidx.compose.foundation.lazy.list.PlacementComparator
import androidx.compose.foundation.lazy.list.TrackPlacedElement
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.getValue
import androidx.compose.runtime.key
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.testutils.ParameterizedComposeTestRule
import androidx.compose.testutils.createParameterizedComposeTestRule
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.layout.BeyondBoundsLayout
import androidx.compose.ui.layout.BeyondBoundsLayout.LayoutDirection.Companion.Above
import androidx.compose.ui.layout.BeyondBoundsLayout.LayoutDirection.Companion.After
import androidx.compose.ui.layout.BeyondBoundsLayout.LayoutDirection.Companion.Before
import androidx.compose.ui.layout.BeyondBoundsLayout.LayoutDirection.Companion.Below
import androidx.compose.ui.layout.BeyondBoundsLayout.LayoutDirection.Companion.Left
import androidx.compose.ui.layout.BeyondBoundsLayout.LayoutDirection.Companion.Right
import androidx.compose.ui.platform.LocalLayoutDirection
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.LayoutDirection
import androidx.compose.ui.unit.LayoutDirection.Ltr
import androidx.compose.ui.unit.LayoutDirection.Rtl
import androidx.test.filters.MediumTest
import com.google.common.truth.Truth.assertThat
import kotlinx.coroutines.runBlocking
import org.junit.Rule
import org.junit.Test

@MediumTest
class LazyGridBeyondBoundsTest {

    @get:Rule val rule = createParameterizedComposeTestRule<Param>()

    // We need to wrap the inline class parameter in another class because Java can't instantiate
    // the inline class.
    class Param(
        val beyondBoundsLayoutDirection: BeyondBoundsLayout.LayoutDirection,
        val reverseLayout: Boolean,
        val layoutDirection: LayoutDirection,
    ) {
        override fun toString() =
            "beyondBoundsLayoutDirection=$beyondBoundsLayoutDirection " +
                "reverseLayout=$reverseLayout " +
                "layoutDirection=$layoutDirection"

        internal fun placementComparator(): PlacementComparator {
            return PlacementComparator(beyondBoundsLayoutDirection, layoutDirection, reverseLayout)
        }
    }

    private val placedItems = sortedMapOf<Int, Rect>()
    private var beyondBoundsLayout: BeyondBoundsLayout? = null
    private lateinit var lazyGridState: LazyGridState

    companion object {

        val ParamsToTest = buildList {
            for (beyondBoundsLayoutDirection in listOf(Left, Right, Above, Below, Before, After)) {
                for (reverseLayout in listOf(false, true)) {
                    for (layoutDirection in listOf(Ltr, Rtl)) {
                        add(Param(beyondBoundsLayoutDirection, reverseLayout, layoutDirection))
                    }
                }
            }
        }
    }

    private fun resetTestCase(firstVisibleItem: Int = 0) {
        rule.runOnIdle { runBlocking { lazyGridState.scrollToItem(firstVisibleItem) } }
        placedItems.clear()
        beyondBoundsLayout = null
    }

    @Test
    fun onlyOneVisibleItemIsPlaced() =
        with(rule) {
            // Arrange.
            setLazyContent(size = 10.toDp(), firstVisibleItem = 0) {
                items(100) { index -> Box(Modifier.size(10.toDp()).trackPlaced(index)) }
            }
            forEachParameter(ParamsToTest) { _ ->
                // Assert.
                runOnIdle {
                    assertThat(placedItems.keys).containsExactly(0)
                    assertThat(visibleItems).containsExactly(0)
                }
                resetTestCase()
            }
        }

    @Test
    fun onlyTwoVisibleItemsArePlaced() =
        with(rule) {
            // Arrange.
            setLazyContent(size = 20.toDp(), firstVisibleItem = 0) {
                items(100) { index -> Box(Modifier.size(10.toDp()).trackPlaced(index)) }
            }

            forEachParameter(ParamsToTest) { _ ->
                // Assert.
                runOnIdle {
                    assertThat(placedItems.keys).containsExactly(0, 1)
                    assertThat(visibleItems).containsExactly(0, 1)
                }
                resetTestCase()
            }
        }

    @Test
    fun onlyThreeVisibleItemsArePlaced() =
        with(rule) {
            // Arrange.
            setLazyContent(size = 30.toDp(), firstVisibleItem = 0) {
                items(100) { index -> Box(Modifier.size(10.toDp()).trackPlaced(index)) }
            }

            forEachParameter(ParamsToTest) { _ ->
                // Assert.
                runOnIdle {
                    assertThat(placedItems.keys).containsExactly(0, 1, 2)
                    assertThat(visibleItems).containsExactly(0, 1, 2)
                }
                resetTestCase()
            }
        }

    @Test
    fun emptyLazyList_doesNotCrash() =
        with(rule) {
            // Arrange.
            var addItems by mutableStateOf(true)
            lateinit var beyondBoundsLayoutRef: BeyondBoundsLayout
            setLazyContent(size = 30.toDp(), firstVisibleItem = 0) {
                if (addItems) {
                    item {
                        Box(
                            Modifier.then(
                                ParentCapturingModifierNodeElement { beyondBoundsLayout = it }
                            )
                        )
                    }
                }
            }

            forEachParameter(ParamsToTest) { param ->
                runOnIdle {
                    beyondBoundsLayoutRef = beyondBoundsLayout!!
                    addItems = false
                }

                // Act.
                val hasMoreContent = runOnIdle {
                    beyondBoundsLayoutRef.layout(param.beyondBoundsLayoutDirection) {
                        hasMoreContent
                    }
                }

                // Assert.
                runOnIdle { assertThat(hasMoreContent).isFalse() }
                resetTestCase()
                addItems = true
            }
        }

    @Test
    fun oneExtraItemBeyondVisibleBounds() =
        with(rule) {
            // Arrange.
            setLazyContent(size = 30.toDp(), firstVisibleItem = 5) {
                items(5) { index -> Box(Modifier.size(10.toDp()).trackPlaced(index)) }
                item {
                    Box(
                        Modifier.size(10.toDp())
                            .trackPlaced(5)
                            .then(ParentCapturingModifierNodeElement { beyondBoundsLayout = it })
                    )
                }
                items(5) { index -> Box(Modifier.size(10.toDp()).trackPlaced(index + 6)) }
            }

            forEachParameter(ParamsToTest) { param ->
                // Act.
                runOnUiThread {
                    beyondBoundsLayout!!.layout(param.beyondBoundsLayoutDirection) {
                        // Assert that the beyond bounds items are present.
                        if (param.expectedExtraItemsBeforeVisibleBounds()) {
                            assertThat(placedItems.keys).containsExactly(4, 5, 6, 7)
                        } else {
                            assertThat(placedItems.keys).containsExactly(5, 6, 7, 8)
                        }
                        assertThat(visibleItems).containsExactly(5, 6, 7)

                        assertThat(placedItems.values).isInOrder(param.placementComparator())

                        // Just return true so that we stop as soon as we run this once.
                        // This should result in one extra item being added.
                        true
                    }
                }

                // Assert that the beyond bounds items are removed.
                runOnIdle {
                    assertThat(placedItems.keys).containsExactly(5, 6, 7)
                    assertThat(visibleItems).containsExactly(5, 6, 7)
                }
                resetTestCase(5)
            }
        }

    @Test
    fun oneExtraItemBeyondVisibleBounds_multipleCells() =
        with(rule) {
            val itemSize = 50
            val itemSizeDp = itemSize.toDp()
            // Arrange.
            setLazyContent(cells = 2, size = itemSizeDp * 3, firstVisibleItem = 10) {
                // item | item  | x5
                // item | local | x1
                // item | item  | x5
                items(11) { index -> Box(Modifier.size(itemSizeDp).trackPlaced(index)) }
                item {
                    Box(
                        Modifier.size(itemSizeDp)
                            .trackPlaced(11)
                            .then(ParentCapturingModifierNodeElement { beyondBoundsLayout = it })
                    )
                }
                items(10) { index -> Box(Modifier.size(itemSizeDp).trackPlaced(index + 12)) }
            }

            forEachParameter(ParamsToTest) { param ->
                // Act.
                runOnUiThread {
                    beyondBoundsLayout!!.layout(param.beyondBoundsLayoutDirection) {
                        // Assert that the beyond bounds items are present.
                        if (param.expectedExtraItemsBeforeVisibleBounds()) {
                            assertThat(placedItems.keys).containsExactly(9, 10, 11, 12, 13, 14, 15)
                        } else {
                            assertThat(placedItems.keys).containsExactly(10, 11, 12, 13, 14, 15, 16)
                        }
                        assertThat(visibleItems).containsExactly(10, 11, 12, 13, 14, 15)

                        assertThat(placedItems.values).isInOrder(param.placementComparator())

                        // Just return true so that we stop as soon as we run this once.
                        // This should result in one extra item being added.
                        true
                    }
                }

                // Assert that the beyond bounds items are removed.
                runOnIdle {
                    assertThat(placedItems.keys).containsExactly(10, 11, 12, 13, 14, 15)
                    assertThat(visibleItems).containsExactly(10, 11, 12, 13, 14, 15)
                }
                resetTestCase(10)
            }
        }

    @Test
    fun twoExtraItemsBeyondVisibleBounds() =
        with(rule) {
            // Arrange.
            setLazyContent(size = 30.toDp(), firstVisibleItem = 5) {
                items(5) { index -> Box(Modifier.size(10.toDp()).trackPlaced(index)) }
                item {
                    Box(
                        Modifier.size(10.toDp())
                            .trackPlaced(5)
                            .then(ParentCapturingModifierNodeElement { beyondBoundsLayout = it })
                    )
                }
                items(5) { index -> Box(Modifier.size(10.toDp()).trackPlaced(index + 6)) }
            }

            forEachParameter(ParamsToTest) { param ->
                var extraItemCount = 2
                // Act.
                runOnUiThread {
                    beyondBoundsLayout!!.layout(param.beyondBoundsLayoutDirection) {
                        if (--extraItemCount > 0) {
                            // Return null to continue the search.
                            null
                        } else {
                            // Assert that the beyond bounds items are present.
                            if (param.expectedExtraItemsBeforeVisibleBounds()) {
                                assertThat(placedItems.keys).containsExactly(3, 4, 5, 6, 7)
                            } else {
                                assertThat(placedItems.keys).containsExactly(5, 6, 7, 8, 9)
                            }
                            assertThat(visibleItems).containsExactly(5, 6, 7)

                            assertThat(placedItems.values).isInOrder(param.placementComparator())

                            // Return true to stop the search.
                            true
                        }
                    }
                }

                // Assert that the beyond bounds items are removed.
                runOnIdle {
                    assertThat(placedItems.keys).containsExactly(5, 6, 7)
                    assertThat(visibleItems).containsExactly(5, 6, 7)
                }
                resetTestCase(5)
            }
        }

    @Test
    fun allBeyondBoundsItemsInSpecifiedDirection() =
        with(rule) {
            // Arrange.
            setLazyContent(size = 30.toDp(), firstVisibleItem = 5) {
                items(5) { index -> Box(Modifier.size(10.toDp()).trackPlaced(index)) }
                item {
                    Box(
                        Modifier.size(10.toDp())
                            .then(ParentCapturingModifierNodeElement { beyondBoundsLayout = it })
                            .trackPlaced(5)
                    )
                }
                items(5) { index -> Box(Modifier.size(10.toDp()).trackPlaced(index + 6)) }
            }

            forEachParameter(ParamsToTest) { param ->
                // Act.
                runOnUiThread {
                    beyondBoundsLayout!!.layout(param.beyondBoundsLayoutDirection) {
                        if (hasMoreContent) {
                            // Just return null so that we keep adding more items till we reach the
                            // end.
                            null
                        } else {
                            // Assert that the beyond bounds items are present.
                            if (param.expectedExtraItemsBeforeVisibleBounds()) {
                                assertThat(placedItems.keys).containsExactly(0, 1, 2, 3, 4, 5, 6, 7)
                            } else {
                                assertThat(placedItems.keys).containsExactly(5, 6, 7, 8, 9, 10)
                            }
                            assertThat(visibleItems).containsExactly(5, 6, 7)

                            assertThat(placedItems.values).isInOrder(param.placementComparator())

                            // Return true to end the search.
                            true
                        }
                    }
                }

                // Assert that the beyond bounds items are removed.
                runOnIdle { assertThat(placedItems.keys).containsExactly(5, 6, 7) }
                resetTestCase(5)
            }
        }

    @Test
    fun beyondBoundsLayoutRequest_inDirectionPerpendicularToLazyListOrientation() =
        with(rule) {
            // Arrange.
            setLazyContentInPerpendicularDirection(size = 30.toDp(), firstVisibleItem = 5) {
                items(5) { index -> Box(Modifier.size(10.toDp()).trackPlaced(index)) }
                item {
                    Box(
                        Modifier.size(10.toDp())
                            .trackPlaced(5)
                            .then(ParentCapturingModifierNodeElement { beyondBoundsLayout = it })
                    )
                }
                items(5) { index -> Box(Modifier.size(10.toDp()).trackPlaced(index + 6)) }
            }

            forEachParameter(ParamsToTest) { param ->
                var beyondBoundsLayoutCount = 0
                runOnIdle {
                    assertThat(placedItems.keys).containsExactly(5, 6, 7)
                    assertThat(visibleItems).containsExactly(5, 6, 7)
                }

                // Act.
                runOnUiThread {
                    beyondBoundsLayout!!.layout(param.beyondBoundsLayoutDirection) {
                        beyondBoundsLayoutCount++
                        when (param.beyondBoundsLayoutDirection) {
                            Left,
                            Right,
                            Above,
                            Below -> {
                                assertThat(placedItems.keys).containsExactly(5, 6, 7)
                                assertThat(visibleItems).containsExactly(5, 6, 7)
                            }
                            Before,
                            After -> {
                                if (param.expectedExtraItemsBeforeVisibleBounds()) {
                                    assertThat(placedItems.keys).containsExactly(4, 5, 6, 7)
                                    assertThat(visibleItems).containsExactly(5, 6, 7)
                                } else {
                                    assertThat(placedItems.keys).containsExactly(5, 6, 7, 8)
                                    assertThat(visibleItems).containsExactly(5, 6, 7)
                                }
                            }
                        }
                        // Just return true so that we stop as soon as we run this once.
                        // This should result in one extra item being added.
                        true
                    }
                }

                runOnIdle {
                    when (param.beyondBoundsLayoutDirection) {
                        Left,
                        Right,
                        Above,
                        Below -> {
                            assertThat(beyondBoundsLayoutCount).isEqualTo(0)
                        }
                        Before,
                        After -> {
                            assertThat(beyondBoundsLayoutCount).isEqualTo(1)

                            // Assert that the beyond bounds items are removed.
                            assertThat(placedItems.keys).containsExactly(5, 6, 7)
                            assertThat(visibleItems).containsExactly(5, 6, 7)
                        }
                        else -> error("Unsupported BeyondBoundsLayoutDirection")
                    }
                }
                resetTestCase(5)
            }
        }

    @Test
    fun returningNullDoesNotCauseInfiniteLoop() =
        with(rule) {
            // Arrange.
            setLazyContent(size = 30.toDp(), firstVisibleItem = 5) {
                items(5) { index -> Box(Modifier.size(10.toDp()).trackPlaced(index)) }
                item {
                    Box(
                        Modifier.size(10.toDp())
                            .then(ParentCapturingModifierNodeElement { beyondBoundsLayout = it })
                            .trackPlaced(5)
                    )
                }
                items(5) { index -> Box(Modifier.size(10.toDp()).trackPlaced(index + 6)) }
            }

            forEachParameter(ParamsToTest) { param ->
                // Act.
                var count = 0
                runOnUiThread {
                    beyondBoundsLayout!!.layout(param.beyondBoundsLayoutDirection) {
                        // Assert that we don't keep iterating when there is no ending condition.
                        assertThat(count++).isLessThan(lazyGridState.layoutInfo.totalItemsCount)
                        // Always return null to continue the search.
                        null
                    }
                }

                // Assert that the beyond bounds items are removed.
                runOnIdle {
                    assertThat(placedItems.keys).containsExactly(5, 6, 7)
                    assertThat(visibleItems).containsExactly(5, 6, 7)
                }
                resetTestCase(5)
            }
        }

    private fun ParameterizedComposeTestRule<Param>.setLazyContent(
        size: Dp,
        firstVisibleItem: Int,
        cells: Int = 1,
        content: LazyGridScope.() -> Unit,
    ) {
        setContent {
            key(it) {
                CompositionLocalProvider(LocalLayoutDirection provides it.layoutDirection) {
                    lazyGridState = rememberLazyGridState(firstVisibleItem)
                    when (it.beyondBoundsLayoutDirection) {
                        Left,
                        Right,
                        Before,
                        After ->
                            LazyHorizontalGrid(
                                rows = GridCells.Fixed(cells),
                                modifier = Modifier.size(size),
                                state = lazyGridState,
                                reverseLayout = it.reverseLayout,
                                content = content,
                            )
                        Above,
                        Below ->
                            LazyVerticalGrid(
                                columns = GridCells.Fixed(cells),
                                modifier = Modifier.size(size),
                                state = lazyGridState,
                                reverseLayout = it.reverseLayout,
                                content = content,
                            )
                        else -> unsupportedDirection()
                    }
                }
            }
        }
    }

    private fun ParameterizedComposeTestRule<Param>.setLazyContentInPerpendicularDirection(
        size: Dp,
        firstVisibleItem: Int,
        content: LazyGridScope.() -> Unit,
    ) {
        setContent {
            key(it) {
                CompositionLocalProvider(LocalLayoutDirection provides it.layoutDirection) {
                    lazyGridState = rememberLazyGridState(firstVisibleItem)
                    when (it.beyondBoundsLayoutDirection) {
                        Left,
                        Right,
                        Before,
                        After ->
                            LazyVerticalGrid(
                                columns = GridCells.Fixed(1),
                                modifier = Modifier.size(size),
                                state = lazyGridState,
                                reverseLayout = it.reverseLayout,
                                content = content,
                            )
                        Above,
                        Below ->
                            LazyHorizontalGrid(
                                rows = GridCells.Fixed(1),
                                modifier = Modifier.size(size),
                                state = lazyGridState,
                                reverseLayout = it.reverseLayout,
                                content = content,
                            )
                        else -> unsupportedDirection()
                    }
                }
            }
        }
    }

    private fun Int.toDp(): Dp = with(rule.density) { toDp() }

    private val visibleItems: List<Int>
        get() = lazyGridState.layoutInfo.visibleItemsInfo.map { it.index }

    private fun Param.expectedExtraItemsBeforeVisibleBounds() =
        when (beyondBoundsLayoutDirection) {
            Right -> if (layoutDirection == Ltr) reverseLayout else !reverseLayout
            Left -> if (layoutDirection == Ltr) !reverseLayout else reverseLayout
            Above -> !reverseLayout
            Below -> reverseLayout
            After -> false
            Before -> true
            else -> error("Unsupported BeyondBoundsDirection")
        }

    private fun unsupportedDirection(): Nothing =
        error("Lazy list does not support beyond bounds layout for the specified direction")

    private fun Modifier.trackPlaced(index: Int): Modifier =
        this then TrackPlacedElement(index, placedItems)
}
