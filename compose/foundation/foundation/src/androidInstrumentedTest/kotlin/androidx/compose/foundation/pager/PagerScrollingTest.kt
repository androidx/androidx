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

import androidx.compose.foundation.gestures.Orientation
import androidx.compose.foundation.gestures.snapping.MinFlingVelocityDp
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.size
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.performTouchInput
import androidx.compose.ui.test.swipeWithVelocity
import androidx.compose.ui.unit.LayoutDirection
import androidx.compose.ui.unit.dp
import androidx.test.filters.LargeTest
import com.google.common.truth.Truth.assertThat
import kotlinx.coroutines.runBlocking
import org.junit.Test

@LargeTest
class PagerScrollingTest : SingleParamBasePagerTest() {

    private fun resetTestCase(initialPage: Int = 0) {
        rule.runOnIdle { runBlocking { pagerState.scrollToPage(initialPage) } }
    }

    @Test
    fun swipeWithLowVelocity_positionalThresholdLessThanDefaultThreshold_shouldBounceBack_ltr() =
        with(rule) {
            // Arrange
            setContent {
                ParameterizedPager(
                    initialPage = 5,
                    modifier = Modifier.fillMaxSize(),
                    orientation = it.orientation,
                    pageSpacing = it.pageSpacing
                )
            }

            forEachParameter(ParamsToTest) { param ->
                val swipeValue = 0.4f
                val delta = pagerSize * swipeValue * param.scrollForwardSign

                // Act - forward
                onPager().performTouchInput {
                    with(param) {
                        swipeWithVelocityAcrossMainAxis(0.5f * MinFlingVelocityDp.toPx(), delta)
                    }
                }
                waitForIdle()

                // Assert
                onNodeWithTag("5").assertIsDisplayed()
                param.confirmPageIsInCorrectPosition(5)

                // Act - backward
                onPager().performTouchInput {
                    with(param) {
                        swipeWithVelocityAcrossMainAxis(
                            0.5f * MinFlingVelocityDp.toPx(),
                            delta * -1
                        )
                    }
                }
                waitForIdle()

                // Assert
                onNodeWithTag("5").assertIsDisplayed()
                param.confirmPageIsInCorrectPosition(5)
                resetTestCase(5)
            }
        }

    @Test
    fun swipeWithLowVelocity_positionalThresholdLessThanDefaultThreshold_shouldBounceBack_rtl() =
        with(rule) {
            // Arrange
            setContent {
                ParameterizedPager(
                    initialPage = 5,
                    modifier = Modifier.fillMaxSize(),
                    orientation = it.orientation,
                    pageSpacing = it.pageSpacing,
                    layoutDirection = it.layoutDirection
                )
            }
            val ParamsWithRtl = ParamsToTest.map { it.copy(layoutDirection = LayoutDirection.Rtl) }
            forEachParameter(ParamsWithRtl) { param ->
                val swipeValue = 0.4f
                val delta = pagerSize * swipeValue

                // Act - forward
                onPager().performTouchInput {
                    val (start, end) =
                        if (param.orientation == Orientation.Vertical) {
                            topCenter to topCenter.copy(y = topCenter.y + delta)
                        } else {
                            centerRight to centerRight.copy(x = centerRight.x - delta)
                        }
                    swipeWithVelocity(start, end, 0.5f * MinFlingVelocityDp.toPx())
                }
                waitForIdle()

                // Assert
                onNodeWithTag("5").assertIsDisplayed()
                param.confirmPageIsInCorrectPosition(5)

                // Act - backward
                onPager().performTouchInput {
                    val (start, end) =
                        if (param.orientation == Orientation.Vertical) {
                            topCenter to topCenter.copy(y = topCenter.y - delta)
                        } else {
                            centerRight to centerRight.copy(x = centerRight.x + delta)
                        }
                    swipeWithVelocity(start, end, 0.5f * MinFlingVelocityDp.toPx())
                }
                waitForIdle()

                // Assert
                onNodeWithTag("5").assertIsDisplayed()
                param.confirmPageIsInCorrectPosition(5)
                resetTestCase(5)
            }
        }

    @Test
    fun swipeWithLowVelocity_positionalThresholdLessThanLowThreshold_shouldBounceBack() =
        with(rule) {
            // Arrange
            setContent {
                ParameterizedPager(
                    initialPage = 5,
                    modifier = Modifier.fillMaxSize(),
                    snapPositionalThreshold = 0.2f,
                    orientation = it.orientation,
                    pageSpacing = it.pageSpacing
                )
            }

            forEachParameter(ParamsToTest) { param ->
                val swipeValue = 0.1f
                val delta = pagerSize * swipeValue * param.scrollForwardSign

                // Act - forward
                runAndWaitForPageSettling {
                    onPager().performTouchInput {
                        with(param) {
                            swipeWithVelocityAcrossMainAxis(0.5f * MinFlingVelocityDp.toPx(), delta)
                        }
                    }
                }

                // Assert
                onNodeWithTag("5").assertIsDisplayed()
                param.confirmPageIsInCorrectPosition(5)

                // Act - backward
                runAndWaitForPageSettling {
                    onPager().performTouchInput {
                        with(param) {
                            swipeWithVelocityAcrossMainAxis(
                                0.5f * MinFlingVelocityDp.toPx(),
                                delta * -1
                            )
                        }
                    }
                }

                // Assert
                onNodeWithTag("5").assertIsDisplayed()
                param.confirmPageIsInCorrectPosition(5)
                resetTestCase(5)
            }
        }

    @Test
    fun swipeWithLowVelocity_positionalThresholdLessThanHighThreshold_shouldBounceBack() =
        with(rule) {
            // Arrange
            setContent {
                ParameterizedPager(
                    initialPage = 5,
                    modifier = Modifier.fillMaxSize(),
                    snapPositionalThreshold = 0.8f,
                    orientation = it.orientation,
                    pageSpacing = it.pageSpacing
                )
            }

            forEachParameter(ParamsToTest) { param ->
                val swipeValue = 0.6f
                val delta = pagerSize * swipeValue * param.scrollForwardSign

                // Act - forward
                runAndWaitForPageSettling {
                    onPager().performTouchInput {
                        with(param) {
                            swipeWithVelocityAcrossMainAxis(0.5f * MinFlingVelocityDp.toPx(), delta)
                        }
                    }
                }

                // Assert
                onNodeWithTag("5").assertIsDisplayed()
                param.confirmPageIsInCorrectPosition(5)

                // Act - backward
                runAndWaitForPageSettling {
                    onPager().performTouchInput {
                        with(param) {
                            swipeWithVelocityAcrossMainAxis(
                                0.5f * MinFlingVelocityDp.toPx(),
                                delta * -1
                            )
                        }
                    }
                }

                // Assert
                onNodeWithTag("5").assertIsDisplayed()
                param.confirmPageIsInCorrectPosition(5)
                resetTestCase(5)
            }
        }

    @Test
    fun swipeWithLowVelocity_positionalThresholdLessThanDefault_customPageSize_shouldBounceBack() =
        with(rule) {
            // Arrange
            setContent {
                ParameterizedPager(
                    initialPage = 2,
                    modifier = Modifier.fillMaxSize(),
                    pageSize = PageSize.Fixed(200.dp),
                    orientation = it.orientation,
                    pageSpacing = it.pageSpacing
                )
            }

            forEachParameter(ParamsToTest) { param ->
                val delta = (2.4f * pageSize) * param.scrollForwardSign // 2.4 pages
                // Act - forward
                onPager().performTouchInput {
                    with(param) {
                        swipeWithVelocityAcrossMainAxis(0.5f * MinFlingVelocityDp.toPx(), delta)
                    }
                }
                waitForIdle()

                // Assert
                rule.onNodeWithTag("4").assertIsDisplayed()
                param.confirmPageIsInCorrectPosition(4)

                // Act - backward
                onPager().performTouchInput {
                    with(param) {
                        swipeWithVelocityAcrossMainAxis(
                            0.5f * MinFlingVelocityDp.toPx(),
                            delta * -1
                        )
                    }
                }
                waitForIdle()

                // Assert
                rule.onNodeWithTag("2").assertIsDisplayed()
                param.confirmPageIsInCorrectPosition(2)
                resetTestCase(2)
            }
        }

    @Test
    fun swipeWithLowVelocity_atTheEndOfTheList_shouldNotMove() =
        with(rule) {
            // Arrange
            mainClock.autoAdvance = false
            setContent {
                ParameterizedPager(
                    initialPage = DefaultPageCount - 1,
                    modifier = Modifier.size(125.dp),
                    pageSize = PageSize.Fixed(50.dp),
                    orientation = it.orientation,
                    pageSpacing = it.pageSpacing
                )
            }

            forEachParameter(ParamsToTest) { param ->
                val swipeValue = 0.1f
                val delta =
                    pagerSize * swipeValue * param.scrollForwardSign * -1 // scroll a bit at the end

                // Act - forward
                onPager().performTouchInput {
                    with(param) {
                        swipeWithVelocityAcrossMainAxis(0.5f * MinFlingVelocityDp.toPx(), delta)
                    }
                }

                // Assert
                runOnIdle {
                    // page is out of snap
                    assertThat(pagerState.currentPageOffsetFraction).isNotEqualTo(0.0f)
                }
                resetTestCase(DefaultPageCount - 1)
            }
        }

    @Test
    fun swipeWithLowVelocity_positionalThresholdOverDefaultThreshold_shouldGoToNextPage() =
        with(rule) {
            // Arrange
            setContent {
                ParameterizedPager(
                    initialPage = 5,
                    modifier = Modifier.fillMaxSize(),
                    orientation = it.orientation,
                    pageSpacing = it.pageSpacing
                )
            }

            forEachParameter(ParamsToTest) { param ->
                val swipeValue = 0.51f
                val delta = pagerSize * swipeValue * param.scrollForwardSign

                // Act - forward
                onPager().performTouchInput {
                    with(param) {
                        swipeWithVelocityAcrossMainAxis(0.5f * MinFlingVelocityDp.toPx(), delta)
                    }
                }
                waitForIdle()

                // Assert
                onNodeWithTag("6").assertIsDisplayed()
                param.confirmPageIsInCorrectPosition(6)

                // Act - backward
                onPager().performTouchInput {
                    with(param) {
                        swipeWithVelocityAcrossMainAxis(
                            0.5f * MinFlingVelocityDp.toPx(),
                            delta * -1
                        )
                    }
                }
                waitForIdle()

                // Assert
                onNodeWithTag("5").assertIsDisplayed()
                param.confirmPageIsInCorrectPosition(5)
                resetTestCase(5)
            }
        }

    @Test
    fun swipeWithLowVelocity_positionalThresholdOverLowThreshold_shouldGoToNextPage() =
        with(rule) {
            // Arrange
            setContent {
                ParameterizedPager(
                    initialPage = 5,
                    modifier = Modifier.fillMaxSize(),
                    snapPositionalThreshold = 0.2f,
                    orientation = it.orientation,
                    pageSpacing = it.pageSpacing
                )
            }

            forEachParameter(ParamsToTest) { param ->
                val swipeValue = 0.21f
                val delta = pagerSize * swipeValue * param.scrollForwardSign

                // Act - forward
                onPager().performTouchInput {
                    with(param) {
                        swipeWithVelocityAcrossMainAxis(0.5f * MinFlingVelocityDp.toPx(), delta)
                    }
                }
                waitForIdle()

                // Assert
                onNodeWithTag("6").assertIsDisplayed()
                param.confirmPageIsInCorrectPosition(6)

                // Act - backward
                onPager().performTouchInput {
                    with(param) {
                        swipeWithVelocityAcrossMainAxis(
                            0.5f * MinFlingVelocityDp.toPx(),
                            delta * -1
                        )
                    }
                }
                waitForIdle()

                // Assert
                onNodeWithTag("5").assertIsDisplayed()
                param.confirmPageIsInCorrectPosition(5)
                resetTestCase(5)
            }
        }

    @Test
    fun swipeWithLowVelocity_onEdgeOfList_smallDeltas_shouldGoToClosestPage_backward() =
        with(rule) {
            // Arrange
            setContent {
                ParameterizedPager(
                    modifier = Modifier.fillMaxSize(),
                    orientation = it.orientation,
                    pageSpacing = it.pageSpacing
                )
            }

            forEachParameter(ParamsToTest) { param ->
                val delta = 10f * param.scrollForwardSign * -1

                onPager().performTouchInput {
                    down(center)
                    // series of backward delta on edge
                    moveBy(
                        Offset(
                            if (param.vertical) 0.0f else delta,
                            if (param.vertical) delta else 0.0f
                        )
                    )
                    moveBy(
                        Offset(
                            if (param.vertical) 0.0f else delta,
                            if (param.vertical) delta else 0.0f
                        )
                    )
                    moveBy(
                        Offset(
                            if (param.vertical) 0.0f else delta,
                            if (param.vertical) delta else 0.0f
                        )
                    )

                    // single delta on opposite direction
                    moveBy(
                        Offset(
                            if (param.vertical) 0.0f else -delta,
                            if (param.vertical) -delta else 0.0f
                        )
                    )
                    up()
                }
                mainClock.advanceTimeUntil { !pagerState.isScrollInProgress }

                // Assert
                onNodeWithTag("0").assertIsDisplayed()
                param.confirmPageIsInCorrectPosition(0)
                resetTestCase()
            }
        }

    @Test
    fun swipeWithLowVelocity_onEdgeOfList_smallDeltas_shouldGoToClosestPage_forward() =
        with(rule) {
            // Arrange
            setContent {
                ParameterizedPager(
                    modifier = Modifier.fillMaxSize(),
                    initialPage = DefaultPageCount - 1,
                    orientation = it.orientation,
                    pageSpacing = it.pageSpacing
                )
            }
            forEachParameter(ParamsToTest) { param ->
                val delta = 10f * param.scrollForwardSign

                onPager().performTouchInput {
                    down(center)
                    // series of backward delta on edge
                    moveBy(
                        Offset(
                            if (param.vertical) 0.0f else delta,
                            if (param.vertical) delta else 0.0f
                        )
                    )
                    moveBy(
                        Offset(
                            if (param.vertical) 0.0f else delta,
                            if (param.vertical) delta else 0.0f
                        )
                    )
                    moveBy(
                        Offset(
                            if (param.vertical) 0.0f else delta,
                            if (param.vertical) delta else 0.0f
                        )
                    )

                    // single delta on opposite direction
                    moveBy(
                        Offset(
                            if (param.vertical) 0.0f else -delta,
                            if (param.vertical) -delta else 0.0f
                        )
                    )
                    up()
                }
                mainClock.advanceTimeUntil { !pagerState.isScrollInProgress }

                // Assert
                onNodeWithTag("${DefaultPageCount - 1}").assertIsDisplayed()
                param.confirmPageIsInCorrectPosition(DefaultPageCount - 1)
                resetTestCase(DefaultPageCount - 1)
            }
        }

    @Test
    fun swipeWithLowVelocity_positionalThresholdOverThreshold_customPage_shouldGoToNextPage() =
        with(rule) {
            // Arrange
            setContent {
                ParameterizedPager(
                    initialPage = 2,
                    modifier = Modifier.fillMaxSize(),
                    pageSize = PageSize.Fixed(200.dp),
                    orientation = it.orientation,
                    pageSpacing = it.pageSpacing
                )
            }

            forEachParameter(ParamsToTest) { param ->
                val delta = 2.6f * pageSize * param.scrollForwardSign

                // Act - forward
                onPager().performTouchInput {
                    with(param) {
                        swipeWithVelocityAcrossMainAxis(0.5f * MinFlingVelocityDp.toPx(), delta)
                    }
                }
                waitForIdle()

                // Assert
                onNodeWithTag("5").assertIsDisplayed()
                param.confirmPageIsInCorrectPosition(5)

                // Act - backward
                onPager().performTouchInput {
                    with(param) {
                        swipeWithVelocityAcrossMainAxis(
                            0.5f * MinFlingVelocityDp.toPx(),
                            delta * -1
                        )
                    }
                }
                waitForIdle()

                // Assert
                onNodeWithTag("2").assertIsDisplayed()
                param.confirmPageIsInCorrectPosition(2)
                resetTestCase(2)
            }
        }

    @Test
    fun swipeWithLowVelocity_positionalThresholdOverHighThreshold_shouldGoToNextPage() =
        with(rule) {
            // Arrange
            setContent {
                ParameterizedPager(
                    initialPage = 5,
                    modifier = Modifier.fillMaxSize(),
                    snapPositionalThreshold = 0.8f,
                    orientation = it.orientation,
                    pageSpacing = it.pageSpacing
                )
            }

            forEachParameter(ParamsToTest) { param ->
                val swipeValue = 0.81f
                val delta = pagerSize * swipeValue * param.scrollForwardSign

                // Act - forward
                runAndWaitForPageSettling {
                    onPager().performTouchInput {
                        with(param) {
                            swipeWithVelocityAcrossMainAxis(0.5f * MinFlingVelocityDp.toPx(), delta)
                        }
                    }
                }

                // Assert
                onNodeWithTag("6").assertIsDisplayed()
                param.confirmPageIsInCorrectPosition(6)

                // Act - backward
                runAndWaitForPageSettling {
                    onPager().performTouchInput {
                        with(param) {
                            swipeWithVelocityAcrossMainAxis(
                                0.5f * MinFlingVelocityDp.toPx(),
                                delta * -1
                            )
                        }
                    }
                }

                // Assert
                onNodeWithTag("5").assertIsDisplayed()
                param.confirmPageIsInCorrectPosition(5)
                resetTestCase(5)
            }
        }

    @Test
    fun swipeWithHighVelocity_defaultVelocityThreshold_shouldGoToNextPage() =
        with(rule) {
            // Arrange
            setContent {
                ParameterizedPager(
                    initialPage = 5,
                    modifier = Modifier.fillMaxSize(),
                    orientation = it.orientation,
                    pageSpacing = it.pageSpacing
                )
            }
            forEachParameter(ParamsToTest) { param ->
                // make sure the scroll distance is not enough to go to next page
                val delta = pagerSize * 0.4f * param.scrollForwardSign

                // Act - forward
                runAndWaitForPageSettling {
                    onPager().performTouchInput {
                        with(param) {
                            swipeWithVelocityAcrossMainAxis(1.1f * MinFlingVelocityDp.toPx(), delta)
                        }
                    }
                }

                // Assert
                onNodeWithTag("6").assertIsDisplayed()
                param.confirmPageIsInCorrectPosition(6)

                // Act - backward
                runAndWaitForPageSettling {
                    onPager().performTouchInput {
                        with(param) {
                            swipeWithVelocityAcrossMainAxis(
                                1.1f * MinFlingVelocityDp.toPx(),
                                delta * -1
                            )
                        }
                    }
                }

                // Assert
                onNodeWithTag("5").assertIsDisplayed()
                param.confirmPageIsInCorrectPosition(5)
                resetTestCase(5)
            }
        }

    @Test
    fun swipeWithHighVelocity_overHalfPage_shouldGoToNextPage() =
        with(rule) {
            // Arrange
            setContent {
                ParameterizedPager(
                    initialPage = 5,
                    modifier = Modifier.fillMaxSize(),
                    orientation = it.orientation,
                    pageSpacing = it.pageSpacing
                )
            }
            forEachParameter(ParamsToTest) { param ->
                // make sure the scroll distance is not enough to go to next page
                val delta = pagerSize * 0.8f * param.scrollForwardSign

                // Act - forward
                runAndWaitForPageSettling {
                    onPager().performTouchInput {
                        with(param) {
                            swipeWithVelocityAcrossMainAxis(1.1f * MinFlingVelocityDp.toPx(), delta)
                        }
                    }
                }

                // Assert
                onNodeWithTag("6").assertIsDisplayed()
                param.confirmPageIsInCorrectPosition(6)

                // Act - backward
                runAndWaitForPageSettling {
                    onPager().performTouchInput {
                        with(param) {
                            swipeWithVelocityAcrossMainAxis(
                                1.1f * MinFlingVelocityDp.toPx(),
                                delta * -1
                            )
                        }
                    }
                }

                // Assert
                onNodeWithTag("5").assertIsDisplayed()
                param.confirmPageIsInCorrectPosition(5)
                resetTestCase(5)
            }
        }

    @Test
    fun swipeWithHighVelocity_overHalfPage_customPageSize_shouldGoToNextPage() =
        with(rule) {
            // Arrange
            setContent {
                ParameterizedPager(
                    initialPage = 5,
                    modifier = Modifier.fillMaxSize(),
                    orientation = it.orientation,
                    pageSpacing = it.pageSpacing,
                    contentPadding = it.mainAxisContentPadding,
                    snapPosition = it.snapPosition.first,
                    pageSize = PageSize.Fixed(200.dp)
                )
            }

            val ParamsToTest =
                mutableListOf<SingleParamConfig>().apply {
                    for (orientation in TestOrientation) {
                        for (pageSpacing in TestPageSpacing) {
                            for (contentPadding in testContentPaddings(orientation)) {
                                for (snapPosition in TestSnapPosition) {
                                    add(
                                        SingleParamConfig(
                                            orientation = orientation,
                                            pageSpacing = pageSpacing,
                                            mainAxisContentPadding = contentPadding,
                                            snapPosition = snapPosition
                                        )
                                    )
                                }
                            }
                        }
                    }
                }

            forEachParameter(ParamsToTest) { param ->
                // make sure the scroll distance is enough to go to next page
                val delta = pageSize * 0.8f * param.scrollForwardSign

                // Act - forward
                runAndWaitForPageSettling {
                    onPager().performTouchInput {
                        with(param) {
                            swipeWithVelocityAcrossMainAxis(1.1f * MinFlingVelocityDp.toPx(), delta)
                        }
                    }
                }

                // Assert
                onNodeWithTag("6").assertIsDisplayed()
                assertThat(pagerState.currentPage).isEqualTo(6)
                assertThat(pagerState.currentPageOffsetFraction).isWithin(0.1f).of(0.0f)

                // Act - backward
                runAndWaitForPageSettling {
                    onPager().performTouchInput {
                        with(param) {
                            swipeWithVelocityAcrossMainAxis(
                                1.1f * MinFlingVelocityDp.toPx(),
                                delta * -1
                            )
                        }
                    }
                }

                // Assert
                onNodeWithTag("5").assertIsDisplayed()
                assertThat(pagerState.currentPage).isEqualTo(5)
                assertThat(pagerState.currentPageOffsetFraction).isWithin(0.1f).of(0.0f)
                resetTestCase(5)
            }
        }

    @Test
    fun scrollWithoutVelocity_shouldSettlingInClosestPage() =
        with(rule) {
            // Arrange
            setContent {
                ParameterizedPager(
                    initialPage = 5,
                    modifier = Modifier.fillMaxSize(),
                    orientation = it.orientation,
                    pageSpacing = it.pageSpacing
                )
            }
            forEachParameter(ParamsToTest) { param ->
                // This will scroll 1 whole page before flinging
                val delta = pagerSize * 1.4f * param.scrollForwardSign

                // Act - forward
                runAndWaitForPageSettling {
                    onPager().performTouchInput {
                        with(param) { swipeWithVelocityAcrossMainAxis(0f, delta) }
                    }
                }

                // Assert
                assertThat(pagerState.currentPage).isAtMost(7)
                onNodeWithTag("${pagerState.currentPage}").assertIsDisplayed()
                param.confirmPageIsInCorrectPosition(pagerState.currentPage)

                // Act - backward
                runAndWaitForPageSettling {
                    onPager().performTouchInput {
                        with(param) { swipeWithVelocityAcrossMainAxis(0f, delta * -1) }
                    }
                }

                // Assert
                assertThat(pagerState.currentPage).isAtLeast(5)
                onNodeWithTag("${pagerState.currentPage}").assertIsDisplayed()
                param.confirmPageIsInCorrectPosition(pagerState.currentPage)
                resetTestCase(5)
            }
        }

    @Test
    fun scrollWithSameVelocity_shouldYieldSameResult_forward() =
        with(rule) {
            // Arrange
            var initialPage = 1
            setContent {
                ParameterizedPager(
                    pageSize = PageSize.Fixed(200.dp),
                    initialPage = initialPage,
                    modifier = Modifier.fillMaxSize(),
                    pageCount = { 100 },
                    snappingPage = PagerSnapDistance.atMost(3),
                    orientation = it.orientation,
                    pageSpacing = it.pageSpacing
                )
            }

            forEachParameter(ParamsToTest) { param ->
                // This will scroll 0.5 page before flinging
                val delta = pagerSize * 0.5f * param.scrollForwardSign

                // Act - forward
                onPager().performTouchInput {
                    with(param) { swipeWithVelocityAcrossMainAxis(2000f, delta) }
                }
                waitForIdle()

                val pageDisplacement = pagerState.currentPage - initialPage

                // Repeat starting from different places
                // reset
                initialPage = 10
                runOnIdle { runBlocking { pagerState.scrollToPage(initialPage) } }

                onPager().performTouchInput {
                    with(param) { swipeWithVelocityAcrossMainAxis(2000f, delta) }
                }
                waitForIdle()

                assertThat(pagerState.currentPage - initialPage).isEqualTo(pageDisplacement)

                initialPage = 50
                runOnIdle { runBlocking { pagerState.scrollToPage(initialPage) } }

                onPager().performTouchInput {
                    with(param) { swipeWithVelocityAcrossMainAxis(2000f, delta) }
                }
                waitForIdle()

                assertThat(pagerState.currentPage - initialPage).isEqualTo(pageDisplacement)
                initialPage = 1
                resetTestCase(initialPage)
            }
        }

    @Test
    fun scrollWithSameVelocity_shouldYieldSameResult_backward() =
        with(rule) {
            // Arrange
            var initialPage = 90
            setContent {
                ParameterizedPager(
                    pageSize = PageSize.Fixed(200.dp),
                    initialPage = initialPage,
                    modifier = Modifier.fillMaxSize(),
                    pageCount = { 100 },
                    snappingPage = PagerSnapDistance.atMost(3),
                    orientation = it.orientation,
                    pageSpacing = it.pageSpacing
                )
            }

            forEachParameter(ParamsToTest) { param ->
                // This will scroll 0.5 page before flinging
                val delta = pagerSize * -0.5f * param.scrollForwardSign

                // Act - forward
                onPager().performTouchInput {
                    with(param) { swipeWithVelocityAcrossMainAxis(2000f, delta) }
                }
                waitForIdle()

                val pageDisplacement = pagerState.currentPage - initialPage

                // Repeat starting from different places
                // reset
                initialPage = 70
                runOnIdle { runBlocking { pagerState.scrollToPage(initialPage) } }

                onPager().performTouchInput {
                    with(param) { swipeWithVelocityAcrossMainAxis(2000f, delta) }
                }
                waitForIdle()

                assertThat(pagerState.currentPage - initialPage).isEqualTo(pageDisplacement)

                initialPage = 30
                runOnIdle { runBlocking { pagerState.scrollToPage(initialPage) } }

                onPager().performTouchInput {
                    with(param) { swipeWithVelocityAcrossMainAxis(2000f, delta) }
                }
                waitForIdle()

                assertThat(pagerState.currentPage - initialPage).isEqualTo(pageDisplacement)
                initialPage = 90
                resetTestCase(initialPage)
            }
        }

    @Test
    fun scrollForwardAndBackwards_shouldSettleInZeroPageFraction() =
        with(rule) {
            // Arrange
            setContent {
                ParameterizedPager(
                    initialPage = 5,
                    modifier = Modifier.fillMaxSize(),
                    orientation = it.orientation,
                    pageSpacing = it.pageSpacing,
                    contentPadding = PaddingValues(1.dp)
                )
            }

            forEachParameter(ParamsToTest) { param ->
                val swipeValue = 0.51f
                val delta = pagerSize * swipeValue * param.scrollForwardSign

                // Act - forward
                onPager().performTouchInput {
                    with(param) {
                        swipeWithVelocityAcrossMainAxis(0.5f * MinFlingVelocityDp.toPx(), delta)
                    }
                }
                waitForIdle()

                // Assert
                onNodeWithTag("6").assertIsDisplayed()
                assertThat(pagerState.currentPageOffsetFraction).isZero()

                // Act - backward
                onPager().performTouchInput {
                    with(param) {
                        swipeWithVelocityAcrossMainAxis(
                            0.5f * MinFlingVelocityDp.toPx(),
                            delta * -1
                        )
                    }
                }
                waitForIdle()

                // Assert
                onNodeWithTag("5").assertIsDisplayed()
                assertThat(pagerState.currentPageOffsetFraction).isZero()
                resetTestCase(5)
            }
        }

    companion object {
        val ParamsToTest =
            mutableListOf<SingleParamConfig>().apply {
                for (orientation in TestOrientation) {
                    for (pageSpacing in TestPageSpacing) {
                        add(SingleParamConfig(orientation = orientation, pageSpacing = pageSpacing))
                    }
                }
            }
    }
}
