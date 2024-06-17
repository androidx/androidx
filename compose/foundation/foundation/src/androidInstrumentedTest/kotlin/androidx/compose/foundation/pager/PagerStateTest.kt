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
import androidx.compose.foundation.gestures.Orientation
import androidx.compose.foundation.gestures.ScrollScope
import androidx.compose.foundation.gestures.TargetedFlingBehavior
import androidx.compose.foundation.gestures.scrollBy
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.layout.layout
import androidx.compose.ui.test.performTouchInput
import androidx.compose.ui.unit.dp
import androidx.test.filters.LargeTest
import com.google.common.truth.Truth.assertThat
import kotlin.math.abs
import kotlin.math.roundToInt
import kotlin.test.assertTrue
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.withContext
import org.junit.Test

@LargeTest
class PagerStateTest : SingleParamBasePagerTest() {

    @Test
    fun scrollToPage_shouldPlacePagesCorrectly() {
        // Arrange
        rule.setContent { config ->
            ParameterizedPager(
                modifier = Modifier.fillMaxSize(),
                orientation = config.orientation,
                layoutDirection = config.layoutDirection,
                reverseLayout = config.reverseLayout,
                snapPosition = config.snapPosition.first
            )
        }

        rule.forEachParameter(PagerStateTestParams) { param ->
            runBlocking {
                repeat(DefaultAnimationRepetition) {
                    assertThat(pagerState.currentPage).isEqualTo(it)
                    val nextPage = pagerState.currentPage + 1
                    withContext(Dispatchers.Main + AutoTestFrameClock()) {
                        pagerState.scrollToPage(nextPage)
                    }
                    rule.mainClock.advanceTimeUntil { pagerState.currentPage == nextPage }
                    param.confirmPageIsInCorrectPosition(pagerState.currentPage)
                }
                // reset
                resetTestCase()
            }
        }
    }

    @Test
    fun scrollToPage_usedOffset_shouldPlacePagesCorrectly() {
        // Arrange
        suspend fun scrollToPageWithOffset(page: Int, offset: Float) {
            withContext(Dispatchers.Main + AutoTestFrameClock()) {
                pagerState.scrollToPage(page, offset)
            }
        }

        rule.setContent { config ->
            ParameterizedPager(
                modifier = Modifier.fillMaxSize(),
                orientation = config.orientation,
                layoutDirection = config.layoutDirection,
                reverseLayout = config.reverseLayout,
                snapPosition = config.snapPosition.first
            )
        }

        rule.forEachParameter(PagerStateTestParams) { param ->
            // Act and Assert
            runBlocking {
                // Act
                scrollToPageWithOffset(10, 0.5f)

                // Assert
                param.confirmPageIsInCorrectPosition(pagerState.currentPage, 10, pageOffset = 0.5f)

                // Act
                scrollToPageWithOffset(4, 0.2f)

                // Assert
                param.confirmPageIsInCorrectPosition(pagerState.currentPage, 4, pageOffset = 0.2f)

                // Act
                scrollToPageWithOffset(12, -0.4f)

                // Assert
                param.confirmPageIsInCorrectPosition(pagerState.currentPage, 12, pageOffset = -0.4f)

                // Act
                scrollToPageWithOffset(DefaultPageCount - 1, 0.5f)

                // Assert
                param.confirmPageIsInCorrectPosition(pagerState.currentPage, DefaultPageCount - 1)

                // Act
                scrollToPageWithOffset(0, -0.5f)

                // Assert
                param.confirmPageIsInCorrectPosition(pagerState.currentPage, 0)

                // reset
                resetTestCase()
            }
        }
    }

    @Test
    fun scrollToPage_longSkipShouldNotPlaceIntermediatePages() {
        // Arrange
        rule.setContent { config ->
            ParameterizedPager(
                modifier = Modifier.fillMaxSize(),
                orientation = config.orientation,
                layoutDirection = config.layoutDirection,
                reverseLayout = config.reverseLayout,
                snapPosition = config.snapPosition.first
            )
        }

        rule.forEachParameter(PagerStateTestParams) { param ->
            runBlocking {
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
                param.confirmPageIsInCorrectPosition(pagerState.currentPage)

                // reset
                resetTestCase()
            }
        }
    }

    @Test
    fun animateScrollToPage_shouldPlacePagesCorrectly() {
        // Arrange
        rule.setContent { config ->
            ParameterizedPager(
                modifier = Modifier.fillMaxSize(),
                orientation = config.orientation,
                layoutDirection = config.layoutDirection,
                reverseLayout = config.reverseLayout,
                snapPosition = config.snapPosition.first
            )
        }

        rule.forEachParameter(PagerStateTestParams) { param ->
            runBlocking {
                // Act and Assert
                repeat(DefaultAnimationRepetition) {
                    assertThat(pagerState.currentPage).isEqualTo(it)
                    val nextPage = pagerState.currentPage + 1
                    withContext(Dispatchers.Main + AutoTestFrameClock()) {
                        pagerState.animateScrollToPage(nextPage)
                    }
                    rule.waitForIdle()
                    assertTrue { pagerState.currentPage == nextPage }
                    param.confirmPageIsInCorrectPosition(pagerState.currentPage)
                }

                // reset
                resetTestCase()
            }
        }
    }

    @Test
    fun animateScrollToPage_fixedPageSize_shouldPlacePagesCorrectly() {
        // Arrange
        rule.setContent { config ->
            ParameterizedPager(
                modifier = Modifier.fillMaxSize(),
                orientation = config.orientation,
                layoutDirection = config.layoutDirection,
                reverseLayout = config.reverseLayout,
                snapPosition = config.snapPosition.first,
                pageSize = PageSize.Fixed(200.dp)
            )
        }

        rule.forEachParameter(PagerStateTestParams) { _ ->
            runBlocking {
                // Act and Assert
                repeat(DefaultAnimationRepetition) {
                    val nextPage = pagerState.currentPage + 1
                    withContext(Dispatchers.Main + AutoTestFrameClock()) {
                        pagerState.animateScrollToPage(nextPage)
                    }
                    rule.waitForIdle()
                    assertTrue { pagerState.currentPage == nextPage }
                    assertThat(pagerState.currentPageOffsetFraction).isWithin(0.1f).of(0.0f)
                }

                // reset
                resetTestCase()
            }
        }
    }

    @Test
    fun animateScrollToPage_usedOffset_shouldPlacePagesCorrectly() {
        // Arrange
        rule.setContent { config ->
            ParameterizedPager(
                modifier = Modifier.fillMaxSize(),
                orientation = config.orientation,
                layoutDirection = config.layoutDirection,
                reverseLayout = config.reverseLayout,
                snapPosition = config.snapPosition.first
            )
        }

        rule.forEachParameter(PagerStateTestParams) { param ->
            runBlocking {
                // Arrange
                suspend fun animateScrollToPageWithOffset(page: Int, offset: Float) {
                    withContext(Dispatchers.Main + AutoTestFrameClock()) {
                        pagerState.animateScrollToPage(page, offset)
                    }
                    rule.waitForIdle()
                }

                // Act
                animateScrollToPageWithOffset(10, 0.49f)

                // Assert
                param.confirmPageIsInCorrectPosition(pagerState.currentPage, 10, pageOffset = 0.49f)

                // Act
                animateScrollToPageWithOffset(4, 0.2f)

                // Assert
                param.confirmPageIsInCorrectPosition(pagerState.currentPage, 4, pageOffset = 0.2f)

                // Act
                animateScrollToPageWithOffset(12, -0.4f)

                // Assert
                param.confirmPageIsInCorrectPosition(pagerState.currentPage, 12, pageOffset = -0.4f)

                // Act
                animateScrollToPageWithOffset(DefaultPageCount - 1, 0.5f)

                // Assert
                param.confirmPageIsInCorrectPosition(pagerState.currentPage, DefaultPageCount - 1)

                // Act
                animateScrollToPageWithOffset(0, -0.5f)

                // Assert
                param.confirmPageIsInCorrectPosition(pagerState.currentPage, 0)

                // reset
                resetTestCase()
            }
        }
    }

    @Test
    fun animateScrollToPage_longSkipShouldNotPlaceIntermediatePages() {
        // Arrange
        rule.setContent { config ->
            ParameterizedPager(
                modifier = Modifier.fillMaxSize(),
                orientation = config.orientation,
                layoutDirection = config.layoutDirection,
                reverseLayout = config.reverseLayout,
                snapPosition = config.snapPosition.first
            )
        }

        rule.forEachParameter(PagerStateTestParams) { param ->
            runBlocking {
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
                param.confirmPageIsInCorrectPosition(pagerState.currentPage)

                // reset
                resetTestCase()
            }
        }
    }

    @Test
    fun scrollToPage_shouldCoerceWithinRange() {
        // Arrange
        rule.setContent { config ->
            ParameterizedPager(
                modifier = Modifier.fillMaxSize(),
                orientation = config.orientation,
                layoutDirection = config.layoutDirection,
                reverseLayout = config.reverseLayout,
                snapPosition = config.snapPosition.first
            )
        }

        rule.forEachParameter(PagerStateTestParams) {
            runBlocking {
                // Act
                assertThat(pagerState.currentPage).isEqualTo(0)
                withContext(Dispatchers.Main + AutoTestFrameClock()) {
                    pagerState.scrollToPage(DefaultPageCount)
                }

                // Assert
                rule.runOnIdle {
                    assertThat(pagerState.currentPage).isEqualTo(DefaultPageCount - 1)
                }

                // Act
                withContext(Dispatchers.Main + AutoTestFrameClock()) { pagerState.scrollToPage(-1) }

                // Assert
                rule.runOnIdle { assertThat(pagerState.currentPage).isEqualTo(0) }
                resetTestCase()
            }
        }
    }

    @Test
    fun animateScrollToPage_shouldCoerceWithinRange() {
        // Arrange
        rule.setContent { config ->
            ParameterizedPager(
                modifier = Modifier.fillMaxSize(),
                orientation = config.orientation,
                layoutDirection = config.layoutDirection,
                reverseLayout = config.reverseLayout,
                snapPosition = config.snapPosition.first
            )
        }

        rule.forEachParameter(PagerStateTestParams) {
            runBlocking {
                // Act
                assertThat(pagerState.currentPage).isEqualTo(0)
                withContext(Dispatchers.Main + AutoTestFrameClock()) {
                    pagerState.animateScrollToPage(DefaultPageCount)
                }

                // Assert
                rule.runOnIdle {
                    assertThat(pagerState.currentPage).isEqualTo(DefaultPageCount - 1)
                }

                // Act
                withContext(Dispatchers.Main + AutoTestFrameClock()) {
                    pagerState.animateScrollToPage(-1)
                }

                // Assert
                rule.runOnIdle { assertThat(pagerState.currentPage).isEqualTo(0) }
                resetTestCase()
            }
        }
    }

    @Test
    fun animateScrollToPage_moveToSamePageWithOffset_shouldScroll() {
        // Arrange
        rule.setContent { config ->
            ParameterizedPager(
                initialPage = 5,
                modifier = Modifier.fillMaxSize(),
                orientation = config.orientation,
                layoutDirection = config.layoutDirection,
                reverseLayout = config.reverseLayout,
                snapPosition = config.snapPosition.first
            )
        }

        rule.forEachParameter(PagerStateTestParams) {
            runBlocking {
                // Act
                assertThat(pagerState.currentPage).isEqualTo(5)

                withContext(Dispatchers.Main + AutoTestFrameClock()) {
                    pagerState.animateScrollToPage(5, 0.4f)
                }

                // Assert
                rule.runOnIdle { assertThat(pagerState.currentPage).isEqualTo(5) }
                rule.runOnIdle {
                    assertThat(pagerState.currentPageOffsetFraction).isWithin(0.01f).of(0.4f)
                }
                resetTestCase(initialPage = 5)
            }
        }
    }

    @Test
    fun animateScrollToPage_withPassedAnimation() {
        rule.mainClock.autoAdvance = false
        rule.setContent { config ->
            ParameterizedPager(
                modifier = Modifier.fillMaxSize(),
                orientation = config.orientation,
                layoutDirection = config.layoutDirection,
                reverseLayout = config.reverseLayout,
                snapPosition = config.snapPosition.first
            )
        }

        rule.forEachParameter(PagerStateTestParams) { param ->
            runBlocking {
                // Arrange
                val differentAnimation: AnimationSpec<Float> = tween()

                // Act and Assert
                repeat(DefaultAnimationRepetition) {
                    assertThat(pagerState.currentPage).isEqualTo(it)
                    val nextPage = pagerState.currentPage + 1
                    withContext(Dispatchers.Main + AutoTestFrameClock()) {
                        pagerState.animateScrollToPage(nextPage, animationSpec = differentAnimation)
                    }
                    rule.waitForIdle()
                    assertTrue { pagerState.currentPage == nextPage }
                    param.confirmPageIsInCorrectPosition(pagerState.currentPage)
                }
                resetTestCase()
            }
        }
    }

    @Test
    fun currentPage_shouldChangeWhenClosestPageToSnappedPositionChanges() {
        // Arrange
        rule.setContent { config ->
            ParameterizedPager(
                modifier = Modifier.fillMaxSize(),
                orientation = config.orientation,
                layoutDirection = config.layoutDirection,
                reverseLayout = config.reverseLayout,
                snapPosition = config.snapPosition.first
            )
        }
        rule.forEachParameter(PagerStateTestParams) { param ->
            var previousCurrentPage = pagerState.currentPage

            // Act
            // Move less than half an item
            val firstDelta = (pagerSize * 0.4f) * param.scrollForwardSign
            onPager().performTouchInput {
                down(with(param) { layoutStart })
                if (param.orientation == Orientation.Vertical) {
                    moveBy(Offset(0f, firstDelta))
                } else {
                    moveBy(Offset(firstDelta, 0f))
                }
            }

            // Assert
            rule.runOnIdle { assertThat(pagerState.currentPage).isEqualTo(previousCurrentPage) }
            // Release pointer
            onPager().performTouchInput { up() }

            rule.runOnIdle { previousCurrentPage = pagerState.currentPage }
            param.confirmPageIsInCorrectPosition(pagerState.currentPage)

            // Arrange
            // Pass closest to snap position threshold (over half an item)
            val secondDelta = (pagerSize * 0.6f) * param.scrollForwardSign

            // Act
            onPager().performTouchInput {
                down(with(param) { layoutStart })
                if (param.orientation == Orientation.Vertical) {
                    moveBy(Offset(0f, secondDelta))
                } else {
                    moveBy(Offset(secondDelta, 0f))
                }
            }

            // Assert
            rule.runOnIdle { assertThat(pagerState.currentPage).isEqualTo(previousCurrentPage + 1) }

            onPager().performTouchInput { up() }
            rule.waitForIdle()
            param.confirmPageIsInCorrectPosition(pagerState.currentPage)
            runBlocking { resetTestCase() }
        }
    }

    @Test
    fun targetPage_performScrollBelowMinThreshold_shouldNotShowNextPage() {
        // Arrange
        val snapDistance = PagerSnapDistance.atMost(3)
        rule.setContent { config ->
            ParameterizedPager(
                modifier = Modifier.fillMaxSize(),
                orientation = config.orientation,
                layoutDirection = config.layoutDirection,
                reverseLayout = config.reverseLayout,
                snappingPage = snapDistance,
                snapPosition = config.snapPosition.first
            )
        }

        rule.forEachParameter(PagerStateTestParams) { param ->
            rule.runOnIdle { assertThat(pagerState.targetPage).isEqualTo(pagerState.currentPage) }

            rule.mainClock.autoAdvance = false
            // Act
            // Moving less than threshold
            val forwardDelta =
                param.scrollForwardSign.toFloat() *
                    with(rule.density) { DefaultPositionThreshold.toPx() / 2 }

            var previousTargetPage = pagerState.targetPage

            onPager().performTouchInput {
                down(with(param) { layoutStart })
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
            val backwardDelta =
                param.scrollForwardSign.toFloat() *
                    with(rule.density) { -DefaultPositionThreshold.toPx() / 2 }

            previousTargetPage = pagerState.targetPage

            onPager().performTouchInput {
                down(with(param) { layoutStart })
                moveBy(Offset(backwardDelta, backwardDelta))
            }

            // Assert
            assertThat(pagerState.targetPage).isEqualTo(previousTargetPage)
            onPager().performTouchInput { up() }
            runBlocking { resetTestCase() }
        }
    }

    @Test
    fun targetPage_performScroll_shouldShowNextPage() {
        // Arrange
        val snapDistance = PagerSnapDistance.atMost(3)
        rule.setContent { config ->
            ParameterizedPager(
                modifier = Modifier.fillMaxSize(),
                orientation = config.orientation,
                layoutDirection = config.layoutDirection,
                reverseLayout = config.reverseLayout,
                snappingPage = snapDistance,
                snapPosition = config.snapPosition.first
            )
        }
        rule.forEachParameter(PagerStateTestParams) { param ->
            rule.runOnIdle { assertThat(pagerState.targetPage).isEqualTo(pagerState.currentPage) }

            rule.mainClock.autoAdvance = false
            // Act
            // Moving forward
            val forwardDelta = pagerSize * 0.4f * param.scrollForwardSign.toFloat()
            onPager().performTouchInput {
                down(with(param) { layoutStart })
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
            rule.runOnIdle { scope.launch { pagerState.scrollToPage(5) } }

            rule.mainClock.autoAdvance = false
            // Act
            // Moving backward
            val backwardDelta = -pagerSize * 0.4f * param.scrollForwardSign.toFloat()
            onPager().performTouchInput {
                down(with(param) { layoutEnd })
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
            runBlocking { resetTestCase() }
        }
    }

    @Test
    fun targetPage_performingFlingWithSnapFlingBehavior_shouldGoToPredictedPage() {
        // Arrange
        val snapDistance = PagerSnapDistance.atMost(3)
        rule.setContent { config ->
            ParameterizedPager(
                modifier = Modifier.fillMaxSize(),
                orientation = config.orientation,
                layoutDirection = config.layoutDirection,
                reverseLayout = config.reverseLayout,
                snappingPage = snapDistance,
                snapPosition = config.snapPosition.first
            )
        }
        rule.forEachParameter(PagerStateTestParams) { param ->
            rule.runOnIdle { assertThat(pagerState.targetPage).isEqualTo(pagerState.currentPage) }

            rule.mainClock.autoAdvance = false
            // Act
            // Moving forward
            var previousTarget = pagerState.targetPage
            val forwardDelta = pagerSize * param.scrollForwardSign.toFloat()
            onPager().performTouchInput {
                with(param) { swipeWithVelocityAcrossMainAxis(20000f, forwardDelta) }
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
            val backwardDelta = -pagerSize * param.scrollForwardSign.toFloat()
            onPager().performTouchInput {
                with(param) { swipeWithVelocityAcrossMainAxis(20000f, backwardDelta) }
            }
            rule.mainClock.advanceTimeUntil { pagerState.targetPage != previousTarget }

            // Assert
            flingOriginIndex = pagerState.firstVisiblePage + 1
            assertThat(pagerState.targetPage).isEqualTo(flingOriginIndex - 3)
            assertThat(pagerState.targetPage).isNotEqualTo(pagerState.currentPage)

            rule.mainClock.autoAdvance = true
            rule.runOnIdle { assertThat(pagerState.targetPage).isEqualTo(pagerState.currentPage) }
            runBlocking { resetTestCase() }
        }
    }

    @Test
    fun targetPage_performingFlingWithCustomFling_shouldGoToPredictedPage() {
        // Arrange
        var flingPredictedPage = -1
        val myCustomFling =
            object : TargetedFlingBehavior {
                val decay = splineBasedDecay<Float>(rule.density)

                override suspend fun ScrollScope.performFling(
                    initialVelocity: Float,
                    onRemainingDistanceUpdated: (Float) -> Unit
                ): Float {
                    val finalOffset = decay.calculateTargetValue(0.0f, initialVelocity)
                    val pageDisplacement = finalOffset / pagerState.pageSizeWithSpacing
                    val targetPage = pageDisplacement.roundToInt() + pagerState.currentPage
                    flingPredictedPage = targetPage
                    return if (abs(initialVelocity) > 1f) {
                        var velocityLeft = initialVelocity
                        var lastValue = 0f
                        val animationState =
                            AnimationState(
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

        // Arrange
        rule.setContent { config ->
            ParameterizedPager(
                modifier = Modifier.fillMaxSize(),
                orientation = config.orientation,
                layoutDirection = config.layoutDirection,
                reverseLayout = config.reverseLayout,
                pageCount = { 100 },
                flingBehavior = myCustomFling,
                snapPosition = config.snapPosition.first
            )
        }
        rule.forEachParameter(PagerStateTestParams) { param ->
            rule.runOnIdle { assertThat(pagerState.targetPage).isEqualTo(pagerState.currentPage) }

            // Act
            // Moving forward
            rule.mainClock.autoAdvance = false
            val forwardDelta = pagerSize * param.scrollForwardSign.toFloat()
            onPager().performTouchInput {
                with(param) { swipeWithVelocityAcrossMainAxis(20000f, forwardDelta) }
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
            val backwardDelta = -pagerSize * param.scrollForwardSign.toFloat()
            onPager().performTouchInput {
                with(param) { swipeWithVelocityAcrossMainAxis(20000f, backwardDelta) }
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
            runBlocking { resetTestCase() }
        }
    }

    @Test
    fun targetPage_shouldReflectTargetWithAnimation() {
        // Arrange
        rule.setContent { config ->
            ParameterizedPager(
                modifier = Modifier.fillMaxSize(),
                orientation = config.orientation,
                layoutDirection = config.layoutDirection,
                reverseLayout = config.reverseLayout,
                snapPosition = config.snapPosition.first
            )
        }

        rule.forEachParameter(PagerStateTestParams) {
            rule.runOnIdle { assertThat(pagerState.targetPage).isEqualTo(pagerState.currentPage) }

            rule.mainClock.autoAdvance = false
            // Act
            // Moving forward
            var previousTarget = pagerState.targetPage
            rule.runOnIdle { scope.launch { pagerState.animateScrollToPage(DefaultPageCount - 1) } }
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
            rule.runOnIdle { scope.launch { pagerState.animateScrollToPage(0) } }
            rule.mainClock.advanceTimeUntil { pagerState.targetPage != previousTarget }

            // Assert
            assertThat(pagerState.targetPage).isEqualTo(0)
            assertThat(pagerState.targetPage).isNotEqualTo(pagerState.currentPage)

            rule.mainClock.autoAdvance = true
            rule.runOnIdle { assertThat(pagerState.targetPage).isEqualTo(pagerState.currentPage) }
            runBlocking { resetTestCase() }
        }
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

        rule.setContent { config ->
            ParameterizedPager(
                modifier = Modifier.fillMaxSize(),
                orientation = config.orientation,
                layoutDirection = config.layoutDirection,
                reverseLayout = config.reverseLayout,
                snapPosition = config.snapPosition.first
            )
        }
        rule.forEachParameter(PagerStateTestParams) {
            rule.runOnIdle { assertThat(pagerState.targetPage).isEqualTo(pagerState.currentPage) }

            rule.mainClock.autoAdvance = false
            // Act
            // Moving forward
            var previousTarget = pagerState.targetPage
            rule.runOnIdle {
                scope.launch { pagerState.customAnimateScrollToPage(DefaultPageCount - 1) }
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
            rule.runOnIdle { scope.launch { pagerState.customAnimateScrollToPage(0) } }
            rule.mainClock.advanceTimeUntil { pagerState.targetPage != previousTarget }

            // Assert
            assertThat(pagerState.targetPage).isEqualTo(0)
            assertThat(pagerState.targetPage).isNotEqualTo(pagerState.currentPage)

            rule.mainClock.autoAdvance = true
            rule.runOnIdle { assertThat(pagerState.targetPage).isEqualTo(pagerState.currentPage) }
            runBlocking { resetTestCase() }
        }
    }

    @Test
    fun targetPage_valueAfterScrollingAfterMidpoint() {
        rule.setContent { config ->
            ParameterizedPager(
                initialPage = 5,
                modifier = Modifier.fillMaxSize(),
                orientation = config.orientation,
                layoutDirection = config.layoutDirection,
                reverseLayout = config.reverseLayout,
                snapPosition = config.snapPosition.first
            )
        }

        rule.forEachParameter(PagerStateTestParams) { param ->
            var previousCurrentPage = pagerState.currentPage

            val forwardDelta = (pagerSize * 0.7f) * param.scrollForwardSign
            onPager().performTouchInput {
                down(with(param) { layoutStart })
                if (param.orientation == Orientation.Vertical) {
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

            rule.runOnIdle { previousCurrentPage = pagerState.currentPage }

            val backwardDelta = (pagerSize * 0.7f) * param.scrollForwardSign * -1
            onPager().performTouchInput {
                down(with(param) { layoutEnd })
                if (param.orientation == Orientation.Vertical) {
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
            runBlocking { resetTestCase(initialPage = 5) }
        }
    }

    @Test
    fun targetPage_valueAfterScrollingForwardAndBackward() {
        rule.setContent { config ->
            ParameterizedPager(
                initialPage = 5,
                modifier = Modifier.fillMaxSize(),
                orientation = config.orientation,
                layoutDirection = config.layoutDirection,
                reverseLayout = config.reverseLayout,
                snapPosition = config.snapPosition.first
            )
        }

        rule.forEachParameter(PagerStateTestParams) { param ->
            val startCurrentPage = pagerState.currentPage

            val forwardDelta = (pagerSize * 0.8f) * param.scrollForwardSign
            onPager().performTouchInput {
                down(with(param) { layoutStart })
                if (param.orientation == Orientation.Vertical) {
                    moveBy(Offset(0f, forwardDelta))
                } else {
                    moveBy(Offset(forwardDelta, 0f))
                }
            }

            rule.runOnIdle {
                assertThat(pagerState.currentPage).isNotEqualTo(startCurrentPage)
                assertThat(pagerState.targetPage).isEqualTo(startCurrentPage + 1)
            }

            val backwardDelta = (pagerSize * 0.2f) * param.scrollForwardSign * -1
            onPager().performTouchInput {
                if (param.orientation == Orientation.Vertical) {
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
            runBlocking { resetTestCase(initialPage = 5) }
        }
    }

    @Test
    fun settledPage_onAnimationScroll_shouldChangeOnScrollFinishedOnly() {
        // Arrange
        var settledPageChanges = 0

        rule.setContent { config ->
            ParameterizedPager(
                modifier = Modifier.fillMaxSize(),
                orientation = config.orientation,
                layoutDirection = config.layoutDirection,
                reverseLayout = config.reverseLayout,
                snapPosition = config.snapPosition.first,
                additionalContent = {
                    LaunchedEffect(key1 = pagerState.settledPage) { settledPageChanges++ }
                }
            )
        }

        rule.forEachParameter(PagerStateTestParams) {
            // Settle page changed once for first composition
            rule.runOnIdle { assertThat(pagerState.settledPage).isEqualTo(pagerState.currentPage) }

            settledPageChanges = 0
            val previousSettled = pagerState.settledPage
            rule.mainClock.autoAdvance = false
            // Act
            // Moving forward
            rule.runOnIdle { scope.launch { pagerState.animateScrollToPage(DefaultPageCount - 1) } }

            // Settled page shouldn't change whilst scroll is in progress.
            assertTrue { pagerState.isScrollInProgress }
            assertTrue { settledPageChanges == 0 }
            assertThat(pagerState.settledPage).isEqualTo(previousSettled)

            rule.mainClock.advanceTimeUntil { settledPageChanges != 0 }

            rule.runOnIdle {
                assertTrue { !pagerState.isScrollInProgress }
                assertThat(pagerState.settledPage).isEqualTo(pagerState.currentPage)
            }
            rule.mainClock.autoAdvance = true // let time run freely
            runBlocking { resetTestCase() }
            settledPageChanges = 0
        }
    }

    @Test
    fun settledPage_onGestureScroll_shouldChangeOnScrollFinishedOnly() {
        // Arrange
        var settledPageChanges = 0
        rule.setContent { config ->
            ParameterizedPager(
                modifier = Modifier.fillMaxSize(),
                orientation = config.orientation,
                layoutDirection = config.layoutDirection,
                reverseLayout = config.reverseLayout,
                snapPosition = config.snapPosition.first,
                additionalContent = {
                    LaunchedEffect(key1 = pagerState.settledPage) { settledPageChanges++ }
                }
            )
        }

        rule.forEachParameter(PagerStateTestParams) { param ->
            settledPageChanges = 0
            val previousSettled = pagerState.settledPage
            rule.mainClock.autoAdvance = false
            // Act
            // Moving forward
            val forwardDelta = pagerSize / 2f * param.scrollForwardSign.toFloat()
            onPager().performTouchInput {
                with(param) { swipeWithVelocityAcrossMainAxis(10000f, forwardDelta) }
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
            runBlocking { resetTestCase() }
            rule.mainClock.autoAdvance = true // let time run freely
            settledPageChanges = 0
        }
    }

    @Test
    fun currentPageOffset_shouldReflectScrollingOfCurrentPage() {
        // Arrange
        rule.setContent { config ->
            ParameterizedPager(
                initialPage = DefaultPageCount / 2,
                modifier = Modifier.fillMaxSize(),
                orientation = config.orientation,
                layoutDirection = config.layoutDirection,
                reverseLayout = config.reverseLayout,
                snapPosition = config.snapPosition.first
            )
        }

        rule.forEachParameter(PagerStateTestParams) { param ->
            // No offset initially
            rule.runOnIdle {
                assertThat(pagerState.currentPageOffsetFraction).isWithin(0.01f).of(0f)
            }

            // Act
            // Moving forward
            onPager().performTouchInput {
                down(with(param) { layoutStart })
                if (param.orientation == Orientation.Vertical) {
                    moveBy(Offset(0f, param.scrollForwardSign * pagerSize / 4f))
                } else {
                    moveBy(Offset(param.scrollForwardSign * pagerSize / 4f, 0f))
                }
            }

            rule.runOnIdle {
                assertThat(pagerState.currentPageOffsetFraction).isWithin(0.1f).of(0.25f)
            }

            onPager().performTouchInput { up() }
            rule.waitForIdle()

            // Reset
            rule.runOnIdle { scope.launch { pagerState.scrollToPage(DefaultPageCount / 2) } }

            // No offset initially (again)
            rule.runOnIdle {
                assertThat(pagerState.currentPageOffsetFraction).isWithin(0.01f).of(0f)
            }

            // Act
            // Moving backward
            onPager().performTouchInput {
                down(with(param) { layoutStart })
                if (param.orientation == Orientation.Vertical) {
                    moveBy(Offset(0f, -param.scrollForwardSign * pagerSize / 4f))
                } else {
                    moveBy(Offset(-param.scrollForwardSign * pagerSize / 4f, 0f))
                }
            }

            rule.runOnIdle {
                assertThat(pagerState.currentPageOffsetFraction).isWithin(0.1f).of(-0.25f)
            }
            onPager().performTouchInput { up() }
            runBlocking { resetTestCase(initialPage = DefaultPageCount / 2) }
        }
    }

    @Test
    fun onScroll_shouldNotGenerateExtraMeasurements() {
        // Arrange
        var layoutCount = 0
        // Arrange
        rule.setContent { config ->
            ParameterizedPager(
                initialPage = 5,
                modifier =
                    Modifier.layout { measurable, constraints ->
                        layoutCount++
                        val placeables = measurable.measure(constraints)
                        layout(constraints.maxWidth, constraints.maxHeight) {
                            placeables.place(0, 0)
                        }
                    },
                layoutDirection = config.layoutDirection,
                reverseLayout = config.reverseLayout,
                snapPosition = config.snapPosition.first
            )
        }

        rule.forEachParameter(PagerStateTestParams) { param ->
            // Act: Scroll.
            val previousMeasurementCount = layoutCount
            val previousOffsetFraction = pagerState.currentPageOffsetFraction
            rule.runOnIdle {
                runBlocking { pagerState.scrollBy((pageSize * 0.2f) * param.scrollForwardSign) }
            }
            rule.runOnIdle {
                assertThat(pagerState.currentPageOffsetFraction)
                    .isNotEqualTo(previousOffsetFraction)
                assertThat(layoutCount).isEqualTo(previousMeasurementCount + 1)
            }
            runBlocking { resetTestCase(5) }
            layoutCount = 0
        }
    }

    private suspend fun resetTestCase(initialPage: Int = 0) {
        withContext(Dispatchers.Main + AutoTestFrameClock()) {
            pagerState.scrollToPage(initialPage)
        }
        placed.clear()
    }

    companion object {
        val PagerStateTestParams =
            mutableSetOf<SingleParamConfig>()
                .apply {
                    for (orientation in TestOrientation) {
                        for (snapPosition in TestSnapPosition) {
                            add(
                                SingleParamConfig(
                                    orientation = orientation,
                                    snapPosition = snapPosition
                                )
                            )
                        }
                    }

                    for (orientation in TestOrientation) {
                        for (reverseLayout in TestReverseLayout) {
                            for (layoutDirection in TestLayoutDirection) {
                                add(
                                    SingleParamConfig(
                                        orientation = orientation,
                                        reverseLayout = reverseLayout,
                                        layoutDirection = layoutDirection
                                    )
                                )
                            }
                        }
                    }
                }
                .toList()
    }
}
