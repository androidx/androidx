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

package androidx.compose.foundation.pager

import android.view.View
import androidx.compose.foundation.LocalOverscrollConfiguration
import androidx.compose.foundation.background
import androidx.compose.foundation.focusable
import androidx.compose.foundation.gestures.Orientation
import androidx.compose.foundation.gestures.TargetedFlingBehavior
import androidx.compose.foundation.gestures.snapping.SnapPosition
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.text.BasicText
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.testutils.createParameterizedComposeTestRule
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.FocusManager
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.focus.onFocusChanged
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.nestedscroll.NestedScrollConnection
import androidx.compose.ui.input.nestedscroll.nestedScroll
import androidx.compose.ui.layout.onPlaced
import androidx.compose.ui.layout.onSizeChanged
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.platform.LocalLayoutDirection
import androidx.compose.ui.platform.LocalView
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.test.SemanticsNodeInteraction
import androidx.compose.ui.test.TouchInjectionScope
import androidx.compose.ui.test.assertPositionInRootIsEqualTo
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.swipeWithVelocity
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.LayoutDirection
import androidx.compose.ui.unit.dp
import kotlin.math.absoluteValue
import kotlin.test.assertTrue
import kotlinx.coroutines.CoroutineScope
import org.junit.Rule

/**
 * Transition BasePagerTest to be used whilst we adopt [ParameterizedInCompositionRule] in the
 * necessary Pager Tests.
 */
open class SingleParamBasePagerTest {

    @get:Rule val rule = createParameterizedComposeTestRule<SingleParamConfig>()

    lateinit var scope: CoroutineScope
    var pagerSize: Int = 0
    var placed = mutableSetOf<Int>()
    var focused = mutableSetOf<Int>()
    var pageSize: Int = 0
    lateinit var focusManager: FocusManager
    lateinit var initialFocusedItem: FocusRequester
    var composeView: View? = null
    lateinit var pagerState: PagerState

    @Composable
    internal fun ParameterizedPager(
        initialPage: Int = 0,
        initialPageOffsetFraction: Float = 0f,
        pageCount: () -> Int = { DefaultPageCount },
        modifier: Modifier = Modifier,
        orientation: Orientation = Orientation.Horizontal,
        beyondViewportPageCount: Int = PagerDefaults.BeyondViewportPageCount,
        pageSize: PageSize = PageSize.Fill,
        userScrollEnabled: Boolean = true,
        snappingPage: PagerSnapDistance = PagerSnapDistance.atMost(1),
        nestedScrollConnection: NestedScrollConnection = object : NestedScrollConnection {},
        additionalContent: @Composable () -> Unit = {},
        contentPadding: PaddingValues = PaddingValues(0.dp),
        pageSpacing: Dp = 0.dp,
        reverseLayout: Boolean = false,
        snapPositionalThreshold: Float = 0.5f,
        key: ((index: Int) -> Any)? = null,
        snapPosition: SnapPosition = SnapPosition.Start,
        flingBehavior: TargetedFlingBehavior? = null,
        layoutDirection: LayoutDirection = LayoutDirection.Ltr,
        pageContent: @Composable PagerScope.(page: Int) -> Unit = { page ->
            Page(index = page, orientation = orientation)
        }
    ) {
        val state =
            rememberPagerState(initialPage, initialPageOffsetFraction, pageCount).also {
                pagerState = it
            }
        composeView = LocalView.current
        focusManager = LocalFocusManager.current

        CompositionLocalProvider(
            LocalLayoutDirection provides layoutDirection,
            LocalOverscrollConfiguration provides null
        ) {
            val resolvedFlingBehavior =
                flingBehavior
                    ?: PagerDefaults.flingBehavior(
                        state = state,
                        pagerSnapDistance = snappingPage,
                        snapPositionalThreshold = snapPositionalThreshold
                    )

            scope = rememberCoroutineScope()
            Box(modifier = Modifier.fillMaxSize().nestedScroll(nestedScrollConnection)) {
                HorizontalOrVerticalPager(
                    state = state,
                    beyondViewportPageCount = beyondViewportPageCount,
                    orientation = orientation,
                    modifier =
                        modifier.testTag(PagerTestTag).onSizeChanged {
                            pagerSize =
                                if (orientation == Orientation.Vertical) it.height else it.width
                        },
                    pageSize = pageSize,
                    userScrollEnabled = userScrollEnabled,
                    reverseLayout = reverseLayout,
                    flingBehavior = resolvedFlingBehavior,
                    pageSpacing = pageSpacing,
                    contentPadding = contentPadding,
                    pageContent = pageContent,
                    snapPosition = snapPosition,
                    key = key
                )
            }
        }
        additionalContent()
    }

    @Composable
    internal fun Page(index: Int, orientation: Orientation, initialFocusedItemIndex: Int = 0) {
        val focusRequester =
            FocusRequester().also { if (index == initialFocusedItemIndex) initialFocusedItem = it }
        Box(
            modifier =
                Modifier.focusRequester(focusRequester)
                    .onPlaced {
                        placed.add(index)
                        pageSize =
                            if (orientation == Orientation.Vertical) it.size.height
                            else it.size.width
                    }
                    .fillMaxSize()
                    .background(if (index % 2 == 0) Color.Blue else Color.Red)
                    .testTag("$index")
                    .onFocusChanged {
                        if (it.isFocused) {
                            focused.add(index)
                        } else {
                            focused.remove(index)
                        }
                    }
                    .focusable(),
            contentAlignment = Alignment.Center
        ) {
            BasicText(text = index.toString())
        }
    }

    internal fun onPager(): SemanticsNodeInteraction {
        return rule.onNodeWithTag(PagerTestTag)
    }

    @Composable
    internal fun HorizontalOrVerticalPager(
        state: PagerState = rememberPagerState(pageCount = { DefaultPageCount }),
        modifier: Modifier = Modifier,
        userScrollEnabled: Boolean = true,
        reverseLayout: Boolean = false,
        contentPadding: PaddingValues = PaddingValues(0.dp),
        beyondViewportPageCount: Int = 0,
        pageSize: PageSize = PageSize.Fill,
        flingBehavior: TargetedFlingBehavior = PagerDefaults.flingBehavior(state = state),
        pageSpacing: Dp = 0.dp,
        orientation: Orientation = Orientation.Horizontal,
        key: ((index: Int) -> Any)? = null,
        snapPosition: SnapPosition = SnapPosition.Start,
        pageContent: @Composable PagerScope.(pager: Int) -> Unit
    ) {
        if (orientation == Orientation.Vertical) {
            VerticalPager(
                state = state,
                modifier = modifier,
                userScrollEnabled = userScrollEnabled,
                reverseLayout = reverseLayout,
                contentPadding = contentPadding,
                beyondViewportPageCount = beyondViewportPageCount,
                pageSize = pageSize,
                flingBehavior = flingBehavior,
                pageSpacing = pageSpacing,
                key = key,
                snapPosition = snapPosition,
                pageContent = pageContent
            )
        } else {
            HorizontalPager(
                state = state,
                modifier = modifier,
                userScrollEnabled = userScrollEnabled,
                reverseLayout = reverseLayout,
                contentPadding = contentPadding,
                beyondViewportPageCount = beyondViewportPageCount,
                pageSize = pageSize,
                flingBehavior = flingBehavior,
                pageSpacing = pageSpacing,
                key = key,
                snapPosition = snapPosition,
                pageContent = pageContent
            )
        }
    }

    internal fun SingleParamConfig.confirmPageIsInCorrectPosition(
        currentPageIndex: Int,
        pageToVerifyPosition: Int = currentPageIndex,
        pageOffset: Float = 0f,
    ) {
        val leftContentPadding = mainAxisContentPadding.calculateLeftPadding(layoutDirection)
        val topContentPadding = mainAxisContentPadding.calculateTopPadding()

        val (left, top) =
            with(rule.density) {
                val spacings = pageSpacing.roundToPx()
                val initialPageOffset = currentPageIndex * (pageSize + spacings)

                val position = pageToVerifyPosition * (pageSize + spacings) - initialPageOffset
                val positionWithOffset =
                    position + (pageSize + spacings) * pageOffset * scrollForwardSign
                if (orientation == Orientation.Vertical) {
                    0.dp to positionWithOffset.toDp()
                } else {
                    positionWithOffset.toDp() to 0.dp
                }
            }
        rule
            .onNodeWithTag("$pageToVerifyPosition")
            .assertPositionInRootIsEqualTo(left + leftContentPadding, top + topContentPadding)
    }

    internal fun runAndWaitForPageSettling(block: () -> Unit) {
        block()
        rule.mainClock.advanceTimeUntil {
            pagerState.currentPageOffsetFraction != 0.0f
        } // wait for first move from drag
        rule.mainClock.advanceTimeUntil {
            pagerState.currentPageOffsetFraction.absoluteValue < 0.00001
        } // wait for fling settling
        // pump the clock twice and check we're still settled.
        rule.mainClock.advanceTimeByFrame()
        rule.mainClock.advanceTimeByFrame()
        assertTrue { pagerState.currentPageOffsetFraction.absoluteValue < 0.00001 }
    }
}

data class SingleParamConfig(
    val orientation: Orientation = Orientation.Horizontal,
    val reverseLayout: Boolean = false,
    val layoutDirection: LayoutDirection = LayoutDirection.Ltr,
    val pageSpacing: Dp = 0.dp,
    val mainAxisContentPadding: PaddingValues = PaddingValues(0.dp),
    val beyondViewportPageCount: Int = 0,
    val snapPosition: Pair<SnapPosition, String> = SnapPosition.Start to "Start",
) {
    fun TouchInjectionScope.swipeWithVelocityAcrossMainAxis(velocity: Float, delta: Float? = null) {
        val end =
            if (delta == null) {
                layoutEnd
            } else {
                if (orientation == Orientation.Vertical) {
                    layoutStart.copy(y = layoutStart.y + delta)
                } else {
                    layoutStart.copy(x = layoutStart.x + delta)
                }
            }
        swipeWithVelocity(layoutStart, end, velocity)
    }

    val TouchInjectionScope.layoutStart: Offset
        get() =
            if (orientation == Orientation.Vertical) {
                if (reverseLayout && layoutDirection == LayoutDirection.Rtl) {
                    topCenter
                } else if (!reverseLayout && layoutDirection == LayoutDirection.Rtl) {
                    bottomCenter
                } else if (reverseLayout && layoutDirection == LayoutDirection.Ltr) {
                    topCenter
                } else {
                    bottomCenter
                }
            } else {
                if (reverseLayout && layoutDirection == LayoutDirection.Rtl) {
                    centerRight
                } else if (!reverseLayout && layoutDirection == LayoutDirection.Rtl) {
                    centerLeft
                } else if (reverseLayout && layoutDirection == LayoutDirection.Ltr) {
                    centerLeft
                } else {
                    centerRight
                }
            }

    val TouchInjectionScope.layoutEnd: Offset
        get() =
            if (orientation == Orientation.Vertical) {
                if (reverseLayout && layoutDirection == LayoutDirection.Rtl) {
                    bottomCenter
                } else if (!reverseLayout && layoutDirection == LayoutDirection.Rtl) {
                    topCenter
                } else if (reverseLayout && layoutDirection == LayoutDirection.Ltr) {
                    bottomCenter
                } else {
                    topCenter
                }
            } else {
                if (reverseLayout && layoutDirection == LayoutDirection.Rtl) {
                    centerLeft
                } else if (!reverseLayout && layoutDirection == LayoutDirection.Rtl) {
                    centerRight
                } else if (reverseLayout && layoutDirection == LayoutDirection.Ltr) {
                    centerRight
                } else {
                    centerLeft
                }
            }

    val scrollForwardSign: Int
        get() =
            if (orientation == Orientation.Vertical) {
                if (reverseLayout && layoutDirection == LayoutDirection.Rtl) {
                    1
                } else if (!reverseLayout && layoutDirection == LayoutDirection.Rtl) {
                    -1
                } else if (reverseLayout && layoutDirection == LayoutDirection.Ltr) {
                    1
                } else {
                    -1
                }
            } else {
                if (reverseLayout && layoutDirection == LayoutDirection.Rtl) {
                    -1
                } else if (!reverseLayout && layoutDirection == LayoutDirection.Rtl) {
                    1
                } else if (reverseLayout && layoutDirection == LayoutDirection.Ltr) {
                    1
                } else {
                    -1
                }
            }

    val vertical: Boolean
        get() = orientation == Orientation.Vertical
}
