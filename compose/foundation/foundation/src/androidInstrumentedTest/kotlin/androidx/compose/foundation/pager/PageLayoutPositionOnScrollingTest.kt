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

import androidx.compose.foundation.gestures.snapping.MinFlingVelocityDp
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.ui.Modifier
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.performTouchInput
import androidx.test.filters.LargeTest
import kotlinx.coroutines.runBlocking
import org.junit.Before
import org.junit.Test

@LargeTest
class PageLayoutPositionOnScrollingTest : SingleParamBasePagerTest() {

    @Before
    fun setUp() {
        rule.mainClock.autoAdvance = false
    }

    @Test
    fun swipeForwardAndBackward_verifyPagesAreLaidOutCorrectly() =
        with(rule) {
            // Arrange
            setContent {
                ParameterizedPager(
                    modifier = Modifier.fillMaxSize(),
                    orientation = it.orientation,
                    layoutDirection = it.layoutDirection,
                    pageSpacing = it.pageSpacing,
                    contentPadding = it.mainAxisContentPadding,
                    reverseLayout = it.reverseLayout
                )
            }

            forEachParameter(ParamsToTest) { param ->
                val delta = pagerSize * 0.4f * param.scrollForwardSign

                // Act and Assert - forward
                repeat(DefaultAnimationRepetition) {
                    onNodeWithTag(it.toString()).assertIsDisplayed()
                    param.confirmPageIsInCorrectPosition(it)
                    runAndWaitForPageSettling {
                        onNodeWithTag(it.toString()).performTouchInput {
                            with(param) {
                                swipeWithVelocityAcrossMainAxis(
                                    with(rule.density) { 1.5f * MinFlingVelocityDp.toPx() },
                                    delta
                                )
                            }
                        }
                    }
                }

                // Act - backward
                repeat(DefaultAnimationRepetition) {
                    val countDown = DefaultAnimationRepetition - it
                    onNodeWithTag(countDown.toString()).assertIsDisplayed()
                    param.confirmPageIsInCorrectPosition(countDown)
                    runAndWaitForPageSettling {
                        rule.onNodeWithTag(countDown.toString()).performTouchInput {
                            with(param) {
                                swipeWithVelocityAcrossMainAxis(
                                    with(rule.density) { 1.5f * MinFlingVelocityDp.toPx() },
                                    delta * -1f
                                )
                            }
                        }
                    }
                }

                resetTestCase()
            }
        }

    private fun resetTestCase() {
        rule.runOnIdle { runBlocking { pagerState.scrollToPage(0) } }
    }

    companion object {
        val ParamsToTest =
            mutableListOf<SingleParamConfig>().apply {
                for (orientation in TestOrientation) {
                    for (pageSpacing in TestPageSpacing) {
                        for (reverseLayout in TestReverseLayout) {
                            for (layoutDirection in TestLayoutDirection) {
                                for (contentPadding in testContentPaddings(orientation)) {
                                    add(
                                        SingleParamConfig(
                                            orientation = orientation,
                                            mainAxisContentPadding = contentPadding,
                                            reverseLayout = reverseLayout,
                                            layoutDirection = layoutDirection,
                                            pageSpacing = pageSpacing
                                        )
                                    )
                                }
                            }
                        }
                    }
                }
            }
    }
}
