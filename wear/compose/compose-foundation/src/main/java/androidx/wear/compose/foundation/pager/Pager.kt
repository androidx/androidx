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

package androidx.wear.compose.foundation.pager

import androidx.annotation.FloatRange
import androidx.compose.animation.core.tween
import androidx.compose.foundation.gestures.Orientation
import androidx.compose.foundation.gestures.TargetedFlingBehavior
import androidx.compose.foundation.gestures.awaitEachGesture
import androidx.compose.foundation.gestures.awaitFirstDown
import androidx.compose.foundation.gestures.snapping.SnapPosition
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.PageSize
import androidx.compose.foundation.pager.PagerDefaults as ComposePagerDefaults
import androidx.compose.foundation.pager.PagerScope
import androidx.compose.foundation.pager.PagerSnapDistance
import androidx.compose.foundation.pager.VerticalPager
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.input.pointer.PointerEventPass
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.LocalViewConfiguration
import androidx.compose.ui.platform.ViewConfiguration
import androidx.compose.ui.semantics.ScrollAxisRange
import androidx.compose.ui.semantics.horizontalScrollAxisRange
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.unit.dp
import androidx.wear.compose.foundation.ExperimentalWearFoundationApi
import androidx.wear.compose.foundation.HierarchicalFocusCoordinator
import kotlinx.coroutines.coroutineScope

/**
 * A full-screen horizontally scrolling Pager optimized for Wear OS devices. This component wraps
 * the standard Compose Foundation [HorizontalPager] and provides Wear-specific enhancements to
 * improve performance, usability, and adherence to Wear OS design guidelines.
 *
 * Please refer to the samples to learn how to use this API.
 *
 * @sample androidx.wear.compose.foundation.samples.SimpleHorizontalPagerSample
 * @param state The state to control this pager
 * @param modifier A modifier instance to be applied to this Pager outer layout
 * @param contentPadding a padding around the whole content. This will add padding for the content
 *   after it has been clipped, which is not possible via [modifier] param. You can use it to add a
 *   padding before the first page or after the last one.
 * @param beyondViewportPageCount Pages to compose and layout before and after the list of visible
 *   pages. Note: Be aware that using a large value for [beyondViewportPageCount] will cause a lot
 *   of pages to be composed, measured and placed which will defeat the purpose of using lazy
 *   loading. This should be used as an optimization to pre-load a couple of pages before and after
 *   the visible ones. This does not include the pages automatically composed and laid out by the
 *   pre-fetcher in the direction of the scroll during scroll events.
 * @param flingBehavior The [TargetedFlingBehavior] to be used for post scroll gestures.
 * @param userScrollEnabled whether the scrolling via the user gestures or accessibility actions is
 *   allowed. You can still scroll programmatically using [PagerState.scroll] even when it is
 *   disabled.
 * @param reverseLayout reverse the direction of scrolling and layout.
 * @param key a stable and unique key representing the item. When you specify the key the scroll
 *   position will be maintained based on the key, which means if you add/remove items before the
 *   current visible item the item with the given key will be kept as the first visible one. If null
 *   is passed the position in the list will represent the key.
 * @param swipeToDismissEdgeZoneFraction A float which controls the size of the screen edge area
 *   used for the Wear system's swipe to dismiss gesture. This value, between 0 and 1, represents
 *   the fraction of the screen width that will be sensitive to the gesture. For example, 0.25 means
 *   the leftmost 25% of the screen will trigger the gesture. Even when RTL mode is enabled, this
 *   parameter only ever applies to the left edge of the screen. Setting this to 0 will disable the
 *   gesture.
 * @param content A composable function that defines the content of each page displayed by the
 *   Pager. This is where the UI elements that should appear within each page should be placed.
 */
@ExperimentalWearFoundationApi
@Composable
fun HorizontalPager(
    state: PagerState,
    modifier: Modifier = Modifier,
    contentPadding: PaddingValues = PaddingValues(0.dp),
    beyondViewportPageCount: Int = ComposePagerDefaults.BeyondViewportPageCount,
    flingBehavior: TargetedFlingBehavior = PagerDefaults.flingBehavior(state = state),
    userScrollEnabled: Boolean = true,
    reverseLayout: Boolean = false,
    key: ((index: Int) -> Any)? = null,
    @FloatRange(from = 0.0, to = 1.0)
    swipeToDismissEdgeZoneFraction: Float = PagerDefaults.SwipeToDismissEdgeZoneFraction,
    content: @Composable PagerScope.(page: Int) -> Unit
) {
    val swipeToDismissEnabled = swipeToDismissEdgeZoneFraction != 0f
    var allowPaging by remember { mutableStateOf(true) }
    val screenWidth =
        with(LocalDensity.current) { LocalConfiguration.current.screenWidthDp.dp.toPx() }
    val edgeBoundary = screenWidth * swipeToDismissEdgeZoneFraction.coerceIn(0f, 1f)

    val originalTouchSlop = LocalViewConfiguration.current.touchSlop
    CustomTouchSlopProvider(
        newTouchSlop =
            if (swipeToDismissEnabled) originalTouchSlop * CustomTouchSlopMultiplier
            else originalTouchSlop
    ) {
        HorizontalPager(
            state = state,
            modifier =
                modifier
                    .fillMaxSize()
                    .pointerInput(screenWidth, swipeToDismissEnabled) {
                        coroutineScope {
                            awaitEachGesture {
                                allowPaging = true
                                val firstDown = awaitFirstDown(false, PointerEventPass.Initial)
                                val xPosition = firstDown.position.x
                                allowPaging = !swipeToDismissEnabled || xPosition > edgeBoundary
                            }
                        }
                    }
                    .semantics {
                        horizontalScrollAxisRange =
                            if (allowPaging) {
                                ScrollAxisRange(
                                    value = { state.currentPage.toFloat() },
                                    maxValue = { state.pageCount.toFloat() }
                                )
                            } else {
                                // signals system swipe to dismiss that it can take over
                                ScrollAxisRange(value = { 0f }, maxValue = { 0f })
                            }
                    },
            contentPadding = contentPadding,
            pageSize = PageSize.Fill,
            beyondViewportPageCount = beyondViewportPageCount,
            pageSpacing = 0.dp,
            verticalAlignment = Alignment.CenterVertically,
            flingBehavior = flingBehavior,
            userScrollEnabled = userScrollEnabled && allowPaging,
            reverseLayout = reverseLayout,
            key = key,
            pageNestedScrollConnection =
                ComposePagerDefaults.pageNestedScrollConnection(state, Orientation.Horizontal),
            snapPosition = SnapPosition.Start,
        ) { page ->
            CustomTouchSlopProvider(newTouchSlop = originalTouchSlop) {
                FocusedPageContent(page = page, pagerState = state, content = { content(page) })
            }
        }
    }
}

/**
 * A full-screen vertically scrolling Pager optimized for Wear OS devices. This component wraps the
 * standard Compose Foundation [VerticalPager] and provides Wear-specific enhancements to improve
 * performance, usability, and adherence to Wear OS design guidelines.
 *
 * Please refer to the sample to learn how to use this API.
 *
 * @sample androidx.wear.compose.foundation.samples.SimpleVerticalPagerSample
 * @param state The state to control this pager
 * @param modifier A modifier instance to be apply to this Pager outer layout
 * @param contentPadding a padding around the whole content. This will add padding for the content
 *   after it has been clipped, which is not possible via [modifier] param. You can use it to add a
 *   padding before the first page or after the last one.
 * @param beyondViewportPageCount Pages to compose and layout before and after the list of visible
 *   pages. Note: Be aware that using a large value for [beyondViewportPageCount] will cause a lot
 *   of pages to be composed, measured and placed which will defeat the purpose of using lazy
 *   loading. This should be used as an optimization to pre-load a couple of pages before and after
 *   the visible ones. This does not include the pages automatically composed and laid out by the
 *   pre-fetcher in the direction of the scroll during scroll events.
 * @param flingBehavior The [TargetedFlingBehavior] to be used for post scroll gestures.
 * @param userScrollEnabled whether the scrolling via the user gestures or accessibility actions is
 *   allowed. You can still scroll programmatically using [PagerState.scroll] even when it is
 *   disabled.
 * @param reverseLayout reverse the direction of scrolling and layout.
 * @param key a stable and unique key representing the item. When you specify the key the scroll
 *   position will be maintained based on the key, which means if you add/remove items before the
 *   current visible item the item with the given key will be kept as the first visible one. If null
 *   is passed the position in the list will represent the key.
 * @param content A composable function that defines the content of each page displayed by the
 *   Pager. This is where the UI elements that should appear within each page should be placed.
 */
@ExperimentalWearFoundationApi
@Composable
fun VerticalPager(
    state: PagerState,
    modifier: Modifier = Modifier,
    contentPadding: PaddingValues = PaddingValues(0.dp),
    beyondViewportPageCount: Int = ComposePagerDefaults.BeyondViewportPageCount,
    flingBehavior: TargetedFlingBehavior = PagerDefaults.flingBehavior(state = state),
    userScrollEnabled: Boolean = true,
    reverseLayout: Boolean = false,
    key: ((index: Int) -> Any)? = null,
    content: @Composable PagerScope.(page: Int) -> Unit
) {
    VerticalPager(
        state = state,
        modifier = modifier.fillMaxSize(),
        contentPadding = contentPadding,
        pageSize = PageSize.Fill,
        beyondViewportPageCount = beyondViewportPageCount,
        pageSpacing = 0.dp,
        horizontalAlignment = Alignment.CenterHorizontally,
        flingBehavior = flingBehavior,
        userScrollEnabled = userScrollEnabled,
        reverseLayout = reverseLayout,
        key = key,
        pageNestedScrollConnection =
            ComposePagerDefaults.pageNestedScrollConnection(state, Orientation.Vertical),
        snapPosition = SnapPosition.Start,
    ) { page ->
        FocusedPageContent(page = page, pagerState = state, content = { content(page) })
    }
}

/** Convenience fling behavior optimised for Wear. */
object PagerDefaults {
    @Composable
    fun flingBehavior(
        state: PagerState,
    ): TargetedFlingBehavior {
        return ComposePagerDefaults.flingBehavior(
            state = state,
            pagerSnapDistance = PagerSnapDistance.atMost(1),
            snapAnimationSpec = tween(150, 0),
        )
    }

    /** Configure Swipe To Dismiss behaviour, only applies to [HorizontalPager]. */
    const val SwipeToDismissEdgeZoneFraction: Float = 0.15f
}

@Composable
internal fun FocusedPageContent(
    page: Int,
    pagerState: PagerState,
    content: @Composable () -> Unit
) {
    HierarchicalFocusCoordinator(
        requiresFocus = { pagerState.currentPage == page },
        content = content
    )
}

@Composable
private fun CustomTouchSlopProvider(newTouchSlop: Float, content: @Composable () -> Unit) {
    CompositionLocalProvider(
        value =
            LocalViewConfiguration provides
                CustomTouchSlop(newTouchSlop, LocalViewConfiguration.current),
        content = content
    )
}

private class CustomTouchSlop(
    private val customTouchSlop: Float,
    currentConfiguration: ViewConfiguration
) : ViewConfiguration by currentConfiguration {
    override val touchSlop: Float
        get() = customTouchSlop
}

internal const val CustomTouchSlopMultiplier = 1.10f
