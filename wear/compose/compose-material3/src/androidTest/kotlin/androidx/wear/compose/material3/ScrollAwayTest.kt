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

package androidx.wear.compose.material3

import android.os.Build
import androidx.annotation.RequiresApi
import androidx.compose.foundation.ScrollState
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyListState
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.runtime.Composable
import androidx.compose.testutils.WithTouchSlop
import androidx.compose.testutils.assertContainsColor
import androidx.compose.testutils.assertDoesNotContainColor
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.captureToImage
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.performTouchInput
import androidx.compose.ui.test.swipeUp
import androidx.compose.ui.unit.dp
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.filters.MediumTest
import androidx.wear.compose.foundation.ScrollInfoProvider
import androidx.wear.compose.foundation.lazy.AutoCenteringParams
import androidx.wear.compose.foundation.lazy.ScalingLazyColumn
import androidx.wear.compose.foundation.lazy.ScalingLazyListState
import androidx.wear.compose.foundation.lazy.rememberScalingLazyListState
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

@MediumTest
@RunWith(AndroidJUnit4::class)
class ScrollAwayTest {
    @get:Rule val rule = createComposeRule()

    @RequiresApi(Build.VERSION_CODES.O)
    @Test
    fun showsTimeTextWithScalingLazyColumnInitially() {
        val timeTextColor = Color.Red

        rule.setContentWithTheme {
            val scrollState = rememberScalingLazyListState()
            ScalingLazyColumnTest(scrollState, timeTextColor)
        }

        rule.onNodeWithTag(TEST_TAG).captureToImage().assertContainsColor(timeTextColor)
    }

    @Test
    fun showsTimeTextWithScalingLazyColumnIfItemIndexInvalid() {
        val timeTextColor = Color.Red
        lateinit var scrollState: ScalingLazyListState
        rule.setContentWithTheme {
            scrollState =
                rememberScalingLazyListState(
                    initialCenterItemIndex = 1,
                    initialCenterItemScrollOffset = 0
                )
            ScalingLazyColumnTest(scrollState, itemIndex = 100, timeTextColor = timeTextColor)
        }

        rule.onNodeWithTag(SCROLL_TAG).performTouchInput { swipeUp() }

        rule.onNodeWithTag(TIME_TEXT_TAG).assertIsDisplayed()
    }

    @RequiresApi(Build.VERSION_CODES.O)
    @Test
    fun showsTimeTextWithLazyColumnInitially() {
        val timeTextColor = Color.Red

        rule.setContentWithTheme {
            val scrollState = rememberLazyListState()
            LazyColumnTest(scrollState, timeTextColor)
        }

        rule.onNodeWithTag(TEST_TAG).captureToImage().assertContainsColor(timeTextColor)
    }

    @RequiresApi(Build.VERSION_CODES.O)
    @Test
    fun showsTimeTextWithColumnInitially() {
        val timeTextColor = Color.Red

        rule.setContentWithTheme {
            val scrollState = rememberLazyListState()
            LazyColumnTest(scrollState, timeTextColor)
        }

        rule.onNodeWithTag(TEST_TAG).captureToImage().assertContainsColor(timeTextColor)
    }

    @RequiresApi(Build.VERSION_CODES.O)
    @Test
    fun hidesTimeTextAfterScrollingScalingLazyColumn() {
        val timeTextColor = Color.Red
        rule.setContentWithTheme {
            val scrollState = rememberScalingLazyListState()
            ScalingLazyColumnTest(scrollState, timeTextColor = timeTextColor)
        }

        rule.onNodeWithTag(SCROLL_TAG).performTouchInput { swipeUp() }

        // Allow slight delay for TimeText to scroll off, but not long enough for it to come
        // back onto the screen after the idle timeout.
        rule.mainClock.autoAdvance = false
        rule.mainClock.advanceTimeBy(500)
        rule.onNodeWithTag(TEST_TAG).captureToImage().assertDoesNotContainColor(timeTextColor)
    }

    @RequiresApi(Build.VERSION_CODES.O)
    @Test
    fun hidesTimeTextWithLazyColumn() {
        val timeTextColor = Color.Red
        lateinit var scrollState: LazyListState
        rule.setContentWithTheme {
            scrollState =
                rememberLazyListState(
                    initialFirstVisibleItemIndex = 1,
                )

            LazyColumnTest(scrollState, timeTextColor = timeTextColor)
        }

        rule.onNodeWithTag(SCROLL_TAG).performTouchInput { swipeUp() }

        // Allow slight delay for TimeText to scroll off, but not long enough for it to come
        // back onto the screen after the idle timeout.
        rule.mainClock.autoAdvance = false
        rule.mainClock.advanceTimeBy(500)
        rule.onNodeWithTag(TEST_TAG).captureToImage().assertDoesNotContainColor(timeTextColor)
    }

    @RequiresApi(Build.VERSION_CODES.O)
    @Test
    fun hidesTimeTextWithColumn() {
        val timeTextColor = Color.Red
        lateinit var scrollState: ScrollState
        rule.setContentWithTheme {
            scrollState = rememberScrollState()

            ColumnTest(scrollState, timeTextColor = timeTextColor)
        }

        rule.onNodeWithTag(SCROLL_TAG).performTouchInput { swipeUp() }

        // Allow slight delay for TimeText to scroll off, but not long enough for it to come
        // back onto the screen after the idle timeout.
        rule.mainClock.autoAdvance = false
        rule.mainClock.advanceTimeBy(500)
        rule.onNodeWithTag(TEST_TAG).captureToImage().assertDoesNotContainColor(timeTextColor)
    }

    @Composable
    private fun ScalingLazyColumnTest(
        scrollState: ScalingLazyListState,
        timeTextColor: Color,
        itemIndex: Int = 1
    ) {
        WithTouchSlop(0f) {
            Box(modifier = Modifier.fillMaxSize().testTag(TEST_TAG)) {
                ScalingLazyColumn(
                    state = scrollState,
                    autoCentering = AutoCenteringParams(itemIndex = itemIndex),
                    modifier = Modifier.fillMaxSize().testTag(SCROLL_TAG)
                ) {
                    item { ListHeader { Text("Buttons") } }

                    items(10) { i -> TestButton(i, Modifier.fillParentMaxHeight(0.5f)) }
                }
                TimeText(
                    modifier =
                        Modifier.scrollAway(
                                scrollInfoProvider = ScrollInfoProvider(scrollState),
                                screenStage = {
                                    if (scrollState.isScrollInProgress) ScreenStage.Scrolling
                                    else ScreenStage.Idle
                                }
                            )
                            .testTag(TIME_TEXT_TAG),
                ) {
                    composable { Box(Modifier.size(20.dp).background(timeTextColor)) }
                }
            }
        }
    }

    @Composable
    private fun LazyColumnTest(scrollState: LazyListState, timeTextColor: Color) {
        WithTouchSlop(0f) {
            Box(
                modifier =
                    Modifier.fillMaxSize()
                        .background(MaterialTheme.colorScheme.background)
                        .testTag(TEST_TAG)
            ) {
                TimeText(
                    modifier =
                        Modifier.scrollAway(
                                scrollInfoProvider = ScrollInfoProvider(scrollState),
                                screenStage = {
                                    if (scrollState.isScrollInProgress) ScreenStage.Scrolling
                                    else ScreenStage.Idle
                                }
                            )
                            .testTag(TIME_TEXT_TAG)
                ) {
                    composable { Box(Modifier.size(20.dp).background(timeTextColor)) }
                }
                LazyColumn(state = scrollState, modifier = Modifier.testTag(SCROLL_TAG)) {
                    item { ListHeader { Text("Buttons") } }
                    items(5) { i -> TestButton(i, Modifier.fillParentMaxHeight(0.5f)) }
                }
            }
        }
    }

    @Composable
    private fun ColumnTest(scrollState: ScrollState, timeTextColor: Color) {
        WithTouchSlop(0f) {
            Box(
                modifier =
                    Modifier.fillMaxSize()
                        .background(MaterialTheme.colorScheme.background)
                        .testTag(TEST_TAG)
            ) {
                TimeText(
                    contentColor = timeTextColor,
                    modifier =
                        Modifier.scrollAway(
                                scrollInfoProvider = ScrollInfoProvider(scrollState),
                                screenStage = {
                                    if (scrollState.isScrollInProgress) ScreenStage.Scrolling
                                    else ScreenStage.Idle
                                }
                            )
                            .testTag(TIME_TEXT_TAG)
                ) {
                    composable { Box(Modifier.size(20.dp).background(timeTextColor)) }
                }
                Column(modifier = Modifier.verticalScroll(scrollState).testTag(SCROLL_TAG)) {
                    ListHeader { Text("Buttons") }
                    repeat(20) { i -> TestButton(i) }
                }
            }
        }
    }

    @Composable
    private fun TestButton(i: Int, modifier: Modifier = Modifier) {
        Button(
            modifier = modifier.fillMaxWidth().padding(horizontal = 36.dp),
            onClick = {},
        ) {
            Text(text = "Button $i")
        }
    }
}

private const val SCROLL_TAG = "ScrollTag"
private const val TIME_TEXT_TAG = "TimeTextTag"
