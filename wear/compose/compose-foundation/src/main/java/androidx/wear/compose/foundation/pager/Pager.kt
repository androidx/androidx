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
import androidx.compose.animation.core.AnimationSpec
import androidx.compose.animation.core.DecayAnimationSpec
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.spring
import androidx.compose.animation.rememberSplineBasedDecay
import androidx.compose.foundation.gestures.TargetedFlingBehavior
import androidx.compose.foundation.gestures.awaitEachGesture
import androidx.compose.foundation.gestures.awaitFirstDown
import androidx.compose.foundation.gestures.snapping.SnapPosition
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.PageSize
import androidx.compose.foundation.pager.PagerDefaults as ComposePagerDefaults
import androidx.compose.foundation.pager.PagerSnapDistance as ComposePagerSnapDistance
import androidx.compose.foundation.pager.VerticalPager
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.input.pointer.PointerEventPass
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.LayoutCoordinates
import androidx.compose.ui.layout.findRootCoordinates
import androidx.compose.ui.layout.onPlaced
import androidx.compose.ui.platform.LocalViewConfiguration
import androidx.compose.ui.semantics.ScrollAxisRange
import androidx.compose.ui.semantics.horizontalScrollAxisRange
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.unit.dp
import androidx.wear.compose.foundation.CustomTouchSlopProvider
import androidx.wear.compose.foundation.DefaultTouchExplorationStateProvider
import androidx.wear.compose.foundation.GestureInclusion
import androidx.wear.compose.foundation.LocalScreenIsActive
import androidx.wear.compose.foundation.hierarchicalFocusGroup
import androidx.wear.compose.foundation.requestFocusOnHierarchyActive
import androidx.wear.compose.foundation.rotary.RotaryScrollableBehavior
import androidx.wear.compose.foundation.rotary.RotaryScrollableDefaults
import androidx.wear.compose.foundation.rotary.rotaryScrollable

/**
 * A horizontally scrolling Pager optimized for Wear OS devices. This component wraps the standard
 * Compose Foundation [HorizontalPager] and provides Wear-specific enhancements to improve
 * performance, usability, and adherence to Wear OS design guidelines.
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
 * @param gestureInclusion When userScrollEnabled=true, this function provides more fine-grained
 *   control so that touch gestures can be excluded when they start in a certain region. An instance
 *   of [GestureInclusion] can be passed in here which will determine via
 *   [GestureInclusion.ignoreGestureStart] whether the gesture should proceed or not. By default,
 *   [gestureInclusion] allows gestures everywhere except a zone on the left edge of the first page,
 *   which is used for swipe-to-dismiss (see [PagerDefaults.gestureInclusion]).
 * @param reverseLayout reverse the direction of scrolling and layout.
 * @param key a stable and unique key representing the item. When you specify the key the scroll
 *   position will be maintained based on the key, which means if you add/remove items before the
 *   current visible item the item with the given key will be kept as the first visible one. If null
 *   is passed the position in the list will represent the key.
 * @param rotaryScrollableBehavior Parameter for changing rotary behavior. By default rotary support
 *   is disabled for [HorizontalPager]. It can be enabled by passing
 *   [RotaryScrollableDefaults.snapBehavior] with pagerState parameter.
 * @param content A composable function that defines the content of each page displayed by the
 *   Pager. This is where the UI elements that should appear within each page should be placed.
 */
@Composable
public fun HorizontalPager(
    state: PagerState,
    modifier: Modifier = Modifier,
    contentPadding: PaddingValues = PaddingValues(0.dp),
    beyondViewportPageCount: Int = PagerDefaults.BeyondViewportPageCount,
    flingBehavior: TargetedFlingBehavior = PagerDefaults.snapFlingBehavior(state = state),
    userScrollEnabled: Boolean = true,
    gestureInclusion: GestureInclusion = PagerDefaults.gestureInclusion(state),
    reverseLayout: Boolean = false,
    key: ((index: Int) -> Any)? = null,
    rotaryScrollableBehavior: RotaryScrollableBehavior? = null,
    content: @Composable PagerScope.(page: Int) -> Unit,
) {
    var allowPaging by remember { mutableStateOf(true) }
    var pagerCoordinates = remember { mutableStateOf<LayoutCoordinates?>(null) }

    val originalTouchSlop = LocalViewConfiguration.current.touchSlop
    val focusRequester = remember { FocusRequester() }

    CustomTouchSlopProvider(newTouchSlop = originalTouchSlop * CustomTouchSlopMultiplier) {
        val rotaryModifier =
            if (rotaryScrollableBehavior != null && userScrollEnabled)
                Modifier.requestFocusOnHierarchyActive()
                    .rotaryScrollable(
                        behavior = rotaryScrollableBehavior,
                        focusRequester = focusRequester,
                        reverseDirection = reverseLayout,
                    )
            else Modifier

        HorizontalPager(
            state = state.pagerState,
            modifier =
                modifier
                    .onPlaced { layoutCoordinates -> pagerCoordinates.value = layoutCoordinates }
                    .pointerInput(gestureInclusion, userScrollEnabled) {
                        if (!userScrollEnabled || pagerCoordinates.value == null) {
                            allowPaging = false
                            return@pointerInput
                        }
                        awaitEachGesture {
                            allowPaging = true
                            val firstDown = awaitFirstDown(false, PointerEventPass.Initial)

                            allowPaging =
                                !gestureInclusion.ignoreGestureStart(
                                    firstDown.position,
                                    pagerCoordinates.value!!,
                                )
                        }
                    }
                    .semantics {
                        horizontalScrollAxisRange =
                            if (allowPaging) {
                                ScrollAxisRange(
                                    value = { state.currentPage.toFloat() },
                                    maxValue = { state.pageCount.toFloat() },
                                )
                            } else {
                                // signals system swipe to dismiss that it can take over
                                ScrollAxisRange(value = { 0f }, maxValue = { 0f })
                            }
                    }
                    .then(rotaryModifier),
            contentPadding = contentPadding,
            pageSize = PageSize.Fill,
            beyondViewportPageCount = beyondViewportPageCount,
            pageSpacing = 0.dp,
            verticalAlignment = Alignment.CenterVertically,
            flingBehavior = flingBehavior,
            userScrollEnabled = userScrollEnabled && allowPaging,
            reverseLayout = reverseLayout,
            key = key,
            snapPosition = SnapPosition.Start,
        ) { page ->
            CustomTouchSlopProvider(newTouchSlop = originalTouchSlop) {
                val parentScreenActive = LocalScreenIsActive.current
                CompositionLocalProvider(
                    LocalScreenIsActive provides (state.currentPage == page && parentScreenActive)
                ) {
                    Box(
                        if (rotaryScrollableBehavior == null) {
                            Modifier.hierarchicalFocusGroup(state.currentPage == page)
                        } else {
                            Modifier
                        }
                    ) {
                        WearPagerScopeImpl.content(page)
                    }
                }
            }
        }
    }
}

/**
 * A vertically scrolling Pager optimized for Wear OS devices. This component wraps the standard
 * Compose Foundation [VerticalPager] and provides Wear-specific enhancements to improve
 * performance, usability, and adherence to Wear OS design guidelines.
 *
 * [VerticalPager] supports rotary input by default. Rotary input allows users to scroll through the
 * pager's content - by using a crown or a rotating bezel on their Wear OS device. It can be
 * modified or turned off using the [rotaryScrollableBehavior] parameter.
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
 * @param rotaryScrollableBehavior Parameter for changing rotary behavior. We recommend to use
 *   [RotaryScrollableDefaults.snapBehavior] with pagerState parameter. Passing null turns off the
 *   rotary handling if it is not required.
 * @param content A composable function that defines the content of each page displayed by the
 *   Pager. This is where the UI elements that should appear within each page should be placed.
 */
@Composable
public fun VerticalPager(
    state: PagerState,
    modifier: Modifier = Modifier,
    contentPadding: PaddingValues = PaddingValues(0.dp),
    beyondViewportPageCount: Int = PagerDefaults.BeyondViewportPageCount,
    flingBehavior: TargetedFlingBehavior = PagerDefaults.snapFlingBehavior(state = state),
    userScrollEnabled: Boolean = true,
    reverseLayout: Boolean = false,
    key: ((index: Int) -> Any)? = null,
    rotaryScrollableBehavior: RotaryScrollableBehavior? =
        RotaryScrollableDefaults.snapBehavior(state),
    content: @Composable PagerScope.(page: Int) -> Unit,
) {
    val focusRequester = remember { FocusRequester() }
    val rotaryModifier =
        if (rotaryScrollableBehavior != null && userScrollEnabled)
            Modifier.requestFocusOnHierarchyActive()
                .rotaryScrollable(
                    behavior = rotaryScrollableBehavior,
                    focusRequester = focusRequester,
                    reverseDirection = reverseLayout,
                )
        else Modifier

    VerticalPager(
        state = state.pagerState,
        modifier = modifier.then(rotaryModifier),
        contentPadding = contentPadding,
        pageSize = PageSize.Fill,
        beyondViewportPageCount = beyondViewportPageCount,
        pageSpacing = 0.dp,
        horizontalAlignment = Alignment.CenterHorizontally,
        flingBehavior = flingBehavior,
        userScrollEnabled = userScrollEnabled,
        reverseLayout = reverseLayout,
        key = key,
        snapPosition = SnapPosition.Start,
    ) { page ->
        val parentScreenActive = LocalScreenIsActive.current
        CompositionLocalProvider(
            LocalScreenIsActive provides (state.currentPage == page && parentScreenActive)
        ) {
            Box(
                if (rotaryScrollableBehavior == null) {
                    Modifier.hierarchicalFocusGroup(state.currentPage == page)
                } else {
                    Modifier
                }
            ) {
                WearPagerScopeImpl.content(page)
            }
        }
    }
}

/** Contains the default values used by [Pager]. These are optimised for Wear. */
public object PagerDefaults {
    /**
     * The default behaviour for when [HorizontalPager] should handle gestures. In this
     * implementation of [gestureInclusion], scroll events that originate in the left edge of the
     * first page of the Pager (as determined by [LeftEdgeZoneFraction]) will be ignored. This
     * allows swipe-to-dismiss handlers (if present) to handle the gesture in this region. However
     * if talkback is enabled then the Pager will always handle gestures, never allowing swipe to
     * dismiss handlers to take over.
     *
     * @param state The state of the [HorizontalPager]. Used to determine the current page.
     * @param edgeZoneFraction The fraction of the screen width from the left edge where gestures
     *   should be ignored on the first page. Defaults to [LeftEdgeZoneFraction].
     */
    @Composable
    public fun gestureInclusion(
        state: PagerState,
        edgeZoneFraction: Float = LeftEdgeZoneFraction,
    ): GestureInclusion {
        val touchExplorationStateProvider = remember { DefaultTouchExplorationStateProvider() }
        val touchExplorationServicesEnabled by touchExplorationStateProvider.touchExplorationState()

        return remember(state, touchExplorationServicesEnabled, edgeZoneFraction) {
            object : GestureInclusion {
                override fun ignoreGestureStart(
                    offset: Offset,
                    layoutCoordinates: LayoutCoordinates,
                ): Boolean {
                    if (touchExplorationServicesEnabled || state.currentPage != 0) {
                        return false
                    }

                    // On Page 0 - only allow gestures to be consumed by Pager if they are on the
                    // right of edgeZoneFraction, gestures to the left of this can be ignored and
                    // handled by swipe to dismiss handlers
                    val screenOffset = layoutCoordinates.localToScreen(offset)
                    val screenWidth = layoutCoordinates.findRootCoordinates().size.width
                    return screenOffset.x <= screenWidth * edgeZoneFraction
                }
            }
        }
    }

    /**
     * Default fling behavior for pagers on Wear, snaps at most one page at a time.
     *
     * @param state The [PagerState] that controls the [Pager] to which this FlingBehavior will be
     *   applied to.
     * @param maxFlingPages the maximum number of pages this [Pager] is allowed to fling after
     *   scrolling is finished and fling has started.
     * @param decayAnimationSpec The animation spec used to approach the target offset. When the
     *   fling velocity is large enough. Large enough means large enough to naturally decay. For
     *   single page snapping this usually never happens since there won't be enough space to run a
     *   decay animation.
     * @param snapAnimationSpec The animation spec used to finally snap to the position. This
     *   animation will be often used in 2 cases: 1) There was enough space to an approach
     *   animation, the Pager will use [snapAnimationSpec] in the last step of the animation to
     *   settle the page into position. 2) There was not enough space to run the approach animation.
     *   By default a Spring animation with no bounciness and high stiffness is used to ensure the
     *   Pager settles quickly so that contents are focused and clickable.
     * @param snapPositionalThreshold If the fling has a low velocity (e.g. slow scroll), this fling
     *   behavior will use this snap threshold in order to determine if the pager should snap back
     *   or move forward. Use a number between 0 and 1 as a fraction of the page size that needs to
     *   be scrolled before the Pager considers it should move to the next page. For instance, if
     *   snapPositionalThreshold = 0.35, it means if this pager is scrolled with a slow velocity and
     *   the Pager scrolls more than 35% of the page size, then will jump to the next page, if not
     *   it scrolls back. Note that any fling that has high enough velocity will *always* move to
     *   the next page in the direction of the fling.
     */
    @Composable
    public fun snapFlingBehavior(
        state: PagerState,
        maxFlingPages: Int = 1,
        decayAnimationSpec: DecayAnimationSpec<Float> = rememberSplineBasedDecay(),
        snapAnimationSpec: AnimationSpec<Float> = PagerDefaults.SnapAnimationSpec,
        @FloatRange(from = 0.0, to = 1.0) snapPositionalThreshold: Float = 0.5f,
    ): TargetedFlingBehavior {
        return ComposePagerDefaults.flingBehavior(
            state = state.pagerState,
            pagerSnapDistance = ComposePagerSnapDistance.atMost(maxFlingPages),
            decayAnimationSpec = decayAnimationSpec,
            snapAnimationSpec = snapAnimationSpec,
            snapPositionalThreshold = snapPositionalThreshold,
        )
    }

    /**
     * The recommended medium-high stiffness used by default for the spring stiffness parameter in
     * the Pager's snap animation.
     */
    private val MediumHighStiffness: Float = 2000f

    /**
     * The default spring animation used for the Pager's snap animation spec - a spring based
     * animation with medium-high stiffness and no bounce.
     */
    public val SnapAnimationSpec: AnimationSpec<Float> =
        spring(Spring.DampingRatioNoBouncy, MediumHighStiffness)

    /**
     * The default value used to configure the size of the left edge zone in a [HorizontalPager].
     * The left edge zone in this case refers to the leftmost edge of the screen, in this region in
     * a [Pager] it is common to disable scrolling in order for swipe-to-dismiss handlers to take
     * over.
     */
    public val LeftEdgeZoneFraction: Float = 0.15f

    /**
     * The default value of beyondViewportPageCount used to specify the number of pages to compose
     * and layout before and after the visible pages. It does not include the pages automatically
     * composed and laid out by the pre-fetcher in the direction of the scroll during scroll events.
     */
    public val BeyondViewportPageCount: Int = 0
}

internal const val CustomTouchSlopMultiplier = 1.10f
