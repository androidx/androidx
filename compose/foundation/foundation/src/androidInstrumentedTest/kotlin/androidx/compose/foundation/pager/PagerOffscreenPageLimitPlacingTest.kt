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
import androidx.compose.foundation.ExperimentalFoundationApi
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
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.junit.runners.Parameterized

@OptIn(ExperimentalFoundationApi::class)
@LargeTest
@RunWith(Parameterized::class)
class PagerOffscreenPageLimitPlacingTest(
    val config: ParamConfig
) : BasePagerTest(config) {

    @Before
    fun setUp() {
        rule.mainClock.autoAdvance = false
    }

    @Test
    fun offscreenPageLimitIsUsed_shouldPlaceMoreItemsThanVisibleOnesAsWeScroll() {
        // Arrange
        createPager(
            pageCount = { DefaultPageCount },
            modifier = Modifier.fillMaxSize(),
            outOfBoundsPageCount = 1
        )
        val delta = pagerSize * 1.4f * scrollForwardSign

        repeat(DefaultAnimationRepetition) {
            // Act
            runAndWaitForPageSettling {
                onPager().performTouchInput {
                    swipeWithVelocityAcrossMainAxis(0f, delta)
                }
            }

            // Next page was placed
            rule.runOnIdle {
                Truth.assertThat(placed).contains(
                    (pagerState.currentPage + 1)
                        .coerceAtMost(DefaultPageCount - 1)
                )
            }
        }

        confirmPageIsInCorrectPosition(pagerState.currentPage)
    }

    @Test
    fun offscreenPageLimitIsUsed_shouldPlaceMoreItemsThanVisibleOnes() {
        // Arrange
        val initialIndex = 5

        // Act
        createPager(
            initialPage = initialIndex,
            pageCount = { DefaultPageCount },
            modifier = Modifier.fillMaxSize(),
            outOfBoundsPageCount = 2
        )
        val firstVisible = pagerState.layoutInfo.visiblePagesInfo.first().index
        val lastVisible = pagerState.layoutInfo.visiblePagesInfo.last().index
        // Assert
        rule.runOnIdle {
            Truth.assertThat(placed).contains(firstVisible - 2)
            Truth.assertThat(placed).contains(firstVisible - 1)
            Truth.assertThat(placed).contains(lastVisible + 1)
            Truth.assertThat(placed).contains(lastVisible + 2)
        }
        confirmPageIsInCorrectPosition(initialIndex, firstVisible - 2)
        confirmPageIsInCorrectPosition(initialIndex, firstVisible - 1)
        confirmPageIsInCorrectPosition(initialIndex, lastVisible + 1)
        confirmPageIsInCorrectPosition(initialIndex, lastVisible + 2)
    }

    @Test
    fun offscreenPageLimitIsNotUsed_shouldNotPlaceMoreItemsThanVisibleOnes() {
        // Arrange

        // Act
        createPager(
            initialPage = 5,
            pageCount = { DefaultPageCount },
            modifier = Modifier.fillMaxSize(),
            outOfBoundsPageCount = 0
        )

        // Assert
        val firstVisible = pagerState.layoutInfo.visiblePagesInfo.first().index
        val lastVisible = pagerState.layoutInfo.visiblePagesInfo.last().index
        Truth.assertThat(placed).doesNotContain(firstVisible - 1)
        Truth.assertThat(placed).contains(5)
        Truth.assertThat(placed).doesNotContain(lastVisible + 1)
        confirmPageIsInCorrectPosition(5)
    }

    @Test
    fun offsetPageLimitIsUsed_visiblePagesDidNotChange_shouldNotRemeasure() {
        val pageSizePx = 100
        val pageSizeDp = with(rule.density) { pageSizePx.toDp() }

        val delta = (pageSizePx / 3f).roundToInt()
        val initialIndex = 0
        createPager(
            initialPage = initialIndex,
            pageCount = { DefaultPageCount },
            modifier = Modifier.size(pageSizeDp * 1.5f),
            pageSize = { PageSize.Fixed(pageSizeDp) },
            outOfBoundsPageCount = 2
        )

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
            confirmPageIsInCorrectPosition(
                pagerState.currentPage,
                lastVisible + 1,
                pagerState.currentPageOffsetFraction
            )
            confirmPageIsInCorrectPosition(
                pagerState.currentPage,
                lastVisible + 2,
                pagerState.currentPageOffsetFraction
            )
        }
    }

    companion object {
        @JvmStatic
        @Parameterized.Parameters(name = "{0}")
        fun params() = mutableListOf<ParamConfig>().apply {
            for (orientation in TestOrientation) {
                for (pageSpacing in TestPageSpacing) {
                    for (contentPadding in testContentPaddings(orientation)) {
                        add(
                            ParamConfig(
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
