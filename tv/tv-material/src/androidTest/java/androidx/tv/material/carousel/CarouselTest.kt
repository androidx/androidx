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

package androidx.tv.material.carousel

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
import androidx.compose.ui.test.performSemanticsAction
import androidx.compose.ui.unit.LayoutDirection
import androidx.compose.ui.unit.dp
import androidx.test.platform.app.InstrumentationRegistry
import androidx.tv.material.ExperimentalTvMaterialApi
import com.google.common.truth.Truth.assertThat
import org.junit.Rule
import org.junit.Test

private const val delayBetweenSlides = 2500L
private const val animationTime = 900L
private const val overlayRenderWaitTime = 1500L

@OptIn(ExperimentalTvMaterialApi::class)
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

        rule.mainClock.advanceTimeBy(delayBetweenSlides)
        rule.onNodeWithText("Text 2").assertIsDisplayed()

        rule.mainClock.advanceTimeBy(delayBetweenSlides)
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

        rule.mainClock.advanceTimeBy(delayBetweenSlides)

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

        rule.mainClock.advanceTimeBy(delayBetweenSlides)

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

        rule.mainClock.advanceTimeBy(delayBetweenSlides)

        // pause handle has not been resumed, so Text 1 should still be on the screen.
        rule.onNodeWithText("Text 2").assertDoesNotExist()
        rule.onNodeWithText("Text 1").assertIsDisplayed()

        rule.runOnIdle { pauseHandle?.resumeAutoScroll() }
        rule.mainClock.advanceTimeBy(delayBetweenSlides)

        // pause handle has been resumed, so Text 2 should be on the screen after
        // delayBetweenSlides + animationTime
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

        rule.mainClock.advanceTimeBy(delayBetweenSlides)

        // pause handles have not been resumed, so Text 1 should still be on the screen.
        rule.onNodeWithText("Text 2").assertDoesNotExist()
        rule.onNodeWithText("Text 1").assertIsDisplayed()

        rule.runOnIdle { pauseHandle1?.resumeAutoScroll() }
        rule.mainClock.advanceTimeBy(delayBetweenSlides)

        // Second pause handle has not been resumed, so Text 1 should still be on the screen.
        rule.onNodeWithText("Text 2").assertDoesNotExist()
        rule.onNodeWithText("Text 1").assertIsDisplayed()

        rule.runOnIdle { pauseHandle2?.resumeAutoScroll() }
        rule.mainClock.advanceTimeBy(delayBetweenSlides)
        // All pause handles have been resumed, so Text 2 should be on the screen after
        // delayBetweenSlides + animationTime
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

        rule.mainClock.advanceTimeBy(delayBetweenSlides)

        // pause handles have not been resumed, so Text 1 should still be on the screen.
        rule.onNodeWithText("Text 2").assertDoesNotExist()
        rule.onNodeWithText("Text 1").assertIsDisplayed()

        rule.runOnIdle { pauseHandle1?.resumeAutoScroll() }
        // subsequent call to resume should be ignored
        rule.runOnIdle { pauseHandle1?.resumeAutoScroll() }
        rule.mainClock.advanceTimeBy(delayBetweenSlides)

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

        rule.mainClock.advanceTimeBy(delayBetweenSlides)
        rule.onNodeWithText("Text 1").assertDoesNotExist()
        rule.onNodeWithText("Text 2").assertIsDisplayed()
    }

    @Test
    fun carousel_pagerIndicatorDisplayed() {
        rule.setContent {
            SampleCarousel {
                SampleCarouselSlide(index = it)
            }
        }

        rule.onNodeWithTag("indicator").assertIsDisplayed()
    }

    @Test
    fun carousel_withAnimatedContent_successfulTransition() {
        rule.setContent {
            SampleCarousel {
                SampleCarouselSlide(index = it) {
                    Column {
                        BasicText(text = "Text ${it + 1}")
                        BasicText(text = "PLAY")
                    }
                }
            }
        }

        rule.onNodeWithText("Text 1").assertDoesNotExist()

        rule.mainClock.advanceTimeBy(overlayRenderWaitTime + animationTime, true)
        rule.mainClock.advanceTimeByFrame()

        rule.onNodeWithText("Text 1").assertIsDisplayed()
        rule.onNodeWithText("PLAY").assertIsDisplayed()
    }

    @Test
    fun carousel_withAnimatedContent_successfulFocusIn() {
        rule.setContent {
            SampleCarousel {
                SampleCarouselSlide(index = it)
            }
        }

        rule.mainClock.autoAdvance = false
        rule.onNodeWithTag("pager")
            .performSemanticsAction(SemanticsActions.RequestFocus)

        // current slide overlay render delay
        rule.mainClock.advanceTimeBy(animationTime + overlayRenderWaitTime, false)
        rule.mainClock.advanceTimeBy(animationTime, false)
        rule.mainClock.advanceTimeByFrame()

        rule.onNodeWithText("Play 0").assertIsDisplayed()
        rule.onNodeWithText("Play 0").assertIsFocused()
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
        rule.onNodeWithText("Button-1").assertIsFocused()

        // Trigger back press event to exit focus
        performKeyPress(NativeKeyEvent.KEYCODE_BACK)
        rule.mainClock.advanceTimeByFrame()
        rule.waitForIdle()

        // Check if carousel loses focus and parent container gains focus
        rule.onNodeWithText("Button-1").assertIsNotFocused()
        rule.onNodeWithTag("box-container").assertIsFocused()
    }

    @Test
    fun carousel_withCarouselItem_parentContainerGainsFocus_onBackPress() {
        rule.setContent {
            Box(modifier = Modifier
                .testTag("box-container")
                .fillMaxSize()
                .focusable()) {
                SampleCarousel {
                    SampleCarouselSlide(index = it)
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
        rule.mainClock.advanceTimeBy(animationTime + overlayRenderWaitTime, false)
        rule.waitForIdle()

        // Check if the overlay button is focused
        rule.onNodeWithText("Play 0").assertIsFocused()

        // Trigger back press event to exit focus
        performKeyPress(NativeKeyEvent.KEYCODE_BACK)
        rule.mainClock.advanceTimeByFrame()
        rule.waitForIdle()

        // Check if carousel loses focus and parent container gains focus
        rule.onNodeWithText("Play 0").assertIsNotFocused()
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
                        slideCount = 3,
                        timeToDisplaySlideMillis = delayBetweenSlides
                    ) {
                        SampleCarouselSlide(
                            index = it,
                            overlayRenderWaitTime = overlayRenderWaitTime,
                        ) {
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
    fun carousel_zeroSlideCount_shouldNotCrash() {
        val testTag = "emptyCarousel"
        rule.setContent {
            Carousel(slideCount = 0, modifier = Modifier.testTag(testTag)) {}
        }

        rule.onNodeWithTag(testTag).assertExists()
    }

    @Test
    fun carousel_oneSlideCount_shouldNotCrash() {
        val testTag = "emptyCarousel"
        rule.setContent {
            Carousel(slideCount = 1, modifier = Modifier.testTag(testTag)) {}
        }

        rule.onNodeWithTag(testTag).assertExists()
    }

    @Test
    fun carousel_manualScrolling_withFocusableItemsOnTop() {
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

        // Check that slide 1 is in view and button 1 has focus
        rule.onNodeWithText("Button-1").assertIsDisplayed()
        rule.onNodeWithText("Button-1").assertIsFocused()

        // press dpad right to scroll to next slide
        performKeyPress(NativeKeyEvent.KEYCODE_DPAD_RIGHT)

        // Wait for slide to load
        rule.mainClock.advanceTimeByFrame()
        rule.waitForIdle()
        rule.mainClock.advanceTimeBy(animationTime, false)
        rule.waitForIdle()

        // Check that slide 2 is in view and button 2 has focus
        rule.onNodeWithText("Button-2").assertIsDisplayed()
        // TODO: Fix button 2 isn't gaining focus
        // rule.onNodeWithText("Button-2").assertIsFocused()

        // Check if the first focusable element in parent has focus
        rule.onNodeWithText("Row-button-1").assertIsNotFocused()

        // press dpad left to scroll to previous slide
        performKeyPress(NativeKeyEvent.KEYCODE_DPAD_LEFT)

        // Wait for slide to load
        rule.mainClock.advanceTimeByFrame()
        rule.waitForIdle()
        rule.mainClock.advanceTimeBy(animationTime, false)
        rule.waitForIdle()

        // Check that slide 1 is in view and button 1 has focus
        rule.onNodeWithText("Button-1").assertIsDisplayed()
        rule.onNodeWithText("Button-1").assertIsFocused()
    }

    @Test
    fun carousel_manualScrolling_ltr() {
        rule.setContent {
            SampleCarousel { index ->
                SampleButton("Button ${index + 1}")
            }
        }

        rule.mainClock.autoAdvance = false
        rule.onNodeWithTag("pager")
            .performSemanticsAction(SemanticsActions.RequestFocus)

        // current slide overlay render delay
        rule.mainClock.advanceTimeBy(animationTime + overlayRenderWaitTime, false)
        rule.mainClock.advanceTimeBy(animationTime, false)
        rule.mainClock.advanceTimeByFrame()

        // Assert that slide 1 is in view
        rule.onNodeWithText("Button 1").assertIsDisplayed()

        // advance time
        rule.mainClock.advanceTimeBy(delayBetweenSlides + animationTime, false)
        rule.mainClock.advanceTimeByFrame()

        // go right once
        performKeyPress(NativeKeyEvent.KEYCODE_DPAD_RIGHT)

        // Wait for slide to load
        rule.mainClock.advanceTimeBy(animationTime)
        rule.mainClock.advanceTimeByFrame()

        // Assert that slide 2 is in view
        rule.onNodeWithText("Button 2").assertIsDisplayed()

        // go left once
        performKeyPress(NativeKeyEvent.KEYCODE_DPAD_LEFT)

        // Wait for slide to load
        rule.mainClock.advanceTimeBy(delayBetweenSlides)
        rule.mainClock.advanceTimeBy(animationTime)
        rule.mainClock.advanceTimeByFrame()

        // Assert that slide 1 is in view
        rule.onNodeWithText("Button 1").assertIsDisplayed()
    }

    @Test
    fun carousel_manualScrolling_rtl() {
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

        // current slide overlay render delay
        rule.mainClock.advanceTimeBy(animationTime + overlayRenderWaitTime, false)
        rule.mainClock.advanceTimeBy(animationTime, false)
        rule.mainClock.advanceTimeByFrame()

        // Assert that slide 1 is in view
        rule.onNodeWithText("Button 1").assertIsDisplayed()

        // advance time
        rule.mainClock.advanceTimeBy(delayBetweenSlides + animationTime, false)
        rule.mainClock.advanceTimeByFrame()

        // go right once
        performKeyPress(NativeKeyEvent.KEYCODE_DPAD_LEFT)

        // Wait for slide to load
        rule.mainClock.advanceTimeBy(animationTime)
        rule.mainClock.advanceTimeByFrame()

        // Assert that slide 2 is in view
        rule.onNodeWithText("Button 2").assertIsDisplayed()

        // go left once
        performKeyPress(NativeKeyEvent.KEYCODE_DPAD_RIGHT)

        // Wait for slide to load
        rule.mainClock.advanceTimeBy(delayBetweenSlides + animationTime, false)
        rule.mainClock.advanceTimeByFrame()

        // Assert that slide 1 is in view
        rule.onNodeWithText("Button 1").assertIsDisplayed()
    }
}

@OptIn(ExperimentalTvMaterialApi::class)
@Composable
private fun SampleCarousel(
    carouselState: CarouselState = remember { CarouselState() },
    slideCount: Int = 3,
    timeToDisplaySlideMillis: Long = delayBetweenSlides,
    content: @Composable (index: Int) -> Unit
) {
    Carousel(
        modifier = Modifier
            .padding(5.dp)
            .fillMaxWidth()
            .height(50.dp)
            .testTag("pager"),
        carouselState = carouselState,
        slideCount = slideCount,
        timeToDisplaySlideMillis = timeToDisplaySlideMillis,
        carouselIndicator = {
            CarouselDefaults.IndicatorRow(
                modifier = Modifier
                    .align(Alignment.BottomEnd)
                    .padding(16.dp)
                    .testTag("indicator"),
                activeSlideIndex = carouselState.activeSlideIndex,
                slideCount = slideCount
            )
        },
        content = content,
    )
}

@OptIn(ExperimentalTvMaterialApi::class)
@Composable
private fun SampleCarouselSlide(
    index: Int,
    overlayRenderWaitTime: Long = CarouselItemDefaults.OverlayEnterTransitionStartDelayMillis,
    content: (@Composable () -> Unit) = { SampleButton("Play $index") },
) {
    CarouselItem(
        overlayEnterTransitionStartDelayMillis = overlayRenderWaitTime,
        background = {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .background(Color.Red)
                    .border(2.dp, Color.Blue)
            )
        },
        overlay = content
    )
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

private fun performKeyPress(keyCode: Int, count: Int = 1) {
    repeat(count) {
        InstrumentationRegistry
            .getInstrumentation()
            .sendKeyDownUpSync(keyCode)
    }
}
