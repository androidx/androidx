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

import android.view.KeyEvent.KEYCODE_BACK
import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.AnimatedVisibilityScope
import androidx.compose.animation.EnterTransition
import androidx.compose.animation.ExitTransition
import androidx.compose.animation.ExperimentalAnimationApi
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.with
import androidx.compose.foundation.background
import androidx.compose.foundation.focusable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxScope
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.Stable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.setValue
import androidx.compose.runtime.snapshotFlow
import androidx.compose.ui.Alignment
import androidx.compose.ui.ExperimentalComposeUiApi
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.FocusDirection
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.FocusState
import androidx.compose.ui.focus.focusProperties
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.focus.onFocusChanged
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.key.KeyEventType.Companion.KeyDown
import androidx.compose.ui.input.key.key
import androidx.compose.ui.input.key.nativeKeyCode
import androidx.compose.ui.input.key.onKeyEvent
import androidx.compose.ui.input.key.type
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.platform.LocalLayoutDirection
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.LayoutDirection
import androidx.compose.ui.unit.dp
import androidx.tv.material.ExperimentalTvMaterialApi
import androidx.tv.material.bringIntoViewIfChildrenAreFocused
import java.lang.Math.floorMod
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.yield

/**
 * Composes a hero card rotator to highlight a piece of content.
 *
 * Examples:
 * @sample androidx.tv.samples.SimpleCarousel
 * @sample androidx.tv.samples.CarouselIndicatorWithRectangleShape
 *
 * @param slideCount total number of slides present in the carousel.
 * @param carouselState state associated with this carousel.
 * @param timeToDisplaySlideMillis duration for which slide should be visible before moving to
 * the next slide.
 * @param enterTransition transition used to bring a slide into view.
 * @param exitTransition transition used to remove a slide from view.
 * @param carouselIndicator indicator showing the position of the current slide among all slides.
 * @param content defines the slides for a given index.
 */

@Suppress("IllegalExperimentalApiUsage")
@OptIn(ExperimentalComposeUiApi::class, ExperimentalAnimationApi::class)
@ExperimentalTvMaterialApi
@Composable
fun Carousel(
    slideCount: Int,
    modifier: Modifier = Modifier,
    carouselState: CarouselState = remember { CarouselState() },
    timeToDisplaySlideMillis: Long = CarouselDefaults.TimeToDisplaySlideMillis,
    enterTransition: EnterTransition = CarouselDefaults.EnterTransition,
    exitTransition: ExitTransition = CarouselDefaults.ExitTransition,
    carouselIndicator:
    @Composable BoxScope.() -> Unit = {
        CarouselDefaults.IndicatorRow(
            slideCount = slideCount,
            activeSlideIndex = carouselState.activeSlideIndex,
            modifier = Modifier
                .align(Alignment.BottomEnd)
                .padding(16.dp),
        )
    },
    content: @Composable (index: Int) -> Unit
) {
    CarouselStateUpdater(carouselState, slideCount)
    var focusState: FocusState? by remember { mutableStateOf(null) }
    val focusManager = LocalFocusManager.current
    val isLtr = LocalLayoutDirection.current == LayoutDirection.Ltr
    val carouselOuterBoxFocusRequester = remember { FocusRequester() }
    var isAutoScrollActive by remember { mutableStateOf(false) }

    AutoScrollSideEffect(
        timeToDisplaySlideMillis,
        slideCount,
        carouselState,
        focusState,
        onAutoScrollChange = { isAutoScrollActive = it })

    Box(modifier = modifier
        .bringIntoViewIfChildrenAreFocused()
        .focusRequester(carouselOuterBoxFocusRequester)
        .onKeyEvent {
            if (it.key.nativeKeyCode == KEYCODE_BACK && it.type == KeyDown) {
                focusManager.moveFocus(FocusDirection.Exit)
            }
            false
        }
        .onFocusChanged {
            focusState = it
            if (it.isFocused && isAutoScrollActive) {
                focusManager.moveFocus(FocusDirection.Enter)
            }
        }
        .manualScrolling(carouselState, slideCount, isLtr)
        .focusable()) {
        AnimatedContent(
            targetState = carouselState.activeSlideIndex,
            transitionSpec = { enterTransition.with(exitTransition) }
        ) {
            LaunchedEffect(Unit) {
                this@AnimatedContent.onAnimationCompletion {
                    if (isAutoScrollActive.not()) {
                        carouselOuterBoxFocusRequester.requestFocus()
                        focusManager.moveFocus(FocusDirection.Enter)
                    }
                }
            }
            content.invoke(it)
        }
        this.carouselIndicator()
    }
}

@Suppress("IllegalExperimentalApiUsage")
@OptIn(ExperimentalAnimationApi::class)
private suspend fun AnimatedVisibilityScope.onAnimationCompletion(action: suspend () -> Unit) {
    snapshotFlow { transition.currentState == transition.targetState }.first { it }
    action.invoke()
}

@OptIn(ExperimentalTvMaterialApi::class)
@Composable
private fun AutoScrollSideEffect(
    timeToDisplaySlideMillis: Long,
    slideCount: Int,
    carouselState: CarouselState,
    focusState: FocusState?,
    onAutoScrollChange: (isAutoScrollActive: Boolean) -> Unit = {},
) {
    val currentTimeToDisplaySlideMillis by rememberUpdatedState(timeToDisplaySlideMillis)
    val currentSlideCount by rememberUpdatedState(slideCount)
    val carouselIsFocused = focusState?.isFocused ?: false
    val carouselHasFocus = focusState?.hasFocus ?: false
    val doAutoScroll = (carouselIsFocused || carouselHasFocus).not()

    if (doAutoScroll) {
        LaunchedEffect(carouselState) {
            while (true) {
                yield()
                delay(currentTimeToDisplaySlideMillis)
                if (carouselState.activePauseHandlesCount > 0) {
                    snapshotFlow { carouselState.activePauseHandlesCount }
                        .first { pauseHandleCount -> pauseHandleCount == 0 }
                }
                carouselState.moveToNextSlide(currentSlideCount)
            }
        }
    }
    onAutoScrollChange(doAutoScroll)
}

@Suppress("IllegalExperimentalApiUsage")
@OptIn(ExperimentalTvMaterialApi::class, ExperimentalComposeUiApi::class)
private fun Modifier.manualScrolling(
    carouselState: CarouselState,
    slideCount: Int,
    isLtr: Boolean
): Modifier =
    this.focusProperties {
        exit = {
            val showPreviousSlideAndGetFocusRequester = {
                if (carouselState.isFirstSlide().not()) {
                    carouselState.moveToPreviousSlide(slideCount)
                    FocusRequester.Cancel
                } else {
                    FocusRequester.Default
                }
            }
            val showNextSlideAndGetFocusRequester = {
                if (carouselState.isLastSlide(slideCount).not()) {
                    carouselState.moveToNextSlide(slideCount)
                    FocusRequester.Cancel
                } else {
                    FocusRequester.Default
                }
            }
            when (it) {
                FocusDirection.Left -> {
                    if (isLtr) {
                        showPreviousSlideAndGetFocusRequester()
                    } else {
                        showNextSlideAndGetFocusRequester()
                    }
                }

                FocusDirection.Right -> {
                    if (isLtr) {
                        showNextSlideAndGetFocusRequester()
                    } else {
                        showPreviousSlideAndGetFocusRequester()
                    }
                }

                else -> FocusRequester.Default
            }
        }
    }

@OptIn(ExperimentalTvMaterialApi::class)
@Composable
private fun CarouselStateUpdater(carouselState: CarouselState, slideCount: Int) {
    LaunchedEffect(carouselState, slideCount) {
        if (slideCount != 0) {
            carouselState.activeSlideIndex = floorMod(carouselState.activeSlideIndex, slideCount)
        }
    }
}

/**
 * State of the Carousel which allows the user to specify the first slide that is shown when the
 * Carousel is instantiated in the constructor.
 *
 * It also provides the user with support to pause and resume the auto-scroll behaviour of the
 * Carousel.
 * @param initialActiveSlideIndex the index of the first active slide
 */
@Stable
@ExperimentalTvMaterialApi
class CarouselState(initialActiveSlideIndex: Int = 0) {
    internal var activePauseHandlesCount by mutableStateOf(0)

    /**
     * The index of the slide that is currently displayed by the carousel
     */
    var activeSlideIndex by mutableStateOf(initialActiveSlideIndex)
        internal set

    /**
     * Pauses the auto-scrolling behaviour of Carousel.
     * The pause request is ignored if [slideIndex] is not the current slide that is visible.
     * Returns a [ScrollPauseHandle] that can be used to resume
     */
    fun pauseAutoScroll(slideIndex: Int): ScrollPauseHandle {
        if (this.activeSlideIndex != slideIndex) {
            return NoOpScrollPauseHandle
        }
        return ScrollPauseHandleImpl(this)
    }

    internal fun isFirstSlide() = activeSlideIndex == 0

    internal fun isLastSlide(slideCount: Int) = activeSlideIndex == slideCount - 1

    internal fun moveToPreviousSlide(slideCount: Int) {
        // No slides available for carousel
        if (slideCount == 0) return

        // Go to previous slide
        activeSlideIndex = floorMod(activeSlideIndex - 1, slideCount)
    }

    internal fun moveToNextSlide(slideCount: Int) {
        // No slides available for carousel
        if (slideCount == 0) return

        // Go to next slide
        activeSlideIndex = floorMod(activeSlideIndex + 1, slideCount)
    }
}

@ExperimentalTvMaterialApi
/**
 * Handle returned by [CarouselState.pauseAutoScroll] that can be used to resume auto-scroll.
 */
sealed interface ScrollPauseHandle {
    /**
     * Resumes the auto-scroll behaviour if there are no other active [ScrollPauseHandle]s.
     */
    fun resumeAutoScroll()
}

@OptIn(ExperimentalTvMaterialApi::class)
internal object NoOpScrollPauseHandle : ScrollPauseHandle {
    /**
     * Resumes the auto-scroll behaviour if there are no other active [ScrollPauseHandle]s.
     */
    override fun resumeAutoScroll() {}
}

@OptIn(ExperimentalTvMaterialApi::class)
internal class ScrollPauseHandleImpl(private val carouselState: CarouselState) : ScrollPauseHandle {
    private var active by mutableStateOf(true)
    init {
        carouselState.activePauseHandlesCount += 1
    }
    /**
     * Resumes the auto-scroll behaviour if there are no other active [ScrollPauseHandle]s.
     */
    override fun resumeAutoScroll() {
        if (active) {
            active = false
            carouselState.activePauseHandlesCount -= 1
        }
    }
}

@ExperimentalTvMaterialApi
object CarouselDefaults {
    /**
     * Default time for which the slide is visible to the user.
     */
    const val TimeToDisplaySlideMillis: Long = 5000

    /**
     * Default transition used to bring the slide into view
     */
    val EnterTransition: EnterTransition = fadeIn(animationSpec = tween(100))

    /**
     * Default transition used to remove the slide from view
     */
    val ExitTransition: ExitTransition = fadeOut(animationSpec = tween(100))

    /**
     * An indicator showing the position of the current active slide among the slides of the
     * carousel.
     *
     * @param slideCount total number of slides in the carousel
     * @param activeSlideIndex the current active slide index
     * @param modifier Modifier applied to the indicators' container
     * @param spacing spacing between the indicator dots
     * @param indicator indicator dot representing each slide in the carousel
     */
    @ExperimentalTvMaterialApi
    @Composable
    fun IndicatorRow(
        slideCount: Int,
        activeSlideIndex: Int,
        modifier: Modifier = Modifier,
        spacing: Dp = 8.dp,
        indicator: @Composable (isActive: Boolean) -> Unit = { isActive ->
            val activeColor = Color.White
            val inactiveColor = activeColor.copy(alpha = 0.5f)
            Box(
                modifier = Modifier
                    .size(8.dp)
                    .background(
                        color = if (isActive) activeColor else inactiveColor,
                        shape = CircleShape,
                    ),
            )
        }
    ) {
        Row(
            horizontalArrangement = Arrangement.spacedBy(spacing),
            verticalAlignment = Alignment.CenterVertically,
            modifier = modifier,
        ) {
            repeat(slideCount) {
                val isActive = it == activeSlideIndex
                indicator(isActive = isActive)
            }
        }
    }
}
