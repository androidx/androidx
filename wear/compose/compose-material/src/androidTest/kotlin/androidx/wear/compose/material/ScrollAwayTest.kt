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

package androidx.wear.compose.material

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyListState
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.runtime.Composable
import androidx.compose.testutils.WithTouchSlop
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.assertIsNotDisplayed
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.performTouchInput
import androidx.compose.ui.test.swipeUp
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.filters.MediumTest
import androidx.wear.compose.foundation.lazy.AutoCenteringParams
import androidx.wear.compose.foundation.lazy.ScalingLazyColumn
import androidx.wear.compose.foundation.lazy.ScalingLazyListState
import androidx.wear.compose.foundation.lazy.rememberScalingLazyListState
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

@MediumTest
@RunWith(AndroidJUnit4::class)
public class ScrollAwayTest {
    @get:Rule
    val rule = createComposeRule()

    @Test
    fun hidesTimeTextWithScalingLazyColumn() {
        lateinit var scrollState: ScalingLazyListState
        rule.setContentWithTheme {
            scrollState =
                rememberScalingLazyListState(
                    initialCenterItemIndex = 1,
                    initialCenterItemScrollOffset = 0
                )
            ScalingLazyColumnTest(itemIndex = 1, offset = 0.dp, scrollState)
        }

        rule.onNodeWithTag(SCROLL_TAG).performTouchInput { swipeUp() }

        rule.onNodeWithTag(TIME_TEXT_TAG).assertIsNotDisplayed()
    }

    @Test
    fun showsTimeTextWithScalingLazyColumnIfItemIndexInvalid() {
        val scrollAwayItemIndex = 10
        lateinit var scrollState: ScalingLazyListState
        rule.setContentWithTheme {
            scrollState =
                rememberScalingLazyListState(
                    initialCenterItemIndex = 1,
                    initialCenterItemScrollOffset = 0
                )
            ScalingLazyColumnTest(itemIndex = scrollAwayItemIndex, offset = 0.dp, scrollState)
        }

        rule.onNodeWithTag(SCROLL_TAG).performTouchInput { swipeUp() }

        // b/256166359 - itemIndex > number of items in the list.
        // ScrollAway should default to always showing TimeText
        rule.onNodeWithTag(TIME_TEXT_TAG).assertIsDisplayed()
    }

    @Suppress("DEPRECATION")
    @Test
    fun hidesTimeTextWithMaterialScalingLazyColumn() {
        lateinit var scrollState: androidx.wear.compose.material.ScalingLazyListState
        rule.setContentWithTheme {
            scrollState =
                androidx.wear.compose.material.rememberScalingLazyListState(
                    initialCenterItemIndex = 1,
                    initialCenterItemScrollOffset = 0
                )
            MaterialScalingLazyColumnTest(itemIndex = 1, offset = 0.dp, scrollState)
        }

        rule.onNodeWithTag(SCROLL_TAG).performTouchInput { swipeUp() }

        rule.onNodeWithTag(TIME_TEXT_TAG).assertIsNotDisplayed()
    }

    @Suppress("DEPRECATION")
    @Test
    fun showsTimeTextWithMaterialScalingLazyColumnIfItemIndexInvalid() {
        val scrollAwayItemIndex = 10
        lateinit var scrollState: androidx.wear.compose.material.ScalingLazyListState
        rule.setContentWithTheme {
            scrollState =
                androidx.wear.compose.material.rememberScalingLazyListState(
                    initialCenterItemIndex = 1,
                    initialCenterItemScrollOffset = 0
                )
            MaterialScalingLazyColumnTest(
                itemIndex = scrollAwayItemIndex,
                offset = 0.dp,
                scrollState
            )
        }

        rule.onNodeWithTag(SCROLL_TAG).performTouchInput { swipeUp() }

        // b/256166359 - itemIndex > number of items in the list.
        // ScrollAway should default to always showing TimeText
        rule.onNodeWithTag(TIME_TEXT_TAG).assertIsDisplayed()
    }

    @Composable
    private fun ScalingLazyColumnTest(
        itemIndex: Int,
        offset: Dp,
        scrollState: ScalingLazyListState
    ) {
        WithTouchSlop(0f) {
            Scaffold(
                modifier = Modifier
                    .fillMaxSize()
                    .background(MaterialTheme.colors.background),
                timeText = {
                    TimeText(
                        modifier = Modifier
                            .scrollAway(
                                scrollState = scrollState,
                                itemIndex = itemIndex,
                                offset = offset,
                            )
                            .testTag(TIME_TEXT_TAG)
                    )
                },
            ) {
                ScalingLazyColumn(
                    contentPadding = PaddingValues(10.dp),
                    state = scrollState,
                    autoCentering = AutoCenteringParams(itemIndex = 1, itemOffset = 0),
                    modifier = Modifier.testTag(SCROLL_TAG)
                ) {
                    item {
                        ListHeader { Text("Chips") }
                    }

                    items(5) { i ->
                        ChipTest(Modifier.fillParentMaxHeight(0.5f), i)
                    }
                }
            }
        }
    }

    @Suppress("DEPRECATION")
    @Composable
    private fun MaterialScalingLazyColumnTest(
        itemIndex: Int,
        offset: Dp,
        scrollState: androidx.wear.compose.material.ScalingLazyListState
    ) {
        WithTouchSlop(0f) {
            Scaffold(
                modifier = Modifier
                    .fillMaxSize()
                    .background(MaterialTheme.colors.background),
                timeText = {
                    TimeText(
                        modifier = Modifier
                            .scrollAway(
                                scrollState = scrollState,
                                itemIndex = itemIndex,
                                offset = offset,
                            )
                            .testTag(TIME_TEXT_TAG)
                    )
                },
            ) {
                ScalingLazyColumn(
                    contentPadding = PaddingValues(10.dp),
                    state = scrollState,
                    autoCentering = androidx.wear.compose.material.AutoCenteringParams(
                        itemIndex = 1, itemOffset = 0
                    ),
                    modifier = Modifier.testTag(SCROLL_TAG)
                ) {
                    item {
                        ListHeader { Text("Chips") }
                    }

                    items(5) { i ->
                        ChipTest(Modifier.fillParentMaxHeight(0.5f), i)
                    }
                }
            }
        }
    }

    @Test
    fun hidesTimeTextWithLazyColumn() {
        lateinit var scrollState: LazyListState
        rule.setContentWithTheme {
            scrollState =
                rememberLazyListState(
                    initialFirstVisibleItemIndex = 1,
                )

            LazyColumnTest(itemIndex = 1, offset = 0.dp, scrollState)
        }

        rule.onNodeWithTag(SCROLL_TAG).performTouchInput { swipeUp() }

        rule.onNodeWithTag(TIME_TEXT_TAG).assertIsNotDisplayed()
    }

    @Test
    fun showsTimeTextWithLazyColumnIfItemIndexInvalid() {
        val scrollAwayItemIndex = 10
        lateinit var scrollState: LazyListState
        rule.setContentWithTheme {
            scrollState =
                rememberLazyListState(
                    initialFirstVisibleItemIndex = 1,
                )
            LazyColumnTest(
                itemIndex = scrollAwayItemIndex, offset = 0.dp, scrollState
            )
        }

        rule.onNodeWithTag(SCROLL_TAG).performTouchInput { swipeUp() }

        // b/256166359 - itemIndex > number of items in the list.
        // ScrollAway should default to always showing TimeText
        rule.onNodeWithTag(TIME_TEXT_TAG).assertIsDisplayed()
    }

    @Composable
    private fun LazyColumnTest(
        itemIndex: Int,
        offset: Dp,
        scrollState: LazyListState
    ) {
        WithTouchSlop(0f) {
            Scaffold(
                modifier = Modifier
                    .fillMaxSize()
                    .background(MaterialTheme.colors.background),
                timeText = {
                    TimeText(
                        modifier = Modifier
                            .scrollAway(
                                scrollState = scrollState,
                                itemIndex = itemIndex,
                                offset = offset,
                            )
                            .testTag(TIME_TEXT_TAG)
                    )
                },
            ) {
                LazyColumn(
                    state = scrollState,
                    modifier = Modifier.testTag(SCROLL_TAG)
                ) {
                    item {
                        ListHeader { Text("Chips") }
                    }
                    items(5) { i ->
                        ChipTest(Modifier.fillParentMaxHeight(0.5f), i)
                    }
                }
            }
        }
    }

    @Composable
    private fun ChipTest(modifier: Modifier, i: Int) {
        Chip(
            modifier = modifier,
            onClick = { },
            colors = ChipDefaults.primaryChipColors(),
            border = ChipDefaults.chipBorder()
        ) {
            Text(text = "Chip $i")
        }
    }
}

private const val SCROLL_TAG = "ScrollTag"
private const val TIME_TEXT_TAG = "TimeTextTag"
