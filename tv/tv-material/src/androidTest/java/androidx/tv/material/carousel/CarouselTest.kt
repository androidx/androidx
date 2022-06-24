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

import androidx.compose.animation.animateColor
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.focusable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicText
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.scale
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.focus.onFocusChanged
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.semantics.SemanticsActions
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.assertIsFocused
import androidx.compose.ui.test.assertIsNotFocused
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.onParent
import androidx.compose.ui.test.performSemanticsAction
import androidx.compose.ui.unit.dp
import androidx.tv.material.ExperimentalTvMaterialApi
import java.time.Duration
import org.junit.Rule
import org.junit.Test

@OptIn(ExperimentalTvMaterialApi::class)
class CarouselTest {

    private val delayBetweenSlides = 2500L
    private val animationTime = 900L
    private val overlayRenderWaitTime = 1500L

    @get:Rule
    val rule = createComposeRule()

    @Test
    fun carousel_autoScrolls() {

        rule.setContent {
            Content()
        }

        rule.onNodeWithText("Text 1").assertIsDisplayed()

        rule.mainClock.advanceTimeBy(delayBetweenSlides)
        rule.mainClock.advanceTimeBy(animationTime)
        rule.onNodeWithText("Text 2").assertIsDisplayed()

        rule.mainClock.advanceTimeBy(delayBetweenSlides)
        rule.mainClock.advanceTimeBy(animationTime)
        rule.onNodeWithText("Text 3").assertIsDisplayed()
    }

    @Test
    fun carousel_onFocus_stopsScroll() {

        rule.setContent {
            Content()
        }

        rule.onNodeWithText("Text 1").assertIsDisplayed()
        rule.onNodeWithText("Text 1").onParent().assertIsNotFocused()

        rule.onNodeWithText("Text 1")
            .onParent()
            .performSemanticsAction(SemanticsActions.RequestFocus)

        rule.mainClock.advanceTimeBy(delayBetweenSlides)
        rule.mainClock.advanceTimeBy(animationTime)

        rule.onNodeWithText("Text 2").assertDoesNotExist()
        rule.onNodeWithText("Text 1").onParent().assertIsFocused()
    }

    @Test
    fun carousel_onUserTriggeredPause_stopsScroll() {
        var carouselState: CarouselState?
        rule.setContent {
            carouselState = remember { CarouselState() }
            Content(
                carouselState = carouselState!!,
                content = {
                    BasicText(text = "Text ${it + 1}")
                    LaunchedEffect(carouselState) { carouselState?.pauseAutoScroll(it) }
                })
        }

        rule.onNodeWithText("Text 1").assertIsDisplayed()
        rule.onNodeWithText("Text 1").onParent().assertIsNotFocused()

        rule.mainClock.advanceTimeBy(delayBetweenSlides)
        rule.mainClock.advanceTimeBy(animationTime)

        rule.onNodeWithText("Text 2").assertDoesNotExist()
        rule.onNodeWithText("Text 1").assertIsDisplayed()
    }

    @Test
    fun carousel_onUserTriggeredPauseAndResume_resumeScroll() {
        var carouselState: CarouselState?
        var pauseHandle: ScrollPauseHandle? = null
        rule.setContent {
            carouselState = remember { CarouselState() }
            Content(
                carouselState = carouselState!!,
                content = {
                    BasicText(text = "Text ${it + 1}")
                    LaunchedEffect(carouselState) {
                        pauseHandle = carouselState?.pauseAutoScroll(it)
                    }
                })
        }

        rule.mainClock.autoAdvance = false

        rule.onNodeWithText("Text 1").assertIsDisplayed()
        rule.onNodeWithText("Text 1").onParent().assertIsNotFocused()

        rule.mainClock.advanceTimeBy(delayBetweenSlides)
        rule.mainClock.advanceTimeBy(animationTime)

        // pause handle has not been resumed, so Text 1 should still be on the screen.
        rule.onNodeWithText("Text 2").assertDoesNotExist()
        rule.onNodeWithText("Text 1").assertIsDisplayed()

        rule.runOnIdle { pauseHandle?.resumeAutoScroll() }
        rule.mainClock.advanceTimeBy(delayBetweenSlides)
        rule.mainClock.advanceTimeBy(animationTime)

        // pause handle has been resumed, so Text 2 should be on the screen after
        // delayBetweenSlides + animationTime
        rule.onNodeWithText("Text 1").assertDoesNotExist()
        rule.onNodeWithText("Text 2").assertIsDisplayed()
    }

    @Test
    fun carousel_onMultipleUserTriggeredPauseAndResume_resumesScroll() {
        var carouselState: CarouselState?
        var pauseHandle1: ScrollPauseHandle? = null
        var pauseHandle2: ScrollPauseHandle? = null
        rule.setContent {
            carouselState = remember { CarouselState() }
            Content(
                carouselState = carouselState!!,
                content = {
                    BasicText(text = "Text ${it + 1}")
                    LaunchedEffect(carouselState) {
                        if (pauseHandle1 == null) {
                            pauseHandle1 = carouselState?.pauseAutoScroll(it)
                        }
                        if (pauseHandle2 == null) {
                            pauseHandle2 = carouselState?.pauseAutoScroll(it)
                        }
                    }
                })
        }

        rule.mainClock.autoAdvance = false
        rule.onNodeWithText("Text 1").assertIsDisplayed()
        rule.onNodeWithText("Text 1").onParent().assertIsNotFocused()

        rule.mainClock.advanceTimeBy(delayBetweenSlides)
        rule.mainClock.advanceTimeBy(animationTime)

        // pause handles have not been resumed, so Text 1 should still be on the screen.
        rule.onNodeWithText("Text 2").assertDoesNotExist()
        rule.onNodeWithText("Text 1").assertIsDisplayed()

        rule.runOnIdle { pauseHandle1?.resumeAutoScroll() }
        rule.mainClock.advanceTimeBy(delayBetweenSlides)
        rule.mainClock.advanceTimeBy(animationTime)

        // Second pause handle has not been resumed, so Text 1 should still be on the screen.
        rule.onNodeWithText("Text 2").assertDoesNotExist()
        rule.onNodeWithText("Text 1").assertIsDisplayed()

        rule.runOnIdle { pauseHandle2?.resumeAutoScroll() }
        rule.mainClock.advanceTimeBy(delayBetweenSlides)
        rule.mainClock.advanceTimeBy(animationTime)
        // All pause handles have been resumed, so Text 2 should be on the screen after
        // delayBetweenSlides + animationTime
        rule.onNodeWithText("Text 1").assertDoesNotExist()
        rule.onNodeWithText("Text 2").assertIsDisplayed()
    }

    @Test
    fun carousel_onRepeatedResumesOnSamePauseHandle_ignoresSubsequentResumeCalls() {
        var carouselState: CarouselState?
        var pauseHandle1: ScrollPauseHandle? = null
        var pauseHandle2: ScrollPauseHandle? = null
        rule.setContent {
            carouselState = remember { CarouselState() }
            Content(
                carouselState = carouselState!!,
                content = {
                    BasicText(text = "Text ${it + 1}")
                    LaunchedEffect(carouselState) {
                        if (pauseHandle1 == null) {
                            pauseHandle1 = carouselState?.pauseAutoScroll(it)
                        }
                        if (pauseHandle2 == null) {
                            pauseHandle2 = carouselState?.pauseAutoScroll(it)
                        }
                    }
                })
        }

        rule.mainClock.autoAdvance = false
        rule.onNodeWithText("Text 1").assertIsDisplayed()
        rule.onNodeWithText("Text 1").onParent().assertIsNotFocused()

        rule.mainClock.advanceTimeBy(delayBetweenSlides)
        rule.mainClock.advanceTimeBy(animationTime)

        // pause handles have not been resumed, so Text 1 should still be on the screen.
        rule.onNodeWithText("Text 2").assertDoesNotExist()
        rule.onNodeWithText("Text 1").assertIsDisplayed()

        rule.runOnIdle { pauseHandle1?.resumeAutoScroll() }
        // subsequent call to resume should be ignored
        rule.runOnIdle { pauseHandle1?.resumeAutoScroll() }
        rule.mainClock.advanceTimeBy(delayBetweenSlides)
        rule.mainClock.advanceTimeBy(animationTime)

        // Second pause handle has not been resumed, so Text 1 should still be on the screen.
        rule.onNodeWithText("Text 2").assertDoesNotExist()
        rule.onNodeWithText("Text 1").assertIsDisplayed()
    }

    @Test
    fun carousel_outOfFocus_resumesScroll() {
        rule.setContent {
            Content()
        }

        rule.onNodeWithText("Text 1")
            .onParent()
            .performSemanticsAction(SemanticsActions.RequestFocus)

        rule.onNodeWithText("Card").performSemanticsAction(SemanticsActions.RequestFocus)
        rule.onNodeWithText("Card").assertIsFocused()

        rule.mainClock.advanceTimeBy(delayBetweenSlides)
        rule.mainClock.advanceTimeBy(animationTime)
        rule.onNodeWithText("Text 1").assertDoesNotExist()
        rule.onNodeWithText("Text 2").assertIsDisplayed()
    }

    @Test
    fun carousel_pagerIndicatorDisplayed() {
        rule.setContent {
            Content()
        }

        rule.onNodeWithTag("indicator").assertIsDisplayed()
    }

    @Test
    fun carousel_withAnimatedContent_successfulTransition() {
        rule.setContent {
            AnimatedContent()
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
            AnimatedContent()
        }

        rule.mainClock.autoAdvance = false
        rule.onNodeWithTag("pager")
            .performSemanticsAction(SemanticsActions.RequestFocus)

        // current slide overlay render delay
        rule.mainClock.advanceTimeBy(animationTime + overlayRenderWaitTime, false)
        rule.mainClock.advanceTimeBy(animationTime, false)
        rule.mainClock.advanceTimeByFrame()

        rule.onNodeWithText("PLAY").assertIsDisplayed()
        rule.onNodeWithText("PLAY").assertIsFocused()
    }

    @Composable
    fun Content(
        carouselState: CarouselState = remember { CarouselState() },
        content: @Composable (index: Int) -> Unit = { BasicText(text = "Text ${it + 1}")
        }
    ) {
        val slideCount = 3
        LazyColumn {
            item {
                Carousel(
                    modifier = Modifier.fillMaxSize(),
                    carouselState = carouselState,
                    slideCount = slideCount,
                    timeToDisplaySlide = Duration.ofMillis(delayBetweenSlides),
                    carouselIndicator = {
                        CarouselDefaults.Indicator(modifier = Modifier
                            .align(Alignment.BottomEnd)
                            .padding(16.dp)
                            .testTag("indicator"),
                            carouselState = carouselState,
                            slideCount = slideCount)
                    },
                    content = content
                )
            }
            item {
                Box(modifier = Modifier.focusable()
                ) {
                    BasicText(
                        text = "Card",
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(100.dp)
                            .padding(12.dp)
                            .focusable()
                    )
                }
            }
        }
    }

    @Composable
    fun AnimatedContent(carouselState: CarouselState = remember { CarouselState() }) {
        LazyColumn {
            item {
                Carousel(
                    modifier = Modifier
                        .fillMaxSize()
                        .testTag("pager"),
                    slideCount = 3,
                    timeToDisplaySlide = Duration.ofMillis(delayBetweenSlides),
                    carouselState = carouselState
                ) { Frame(text = "Text ${it + 1}") }
            }
            item {
                Box(modifier = Modifier.focusable()
                ) {
                    BasicText(
                        text = "Card",
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(100.dp)
                            .padding(12.dp)
                            .focusable()
                    )
                }
            }
        }
    }

    @Composable
    fun Frame(text: String) {
        val focusRequester = FocusRequester()
        CarouselItem(
            overlayEnterTransitionStartDelay = Duration.ofMillis(overlayRenderWaitTime),
            background = {}) {
            Column(modifier = Modifier
                .onFocusChanged {
                    if (it.isFocused) {
                        focusRequester.requestFocus()
                    }
                }
                .focusable()) {
                BasicText(text = text)
                Row(modifier = Modifier
                    .horizontalScroll(rememberScrollState())
                    .onFocusChanged {
                        if (it.isFocused) {
                            focusRequester.requestFocus()
                        }
                    }
                    .focusable()) {
                    TestButton(text = "PLAY", focusRequester)
                    TestButton(text = "INFO")
                }
            }
        }
    }

    @Composable
    fun TestButton(text: String, focusRequester: FocusRequester? = null) {
        var cardScale
            by remember { mutableStateOf(0.5f) }
        val borderGlowColorTransition =
            rememberInfiniteTransition()
        var initialValue
            by remember { mutableStateOf(Color.Transparent) }
        val glowingColor
            by borderGlowColorTransition.animateColor(
                initialValue = initialValue,
                targetValue = Color.Transparent,
                animationSpec = infiniteRepeatable(
                    animation = tween(1000,
                        easing = LinearEasing),
                    repeatMode = RepeatMode.Reverse
                )
            )

        val baseModifier =
            if (focusRequester == null) Modifier else Modifier.focusRequester(focusRequester)

        Box(
            modifier = baseModifier
                .scale(cardScale)
                .border(
                    2.dp, glowingColor,
                    RoundedCornerShape(12.dp)
                )
                .onFocusChanged { focusState ->
                    if (focusState.isFocused) {
                        cardScale = 1.0f
                        initialValue = Color.White
                    } else {
                        cardScale = 0.5f
                        initialValue = Color.Transparent
                    }
                }
                .clickable(onClick = {})) {
            BasicText(text = text)
        }
    }

    @Test
    fun carousel_zeroSlideCount_drawsSomething() {
        val testTag = "emptyCarousel"
        rule.setContent {
            Carousel(slideCount = 0, modifier = Modifier.testTag(testTag)) {}
        }

        rule.onNodeWithTag(testTag).assertExists()
    }
}
