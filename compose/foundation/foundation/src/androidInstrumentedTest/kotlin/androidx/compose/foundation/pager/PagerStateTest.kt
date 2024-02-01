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

import androidx.compose.animation.core.AnimationSpec
import androidx.compose.animation.core.AnimationState
import androidx.compose.animation.core.animate
import androidx.compose.animation.core.animateDecay
import androidx.compose.animation.core.calculateTargetValue
import androidx.compose.animation.core.tween
import androidx.compose.animation.splineBasedDecay
import androidx.compose.foundation.AutoTestFrameClock
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.gestures.ScrollScope
import androidx.compose.foundation.gestures.TargetedFlingBehavior
import androidx.compose.foundation.gestures.scrollBy
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.layout.layout
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.performTouchInput
import androidx.test.filters.LargeTest
import androidx.test.filters.SdkSuppress
import com.google.common.truth.Truth.assertThat
import kotlin.math.abs
import kotlin.math.roundToInt
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
class PagerStateTest(val config: ParamConfig) : BasePagerTest(config) {

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
    fun scrollToPage_longSkipShouldNotPlaceIntermediatePages() = runBlocking {
        // Arrange

        createPager(modifier = Modifier.fillMaxSize())

        // Act
        assertThat(pagerState.currentPage).isEqualTo(0)
        withContext(Dispatchers.Main + AutoTestFrameClock()) {
            pagerState.scrollToPage(DefaultPageCount - 1)
        }

        // Assert
        rule.runOnIdle {
            assertThat(pagerState.currentPage).isEqualTo(DefaultPageCount - 1)
            assertThat(placed).doesNotContain(DefaultPageCount / 2 - 1)
            assertThat(placed).doesNotContain(DefaultPageCount / 2)
            assertThat(placed).doesNotContain(DefaultPageCount / 2 + 1)
        }
        confirmPageIsInCorrectPosition(pagerState.currentPage)
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
    fun animateScrollToPage_longSkipShouldNotPlaceIntermediatePages() = runBlocking {
        // Arrange

        createPager(modifier = Modifier.fillMaxSize())

        // Act
        assertThat(pagerState.currentPage).isEqualTo(0)
        withContext(Dispatchers.Main + AutoTestFrameClock()) {
            pagerState.animateScrollToPage(DefaultPageCount - 1)
        }

        // Assert
        rule.runOnIdle {
            assertThat(pagerState.currentPage).isEqualTo(DefaultPageCount - 1)
            assertThat(placed).doesNotContain(DefaultPageCount / 2 - 1)
            assertThat(placed).doesNotContain(DefaultPageCount / 2)
            assertThat(placed).doesNotContain(DefaultPageCount / 2 + 1)
        }
        confirmPageIsInCorrectPosition(pagerState.currentPage)
    }

    @Test
    fun scrollToPage_shouldCoerceWithinRange() = runBlocking {
        // Arrange

        createPager(modifier = Modifier.fillMaxSize())

        // Act
        assertThat(pagerState.currentPage).isEqualTo(0)
        withContext(Dispatchers.Main + AutoTestFrameClock()) {
            pagerState.scrollToPage(DefaultPageCount)
        }

        // Assert
        rule.runOnIdle { assertThat(pagerState.currentPage).isEqualTo(DefaultPageCount - 1) }

        // Act
        withContext(Dispatchers.Main + AutoTestFrameClock()) {
            pagerState.scrollToPage(-1)
        }

        // Assert
        rule.runOnIdle { assertThat(pagerState.currentPage).isEqualTo(0) }
    }

    @Test
    fun animateScrollToPage_shouldCoerceWithinRange() = runBlocking {
        // Arrange

        createPager(modifier = Modifier.fillMaxSize())

        // Act
        assertThat(pagerState.currentPage).isEqualTo(0)
        withContext(Dispatchers.Main + AutoTestFrameClock()) {
            pagerState.animateScrollToPage(DefaultPageCount)
        }

        // Assert
        rule.runOnIdle { assertThat(pagerState.currentPage).isEqualTo(DefaultPageCount - 1) }

        // Act
        withContext(Dispatchers.Main + AutoTestFrameClock()) {
            pagerState.animateScrollToPage(-1)
        }

        // Assert
        rule.runOnIdle { assertThat(pagerState.currentPage).isEqualTo(0) }
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
    fun animateScrollToPage_withPassedAnimation() = runBlocking {
        // Arrange
        rule.mainClock.autoAdvance = false
        createPager(modifier = Modifier.fillMaxSize())
        val differentAnimation: AnimationSpec<Float> = tween()

        // Act and Assert
        repeat(DefaultAnimationRepetition) {
            assertThat(pagerState.currentPage).isEqualTo(it)
            val nextPage = pagerState.currentPage + 1
            withContext(Dispatchers.Main + AutoTestFrameClock()) {
                pagerState.animateScrollToPage(
                    nextPage,
                    animationSpec = differentAnimation
                )
            }
            rule.waitForIdle()
            assertTrue { pagerState.currentPage == nextPage }
            confirmPageIsInCorrectPosition(pagerState.currentPage)
        }
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
    fun targetPage_performingFlingWithSnapFlingBehavior_shouldGoToPredictedPage() {
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
    fun targetPage_performingFlingWithCustomFling_shouldGoToPredictedPage() {
        // Arrange
        var flingPredictedPage = -1
        val myCustomFling = object : TargetedFlingBehavior {
            val decay = splineBasedDecay<Float>(rule.density)

            override suspend fun ScrollScope.performFling(
                initialVelocity: Float,
                onRemainingDistanceUpdated: (Float) -> Unit
            ): Float {
                val finalOffset = decay.calculateTargetValue(
                    0.0f,
                    initialVelocity
                )
                val pageDisplacement = finalOffset / pagerState.pageSizeWithSpacing
                val targetPage = pageDisplacement.roundToInt() + pagerState.currentPage
                flingPredictedPage = targetPage
                return if (abs(initialVelocity) > 1f) {
                    var velocityLeft = initialVelocity
                    var lastValue = 0f
                    val animationState = AnimationState(
                        initialValue = 0f,
                        initialVelocity = initialVelocity,
                    )
                    animationState.animateDecay(decay) {
                        onRemainingDistanceUpdated(finalOffset - value)
                        val delta = value - lastValue
                        val consumed = scrollBy(delta)
                        lastValue = value
                        velocityLeft = this.velocity
                        // avoid rounding errors and stop if anything is unconsumed
                        if (abs(delta - consumed) > 0.5f) this.cancelAnimation()
                    }
                    velocityLeft
                } else {
                    initialVelocity
                }
            }
        }

        createPager(
            pageCount = { 100 },
            modifier = Modifier.fillMaxSize(),
            flingBehavior = myCustomFling
        )
        rule.runOnIdle { assertThat(pagerState.targetPage).isEqualTo(pagerState.currentPage) }

        // Act
        // Moving forward
        rule.mainClock.autoAdvance = false
        val forwardDelta = pagerSize * scrollForwardSign.toFloat()
        onPager().performTouchInput {
            swipeWithVelocityAcrossMainAxis(20000f, forwardDelta)
        }
        rule.mainClock.advanceTimeUntil { flingPredictedPage != -1 }
        var targetPage = pagerState.targetPage

        // wait for targetPage to change
        rule.mainClock.advanceTimeUntil { pagerState.targetPage != targetPage }

        // Assert
        // Check if target page changed
        rule.runOnIdle { assertThat(pagerState.targetPage).isEqualTo(flingPredictedPage) }
        rule.mainClock.autoAdvance = true // let time run
        // Check if we actually stopped in the predicted page
        rule.runOnIdle { assertThat(pagerState.currentPage).isEqualTo(flingPredictedPage) }

        // Act
        // Moving backward
        flingPredictedPage = -1
        rule.mainClock.autoAdvance = false
        val backwardDelta = -pagerSize * scrollForwardSign.toFloat()
        onPager().performTouchInput {
            swipeWithVelocityAcrossMainAxis(20000f, backwardDelta)
        }
        rule.mainClock.advanceTimeUntil { flingPredictedPage != -1 }
        targetPage = pagerState.targetPage

        // wait for targetPage to change
        rule.mainClock.advanceTimeUntil { pagerState.targetPage != targetPage }

        // Assert
        // Check if target page changed
        rule.runOnIdle { assertThat(pagerState.targetPage).isEqualTo(flingPredictedPage) }
        rule.mainClock.autoAdvance = true // let time run
        // Check if we actually stopped in the predicted page
        rule.runOnIdle { assertThat(pagerState.currentPage).isEqualTo(flingPredictedPage) }
    }

    @Test
    fun targetPage_shouldReflectTargetWithAnimation() {
        // Arrange

        createPager(
            modifier = Modifier.fillMaxSize()
        )
        rule.runOnIdle { assertThat(pagerState.targetPage).isEqualTo(pagerState.currentPage) }

        rule.mainClock.autoAdvance = false
        // Act
        // Moving forward
        var previousTarget = pagerState.targetPage
        rule.runOnIdle {
            scope.launch {
                pagerState.animateScrollToPage(DefaultPageCount - 1)
            }
        }
        rule.mainClock.advanceTimeUntil { pagerState.targetPage != previousTarget }

        // Assert
        assertThat(pagerState.targetPage).isEqualTo(DefaultPageCount - 1)
        assertThat(pagerState.targetPage).isNotEqualTo(pagerState.currentPage)

        rule.mainClock.autoAdvance = true
        rule.runOnIdle { assertThat(pagerState.targetPage).isEqualTo(pagerState.currentPage) }
        rule.mainClock.autoAdvance = false

        // Act
        // Moving backward
        previousTarget = pagerState.targetPage
        rule.runOnIdle {
            scope.launch {
                pagerState.animateScrollToPage(0)
            }
        }
        rule.mainClock.advanceTimeUntil { pagerState.targetPage != previousTarget }

        // Assert
        assertThat(pagerState.targetPage).isEqualTo(0)
        assertThat(pagerState.targetPage).isNotEqualTo(pagerState.currentPage)

        rule.mainClock.autoAdvance = true
        rule.runOnIdle { assertThat(pagerState.targetPage).isEqualTo(pagerState.currentPage) }
    }

    @Test
    fun targetPage_shouldReflectTargetWithCustomAnimation() {
        // Arrange
        suspend fun PagerState.customAnimateScrollToPage(page: Int) {
            scroll {
                updateTargetPage(page)
                val targetPageDiff = page - currentPage
                val distance = targetPageDiff * layoutInfo.pageSize.toFloat()
                var previousValue = 0.0f
                animate(
                    0f,
                    distance,
                ) { currentValue, _ ->
                    previousValue += scrollBy(currentValue - previousValue)
                }
            }
        }

        createPager(
            modifier = Modifier.fillMaxSize()
        )
        rule.runOnIdle { assertThat(pagerState.targetPage).isEqualTo(pagerState.currentPage) }

        rule.mainClock.autoAdvance = false
        // Act
        // Moving forward
        var previousTarget = pagerState.targetPage
        rule.runOnIdle {
            scope.launch {
                pagerState.customAnimateScrollToPage(DefaultPageCount - 1)
            }
        }
        rule.mainClock.advanceTimeUntil { pagerState.targetPage != previousTarget }

        // Assert
        assertThat(pagerState.targetPage).isEqualTo(DefaultPageCount - 1)
        assertThat(pagerState.targetPage).isNotEqualTo(pagerState.currentPage)

        rule.mainClock.autoAdvance = true
        rule.runOnIdle { assertThat(pagerState.targetPage).isEqualTo(pagerState.currentPage) }
        rule.mainClock.autoAdvance = false

        // Act
        // Moving backward
        previousTarget = pagerState.targetPage
        rule.runOnIdle {
            scope.launch {
                pagerState.customAnimateScrollToPage(0)
            }
        }
        rule.mainClock.advanceTimeUntil { pagerState.targetPage != previousTarget }

        // Assert
        assertThat(pagerState.targetPage).isEqualTo(0)
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
    fun settledPage_onAnimationScroll_shouldChangeOnScrollFinishedOnly() {
        // Arrange
        var settledPageChanges = 0
        createPager(
            modifier = Modifier.fillMaxSize(),
            additionalContent = {
                LaunchedEffect(key1 = pagerState.settledPage) {
                    settledPageChanges++
                }
            }
        )

        // Settle page changed once for first composition
        rule.runOnIdle {
            assertThat(pagerState.settledPage).isEqualTo(pagerState.currentPage)
            assertTrue { settledPageChanges == 1 }
        }

        settledPageChanges = 0
        val previousSettled = pagerState.settledPage
        rule.mainClock.autoAdvance = false
        // Act
        // Moving forward
        rule.runOnIdle {
            scope.launch {
                pagerState.animateScrollToPage(DefaultPageCount - 1)
            }
        }

        // Settled page shouldn't change whilst scroll is in progress.
        assertTrue { pagerState.isScrollInProgress }
        assertTrue { settledPageChanges == 0 }
        assertThat(pagerState.settledPage).isEqualTo(previousSettled)

        rule.mainClock.advanceTimeUntil { settledPageChanges != 0 }

        rule.runOnIdle {
            assertTrue { !pagerState.isScrollInProgress }
            assertThat(pagerState.settledPage).isEqualTo(pagerState.currentPage)
        }
    }

    @Test
    fun settledPage_onGestureScroll_shouldChangeOnScrollFinishedOnly() {
        // Arrange
        var settledPageChanges = 0
        createPager(
            modifier = Modifier.fillMaxSize(),
            additionalContent = {
                LaunchedEffect(key1 = pagerState.settledPage) {
                    settledPageChanges++
                }
            }
        )

        settledPageChanges = 0
        val previousSettled = pagerState.settledPage
        rule.mainClock.autoAdvance = false
        // Act
        // Moving forward
        val forwardDelta = pagerSize / 2f * scrollForwardSign.toFloat()
        rule.onNodeWithTag(PagerTestTag).performTouchInput {
            swipeWithVelocityAcrossMainAxis(10000f, forwardDelta)
        }

        // Settled page shouldn't change whilst scroll is in progress.
        assertTrue { pagerState.isScrollInProgress }
        assertTrue { settledPageChanges == 0 }
        assertThat(pagerState.settledPage).isEqualTo(previousSettled)

        rule.mainClock.advanceTimeUntil { settledPageChanges != 0 }

        rule.runOnIdle {
            assertTrue { !pagerState.isScrollInProgress }
            assertThat(pagerState.settledPage).isEqualTo(pagerState.currentPage)
        }
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
    fun onScroll_shouldNotGenerateExtraMeasurements() {
        // Arrange
        var layoutCount = 0
        createPager(initialPage = 5, modifier = Modifier.layout { measurable, constraints ->
            layoutCount++
            val placeables = measurable.measure(constraints)
            layout(constraints.maxWidth, constraints.maxHeight) {
                placeables.place(0, 0)
            }
        })

        // Act: Scroll.
        val previousMeasurementCount = layoutCount
        val previousOffsetFraction = pagerState.currentPageOffsetFraction
        rule.runOnIdle {
            runBlocking {
                pagerState.scrollBy((pageSize * 0.2f) * scrollForwardSign)
            }
        }
        rule.runOnIdle {
            assertThat(pagerState.currentPageOffsetFraction).isNotEqualTo(previousOffsetFraction)
            assertThat(layoutCount).isEqualTo(previousMeasurementCount + 1)
        }
    }

    companion object {
        @JvmStatic
        @Parameterized.Parameters(name = "{0}")
        fun params() = mutableListOf<ParamConfig>().apply {
            for (orientation in TestOrientation) {
                for (reverseLayout in TestReverseLayout) {
                    for (layoutDirection in TestLayoutDirection) {
                        add(
                            ParamConfig(
                                orientation = orientation,
                                reverseLayout = reverseLayout,
                                layoutDirection = layoutDirection
                            )
                        )
                    }
                }
            }
        }
    }
}
