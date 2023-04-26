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

package androidx.tv.material3

import android.os.SystemClock
import android.view.KeyEvent
import androidx.compose.animation.ContentTransform
import androidx.compose.animation.ExperimentalAnimationApi
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.focusable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.text.BasicText
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.focus.onFocusChanged
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.key.NativeKeyEvent
import androidx.compose.ui.platform.LocalLayoutDirection
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.semantics.SemanticsActions
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.assertIsFocused
import androidx.compose.ui.test.assertIsNotFocused
import androidx.compose.ui.test.getUnclippedBoundsInRoot
import androidx.compose.ui.test.junit4.ComposeContentTestRule
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.onParent
import androidx.compose.ui.test.onRoot
import androidx.compose.ui.test.performKeyPress
import androidx.compose.ui.test.performSemanticsAction
import androidx.compose.ui.unit.LayoutDirection
import androidx.compose.ui.unit.dp
import androidx.test.platform.app.InstrumentationRegistry
import com.google.common.truth.Truth.assertThat
import kotlin.math.abs
import kotlinx.coroutines.delay
import org.junit.Rule
import org.junit.Test

private const val delayBetweenItems = 2500L
private const val animationTime = 900L

@OptIn(ExperimentalTvMaterial3Api::class)
class CarouselTest {
    @get:Rule
    val rule = createComposeRule()

    @Test
    fun carousel_autoScrolls() {
        rule.setContent {
            SampleCarousel {
                BasicText(text = "Text ${it + 1}")
            }
        }

        rule.onNodeWithText("Text 1").assertIsDisplayed()

        rule.mainClock.advanceTimeBy(delayBetweenItems)
        rule.onNodeWithText("Text 2").assertIsDisplayed()

        rule.mainClock.advanceTimeBy(delayBetweenItems)
        rule.onNodeWithText("Text 3").assertIsDisplayed()
    }

    @Test
    fun carousel_onFocus_stopsScroll() {
        rule.setContent {
            SampleCarousel {
                BasicText(text = "Text ${it + 1}")
            }
        }

        rule.onNodeWithText("Text 1").assertIsDisplayed()
        rule.onNodeWithText("Text 1").onParent().assertIsNotFocused()

        rule.onNodeWithText("Text 1")
            .onParent()
            .performSemanticsAction(SemanticsActions.RequestFocus)

        rule.mainClock.advanceTimeBy(delayBetweenItems)

        rule.onNodeWithText("Text 2").assertDoesNotExist()
        rule.onNodeWithText("Text 1").onParent().assertIsFocused()
    }

    @Test
    fun carousel_onUserTriggeredPause_stopsScroll() {
        rule.setContent {
            val carouselState = remember { CarouselState() }
            SampleCarousel(carouselState = carouselState) {
                BasicText(text = "Text ${it + 1}")
                LaunchedEffect(carouselState) { carouselState.pauseAutoScroll(it) }
            }
        }

        rule.onNodeWithText("Text 1").assertIsDisplayed()
        rule.onNodeWithText("Text 1").onParent().assertIsNotFocused()

        rule.mainClock.advanceTimeBy(delayBetweenItems)

        rule.onNodeWithText("Text 2").assertDoesNotExist()
        rule.onNodeWithText("Text 1").assertIsDisplayed()
    }

    @Test
    fun carousel_onUserTriggeredPauseAndResume_resumeScroll() {
        var pauseHandle: ScrollPauseHandle? = null
        rule.setContent {
            val carouselState = remember { CarouselState() }
            SampleCarousel(carouselState = carouselState) {
                BasicText(text = "Text ${it + 1}")
                LaunchedEffect(carouselState) {
                    pauseHandle = carouselState.pauseAutoScroll(it)
                }
            }
        }

        rule.mainClock.autoAdvance = false

        rule.onNodeWithText("Text 1").assertIsDisplayed()
        rule.onNodeWithText("Text 1").onParent().assertIsNotFocused()

        rule.mainClock.advanceTimeBy(delayBetweenItems)

        // pause handle has not been resumed, so Text 1 should still be on the screen.
        rule.onNodeWithText("Text 2").assertDoesNotExist()
        rule.onNodeWithText("Text 1").assertIsDisplayed()

        rule.runOnIdle { pauseHandle?.resumeAutoScroll() }
        rule.mainClock.advanceTimeBy(delayBetweenItems)

        // pause handle has been resumed, so Text 2 should be on the screen after
        // delayBetweenItems + animationTime
        rule.onNodeWithText("Text 1").assertDoesNotExist()
        rule.onNodeWithText("Text 2").assertIsDisplayed()
    }

    @Test
    fun carousel_onMultipleUserTriggeredPauseAndResume_resumesScroll() {
        var pauseHandle1: ScrollPauseHandle? = null
        var pauseHandle2: ScrollPauseHandle? = null
        rule.setContent {
            val carouselState = remember { CarouselState() }
            SampleCarousel(carouselState = carouselState) {
                BasicText(text = "Text ${it + 1}")
                LaunchedEffect(carouselState) {
                    if (pauseHandle1 == null) {
                        pauseHandle1 = carouselState.pauseAutoScroll(it)
                    }
                    if (pauseHandle2 == null) {
                        pauseHandle2 = carouselState.pauseAutoScroll(it)
                    }
                }
            }
        }

        rule.mainClock.autoAdvance = false
        rule.onNodeWithText("Text 1").assertIsDisplayed()
        rule.onNodeWithText("Text 1").onParent().assertIsNotFocused()

        rule.mainClock.advanceTimeBy(delayBetweenItems)

        // pause handles have not been resumed, so Text 1 should still be on the screen.
        rule.onNodeWithText("Text 2").assertDoesNotExist()
        rule.onNodeWithText("Text 1").assertIsDisplayed()

        rule.runOnIdle { pauseHandle1?.resumeAutoScroll() }
        rule.mainClock.advanceTimeBy(delayBetweenItems)

        // Second pause handle has not been resumed, so Text 1 should still be on the screen.
        rule.onNodeWithText("Text 2").assertDoesNotExist()
        rule.onNodeWithText("Text 1").assertIsDisplayed()

        rule.runOnIdle { pauseHandle2?.resumeAutoScroll() }
        rule.mainClock.advanceTimeBy(delayBetweenItems)
        // All pause handles have been resumed, so Text 2 should be on the screen after
        // delayBetweenItems + animationTime
        rule.onNodeWithText("Text 1").assertDoesNotExist()
        rule.onNodeWithText("Text 2").assertIsDisplayed()
    }

    @Test
    fun carousel_onRepeatedResumesOnSamePauseHandle_ignoresSubsequentResumeCalls() {
        var pauseHandle1: ScrollPauseHandle? = null
        rule.setContent {
            val carouselState = remember { CarouselState() }
            var pauseHandle2: ScrollPauseHandle? = null
            SampleCarousel(carouselState = carouselState) {
                BasicText(text = "Text ${it + 1}")
                LaunchedEffect(carouselState) {
                    if (pauseHandle1 == null) {
                        pauseHandle1 = carouselState.pauseAutoScroll(it)
                    }
                    if (pauseHandle2 == null) {
                        pauseHandle2 = carouselState.pauseAutoScroll(it)
                    }
                }
            }
        }

        rule.mainClock.autoAdvance = false
        rule.onNodeWithText("Text 1").assertIsDisplayed()
        rule.onNodeWithText("Text 1").onParent().assertIsNotFocused()

        rule.mainClock.advanceTimeBy(delayBetweenItems)

        // pause handles have not been resumed, so Text 1 should still be on the screen.
        rule.onNodeWithText("Text 2").assertDoesNotExist()
        rule.onNodeWithText("Text 1").assertIsDisplayed()

        rule.runOnIdle { pauseHandle1?.resumeAutoScroll() }
        // subsequent call to resume should be ignored
        rule.runOnIdle { pauseHandle1?.resumeAutoScroll() }
        rule.mainClock.advanceTimeBy(delayBetweenItems)

        // Second pause handle has not been resumed, so Text 1 should still be on the screen.
        rule.onNodeWithText("Text 2").assertDoesNotExist()
        rule.onNodeWithText("Text 1").assertIsDisplayed()
    }

    @Test
    fun carousel_outOfFocus_resumesScroll() {
        rule.setContent {
            Column {
                SampleCarousel {
                    BasicText(text = "Text ${it + 1}")
                }
                BasicText(text = "Card", modifier = Modifier.focusable())
            }
        }

        rule.onNodeWithText("Text 1")
            .onParent()
            .performSemanticsAction(SemanticsActions.RequestFocus)

        rule.onNodeWithText("Card").performSemanticsAction(SemanticsActions.RequestFocus)
        rule.onNodeWithText("Card").assertIsFocused()

        rule.mainClock.advanceTimeBy(delayBetweenItems)
        rule.onNodeWithText("Text 1").assertDoesNotExist()
        rule.onNodeWithText("Text 2").assertIsDisplayed()
    }

    @Test
    fun carousel_pagerIndicatorDisplayed() {
        rule.setContent {
            SampleCarousel {
                SampleCarouselItem(index = it)
            }
        }

        rule.onNodeWithTag("indicator").assertIsDisplayed()
    }

    @Test
    fun carousel_withAnimatedContent_successfulTransition() {
        rule.setContent {
            SampleCarousel {
                SampleCarouselItem(index = it) {
                    Column {
                        BasicText(text = "Text ${it + 1}")
                        BasicText(text = "PLAY")
                    }
                }
            }
        }

        rule.mainClock.advanceTimeBy(animationTime, true)
        rule.mainClock.advanceTimeByFrame()

        rule.onNodeWithText("Text 1").assertIsDisplayed()
        rule.onNodeWithText("PLAY").assertIsDisplayed()
    }

    @Test
    fun carousel_withAnimatedContent_successfulFocusIn() {
        rule.setContent {
            SampleCarousel {
                SampleCarouselItem(index = it)
            }
        }

        rule.mainClock.autoAdvance = false
        rule.onNodeWithTag("pager")
            .performSemanticsAction(SemanticsActions.RequestFocus)

        // current item overlay render delay
        rule.mainClock.advanceTimeBy(animationTime, false)
        rule.mainClock.advanceTimeBy(animationTime, false)
        rule.mainClock.advanceTimeByFrame()

        rule.onNodeWithText("Play 0", useUnmergedTree = true)
            .assertIsDisplayed()
            .assertIsFocused()
    }

    @Test
    fun carousel_parentContainerGainsFocus_onBackPress() {
        rule.setContent {
            Box(modifier = Modifier
                .testTag("box-container")
                .fillMaxSize()
                .focusable()) {
                SampleCarousel { index ->
                    SampleButton("Button-${index + 1}")
                }
            }
        }

        // Request focus for Carousel on start
        rule.mainClock.autoAdvance = false
        rule.onNodeWithTag("pager")
            .performSemanticsAction(SemanticsActions.RequestFocus)

        // Trigger recomposition after requesting focus
        rule.mainClock.advanceTimeByFrame()
        rule.waitForIdle()

        // Check if the overlay button is focused
        rule.onNodeWithText("Button-1", useUnmergedTree = true).assertIsFocused()

        // Trigger back press event to exit focus
        performKeyPress(NativeKeyEvent.KEYCODE_BACK)
        rule.mainClock.advanceTimeByFrame()
        rule.waitForIdle()

        // Check if carousel loses focus and parent container gains focus
        rule.onNodeWithText("Button-1", useUnmergedTree = true).assertIsNotFocused()
        rule.onNodeWithTag("box-container").assertIsFocused()
    }

    @Test
    fun carousel_withCarouselItem_parentContainerGainsFocusOnBackPress() {
        rule.setContent {
            Box(modifier = Modifier
                .testTag("box-container")
                .fillMaxSize()
                .focusable()) {
                SampleCarousel {
                    SampleCarouselItem(index = it)
                }
            }
        }

        // Request focus for Carousel on start
        rule.mainClock.autoAdvance = false
        rule.onNodeWithTag("pager")
            .performSemanticsAction(SemanticsActions.RequestFocus)

        // Trigger recomposition after requesting focus and advance time to finish animations
        rule.mainClock.advanceTimeByFrame()
        rule.waitForIdle()
        rule.mainClock.advanceTimeBy(animationTime, false)
        rule.waitForIdle()

        // Check if the overlay button is focused
        rule.onNodeWithText("Play 0", useUnmergedTree = true).assertIsFocused()

        // Trigger back press event to exit focus
        performKeyPress(NativeKeyEvent.KEYCODE_BACK)
        rule.mainClock.advanceTimeByFrame()
        rule.waitForIdle()

        // Check if carousel loses focus and parent container gains focus
        rule.onNodeWithText("Play 0", useUnmergedTree = true).assertIsNotFocused()
        rule.onNodeWithTag("box-container").assertIsFocused()
    }

    @Test
    fun carousel_scrollToRegainFocus_checkBringIntoView() {
        val focusRequester = FocusRequester()
        rule.setContent {
            LazyColumn {
                items(3) {
                    var isFocused by remember { mutableStateOf(false) }
                    BasicText(
                        text = "test-card-$it",
                        modifier = Modifier
                            .focusRequester(if (it == 0) focusRequester else FocusRequester.Default)
                            .testTag("test-card-$it")
                            .size(200.dp)
                            .border(2.dp, if (isFocused) Color.Red else Color.Black)
                            .onFocusChanged { fs ->
                                isFocused = fs.isFocused
                            }
                            .focusable()
                    )
                }
                item {
                    Carousel(
                        modifier = Modifier
                            .height(500.dp)
                            .fillMaxWidth()
                            .testTag("featured-carousel")
                            .border(2.dp, Color.Black),
                        carouselState = remember { CarouselState() },
                        itemCount = 3,
                        autoScrollDurationMillis = delayBetweenItems
                    ) {
                        SampleCarouselItem(index = it) {
                            Box {
                                Column(modifier = Modifier.align(Alignment.BottomStart)) {
                                    BasicText(text = "carousel-frame")
                                    Row {
                                        SampleButton(text = "PLAY")
                                    }
                                }
                            }
                        }
                    }
                }
                items(2) {
                    var isFocused by remember { mutableStateOf(false) }
                    BasicText(
                        text = "test-card-${it + 3}",
                        modifier = Modifier
                            .testTag("test-card-${it + 3}")
                            .size(250.dp)
                            .border(
                                2.dp,
                                if (isFocused) Color.Red else Color.Black
                            )
                            .onFocusChanged { fs ->
                                isFocused = fs.isFocused
                            }
                            .focusable()
                    )
                }
            }
        }
        rule.runOnIdle { focusRequester.requestFocus() }

        // Initially first focusable element would be focused
        rule.waitForIdle()
        rule.onNodeWithTag("test-card-0").assertIsFocused()

        // Scroll down to the Carousel and check if it's brought into view on gaining focus
        performKeyPress(NativeKeyEvent.KEYCODE_DPAD_DOWN, 3)
        rule.waitForIdle()
        rule.onNodeWithTag("featured-carousel").assertIsDisplayed()
        assertThat(checkNodeCompletelyVisible(rule, "featured-carousel")).isTrue()

        // Scroll down to last element, making sure the carousel is partially visible
        performKeyPress(NativeKeyEvent.KEYCODE_DPAD_DOWN, 2)
        rule.waitForIdle()
        rule.onNodeWithTag("test-card-4").assertIsFocused()
        rule.onNodeWithTag("featured-carousel").assertIsDisplayed()

        // Scroll back to the carousel to check if it's brought into view on regaining focus
        performKeyPress(NativeKeyEvent.KEYCODE_DPAD_UP, 2)
        rule.waitForIdle()
        rule.onNodeWithTag("featured-carousel").assertIsDisplayed()
        assertThat(checkNodeCompletelyVisible(rule, "featured-carousel")).isTrue()
    }

    @Test
    fun carousel_zeroItemCount_shouldNotCrash() {
        val testTag = "emptyCarousel"
        rule.setContent {
            Carousel(itemCount = 0, modifier = Modifier.testTag(testTag)) {}
        }

        rule.onNodeWithTag(testTag).assertExists()
    }

    @Test
    fun carousel_oneItemCount_shouldNotCrash() {
        val testTag = "emptyCarousel"
        rule.setContent {
            Carousel(itemCount = 1, modifier = Modifier.testTag(testTag)) {}
        }

        rule.onNodeWithTag(testTag).assertExists()
    }

    @Test
    fun carousel_manualScrollingWithFocusableItemsOnTop_focusStaysWithinCarousel() {
        rule.setContent {
            Column {
                Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                    repeat(3) {
                        SampleButton("Row-button-${it + 1}")
                    }
                }
                SampleCarousel { index ->
                    SampleButton("Button-${index + 1}")
                }
            }
        }

        rule.mainClock.autoAdvance = false
        rule.onNodeWithTag("pager")
            .performSemanticsAction(SemanticsActions.RequestFocus)

        // trigger recomposition on requesting focus
        rule.mainClock.advanceTimeByFrame()
        rule.waitForIdle()

        // Check that item 1 is in view and button 1 has focus
        rule.onNodeWithText("Button-1").assertIsDisplayed()
        rule.onNodeWithText("Button-1").assertIsFocused()

        // press dpad right to scroll to next item
        performKeyPress(NativeKeyEvent.KEYCODE_DPAD_RIGHT)

        // Wait for item to load
        rule.mainClock.advanceTimeByFrame()
        rule.waitForIdle()
        rule.mainClock.advanceTimeBy(animationTime, false)
        rule.waitForIdle()

        // Check that item 2 is in view and button 2 has focus
        rule.onNodeWithText("Button-2").assertIsDisplayed()
        rule.onNodeWithText("Button-2").assertIsFocused()

        // Check if the first focusable element in parent has focus
        rule.onNodeWithText("Row-button-1").assertIsNotFocused()

        // press dpad left to scroll to previous item
        performKeyPress(NativeKeyEvent.KEYCODE_DPAD_LEFT)

        // Wait for item to load
        rule.mainClock.advanceTimeByFrame()
        rule.waitForIdle()
        rule.mainClock.advanceTimeBy(animationTime, false)
        rule.waitForIdle()

        // Check that item 1 is in view and button 1 has focus
        rule.onNodeWithText("Button-1").assertIsDisplayed()
        rule.onNodeWithText("Button-1").assertIsFocused()
    }

    @Test
    fun carousel_manualScrollingFastMultipleKeyPresses_focusStaysWithinCarousel() {
        val carouselState = CarouselState()
        val tabs = listOf("Tab 1", "Tab 2", "Tab 3")
        var numberOfTimesTabGainedFocus = 0

        rule.setContent {
            var selectedTabIndex by remember { mutableStateOf(0) }

            Column {
                TabRow(
                    modifier = Modifier.onFocusChanged {
                        if (it.hasFocus || it.isFocused) {
                            numberOfTimesTabGainedFocus++
                        }
                    },
                    selectedTabIndex = selectedTabIndex
                ) {
                    tabs.forEachIndexed { index, tab ->
                        Tab(
                            selected = index == selectedTabIndex,
                            onFocus = { selectedTabIndex = index },
                        ) {
                            Text(text = tab)
                        }
                    }
                }

                SampleCarousel(carouselState = carouselState, itemCount = 20) {
                    SampleCarouselItem(modifier = Modifier.testTag("item-$it"), index = it)
                }
            }
        }

        rule.waitForIdle()
        rule.onNodeWithText("Play 0").performSemanticsAction(SemanticsActions.RequestFocus)
        rule.waitForIdle()

        val itemProgression = listOf(6, 3, -4, 3, -6, 5, 3)
        // reset the counter at test start.
        numberOfTimesTabGainedFocus = 0

        itemProgression.forEach {
            performKeyPress(
                if (it < 0) NativeKeyEvent.KEYCODE_DPAD_LEFT else NativeKeyEvent.KEYCODE_DPAD_RIGHT,
                abs(it)
            )
            rule.waitForIdle()
        }

        rule.mainClock.advanceTimeBy(animationTime)

        val finalItem = itemProgression.sum()
        assertThat(numberOfTimesTabGainedFocus).isEqualTo(0)
        rule.onNodeWithText("Play $finalItem", useUnmergedTree = true).assertIsFocused()

        performKeyPress(NativeKeyEvent.KEYCODE_DPAD_RIGHT, 3)

        rule.mainClock.advanceTimeBy((animationTime) * 3)

        rule.onNodeWithText("Play ${finalItem + 3}", useUnmergedTree = true).assertIsFocused()
    }

    @Test
    fun carousel_manualScrollingDpadLongPress_moveOnlyOneSlide() {
        rule.setContent {
            SampleCarousel(itemCount = 6) { index ->
                SampleButton("Button ${index + 1}")
            }
        }

        // Request focus for Carousel on start
        rule.mainClock.autoAdvance = false
        rule.onNodeWithTag("pager")
            .performSemanticsAction(SemanticsActions.RequestFocus)

        // Trigger recomposition after requesting focus
        rule.mainClock.advanceTimeByFrame()
        rule.waitForIdle()

        // Assert that Button 1 from first item is focused
        rule.onNodeWithText("Button 1").assertIsFocused()

        // Trigger dpad right key long press
        performLongKeyPress(rule, NativeKeyEvent.KEYCODE_DPAD_RIGHT)

        // Advance time and trigger recomposition to switch to next item
        rule.mainClock.advanceTimeByFrame()
        rule.waitForIdle()
        rule.mainClock.advanceTimeBy(delayBetweenItems, false)
        rule.waitForIdle()

        // Assert that Button 2 from second item is focused
        rule.onNodeWithText("Button 2").assertIsFocused()

        // Trigger dpad left key long press
        performLongKeyPress(rule, NativeKeyEvent.KEYCODE_DPAD_LEFT)

        // Advance time and trigger recomposition to switch to previous item
        rule.mainClock.advanceTimeBy(delayBetweenItems, false)
        rule.waitForIdle()
        rule.mainClock.advanceTimeByFrame()
        rule.waitForIdle()

        // Assert that Button 1 from first item is focused
        rule.onNodeWithText("Button 1").assertIsFocused()
    }

    @Test
    fun carousel_manualScrollingLtr_RightMovesToNextSlideLeftMovesToPrevSlide() {
        rule.setContent {
            SampleCarousel { index ->
                SampleButton("Button ${index + 1}")
            }
        }

        rule.mainClock.autoAdvance = false
        rule.onNodeWithTag("pager")
            .performSemanticsAction(SemanticsActions.RequestFocus)

        // current item overlay render delay
        rule.mainClock.advanceTimeBy(animationTime, false)
        rule.mainClock.advanceTimeBy(animationTime, false)
        rule.mainClock.advanceTimeByFrame()

        // Assert that item 1 is in view
        rule.onNodeWithText("Button 1").assertIsDisplayed()

        // advance time
        rule.mainClock.advanceTimeBy(delayBetweenItems + animationTime, false)
        rule.mainClock.advanceTimeByFrame()

        // go right once
        performKeyPress(NativeKeyEvent.KEYCODE_DPAD_RIGHT)

        // Wait for item to load
        rule.mainClock.advanceTimeBy(animationTime)
        rule.mainClock.advanceTimeByFrame()

        // Assert that item 2 is in view
        rule.onNodeWithText("Button 2").assertIsDisplayed()

        // go left once
        performKeyPress(NativeKeyEvent.KEYCODE_DPAD_LEFT)

        // Wait for item to load
        rule.mainClock.advanceTimeBy(delayBetweenItems)
        rule.mainClock.advanceTimeBy(animationTime)
        rule.mainClock.advanceTimeByFrame()

        // Assert that item 1 is in view
        rule.onNodeWithText("Button 1").assertIsDisplayed()
    }

    @Test
    fun carousel_manualScrollingRtl_LeftMovesToNextSlideRightMovesToPrevSlide() {
        rule.setContent {
            CompositionLocalProvider(
                LocalLayoutDirection provides LayoutDirection.Rtl
            ) {
                SampleCarousel {
                    SampleButton("Button ${it + 1}")
                }
            }
        }

        rule.mainClock.autoAdvance = false
        rule.onNodeWithTag("pager")
            .performSemanticsAction(SemanticsActions.RequestFocus)

        // current item overlay render delay
        rule.mainClock.advanceTimeBy(animationTime, false)
        rule.mainClock.advanceTimeBy(animationTime, false)
        rule.mainClock.advanceTimeByFrame()

        // Assert that item 1 is in view
        rule.onNodeWithText("Button 1").assertIsDisplayed()

        // advance time
        rule.mainClock.advanceTimeBy(delayBetweenItems + animationTime, false)
        rule.mainClock.advanceTimeByFrame()

        // go right once
        performKeyPress(NativeKeyEvent.KEYCODE_DPAD_LEFT)

        // Wait for item to load
        rule.mainClock.advanceTimeBy(animationTime)
        rule.mainClock.advanceTimeByFrame()

        // Assert that item 2 is in view
        rule.onNodeWithText("Button 2").assertIsDisplayed()

        // go left once
        performKeyPress(NativeKeyEvent.KEYCODE_DPAD_RIGHT)

        // Wait for item to load
        rule.mainClock.advanceTimeBy(delayBetweenItems + animationTime, false)
        rule.mainClock.advanceTimeByFrame()

        // Assert that item 1 is in view
        rule.onNodeWithText("Button 1").assertIsDisplayed()
    }

    @Test
    fun carousel_itemCountChangesDuringAnimation_shouldNotCrash() {
        val itemDisplayDurationMs: Long = 100
        var itemChanges = 0
        // number of items will fall from 4 to 2, but 4 item transitions should happen without a
        // crash
        val minSuccessfulItemChanges = 4
        rule.setContent {
            var itemCount by remember { mutableStateOf(4) }
            LaunchedEffect(Unit) {
                while (itemCount >= 2) {
                    delay(itemDisplayDurationMs)
                    itemCount--
                }
            }
            SampleCarousel(
                itemCount = itemCount,
                timeToDisplayItemMillis = itemDisplayDurationMs
            ) { index ->
                if (index >= itemCount) {
                    // itemIndex requested should not be greater than itemCount. User could be
                    // using a data-structure that could throw an IndexOutOfBoundsException.
                    // This can happen when the itemCount changes during the transition between
                    // items.
                    throw Exception("Index is larger, index=$index, itemCount=$itemCount")
                }
                itemChanges++
            }
        }

        rule.waitUntil(timeoutMillis = 5000) { itemChanges > minSuccessfulItemChanges }
    }

    @Test
    fun carousel_slideWithTwoButtonsInARow_focusMovesWithinSlideAndChangesSlideOnlyOnFocusExit() {
        rule.setContent {
            // No AutoScrolling
            SampleCarousel(timeToDisplayItemMillis = Long.MAX_VALUE) {
                Row {
                    SampleButton("Left Button ${it + 1}")
                    SampleButton("Right Button ${it + 1}")
                }
            }
        }

        rule.onNodeWithText("Left Button 1").performSemanticsAction(SemanticsActions.RequestFocus)
        performKeyPress(KeyEvent.KEYCODE_DPAD_RIGHT)
        // focus should have moved from left to right button
        rule.onNodeWithText("Right Button 1").assertIsFocused()
        performKeyPress(KeyEvent.KEYCODE_DPAD_RIGHT)
        // slide should have changed.
        rule.onNodeWithText("Left Button 2").assertIsFocused()
    }
}

@OptIn(ExperimentalTvMaterial3Api::class)
@Composable
private fun SampleCarousel(
    carouselState: CarouselState = remember { CarouselState() },
    itemCount: Int = 3,
    timeToDisplayItemMillis: Long = delayBetweenItems,
    content: @Composable CarouselScope.(index: Int) -> Unit
) {
    Carousel(
        modifier = Modifier
            .padding(5.dp)
            .fillMaxWidth()
            .height(200.dp)
            .testTag("pager"),
        carouselState = carouselState,
        itemCount = itemCount,
        autoScrollDurationMillis = timeToDisplayItemMillis,
        carouselIndicator = {
            CarouselDefaults.IndicatorRow(
                modifier = Modifier
                    .align(Alignment.BottomEnd)
                    .padding(16.dp)
                    .testTag("indicator"),
                activeItemIndex = carouselState.activeItemIndex,
                itemCount = itemCount
            )
        },
        content = { content(it) },
    )
}

@OptIn(ExperimentalTvMaterial3Api::class, ExperimentalAnimationApi::class)
@Composable
private fun CarouselScope.SampleCarouselItem(
    index: Int,
    modifier: Modifier = Modifier,
    contentTransformStartToEnd: ContentTransform =
        CarouselItemDefaults.contentTransformStartToEnd,
    content: (@Composable () -> Unit) = { SampleButton("Play $index") },
) {
    CarouselItem(
        modifier = modifier,
        contentTransformStartToEnd = contentTransformStartToEnd,
        background = {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .background(Color.Red)
                    .border(2.dp, Color.Blue)
            )
        }
    ) {
        content()
    }
}

@Composable
private fun SampleButton(text: String = "Play") {
    var isFocused by remember { mutableStateOf(false) }
    BasicText(
        text = text,
        modifier = Modifier
            .size(100.dp, 20.dp)
            .background(Color.Yellow)
            .onFocusChanged { isFocused = it.isFocused }
            .border(2.dp, if (isFocused) Color.Green else Color.Transparent)
            .focusable(),
    )
}

private fun checkNodeCompletelyVisible(
    rule: ComposeContentTestRule,
    tag: String,
): Boolean {
    rule.waitForIdle()

    val rootRect = rule.onRoot().getUnclippedBoundsInRoot()
    val itemRect = rule.onNodeWithTag(tag).getUnclippedBoundsInRoot()

    return itemRect.left >= rootRect.left &&
        itemRect.right <= rootRect.right &&
        itemRect.top >= rootRect.top &&
        itemRect.bottom <= rootRect.bottom
}

private fun performKeyPress(keyCode: Int, count: Int = 1, afterEachPress: () -> Unit = { }) {
    repeat(count) {
        InstrumentationRegistry
            .getInstrumentation()
            .sendKeyDownUpSync(keyCode)
        afterEachPress()
    }
}

private fun performLongKeyPress(
    rule: ComposeContentTestRule,
    keyCode: Int,
    count: Int = 1
) {
    repeat(count) {
        // Trigger the first key down event to simulate key press
        val firstKeyDownEvent = KeyEvent(
            SystemClock.uptimeMillis(), SystemClock.uptimeMillis(),
            KeyEvent.ACTION_DOWN, keyCode, 0, 0, 0, 0
        )
        rule.onRoot().performKeyPress(androidx.compose.ui.input.key.KeyEvent(firstKeyDownEvent))
        rule.waitForIdle()

        // Trigger multiple key down events with repeat count (>0) to simulate key long press
        val repeatedKeyDownEvent = KeyEvent(
            SystemClock.uptimeMillis(), SystemClock.uptimeMillis(),
            KeyEvent.ACTION_DOWN, keyCode, 5, 0, 0, 0
        )
        rule.onRoot().performKeyPress(androidx.compose.ui.input.key.KeyEvent(repeatedKeyDownEvent))
        rule.waitForIdle()

        // Trigger the final key up event to simulate key release
        val keyUpEvent = KeyEvent(
            SystemClock.uptimeMillis(), SystemClock.uptimeMillis(),
            KeyEvent.ACTION_UP, keyCode, 0, 0, 0, 0
        )
        rule.onRoot().performKeyPress(androidx.compose.ui.input.key.KeyEvent(keyUpEvent))
        rule.waitForIdle()
    }
}
