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

package androidx.wear.compose.material3

import androidx.annotation.FloatRange
import androidx.compose.animation.core.AnimationSpec
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.spring
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.gestures.Orientation
import androidx.compose.foundation.gestures.TargetedFlingBehavior
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxScope
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.pager.PagerScope
import androidx.compose.foundation.pager.PagerSnapDistance
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.key
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.TransformOrigin
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.graphics.isSpecified
import androidx.compose.ui.platform.LocalLayoutDirection
import androidx.compose.ui.unit.LayoutDirection
import androidx.compose.ui.unit.dp
import androidx.compose.ui.util.lerp
import androidx.wear.compose.foundation.ActiveFocusListener
import androidx.wear.compose.foundation.ScrollInfoProvider
import androidx.wear.compose.foundation.pager.HorizontalPager
import androidx.wear.compose.foundation.pager.PagerDefaults
import androidx.wear.compose.foundation.pager.PagerState
import androidx.wear.compose.foundation.pager.VerticalPager
import kotlin.math.absoluteValue

/**
 * [HorizontalPagerScaffold] is one of the Wear Material3 scaffold components.
 *
 * The scaffold components [AppScaffold] and [HorizontalPagerScaffold] lay out the structure of a
 * Pager and coordinate transitions of the [HorizontalPageIndicator] and [TimeText] components.
 *
 * [HorizontalPagerScaffold] displays the [HorizontalPageIndicator] at the center-end of the screen
 * by default and coordinates showing/hiding [TimeText] and [HorizontalPageIndicator] according to
 * whether the Pager is being paged, this is determined by the [PagerState].
 *
 * Example of using [AppScaffold] and [HorizontalPagerScaffold]:
 *
 * @sample androidx.wear.compose.material3.samples.HorizontalPagerScaffoldSample
 * @param pagerState The state of the pager controlling the page content.
 * @param modifier The modifier to be applied to the scaffold.
 * @param pageIndicator A composable function that defines the page indicator to be displayed. By
 *   default, it uses a [HorizontalPageIndicator].
 * @param pageIndicatorAnimationSpec - An optional parameter to set whether the page indicator
 *   should fade out when paging has finished. This is useful for when the underlying page content
 *   conflicts with the page indicator. By default this is null, so the page indicator will be
 *   visible at all times, setting this to [PagerScaffoldDefaults.FadeOutAnimation] ensures the
 *   indicator only shows during paging, and fades out when the Pager is idle.
 * @param content A composable function that takes the current page index as a parameter and defines
 *   the content to be displayed on that page.
 */
@Composable
fun HorizontalPagerScaffold(
    pagerState: PagerState,
    modifier: Modifier = Modifier,
    pageIndicator: (@Composable BoxScope.() -> Unit)? = { HorizontalPageIndicator(pagerState) },
    pageIndicatorAnimationSpec: AnimationSpec<Float>? = null,
    content: @Composable PagerScope.(page: Int) -> Unit,
) =
    PagerScaffoldImpl(
        scrollInfoProvider = ScrollInfoProvider(pagerState, orientation = Orientation.Horizontal),
        pagerContent = { AnimatedHorizontalPager(pagerState, content = content) },
        modifier = modifier,
        pagerState = pagerState,
        pageIndicator = pageIndicator,
        pageIndicatorAnimationSpec = pageIndicatorAnimationSpec,
    )

/**
 * [VerticalPagerScaffold] is one of the Wear Material3 scaffold components.
 *
 * The scaffold components [AppScaffold] and [VerticalPagerScaffold] lay out the structure of a
 * Pager and coordinate transitions of the [VerticalPageIndicator] and [TimeText] components.
 *
 * [VerticalPagerScaffold] displays the [VerticalPageIndicator] at the center-end of the screen by
 * default and coordinates showing/hiding [TimeText] and [VerticalPageIndicator] according to
 * whether the Pager is being paged, this is determined by the [PagerState].
 *
 * Example of using [AppScaffold] and [VerticalPagerScaffold]:
 *
 * @sample androidx.wear.compose.material3.samples.VerticalPagerScaffoldSample
 * @param pagerState The state of the pager controlling the page content.
 * @param modifier The modifier to be applied to the scaffold.
 * @param pageIndicator A composable function that defines the page indicator to be displayed. By
 *   default, it uses a [VerticalPageIndicator].
 * @param pageIndicatorAnimationSpec - An optional parameter to set whether the page indicator
 *   should fade out when paging has finished. This is useful for when the underlying page content
 *   conflicts with the page indicator. By default this is null, so the page indicator will be
 *   visible at all times, setting this to [PagerScaffoldDefaults.FadeOutAnimation] ensures the
 *   indicator only shows during paging, and fades out when the Pager is idle.
 * @param content A composable function that takes the current page index as a parameter and defines
 *   the content to be displayed on that page.
 */
@Composable
fun VerticalPagerScaffold(
    pagerState: PagerState,
    modifier: Modifier = Modifier,
    pageIndicator: (@Composable BoxScope.() -> Unit)? = { VerticalPageIndicator(pagerState) },
    pageIndicatorAnimationSpec: AnimationSpec<Float>? = null,
    content: @Composable PagerScope.(page: Int) -> Unit,
) =
    PagerScaffoldImpl(
        scrollInfoProvider = ScrollInfoProvider(pagerState, orientation = Orientation.Vertical),
        pagerContent = { AnimatedVerticalPager(pagerState, content = content) },
        modifier = modifier,
        pagerState = pagerState,
        pageIndicator = pageIndicator,
        pageIndicatorAnimationSpec = pageIndicatorAnimationSpec,
    )

/** Contains default values used for [HorizontalPagerScaffold] and [VerticalPagerScaffold]. */
object PagerScaffoldDefaults {
    /**
     * The default value for the indicator fade out animation spec. Use this to fade out the page
     * indicator when paging has stopped.
     */
    val FadeOutAnimation: AnimationSpec<Float> = spring(stiffness = Spring.StiffnessMediumLow)
}

@Composable
private fun PagerScaffoldImpl(
    scrollInfoProvider: ScrollInfoProvider,
    pagerContent: @Composable () -> Unit,
    pagerState: PagerState,
    modifier: Modifier,
    pageIndicator: (@Composable BoxScope.() -> Unit)?,
    pageIndicatorAnimationSpec: AnimationSpec<Float>?,
) {
    val scaffoldState = LocalScaffoldState.current
    val key = remember { Any() }

    key(scrollInfoProvider) {
        DisposableEffect(key) { onDispose { scaffoldState.removeScreen(key) } }

        ActiveFocusListener { focused ->
            if (focused) {
                scaffoldState.addScreen(key, null, scrollInfoProvider)
            } else {
                scaffoldState.removeScreen(key)
            }
        }
    }

    scaffoldState.UpdateIdlingDetectorIfNeeded()

    Box(modifier = modifier.fillMaxSize()) {
        pagerContent()

        AnimatedIndicator(
            isVisible = {
                scaffoldState.screenStage.value != ScreenStage.Idle || pagerState.isScrollInProgress
            },
            animationSpec = pageIndicatorAnimationSpec,
            content = pageIndicator,
        )
    }
}

/**
 * A full-screen horizontally scrolling Pager optimized for Wear OS devices. This component wraps
 * the Wear Compose Foundation [HorizontalPager] and provides Material3 animations. Note: If
 * accessibility is enabled the Wear system swipe to dismiss gesture will be disabled, in this case
 * an alternative exit method such as a button which dismisses the Pager should be provided.
 *
 * @param state The state to control this pager
 * @param modifier A modifier instance to be applied to this Pager outer layout
 * @param contentPadding a padding around the whole content. This will add padding for the content
 *   after it has been clipped, which is not possible via [modifier] param. You can use it to add a
 *   padding before the first page or after the last one.
 * @param contentScrimColor [Color] used for the scrim over the content composable during the swipe
 *   gesture. In order to turn off this effect, set this to Color.Unspecified.
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
@Composable
internal fun AnimatedHorizontalPager(
    state: PagerState,
    modifier: Modifier = Modifier,
    contentPadding: PaddingValues = PaddingValues(0.dp),
    contentScrimColor: Color = MaterialTheme.colorScheme.background,
    beyondViewportPageCount: Int = PagerDefaults.BeyondViewportPageCount,
    flingBehavior: TargetedFlingBehavior = snapWithSpringBehavior(state = state),
    userScrollEnabled: Boolean = true,
    reverseLayout: Boolean = false,
    key: ((index: Int) -> Any)? = null,
    @FloatRange(from = 0.0, to = 1.0)
    swipeToDismissEdgeZoneFraction: Float = PagerDefaults.SwipeToDismissEdgeZoneFraction,
    content: @Composable PagerScope.(page: Int) -> Unit
) {
    val touchExplorationStateProvider = remember { DefaultTouchExplorationStateProvider() }
    val touchExplorationServicesEnabled by touchExplorationStateProvider.touchExplorationState()

    HorizontalPager(
        state = state,
        modifier = modifier,
        contentPadding = contentPadding,
        beyondViewportPageCount = beyondViewportPageCount,
        flingBehavior = flingBehavior,
        userScrollEnabled = userScrollEnabled,
        reverseLayout = reverseLayout,
        key = key,
        swipeToDismissEdgeZoneFraction =
            if (touchExplorationServicesEnabled) 0f else swipeToDismissEdgeZoneFraction,
    ) { page ->
        AnimatedPageContent(
            orientation = Orientation.Horizontal,
            page = page,
            pagerState = state,
            contentScrimColor = contentScrimColor,
            content = { content(page) }
        )
    }
}

/**
 * A full-screen vertically scrolling Pager optimized for Wear OS devices. This component wraps the
 * Wear Compose Foundation [VerticalPager] and provides Material3 animations.
 *
 * @param state The state to control this pager
 * @param modifier A modifier instance to be apply to this Pager outer layout
 * @param contentPadding a padding around the whole content. This will add padding for the content
 *   after it has been clipped, which is not possible via [modifier] param. You can use it to add a
 *   padding before the first page or after the last one.
 * @param contentScrimColor [Color] used for the scrim over the content composable during the swipe
 *   gesture. In order to turn off this effect, set this to Color.Unspecified.
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
@Composable
internal fun AnimatedVerticalPager(
    state: PagerState,
    modifier: Modifier = Modifier,
    contentPadding: PaddingValues = PaddingValues(0.dp),
    contentScrimColor: Color = MaterialTheme.colorScheme.background,
    beyondViewportPageCount: Int = PagerDefaults.BeyondViewportPageCount,
    flingBehavior: TargetedFlingBehavior = snapWithSpringBehavior(state = state),
    userScrollEnabled: Boolean = true,
    reverseLayout: Boolean = false,
    key: ((index: Int) -> Any)? = null,
    content: @Composable PagerScope.(page: Int) -> Unit
) {
    VerticalPager(
        state = state,
        modifier = modifier,
        contentPadding = contentPadding,
        beyondViewportPageCount = beyondViewportPageCount,
        flingBehavior = flingBehavior,
        userScrollEnabled = userScrollEnabled,
        reverseLayout = reverseLayout,
        key = key,
    ) { page ->
        AnimatedPageContent(
            orientation = Orientation.Vertical,
            page = page,
            pagerState = state,
            contentScrimColor = contentScrimColor,
            content = { content(page) }
        )
    }
}

@Composable
internal fun snapWithSpringBehavior(
    state: PagerState,
): TargetedFlingBehavior {
    return PagerDefaults.snapFlingBehavior(
        state = state,
        pagerSnapDistance = PagerSnapDistance.atMost(1),
        snapAnimationSpec = spring(dampingRatio = 1f, stiffness = 200f),
        snapPositionalThreshold = 0.35f,
    )
}

@Composable
private fun AnimatedPageContent(
    orientation: Orientation,
    page: Int,
    pagerState: PagerState,
    contentScrimColor: Color,
    content: @Composable () -> Unit
) {
    val isRtlEnabled = LocalLayoutDirection.current == LayoutDirection.Rtl
    val isCurrentPage: Boolean = page == pagerState.currentPage
    val pageTransitionFraction =
        if (isCurrentPage) {
            pagerState.currentPageOffsetFraction.absoluteValue
        } else {
            // interpolate left or right pages in opposite direction
            1 - pagerState.currentPageOffsetFraction.absoluteValue
        }
    Box(
        modifier =
            Modifier.graphicsLayer {
                    val pivotFractionX by derivedStateOf {
                        val direction = if (isRtlEnabled) -1 else 1
                        val isSwipingRightToLeft =
                            direction * pagerState.currentPageOffsetFraction > 0
                        val isSwipingLeftToRight =
                            direction * pagerState.currentPageOffsetFraction < 0
                        val shouldAnchorRight =
                            (isSwipingRightToLeft && isCurrentPage) ||
                                (isSwipingLeftToRight && !isCurrentPage)
                        if (shouldAnchorRight) 1f else 0f
                    }
                    transformOrigin =
                        if (orientation == Orientation.Horizontal) {
                            TransformOrigin(pivotFractionX, 0.5f)
                        } else {
                            // Flip X and Y for vertical pager
                            TransformOrigin(0.5f, pivotFractionX)
                        }
                    val scale = lerp(start = 1f, stop = 0.55f, fraction = pageTransitionFraction)
                    scaleX = scale
                    scaleY = scale
                }
                .clip(CircleShape)
    ) {
        content()

        if (contentScrimColor.isSpecified) {
            Canvas(Modifier.fillMaxSize()) {
                val color =
                    contentScrimColor.copy(
                        alpha = lerp(start = 0f, stop = 0.5f, fraction = pageTransitionFraction)
                    )

                drawRect(color = color)
            }
        }
    }
}
