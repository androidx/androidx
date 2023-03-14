/*
 * Copyright 2023 The Android Open Source Project
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

import android.view.KeyEvent.KEYCODE_BACK
import android.view.KeyEvent.KEYCODE_DPAD_LEFT
import android.view.KeyEvent.KEYCODE_DPAD_RIGHT
import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.AnimatedVisibilityScope
import androidx.compose.animation.ContentTransform
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
import androidx.compose.ui.focus.FocusManager
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.FocusState
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.focus.onFocusChanged
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.key.KeyEventType.Companion.KeyUp
import androidx.compose.ui.input.key.key
import androidx.compose.ui.input.key.nativeKeyCode
import androidx.compose.ui.input.key.onKeyEvent
import androidx.compose.ui.input.key.type
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.platform.LocalLayoutDirection
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.LayoutDirection
import androidx.compose.ui.unit.dp
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
 * @param modifier Modifier applied to the Carousel.
 * @param slideCount total number of slides present in the carousel.
 * @param carouselState state associated with this carousel.
 * @param autoScrollDurationMillis duration for which slide should be visible before moving to
 * the next slide.
 * @param contentTransformForward animation transform applied when we are moving forward in the
 * carousel while scrolling
 * @param contentTransformBackward animation transform applied when we are moving backward in the
 * carousel while scrolling
 * in the next slide
 * @param carouselIndicator indicator showing the position of the current slide among all slides.
 * @param content defines the slides for a given index.
 */
@Suppress("IllegalExperimentalApiUsage")
@OptIn(ExperimentalComposeUiApi::class, ExperimentalAnimationApi::class)
@ExperimentalTvMaterial3Api
@Composable
fun Carousel(
    slideCount: Int,
    modifier: Modifier = Modifier,
    carouselState: CarouselState = remember { CarouselState() },
    autoScrollDurationMillis: Long = CarouselDefaults.TimeToDisplaySlideMillis,
    contentTransformForward: ContentTransform = CarouselDefaults.contentTransform,
    contentTransformBackward: ContentTransform = CarouselDefaults.contentTransform,
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
    content: @Composable CarouselScope.(index: Int) -> Unit
) {
    CarouselStateUpdater(carouselState, slideCount)
    var focusState: FocusState? by remember { mutableStateOf(null) }
    val focusManager = LocalFocusManager.current
    val isLtr = LocalLayoutDirection.current == LayoutDirection.Ltr
    val carouselOuterBoxFocusRequester = remember { FocusRequester() }
    var isAutoScrollActive by remember { mutableStateOf(false) }

    AutoScrollSideEffect(
        autoScrollDurationMillis = autoScrollDurationMillis,
        slideCount = slideCount,
        carouselState = carouselState,
        doAutoScroll = shouldPerformAutoScroll(focusState),
        onAutoScrollChange = { isAutoScrollActive = it })

    Box(modifier = modifier
        .bringIntoViewIfChildrenAreFocused()
        .focusRequester(carouselOuterBoxFocusRequester)
        .onFocusChanged {
            focusState = it

            // When the carousel gains focus for the first time
            if (it.isFocused && isAutoScrollActive) {
                focusManager.moveFocus(FocusDirection.Enter)
            }
        }
        .handleKeyEvents(
            carouselState = carouselState,
            outerBoxFocusRequester = carouselOuterBoxFocusRequester,
            focusManager = focusManager,
            slideCount = slideCount,
            isLtr = isLtr,
        )
        .focusable()
    ) {
        AnimatedContent(
            targetState = carouselState.activeSlideIndex,
            transitionSpec = {
                if (carouselState.isMovingBackward) {
                    contentTransformBackward
                } else {
                    contentTransformForward
                }
            }
        ) { activeSlideIndex ->
            LaunchedEffect(Unit) {
                this@AnimatedContent.onAnimationCompletion {
                    // Outer box is focused
                    if (!isAutoScrollActive && focusState?.isFocused == true) {
                        carouselOuterBoxFocusRequester.requestFocus()
                        focusManager.moveFocus(FocusDirection.Enter)
                    }
                }
            }
            // it is possible for the slideCount to have changed during the transition.
            // This can cause the slideIndex to be greater than or equal to slideCount and cause
            // IndexOutOfBoundsException. Guarding against this by checking against slideCount
            // before invoking.
            if (slideCount > 0) {
                CarouselScope(carouselState = carouselState)
                    .content(if (activeSlideIndex < slideCount) activeSlideIndex else 0)
            }
        }
        this.carouselIndicator()
    }
}

@Composable
private fun shouldPerformAutoScroll(focusState: FocusState?): Boolean {
    val carouselIsFocused = focusState?.isFocused ?: false
    val carouselHasFocus = focusState?.hasFocus ?: false
    return !(carouselIsFocused || carouselHasFocus)
}

@Suppress("IllegalExperimentalApiUsage")
@OptIn(ExperimentalAnimationApi::class)
private suspend fun AnimatedVisibilityScope.onAnimationCompletion(action: suspend () -> Unit) {
    snapshotFlow { transition.currentState == transition.targetState }.first { it }
    action.invoke()
}

@OptIn(ExperimentalTvMaterial3Api::class)
@Composable
private fun AutoScrollSideEffect(
    autoScrollDurationMillis: Long,
    slideCount: Int,
    carouselState: CarouselState,
    doAutoScroll: Boolean,
    onAutoScrollChange: (isAutoScrollActive: Boolean) -> Unit = {},
) {
    // Needed to ensure that the code within LaunchedEffect receives updates to the slideCount.
    val updatedSlideCount by rememberUpdatedState(newValue = slideCount)
    if (doAutoScroll) {
        LaunchedEffect(carouselState) {
            while (true) {
                yield()
                delay(autoScrollDurationMillis)
                if (carouselState.activePauseHandlesCount > 0) {
                    snapshotFlow { carouselState.activePauseHandlesCount }
                        .first { pauseHandleCount -> pauseHandleCount == 0 }
                }
                carouselState.moveToNextSlide(updatedSlideCount)
            }
        }
    }
    onAutoScrollChange(doAutoScroll)
}

@Suppress("IllegalExperimentalApiUsage")
@OptIn(ExperimentalTvMaterial3Api::class, ExperimentalComposeUiApi::class)
private fun Modifier.handleKeyEvents(
    carouselState: CarouselState,
    outerBoxFocusRequester: FocusRequester,
    focusManager: FocusManager,
    slideCount: Int,
    isLtr: Boolean
): Modifier = onKeyEvent {
    // Ignore KeyUp action type
    if (it.type == KeyUp) {
        return@onKeyEvent KeyEventPropagation.ContinuePropagation
    }

    val showPreviousSlideAndGetKeyEventPropagation = {
        if (carouselState.isFirstSlide()) {
            KeyEventPropagation.ContinuePropagation
        } else {
            carouselState.moveToPreviousSlide(slideCount)
            outerBoxFocusRequester.requestFocus()
            KeyEventPropagation.StopPropagation
        }
    }
    val showNextSlideAndGetKeyEventPropagation = {
        if (carouselState.isLastSlide(slideCount)) {
            KeyEventPropagation.ContinuePropagation
        } else {
            carouselState.moveToNextSlide(slideCount)
            outerBoxFocusRequester.requestFocus()
            KeyEventPropagation.StopPropagation
        }
    }

    when (it.key.nativeKeyCode) {
        KEYCODE_BACK -> {
            focusManager.moveFocus(FocusDirection.Exit)
            KeyEventPropagation.ContinuePropagation
        }

        KEYCODE_DPAD_LEFT -> {
            // Ignore long press key event for manual scrolling
            if (it.nativeKeyEvent.repeatCount > 0) {
                return@onKeyEvent KeyEventPropagation.StopPropagation
            }

            if (isLtr) {
                showPreviousSlideAndGetKeyEventPropagation()
            } else {
                showNextSlideAndGetKeyEventPropagation()
            }
        }

        KEYCODE_DPAD_RIGHT -> {
            // Ignore long press key event for manual scrolling
            if (it.nativeKeyEvent.repeatCount > 0) {
                return@onKeyEvent KeyEventPropagation.StopPropagation
            }

            if (isLtr) {
                showNextSlideAndGetKeyEventPropagation()
            } else {
                showPreviousSlideAndGetKeyEventPropagation()
            }
        }

        else -> KeyEventPropagation.ContinuePropagation
    }
}

@OptIn(ExperimentalTvMaterial3Api::class)
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
@ExperimentalTvMaterial3Api
class CarouselState(initialActiveSlideIndex: Int = 0) {
    internal var activePauseHandlesCount by mutableStateOf(0)

    /**
     * The index of the slide that is currently displayed by the carousel
     */
    var activeSlideIndex by mutableStateOf(initialActiveSlideIndex)
        internal set

    /**
     * Tracks whether we are scrolling backward in the Carousel. By default, we are moving forward
     * because of auto-scroll
     */
    internal var isMovingBackward = false
        private set

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

        isMovingBackward = true

        // Go to previous slide
        activeSlideIndex = floorMod(activeSlideIndex - 1, slideCount)
    }

    internal fun moveToNextSlide(slideCount: Int) {
        // No slides available for carousel
        if (slideCount == 0) return

        isMovingBackward = false

        // Go to next slide
        activeSlideIndex = floorMod(activeSlideIndex + 1, slideCount)
    }
}

@ExperimentalTvMaterial3Api
/**
 * Handle returned by [CarouselState.pauseAutoScroll] that can be used to resume auto-scroll.
 */
sealed interface ScrollPauseHandle {
    /**
     * Resumes the auto-scroll behaviour if there are no other active [ScrollPauseHandle]s.
     */
    fun resumeAutoScroll()
}

@OptIn(ExperimentalTvMaterial3Api::class)
internal object NoOpScrollPauseHandle : ScrollPauseHandle {
    /**
     * Resumes the auto-scroll behaviour if there are no other active [ScrollPauseHandle]s.
     */
    override fun resumeAutoScroll() {}
}

@OptIn(ExperimentalTvMaterial3Api::class)
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

@ExperimentalTvMaterial3Api
object CarouselDefaults {
    /**
     * Default time for which the slide is visible to the user.
     */
    const val TimeToDisplaySlideMillis: Long = 5000

    /**
     * Transition applied when bringing it into view and removing it from the view
     */
    @OptIn(ExperimentalAnimationApi::class)
    val contentTransform: ContentTransform
    @Composable get() =
        fadeIn(animationSpec = tween(100))
            .with(fadeOut(animationSpec = tween(100)))

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
    @ExperimentalTvMaterial3Api
    @Composable
    fun IndicatorRow(
        slideCount: Int,
        activeSlideIndex: Int,
        modifier: Modifier = Modifier,
        spacing: Dp = 8.dp,
        indicator: @Composable (isActive: Boolean) -> Unit = { isActive ->
            val activeColor = Color.White
            val inactiveColor = activeColor.copy(alpha = 0.3f)
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
