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

import android.content.Context
import android.view.accessibility.AccessibilityManager
import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.AnimatedContentScope
import androidx.compose.animation.AnimatedVisibilityScope
import androidx.compose.animation.ContentTransform
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
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.saveable.Saver
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.runtime.snapshotFlow
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.FocusDirection
import androidx.compose.ui.focus.FocusDirection.Companion.Left
import androidx.compose.ui.focus.FocusDirection.Companion.Right
import androidx.compose.ui.focus.FocusManager
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.FocusState
import androidx.compose.ui.focus.focusProperties
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.focus.onFocusChanged
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.key.Key
import androidx.compose.ui.input.key.KeyEventType.Companion.KeyUp
import androidx.compose.ui.input.key.key
import androidx.compose.ui.input.key.onKeyEvent
import androidx.compose.ui.input.key.type
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.platform.LocalLayoutDirection
import androidx.compose.ui.semantics.CollectionInfo
import androidx.compose.ui.semantics.ScrollAxisRange
import androidx.compose.ui.semantics.collectionInfo
import androidx.compose.ui.semantics.horizontalScrollAxisRange
import androidx.compose.ui.semantics.scrollBy
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
 * Note: The animations and focus management features have been dropped temporarily due to
 * some technical challenges. If you need them, consider using the previous version of the
 * library (1.0.0-alpha10) or kindly wait until the next alpha version (1.1.0-alpha01).
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
// @OptIn(ExperimentalComposeUiApi::class)
@ExperimentalTvMaterial3Api
@Composable
fun Carousel(
    itemCount: Int,
    modifier: Modifier = Modifier,
    carouselState: CarouselState = rememberCarouselState(),
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
    content: @Composable AnimatedContentScope.(index: Int) -> Unit
) {
    CarouselStateUpdater(carouselState, itemCount)
    var focusState: FocusState? by remember { mutableStateOf(null) }
    val focusManager = LocalFocusManager.current
    val isLtr = LocalLayoutDirection.current == LayoutDirection.Ltr
    val carouselOuterBoxFocusRequester = remember { FocusRequester() }
    var isAutoScrollActive by remember { mutableStateOf(false) }
    val context = LocalContext.current
    val accessibilityManager = remember {
        context.getSystemService(Context.ACCESSIBILITY_SERVICE) as AccessibilityManager
    }

    AutoScrollSideEffect(
        autoScrollDurationMillis = autoScrollDurationMillis,
        itemCount = itemCount,
        carouselState = carouselState,
        doAutoScroll = shouldPerformAutoScroll(focusState, accessibilityManager),
        onAutoScrollChange = { isAutoScrollActive = it })

    Box(modifier = modifier
        .carouselSemantics(itemCount = itemCount, state = carouselState)
        // .bringIntoViewIfChildrenAreFocused()
        .focusRequester(carouselOuterBoxFocusRequester)
        .onFocusChanged {
            focusState = it
            // When the carousel gains focus for the first time
//            if (it.isFocused && isAutoScrollActive) {
//                focusManager.moveFocus(FocusDirection.Enter)
//            }
        }
        .handleKeyEvents(
            carouselState = carouselState,
            outerBoxFocusRequester = carouselOuterBoxFocusRequester,
            focusManager = focusManager,
            itemCount = itemCount,
            isLtr = isLtr
        ) { focusState }
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
            },
            label = "CarouselAnimation"
        ) { activeItemIndex ->
            LaunchedEffect(Unit) {
                if (accessibilityManager.isEnabled) {
                    carouselOuterBoxFocusRequester.requestFocus()
                }
                this@AnimatedContent.onAnimationCompletion {
                    // Outer box is focused
                    if (!isAutoScrollActive && focusState?.isFocused == true) {
                        carouselOuterBoxFocusRequester.requestFocus()
//                        focusManager.moveFocus(FocusDirection.Enter)
                    }
                }
            }
            // it is possible for the itemCount to have changed during the transition.
            // This can cause the itemIndex to be greater than or equal to itemCount and cause
            // IndexOutOfBoundsException. Guarding against this by checking against itemCount
            // before invoking.
            if (itemCount > 0) {
                content(if (activeItemIndex < itemCount) activeItemIndex else 0)
            }
        }
        this.carouselIndicator()
    }
}

@Composable
private fun shouldPerformAutoScroll(
    focusState: FocusState?,
    accessibilityManager: AccessibilityManager
): Boolean {
    val carouselIsFocused = focusState?.isFocused ?: false
    val carouselHasFocus = focusState?.hasFocus ?: false

    // Disable auto scroll when accessibility mode is enabled or the carousel is focused
    return !accessibilityManager.isEnabled && !(carouselIsFocused || carouselHasFocus)
}

// @OptIn(ExperimentalAnimationApi::class)
private suspend fun AnimatedVisibilityScope.onAnimationCompletion(action: suspend () -> Unit) {
//    snapshotFlow { transition.currentState == transition.targetState }.first { it }
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
    if (autoScrollDurationMillis == Long.MAX_VALUE || autoScrollDurationMillis < 0) {
        return
    }

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

@OptIn(
    ExperimentalTvMaterial3Api::class,
//    ExperimentalComposeUiApi::class
)
private fun Modifier.handleKeyEvents(
    carouselState: CarouselState,
    outerBoxFocusRequester: FocusRequester,
    focusManager: FocusManager,
    itemCount: Int,
    isLtr: Boolean,
    currentCarouselBoxFocusState: () -> FocusState?
): Modifier = onKeyEvent {
    fun showPreviousItem() {
        carouselState.moveToPreviousItem(itemCount)
        outerBoxFocusRequester.requestFocus()
    }

    fun showNextItem() {
        carouselState.moveToNextItem(itemCount)
        outerBoxFocusRequester.requestFocus()
    }

    fun updateItemBasedOnLayout(direction: FocusDirection, isLtr: Boolean) {
        when (direction) {
            Left -> if (isLtr) showPreviousItem() else showNextItem()
            Right -> if (isLtr) showNextItem() else showPreviousItem()
        }
    }

    fun handledHorizontalFocusMove(direction: FocusDirection): Boolean =
        when {
            it.nativeKeyEvent.repeatCount > 0 ->
                // Ignore long press key event for manual scrolling
                KeyEventPropagation.StopPropagation

            currentCarouselBoxFocusState()?.isFocused == true ->
                // if carousel box has focus, do not trigger focus search as it can cause focus to
                // move out of Carousel unintentionally.
                if (shouldFocusExitCarousel(direction, carouselState, itemCount, isLtr)) {
                    KeyEventPropagation.ContinuePropagation
                } else {
                    updateItemBasedOnLayout(direction, isLtr)
                    KeyEventPropagation.StopPropagation
                }

            !focusManager.moveFocus(direction) &&
                    currentCarouselBoxFocusState()?.hasFocus == true -> {
                // if focus search was unsuccessful, interpret as input for slide change
                updateItemBasedOnLayout(direction, isLtr)
                KeyEventPropagation.StopPropagation
            }

            else -> KeyEventPropagation.StopPropagation
        }

    when {
        // Ignore KeyUp action type
        it.type == KeyUp -> KeyEventPropagation.ContinuePropagation
        it.key == Key.Back -> {
//            focusManager.moveFocus(FocusDirection.Exit)
            KeyEventPropagation.ContinuePropagation
        }

        it.key == Key.DirectionLeft -> handledHorizontalFocusMove(Left)
        it.key == Key.DirectionRight -> handledHorizontalFocusMove(Right)

        else -> KeyEventPropagation.ContinuePropagation
    }
}.focusProperties {
    // allow exit along horizontal axis only for first and last slide.
//    exit = {
//        when {
//            shouldFocusExitCarousel(it, carouselState, itemCount, isLtr) ->
//                FocusRequester.Default
//
//            else -> FocusRequester.Cancel
//        }
//    }
}

@OptIn(ExperimentalTvMaterial3Api::class)
private fun shouldFocusExitCarousel(
    focusDirection: FocusDirection,
    carouselState: CarouselState,
    itemCount: Int,
    isLtr: Boolean
): Boolean =
    when {
        // LTR: Don't exit if not first item
        focusDirection == Left && isLtr && !carouselState.isFirstItem() -> false
        // RTL: Don't exit if it is not the last item
        focusDirection == Left && !isLtr && !carouselState.isLastItem(itemCount) -> false
        // LTR: Don't exit if not last item
        focusDirection == Right && isLtr && !carouselState.isLastItem(itemCount) -> false
        // RTL: Don't exit if it is not the first item
        focusDirection == Right && !isLtr && !carouselState.isFirstItem() -> false
        else -> true
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
 * Creates a [CarouselState] that is remembered across compositions.
 *
 * Changes to the provided initial values will **not** result in the state being recreated or
 * changed in any way if it has already been created.
 *
 * @param initialActiveItemIndex the index of the first active item
 */
@ExperimentalTvMaterial3Api
@Composable
fun rememberCarouselState(initialActiveItemIndex: Int = 0): CarouselState {
    return rememberSaveable(saver = CarouselState.Saver) {
        CarouselState(initialActiveItemIndex)
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
    internal var activePauseHandlesCount by mutableIntStateOf(0)

    /**
     * The index of the item that is currently displayed by the carousel
     */
    var activeItemIndex by mutableIntStateOf(initialActiveItemIndex)
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

    companion object {
        /**
         * The default [Saver] implementation for [CarouselState].
         */
        val Saver: Saver<CarouselState, *> = Saver(
            save = { it.activeItemIndex },
            restore = { CarouselState(it) }
        )
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
                indicator(isActive)
            }
        }
    }
}

@OptIn(ExperimentalTvMaterial3Api::class)
@Suppress("ComposableModifierFactory")
@Composable
internal fun Modifier.carouselSemantics(
    itemCount: Int,
    state: CarouselState
): Modifier {
    return this.then(
        remember(
            state,
            itemCount
        ) {
            val accessibilityScrollState = ScrollAxisRange(
                value = {
                    // Active slide index represents the current position
                    state.activeItemIndex.toFloat()
                },
                maxValue = {
                    // Last slide index represents the max. value
                    (itemCount - 1).toFloat()
                },
                reverseScrolling = false
            )

            val scrollByAction: ((x: Float, y: Float) -> Boolean) =
                { x, _ ->
                    when {
                        // Positive value of x represents forward scrolling
                        x > 0f -> state.moveToNextItem(itemCount)

                        // Negative value of x represents backward scrolling
                        x < 0f -> state.moveToPreviousItem(itemCount)
                    }

                    // Return false for non-horizontal scrolling (x==0)
                    x != 0f
                }

            Modifier.semantics {
                horizontalScrollAxisRange = accessibilityScrollState

                scrollBy(action = scrollByAction)

                collectionInfo = CollectionInfo(rowCount = 1, columnCount = itemCount)
            }
        }
    )
}
