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

package androidx.compose.foundation.lazy.grid

import androidx.compose.foundation.gestures.scrollBy
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.requiredSize
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.list.scrollBy
import androidx.compose.foundation.lazy.list.setContentWithTestViewConfiguration
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.assertIsNotDisplayed
import androidx.compose.ui.test.assertLeftPositionInRootIsEqualTo
import androidx.compose.ui.test.assertTopPositionInRootIsEqualTo
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.dp
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.filters.LargeTest
import kotlinx.coroutines.runBlocking
import org.junit.Assert
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

@LargeTest
@RunWith(AndroidJUnit4::class)
class LazyGridHeadersTest {

    private val LazyGridTag = "LazyGrid"

    @get:Rule val rule = createComposeRule()

    @Test
    fun lazyVerticalGridShowsHeader() {
        val items = (1..6).map { it.toString() }
        val firstHeaderTag = "firstHeaderTag"
        val secondHeaderTag = "secondHeaderTag"

        rule.setContent {
            LazyVerticalGrid(columns = GridCells.Fixed(3), modifier = Modifier.height(300.dp)) {
                stickyHeader {
                    Spacer(Modifier.height(101.dp).fillMaxWidth().testTag(firstHeaderTag))
                }

                items(items) { Spacer(Modifier.height(101.dp).fillMaxWidth().testTag(it)) }

                stickyHeader {
                    Spacer(Modifier.height(101.dp).fillMaxWidth().testTag(secondHeaderTag))
                }
            }
        }

        rule.onNodeWithTag(firstHeaderTag).assertIsDisplayed()

        rule.onNodeWithTag("1").assertIsDisplayed()

        rule.onNodeWithTag("2").assertIsDisplayed()

        rule.onNodeWithTag(secondHeaderTag).assertDoesNotExist()
    }

    @Test
    fun lazyVerticalGridShowsHeadersOnScroll() {
        val items = (1..3).map { it.toString() }
        val firstHeaderTag = "firstHeaderTag"
        val secondHeaderTag = "secondHeaderTag"
        lateinit var state: LazyGridState

        rule.setContentWithTestViewConfiguration {
            LazyVerticalGrid(
                columns = GridCells.Fixed(3),
                modifier = Modifier.height(300.dp).testTag(LazyGridTag),
                state = rememberLazyGridState().also { state = it }
            ) {
                stickyHeader {
                    Spacer(Modifier.height(101.dp).fillMaxWidth().testTag(firstHeaderTag))
                }

                items(items) { Spacer(Modifier.height(101.dp).fillMaxWidth().testTag(it)) }

                stickyHeader {
                    Spacer(Modifier.height(101.dp).fillMaxWidth().testTag(secondHeaderTag))
                }
            }
        }

        rule.onNodeWithTag(LazyGridTag).scrollBy(y = 102.dp, density = rule.density)

        rule
            .onNodeWithTag(firstHeaderTag)
            .assertIsDisplayed()
            .assertTopPositionInRootIsEqualTo(0.dp)

        rule.runOnIdle {
            Assert.assertEquals(0, state.layoutInfo.visibleItemsInfo.first().index)
            Assert.assertEquals(IntOffset.Zero, state.layoutInfo.visibleItemsInfo.first().offset)
        }

        rule.onNodeWithTag("2").assertIsDisplayed()

        rule.onNodeWithTag(secondHeaderTag).assertIsDisplayed()
    }

    @Test
    fun lazyVerticalGridHeaderIsReplaced() {
        val items = (1..6).map { it.toString() }
        val firstHeaderTag = "firstHeaderTag"
        val secondHeaderTag = "secondHeaderTag"

        rule.setContentWithTestViewConfiguration {
            LazyVerticalGrid(
                columns = GridCells.Fixed(3),
                modifier = Modifier.height(300.dp).testTag(LazyGridTag)
            ) {
                stickyHeader {
                    Spacer(Modifier.height(101.dp).fillMaxWidth().testTag(firstHeaderTag))
                }

                stickyHeader {
                    Spacer(Modifier.height(101.dp).fillMaxWidth().testTag(secondHeaderTag))
                }

                items(items) { Spacer(Modifier.height(101.dp).fillMaxWidth().testTag(it)) }
            }
        }

        rule.onNodeWithTag(LazyGridTag).scrollBy(y = 105.dp, density = rule.density)

        rule.onNodeWithTag(firstHeaderTag).assertIsNotDisplayed()

        rule.onNodeWithTag(secondHeaderTag).assertIsDisplayed()

        rule.onNodeWithTag("1").assertIsDisplayed()

        rule.onNodeWithTag("2").assertIsDisplayed()
    }

    @Test
    fun lazyHorizontalGridShowsHeader() {
        val items = (1..6).map { it.toString() }
        val firstHeaderTag = "firstHeaderTag"
        val secondHeaderTag = "secondHeaderTag"

        rule.setContent {
            LazyHorizontalGrid(rows = GridCells.Fixed(3), modifier = Modifier.width(300.dp)) {
                stickyHeader {
                    Spacer(Modifier.width(101.dp).fillMaxHeight().testTag(firstHeaderTag))
                }

                items(items) { Spacer(Modifier.width(101.dp).fillMaxHeight().testTag(it)) }

                stickyHeader {
                    Spacer(Modifier.width(101.dp).fillMaxHeight().testTag(secondHeaderTag))
                }
            }
        }

        rule.onNodeWithTag(firstHeaderTag).assertIsDisplayed()

        rule.onNodeWithTag("1").assertIsDisplayed()

        rule.onNodeWithTag("2").assertIsDisplayed()

        rule.onNodeWithTag(secondHeaderTag).assertDoesNotExist()
    }

    @Test
    fun lazyHorizontalGridShowsHeadersOnScroll() {
        val items = (1..3).map { it.toString() }
        val firstHeaderTag = "firstHeaderTag"
        val secondHeaderTag = "secondHeaderTag"
        lateinit var state: LazyGridState

        rule.setContentWithTestViewConfiguration {
            LazyHorizontalGrid(
                rows = GridCells.Fixed(3),
                modifier = Modifier.width(300.dp).testTag(LazyGridTag),
                state = rememberLazyGridState().also { state = it }
            ) {
                stickyHeader {
                    Spacer(Modifier.width(101.dp).fillMaxHeight().testTag(firstHeaderTag))
                }

                items(items) { Spacer(Modifier.width(101.dp).fillMaxHeight().testTag(it)) }

                stickyHeader {
                    Spacer(Modifier.width(101.dp).fillMaxHeight().testTag(secondHeaderTag))
                }
            }
        }

        rule.onNodeWithTag(LazyGridTag).scrollBy(x = 102.dp, density = rule.density)

        rule
            .onNodeWithTag(firstHeaderTag)
            .assertIsDisplayed()
            .assertLeftPositionInRootIsEqualTo(0.dp)

        rule.runOnIdle {
            Assert.assertEquals(0, state.layoutInfo.visibleItemsInfo.first().index)
            Assert.assertEquals(IntOffset.Zero, state.layoutInfo.visibleItemsInfo.first().offset)
        }

        rule.onNodeWithTag("2").assertIsDisplayed()

        rule.onNodeWithTag(secondHeaderTag).assertIsDisplayed()
    }

    @Test
    fun lazyHorizontalGridHeaderIsReplaced() {
        val items = (1..6).map { it.toString() }
        val firstHeaderTag = "firstHeaderTag"
        val secondHeaderTag = "secondHeaderTag"

        rule.setContentWithTestViewConfiguration {
            LazyHorizontalGrid(
                rows = GridCells.Fixed(3),
                modifier = Modifier.width(300.dp).testTag(LazyGridTag)
            ) {
                stickyHeader {
                    Spacer(Modifier.width(101.dp).fillMaxHeight().testTag(firstHeaderTag))
                }

                stickyHeader {
                    Spacer(Modifier.width(101.dp).fillMaxHeight().testTag(secondHeaderTag))
                }

                items(items) { Spacer(Modifier.width(101.dp).fillMaxHeight().testTag(it)) }
            }
        }

        rule.onNodeWithTag(LazyGridTag).scrollBy(x = 105.dp, density = rule.density)

        rule.onNodeWithTag(firstHeaderTag).assertIsNotDisplayed()

        rule.onNodeWithTag(secondHeaderTag).assertIsDisplayed()

        rule.onNodeWithTag("1").assertIsDisplayed()

        rule.onNodeWithTag("2").assertIsDisplayed()
    }

    @Test
    fun headerIsDisplayedWhenItIsFullyInContentPadding() {
        val headerTag = "header"
        val itemIndexPx = 100
        val itemIndexDp = with(rule.density) { itemIndexPx.toDp() }
        lateinit var state: LazyGridState

        rule.setContent {
            LazyVerticalGrid(
                columns = GridCells.Fixed(3),
                modifier = Modifier.requiredSize(itemIndexDp * 4),
                state = rememberLazyGridState().also { state = it },
                contentPadding = PaddingValues(top = itemIndexDp * 2)
            ) {
                stickyHeader { Spacer(Modifier.requiredSize(itemIndexDp).testTag(headerTag)) }

                items((0..11).toList()) {
                    Spacer(Modifier.requiredSize(itemIndexDp).testTag("$it"))
                }
            }
        }

        rule.runOnIdle { runBlocking { state.scrollToItem(1, itemIndexPx / 2) } }

        rule.onNodeWithTag(headerTag).assertTopPositionInRootIsEqualTo(itemIndexDp / 2)

        rule.runOnIdle {
            Assert.assertEquals(0, state.layoutInfo.visibleItemsInfo.first().index)
            Assert.assertEquals(
                itemIndexPx / 2 - /* content padding size */ itemIndexPx * 2,
                state.layoutInfo.visibleItemsInfo.first().offset.y
            )
        }

        rule.onNodeWithTag("0").assertTopPositionInRootIsEqualTo(itemIndexDp * 3 / 2)
    }

    @Test
    fun lazyVerticalGridShowsHeader2() {
        val firstHeaderTag = "firstHeaderTag"
        val secondHeaderTag = "secondHeaderTag"
        val itemSizeDp = with(rule.density) { 100.toDp() }
        val scrollDistance = 20
        val scrollDistanceDp = with(rule.density) { scrollDistance.toDp() }
        val state = LazyGridState()

        rule.setContent {
            LazyVerticalGrid(
                columns = GridCells.Fixed(3),
                modifier = Modifier.height(itemSizeDp * 3.5f),
                state = state
            ) {
                stickyHeader {
                    Spacer(Modifier.height(itemSizeDp).fillMaxWidth().testTag(firstHeaderTag))
                }
                stickyHeader {
                    Spacer(Modifier.height(itemSizeDp).fillMaxWidth().testTag(secondHeaderTag))
                }

                items(100) {
                    Spacer(Modifier.height(itemSizeDp).fillMaxWidth().testTag(it.toString()))
                }
            }
        }

        rule.runOnIdle { runBlocking { state.scrollBy(scrollDistance.toFloat()) } }

        rule.onNodeWithTag(firstHeaderTag).assertTopPositionInRootIsEqualTo(-scrollDistanceDp)
        rule
            .onNodeWithTag(secondHeaderTag)
            .assertTopPositionInRootIsEqualTo(itemSizeDp - scrollDistanceDp)
        rule.onNodeWithTag("0").assertTopPositionInRootIsEqualTo(itemSizeDp * 2 - scrollDistanceDp)
    }
}
