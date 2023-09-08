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

import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.gestures.snapping.MinFlingVelocityDp
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.performTouchInput
import androidx.compose.ui.unit.dp
import androidx.test.filters.LargeTest
import com.google.common.truth.Truth.assertThat
import kotlinx.coroutines.runBlocking
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.junit.runners.Parameterized

@OptIn(ExperimentalFoundationApi::class)
@LargeTest
@RunWith(Parameterized::class)
class PagerScrollingTest(
    val config: ParamConfig
) : BasePagerTest(config) {

    @Before
    fun setUp() {
        rule.mainClock.autoAdvance = false
    }

    @Test
    fun swipeWithLowVelocity_positionalThresholdLessThanDefaultThreshold_shouldBounceBack() {
        // Arrange
        createPager(initialPage = 5, modifier = Modifier.fillMaxSize())
        val swipeValue = 0.4f
        val delta = pagerSize * swipeValue * scrollForwardSign

        // Act - forward
        runAndWaitForPageSettling {
            onPager().performTouchInput {
                swipeWithVelocityAcrossMainAxis(
                    with(rule.density) { 0.5f * MinFlingVelocityDp.toPx() },
                    delta
                )
            }
        }

        // Assert
        rule.onNodeWithTag("5").assertIsDisplayed()
        confirmPageIsInCorrectPosition(5)

        // Act - backward
        runAndWaitForPageSettling {
            onPager().performTouchInput {
                swipeWithVelocityAcrossMainAxis(
                    with(rule.density) { 0.5f * MinFlingVelocityDp.toPx() },
                    delta * -1
                )
            }
        }

        // Assert
        rule.onNodeWithTag("5").assertIsDisplayed()
        confirmPageIsInCorrectPosition(5)
    }

    @Test
    fun swipeWithLowVelocity_positionalThresholdLessThanLowThreshold_shouldBounceBack() {
        // Arrange
        createPager(
            initialPage = 5,
            modifier = Modifier.fillMaxSize(),
            snapPositionalThreshold = 0.2f
        )
        val swipeValue = 0.1f
        val delta = pagerSize * swipeValue * scrollForwardSign

        // Act - forward
        runAndWaitForPageSettling {
            onPager().performTouchInput {
                swipeWithVelocityAcrossMainAxis(
                    with(rule.density) { 0.5f * MinFlingVelocityDp.toPx() },
                    delta
                )
            }
        }

        // Assert
        rule.onNodeWithTag("5").assertIsDisplayed()
        confirmPageIsInCorrectPosition(5)

        // Act - backward
        runAndWaitForPageSettling {
            onPager().performTouchInput {
                swipeWithVelocityAcrossMainAxis(
                    with(rule.density) { 0.5f * MinFlingVelocityDp.toPx() },
                    delta * -1
                )
            }
        }

        // Assert
        rule.onNodeWithTag("5").assertIsDisplayed()
        confirmPageIsInCorrectPosition(5)
    }

    @Test
    fun swipeWithLowVelocity_positionalThresholdLessThanHighThreshold_shouldBounceBack() {
        // Arrange
        createPager(
            initialPage = 5,
            modifier = Modifier.fillMaxSize(),
            snapPositionalThreshold = 0.8f
        )
        val swipeValue = 0.6f
        val delta = pagerSize * swipeValue * scrollForwardSign

        // Act - forward
        runAndWaitForPageSettling {
            onPager().performTouchInput {
                swipeWithVelocityAcrossMainAxis(
                    with(rule.density) { 0.5f * MinFlingVelocityDp.toPx() },
                    delta
                )
            }
        }

        // Assert
        rule.onNodeWithTag("5").assertIsDisplayed()
        confirmPageIsInCorrectPosition(5)

        // Act - backward
        runAndWaitForPageSettling {
            onPager().performTouchInput {
                swipeWithVelocityAcrossMainAxis(
                    with(rule.density) { 0.5f * MinFlingVelocityDp.toPx() },
                    delta * -1
                )
            }
        }

        // Assert
        rule.onNodeWithTag("5").assertIsDisplayed()
        confirmPageIsInCorrectPosition(5)
    }

    @Test
    fun swipeWithLowVelocity_positionalThresholdLessThanDefault_customPageSize_shouldBounceBack() {
        // Arrange
        createPager(initialPage = 2, modifier = Modifier.fillMaxSize(), pageSize = {
            PageSize.Fixed(200.dp)
        })

        val delta = (2.4f * pageSize) * scrollForwardSign // 2.4 pages

        // Act - forward
        runAndWaitForPageSettling {
            onPager().performTouchInput {
                swipeWithVelocityAcrossMainAxis(
                    with(rule.density) { 0.5f * MinFlingVelocityDp.toPx() },
                    delta
                )
            }
        }

        // Assert
        rule.onNodeWithTag("4").assertIsDisplayed()
        confirmPageIsInCorrectPosition(4)

        // Act - backward
        runAndWaitForPageSettling {
            onPager().performTouchInput {
                swipeWithVelocityAcrossMainAxis(
                    with(rule.density) { 0.5f * MinFlingVelocityDp.toPx() },
                    delta * -1
                )
            }
        }

        // Assert
        rule.onNodeWithTag("2").assertIsDisplayed()
        confirmPageIsInCorrectPosition(2)
    }

    @Test
    fun swipeWithLowVelocity_positionalThresholdOverDefaultThreshold_shouldGoToNextPage() {
        // Arrange
        createPager(initialPage = 5, modifier = Modifier.fillMaxSize())
        val swipeValue = 0.51f
        val delta = pagerSize * swipeValue * scrollForwardSign

        // Act - forward
        runAndWaitForPageSettling {
            onPager().performTouchInput {
                swipeWithVelocityAcrossMainAxis(
                    with(rule.density) { 0.5f * MinFlingVelocityDp.toPx() },
                    delta
                )
            }
        }

        // Assert
        rule.onNodeWithTag("6").assertIsDisplayed()
        confirmPageIsInCorrectPosition(6)

        // Act - backward
        runAndWaitForPageSettling {
            onPager().performTouchInput {
                swipeWithVelocityAcrossMainAxis(
                    with(rule.density) { 0.5f * MinFlingVelocityDp.toPx() },
                    delta * -1
                )
            }
        }

        // Assert
        rule.onNodeWithTag("5").assertIsDisplayed()
        confirmPageIsInCorrectPosition(5)
    }

    @Test
    fun swipeWithLowVelocity_positionalThresholdOverLowThreshold_shouldGoToNextPage() {
        // Arrange
        createPager(
            initialPage = 5,
            modifier = Modifier.fillMaxSize(),
            snapPositionalThreshold = 0.2f
        )
        val swipeValue = 0.21f
        val delta = pagerSize * swipeValue * scrollForwardSign

        // Act - forward
        runAndWaitForPageSettling {
            onPager().performTouchInput {
                swipeWithVelocityAcrossMainAxis(
                    with(rule.density) { 0.5f * MinFlingVelocityDp.toPx() },
                    delta
                )
            }
        }

        // Assert
        rule.onNodeWithTag("6").assertIsDisplayed()
        confirmPageIsInCorrectPosition(6)

        // Act - backward
        runAndWaitForPageSettling {
            onPager().performTouchInput {
                swipeWithVelocityAcrossMainAxis(
                    with(rule.density) { 0.5f * MinFlingVelocityDp.toPx() },
                    delta * -1
                )
            }
        }

        // Assert
        rule.onNodeWithTag("5").assertIsDisplayed()
        confirmPageIsInCorrectPosition(5)
    }

    @Test
    fun swipeWithLowVelocity_onEdgeOfList_smallDeltas_shouldGoToClosestPage_backward() {
        // Arrange
        createPager(modifier = Modifier.fillMaxSize())
        val delta = 10f * scrollForwardSign * -1

        onPager().performTouchInput {
            down(center)
            // series of backward delta on edge
            moveBy(Offset(if (vertical) 0.0f else delta, if (vertical) delta else 0.0f))
            moveBy(Offset(if (vertical) 0.0f else delta, if (vertical) delta else 0.0f))
            moveBy(Offset(if (vertical) 0.0f else delta, if (vertical) delta else 0.0f))

            // single delta on opposite direction
            moveBy(
                Offset(
                    if (vertical) 0.0f else -delta,
                    if (vertical) -delta else 0.0f
                )
            )
            up()
        }
        rule.mainClock.advanceTimeUntil { pagerState.isScrollInProgress == false }

        // Assert
        rule.onNodeWithTag("0").assertIsDisplayed()
        confirmPageIsInCorrectPosition(0)
    }

    @Test
    fun swipeWithLowVelocity_onEdgeOfList_smallDeltas_shouldGoToClosestPage_forward() {
        // Arrange
        createPager(modifier = Modifier.fillMaxSize(), initialPage = DefaultPageCount - 1)
        val delta = 10f * scrollForwardSign

        onPager().performTouchInput {
            down(center)
            // series of backward delta on edge
            moveBy(Offset(if (vertical) 0.0f else delta, if (vertical) delta else 0.0f))
            moveBy(Offset(if (vertical) 0.0f else delta, if (vertical) delta else 0.0f))
            moveBy(Offset(if (vertical) 0.0f else delta, if (vertical) delta else 0.0f))

            // single delta on opposite direction
            moveBy(
                Offset(
                    if (vertical) 0.0f else -delta,
                    if (vertical) -delta else 0.0f
                )
            )
            up()
        }
        rule.mainClock.advanceTimeUntil { pagerState.isScrollInProgress == false }

        // Assert
        rule.onNodeWithTag("${DefaultPageCount - 1}").assertIsDisplayed()
        confirmPageIsInCorrectPosition(DefaultPageCount - 1)
    }

    @Test
    fun swipeWithLowVelocity_positionalThresholdOverThreshold_customPage_shouldGoToNextPage() {
        // Arrange
        createPager(
            initialPage = 2,
            modifier = Modifier.fillMaxSize(),
            pageSize = {
                PageSize.Fixed(200.dp)
            }
        )

        val delta = 2.6f * pageSize * scrollForwardSign

        // Act - forward
        runAndWaitForPageSettling {
            onPager().performTouchInput {
                swipeWithVelocityAcrossMainAxis(
                    with(rule.density) { 0.5f * MinFlingVelocityDp.toPx() },
                    delta
                )
            }
        }

        // Assert
        rule.onNodeWithTag("5").assertIsDisplayed()
        confirmPageIsInCorrectPosition(5)

        // Act - backward
        runAndWaitForPageSettling {
            onPager().performTouchInput {
                swipeWithVelocityAcrossMainAxis(
                    with(rule.density) { 0.5f * MinFlingVelocityDp.toPx() },
                    delta * -1
                )
            }
        }

        // Assert
        rule.onNodeWithTag("2").assertIsDisplayed()
        confirmPageIsInCorrectPosition(2)
    }

    @Test
    fun swipeWithLowVelocity_positionalThresholdOverHighThreshold_shouldGoToNextPage() {
        // Arrange
        createPager(
            initialPage = 5,
            modifier = Modifier.fillMaxSize(),
            snapPositionalThreshold = 0.8f
        )
        val swipeValue = 0.81f
        val delta = pagerSize * swipeValue * scrollForwardSign

        // Act - forward
        runAndWaitForPageSettling {
            onPager().performTouchInput {
                swipeWithVelocityAcrossMainAxis(
                    with(rule.density) { 0.5f * MinFlingVelocityDp.toPx() },
                    delta
                )
            }
        }

        // Assert
        rule.onNodeWithTag("6").assertIsDisplayed()
        confirmPageIsInCorrectPosition(6)

        // Act - backward
        runAndWaitForPageSettling {
            onPager().performTouchInput {
                swipeWithVelocityAcrossMainAxis(
                    with(rule.density) { 0.5f * MinFlingVelocityDp.toPx() },
                    delta * -1
                )
            }
        }

        // Assert
        rule.onNodeWithTag("5").assertIsDisplayed()
        confirmPageIsInCorrectPosition(5)
    }

    @Test
    fun swipeWithLowVelocity_customVelocityThreshold_shouldBounceBack() {
        // Arrange
        val snapVelocityThreshold = 200.dp
        createPager(
            initialPage = 5,
            modifier = Modifier.fillMaxSize(),
            snapVelocityThreshold = snapVelocityThreshold
        )
        val delta = pagerSize * 0.4f * scrollForwardSign

        // Act - forward
        runAndWaitForPageSettling {
            onPager().performTouchInput {
                swipeWithVelocityAcrossMainAxis(
                    with(rule.density) { 0.5f * snapVelocityThreshold.toPx() },
                    delta
                )
            }
        }

        // Assert
        rule.onNodeWithTag("5").assertIsDisplayed()
        confirmPageIsInCorrectPosition(5)

        // Act - backward
        runAndWaitForPageSettling {
            onPager().performTouchInput {
                swipeWithVelocityAcrossMainAxis(
                    with(rule.density) { 0.5f * snapVelocityThreshold.toPx() },
                    delta * -1
                )
            }
        }

        // Assert
        rule.onNodeWithTag("5").assertIsDisplayed()
        confirmPageIsInCorrectPosition(5)
    }

    @Test
    fun swipeWithHighVelocity_defaultVelocityThreshold_shouldGoToNextPage() {
        // Arrange
        createPager(initialPage = 5, modifier = Modifier.fillMaxSize())
        // make sure the scroll distance is not enough to go to next page
        val delta = pagerSize * 0.4f * scrollForwardSign

        // Act - forward
        runAndWaitForPageSettling {
            onPager().performTouchInput {
                swipeWithVelocityAcrossMainAxis(
                    with(rule.density) { 1.1f * MinFlingVelocityDp.toPx() },
                    delta
                )
            }
        }

        // Assert
        rule.onNodeWithTag("6").assertIsDisplayed()
        confirmPageIsInCorrectPosition(6)

        // Act - backward
        runAndWaitForPageSettling {
            onPager().performTouchInput {
                swipeWithVelocityAcrossMainAxis(
                    with(rule.density) { 1.1f * MinFlingVelocityDp.toPx() },
                    delta * -1
                )
            }
        }

        // Assert
        rule.onNodeWithTag("5").assertIsDisplayed()
        confirmPageIsInCorrectPosition(5)
    }

    @Test
    fun swipeWithHighVelocity_customVelocityThreshold_shouldGoToNextPage() {
        // Arrange
        val snapVelocityThreshold = 200.dp
        createPager(
            initialPage = 5,
            modifier = Modifier.fillMaxSize(),
            snapVelocityThreshold = snapVelocityThreshold
        )
        // make sure the scroll distance is not enough to go to next page
        val delta = pagerSize * 0.4f * scrollForwardSign

        // Act - forward
        runAndWaitForPageSettling {
            onPager().performTouchInput {
                swipeWithVelocityAcrossMainAxis(
                    with(rule.density) { 1.1f * snapVelocityThreshold.toPx() },
                    delta
                )
            }
        }

        // Assert
        rule.onNodeWithTag("6").assertIsDisplayed()
        confirmPageIsInCorrectPosition(6)

        // Act - backward
        runAndWaitForPageSettling {
            onPager().performTouchInput {
                swipeWithVelocityAcrossMainAxis(
                    with(rule.density) { 1.1f * snapVelocityThreshold.toPx() },
                    delta * -1
                )
            }
        }

        // Assert
        rule.onNodeWithTag("5").assertIsDisplayed()
        confirmPageIsInCorrectPosition(5)
    }

    @Test
    fun swipeWithHighVelocity_overHalfPage_shouldGoToNextPage() {
        // Arrange
        createPager(initialPage = 5, modifier = Modifier.fillMaxSize())
        // make sure the scroll distance is not enough to go to next page
        val delta = pagerSize * 0.8f * scrollForwardSign

        // Act - forward
        runAndWaitForPageSettling {
            onPager().performTouchInput {
                swipeWithVelocityAcrossMainAxis(
                    with(rule.density) { 1.1f * MinFlingVelocityDp.toPx() },
                    delta
                )
            }
        }

        // Assert
        rule.onNodeWithTag("6").assertIsDisplayed()
        confirmPageIsInCorrectPosition(6)

        // Act - backward
        runAndWaitForPageSettling {
            onPager().performTouchInput {
                swipeWithVelocityAcrossMainAxis(
                    with(rule.density) { 1.1f * MinFlingVelocityDp.toPx() },
                    delta * -1
                )
            }
        }

        // Assert
        rule.onNodeWithTag("5").assertIsDisplayed()
        confirmPageIsInCorrectPosition(5)
    }

    @Test
    fun scrollWithoutVelocity_shouldSettlingInClosestPage() {
        // Arrange
        createPager(initialPage = 5, modifier = Modifier.fillMaxSize())
        // This will scroll 1 whole page before flinging
        val delta = pagerSize * 1.4f * scrollForwardSign

        // Act - forward
        runAndWaitForPageSettling {
            onPager().performTouchInput {
                swipeWithVelocityAcrossMainAxis(0f, delta)
            }
        }

        // Assert
        assertThat(pagerState.currentPage).isAtMost(7)
        rule.onNodeWithTag("${pagerState.currentPage}").assertIsDisplayed()
        confirmPageIsInCorrectPosition(pagerState.currentPage)

        // Act - backward
        runAndWaitForPageSettling {
            onPager().performTouchInput {
                swipeWithVelocityAcrossMainAxis(0f, delta * -1)
            }
        }

        // Assert
        assertThat(pagerState.currentPage).isAtLeast(5)
        rule.onNodeWithTag("${pagerState.currentPage}").assertIsDisplayed()
        confirmPageIsInCorrectPosition(pagerState.currentPage)
    }

    @Test
    fun scrollWithSameVelocity_shouldYieldSameResult_forward() {
        // Arrange
        var initialPage = 1
        createPager(
            pageSize = { PageSize.Fixed(200.dp) },
            initialPage = initialPage,
            modifier = Modifier.fillMaxSize(),
            pageCount = { 100 },
            snappingPage = PagerSnapDistance.atMost(3)
        )
        // This will scroll 0.5 page before flinging
        val delta = pagerSize * 0.5f * scrollForwardSign

        // Act - forward
        onPager().performTouchInput {
            swipeWithVelocityAcrossMainAxis(2000f, delta)
        }
        rule.waitForIdle()

        val pageDisplacement = pagerState.currentPage - initialPage

        // Repeat starting from different places
        // reset
        initialPage = 10
        rule.runOnIdle {
            runBlocking { pagerState.scrollToPage(initialPage) }
        }

        onPager().performTouchInput {
            swipeWithVelocityAcrossMainAxis(2000f, delta)
        }
        rule.waitForIdle()

        assertThat(pagerState.currentPage - initialPage).isEqualTo(pageDisplacement)

        initialPage = 50
        rule.runOnIdle {
            runBlocking { pagerState.scrollToPage(initialPage) }
        }

        onPager().performTouchInput {
            swipeWithVelocityAcrossMainAxis(2000f, delta)
        }
        rule.waitForIdle()

        assertThat(pagerState.currentPage - initialPage).isEqualTo(pageDisplacement)
    }

    @Test
    fun scrollWithSameVelocity_shouldYieldSameResult_backward() {
        // Arrange
        var initialPage = 90
        createPager(
            pageSize = { PageSize.Fixed(200.dp) },
            initialPage = initialPage,
            modifier = Modifier.fillMaxSize(),
            pageCount = { 100 },
            snappingPage = PagerSnapDistance.atMost(3)
        )
        // This will scroll 0.5 page before flinging
        val delta = pagerSize * -0.5f * scrollForwardSign

        // Act - forward
        onPager().performTouchInput {
            swipeWithVelocityAcrossMainAxis(2000f, delta)
        }
        rule.waitForIdle()

        val pageDisplacement = pagerState.currentPage - initialPage

        // Repeat starting from different places
        // reset
        initialPage = 70
        rule.runOnIdle {
            runBlocking { pagerState.scrollToPage(initialPage) }
        }

        onPager().performTouchInput {
            swipeWithVelocityAcrossMainAxis(2000f, delta)
        }
        rule.waitForIdle()

        assertThat(pagerState.currentPage - initialPage).isEqualTo(pageDisplacement)

        initialPage = 30
        rule.runOnIdle {
            runBlocking { pagerState.scrollToPage(initialPage) }
        }

        onPager().performTouchInput {
            swipeWithVelocityAcrossMainAxis(2000f, delta)
        }
        rule.waitForIdle()

        assertThat(pagerState.currentPage - initialPage).isEqualTo(pageDisplacement)
    }

    companion object {
        @JvmStatic
        @Parameterized.Parameters(name = "{0}")
        fun params() = mutableListOf<ParamConfig>().apply {
            for (orientation in TestOrientation) {
                for (pageSpacing in TestPageSpacing) {
                    add(
                        ParamConfig(
                            orientation = orientation,
                            pageSpacing = pageSpacing
                        )
                    )
                }
            }
        }
    }
}
