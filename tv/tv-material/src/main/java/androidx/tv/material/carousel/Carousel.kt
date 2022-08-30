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

import androidx.compose.animation.EnterTransition
import androidx.compose.animation.ExitTransition
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
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
import androidx.compose.ui.focus.FocusState
import androidx.compose.ui.focus.onFocusChanged
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.unit.dp
import androidx.tv.material.ExperimentalTvMaterialApi
import androidx.tv.material.pager.Pager
import java.lang.Math.floorMod
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.yield

/**
 * Composes a hero card rotator to highlight a piece of content.
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
@OptIn(ExperimentalComposeUiApi::class)
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
        CarouselDefaults.Indicator(
            modifier = Modifier.align(Alignment.BottomEnd).padding(16.dp),
            carouselState = carouselState,
            slideCount = slideCount)
    },
    content: @Composable (index: Int) -> Unit
) {
    CarouselStateUpdater(carouselState, slideCount)
    var focusState: FocusState? by remember { mutableStateOf(null) }
    val focusManager = LocalFocusManager.current

    AutoScrollSideEffect(
        timeToDisplaySlideMillis,
        slideCount,
        carouselState,
        focusState)
    Box(modifier = modifier
        .onFocusChanged {
            focusState = it
            if (it.isFocused) {
                focusManager.moveFocus(FocusDirection.Enter)
            }
        }
        .focusable()) {
        Pager(
            enterTransition = enterTransition,
            exitTransition = exitTransition,
            currentSlide = carouselState.slideIndex,
            slideCount = slideCount
        ) { content.invoke(it) }

        this.carouselIndicator()
    }
}

@OptIn(ExperimentalTvMaterialApi::class)
@Composable
private fun AutoScrollSideEffect(
    timeToDisplaySlideMillis: Long,
    slideCount: Int,
    carouselState: CarouselState,
    focusState: FocusState?
) {
    val currentTimeToDisplaySlideMillis by rememberUpdatedState(timeToDisplaySlideMillis)
    val currentSlideCount by rememberUpdatedState(slideCount)
    val carouselIsFocused = focusState?.isFocused ?: false
    val carouselHasFocus = focusState?.hasFocus ?: false

    if (!(carouselIsFocused || carouselHasFocus)) {
        LaunchedEffect(carouselState) {
            while (true) {
                yield()
                delay(currentTimeToDisplaySlideMillis)
                if (carouselState.activePauseHandlesCount > 0) {
                    snapshotFlow { carouselState.activePauseHandlesCount }
                        .first { pauseHandleCount -> pauseHandleCount == 0 }
                }
                carouselState.nextSlide(currentSlideCount)
            }
        }
    }
}

@OptIn(ExperimentalTvMaterialApi::class)
@Composable
private fun CarouselStateUpdater(carouselState: CarouselState, slideCount: Int) {
    LaunchedEffect(carouselState, slideCount) {
        if (slideCount != 0) {
            carouselState.slideIndex = floorMod(carouselState.slideIndex, slideCount)
        }
    }
}

/**
 * State of the Carousel which allows the user to specify the first slide that is shown when the
 * Carousel is instantiated in the constructor.
 *
 * It also provides the user with support to pause and resume the auto-scroll behaviour of the
 * Carousel.
 * @param initialSlideIndex the index of the first slide that is displayed.
 */
@Stable
@ExperimentalTvMaterialApi
class CarouselState(initialSlideIndex: Int = 0) {
    internal var activePauseHandlesCount by mutableStateOf(0)

    /**
     * The index of the slide that is currently displayed by the carousel
     */
    var slideIndex by mutableStateOf(initialSlideIndex)
        internal set

    /**
     * Pauses the auto-scrolling behaviour of Carousel.
     * The pause request is ignored if [slideIndex] is not the current slide that is visible.
     * Returns a [ScrollPauseHandle] that can be used to resume
     */
    fun pauseAutoScroll(slideIndex: Int): ScrollPauseHandle {
        if (this.slideIndex != slideIndex) {
            return NoOpScrollPauseHandle
        }
        return ScrollPauseHandleImpl(this)
    }

    internal fun nextSlide(slideCount: Int) {
        if (slideCount != 0) {
            slideIndex = floorMod(slideIndex + 1, slideCount)
        }
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
    val TimeToDisplaySlideMillis: Long = 5000

    /**
     * Default transition used to bring the slide into view
     */
    val EnterTransition: EnterTransition = fadeIn(animationSpec = tween(900))

    /**
     * Default transition used to remove the slide from view
     */
    val ExitTransition: ExitTransition = fadeOut(animationSpec = tween(900))

    /**
     * An indicator showing the position of the current slide among the slides of the carousel.
     *
     * @param carouselState is the state associated with the carousel of which this indicator is a
     * part.
     * @param slideCount total number of slides in the carousel
     */
    @ExperimentalTvMaterialApi
    @Composable
    fun Indicator(
        carouselState: CarouselState,
        slideCount: Int,
        modifier: Modifier = Modifier
    ) {
        if (slideCount <= 0) {
            Box(modifier = modifier)
        } else {
            val defaultSize = 8.dp
            val inactiveColor = Color.LightGray
            val activeColor = Color.White
            val shape = CircleShape
            val indicatorModifier = Modifier.size(defaultSize)

            Box(modifier = modifier) {
                Row(
                    horizontalArrangement = Arrangement.spacedBy(defaultSize),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    repeat(slideCount) {
                        Box(indicatorModifier.background(
                            color =
                              if (it == carouselState.slideIndex) {
                                  activeColor
                              } else {
                                  inactiveColor
                              },
                            shape = shape
                        ))
                    }
                }
            }
        }
    }
}
