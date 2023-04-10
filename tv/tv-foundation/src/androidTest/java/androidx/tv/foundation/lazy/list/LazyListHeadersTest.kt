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

package androidx.tv.foundation.lazy.list

import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.requiredSize
import androidx.compose.foundation.layout.width
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.test.SemanticsNodeInteraction
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.assertIsNotDisplayed
import androidx.compose.ui.test.assertLeftPositionInRootIsEqualTo
import androidx.compose.ui.test.assertTopPositionInRootIsEqualTo
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.performTouchInput
import androidx.compose.ui.test.swipeWithVelocity
import androidx.compose.ui.unit.Density
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.filters.LargeTest
import androidx.tv.foundation.ExperimentalTvFoundationApi
import androidx.tv.foundation.PivotOffsets
import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertEquals
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

@OptIn(ExperimentalTvFoundationApi::class)
@LargeTest
@RunWith(AndroidJUnit4::class)
class LazyListHeadersTest {

    private val TvLazyListTag = "TvLazyList"

    @get:Rule
    val rule = createComposeRule()

    @Test
    fun tvLazyColumnShowsHeader_withoutBeyondBoundsItemCount() {
        val items = (1..2).map { it.toString() }
        val firstHeaderTag = "firstHeaderTag"
        val secondHeaderTag = "secondHeaderTag"

        rule.setContent {
            TvLazyColumn(Modifier.height(300.dp), beyondBoundsItemCount = 0) {
                stickyHeader {
                    Spacer(
                        Modifier.height(101.dp).fillParentMaxWidth()
                            .testTag(firstHeaderTag)
                    )
                }

                items(items) {
                    Spacer(Modifier.height(101.dp).fillParentMaxWidth().testTag(it))
                }

                stickyHeader {
                    Spacer(
                        Modifier.height(101.dp).fillParentMaxWidth()
                            .testTag(secondHeaderTag)
                    )
                }
            }
        }

        rule.onNodeWithTag(firstHeaderTag)
            .assertIsDisplayed()

        rule.onNodeWithTag("1")
            .assertIsDisplayed()

        rule.onNodeWithTag("2")
            .assertIsDisplayed()

        rule.onNodeWithTag(secondHeaderTag)
            .assertDoesNotExist()
    }

    @Test
    fun tvLazyColumnPlaceSecondHeader_ifBeyondBoundsItemCountIsUsed() {
        val items = (1..2).map { it.toString() }
        val firstHeaderTag = "firstHeaderTag"
        val secondHeaderTag = "secondHeaderTag"

        rule.setContent {
            TvLazyColumn(Modifier.height(300.dp), beyondBoundsItemCount = 1) {
                stickyHeader {
                    Spacer(
                        Modifier.height(101.dp).fillParentMaxWidth()
                            .testTag(firstHeaderTag)
                    )
                }

                items(items) {
                    Spacer(Modifier.height(101.dp).fillParentMaxWidth().testTag(it))
                }

                stickyHeader {
                    Spacer(
                        Modifier.height(101.dp).fillParentMaxWidth()
                            .testTag(secondHeaderTag)
                    )
                }
            }
        }

        rule.onNodeWithTag(firstHeaderTag)
            .assertIsDisplayed()

        rule.onNodeWithTag("1")
            .assertIsDisplayed()

        rule.onNodeWithTag("2")
            .assertIsDisplayed()

        rule.onNodeWithTag(secondHeaderTag)
            .assertExists()
    }

    @Test
    fun tvLazyColumnShowsHeadersOnScroll() {
        val items = (1..2).map { it.toString() }
        val firstHeaderTag = "firstHeaderTag"
        val secondHeaderTag = "secondHeaderTag"
        lateinit var state: TvLazyListState

        rule.setContentWithTestViewConfiguration {
            TvLazyColumn(
                Modifier.height(300.dp).testTag(TvLazyListTag).border(2.dp, Color.Black),
                rememberTvLazyListState().also { state = it }
            ) {
                stickyHeader {
                    Spacer(
                        Modifier.height(101.dp).fillParentMaxWidth().border(2.dp, Color.Green)
                            .testTag(firstHeaderTag)
                    )
                }

                items(items) {
                    Spacer(Modifier.height(101.dp).fillParentMaxWidth().border(2.dp, Color.Blue)
                        .testTag(it))
                }

                stickyHeader {
                    Spacer(
                        Modifier.height(101.dp).fillParentMaxWidth().border(2.dp, Color.Yellow)
                            .testTag(secondHeaderTag)
                    )
                }
            }
        }

        rule.runOnIdle { runBlocking { state.scrollToItem(1) } }

        rule.onNodeWithTag(firstHeaderTag)
            .assertIsDisplayed()
            .assertTopPositionInRootIsEqualTo(0.dp)

        rule.runOnIdle {
            assertEquals(0, state.layoutInfo.visibleItemsInfo.first().index)
            assertEquals(0, state.layoutInfo.visibleItemsInfo.first().offset)
        }

        rule.onNodeWithTag("2")
            .assertIsDisplayed()

        rule.onNodeWithTag(secondHeaderTag)
            .assertIsDisplayed()
    }

    @Test
    fun tvLazyColumnHeaderIsReplaced() {
        val items = (1..2).map { it.toString() }
        val firstHeaderTag = "firstHeaderTag"
        val secondHeaderTag = "secondHeaderTag"
        lateinit var state: TvLazyListState

        rule.setContentWithTestViewConfiguration {
            state = rememberTvLazyListState()
            TvLazyColumn(
                modifier = Modifier.height(300.dp).testTag(TvLazyListTag),
                state = state
            ) {
                stickyHeader {
                    Spacer(
                        Modifier.height(101.dp).fillParentMaxWidth()
                            .testTag(firstHeaderTag)
                    )
                }

                stickyHeader {
                    Spacer(
                        Modifier.height(101.dp).fillParentMaxWidth()
                            .testTag(secondHeaderTag)
                    )
                }

                items(items) {
                    Spacer(Modifier.height(101.dp).fillParentMaxWidth().testTag(it))
                }
            }
        }

        rule.runOnIdle { runBlocking { state.scrollToItem(1) } }

        rule.onNodeWithTag(firstHeaderTag)
            .assertIsNotDisplayed()

        rule.onNodeWithTag(secondHeaderTag)
            .assertIsDisplayed()

        rule.onNodeWithTag("1")
            .assertIsDisplayed()

        rule.onNodeWithTag("2")
            .assertIsDisplayed()
    }

    @Test
    fun tvLazyRowShowsHeader_withoutOffscreenItens() {
        val items = (1..2).map { it.toString() }
        val firstHeaderTag = "firstHeaderTag"
        val secondHeaderTag = "secondHeaderTag"

        rule.setContent {
            TvLazyRow(Modifier.width(300.dp), beyondBoundsItemCount = 0) {
                stickyHeader {
                    Spacer(
                        Modifier.width(101.dp).fillParentMaxHeight()
                            .testTag(firstHeaderTag)
                    )
                }

                items(items) {
                    Spacer(Modifier.width(101.dp).fillParentMaxHeight().testTag(it))
                }

                stickyHeader {
                    Spacer(
                        Modifier.width(101.dp).fillParentMaxHeight()
                            .testTag(secondHeaderTag)
                    )
                }
            }
        }

        rule.onNodeWithTag(firstHeaderTag)
            .assertIsDisplayed()

        rule.onNodeWithTag("1")
            .assertIsDisplayed()

        rule.onNodeWithTag("2")
            .assertIsDisplayed()

        rule.onNodeWithTag(secondHeaderTag)
            .assertDoesNotExist()
    }

    @Test
    fun tvLazyRowPlaceSecondHeader_ifBeyondBoundsItemCountIsUsed() {
        val items = (1..2).map { it.toString() }
        val firstHeaderTag = "firstHeaderTag"
        val secondHeaderTag = "secondHeaderTag"

        rule.setContent {
            TvLazyRow(Modifier.width(300.dp), beyondBoundsItemCount = 1) {
                stickyHeader {
                    Spacer(
                        Modifier.width(101.dp).fillParentMaxHeight()
                            .testTag(firstHeaderTag)
                    )
                }

                items(items) {
                    Spacer(Modifier.width(101.dp).fillParentMaxHeight().testTag(it))
                }

                stickyHeader {
                    Spacer(
                        Modifier.width(101.dp).fillParentMaxHeight()
                            .testTag(secondHeaderTag)
                    )
                }
            }
        }

        rule.onNodeWithTag(firstHeaderTag)
            .assertIsDisplayed()

        rule.onNodeWithTag("1")
            .assertIsDisplayed()

        rule.onNodeWithTag("2")
            .assertIsDisplayed()

        rule.onNodeWithTag(secondHeaderTag)
            .assertExists()
    }

    @Test
    fun tvLazyRowShowsHeadersOnScroll() {
        val items = (1..2).map { it.toString() }
        val firstHeaderTag = "firstHeaderTag"
        val secondHeaderTag = "secondHeaderTag"
        lateinit var state: TvLazyListState

        rule.setContentWithTestViewConfiguration {
            TvLazyRow(
                Modifier.width(300.dp).testTag(TvLazyListTag),
                rememberTvLazyListState().also { state = it }
            ) {
                stickyHeader {
                    Spacer(
                        Modifier.width(101.dp).fillParentMaxHeight()
                            .testTag(firstHeaderTag)
                    )
                }

                items(items) {
                    Spacer(Modifier.width(101.dp).fillParentMaxHeight().testTag(it))
                }

                stickyHeader {
                    Spacer(
                        Modifier.width(101.dp).fillParentMaxHeight()
                            .testTag(secondHeaderTag)
                    )
                }
            }
        }

        rule.runOnIdle { runBlocking { state.scrollToItem(1) } }

        rule.onNodeWithTag(TvLazyListTag)
            .scrollBy(x = 102.dp, density = rule.density)

        rule.onNodeWithTag(firstHeaderTag)
            .assertIsDisplayed()
            .assertLeftPositionInRootIsEqualTo(0.dp)

        rule.runOnIdle {
            assertEquals(0, state.layoutInfo.visibleItemsInfo.first().index)
            assertEquals(0, state.layoutInfo.visibleItemsInfo.first().offset)
        }

        rule.onNodeWithTag("2")
            .assertIsDisplayed()

        rule.onNodeWithTag(secondHeaderTag)
            .assertIsDisplayed()
    }

    @Test
    fun tvLazyRowHeaderIsReplaced() {
        val items = (1..2).map { it.toString() }
        val firstHeaderTag = "firstHeaderTag"
        val secondHeaderTag = "secondHeaderTag"
        lateinit var state: TvLazyListState

        rule.setContentWithTestViewConfiguration {
            state = rememberTvLazyListState()
            TvLazyRow(modifier = Modifier.width(300.dp).testTag(TvLazyListTag), state = state) {
                stickyHeader {
                    Spacer(
                        Modifier.width(101.dp).fillParentMaxHeight()
                            .testTag(firstHeaderTag)
                    )
                }

                stickyHeader {
                    Spacer(
                        Modifier.width(101.dp).fillParentMaxHeight()
                            .testTag(secondHeaderTag)
                    )
                }

                items(items) {
                    Spacer(Modifier.width(101.dp).fillParentMaxHeight().testTag(it))
                }
            }
        }

        rule.runOnIdle { runBlocking { state.scrollToItem(1) } }

        rule.onNodeWithTag(firstHeaderTag)
            .assertIsNotDisplayed()

        rule.onNodeWithTag(secondHeaderTag)
            .assertIsDisplayed()

        rule.onNodeWithTag("1")
            .assertIsDisplayed()

        rule.onNodeWithTag("2")
            .assertIsDisplayed()
    }

    @Test
    fun headerIsDisplayedWhenItIsFullyInContentPadding() {
        val headerTag = "header"
        val itemIndexPx = 100
        val itemIndexDp = with(rule.density) { itemIndexPx.toDp() }
        lateinit var state: TvLazyListState

        rule.setContent {
            TvLazyColumn(
                Modifier.requiredSize(itemIndexDp * 4),
                state = rememberTvLazyListState().also { state = it },
                contentPadding = PaddingValues(top = itemIndexDp * 2)
            ) {
                stickyHeader {
                    Spacer(Modifier.requiredSize(itemIndexDp).testTag(headerTag))
                }

                items((0..4).toList()) {
                    Spacer(Modifier.requiredSize(itemIndexDp).testTag("$it"))
                }
            }
        }

        rule.runOnIdle {
            runBlocking { state.scrollToItem(1, itemIndexPx / 2) }
        }

        rule.onNodeWithTag(headerTag)
            .assertTopPositionInRootIsEqualTo(itemIndexDp / 2)

        rule.runOnIdle {
            assertEquals(0, state.layoutInfo.visibleItemsInfo.first().index)
            assertEquals(
                itemIndexPx / 2 - /* content padding size */ itemIndexPx * 2,
                state.layoutInfo.visibleItemsInfo.first().offset
            )
        }

        rule.onNodeWithTag("0")
            .assertTopPositionInRootIsEqualTo(itemIndexDp * 3 / 2)
    }
}

@Composable
private fun TvLazyColumn(
    modifier: Modifier = Modifier,
    state: TvLazyListState = rememberTvLazyListState(),
    contentPadding: PaddingValues = PaddingValues(0.dp),
    reverseLayout: Boolean = false,
    verticalArrangement: Arrangement.Vertical =
        if (!reverseLayout) Arrangement.Top else Arrangement.Bottom,
    horizontalAlignment: Alignment.Horizontal = Alignment.Start,
    userScrollEnabled: Boolean = true,
    beyondBoundsItemCount: Int,
    content: TvLazyListScope.() -> Unit
) {
    LazyList(
        modifier = modifier,
        state = state,
        contentPadding = contentPadding,
        horizontalAlignment = horizontalAlignment,
        verticalArrangement = verticalArrangement,
        isVertical = true,
        reverseLayout = reverseLayout,
        userScrollEnabled = userScrollEnabled,
        beyondBoundsItemCount = beyondBoundsItemCount,
        content = content,
        pivotOffsets = PivotOffsets()
    )
}

@Composable
private fun TvLazyRow(
    modifier: Modifier = Modifier,
    state: TvLazyListState = rememberTvLazyListState(),
    contentPadding: PaddingValues = PaddingValues(0.dp),
    reverseLayout: Boolean = false,
    horizontalArrangement: Arrangement.Horizontal =
        if (!reverseLayout) Arrangement.Start else Arrangement.End,
    verticalAlignment: Alignment.Vertical = Alignment.Top,
    userScrollEnabled: Boolean = true,
    beyondBoundsItemCount: Int,
    content: TvLazyListScope.() -> Unit
) {
    LazyList(
        modifier = modifier,
        state = state,
        contentPadding = contentPadding,
        verticalAlignment = verticalAlignment,
        horizontalArrangement = horizontalArrangement,
        isVertical = false,
        reverseLayout = reverseLayout,
        userScrollEnabled = userScrollEnabled,
        beyondBoundsItemCount = beyondBoundsItemCount,
        content = content,
        pivotOffsets = PivotOffsets()
    )
}

internal fun SemanticsNodeInteraction.scrollBy(x: Dp = 0.dp, y: Dp = 0.dp, density: Density) =
    performTouchInput {
        with(density) {
            val touchSlop = TestTouchSlop.toInt()
            val xPx = x.roundToPx()
            val yPx = y.roundToPx()
            val offsetX = if (xPx > 0) xPx + touchSlop else if (xPx < 0) xPx - touchSlop else 0
            val offsetY = if (yPx > 0) yPx + touchSlop else if (yPx < 0) yPx - touchSlop else 0
            swipeWithVelocity(
                start = center,
                end = Offset(center.x - offsetX, center.y - offsetY),
                endVelocity = 0f
            )
        }
    }
