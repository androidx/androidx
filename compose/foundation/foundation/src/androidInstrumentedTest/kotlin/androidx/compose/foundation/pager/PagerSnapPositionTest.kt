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
import androidx.compose.foundation.gestures.snapping.MinFlingVelocityDp
import androidx.compose.foundation.gestures.snapping.SnapPosition
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.test.performTouchInput
import androidx.compose.ui.unit.Density
import androidx.test.filters.LargeTest
import androidx.test.filters.SdkSuppress
import com.google.common.truth.Truth.assertThat
import kotlin.test.assertTrue
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.withContext
import org.junit.Test
import org.junit.runner.RunWith
import org.junit.runners.Parameterized

@OptIn(ExperimentalFoundationApi::class)
@LargeTest
@RunWith(Parameterized::class)
class PagerSnapPositionTest(val config: ParamConfig) : BasePagerTest(config) {

    @Test
    fun scrollToPage_shouldPlacePagesCorrectly() = runBlocking {
        // Arrange
        createPager(modifier = Modifier.fillMaxSize())

        // Act and Assert
        repeat(DefaultAnimationRepetition) {
            assertThat(pagerState.currentPage).isEqualTo(it)
            val nextPage = pagerState.currentPage + 1
            withContext(Dispatchers.Main + AutoTestFrameClock()) {
                pagerState.scrollToPage(nextPage)
            }
            rule.mainClock.advanceTimeUntil { pagerState.currentPage == nextPage }
            confirmPageIsInCorrectPosition(pagerState.currentPage)
        }
    }

    @SdkSuppress(maxSdkVersion = 32) // b/269176638
    @Test
    fun scrollToPage_usedOffset_shouldPlacePagesCorrectly() = runBlocking {
        // Arrange

        suspend fun scrollToPageWithOffset(page: Int, offset: Float) {
            withContext(Dispatchers.Main + AutoTestFrameClock()) {
                pagerState.scrollToPage(page, offset)
            }
        }

        // Arrange
        createPager(modifier = Modifier.fillMaxSize())

        // Act
        scrollToPageWithOffset(10, 0.5f)

        // Assert
        confirmPageIsInCorrectPosition(pagerState.currentPage, 10, pageOffset = 0.5f)

        // Act
        scrollToPageWithOffset(4, 0.2f)

        // Assert
        confirmPageIsInCorrectPosition(pagerState.currentPage, 4, pageOffset = 0.2f)

        // Act
        scrollToPageWithOffset(12, -0.4f)

        // Assert
        confirmPageIsInCorrectPosition(pagerState.currentPage, 12, pageOffset = -0.4f)

        // Act
        scrollToPageWithOffset(DefaultPageCount - 1, 0.5f)

        // Assert
        confirmPageIsInCorrectPosition(pagerState.currentPage, DefaultPageCount - 1)

        // Act
        scrollToPageWithOffset(0, -0.5f)

        // Assert
        confirmPageIsInCorrectPosition(pagerState.currentPage, 0)
    }

    @Test
    fun animateScrollToPage_shouldPlacePagesCorrectly() = runBlocking {
        // Arrange

        createPager(modifier = Modifier.fillMaxSize())

        // Act and Assert
        repeat(DefaultAnimationRepetition) {
            assertThat(pagerState.currentPage).isEqualTo(it)
            val nextPage = pagerState.currentPage + 1
            withContext(Dispatchers.Main + AutoTestFrameClock()) {
                pagerState.animateScrollToPage(nextPage)
            }
            rule.waitForIdle()
            assertTrue { pagerState.currentPage == nextPage }
            confirmPageIsInCorrectPosition(pagerState.currentPage)
        }
    }

    @Test
    fun animateScrollToPage_usedOffset_shouldPlacePagesCorrectly() = runBlocking {
        // Arrange

        suspend fun animateScrollToPageWithOffset(page: Int, offset: Float) {
            withContext(Dispatchers.Main + AutoTestFrameClock()) {
                pagerState.animateScrollToPage(page, offset)
            }
            rule.waitForIdle()
        }

        // Arrange
        createPager(modifier = Modifier.fillMaxSize())

        // Act
        animateScrollToPageWithOffset(10, 0.49f)

        // Assert
        confirmPageIsInCorrectPosition(pagerState.currentPage, 10, pageOffset = 0.49f)

        // Act
        animateScrollToPageWithOffset(4, 0.2f)

        // Assert
        confirmPageIsInCorrectPosition(pagerState.currentPage, 4, pageOffset = 0.2f)

        // Act
        animateScrollToPageWithOffset(12, -0.4f)

        // Assert
        confirmPageIsInCorrectPosition(pagerState.currentPage, 12, pageOffset = -0.4f)

        // Act
        animateScrollToPageWithOffset(DefaultPageCount - 1, 0.5f)

        // Assert
        confirmPageIsInCorrectPosition(pagerState.currentPage, DefaultPageCount - 1)

        // Act
        animateScrollToPageWithOffset(0, -0.5f)

        // Assert
        confirmPageIsInCorrectPosition(pagerState.currentPage, 0)
    }

    @Test
    fun animateScrollToPage_moveToSamePageWithOffset_shouldScroll() = runBlocking {
        // Arrange
        createPager(initialPage = 5, modifier = Modifier.fillMaxSize())

        // Act
        assertThat(pagerState.currentPage).isEqualTo(5)

        withContext(Dispatchers.Main + AutoTestFrameClock()) {
            pagerState.animateScrollToPage(5, 0.4f)
        }

        // Assert
        rule.runOnIdle { assertThat(pagerState.currentPage).isEqualTo(5) }
        rule.runOnIdle { assertThat(pagerState.currentPageOffsetFraction).isWithin(0.01f).of(0.4f) }
    }

    @Test
    fun currentPage_shouldChangeWhenClosestPageToSnappedPositionChanges() {
        // Arrange

        createPager(modifier = Modifier.fillMaxSize())
        var previousCurrentPage = pagerState.currentPage

        // Act
        // Move less than half an item
        val firstDelta = (pagerSize * 0.4f) * scrollForwardSign
        onPager().performTouchInput {
            down(layoutStart)
            if (vertical) {
                moveBy(Offset(0f, firstDelta))
            } else {
                moveBy(Offset(firstDelta, 0f))
            }
        }

        // Assert
        rule.runOnIdle {
            assertThat(pagerState.currentPage).isEqualTo(previousCurrentPage)
        }
        // Release pointer
        onPager().performTouchInput { up() }

        rule.runOnIdle {
            previousCurrentPage = pagerState.currentPage
        }
        confirmPageIsInCorrectPosition(pagerState.currentPage)

        // Arrange
        // Pass closest to snap position threshold (over half an item)
        val secondDelta = (pagerSize * 0.6f) * scrollForwardSign

        // Act
        onPager().performTouchInput {
            down(layoutStart)
            if (vertical) {
                moveBy(Offset(0f, secondDelta))
            } else {
                moveBy(Offset(secondDelta, 0f))
            }
        }

        // Assert
        rule.runOnIdle {
            assertThat(pagerState.currentPage).isEqualTo(previousCurrentPage + 1)
        }

        onPager().performTouchInput { up() }
        rule.waitForIdle()
        confirmPageIsInCorrectPosition(pagerState.currentPage)
    }

    @Test
    fun targetPage_performScrollBelowMinThreshold_shouldNotShowNextPage() {
        // Arrange
        createPager(
            modifier = Modifier.fillMaxSize(),
            snappingPage = PagerSnapDistance.atMost(3)
        )
        rule.runOnIdle { assertThat(pagerState.targetPage).isEqualTo(pagerState.currentPage) }

        rule.mainClock.autoAdvance = false
        // Act
        // Moving less than threshold
        val forwardDelta =
            scrollForwardSign.toFloat() * with(rule.density) { DefaultPositionThreshold.toPx() / 2 }

        var previousTargetPage = pagerState.targetPage

        onPager().performTouchInput {
            down(layoutStart)
            moveBy(Offset(forwardDelta, forwardDelta))
        }

        // Assert
        assertThat(pagerState.targetPage).isEqualTo(previousTargetPage)

        // Reset
        rule.mainClock.autoAdvance = true
        onPager().performTouchInput { up() }
        rule.runOnIdle { assertThat(pagerState.targetPage).isEqualTo(pagerState.currentPage) }

        // Act
        // Moving more than threshold
        val backwardDelta = scrollForwardSign.toFloat() * with(rule.density) {
            -DefaultPositionThreshold.toPx() / 2
        }

        previousTargetPage = pagerState.targetPage

        onPager().performTouchInput {
            down(layoutStart)
            moveBy(Offset(backwardDelta, backwardDelta))
        }

        // Assert
        assertThat(pagerState.targetPage).isEqualTo(previousTargetPage)
    }

    @Test
    fun targetPage_performScroll_shouldShowNextPage() {
        // Arrange
        createPager(
            modifier = Modifier.fillMaxSize(),
            snappingPage = PagerSnapDistance.atMost(3)
        )
        rule.runOnIdle { assertThat(pagerState.targetPage).isEqualTo(pagerState.currentPage) }

        rule.mainClock.autoAdvance = false
        // Act
        // Moving forward
        val forwardDelta = pagerSize * 0.4f * scrollForwardSign.toFloat()
        onPager().performTouchInput {
            down(layoutStart)
            moveBy(Offset(forwardDelta, forwardDelta))
        }

        // Assert
        rule.runOnIdle {
            assertThat(pagerState.targetPage).isEqualTo(pagerState.currentPage + 1)
            assertThat(pagerState.targetPage).isNotEqualTo(pagerState.currentPage)
        }

        // Reset
        rule.mainClock.autoAdvance = true
        onPager().performTouchInput { up() }
        rule.runOnIdle { assertThat(pagerState.targetPage).isEqualTo(pagerState.currentPage) }
        rule.runOnIdle {
            scope.launch {
                pagerState.scrollToPage(5)
            }
        }

        rule.mainClock.autoAdvance = false
        // Act
        // Moving backward
        val backwardDelta = -pagerSize * 0.4f * scrollForwardSign.toFloat()
        onPager().performTouchInput {
            down(layoutEnd)
            moveBy(Offset(backwardDelta, backwardDelta))
        }

        // Assert
        rule.runOnIdle {
            assertThat(pagerState.targetPage).isEqualTo(pagerState.currentPage - 1)
            assertThat(pagerState.targetPage).isNotEqualTo(pagerState.currentPage)
        }

        rule.mainClock.autoAdvance = true
        onPager().performTouchInput { up() }
        rule.runOnIdle { assertThat(pagerState.targetPage).isEqualTo(pagerState.currentPage) }
    }

    @Test
    fun targetPage_performingFling_shouldGoToPredictedPage() {
        // Arrange

        createPager(
            modifier = Modifier.fillMaxSize(),
            snappingPage = PagerSnapDistance.atMost(3)
        )
        rule.runOnIdle { assertThat(pagerState.targetPage).isEqualTo(pagerState.currentPage) }

        rule.mainClock.autoAdvance = false
        // Act
        // Moving forward
        var previousTarget = pagerState.targetPage
        val forwardDelta = pagerSize * scrollForwardSign.toFloat()
        onPager().performTouchInput {
            swipeWithVelocityAcrossMainAxis(20000f, forwardDelta)
        }
        rule.mainClock.advanceTimeUntil { pagerState.targetPage != previousTarget }
        var flingOriginIndex = pagerState.firstVisiblePage
        // Assert
        assertThat(pagerState.targetPage).isEqualTo(flingOriginIndex + 3)
        assertThat(pagerState.targetPage).isNotEqualTo(pagerState.currentPage)

        rule.mainClock.autoAdvance = true
        rule.runOnIdle { assertThat(pagerState.targetPage).isEqualTo(pagerState.currentPage) }
        rule.mainClock.autoAdvance = false
        // Act
        // Moving backward
        previousTarget = pagerState.targetPage
        val backwardDelta = -pagerSize * scrollForwardSign.toFloat()
        onPager().performTouchInput {
            swipeWithVelocityAcrossMainAxis(20000f, backwardDelta)
        }
        rule.mainClock.advanceTimeUntil { pagerState.targetPage != previousTarget }

        // Assert
        flingOriginIndex = pagerState.firstVisiblePage + 1
        assertThat(pagerState.targetPage).isEqualTo(flingOriginIndex - 3)
        assertThat(pagerState.targetPage).isNotEqualTo(pagerState.currentPage)

        rule.mainClock.autoAdvance = true
        rule.runOnIdle { assertThat(pagerState.targetPage).isEqualTo(pagerState.currentPage) }
    }

    @Test
    fun targetPage_valueAfterScrollingAfterMidpoint() {
        createPager(initialPage = 5, modifier = Modifier.fillMaxSize())

        var previousCurrentPage = pagerState.currentPage

        val forwardDelta = (pagerSize * 0.7f) * scrollForwardSign
        onPager().performTouchInput {
            down(layoutStart)
            if (vertical) {
                moveBy(Offset(0f, forwardDelta))
            } else {
                moveBy(Offset(forwardDelta, 0f))
            }
        }

        rule.runOnIdle {
            assertThat(pagerState.currentPage).isNotEqualTo(previousCurrentPage)
            assertThat(pagerState.targetPage).isEqualTo(previousCurrentPage + 1)
        }

        onPager().performTouchInput { up() }

        rule.runOnIdle {
            previousCurrentPage = pagerState.currentPage
        }

        val backwardDelta = (pagerSize * 0.7f) * scrollForwardSign * -1
        onPager().performTouchInput {
            down(layoutEnd)
            if (vertical) {
                moveBy(Offset(0f, backwardDelta))
            } else {
                moveBy(Offset(backwardDelta, 0f))
            }
        }

        rule.runOnIdle {
            assertThat(pagerState.currentPage).isNotEqualTo(previousCurrentPage)
            assertThat(pagerState.targetPage).isEqualTo(previousCurrentPage - 1)
        }

        onPager().performTouchInput { up() }
    }

    @Test
    fun targetPage_valueAfterScrollingForwardAndBackward() {
        createPager(initialPage = 5, modifier = Modifier.fillMaxSize())

        val startCurrentPage = pagerState.currentPage

        val forwardDelta = (pagerSize * 0.8f) * scrollForwardSign
        onPager().performTouchInput {
            down(layoutStart)
            if (vertical) {
                moveBy(Offset(0f, forwardDelta))
            } else {
                moveBy(Offset(forwardDelta, 0f))
            }
        }

        rule.runOnIdle {
            assertThat(pagerState.currentPage).isNotEqualTo(startCurrentPage)
            assertThat(pagerState.targetPage).isEqualTo(startCurrentPage + 1)
        }

        val backwardDelta = (pagerSize * 0.2f) * scrollForwardSign * -1
        onPager().performTouchInput {
            if (vertical) {
                moveBy(Offset(0f, backwardDelta))
            } else {
                moveBy(Offset(backwardDelta, 0f))
            }
        }

        rule.runOnIdle {
            assertThat(pagerState.currentPage).isNotEqualTo(startCurrentPage)
            assertThat(pagerState.targetPage).isEqualTo(startCurrentPage)
        }

        onPager().performTouchInput { up() }
    }

    @Test
    fun currentPageOffset_shouldReflectScrollingOfCurrentPage() {
        // Arrange
        createPager(initialPage = DefaultPageCount / 2, modifier = Modifier.fillMaxSize())

        // No offset initially
        rule.runOnIdle {
            assertThat(pagerState.currentPageOffsetFraction).isWithin(0.01f).of(0f)
        }

        // Act
        // Moving forward
        onPager().performTouchInput {
            down(layoutStart)
            if (vertical) {
                moveBy(Offset(0f, scrollForwardSign * pagerSize / 4f))
            } else {
                moveBy(Offset(scrollForwardSign * pagerSize / 4f, 0f))
            }
        }

        rule.runOnIdle {
            assertThat(pagerState.currentPageOffsetFraction).isWithin(0.1f).of(0.25f)
        }

        onPager().performTouchInput { up() }
        rule.waitForIdle()

        // Reset
        rule.runOnIdle {
            scope.launch {
                pagerState.scrollToPage(DefaultPageCount / 2)
            }
        }

        // No offset initially (again)
        rule.runOnIdle {
            assertThat(pagerState.currentPageOffsetFraction).isWithin(0.01f).of(0f)
        }

        // Act
        // Moving backward
        onPager().performTouchInput {
            down(layoutStart)
            if (vertical) {
                moveBy(Offset(0f, -scrollForwardSign * pagerSize / 4f))
            } else {
                moveBy(Offset(-scrollForwardSign * pagerSize / 4f, 0f))
            }
        }

        rule.runOnIdle {
            assertThat(pagerState.currentPageOffsetFraction).isWithin(0.1f).of(-0.25f)
        }
    }

    @Test
    fun snapPosition_shouldNotInfluenceMaxScroll() {
        val PageSize = object : PageSize {
            override fun Density.calculateMainAxisPageSize(
                availableSpace: Int,
                pageSpacing: Int
            ): Int {
                return (availableSpace + pageSpacing) / 2
            }
        }
        createPager(modifier = Modifier.fillMaxSize(), pageSize = { PageSize })

        // Reset
        rule.runOnIdle {
            scope.launch {
                pagerState.scrollToPage(DefaultPageCount - 2)
            }
        }
        rule.waitForIdle()
        val velocity = with(rule.density) { 2 * MinFlingVelocityDp.roundToPx() }.toFloat()
        val forwardDelta = (pageSize) * scrollForwardSign
        onPager().performTouchInput {
            swipeWithVelocityAcrossMainAxis(velocity, forwardDelta.toFloat())
        }

        rule.runOnIdle {
            assertThat(pagerState.canScrollForward).isFalse()
        }
    }

    companion object {
        @JvmStatic
        @Parameterized.Parameters(name = "{0}")
        fun params() = mutableListOf<ParamConfig>().apply {
            for (orientation in TestOrientation) {
                for (snapPosition in TestSnapPosition) {
                    // skip start since it's being tested in PagerStateTest already.
                    if (snapPosition.first == SnapPosition.Start) continue
                    add(
                        ParamConfig(
                            orientation = orientation,
                            snapPosition = snapPosition
                        )
                    )
                }
            }
        }
    }
}
