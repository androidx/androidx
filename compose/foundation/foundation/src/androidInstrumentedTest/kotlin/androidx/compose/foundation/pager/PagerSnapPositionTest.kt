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
import androidx.compose.foundation.gestures.snapping.MinFlingVelocityDp
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.ui.Modifier
import androidx.compose.ui.test.performTouchInput
import androidx.compose.ui.unit.Density
import androidx.test.filters.LargeTest
import com.google.common.truth.Truth.assertThat
import com.google.common.truth.Truth.assertWithMessage
import kotlin.math.roundToInt
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.withContext
import org.junit.Test

@LargeTest
class PagerSnapPositionTest : SingleParamBasePagerTest() {

    @Test
    fun snapPosition_shouldNotInfluenceMaxScroll() = with(rule) {
        val PageSize = object : PageSize {
            override fun Density.calculateMainAxisPageSize(
                availableSpace: Int,
                pageSpacing: Int
            ): Int {
                return (availableSpace + pageSpacing) / 2
            }
        }
        setContent {
            ParameterizedPager(
                modifier = Modifier.fillMaxSize(),
                pageSize = PageSize,
                orientation = it.orientation,
                snapPosition = it.snapPosition.first
            )
        }

        forEachParameter(ParamsToTest) { param ->
            runBlocking { resetTestCase(DefaultPageCount - 2) }
            val velocity = with(density) { 2 * MinFlingVelocityDp.roundToPx() }.toFloat()
            val forwardDelta = (pageSize) * param.scrollForwardSign
            onPager().performTouchInput {
                with(param) {
                    swipeWithVelocityAcrossMainAxis(velocity, forwardDelta.toFloat())
                }
            }

            runOnIdle {
                assertThat(pagerState.canScrollForward).isFalse()
            }
        }
    }

    @Test
    fun pagerAtBounds_flingTowardsBound_doesNotMove() {
        rule.setContent {
            ParameterizedPager(
                modifier = Modifier.fillMaxSize(),
                pageSize = object : PageSize {
                    override fun Density.calculateMainAxisPageSize(
                        availableSpace: Int,
                        pageSpacing: Int
                    ) = ((availableSpace - 2 * pageSpacing) / 2.5).roundToInt()
                },
                orientation = it.orientation,
                snapPosition = it.snapPosition.first
            )
        }

        val velocity = with(rule.density) { 0.3 * MinFlingVelocityDp.roundToPx() }.toFloat()

        rule.forEachParameter(ParamsToTest) { param ->
            runBlocking { resetTestCase(initialPage = 0) }

            // When we are at the start of the layout, we should only be able to move forward
            assertThat(pagerState.canScrollForward).isTrue()
            assertThat(pagerState.canScrollBackward).isFalse()
            // Our offset fraction can differ depending on the snap position, so we only care about
            // any changes after interactions
            val fractionBeforeFirstSwipe = pagerState.currentPageOffsetFraction
            val pageBeforeFirstSwipe = pagerState.currentPage

            onPager().performTouchInput {
                with(param) {
                    swipeWithVelocityAcrossMainAxis(
                        velocity = velocity,
                        delta = (pageSize / 2f) * (scrollForwardSign * -1)
                    )
                }
            }
            rule.waitForIdle()

            assertWithMessage("currentPageOffsetFraction should not have changed")
                .that(pagerState.currentPageOffsetFraction).isEqualTo(fractionBeforeFirstSwipe)
            assertThat(pagerState.currentPage).isEqualTo(pageBeforeFirstSwipe)
            assertThat(pagerState.canScrollForward).isTrue()
            assertThat(pagerState.canScrollBackward).isFalse()

            // Scroll to the end of the layout
            rule.runOnUiThread {
                runBlocking { pagerState.scrollToPage(DefaultPageCount - 1) }
            }
            rule.waitForIdle()

            // When we are at the end of the layout, we should only be able to move backwards
            assertThat(pagerState.canScrollForward).isFalse()
            assertThat(pagerState.canScrollBackward).isTrue()

            val fractionBeforeSecondSwipe = pagerState.currentPageOffsetFraction
            val pageBeforeSecondSwipe = pagerState.currentPage

            onPager().performTouchInput {
                with(param) {
                    swipeWithVelocityAcrossMainAxis(
                        velocity = velocity,
                        delta = (pageSize / 2f) * scrollForwardSign
                    )
                }
            }
            rule.waitForIdle()

            assertWithMessage("currentPageOffsetFraction should not have changed")
                .that(pagerState.currentPageOffsetFraction).isEqualTo(fractionBeforeSecondSwipe)
            assertThat(pagerState.currentPage).isEqualTo(pageBeforeSecondSwipe)
            assertThat(pagerState.canScrollForward).isFalse()
            assertThat(pagerState.canScrollBackward).isTrue()
        }
    }

    private suspend fun resetTestCase(initialPage: Int = 0) {
        withContext(Dispatchers.Main + AutoTestFrameClock()) {
            pagerState.scrollToPage(initialPage)
        }
    }

    companion object {
        val ParamsToTest = mutableListOf<SingleParamConfig>().apply {
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
        }
    }
}
