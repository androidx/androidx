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

import androidx.compose.foundation.AutoTestFrameClock
import androidx.compose.foundation.gestures.scrollBy
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.size
import androidx.compose.ui.Modifier
import androidx.compose.ui.test.performTouchInput
import androidx.test.filters.LargeTest
import com.google.common.truth.Truth
import kotlin.math.roundToInt
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.withContext
import org.junit.Test

@LargeTest
class PagerOffscreenPageLimitPlacingTest : SingleParamBasePagerTest() {

    private suspend fun resetTestCase(initialPage: Int = 0) {
        withContext(Dispatchers.Main + AutoTestFrameClock()) {
            pagerState.scrollToPage(initialPage)
        }
        placed.clear()
    }

    @Test
    fun offscreenPageLimitIsUsed_shouldPlaceMoreItemsThanVisibleOnesAsWeScroll() =
        with(rule) {
            // Arrange
            setContent {
                ParameterizedPager(
                    pageCount = { DefaultPageCount },
                    modifier = Modifier.fillMaxSize(),
                    beyondViewportPageCount = 1,
                    orientation = it.orientation,
                    pageSpacing = it.pageSpacing,
                    contentPadding = it.mainAxisContentPadding
                )
            }

            forEachParameter(ParamsToTest) { param ->
                runBlocking {
                    val delta = pagerSize * 1.4f * param.scrollForwardSign

                    repeat(DefaultAnimationRepetition) {
                        // Act
                        onPager().performTouchInput {
                            with(param) { swipeWithVelocityAcrossMainAxis(0f, delta) }
                        }

                        // Next page was placed
                        rule.runOnIdle {
                            Truth.assertThat(placed)
                                .contains(
                                    (pagerState.currentPage + 1).coerceAtMost(DefaultPageCount - 1)
                                )
                        }
                    }

                    param.confirmPageIsInCorrectPosition(pagerState.currentPage)
                    resetTestCase()
                }
            }
        }

    @Test
    fun offscreenPageLimitIsUsed_shouldPlaceMoreItemsThanVisibleOnes() =
        with(rule) {
            // Arrange
            val initialIndex = 5

            // Act
            setContent {
                ParameterizedPager(
                    initialPage = initialIndex,
                    pageCount = { DefaultPageCount },
                    modifier = Modifier.fillMaxSize(),
                    beyondViewportPageCount = 2,
                    orientation = it.orientation,
                    pageSpacing = it.pageSpacing,
                    contentPadding = it.mainAxisContentPadding
                )
            }

            forEachParameter(ParamsToTest) { param ->
                val firstVisible = pagerState.layoutInfo.visiblePagesInfo.first().index
                val lastVisible = pagerState.layoutInfo.visiblePagesInfo.last().index
                // Assert
                runOnIdle {
                    Truth.assertThat(placed).contains(firstVisible - 2)
                    Truth.assertThat(placed).contains(firstVisible - 1)
                    Truth.assertThat(placed).contains(lastVisible + 1)
                    Truth.assertThat(placed).contains(lastVisible + 2)
                }
                param.confirmPageIsInCorrectPosition(initialIndex, firstVisible - 2)
                param.confirmPageIsInCorrectPosition(initialIndex, firstVisible - 1)
                param.confirmPageIsInCorrectPosition(initialIndex, lastVisible + 1)
                param.confirmPageIsInCorrectPosition(initialIndex, lastVisible + 2)
                runBlocking { resetTestCase(5) }
            }
        }

    @Test
    fun offscreenPageLimitIsNotUsed_shouldNotPlaceMoreItemsThanVisibleOnes() =
        with(rule) {
            // Arrange
            // Act
            setContent {
                ParameterizedPager(
                    initialPage = 5,
                    pageCount = { DefaultPageCount },
                    modifier = Modifier.fillMaxSize(),
                    beyondViewportPageCount = 0,
                    orientation = it.orientation,
                    pageSpacing = it.pageSpacing,
                    contentPadding = it.mainAxisContentPadding
                )
            }

            forEachParameter(ParamsToTest) { param ->
                // Assert
                val firstVisible = pagerState.layoutInfo.visiblePagesInfo.first().index
                val lastVisible = pagerState.layoutInfo.visiblePagesInfo.last().index
                Truth.assertThat(placed).doesNotContain(firstVisible - 1)
                Truth.assertThat(placed).contains(5)
                Truth.assertThat(placed).doesNotContain(lastVisible + 1)
                param.confirmPageIsInCorrectPosition(5)
                runBlocking { resetTestCase(5) }
            }
        }

    @Test
    fun offsetPageLimitIsUsed_visiblePagesDidNotChange_shouldNotRemeasure() =
        with(rule) {
            val pageSizePx = 100
            val pageSizeDp = with(rule.density) { pageSizePx.toDp() }

            val delta = (pageSizePx / 3f).roundToInt()
            val initialIndex = 0
            setContent {
                ParameterizedPager(
                    initialPage = initialIndex,
                    pageCount = { DefaultPageCount },
                    modifier = Modifier.size(pageSizeDp * 1.5f),
                    pageSize = PageSize.Fixed(pageSizeDp),
                    beyondViewportPageCount = it.beyondViewportPageCount,
                    orientation = it.orientation,
                    pageSpacing = it.pageSpacing
                )
            }

            val Params =
                mutableListOf<SingleParamConfig>().apply {
                    for (orientation in TestOrientation) {
                        for (pageSpacing in TestPageSpacing) {
                            add(
                                SingleParamConfig(
                                    orientation = orientation,
                                    pageSpacing = pageSpacing,
                                    beyondViewportPageCount = 2
                                )
                            )
                        }
                    }
                }

            forEachParameter(Params) { param ->
                val lastVisible = pagerState.layoutInfo.visiblePagesInfo.last().index
                // Assert
                rule.runOnIdle {
                    Truth.assertThat(placed).contains(lastVisible + 1)
                    Truth.assertThat(placed).contains(lastVisible + 2)
                }
                val previousNumberOfRemeasurementPasses = pagerState.layoutWithMeasurement
                runBlocking {
                    withContext(Dispatchers.Main + AutoTestFrameClock()) {
                        // small enough scroll to not cause any new items to be composed or
                        // old ones disposed.
                        pagerState.scrollBy(delta.toFloat())
                    }
                    rule.runOnIdle {
                        Truth.assertThat(pagerState.firstVisiblePageOffset).isEqualTo(delta)
                        Truth.assertThat(pagerState.layoutWithMeasurement)
                            .isEqualTo(previousNumberOfRemeasurementPasses)
                    }

                    // verify that out of bounds pages are correctly placed
                    param.confirmPageIsInCorrectPosition(
                        pagerState.currentPage,
                        lastVisible + 1,
                        pagerState.currentPageOffsetFraction
                    )
                    param.confirmPageIsInCorrectPosition(
                        pagerState.currentPage,
                        lastVisible + 2,
                        pagerState.currentPageOffsetFraction
                    )
                    resetTestCase(initialIndex)
                }
            }
        }

    companion object {
        val ParamsToTest =
            mutableListOf<SingleParamConfig>().apply {
                for (orientation in TestOrientation) {
                    for (pageSpacing in TestPageSpacing) {
                        for (contentPadding in testContentPaddings(orientation)) {
                            add(
                                SingleParamConfig(
                                    orientation = orientation,
                                    pageSpacing = pageSpacing,
                                    mainAxisContentPadding = contentPadding
                                )
                            )
                        }
                    }
                }
            }
    }
}
