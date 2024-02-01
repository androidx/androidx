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
import androidx.compose.foundation.BaseLazyLayoutTestWithOrientation
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.LocalOverscrollConfiguration
import androidx.compose.foundation.background
import androidx.compose.foundation.focusable
import androidx.compose.foundation.gestures.Orientation
import androidx.compose.foundation.gestures.TargetedFlingBehavior
import androidx.compose.foundation.gestures.snapping.SnapPosition
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.text.BasicText
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.rememberCoroutineScope
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

@OptIn(ExperimentalFoundationApi::class)
open class BasePagerTest(private val config: ParamConfig) :
    BaseLazyLayoutTestWithOrientation(config.orientation) {

    lateinit var scope: CoroutineScope
    var pagerSize: Int = 0
    var placed = mutableSetOf<Int>()
    var focused = mutableSetOf<Int>()
    var pageSize: Int = 0
    lateinit var focusManager: FocusManager
    lateinit var initialFocusedItem: FocusRequester
    var composeView: View? = null
    lateinit var pagerState: PagerState

    fun TouchInjectionScope.swipeWithVelocityAcrossMainAxis(velocity: Float, delta: Float? = null) {
        val end = if (delta == null) {
            layoutEnd
        } else {
            if (vertical) {
                layoutStart.copy(y = layoutStart.y + delta)
            } else {
                layoutStart.copy(x = layoutStart.x + delta)
            }
        }
        swipeWithVelocity(layoutStart, end, velocity)
    }

    fun TouchInjectionScope.swipeWithVelocityAcrossCrossAxis(
        velocity: Float,
        delta: Float? = null
    ) {
        val end = if (delta == null) {
            layoutEnd
        } else {
            if (vertical) {
                layoutStart.copy(x = layoutStart.x + delta)
            } else {
                layoutStart.copy(y = layoutStart.y + delta)
            }
        }
        swipeWithVelocity(layoutStart, end, velocity)
    }

    fun Modifier.fillMaxCrossAxis() =
        if (vertical) {
            this.fillMaxWidth()
        } else {
            this.fillMaxHeight()
        }

    internal fun createPager(
        initialPage: Int = 0,
        initialPageOffsetFraction: Float = 0f,
        pageCount: () -> Int = { DefaultPageCount },
        modifier: Modifier = Modifier,
        outOfBoundsPageCount: Int = config.outOfBoundsPageCount,
        pageSize: () -> PageSize = { PageSize.Fill },
        userScrollEnabled: Boolean = true,
        snappingPage: PagerSnapDistance = PagerSnapDistance.atMost(1),
        nestedScrollConnection: NestedScrollConnection = object : NestedScrollConnection {},
        additionalContent: @Composable () -> Unit = {},
        contentPadding: PaddingValues = config.mainAxisContentPadding,
        pageSpacing: Dp = config.pageSpacing,
        reverseLayout: Boolean = config.reverseLayout,
        snapPositionalThreshold: Float = 0.5f,
        key: ((index: Int) -> Any)? = null,
        snapPosition: SnapPosition = config.snapPosition.first,
        flingBehavior: TargetedFlingBehavior? = null,
        pageContent: @Composable PagerScope.(page: Int) -> Unit = { Page(index = it) }
    ) {

        rule.setContent {
            val state = rememberPagerState(initialPage, initialPageOffsetFraction, pageCount).also {
                pagerState = it
            }
            composeView = LocalView.current
            focusManager = LocalFocusManager.current
            val resolvedFlingBehavior = flingBehavior ?: PagerDefaults.flingBehavior(
                state = state,
                pagerSnapDistance = snappingPage,
                snapPositionalThreshold = snapPositionalThreshold
            )
            CompositionLocalProvider(
                LocalLayoutDirection provides config.layoutDirection,
                LocalOverscrollConfiguration provides null
            ) {
                scope = rememberCoroutineScope()
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .nestedScroll(nestedScrollConnection)
                ) {
                    HorizontalOrVerticalPager(
                        state = state,
                        outOfBoundsPageCount = outOfBoundsPageCount,
                        modifier = modifier
                            .testTag(PagerTestTag)
                            .onSizeChanged { pagerSize = if (vertical) it.height else it.width },
                        pageSize = pageSize(),
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
    }

    @Composable
    internal fun Page(index: Int, initialFocusedItemIndex: Int = 0) {
        val focusRequester = FocusRequester().also {
            if (index == initialFocusedItemIndex) initialFocusedItem = it
        }
        Box(modifier = Modifier
            .focusRequester(focusRequester)
            .onPlaced {
                placed.add(index)
                pageSize = if (vertical) it.size.height else it.size.width
            }
            .fillMaxSize()
            .background(Color.Blue)
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

    internal val scrollForwardSign: Int
        get() = if (vertical) {
            if (config.reverseLayout && config.layoutDirection == LayoutDirection.Rtl) {
                1
            } else if (!config.reverseLayout && config.layoutDirection == LayoutDirection.Rtl) {
                -1
            } else if (config.reverseLayout && config.layoutDirection == LayoutDirection.Ltr) {
                1
            } else {
                -1
            }
        } else {
            if (config.reverseLayout && config.layoutDirection == LayoutDirection.Rtl) {
                -1
            } else if (!config.reverseLayout && config.layoutDirection == LayoutDirection.Rtl) {
                1
            } else if (config.reverseLayout && config.layoutDirection == LayoutDirection.Ltr) {
                1
            } else {
                -1
            }
        }

    internal val TouchInjectionScope.layoutStart: Offset
        get() = if (vertical) {
            if (config.reverseLayout && config.layoutDirection == LayoutDirection.Rtl) {
                topCenter
            } else if (!config.reverseLayout && config.layoutDirection == LayoutDirection.Rtl) {
                bottomCenter
            } else if (config.reverseLayout && config.layoutDirection == LayoutDirection.Ltr) {
                topCenter
            } else {
                bottomCenter
            }
        } else {
            if (config.reverseLayout && config.layoutDirection == LayoutDirection.Rtl) {
                centerRight
            } else if (!config.reverseLayout && config.layoutDirection == LayoutDirection.Rtl) {
                centerLeft
            } else if (config.reverseLayout && config.layoutDirection == LayoutDirection.Ltr) {
                centerLeft
            } else {
                centerRight
            }
        }

    internal val TouchInjectionScope.layoutEnd: Offset
        get() = if (vertical) {
            if (config.reverseLayout && config.layoutDirection == LayoutDirection.Rtl) {
                bottomCenter
            } else if (!config.reverseLayout && config.layoutDirection == LayoutDirection.Rtl) {
                topCenter
            } else if (config.reverseLayout && config.layoutDirection == LayoutDirection.Ltr) {
                bottomCenter
            } else {
                topCenter
            }
        } else {
            if (config.reverseLayout && config.layoutDirection == LayoutDirection.Rtl) {
                centerLeft
            } else if (!config.reverseLayout && config.layoutDirection == LayoutDirection.Rtl) {
                centerRight
            } else if (config.reverseLayout && config.layoutDirection == LayoutDirection.Ltr) {
                centerRight
            } else {
                centerLeft
            }
        }

    @OptIn(ExperimentalFoundationApi::class)
    @Composable
    internal fun HorizontalOrVerticalPager(
        state: PagerState = rememberPagerState(pageCount = { DefaultPageCount }),
        modifier: Modifier = Modifier,
        userScrollEnabled: Boolean = true,
        reverseLayout: Boolean = false,
        contentPadding: PaddingValues = PaddingValues(0.dp),
        outOfBoundsPageCount: Int = 0,
        pageSize: PageSize = PageSize.Fill,
        flingBehavior: TargetedFlingBehavior = PagerDefaults.flingBehavior(state = state),
        pageSpacing: Dp = 0.dp,
        key: ((index: Int) -> Any)? = null,
        snapPosition: SnapPosition = config.snapPosition.first,
        pageContent: @Composable PagerScope.(pager: Int) -> Unit
    ) {
        if (vertical) {
            VerticalPager(
                state = state,
                modifier = modifier,
                userScrollEnabled = userScrollEnabled,
                reverseLayout = reverseLayout,
                contentPadding = contentPadding,
                outOfBoundsPageCount = outOfBoundsPageCount,
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
                outOfBoundsPageCount = outOfBoundsPageCount,
                pageSize = pageSize,
                flingBehavior = flingBehavior,
                pageSpacing = pageSpacing,
                key = key,
                snapPosition = snapPosition,
                pageContent = pageContent
            )
        }
    }

    internal fun confirmPageIsInCorrectPosition(
        currentPageIndex: Int,
        pageToVerifyPosition: Int = currentPageIndex,
        pageOffset: Float = 0f
    ) {
        val leftContentPadding =
            config.mainAxisContentPadding.calculateLeftPadding(config.layoutDirection)
        val topContentPadding = config.mainAxisContentPadding.calculateTopPadding()

        val (left, top) = with(rule.density) {
            val spacings = config.pageSpacing.roundToPx()
            val initialPageOffset = currentPageIndex * (pageSize + spacings)

            val position = pageToVerifyPosition * (pageSize + spacings) - initialPageOffset
            val positionWithOffset =
                position + (pageSize + spacings) * pageOffset * scrollForwardSign
            if (vertical) {
                0.dp to positionWithOffset.toDp()
            } else {
                positionWithOffset.toDp() to 0.dp
            }
        }
        rule.onNodeWithTag("$pageToVerifyPosition")
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

@OptIn(ExperimentalFoundationApi::class)
class ParamConfig(
    val orientation: Orientation,
    val reverseLayout: Boolean = false,
    val layoutDirection: LayoutDirection = LayoutDirection.Ltr,
    val pageSpacing: Dp = 0.dp,
    val mainAxisContentPadding: PaddingValues = PaddingValues(0.dp),
    val outOfBoundsPageCount: Int = 0,
    val snapPosition: Pair<SnapPosition, String> = SnapPosition.Start to "Start",
) {
    override fun toString(): String {
        return "orientation=$orientation " +
            "reverseLayout=$reverseLayout " +
            "layoutDirection=$layoutDirection " +
            "pageSpacing=$pageSpacing " +
            "mainAxisContentPadding=$mainAxisContentPadding " +
            "outOfBoundsPageCount=$outOfBoundsPageCount " +
            "snapPosition=${snapPosition.second}"
    }
}

internal const val PagerTestTag = "pager"
internal const val DefaultPageCount = 20
internal const val DefaultAnimationRepetition = 2
internal val TestOrientation = listOf(Orientation.Vertical, Orientation.Horizontal)

@OptIn(ExperimentalFoundationApi::class)
internal val AllOrientationsParams = mutableListOf<ParamConfig>().apply {
    for (orientation in TestOrientation) {
        add(ParamConfig(orientation = orientation))
    }
}
internal val TestReverseLayout = listOf(false, true)
internal val TestLayoutDirection = listOf(LayoutDirection.Rtl, LayoutDirection.Ltr)
internal val TestPageSpacing = listOf(0.dp, 8.dp)

internal val TestSnapPosition = listOf(
    SnapPosition.Start to "Start",
    SnapPosition.Center to "Center",
    SnapPosition.End to "End"
)

internal fun testContentPaddings(orientation: Orientation) = listOf(
    PaddingValues(0.dp),
    if (orientation == Orientation.Vertical)
        PaddingValues(vertical = 16.dp)
    else PaddingValues(horizontal = 16.dp),
    PaddingValues(start = 16.dp),
    PaddingValues(end = 16.dp)
)
