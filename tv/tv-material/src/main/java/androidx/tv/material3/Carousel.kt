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
import androidx.compose.animation.togetherWith
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
import androidx.compose.ui.semantics.CollectionInfo
import androidx.compose.ui.semantics.collectionInfo
import androidx.compose.ui.semantics.semantics
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
 * @param itemCount total number of items present in the carousel.
 * @param carouselState state associated with this carousel.
 * @param autoScrollDurationMillis duration for which item should be visible before moving to
 * the next item.
 * @param contentTransformStartToEnd animation transform applied when we are moving from start to
 * end in the carousel while scrolling to the next item
 * @param contentTransformEndToStart animation transform applied when we are moving from end to
 * start in the carousel while scrolling to the next item
 * @param carouselIndicator indicator showing the position of the current item among all items.
 * @param content defines the items for a given index.
 */
@Suppress("IllegalExperimentalApiUsage")
@OptIn(ExperimentalComposeUiApi::class)
@ExperimentalTvMaterial3Api
@Composable
fun Carousel(
    itemCount: Int,
    modifier: Modifier = Modifier,
    carouselState: CarouselState = remember { CarouselState() },
    autoScrollDurationMillis: Long = CarouselDefaults.TimeToDisplayItemMillis,
    contentTransformStartToEnd: ContentTransform = CarouselDefaults.contentTransform,
    contentTransformEndToStart: ContentTransform = CarouselDefaults.contentTransform,
    carouselIndicator:
    @Composable BoxScope.() -> Unit = {
        CarouselDefaults.IndicatorRow(
            itemCount = itemCount,
            activeItemIndex = carouselState.activeItemIndex,
            modifier = Modifier
                .align(Alignment.BottomEnd)
                .padding(16.dp),
        )
    },
    content: @Composable CarouselScope.(index: Int) -> Unit
) {
    CarouselStateUpdater(carouselState, itemCount)
    var focusState: FocusState? by remember { mutableStateOf(null) }
    val focusManager = LocalFocusManager.current
    val isLtr = LocalLayoutDirection.current == LayoutDirection.Ltr
    val carouselOuterBoxFocusRequester = remember { FocusRequester() }
    var isAutoScrollActive by remember { mutableStateOf(false) }

    AutoScrollSideEffect(
        autoScrollDurationMillis = autoScrollDurationMillis,
        itemCount = itemCount,
        carouselState = carouselState,
        doAutoScroll = shouldPerformAutoScroll(focusState),
        onAutoScrollChange = { isAutoScrollActive = it })

    Box(modifier = modifier
        .semantics {
            collectionInfo = CollectionInfo(rowCount = 1, columnCount = itemCount)
        }
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
            itemCount = itemCount,
            isLtr = isLtr,
        )
        .focusable()
    ) {
        AnimatedContent(
            targetState = carouselState.activeItemIndex,
            transitionSpec = {
                if (carouselState.isMovingBackward) {
                    contentTransformEndToStart
                } else {
                    contentTransformStartToEnd
                }
            }
        ) { activeItemIndex ->
            LaunchedEffect(Unit) {
                this@AnimatedContent.onAnimationCompletion {
                    // Outer box is focused
                    if (!isAutoScrollActive && focusState?.isFocused == true) {
                        carouselOuterBoxFocusRequester.requestFocus()
                        focusManager.moveFocus(FocusDirection.Enter)
                    }
                }
            }
            // it is possible for the itemCount to have changed during the transition.
            // This can cause the itemIndex to be greater than or equal to itemCount and cause
            // IndexOutOfBoundsException. Guarding against this by checking against itemCount
            // before invoking.
            if (itemCount > 0) {
                CarouselScope(carouselState = carouselState)
                    .content(if (activeItemIndex < itemCount) activeItemIndex else 0)
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
    itemCount: Int,
    carouselState: CarouselState,
    doAutoScroll: Boolean,
    onAutoScrollChange: (isAutoScrollActive: Boolean) -> Unit = {},
) {
    // Needed to ensure that the code within LaunchedEffect receives updates to the itemCount.
    val updatedItemCount by rememberUpdatedState(newValue = itemCount)
    if (doAutoScroll) {
        LaunchedEffect(carouselState) {
            while (true) {
                yield()
                delay(autoScrollDurationMillis)
                if (carouselState.activePauseHandlesCount > 0) {
                    snapshotFlow { carouselState.activePauseHandlesCount }
                        .first { pauseHandleCount -> pauseHandleCount == 0 }
                }
                carouselState.moveToNextItem(updatedItemCount)
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
    itemCount: Int,
    isLtr: Boolean
): Modifier = onKeyEvent {
    // Ignore KeyUp action type
    if (it.type == KeyUp) {
        return@onKeyEvent KeyEventPropagation.ContinuePropagation
    }

    val showPreviousItemAndGetKeyEventPropagation = {
        if (carouselState.isFirstItem()) {
            KeyEventPropagation.ContinuePropagation
        } else {
            carouselState.moveToPreviousItem(itemCount)
            outerBoxFocusRequester.requestFocus()
            KeyEventPropagation.StopPropagation
        }
    }
    val showNextItemAndGetKeyEventPropagation = {
        if (carouselState.isLastItem(itemCount)) {
            KeyEventPropagation.ContinuePropagation
        } else {
            carouselState.moveToNextItem(itemCount)
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
                showPreviousItemAndGetKeyEventPropagation()
            } else {
                showNextItemAndGetKeyEventPropagation()
            }
        }

        KEYCODE_DPAD_RIGHT -> {
            // Ignore long press key event for manual scrolling
            if (it.nativeKeyEvent.repeatCount > 0) {
                return@onKeyEvent KeyEventPropagation.StopPropagation
            }

            if (isLtr) {
                showNextItemAndGetKeyEventPropagation()
            } else {
                showPreviousItemAndGetKeyEventPropagation()
            }
        }

        else -> KeyEventPropagation.ContinuePropagation
    }
}

@OptIn(ExperimentalTvMaterial3Api::class)
@Composable
private fun CarouselStateUpdater(carouselState: CarouselState, itemCount: Int) {
    LaunchedEffect(carouselState, itemCount) {
        if (itemCount != 0) {
            carouselState.activeItemIndex = floorMod(carouselState.activeItemIndex, itemCount)
        }
    }
}

/**
 * State of the Carousel which allows the user to specify the first item that is shown when the
 * Carousel is instantiated in the constructor.
 *
 * It also provides the user with support to pause and resume the auto-scroll behaviour of the
 * Carousel.
 * @param initialActiveItemIndex the index of the first active item
 */
@Stable
@ExperimentalTvMaterial3Api
class CarouselState(initialActiveItemIndex: Int = 0) {
    internal var activePauseHandlesCount by mutableStateOf(0)

    /**
     * The index of the item that is currently displayed by the carousel
     */
    var activeItemIndex by mutableStateOf(initialActiveItemIndex)
        internal set

    /**
     * Tracks whether we are scrolling backward in the Carousel. By default, we are moving forward
     * because of auto-scroll
     */
    internal var isMovingBackward = false
        private set

    /**
     * Pauses the auto-scrolling behaviour of Carousel.
     * The pause request is ignored if [itemIndex] is not the current item that is visible.
     * Returns a [ScrollPauseHandle] that can be used to resume
     */
    fun pauseAutoScroll(itemIndex: Int): ScrollPauseHandle {
        if (this.activeItemIndex != itemIndex) {
            return NoOpScrollPauseHandle
        }
        return ScrollPauseHandleImpl(this)
    }

    internal fun isFirstItem() = activeItemIndex == 0

    internal fun isLastItem(itemCount: Int) = activeItemIndex == itemCount - 1

    internal fun moveToPreviousItem(itemCount: Int) {
        // No items available for carousel
        if (itemCount == 0) return

        isMovingBackward = true

        // Go to previous item
        activeItemIndex = floorMod(activeItemIndex - 1, itemCount)
    }

    internal fun moveToNextItem(itemCount: Int) {
        // No items available for carousel
        if (itemCount == 0) return

        isMovingBackward = false

        // Go to next item
        activeItemIndex = floorMod(activeItemIndex + 1, itemCount)
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
     * Default time for which the item is visible to the user.
     */
    const val TimeToDisplayItemMillis: Long = 5000

    /**
     * Transition applied when bringing it into view and removing it from the view
     */
    val contentTransform: ContentTransform
    @Composable get() =
        fadeIn(animationSpec = tween(100))
            .togetherWith(fadeOut(animationSpec = tween(100)))

    /**
     * An indicator showing the position of the current active item among the items of the
     * carousel.
     *
     * @param itemCount total number of items in the carousel
     * @param activeItemIndex the current active item index
     * @param modifier Modifier applied to the indicators' container
     * @param spacing spacing between the indicator dots
     * @param indicator indicator dot representing each item in the carousel
     */
    @ExperimentalTvMaterial3Api
    @Composable
    fun IndicatorRow(
        itemCount: Int,
        activeItemIndex: Int,
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
            repeat(itemCount) {
                val isActive = it == activeItemIndex
                indicator(isActive = isActive)
            }
        }
    }
}
