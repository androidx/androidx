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

import androidx.compose.animation.core.AnimationSpec
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.spring
import androidx.compose.foundation.gestures.Orientation
import androidx.compose.foundation.gestures.TargetedFlingBehavior
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxScope
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawWithContent
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.TransformOrigin
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.graphics.isSpecified
import androidx.compose.ui.platform.LocalLayoutDirection
import androidx.compose.ui.unit.LayoutDirection
import androidx.compose.ui.util.lerp
import androidx.wear.compose.foundation.LocalReduceMotion
import androidx.wear.compose.foundation.LocalScreenIsActive
import androidx.wear.compose.foundation.ScrollInfoProvider
import androidx.wear.compose.foundation.pager.PagerDefaults
import androidx.wear.compose.foundation.pager.PagerState
import androidx.wear.compose.material3.PagerScaffoldDefaults.snapWithSpringFlingBehavior
import androidx.wear.compose.materialcore.screenHeightDp
import androidx.wear.compose.materialcore.screenWidthDp
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
 *   visible at all times, setting this to [PagerScaffoldDefaults.FadeOutAnimationSpec] ensures the
 *   indicator only shows during paging, and fades out when the Pager is idle.
 * @param content A composable function where a [HorizontalPager] can be added.
 */
@Composable
public fun HorizontalPagerScaffold(
    pagerState: PagerState,
    modifier: Modifier = Modifier,
    pageIndicator: (@Composable BoxScope.() -> Unit)? = { HorizontalPageIndicator(pagerState) },
    pageIndicatorAnimationSpec: AnimationSpec<Float>? = null,
    content: @Composable () -> Unit,
): Unit =
    PagerScaffoldImpl(
        orientation = Orientation.Horizontal,
        scrollInfoProvider = ScrollInfoProvider(pagerState),
        pager = content,
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
 * [VerticalPagerScaffold] supports rotary input by default. Rotary input allows users to scroll
 * through the pager's content - by using a crown or a rotating bezel on their Wear OS device. It
 * can be modified or turned off using the [rotaryScrollableBehavior] parameter.
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
 *   visible at all times, setting this to [PagerScaffoldDefaults.FadeOutAnimationSpec] ensures the
 *   indicator only shows during paging, and fades out when the Pager is idle.
 * @param content A composable function where a [VerticalPager] can be added.
 */
@Composable
public fun VerticalPagerScaffold(
    pagerState: PagerState,
    modifier: Modifier = Modifier,
    pageIndicator: (@Composable BoxScope.() -> Unit)? = { VerticalPageIndicator(pagerState) },
    pageIndicatorAnimationSpec: AnimationSpec<Float>? = null,
    content: @Composable () -> Unit,
): Unit =
    PagerScaffoldImpl(
        orientation = Orientation.Vertical,
        scrollInfoProvider = ScrollInfoProvider(pagerState),
        pager = content,
        modifier = modifier,
        pagerState = pagerState,
        pageIndicator = pageIndicator,
        pageIndicatorAnimationSpec = pageIndicatorAnimationSpec,
    )

/**
 * Animates a page within a [Pager] with a scaling and scrim effect based on its position.
 *
 * This composable applies a scaling animation and a scrim overlay to the page content, creating a
 * visual cue for page transitions. The animation is responsive to the page's position within the
 * [Pager] and adapts to the device's reduce motion settings and layout direction.
 *
 * @param pageIndex The index of the page being animated.
 * @param pagerState The [PagerState] of the [Pager].
 * @param contentScrimColor The color of the scrim overlay applied during page transitions. Defaults
 *   to the background color of the [MaterialTheme]. Set this to transparent to have no scrim
 *   applied during page transitions.
 * @param content The composable content of the page.
 */
@Composable
public fun AnimatedPage(
    pageIndex: Int,
    pagerState: PagerState,
    contentScrimColor: Color = MaterialTheme.colorScheme.background,
    content: @Composable (() -> Unit),
) {
    val isReduceMotionEnabled = LocalReduceMotion.current
    val isRtlEnabled = LocalLayoutDirection.current == LayoutDirection.Rtl
    val orientation = remember(pagerState) { pagerState.layoutInfo.orientation }
    val numberOfIntervals =
        (if (orientation == Orientation.Horizontal) screenWidthDp() else screenHeightDp()) / 2

    val currentPageOffsetFraction by
        remember(pagerState) {
            derivedStateOf {
                (pagerState.currentPageOffsetFraction * numberOfIntervals).toInt() /
                    numberOfIntervals.toFloat()
            }
        }

    val graphicsLayerModifier =
        if (isReduceMotionEnabled) Modifier
        else
            Modifier.graphicsLayer {
                val direction = if (isRtlEnabled) -1 else 1
                val offsetFraction = currentPageOffsetFraction
                val isSwipingRightToLeft = direction * offsetFraction > 0
                val isSwipingLeftToRight = direction * offsetFraction < 0
                val isCurrentPage: Boolean = pageIndex == pagerState.currentPage
                val shouldAnchorRight =
                    (isSwipingRightToLeft && isCurrentPage) ||
                        (isSwipingLeftToRight && !isCurrentPage)
                val pivotFractionX = if (shouldAnchorRight) 1f else 0f
                transformOrigin =
                    if (pagerState.layoutInfo.orientation == Orientation.Horizontal) {
                        TransformOrigin(pivotFractionX, 0.5f)
                    } else {
                        // Flip X and Y for vertical pager
                        TransformOrigin(0.5f, pivotFractionX)
                    }
                val pageTransitionFraction =
                    getPageTransitionFraction(isCurrentPage, offsetFraction)
                val scale = lerp(start = 1f, stop = 0.55f, fraction = pageTransitionFraction)
                scaleX = scale
                scaleY = scale
            }
    Box(
        modifier =
            graphicsLayerModifier
                .drawWithContent {
                    drawContent()
                    if (contentScrimColor.isSpecified) {
                        val isCurrentPage: Boolean = pageIndex == pagerState.currentPage

                        val pageTransitionFraction =
                            getPageTransitionFraction(isCurrentPage, currentPageOffsetFraction)
                        val color =
                            contentScrimColor.copy(
                                alpha =
                                    lerp(start = 0f, stop = 0.5f, fraction = pageTransitionFraction)
                            )

                        drawCircle(color = color)
                    }
                }
                .clip(CircleShape)
    ) {
        content()
    }
}

/** Contains default values used for [HorizontalPagerScaffold] and [VerticalPagerScaffold]. */
public object PagerScaffoldDefaults {
    /**
     * Recommended fling behavior for pagers on Wear when using Material3, snaps at most one page at
     * a time. This behavior is tailored for a smooth, spring-like snapping effect, enhancing the
     * user experience with a more fluid transition between pages.
     *
     * Example of using [HorizontalPager] and [snapWithSpringFlingBehavior]:
     *
     * @sample androidx.wear.compose.material3.samples.HorizontalPagerScaffoldSample
     *
     * Example of using [VerticalPager] and [snapWithSpringFlingBehavior]:
     *
     * @sample androidx.wear.compose.material3.samples.VerticalPagerScaffoldSample
     * @param state The [PagerState] that controls the [Pager] to which this FlingBehavior will be
     *   applied to.
     */
    @Composable
    public fun snapWithSpringFlingBehavior(state: PagerState): TargetedFlingBehavior {
        return PagerDefaults.snapFlingBehavior(
            state = state,
            maxFlingPages = 1,
            snapAnimationSpec = MaterialTheme.motionScheme.defaultSpatialSpec(),
            snapPositionalThreshold = 0.35f,
        )
    }

    /**
     * The default value for the indicator fade out animation spec. Use this to fade out the page
     * indicator when paging has stopped.
     */
    public val FadeOutAnimationSpec: AnimationSpec<Float> =
        spring(stiffness = Spring.StiffnessMediumLow)
}

@Composable
private fun PagerScaffoldImpl(
    orientation: Orientation,
    scrollInfoProvider: ScrollInfoProvider,
    pager: @Composable () -> Unit,
    pagerState: PagerState,
    modifier: Modifier,
    pageIndicator: (@Composable BoxScope.() -> Unit)?,
    pageIndicatorAnimationSpec: AnimationSpec<Float>?,
) {
    val scaffoldState = LocalScaffoldState.current
    val key = remember { Any() }

    // Update the timeText & scrollInfoProvider if there is a change and the screen is already
    // present
    scaffoldState.screenContent.updateIfNeeded(key, timeText = null, scrollInfoProvider)

    DisposableEffect(key) { onDispose { scaffoldState.screenContent.removeScreen(key) } }

    scaffoldState.screenContent.UpdateIdlingDetectorIfNeeded()

    val screenIsActive = LocalScreenIsActive.current
    LaunchedEffect(screenIsActive) {
        if (screenIsActive) {
            scaffoldState.screenContent.addScreen(key, timeText = null, scrollInfoProvider)
        } else {
            scaffoldState.screenContent.removeScreen(key)
        }
    }

    Box(modifier = modifier.fillMaxSize()) {
        pager()

        AnimatedIndicator(
            isVisible = {
                scaffoldState.screenContent.screenStage.value != ScreenStage.Idle ||
                    pagerState.isScrollInProgress
            },
            animationSpec = pageIndicatorAnimationSpec,
            content = pageIndicator,
        )
    }
}

private fun getPageTransitionFraction(
    isCurrentPage: Boolean,
    currentPageOffsetFraction: Float,
): Float {
    return if (isCurrentPage) {
        currentPageOffsetFraction.absoluteValue
    } else {
        // interpolate left or right pages in opposite direction
        1 - currentPageOffsetFraction.absoluteValue
    }
}
